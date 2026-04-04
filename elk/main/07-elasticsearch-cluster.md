# 클러스터 관리와 샤드 라우팅

Elasticsearch 클러스터의 샤드 할당 전략, Rebalancing 메커니즘, Split Brain 방지, Cluster State 관리 등 운영에 필수적인 내부 동작 원리를 다룬다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### 클러스터 구성 요소

Elasticsearch 클러스터는 여러 노드로 구성되며, 각 노드는 하나 이상의 역할(role)을 가진다.

```mermaid
flowchart TB
    subgraph Cluster["Elasticsearch Cluster"]
        M["Master Node\n클러스터 상태 관리"]
        D1["Data Node 1\n샤드 저장/검색"]
        D2["Data Node 2\n샤드 저장/검색"]
        D3["Data Node 3\n샤드 저장/검색"]
        C["Coordinating Node\n요청 라우팅/응답 병합"]
        I["Ingest Node\n파이프라인 전처리"]
    end

    Client["Client"] --> C
    C --> D1
    C --> D2
    C --> D3
    M -.->|"Cluster State"| D1
    M -.->|"Cluster State"| D2
    M -.->|"Cluster State"| D3

    style M fill:#fff3e0
    style C fill:#e1f5fe
    style D1 fill:#e8f5e9
    style D2 fill:#e8f5e9
    style D3 fill:#e8f5e9
```

### 노드 역할 (Node Roles)

| 역할 | 설정값 | 책임 |
|------|--------|------|
| **Master-eligible** | `master` | 클러스터 상태 관리, 샤드 할당 결정 |
| **Data** | `data` | 샤드 저장, 인덱싱/검색 수행 |
| **Data (tiered)** | `data_hot`, `data_warm`, `data_cold`, `data_frozen` | ILM 기반 계층화 저장 |
| **Ingest** | `ingest` | Ingest Pipeline으로 문서 전처리 |
| **Coordinating** | (전용 설정 없음, 모든 역할 제거) | 요청 라우팅, 응답 집계 |
| **ML** | `ml` | 머신러닝 작업 실행 |

### 라우팅 알고리즘

문서가 어떤 샤드에 저장되는지는 다음 공식으로 결정된다:

```
shard_num = hash(_routing) % num_primary_shards
```

- `_routing`의 기본값은 문서의 `_id`
- 이것이 **인덱스 생성 후 Primary 샤드 수를 변경할 수 없는 이유**다 (Split/Shrink API 제외)

## 2. 왜 알아야 하는가 (Why)

### 운영 안정성의 핵심

- 샤드 할당을 이해하지 못하면 특정 노드에 샤드가 편중되어 핫스팟 발생
- Split Brain 발생 시 데이터 불일치로 복구 불가능한 상태에 빠질 수 있음
- Rebalancing 중 과도한 디스크 I/O로 서비스 성능 저하

### 확장과 축소 전략의 근거

- 노드 추가/제거 시 샤드가 어떻게 재배치되는지 이해해야 안전한 스케일링 가능
- Allocation Awareness를 설정하지 않으면 같은 AZ(Availability Zone)에 Primary와 Replica가 모두 배치될 수 있음

### 비용 최적화

- Hot-Warm-Cold 아키텍처에서 샤드를 적절한 티어로 이동시켜 스토리지 비용 절감
- 불필요한 Replica 수를 줄여 디스크와 메모리 절약

## 3. 내부 구현 분석 (How)

### 3.1 샤드 할당 전략 (Shard Allocation)

샤드 할당은 Master 노드의 **AllocationService**가 담당한다. Decider 체인을 통해 각 샤드의 배치를 결정한다.

```mermaid
flowchart TB
    A["Unassigned Shard"] --> B["AllocationService"]
    B --> C{"Decider Chain"}
    C --> D["SameShardAllocationDecider\n같은 샤드의 Primary/Replica가\n같은 노드에 배치되지 않도록"]
    C --> E["DiskThresholdDecider\n디스크 사용률 초과 시\n할당 거부"]
    C --> F["AwarenessAllocationDecider\nZone/Rack awareness\n기반 분산"]
    C --> G["FilterAllocationDecider\n노드 속성 기반\n포함/제외 필터"]
    C --> H["RebalanceAllocationDecider\n클러스터 균형 유지"]

    D --> I{"모든 Decider 통과?"}
    E --> I
    F --> I
    G --> I
    H --> I
    I -->|"YES"| J["샤드 할당"]
    I -->|"NO"| K["할당 보류/거부"]

    style C fill:#fff3e0
    style J fill:#e8f5e9
    style K fill:#ffcdd2
```

#### 주요 Decider 설명

| Decider | 역할 |
|---------|------|
| `SameShardAllocationDecider` | Primary와 Replica를 다른 노드에 배치 |
| `DiskThresholdDecider` | Low watermark(85%), High watermark(90%), Flood stage(95%) |
| `AwarenessAllocationDecider` | 특정 속성(zone, rack) 기준으로 분산 배치 |
| `FilterAllocationDecider` | 인덱스/클러스터 레벨에서 노드 필터링 |
| `MaxRetryAllocationDecider` | 할당 실패 시 최대 재시도 횟수 제어 |

#### 디스크 기반 Watermark

```
0%                    85%              90%              95%         100%
├─────── 정상 ────────┤── Low WM ──────┤── High WM ─────┤─ Flood ──┤
                      │                │                │
                      │ 새 샤드 할당    │ 샤드 재배치     │ 인덱싱
                      │ 중단           │ 시작           │ 차단
```

### 3.2 Rebalancing 동작 원리

Rebalancing은 노드 간 샤드 수의 균형을 맞추는 과정이다.

```mermaid
sequenceDiagram
    participant Master as Master Node
    participant Node1 as Node 1 (8 shards)
    participant Node2 as Node 2 (2 shards)
    participant Node3 as Node 3 (신규)

    Note over Master: 불균형 감지<br/>threshold 초과

    Master->>Node1: 샤드 S3 이동 명령
    Node1->>Node3: S3 데이터 복사 (peer recovery)
    Node3-->>Master: S3 할당 완료
    Master->>Node1: S3 삭제 확인

    Master->>Node1: 샤드 S5 이동 명령
    Node1->>Node3: S5 데이터 복사
    Node3-->>Master: S5 할당 완료

    Note over Master: 균형 달성<br/>Node1:5, Node2:2, Node3:3
```

**Rebalancing 관련 설정:**

| 설정 | 기본값 | 설명 |
|------|--------|------|
| `cluster.routing.rebalance.enable` | `all` | Rebalancing 대상 (all, primaries, replicas, none) |
| `cluster.routing.allocation.balance.shard` | `0.45f` | 노드별 전체 샤드 수 균형 가중치 |
| `cluster.routing.allocation.balance.index` | `0.55f` | 인덱스별 샤드 수 균형 가중치 |
| `cluster.routing.allocation.balance.threshold` | `1.0f` | 이 값 이하의 불균형은 무시 |
| `cluster.routing.allocation.cluster_concurrent_rebalance` | `2` | 동시 Rebalancing 샤드 수 |

### 3.3 Split Brain 방지 메커니즘

Split Brain은 네트워크 파티션으로 클러스터가 둘 이상으로 분리되어 각각 독립적인 Master를 선출하는 상황이다.

```mermaid
flowchart TB
    subgraph Before["정상 상태"]
        M1_B["Node A\n(Master)"] --- D1_B["Node B"]
        M1_B --- D2_B["Node C"]
        D1_B --- D2_B
    end

    subgraph SplitBrain["Split Brain (위험!)"]
        subgraph Part1["파티션 1"]
            M1["Node A\n(Master 1)"]
        end
        subgraph Part2["파티션 2"]
            M2["Node B\n(Master 2)"] --- D2["Node C"]
        end
    end

    Before -->|"네트워크 분리"| SplitBrain

    style SplitBrain fill:#ffcdd2
    style Part1 fill:#fff3e0
    style Part2 fill:#fff3e0
```

#### Elasticsearch 7.x+ 방지 메커니즘

Elasticsearch 7.x부터 `discovery.zen.minimum_master_nodes` 설정이 제거되고, 자동화된 **Quorum 기반 선출**이 도입되었다.

**핵심 원리:**
- Master 선출에 과반수(quorum) 이상의 master-eligible 노드 투표 필요
- `cluster.initial_master_nodes`는 최초 클러스터 부트스트랩 시에만 사용
- 이후에는 클러스터 자체가 투표 구성(voting configuration)을 관리

```
Master-eligible 노드: A, B, C (3개)
Quorum = floor(3/2) + 1 = 2

네트워크 파티션 발생:
  파티션 1: [A]     → 1 < 2 (quorum 미달) → Master 선출 불가
  파티션 2: [B, C]  → 2 >= 2 (quorum 충족) → B 또는 C가 Master 선출

→ Split Brain 방지: 항상 하나의 파티션만 Master를 가질 수 있음
```

**권장 구성:**
- Master-eligible 노드는 **3개** (또는 홀수 개)
- 2개 AZ라면 tiebreaker 노드를 3번째 AZ에 배치
- Dedicated master node 사용 권장 (data 역할 분리)

### 3.4 Cluster State 관리와 전파

Cluster State는 클러스터의 모든 메타데이터를 포함하는 전역 상태 객체다.

```mermaid
flowchart TB
    subgraph ClusterState["Cluster State 구성"]
        N["nodes\n클러스터 내 모든 노드 정보"]
        I["metadata\n인덱스 매핑, 설정, 템플릿"]
        R["routing_table\n샤드→노드 매핑 정보"]
        B["blocks\n인덱스/클러스터 레벨 블록"]
        CS["custom\nILM, SLM 등 플러그인 상태"]
    end

    M["Master Node"] -->|"상태 변경 시\n전체 diff 전파"| D1["Data Node 1"]
    M -->|"diff 전파"| D2["Data Node 2"]
    M -->|"diff 전파"| D3["Data Node 3"]

    style M fill:#fff3e0
    style ClusterState fill:#e1f5fe
```

**Cluster State 전파 방식:**
- Master 노드만 Cluster State를 수정할 수 있음
- 변경 시 **diff(차분)만 전파**하여 네트워크 효율성 확보 (7.x+)
- 대규모 클러스터에서 인덱스/샤드가 많으면 Cluster State 크기 자체가 문제
- 경험적으로 Cluster State가 100MB를 초과하면 성능 이슈 발생

### 3.5 라우팅 상세 분석

#### 기본 라우팅

```mermaid
flowchart LR
    A["PUT /index/_doc/doc_123"] --> B["hash('doc_123')"]
    B --> C["murmur3 hash\n= 2087654321"]
    C --> D["2087654321 % 5\n(5 primary shards)"]
    D --> E["= shard 1"]
    E --> F["Shard 1이 있는\nData Node로 전달"]
```

#### 커스텀 라우팅

특정 필드 값으로 라우팅하면 관련 문서가 같은 샤드에 저장되어 검색 효율이 향상된다.

```json
PUT /orders/_doc/order_001?routing=user_123
{
  "user_id": "user_123",
  "product": "laptop",
  "amount": 1500
}
```

```
shard_num = hash("user_123") % num_primary_shards
```

같은 `user_id`의 모든 주문이 동일 샤드에 저장되므로, 해당 사용자의 주문 검색 시 단일 샤드만 조회하면 된다.

**주의**: 커스텀 라우팅 사용 시 데이터 편중(skew)이 발생할 수 있다. 특정 사용자의 데이터가 극단적으로 많은 경우 해당 샤드가 핫스팟이 된다.

## 4. 실전 예제

### 4.1 Allocation Awareness 설정 (AZ 분산)

```yaml
# elasticsearch.yml - Node 설정
node.attr.zone: zone-a  # 각 노드에 zone 속성 부여

# 클러스터 레벨 설정 (API)
```

```json
PUT /_cluster/settings
{
  "persistent": {
    "cluster.routing.allocation.awareness.attributes": "zone",
    "cluster.routing.allocation.awareness.force.zone.values": "zone-a,zone-b"
  }
}
```

이 설정으로 Primary 샤드가 `zone-a`에 있으면 Replica는 반드시 `zone-b`에 배치된다. `force` 설정은 특정 zone이 다운되어도 나머지 zone에 Replica를 추가 배치하지 않도록 한다 (한 zone에 모든 복제본이 몰리는 것 방지).

### 4.2 인덱스 레벨 샤드 필터링

Hot-Warm 아키텍처에서 오래된 인덱스를 Warm 노드로 이동:

```json
PUT /logs-2024-01/_settings
{
  "index.routing.allocation.require.data_tier": "warm",
  "index.routing.allocation.exclude._name": "hot-node-*"
}
```

**필터 연산자:**

| 연산자 | 의미 |
|--------|------|
| `require` | 해당 속성을 가진 노드에만 할당 (AND) |
| `include` | 해당 속성을 가진 노드 중 하나에 할당 (OR) |
| `exclude` | 해당 속성을 가진 노드에 할당하지 않음 |

### 4.3 클러스터 건강 상태 모니터링

```json
GET /_cluster/health
```

응답 예시:
```json
{
  "cluster_name": "production",
  "status": "yellow",
  "timed_out": false,
  "number_of_nodes": 5,
  "number_of_data_nodes": 3,
  "active_primary_shards": 150,
  "active_shards": 280,
  "relocating_shards": 2,
  "initializing_shards": 0,
  "unassigned_shards": 20,
  "delayed_unassigned_shards": 0,
  "number_of_pending_tasks": 0,
  "number_of_in_flight_fetch": 0,
  "task_max_waiting_in_queue_millis": 0,
  "active_shards_percent_as_number": 93.33
}
```

| 상태 | 의미 |
|------|------|
| **Green** | 모든 Primary + Replica 정상 할당 |
| **Yellow** | 모든 Primary 정상, 일부 Replica 미할당 |
| **Red** | 일부 Primary 미할당 → 데이터 손실 위험 |

### 4.4 미할당 샤드 원인 진단

```json
GET /_cluster/allocation/explain
{
  "index": "logs-2024-01",
  "shard": 0,
  "primary": false
}
```

응답에서 `allocate_explanation` 필드가 미할당 원인을 설명한다:
- `"the shard cannot be allocated to the same node"` → 단일 노드에서 Replica 할당 불가
- `"the node is above the high watermark"` → 디스크 공간 부족
- `"node does not match index setting"` → 노드 속성 필터 불일치

### 4.5 안전한 노드 제거 (Rolling Restart)

```json
// 1단계: 제거할 노드에서 샤드 배출
PUT /_cluster/settings
{
  "transient": {
    "cluster.routing.allocation.exclude._name": "data-node-3"
  }
}

// 2단계: 샤드 이동 완료 확인
GET /_cat/shards?v&h=index,shard,prirep,state,node&s=node

// 3단계: 모든 샤드 이동 완료 후 노드 중지
// (노드에 샤드가 0개인지 확인)

// 4단계: 노드 재시작 후 필터 해제
PUT /_cluster/settings
{
  "transient": {
    "cluster.routing.allocation.exclude._name": null
  }
}
```

### 4.6 Cluster State 크기 확인

```json
GET /_cluster/state?filter_path=metadata.indices.*.settings.index.number_of_shards

// 전체 Cluster State 크기 확인
GET /_cluster/stats?human&filter_path=indices.shards.total,indices.count
```

**경험적 가이드라인:**

| 항목 | 권장 범위 |
|------|-----------|
| 노드당 샤드 수 | 20 * heap(GB) 이하 (예: 30GB 힙 → 600개) |
| 샤드 크기 | 10GB ~ 50GB |
| 총 샤드 수 | 클러스터 전체에서 수만 개 이하 |
| Cluster State 크기 | 100MB 이하 |

## 보충: Logstash 파이프라인 아키텍처

Elasticsearch 클러스터로 데이터를 전송하는 Logstash 파이프라인의 내부 동작 원리를 이해하면 클러스터 운영에 도움이 된다. 이 섹션에서는 JavaPipeline의 실행 모델, Config에서 Bytecode까지의 컴파일 체인, WorkerLoop 동작 원리를 소스코드 수준에서 분석한다.

### JavaPipeline

Logstash 파이프라인의 핵심 구현체는 `JavaPipeline` 클래스다. Ruby의 `AbstractPipeline`을 상속하며 Java와 Ruby 하이브리드 구조로 동작한다. 하나의 파이프라인은 다음 요소로 구성된다:

- **Input 플러그인**: 데이터 소스에서 이벤트를 수집
- **Queue**: Input과 Worker 사이의 버퍼 (Memory 또는 Persistent Queue)
- **Worker Thread**: Filter + Output 로직을 실행하는 스레드 (pipeline.workers 설정)
- **Filter 플러그인**: 이벤트를 변환, 보강, 필터링
- **Output 플러그인**: 처리된 이벤트를 목적지로 전송

### Config 컴파일 체인

Logstash 설정 파일은 다음 단계를 거쳐 실행 가능한 코드로 변환된다:

```
Config Text -> AST -> PipelineIR (Graph) -> Dataset (Compiled Bytecode)
```

### 핵심 설정 파라미터

| 파라미터 | 기본값 | 설명 |
|----------|--------|------|
| `pipeline.workers` | CPU 코어 수 | Filter/Output 실행 Worker 스레드 수 |
| `pipeline.batch.size` | 125 | Worker당 한 번에 처리하는 이벤트 수 |
| `pipeline.batch.delay` | 50ms | 배치가 미달일 때 대기 시간 |
| `pipeline.ordered` | auto | 이벤트 순서 보장 여부 |
| `queue.type` | memory | 큐 유형 (memory / persisted) |

### 전체 아키텍처

```mermaid
graph LR
    subgraph "Input Threads"
        I1["Input 1<br/>(beats)"]
        I2["Input 2<br/>(kafka)"]
    end

    subgraph "Queue"
        Q["Memory Queue<br/>/ Persistent Queue"]
    end

    subgraph "Worker Threads"
        W1["Worker 0<br/>Filter -> Output"]
        W2["Worker 1<br/>Filter -> Output"]
        W3["Worker N<br/>Filter -> Output"]
    end

    I1 -->|push events| Q
    I2 -->|push events| Q
    Q -->|read_batch| W1
    Q -->|read_batch| W2
    Q -->|read_batch| W3
```

### JavaPipeline 초기화

```ruby
class JavaPipeline < AbstractPipeline
  def initialize(pipeline_config, namespaced_metric = nil, agent = nil)
    super pipeline_config, namespaced_metric, @logger, agent
    finish_initialization
  end

  def finish_initialization
    open_queue                    # Queue 초기화

    @worker_threads = []
    @worker_observer = org.logstash.execution.WorkerObserver.new(
      process_events_namespace_metric,
      pipeline_events_namespace_metric
    )

    @drain_queue = settings.get_value("queue.drain") ||
                   settings.get("queue.type") == MEMORY

    @events_filtered = java.util.concurrent.atomic.LongAdder.new
    @events_consumed = java.util.concurrent.atomic.LongAdder.new

    @ready = Concurrent::AtomicBoolean.new(false)
    @running = Concurrent::AtomicBoolean.new(false)
    @flushing = java.util.concurrent.atomic.AtomicBoolean.new(false)
    @shutdownRequested = java.util.concurrent.atomic.AtomicBoolean.new(false)
    @crash_detected = Concurrent::AtomicBoolean.new(false)
  end
end
```

### 파이프라인 시작 시퀀스

```mermaid
sequenceDiagram
    participant Main as Pipeline Thread
    participant WI as Worker Init
    participant WL as Worker Loop
    participant IT as Input Threads

    Main->>Main: start()
    Main->>Main: collect_stats / initialize_flow_metrics
    Main->>Main: run()
    Main->>WI: start_workers()
    WI->>WI: maybe_setup_out_plugins()
    WI->>WI: safe_pipeline_worker_count()
    loop N workers
        WI->>WI: Thread.new { init_worker_loop }
    end
    WI->>WL: worker_loop.run() (N threads)
    WI->>IT: start_inputs()
    Main->>Main: transition_to_running
    Main->>Main: start_flusher
    Main->>Main: monitor_inputs_and_workers
```

### Thread-safety 자동 감지

unsafe한 Filter 플러그인이 있으면 자동으로 Worker 수를 1로 제한한다:

```ruby
def safe_pipeline_worker_count
  pipeline_workers = settings.get("pipeline.workers")
  safe_filters, unsafe_filters = filters.partition(&:threadsafe?)

  return pipeline_workers if unsafe_filters.empty?

  if settings.set?("pipeline.workers")
    if pipeline_workers > 1
      @logger.warn("Warning: filters that might not work with multiple workers",
                   :worker_threads => pipeline_workers,
                   :filters => unsafe_filters.collect(&:config_name))
    end
  else
    if default > 1
      return 1  # 자동으로 1로 제한
    end
  end
  pipeline_workers
end
```

### PipelineIR - 중간 표현(Intermediate Representation)

`PipelineIR`은 파이프라인 설정을 Graph 기반 중간 표현으로 변환한다:

```java
public PipelineIR(Graph inputSection, Graph filterSection,
                  Graph outputSection, String originalSource) throws InvalidIRException {
    Graph tempGraph = inputSection.copy();        // Input 섹션 복사

    QueueVertex tempQueue = new QueueVertex();
    tempGraph = tempGraph.chain(tempQueue);        // Input -> Queue 연결

    tempGraph = tempGraph.chain(filterSection);    // Queue -> Filter 연결

    tempGraph = tempGraph.chain(                   // Filter -> Output 분리자
        new SeparatorVertex("filter_to_output")
    );

    this.graph = tempGraph.chain(outputSection);   // Filter -> Output 연결

    this.graph.validate();                         // 그래프 유효성 검증

    // 원본 소스 기반 해시 또는 그래프 구조 기반 해시
    if (this.getOriginalSource() != null) {
        uniqueHash = Util.digest(this.getOriginalSource());
    } else {
        uniqueHash = this.graph.uniqueHash();
    }
}
```

Graph 구조:

```
Input1 --> Queue --> Filter1 --> Filter2 --> [filter_to_output] --> Output1
Input2 ----^                                                   --> Output2
```

### DatasetCompiler - 런타임 코드 생성

`DatasetCompiler`는 PipelineIR의 Graph를 런타임에 Java 바이트코드로 컴파일한다. Filter/Output 플러그인마다 최적화된 `Dataset` 구현을 동적 생성한다:

```java
// Filter Dataset 컴파일
public static ComputeStepSyntaxElement<Dataset> filterDataset(
    final Collection<Dataset> parents,
    final AbstractFilterDelegatorExt plugin)
{
    final ClassFields fields = new ClassFields();
    final ValueSyntaxElement outputBuffer = fields.add("outputBuffer", new ArrayList<>());

    if (parents.isEmpty()) {
        compute = filterBody(outputBuffer, BATCH_ARG, fields, plugin);
    } else {
        // 부모 Dataset에서 입력 버퍼링
        final Collection<ValueSyntaxElement> parentFields = createParentStatementsFields(parents, fields);
        compute = withInputBuffering(
            filterBody(outputBuffer, inputBufferField, fields, plugin),
            parentFields, inputBufferField
        );
    }
    return prepare(withOutputBuffering(compute, clear, outputBuffer, fields));
}
```

### 파이프라인 종료 시퀀스

```mermaid
sequenceDiagram
    participant M as Monitor Thread
    participant I as Input Threads
    participant W as Worker Threads

    M->>M: monitor_inputs_and_workers()
    Note over M: ThreadsWait로 스레드 감시

    alt 정상 종료 (Input 완료)
        I-->>M: Input thread terminated
        M->>M: All inputs done
    else 비정상 종료 (Worker 크래시)
        W-->>M: Worker thread terminated
        M->>I: stop_inputs()
        M->>M: wait_input_threads_termination(10s)
    end

    M->>M: shutdown_flusher
    M->>M: shutdown_workers
    M->>M: close
```

### 프로덕션 파이프라인 설정 예제

```ruby
# /etc/logstash/pipelines.yml
- pipeline.id: main-pipeline
  pipeline.workers: 4
  pipeline.batch.size: 250
  pipeline.batch.delay: 50
  queue.type: persisted
  queue.max_bytes: 4gb
  path.config: "/etc/logstash/conf.d/main.conf"

- pipeline.id: dead-letter-pipeline
  pipeline.workers: 1
  pipeline.batch.size: 50
  path.config: "/etc/logstash/conf.d/dlq.conf"
```

### Worker 수 최적화 진단

```bash
# 파이프라인 성능 메트릭 확인
curl -s localhost:9600/_node/stats/pipelines | jq '
  .pipelines["main-pipeline"] | {
    workers: .pipeline.workers,
    batch_size: .pipeline.batch_size,
    events_in: .events.in,
    events_out: .events.out,
    events_filtered: .events.filtered,
    queue_backpressure_ms: .events.queue_push_duration_in_millis,
    worker_utilization: .pipeline.worker_utilization
  }'
```

Worker 수 결정 기준:
- `queue_push_duration` 높음 -> Worker가 부족하거나 Output 병목
- `worker_utilization` 낮음 -> Worker 수 과다 (줄여도 됨)
- CPU 사용률 포화 -> grok 등 CPU-bound Filter가 병목

## 5. 정리

| 구분 | 핵심 내용 |
|------|-----------|
| **라우팅 공식** | `shard = hash(_routing) % num_primary_shards` — Primary 수 변경 불가의 근본 원인 |
| **샤드 할당** | AllocationService의 Decider 체인이 순차적으로 배치 가능 여부 판단 |
| **Allocation Awareness** | zone/rack 속성으로 Primary-Replica를 다른 가용 영역에 분산 |
| **Disk Watermark** | 85%(low) → 90%(high) → 95%(flood) 단계별 할당/인덱싱 제한 |
| **Rebalancing** | 노드 간 샤드 수 균형을 자동으로 맞춤. 동시 이동 수 제한으로 부하 조절 |
| **Split Brain 방지** | Quorum 기반 선출 (7.x+). Master-eligible 노드 홀수 개 권장 |
| **Cluster State** | Master만 수정, diff 전파. 인덱스/샤드 수 폭발 시 성능 병목 |
| **노드 제거** | exclude 필터로 샤드 배출 후 안전하게 중지 |
| **JavaPipeline** | Logstash 파이프라인의 핵심 실행 엔진. Ruby/Java 하이브리드 구조 |
| **PipelineIR** | Config를 Graph 기반 중간 표현으로 변환. Input -> Queue -> Filter -> Output 체인 |
| **DatasetCompiler** | PipelineIR Graph를 런타임 Java 바이트코드로 컴파일. 플러그인별 최적화 |
| **Thread-safety** | unsafe Filter 감지 시 Worker 수 자동 1로 제한 |

---
*참고: Elasticsearch 8.x 기준*

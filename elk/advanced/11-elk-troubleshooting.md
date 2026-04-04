# ELK 트러블슈팅 가이드

Elasticsearch 클러스터 운영 중 발생하는 주요 장애 상황(Red/Yellow 상태, 느린 쿼리, OOM, 디스크 풀)의 진단 방법과 복구 절차를 단계별로 정리한다. Logstash 파이프라인 장애 대응까지 포함한다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### 클러스터 헬스 상태

Elasticsearch 클러스터는 세 가지 상태를 가진다:

| 상태 | 의미 | 긴급도 |
|------|------|--------|
| **Green** | 모든 Primary + Replica 샤드 정상 할당 | 정상 |
| **Yellow** | 모든 Primary 정상, 일부 Replica 미할당 | 주의 (데이터 유실 위험 없음) |
| **Red** | 일부 Primary 샤드 미할당 | 긴급 (데이터 유실 가능) |

### 주요 장애 유형

```mermaid
graph TB
    ISSUE[ELK 장애 유형]
    
    ISSUE --> CLUSTER[클러스터 상태 이상]
    ISSUE --> PERF[성능 저하]
    ISSUE --> RESOURCE[리소스 고갈]
    ISSUE --> PIPELINE[파이프라인 장애]
    
    CLUSTER --> RED[Red Status<br/>Primary 샤드 유실]
    CLUSTER --> YELLOW[Yellow Status<br/>Replica 미할당]
    
    PERF --> SLOW_Q[느린 쿼리]
    PERF --> SLOW_I[느린 인덱싱]
    
    RESOURCE --> OOM[Out of Memory]
    RESOURCE --> DISK[Disk Full]
    RESOURCE --> CPU[CPU 100%]
    
    PIPELINE --> LS_BACK[Logstash Backpressure]
    PIPELINE --> LS_ERR[파싱/출력 에러]
```

---

## 2. 왜 알아야 하는가 (Why)

### 장애 대응 시간이 비즈니스에 미치는 영향

- **Red 클러스터**: 검색/인덱싱 일부 불가 → 서비스 장애로 직결
- **OOM Kill**: Elasticsearch 프로세스 사망 → 클러스터 불안정, 연쇄 장애
- **Disk Full**: 인덱스 read-only 전환 → 로그 유입 중단, 모니터링 블라인드 스팟
- **느린 쿼리**: Kibana 대시보드 타임아웃 → 장애 감지/분석 불가

### 사전에 알아야 하는 이유

1. 장애 발생 시 **패닉 상태에서 검색하면 늦다** — 사전에 진단/복구 프로세스를 숙지해야 함
2. 잘못된 복구 시도가 **상황을 악화**시킬 수 있음 (예: Red 상태에서 무작정 노드 재시작)
3. 대부분의 장애는 **예측 가능한 패턴**을 따르므로 체계적 접근이 가능

---

## 3. 내부 구현 분석 (How)

### 3.1 클러스터 상태 진단 흐름

```mermaid
flowchart TD
    START[장애 감지] --> HEALTH["GET _cluster/health"]
    
    HEALTH --> |Green| PERF_CHECK[성능 문제 확인]
    HEALTH --> |Yellow| YELLOW_DIAG[Replica 미할당 진단]
    HEALTH --> |Red| RED_DIAG[Primary 미할당 긴급 진단]
    
    YELLOW_DIAG --> ALLOC["GET _cluster/allocation/explain"]
    ALLOC --> ALLOC_FIX{원인}
    ALLOC_FIX --> |노드 부족| ADD_NODE[노드 추가 또는<br/>replica 수 조정]
    ALLOC_FIX --> |디스크 부족| FREE_DISK[디스크 확보]
    ALLOC_FIX --> |인식 규칙| FIX_SETTING[allocation 설정 수정]
    
    RED_DIAG --> UNASSIGNED["GET _cat/shards?h=index,shard,prirep,state,unassigned.reason"]
    UNASSIGNED --> RED_FIX{원인}
    RED_FIX --> |NODE_LEFT| WAIT_OR_REROUTE[노드 복구 대기 또는<br/>reroute 실행]
    RED_FIX --> |ALLOCATION_FAILED| RETRY_ALLOC[할당 재시도]
    RED_FIX --> |INDEX_CREATED| CHECK_CONFIG[인덱스 설정 확인]
    
    PERF_CHECK --> SLOW_LOG[Slow Log 확인]
    PERF_CHECK --> HOT_THREADS["GET _nodes/hot_threads"]
    PERF_CHECK --> TASK_LIST["GET _tasks?actions=*search*"]
```

### 3.2 Elasticsearch 메모리 구조와 OOM

```mermaid
graph TB
    subgraph "JVM Process Memory"
        subgraph "Heap (ES_JAVA_OPTS: -Xms/-Xmx)"
            direction TB
            YOUNG[Young Generation<br/>단기 객체]
            OLD[Old Generation<br/>장기 객체, 캐시]
            
            subgraph "주요 힙 소비자"
                CACHE1[Field Data Cache]
                CACHE2[Node Query Cache]
                CACHE3[Shard Request Cache]
                AGG[Aggregation Buffers]
                BULK[Bulk Indexing Buffer]
            end
        end
        
        subgraph "Off-Heap"
            LUCENE[Lucene Segments<br/>MMap'd Files]
            NETTY[Netty Buffers]
            DIRECT[Direct Buffers]
        end
    end
    
    subgraph "OS Memory"
        FILESYSTEM[Filesystem Cache<br/>Lucene가 활용]
    end
```

**OOM 발생 메커니즘**:
1. 거대한 Aggregation 쿼리 → Heap 폭증
2. Field Data가 text 필드에 로드 → 힙 점유
3. 너무 많은 Bulk 요청 동시 처리 → Indexing Buffer 폭증
4. 많은 샤드 수 → 샤드당 오버헤드 누적 (샤드 1개당 ~10MB 힙)

### 3.3 디스크 워터마크 시스템

Elasticsearch는 디스크 사용량에 따라 3단계 워터마크를 적용한다:

```
디스크 사용량 증가 방향 →

[0%]────[85%]────[90%]────[95%]────[100%]
         │        │        │
         ▼        ▼        ▼
    Low Water   High     Flood
    Mark       Water     Stage
                Mark
                
Low (85%):   새 샤드 할당 중지
High (90%):  기존 샤드를 다른 노드로 이동 시작
Flood (95%): 모든 인덱스를 read-only로 전환
```

### 3.4 Logstash 파이프라인 내부 구조

```mermaid
graph LR
    subgraph "Logstash Pipeline"
        INPUT[Input<br/>Beats/Kafka/File] --> QUEUE[Persistent Queue<br/>또는 In-Memory Queue]
        QUEUE --> FILTER1[Filter Worker 1<br/>Grok, Mutate, ...]
        QUEUE --> FILTER2[Filter Worker 2]
        QUEUE --> FILTER3[Filter Worker N]
        FILTER1 --> OUTPUT[Output<br/>Elasticsearch]
        FILTER2 --> OUTPUT
        FILTER3 --> OUTPUT
    end
    
    OUTPUT -->|Backpressure| QUEUE
    QUEUE -->|Queue Full| INPUT
    INPUT -->|TCP Backpressure| BEAT[Beats Agent]
```

**Backpressure 전파 경로**:
1. Elasticsearch가 느려짐 (429 Too Many Requests)
2. Output이 블록됨 → Queue에 이벤트 적체
3. Queue가 가득 참 → Input이 블록됨
4. Beats에게 TCP Backpressure → Beats가 자체 큐에 보관

---

## 4. 실전 예제

### 4.1 Red 클러스터 복구 절차

```bash
# 1단계: 클러스터 상태 확인
GET _cluster/health?pretty

# 2단계: 미할당 샤드 확인
GET _cat/shards?v&h=index,shard,prirep,state,unassigned.reason&s=state:desc

# 3단계: 미할당 원인 상세 분석
GET _cluster/allocation/explain
{
  "index": "logs-nginx-production",
  "shard": 0,
  "primary": true
}

# 4단계-A: 노드 복구 대기 후 reroute (노드가 살아났을 때)
POST _cluster/reroute?retry_failed=true

# 4단계-B: 데이터 유실 감수하고 빈 Primary 할당 (최후 수단)
POST _cluster/reroute
{
  "commands": [
    {
      "allocate_empty_primary": {
        "index": "logs-nginx-production",
        "shard": 0,
        "node": "data-node-1",
        "accept_data_loss": true
      }
    }
  ]
}

# 4단계-C: 스냅샷에서 복원 (권장)
POST _snapshot/s3-backup-repo/latest-snapshot/_restore
{
  "indices": "logs-nginx-production",
  "rename_pattern": "(.+)",
  "rename_replacement": "restored-$1"
}
```

### 4.2 Yellow 클러스터 진단 및 해결

```bash
# 미할당 Replica 확인
GET _cat/shards?v&h=index,shard,prirep,state,unassigned.reason&s=state

# 흔한 원인 1: 단일 노드 클러스터 → Replica 할당 불가
PUT logs-nginx-production/_settings
{
  "index.number_of_replicas": 0
}

# 흔한 원인 2: 디스크 사용량 > 85% (Low Watermark)
GET _cat/allocation?v&h=node,disk.percent,disk.used,disk.avail,shards

# 워터마크 임시 조정 (긴급 시)
PUT _cluster/settings
{
  "transient": {
    "cluster.routing.allocation.disk.watermark.low": "90%",
    "cluster.routing.allocation.disk.watermark.high": "95%",
    "cluster.routing.allocation.disk.watermark.flood_stage": "97%"
  }
}

# 흔한 원인 3: Allocation Awareness로 인한 미할당
GET _cluster/settings?include_defaults&filter_path=*.cluster.routing.allocation*
```

### 4.3 느린 쿼리 분석

```bash
# Slow Log 활성화
PUT logs-nginx-production/_settings
{
  "index.search.slowlog.threshold.query.warn": "5s",
  "index.search.slowlog.threshold.query.info": "2s",
  "index.search.slowlog.threshold.query.debug": "1s",
  "index.search.slowlog.threshold.fetch.warn": "1s",
  "index.indexing.slowlog.threshold.index.warn": "10s",
  "index.indexing.slowlog.threshold.index.info": "5s"
}

# Profile API로 쿼리 상세 분석
GET logs-nginx-production/_search
{
  "profile": true,
  "query": {
    "bool": {
      "must": [
        { "match": { "message": "error timeout" }},
        { "range": { "@timestamp": { "gte": "now-1h" }}}
      ]
    }
  }
}

# Hot Threads로 CPU 병목 확인
GET _nodes/hot_threads?threads=3&interval=500ms

# 실행 중인 태스크 확인 (장시간 실행 쿼리)
GET _tasks?actions=*search*&detailed&group_by=parents

# 문제 태스크 취소
POST _tasks/{task_id}/_cancel
```

### 4.4 OOM 대응

```bash
# 1. 현재 JVM 힙 사용량 확인
GET _nodes/stats/jvm?filter_path=nodes.*.jvm.mem

# 2. 힙 사용량이 높은 원인 분석
# Circuit Breaker 상태 확인
GET _nodes/stats/breaker

# 3. Field Data 캐시 확인 (text 필드 aggregation 시 폭증)
GET _nodes/stats/indices/fielddata?fields=*

# 4. 즉각 조치: Field Data 캐시 클리어
POST _cache/clear?fielddata=true

# 5. Circuit Breaker 설정 강화
PUT _cluster/settings
{
  "persistent": {
    "indices.breaker.total.limit": "70%",
    "indices.breaker.fielddata.limit": "40%",
    "indices.breaker.request.limit": "40%",
    "network.breaker.inflight_requests.limit": "100%"
  }
}

# 6. 과도한 Aggregation 방지 — 버킷 수 제한
PUT _cluster/settings
{
  "persistent": {
    "search.max_buckets": 10000
  }
}

# 7. text 필드에 fielddata 사용 금지 (keyword 서브필드 사용 유도)
# 잘못된 예: "message" (text) 필드로 aggregation
# 올바른 예: "message.keyword" (keyword) 필드로 aggregation
```

### 4.5 디스크 풀 (Disk Full) 긴급 대응

```bash
# 1. 디스크 상태 확인
GET _cat/allocation?v&h=node,disk.percent,disk.used,disk.avail,shards

# 2. 가장 큰 인덱스 확인
GET _cat/indices?v&h=index,store.size,pri.store.size&s=store.size:desc&format=json

# 3. Flood Stage read-only 해제 (디스크 확보 후)
PUT _all/_settings
{
  "index.blocks.read_only_allow_delete": null
}

# 4. 오래된 인덱스 즉시 삭제
DELETE logs-nginx-production-2023.10.*

# 5. Force Merge로 삭제된 문서 정리 (디스크 회복)
POST logs-nginx-production-2024.01/_forcemerge?max_num_segments=1

# 6. 인덱스 Shrink로 샤드 수 줄이기
# 먼저 인덱스를 하나의 노드로 이동
PUT logs-old-index/_settings
{
  "index.routing.allocation.require._name": "data-node-1",
  "index.blocks.write": true
}

# Shrink 실행
POST logs-old-index/_shrink/logs-old-index-shrunk
{
  "settings": {
    "index.number_of_replicas": 1,
    "index.number_of_shards": 1,
    "index.codec": "best_compression"
  }
}
```

### 4.6 Logstash 파이프라인 장애 대응

```bash
# Logstash 파이프라인 상태 확인
curl -s localhost:9600/_node/stats/pipelines?pretty

# 주요 지표 해석:
# - events.out < events.in → 파이프라인 병목
# - events.filtered 증가 → 필터에서 드롭
# - queue.events 증가 → 백프레셔 발생 중

# Logstash Slow Log 확인
# logstash.yml 에서 설정:
# slowlog.threshold.warn: 2s
# slowlog.threshold.info: 1s
# slowlog.threshold.debug: 500ms

# Pipeline Worker 수 조정
# logstash.yml
# pipeline.workers: 8          # CPU 코어 수에 맞춤
# pipeline.batch.size: 500     # 배치 크기 (기본 125)
# pipeline.batch.delay: 50     # 배치 대기 시간 ms

# Dead Letter Queue 활성화 (파싱 실패 이벤트 보존)
# logstash.yml
# dead_letter_queue.enable: true
# dead_letter_queue.max_bytes: 4096mb
```

```ruby
# Logstash 에러 핸들링 파이프라인 예시
input {
  beats {
    port => 5044
  }
}

filter {
  # Grok 파싱 실패 처리
  grok {
    match => { "message" => "%{COMBINEDAPACHELOG}" }
    tag_on_failure => ["_grokparsefailure"]
  }

  # 파싱 실패한 이벤트를 별도 인덱스로 라우팅
  if "_grokparsefailure" in [tags] {
    mutate {
      add_field => { "[@metadata][target_index]" => "parse-failures" }
    }
  } else {
    mutate {
      add_field => { "[@metadata][target_index]" => "logs-parsed" }
    }
  }
}

output {
  elasticsearch {
    hosts => ["https://es01:9200"]
    index => "%{[@metadata][target_index]}-%{+YYYY.MM.dd}"
    user => "elastic"
    password => "${ES_PASSWORD}"
    ssl_certificate_authorities => ["/etc/logstash/certs/ca.crt"]

    # 재시도 설정
    retry_on_conflict => 3

    # Backpressure 대응
    action => "create"
  }
}
```

### 4.7 종합 진단 스크립트

```bash
#!/bin/bash
# elk-health-check.sh — ELK 클러스터 종합 진단 스크립트

ES_HOST="${ES_HOST:-https://localhost:9200}"
ES_USER="${ES_USER:-elastic}"
ES_PASS="${ES_PASS:-changeme}"
CURL="curl -sk -u ${ES_USER}:${ES_PASS}"

echo "========== ELK Cluster Health Check =========="
echo "Time: $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
echo ""

# 1. 클러스터 헬스
echo "--- Cluster Health ---"
${CURL} "${ES_HOST}/_cluster/health?pretty"
echo ""

# 2. 노드 상태
echo "--- Node Status ---"
${CURL} "${ES_HOST}/_cat/nodes?v&h=name,ip,heap.percent,ram.percent,cpu,load_1m,disk.used_percent,node.role"
echo ""

# 3. 미할당 샤드
echo "--- Unassigned Shards ---"
UNASSIGNED=$(${CURL} -s "${ES_HOST}/_cluster/health" | python3 -c "import sys,json; print(json.load(sys.stdin)['unassigned_shards'])")
if [ "$UNASSIGNED" -gt 0 ]; then
    echo "WARNING: ${UNASSIGNED} unassigned shards detected!"
    ${CURL} "${ES_HOST}/_cat/shards?v&h=index,shard,prirep,state,unassigned.reason&s=state:desc" | head -20
else
    echo "OK: No unassigned shards"
fi
echo ""

# 4. 디스크 사용량
echo "--- Disk Usage ---"
${CURL} "${ES_HOST}/_cat/allocation?v&h=node,disk.percent,disk.used,disk.avail,shards"
echo ""

# 5. JVM 힙 사용량
echo "--- JVM Heap Usage ---"
${CURL} -s "${ES_HOST}/_nodes/stats/jvm" | python3 -c "
import sys, json
data = json.load(sys.stdin)
for node_id, node in data['nodes'].items():
    name = node['name']
    heap_pct = node['jvm']['mem']['heap_used_percent']
    status = 'CRITICAL' if heap_pct > 85 else 'WARNING' if heap_pct > 75 else 'OK'
    print(f'  {name}: {heap_pct}% heap used [{status}]')
"
echo ""

# 6. 큰 인덱스 Top 10
echo "--- Largest Indices (Top 10) ---"
${CURL} "${ES_HOST}/_cat/indices?v&h=index,docs.count,store.size&s=store.size:desc&format=text" | head -11
echo ""

echo "========== Check Complete =========="
```

---

## 5. 정리

| 장애 유형 | 진단 명령 | 핵심 원인 | 즉각 대응 |
|-----------|----------|----------|----------|
| **Red 클러스터** | `_cluster/allocation/explain` | 노드 다운, 디스크 풀, 인덱스 손상 | `_cluster/reroute?retry_failed` 또는 스냅샷 복원 |
| **Yellow 클러스터** | `_cat/shards?s=state` | Replica 할당 불가 (노드/디스크) | Replica 수 조정 또는 노드 추가 |
| **느린 쿼리** | Slow Log + Profile API | 비효율 쿼리, 큰 Aggregation | 쿼리 최적화, 취소 |
| **OOM** | `_nodes/stats/jvm` + Breaker | Field Data, 대형 Aggregation | 캐시 클리어, Breaker 강화 |
| **디스크 풀** | `_cat/allocation` | 로그 보관 미설정 | 인덱스 삭제, ILM 적용 |
| **Logstash 병목** | `:9600/_node/stats/pipelines` | Worker 부족, ES 느림 | Worker 수 증가, Batch 크기 조정 |

### 장애 대응 원칙

1. **진단 먼저, 행동은 그 다음** — 원인 파악 없이 노드 재시작하면 상황 악화
2. **`allocate_empty_primary`는 최후 수단** — 데이터 유실이 발생하므로 스냅샷 복원 먼저 시도
3. **워터마크 임시 조정은 반드시 원복** — 긴급 대응 후 근본 원인(디스크 확보, ILM)을 해결
4. **Circuit Breaker를 신뢰** — OOM이 발생했다면 Breaker 설정이 너무 느슨한 것
5. **정기 진단 스크립트를 cron에 등록** — 장애는 예방이 최선

---
*참고: Elasticsearch 8.x / Logstash 8.x / Kibana 8.x 기준*

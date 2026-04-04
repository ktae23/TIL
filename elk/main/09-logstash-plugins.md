# Logstash 플러그인 시스템

Logstash의 핵심 강점은 200개 이상의 플러그인으로 구성된 확장 가능한 파이프라인 아키텍처에 있다. 이 문서에서는 플러그인 유형별 구조, Grok 패턴 문법, 주요 Filter 플러그인, 그리고 커스텀 플러그인 개발 방법을 다룬다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### 플러그인 4대 카테고리

Logstash 파이프라인은 4가지 유형의 플러그인으로 구성된다.

```mermaid
graph LR
    subgraph Pipeline
        direction LR
        I[Input Plugin] --> C1[Codec]
        C1 --> F[Filter Plugin]
        F --> C2[Codec]
        C2 --> O[Output Plugin]
    end

    style I fill:#4CAF50,color:#fff
    style F fill:#FF9800,color:#fff
    style O fill:#2196F3,color:#fff
    style C1 fill:#9C27B0,color:#fff
    style C2 fill:#9C27B0,color:#fff
```

| 카테고리 | 역할 | 대표 플러그인 |
|---------|------|-------------|
| **Input** | 데이터 소스에서 이벤트 수집 | `beats`, `kafka`, `file`, `jdbc`, `syslog`, `http` |
| **Filter** | 이벤트 변환/가공/강화 | `grok`, `mutate`, `date`, `geoip`, `dissect`, `ruby` |
| **Output** | 가공된 이벤트를 목적지로 전송 | `elasticsearch`, `kafka`, `file`, `stdout`, `s3` |
| **Codec** | Input/Output의 직렬화/역직렬화 | `json`, `plain`, `multiline`, `rubydebug` |

### 플러그인 생명주기

모든 Logstash 플러그인은 공통 생명주기를 따른다:

1. **register** - 초기화 (연결 설정, 리소스 할당)
2. **run** (Input) / **filter** (Filter) / **encode/decode** (Codec) - 핵심 처리
3. **close** - 정리 (연결 해제, 리소스 반환)

각 플러그인은 `logstash-core`의 `LogStash::Plugin` 기본 클래스를 상속하며, JRuby 기반으로 동작한다.

---

## 2. 왜 알아야 하는가 (Why)

### 실무 동기

- **비정형 로그 파싱**: 애플리케이션마다 로그 형식이 다르다. Grok/Dissect 없이는 Elasticsearch에서 의미 있는 필드 검색이 불가능하다.
- **데이터 품질 제어**: Filter 플러그인 조합으로 누락 필드 보정, 타임스탬프 정규화, 민감정보 마스킹을 파이프라인 레벨에서 처리할 수 있다.
- **확장성**: 내장 플러그인으로 해결되지 않는 경우 커스텀 플러그인을 개발하여 기존 파이프라인에 끊김 없이 통합할 수 있다.
- **성능 최적화**: Grok과 Dissect의 성능 차이를 이해하면 파이프라인 처리량을 10배 이상 개선할 수 있다.

### 플러그인 선택이 파이프라인 성능에 미치는 영향

```
[Grok 정규식 파싱]    ~2,000 events/sec
[Dissect 토큰 파싱]   ~20,000 events/sec  (10x 빠름)
[Ruby Filter 커스텀]  ~5,000 events/sec   (유연하지만 중간 성능)
```

동일한 로그를 파싱하더라도 플러그인 선택에 따라 처리량이 크게 달라진다.

---

## 3. 내부 구현 분석 (How)

### 3.1 Grok 패턴 시스템

Grok은 Logstash에서 가장 많이 사용되는 Filter 플러그인이다. 내부적으로 Oniguruma 정규식 엔진을 사용하며, 명명된 캡처 그룹을 재사용 가능한 패턴으로 추상화한다.

#### Grok 패턴 문법

```
%{PATTERN_NAME:field_name:data_type}
```

- `PATTERN_NAME`: 미리 정의된 또는 커스텀 패턴 이름
- `field_name`: 추출된 값이 저장될 필드명
- `data_type`: (선택) `int` 또는 `float`로 타입 변환

#### 내장 패턴 구조

Logstash는 `vendor/bundle/jruby/*/gems/logstash-patterns-core-*/patterns/` 경로에 약 120개의 기본 패턴을 제공한다.

```
# grok-patterns (기본 패턴 파일)
USERNAME [a-zA-Z0-9._-]+
USER %{USERNAME}
EMAILLOCALPART [a-zA-Z][a-zA-Z0-9_.+-=:]+
EMAILADDRESS %{EMAILLOCALPART}@%{HOSTNAME}
INT (?:[+-]?(?:[0-9]+))
BASE10NUM (?:[+-]?(?:[0-9]+(?:\.[0-9]+)?))
NUMBER (?:%{BASE10NUM})
BASE16NUM (?:0[xX]?[0-9a-fA-F]+)
WORD \b\w+\b
NOTSPACE \S+
SPACE \s*
DATA .*?
GREEDYDATA .*
IP (?:%{IPV6}|%{IPV4})
HTTPDATE %{MONTHDAY}/%{MONTH}/%{YEAR}:%{TIME} %{INT}
```

#### Grok 패턴 조합 원리

```mermaid
graph TD
    A["%{COMBINEDAPACHELOG}"] --> B["%{COMMONAPACHELOG}"]
    A --> C["추가 필드 (referrer, agent)"]
    B --> D["%{IPORHOST:clientip}"]
    B --> E["%{USER:ident}"]
    B --> F["%{USER:auth}"]
    B --> G["%{HTTPDATE:timestamp}"]
    B --> H["request, response, bytes"]

    style A fill:#FF9800,color:#fff
    style B fill:#4CAF50,color:#fff
```

패턴은 재귀적으로 확장된다. `%{COMBINEDAPACHELOG}`는 내부적으로 수십 개의 기본 패턴으로 풀어진다.

#### 커스텀 패턴 작성

```bash
# /etc/logstash/patterns/custom_patterns
CUSTOM_TIMESTAMP %{YEAR}-%{MONTHNUM}-%{MONTHDAY}[T ]%{HOUR}:?%{MINUTE}(?::?%{SECOND})?%{ISO8601_TIMEZONE}?
SERVICE_NAME [a-zA-Z][a-zA-Z0-9_-]{2,32}
CUSTOM_LOGLEVEL (?:DEBUG|INFO|WARN|ERROR|FATAL|TRACE)
CUSTOM_LOG %{CUSTOM_TIMESTAMP:timestamp} \[%{CUSTOM_LOGLEVEL:level}\] %{SERVICE_NAME:service} - %{GREEDYDATA:message}
```

### 3.2 주요 Filter 플러그인

#### mutate - 필드 조작의 스위스 아미 나이프

`mutate`는 필드 이름 변경, 타입 변환, 문자열 치환 등 범용 변환을 수행한다. 내부 처리 순서가 정해져 있다:

```
coerce → rename → update → replace → convert → gsub → 
uppercase → capitalize → lowercase → strip → remove → split → join → merge → copy
```

이 순서를 이해해야 의도하지 않은 결과를 방지할 수 있다.

#### date - 타임스탬프 정규화

`date` 필터는 문자열 타임스탬프를 `@timestamp` 필드로 파싱한다. Elasticsearch에서 시계열 분석의 기준점이 되므로, 파이프라인에서 가장 중요한 필터 중 하나다.

#### geoip - IP 주소 지리 정보 매핑

MaxMind GeoLite2 데이터베이스를 사용하여 IP 주소를 위도/경도, 국가, 도시 정보로 변환한다. Kibana Maps 시각화의 기반 데이터를 생성한다.

#### dissect - 고성능 구조적 파싱

Grok과 달리 정규식을 사용하지 않고 구분자 기반 토큰 분리를 수행한다. 구조가 일정한 로그에는 Grok 대비 10배 이상 빠르다.

```
# dissect 문법
%{field_name} - 기본 필드 추출
%{field_name->} - 오른쪽 패딩(연속 구분자) 무시
%{+field_name} - 기존 필드에 값 추가 (append)
%{?skip_field} - 추출하되 이벤트에 포함하지 않음
```

### 3.3 플러그인 내부 아키텍처

```mermaid
graph TB
    subgraph "Logstash Plugin Architecture"
        LC[LogStash::Plugin<br>Base Class] --> IP[LogStash::Inputs::Base]
        LC --> FP[LogStash::Filters::Base]
        LC --> OP[LogStash::Outputs::Base]
        LC --> CP[LogStash::Codecs::Base]

        IP --> |"def register"| IP_R[초기화]
        IP --> |"def run(queue)"| IP_Run[이벤트 수집]
        IP --> |"def stop"| IP_S[정리]

        FP --> |"def register"| FP_R[초기화]
        FP --> |"def filter(event)"| FP_F[이벤트 변환]

        OP --> |"def register"| OP_R[초기화]
        OP --> |"def multi_receive(events)"| OP_MR[배치 출력]
    end

    style LC fill:#E91E63,color:#fff
    style IP fill:#4CAF50,color:#fff
    style FP fill:#FF9800,color:#fff
    style OP fill:#2196F3,color:#fff
    style CP fill:#9C27B0,color:#fff
```

### 3.4 플러그인 관리 CLI

```bash
# 설치된 플러그인 목록 확인
bin/logstash-plugin list
bin/logstash-plugin list --verbose  # 버전 포함
bin/logstash-plugin list '*grok*'   # 패턴 검색

# 플러그인 설치/업데이트/제거
bin/logstash-plugin install logstash-filter-translate
bin/logstash-plugin update logstash-filter-grok
bin/logstash-plugin remove logstash-filter-example

# 오프라인 설치 (에어갭 환경)
bin/logstash-plugin prepare-offline-pack logstash-filter-translate
bin/logstash-plugin install file:///path/to/logstash-offline-plugins.zip
```

---

## 4. 실전 예제

### 4.1 복합 로그 파싱 파이프라인

Spring Boot 애플리케이션의 멀티라인 로그를 파싱하는 실전 파이프라인이다.

```ruby
input {
  beats {
    port => 5044
    codec => multiline {
      pattern => "^%{TIMESTAMP_ISO8601}"
      negate => true
      what => "previous"
    }
  }
}

filter {
  # 1단계: 기본 필드 추출 (dissect - 고속)
  dissect {
    mapping => {
      "message" => "%{timestamp} [%{thread}] %{level} %{logger} - %{msg}"
    }
    tag_on_failure => ["_dissect_failure"]
  }

  # 2단계: dissect 실패 시 grok 폴백
  if "_dissect_failure" in [tags] {
    grok {
      match => {
        "message" => "%{TIMESTAMP_ISO8601:timestamp}\s+\[%{DATA:thread}\]\s+%{LOGLEVEL:level}\s+%{JAVACLASS:logger}\s+-\s+%{GREEDYDATA:msg}"
      }
      tag_on_failure => ["_grok_failure"]
    }
    mutate {
      remove_tag => ["_dissect_failure"]
    }
  }

  # 3단계: 타임스탬프 정규화
  date {
    match => ["timestamp", "yyyy-MM-dd HH:mm:ss.SSS", "ISO8601"]
    target => "@timestamp"
    timezone => "Asia/Seoul"
  }

  # 4단계: 필드 정리 및 타입 변환
  mutate {
    strip => ["level", "thread", "logger"]
    uppercase => ["level"]
    remove_field => ["timestamp", "host", "agent"]
  }

  # 5단계: ERROR 레벨 로그에서 스택 트레이스 추출
  if [level] == "ERROR" {
    grok {
      match => {
        "msg" => "(?<exception_class>[a-zA-Z.]+Exception): %{GREEDYDATA:exception_message}"
      }
      tag_on_failure => []  # 실패해도 태그 추가 안 함
    }
  }

  # 6단계: GeoIP 처리 (IP 필드가 있는 경우)
  if [client_ip] {
    geoip {
      source => "client_ip"
      target => "geo"
      fields => ["city_name", "country_name", "location", "region_name"]
    }
  }
}

output {
  elasticsearch {
    hosts => ["https://es-node1:9200", "https://es-node2:9200"]
    index => "app-logs-%{+YYYY.MM.dd}"
    user => "${ES_USER}"
    password => "${ES_PASSWORD}"
    ssl_certificate_authorities => ["/etc/logstash/certs/ca.crt"]
  }
}
```

### 4.2 커스텀 Filter 플러그인 개발

비즈니스 로직에 특화된 커스텀 Filter 플러그인 작성 예시이다.

```bash
# 플러그인 스캐폴딩 생성
bin/logstash-plugin generate --type filter --name business_enrichment --path /opt/plugins
```

```ruby
# logstash-filter-business_enrichment/lib/logstash/filters/business_enrichment.rb
# encoding: utf-8
require "logstash/filters/base"
require "logstash/namespace"
require "json"

class LogStash::Filters::BusinessEnrichment < LogStash::Filters::Base
  config_name "business_enrichment"

  # 설정 파라미터 선언
  config :lookup_file, :validate => :path, :required => true
  config :source_field, :validate => :string, :default => "user_id"
  config :target_field, :validate => :string, :default => "user_tier"
  config :refresh_interval, :validate => :number, :default => 300

  public
  def register
    @lookup_data = load_lookup_file
    @last_refresh = Time.now
    @logger.info("Business enrichment filter initialized",
                 :lookup_file => @lookup_file,
                 :entries => @lookup_data.size)
  end

  public
  def filter(event)
    # 주기적 데이터 갱신
    refresh_lookup_if_needed

    source_value = event.get(@source_field)
    return unless source_value

    enrichment = @lookup_data[source_value.to_s]
    if enrichment
      event.set(@target_field, enrichment["tier"])
      event.set("user_segment", enrichment["segment"])
      filter_matched(event)
    else
      event.tag("_enrichment_not_found")
    end
  end

  private
  def load_lookup_file
    JSON.parse(File.read(@lookup_file))
  rescue => e
    @logger.error("Failed to load lookup file", :error => e.message)
    {}
  end

  private
  def refresh_lookup_if_needed
    if Time.now - @last_refresh > @refresh_interval
      @lookup_data = load_lookup_file
      @last_refresh = Time.now
      @logger.info("Refreshed lookup data", :entries => @lookup_data.size)
    end
  end
end
```

### 4.3 Nginx 액세스 로그 실전 파이프라인

```ruby
filter {
  # 커스텀 Nginx 로그 포맷 파싱
  grok {
    patterns_dir => ["/etc/logstash/patterns"]
    match => {
      "message" => '%{IPORHOST:client_ip} - %{DATA:user_name} \[%{HTTPDATE:access_time}\] "%{WORD:http_method} %{URIPATHPARAM:request_uri} HTTP/%{NUMBER:http_version}" %{NUMBER:response_code:int} %{NUMBER:body_bytes:int} "%{DATA:referrer}" "%{DATA:user_agent}" %{NUMBER:request_time:float}'
    }
  }

  # User-Agent 파싱
  useragent {
    source => "user_agent"
    target => "ua"
  }

  # 응답 코드 기반 분류
  if [response_code] >= 500 {
    mutate { add_tag => ["server_error"] }
  } else if [response_code] >= 400 {
    mutate { add_tag => ["client_error"] }
  }

  # 느린 요청 태깅
  if [request_time] and [request_time] > 1.0 {
    mutate { add_tag => ["slow_request"] }
  }

  # 민감 정보 마스킹
  mutate {
    gsub => [
      "request_uri", "(?<=token=)[^&]+", "***MASKED***",
      "request_uri", "(?<=password=)[^&]+", "***MASKED***"
    ]
  }
}
```

---

## 보충: Logstash 큐 시스템

Logstash의 내부 큐 시스템은 파이프라인의 input과 filter/output 사이에서 이벤트를 버퍼링하며, Memory Queue와 Persistent Queue 두 가지 모드를 통해 성능과 내구성 사이의 트레이드오프를 제공한다.

### Memory Queue vs Persistent Queue

| 구분 | Memory Queue | Persistent Queue (PQ) |
|------|-------------|----------------------|
| 저장 위치 | JVM 힙 메모리 | 디스크 (memory-mapped file) |
| 내구성 | 프로세스 종료 시 유실 | 프로세스 재시작 후 복구 |
| 성능 | 높음 (메모리 직접 접근) | 약간 낮음 (디스크 I/O) |
| 설정 | `queue.type: memory` (기본값) | `queue.type: persisted` |
| 용량 제한 | `pipeline.batch.size` 기반 | `queue.max_bytes` 설정 |

### AckedQueue 핵심 구조

Persistent Queue의 핵심은 `Queue` 클래스로, Head Page와 Tail Page 개념을 사용한다.

- **Head Page**: 현재 쓰기가 진행되는 활성 페이지
- **Tail Pages**: 쓰기가 완료되어 읽기 전용이 된 페이지들
- **Unread Tail Pages**: 아직 읽히지 않은 tail 페이지 목록
- **Sequence Number**: 각 이벤트에 순차적으로 부여되는 고유 번호
- **Acknowledgement (Ack)**: 이벤트가 성공적으로 처리되었음을 표시

### Queue 클래스 - 쓰기 흐름

```java
// Queue.java - 핵심 흐름 요약
public long write(Queueable element) throws IOException {
    // 1. 직렬화 및 압축
    byte[] serializedBytes = element.serialize();
    byte[] data = compressionCodec.encode(serializedBytes);

    lock.lock();
    try {
        // 2. Head Page 용량 확인 → 부족하면 새 Head Page 생성
        if (!this.headPage.hasSpace(data.length)) {
            int newHeadPageNum = this.headPage.pageNum + 1;
            if (this.headPage.isFullyAcked()) {
                this.headPage.purge();  // 완전 ack된 페이지는 삭제
            } else {
                behead();  // tail로 전환
            }
            newCheckpointedHeadpage(newHeadPageNum);
        }

        // 3. 시퀀스 번호 할당 후 쓰기
        long seqNum = this.seqNum += 1;
        this.headPage.write(data, seqNum, this.checkpointMaxWrites);
        this.unreadCount++;

        // 4. 큐 full 상태면 notFull 조건 대기
        while (isFull() && !isClosed()) {
            notFull.await();
        }
        return seqNum;
    } finally {
        lock.unlock();
    }
}
```

### MmapPageIOV2 - Memory-Mapped I/O

각 페이지 파일은 `MmapPageIOV2`를 통해 memory-mapped file로 관리된다.

```
 Page File Layout (MmapPageIOV2)
 ┌─────────────────────────────────────────────────┐
 │ Version (1 byte)                                │
 ├─────────────────────────────────────────────────┤
 │ Element 1:                                      │
 │   SeqNum (8 bytes) │ Length (4 bytes) │          │
 │   Data (variable)  │ CRC32 (4 bytes)            │
 ├─────────────────────────────────────────────────┤
 │ Element 2:                                      │
 │   SeqNum │ Length │ Data │ CRC32                 │
 ├─────────────────────────────────────────────────┤
 │ ...                                             │
 │                     ▲ head (write position)      │
 │ (unused capacity)                               │
 └─────────────────────────────────────────────────┘
```

각 엘리먼트는 `SeqNum(8B) + Length(4B) + Data(가변) + CRC32(4B)` 구조로 저장된다. CRC32 체크섬으로 데이터 무결성을 검증하며, `recover()` 메서드가 비정상 종료 후 유효한 엘리먼트만 복구한다.

### Checkpoint 메커니즘

체크포인트는 큐의 상태를 디스크에 기록하여 복구 시점을 제공한다.

- **Head Checkpoint**: head 페이지의 현재 상태 (minSeqNum, elementCount, firstUnackedSeqNum)
- **Tail Checkpoint**: 각 tail 페이지별 상태
- `checkpointMaxWrites`: N번 쓰기마다 체크포인트 수행
- `checkpointMaxAcks`: N번 ack마다 체크포인트 수행

### Dead Letter Queue 내부 구현

`DeadLetterQueueWriter`는 세그먼트 파일 기반으로 실패한 이벤트를 저장한다.

핵심 정책:
- **Age Retention**: `retentionTime` 설정 기반, 만료된 세그먼트 자동 삭제
- **Storage Policy**: `maxQueueSize` 초과 시 `DROP_NEWER`(새 이벤트 버림) 또는 `DROP_OLDER`(오래된 세그먼트 삭제) 적용
- **Segment Sealing**: `.log.tmp` → `.log` 파일로 atomic move하여 세그먼트 확정

```java
// DeadLetterQueueWriter - 이벤트 쓰기 핵심 로직
private void innerWriteEntry(DLQEntry entry) throws IOException {
    if (alreadyProcessed(event)) { return; }  // 중복 방지
    byte[] record = entry.serialize();
    int eventPayloadSize = RECORD_HEADER_SIZE + record.length;

    executeAgeRetentionPolicy();       // 만료 세그먼트 정리
    boolean skipWrite = executeStoragePolicy(eventPayloadSize);  // 용량 정책
    if (skipWrite) { return; }

    if (exceedSegmentSize(eventPayloadSize)) {
        finalizeSegment(FinalizeWhen.ALWAYS, SealReason.SEGMENT_FULL);
    }
    long writtenBytes = currentWriter.writeEvent(record);
    currentQueueSize.getAndAdd(writtenBytes);
}
```

### DLQ 재처리 파이프라인 (상세)

```ruby
# dlq-reprocess.conf
input {
  dead_letter_queue {
    path => "/var/lib/logstash/dead_letter_queue"
    pipeline_id => "main"
    commit_offsets => true
  }
}

filter {
  # DLQ 메타데이터에서 실패 원인 확인
  mutate {
    add_field => {
      "dlq_reason" => "%{[@metadata][dead_letter_queue][reason]}"
      "dlq_plugin" => "%{[@metadata][dead_letter_queue][plugin_id]}"
    }
  }

  # 매핑 오류 수정 (예: 타입 변환)
  if [dlq_reason] =~ /mapper_parsing_exception/ {
    mutate {
      convert => { "response_time" => "float" }
    }
  }
}

output {
  elasticsearch {
    hosts => ["http://es-node:9200"]
    index => "recovered-%{+YYYY.MM.dd}"
  }
}
```

## 5. 정리

| 항목 | 핵심 내용 |
|------|----------|
| **플러그인 유형** | Input(수집), Filter(변환), Output(전송), Codec(직렬화) 4가지 |
| **Grok** | Oniguruma 정규식 기반, 유연하지만 CPU 집약적 |
| **Dissect** | 구분자 기반 토큰 파싱, Grok 대비 10x 빠름, 고정 포맷 한정 |
| **mutate** | 필드 조작 범용 플러그인, 내부 처리 순서 고정 |
| **date** | 문자열 타임스탬프를 @timestamp로 정규화 |
| **geoip** | IP → 위치 변환, MaxMind DB 사용 |
| **커스텀 플러그인** | `LogStash::Filters::Base` 상속, `register`/`filter` 구현 |
| **성능 전략** | dissect 우선, grok 폴백 패턴 권장 |
| **플러그인 관리** | `logstash-plugin` CLI로 설치/업데이트/오프라인 배포 |
| **Memory Queue** | JVM 힙 기반, 최고 성능, 프로세스 종료 시 데이터 유실 |
| **Persistent Queue** | MmapPageIOV2 기반 디스크 저장, CRC32 무결성 검증, 프로세스 복구 가능 |
| **Checkpoint** | head/tail 체크포인트 파일로 큐 상태 기록, 복구 시점 보장 |
| **Dead Letter Queue** | 세그먼트 파일 기반, Age/Size 보관 정책, DROP_NEWER/DROP_OLDER 적용 |

---
*참고: Elasticsearch 8.x / Logstash 8.x / Kibana 8.x 기준*

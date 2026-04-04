# Logstash 실전 파이프라인 설계

Logstash 멀티 파이프라인 설계 패턴, 조건 분기, Dead Letter Queue, 에러 핸들링까지 프로덕션 파이프라인 설계의 핵심 패턴을 다룬다.

## 목차
- [1. 핵심 개념 (What)](#1-핵심-개념-what)
- [2. 왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
- [3. 내부 구현 분석 (How)](#3-내부-구현-분석-how)
- [4. 실전 예제](#4-실전-예제)
- [5. 정리](#5-정리)

---

## 1. 핵심 개념 (What)

### 파이프라인 구성 요소

Logstash 파이프라인은 세 단계로 구성된다.

```
Input → Filter → Output
(수집)   (변환)    (전송)
```

단일 파이프라인으로는 복잡한 요구사항을 감당하기 어렵다. 실전에서는 **멀티 파이프라인**, **조건 분기**, **에러 핸들링**을 조합하여 설계한다.

### 파이프라인 설계 핵심 원칙

| 원칙 | 설명 |
|------|------|
| **격리(Isolation)** | 서로 다른 데이터 소스는 별도 파이프라인으로 분리 |
| **단일 책임** | 하나의 파이프라인은 하나의 데이터 흐름만 처리 |
| **실패 허용(Fault Tolerance)** | DLQ, 재처리 경로로 데이터 유실 방지 |
| **관측 가능성** | 파이프라인별 메트릭 수집과 태깅 |

### 멀티 파이프라인 vs 단일 파이프라인 비교

| 항목 | 단일 파이프라인 | 멀티 파이프라인 |
|------|----------------|----------------|
| 설정 복잡도 | 낮음 | 중간 |
| 장애 격리 | 없음 (하나 실패 시 전체 영향) | 파이프라인별 독립 |
| 리소스 제어 | 불가 | 파이프라인별 Worker/Batch 설정 가능 |
| 스케일링 | 전체 단위 | 파이프라인별 독립 스케일 |
| 모니터링 | 어려움 | 파이프라인별 메트릭 분리 |

---

## 2. 왜 알아야 하는가 (Why)

### 실전에서 마주하는 문제들

**문제 1: 한 소스의 장애가 전체를 멈춘다**

단일 파이프라인에서 Kafka input이 장애를 일으키면, 같은 파이프라인의 Filebeat input도 함께 멈출 수 있다. 멀티 파이프라인으로 분리하면 각 파이프라인이 독립적으로 동작한다.

**문제 2: 파싱 실패 이벤트가 사라진다**

Grok 패턴 불일치, JSON 파싱 실패 등으로 이벤트가 `_grokparsefailure` 태그만 달고 Elasticsearch에 저장되거나, 최악의 경우 드랍된다. DLQ와 에러 핸들링 파이프라인이 없으면 데이터 유실을 감지조차 못한다.

**문제 3: 조건 분기가 복잡해지면 유지보수가 불가능하다**

`if/else if/else` 체인이 중첩되면 디버깅이 극도로 어렵다. 체계적인 분기 패턴과 파이프라인 분리가 필요하다.

---

## 3. 내부 구현 분석 (How)

### 3.1 멀티 파이프라인 아키텍처

```mermaid
graph TD
    subgraph "pipelines.yml"
        A[nginx-access Pipeline] --> E[Elasticsearch]
        B[app-logs Pipeline] --> E
        C[metrics Pipeline] --> E
        D[dlq-reprocess Pipeline] --> E
    end
    
    F[Filebeat] --> A
    G[Kafka] --> B
    H[Metricbeat] --> C
    I[Dead Letter Queue] --> D
```

**pipelines.yml 설정**:

```yaml
# /etc/logstash/pipelines.yml

- pipeline.id: nginx-access
  path.config: "/etc/logstash/pipelines/nginx-access/*.conf"
  pipeline.workers: 4
  pipeline.batch.size: 500
  queue.type: persisted
  queue.max_bytes: 1gb

- pipeline.id: app-logs
  path.config: "/etc/logstash/pipelines/app-logs/*.conf"
  pipeline.workers: 2
  pipeline.batch.size: 250
  queue.type: persisted
  queue.max_bytes: 2gb

- pipeline.id: metrics
  path.config: "/etc/logstash/pipelines/metrics/*.conf"
  pipeline.workers: 1
  pipeline.batch.size: 1000
  queue.type: memory

- pipeline.id: dlq-reprocess
  path.config: "/etc/logstash/pipelines/dlq/*.conf"
  pipeline.workers: 1
  pipeline.batch.size: 100
```

### 3.2 Pipeline-to-Pipeline 통신

Logstash 8.x에서는 파이프라인 간 이벤트 전달이 가능하다. `pipeline` input/output을 사용하여 내부 라우팅을 구성한다.

```mermaid
graph LR
    A[Distributor Pipeline] -->|"address: nginx"| B[Nginx Pipeline]
    A -->|"address: app"| C[App Pipeline]
    A -->|"address: default"| D[Default Pipeline]
    B --> E[Elasticsearch]
    C --> E
    D --> E
```

**Distributor 파이프라인**:

```ruby
# pipelines/distributor/input.conf
input {
  beats {
    port => 5044
  }
}

# pipelines/distributor/output.conf
output {
  if [fields][log_type] == "nginx" {
    pipeline {
      send_to => ["nginx"]
    }
  } else if [fields][log_type] == "application" {
    pipeline {
      send_to => ["app"]
    }
  } else {
    pipeline {
      send_to => ["default"]
    }
  }
}
```

**수신 파이프라인 (Nginx)**:

```ruby
# pipelines/nginx/input.conf
input {
  pipeline {
    address => "nginx"
  }
}

# pipelines/nginx/filter.conf
filter {
  grok {
    match => {
      "message" => '%{IPORHOST:client_ip} - %{DATA:user} \[%{HTTPDATE:timestamp}\] "%{WORD:method} %{URIPATHPARAM:request} HTTP/%{NUMBER:http_version}" %{NUMBER:response_code} %{NUMBER:bytes}'
    }
  }
  date {
    match => ["timestamp", "dd/MMM/yyyy:HH:mm:ss Z"]
    target => "@timestamp"
  }
  geoip {
    source => "client_ip"
    target => "geoip"
  }
  mutate {
    remove_field => ["message", "timestamp"]
  }
}

# pipelines/nginx/output.conf
output {
  elasticsearch {
    hosts => ["https://es-prod:9200"]
    index => "nginx-access-%{+YYYY.MM.dd}"
    user => "logstash_writer"
    password => "${ES_PASSWORD}"
    ssl_certificate_authorities => ["/etc/pki/ca.crt"]
  }
}
```

### 3.3 조건 분기 패턴

#### 패턴 1: 태그 기반 분기

```ruby
filter {
  # 공통 처리
  mutate {
    add_field => { "[@metadata][pipeline]" => "unknown" }
  }

  if [agent][type] == "filebeat" {
    if [fileset][module] == "nginx" {
      mutate { replace => { "[@metadata][pipeline]" => "nginx" } }
    } else if [fileset][module] == "system" {
      mutate { replace => { "[@metadata][pipeline]" => "system" } }
    }
  }

  # 모듈별 처리
  if [@metadata][pipeline] == "nginx" {
    grok {
      match => { "message" => "%{COMBINEDAPACHELOG}" }
    }
  } else if [@metadata][pipeline] == "system" {
    grok {
      match => { "message" => "%{SYSLOGLINE}" }
    }
  }
}
```

#### 패턴 2: @metadata를 활용한 라우팅

`@metadata` 필드는 output 이후 자동 제거되므로 라우팅 정보를 저장하기에 적합하다.

```ruby
filter {
  # 인덱스 라우팅 결정
  if [kubernetes][namespace] =~ /^prod-/ {
    mutate {
      add_field => {
        "[@metadata][target_index]" => "prod-%{[kubernetes][labels][app]}-%{+YYYY.MM.dd}"
      }
    }
  } else {
    mutate {
      add_field => {
        "[@metadata][target_index]" => "dev-%{[kubernetes][labels][app]}-%{+YYYY.MM.dd}"
      }
    }
  }
}

output {
  elasticsearch {
    hosts => ["https://es-prod:9200"]
    index => "%{[@metadata][target_index]}"
  }
}
```

### 3.4 Dead Letter Queue (DLQ)

DLQ는 output 플러그인에서 처리 실패한 이벤트를 별도 큐에 저장하는 메커니즘이다.

```mermaid
graph TD
    A[Input] --> B[Filter]
    B --> C[Output: Elasticsearch]
    C -->|"성공"| D[완료]
    C -->|"실패 (400 Bad Request 등)"| E[Dead Letter Queue]
    E --> F[DLQ Reprocess Pipeline]
    F -->|"수정 후 재전송"| C
    F -->|"재실패"| G[Error Index / Alert]
```

**DLQ 활성화**:

```yaml
# logstash.yml
dead_letter_queue.enable: true
dead_letter_queue.max_bytes: 4gb
dead_letter_queue.storage_policy: drop_newer  # or drop_older
dead_letter_queue.retain.age: 7d
path.dead_letter_queue: "/var/logstash/dlq"
```

**DLQ 재처리 파이프라인**:

```ruby
# pipelines/dlq/reprocess.conf
input {
  dead_letter_queue {
    path => "/var/logstash/dlq"
    commit_offsets => true
    pipeline_id => "app-logs"
    clean_consumed => true
  }
}

filter {
  # DLQ 메타데이터 확인
  ruby {
    code => '
      dlq_entry = event.get("[@metadata][dead_letter_queue]")
      if dlq_entry
        event.set("[@metadata][dlq_reason]", dlq_entry["reason"])
        event.set("[@metadata][dlq_plugin_type]", dlq_entry["plugin_type"])
        event.set("[@metadata][dlq_plugin_id]", dlq_entry["plugin_id"])
      end
    '
  }

  # 일반적인 수정 시도: 너무 긴 필드 잘라내기
  if [message] {
    truncate {
      fields => ["message"]
      length_bytes => 32766
    }
  }

  # 매핑 충돌 해결: 문제 필드 타입 변환
  if [@metadata][dlq_reason] =~ /mapper_parsing_exception/ {
    ruby {
      code => '
        # 숫자 필드에 문자열이 들어온 경우 처리
        problematic_fields = ["response_time", "bytes", "status_code"]
        problematic_fields.each do |field|
          val = event.get(field)
          if val.is_a?(String)
            numeric_val = val.to_f rescue nil
            if numeric_val
              event.set(field, numeric_val)
            else
              event.remove(field)
              event.tag("_dlq_field_removed_#{field}")
            end
          end
        end
      '
    }
  }

  # 재처리 메타 추가
  mutate {
    add_field => {
      "[@metadata][reprocessed]" => "true"
      "dlq_reprocessed_at" => "%{+ISO8601}"
    }
  }
}

output {
  elasticsearch {
    hosts => ["https://es-prod:9200"]
    index => "recovered-%{+YYYY.MM.dd}"
    user => "logstash_writer"
    password => "${ES_PASSWORD}"
    ssl_certificate_authorities => ["/etc/pki/ca.crt"]
  }
}
```

### 3.5 에러 핸들링 패턴

```ruby
filter {
  # JSON 파싱 시도
  json {
    source => "message"
    target => "parsed"
    tag_on_failure => ["_json_parse_failure"]
  }

  # 파싱 실패 시 폴백 처리
  if "_json_parse_failure" in [tags] {
    mutate {
      add_field => {
        "parse_error" => "true"
        "raw_message" => "%{message}"
      }
      add_tag => ["needs_review"]
    }
    # 원본 보존을 위해 별도 인덱스로 라우팅
    mutate {
      add_field => {
        "[@metadata][target_index]" => "parse-errors-%{+YYYY.MM.dd}"
      }
    }
  }

  # Grok 실패 핸들링
  grok {
    match => { "message" => "%{COMMONAPACHELOG}" }
    tag_on_failure => ["_grok_nomatch"]
  }

  if "_grok_nomatch" in [tags] {
    # 대체 패턴 시도
    grok {
      match => { "message" => "%{GREEDYDATA:raw_log}" }
      overwrite => ["raw_log"]
      remove_tag => ["_grok_nomatch"]
      add_tag => ["_grok_fallback"]
    }
  }
}

output {
  if "parse-errors" in [@metadata][target_index] {
    elasticsearch {
      hosts => ["https://es-prod:9200"]
      index => "%{[@metadata][target_index]}"
    }
  } else {
    elasticsearch {
      hosts => ["https://es-prod:9200"]
      index => "logs-%{+YYYY.MM.dd}"
    }
  }
}
```

---

## 4. 실전 예제

### 4.1 마이크로서비스 로그 수집 파이프라인

```ruby
# pipelines/microservices/input.conf
input {
  kafka {
    bootstrap_servers => "kafka-01:9092,kafka-02:9092,kafka-03:9092"
    topics_pattern => "logs\\..*"
    group_id => "logstash-microservices"
    consumer_threads => 3
    codec => json
    decorate_events => "extended"
    auto_offset_reset => "latest"
  }
}

# pipelines/microservices/filter.conf
filter {
  # 공통 필드 정규화
  mutate {
    rename => {
      "[kafka][topic]" => "[@metadata][kafka_topic]"
    }
  }

  # 서비스별 파싱
  if [@metadata][kafka_topic] =~ /^logs\.order-service/ {
    # 주문 서비스 로그 파싱
    if [level] == "ERROR" {
      grok {
        match => {
          "stack_trace" => "(?<exception_class>[a-zA-Z.]+Exception): %{GREEDYDATA:exception_message}"
        }
        tag_on_failure => []
      }
    }
    mutate {
      add_field => { "[@metadata][service]" => "order-service" }
    }
  } else if [@metadata][kafka_topic] =~ /^logs\.payment-service/ {
    # 결제 서비스 - 민감 정보 마스킹
    mutate {
      gsub => [
        "message", "\b\d{4}[-\s]?\d{4}[-\s]?\d{4}[-\s]?\d{4}\b", "[CARD_MASKED]",
        "message", "\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b", "[EMAIL_MASKED]"
      ]
    }
    mutate {
      add_field => { "[@metadata][service]" => "payment-service" }
    }
  }

  # 공통 enrichment
  mutate {
    add_field => {
      "[@metadata][target_index]" => "svc-%{[@metadata][service]}-%{+YYYY.MM.dd}"
    }
  }
}

# pipelines/microservices/output.conf
output {
  elasticsearch {
    hosts => ["https://es-prod:9200"]
    index => "%{[@metadata][target_index]}"
    user => "logstash_writer"
    password => "${ES_PASSWORD}"
    ssl_certificate_authorities => ["/etc/pki/ca.crt"]
    action => "create"
  }
}
```

### 4.2 파이프라인 테스트

Logstash는 `--config.test_and_exit` 옵션으로 설정 문법을 검증할 수 있다.

```bash
# 설정 문법 검증
/usr/share/logstash/bin/logstash \
  --config.test_and_exit \
  --path.config /etc/logstash/pipelines/nginx-access/

# 단일 파이프라인 디버그 모드
/usr/share/logstash/bin/logstash \
  -f /etc/logstash/pipelines/nginx-access/ \
  --pipeline.workers 1 \
  --log.level debug
```

**stdin/stdout을 이용한 필터 테스트**:

```ruby
# test-filter.conf
input {
  stdin {
    codec => json
  }
}

filter {
  grok {
    match => {
      "message" => '%{IPORHOST:client_ip} - %{DATA:user} \[%{HTTPDATE:timestamp}\] "%{WORD:method} %{URIPATHPARAM:request} HTTP/%{NUMBER:http_version}" %{NUMBER:response_code} %{NUMBER:bytes}'
    }
  }
}

output {
  stdout {
    codec => rubydebug
  }
}
```

```bash
# 테스트 실행
echo '{"message": "192.168.1.1 - frank [10/Oct/2024:13:55:36 +0000] \"GET /api/users HTTP/1.1\" 200 1234"}' | \
  /usr/share/logstash/bin/logstash -f test-filter.conf --pipeline.workers 1
```

### 4.3 파이프라인 모니터링 API

```bash
# 전체 파이프라인 상태
curl -s localhost:9600/_node/stats/pipelines?pretty

# 특정 파이프라인 상태
curl -s localhost:9600/_node/stats/pipelines/nginx-access?pretty

# 핵심 메트릭 추출
curl -s localhost:9600/_node/stats/pipelines | \
  python3 -c "
import sys, json
data = json.load(sys.stdin)
for pid, stats in data['pipelines'].items():
    events = stats['events']
    print(f'Pipeline: {pid}')
    print(f'  In: {events[\"in\"]}  Out: {events[\"out\"]}  Filtered: {events[\"filtered\"]}')
    print(f'  Duration (ms): {events[\"duration_in_millis\"]}')
    if events['in'] > 0:
        drop_rate = (1 - events['out'] / events['in']) * 100
        print(f'  Drop Rate: {drop_rate:.2f}%')
    print()
"
```

---

## 5. 정리

| 항목 | 핵심 포인트 |
|------|-------------|
| **멀티 파이프라인** | `pipelines.yml`로 파이프라인 분리, 각각 독립 Worker/Queue 설정 |
| **Pipeline-to-Pipeline** | `pipeline` input/output으로 내부 라우팅, Distributor 패턴 |
| **조건 분기** | `@metadata` 활용, 태그 기반 분기, 중첩 최소화 |
| **DLQ** | `dead_letter_queue.enable: true`, 별도 재처리 파이프라인 구성 |
| **에러 핸들링** | `tag_on_failure`로 실패 감지, 폴백 처리, 별도 에러 인덱스 |
| **테스트** | `--config.test_and_exit`로 문법 검증, stdin/stdout으로 필터 테스트 |
| **모니터링** | `_node/stats/pipelines` API로 in/out/filtered/duration 확인 |

### 파이프라인 설계 체크리스트

- [ ] 데이터 소스별 파이프라인 분리 여부 검토
- [ ] 각 파이프라인의 Worker/Batch 사이즈 적정성 확인
- [ ] DLQ 활성화 및 재처리 파이프라인 구성
- [ ] 파싱 실패 이벤트의 처리 경로 정의
- [ ] `@metadata` 기반 라우팅으로 불필요한 필드 제거
- [ ] 민감 정보 마스킹 처리 확인
- [ ] Persistent Queue 사용 여부 결정 (데이터 유실 방지)
- [ ] 파이프라인 모니터링 메트릭 수집 확인

---

## 보충: Kibana 대시보드 시각화

Kibana의 Lens 시각화 빌더, Discover 로그 탐색, Dashboard 설계 원칙, KQL/ES|QL 쿼리 언어, Saved Objects 관리를 통한 실전 모니터링 대시보드 구축 방법을 정리한다.

### Kibana 핵심 기능

| 기능 | 설명 |
|------|------|
| **Discover** | 로그 데이터 실시간 탐색 및 필터링 |
| **Lens** | 드래그 앤 드롭 기반 시각화 빌더 |
| **Dashboard** | 여러 시각화를 조합한 모니터링 화면 |
| **Canvas** | 프레젠테이션용 인포그래픽 |
| **Maps** | 지리 데이터 시각화 |

### 쿼리 언어

- **KQL (Kibana Query Language)**: Kibana 기본 쿼리 언어. 자동완성 지원, 간결한 문법
- **ES|QL (Elasticsearch Query Language)**: Elasticsearch 8.11+에서 도입된 파이프 기반 쿼리 언어
- **Lucene**: 전통적인 전문 검색 쿼리 문법

### Kibana 시각화 아키텍처

```mermaid
graph TB
    USER[사용자] --> KIBANA[Kibana Server]

    subgraph "Kibana 내부"
        KIBANA --> DISCOVER[Discover]
        KIBANA --> LENS[Lens Editor]
        KIBANA --> DASHBOARD[Dashboard]

        DISCOVER --> KQL_PARSER[KQL Parser]
        LENS --> AGG_BUILDER[Aggregation Builder]
        DASHBOARD --> PANELS[Panel Manager]

        KQL_PARSER --> QUERY_DSL[Elasticsearch Query DSL 변환]
        AGG_BUILDER --> QUERY_DSL
        PANELS --> QUERY_DSL
    end

    QUERY_DSL --> ES[Elasticsearch Cluster]
    ES --> RESULT[검색 결과 / 집계 결과]
    RESULT --> RENDER[시각화 렌더링]
    RENDER --> USER
```

### KQL 쿼리 패턴

```
# 기본 필드 검색
status: 500
service: "api-gateway"

# 범위 검색
response_time > 1000
status >= 400 and status < 500

# 와일드카드
message: *timeout*
host.name: web-server-*

# 논리 연산
status: 500 and service: "api-gateway"
status: 404 or status: 503
not status: 200

# 존재 여부
error.message: *
not response_time: *
```

### ES|QL 쿼리 패턴

```sql
-- 기본 조회 및 필터링
FROM app-logs-*
| WHERE status >= 400
| SORT @timestamp DESC
| LIMIT 100

-- 집계: 서비스별 에러 비율
FROM app-logs-*
| WHERE @timestamp > NOW() - 1 hour
| STATS total = COUNT(*),
        errors = COUNT(*) WHERE status >= 500,
        error_rate = ROUND(COUNT(*) WHERE status >= 500 / COUNT(*) * 100, 2)
  BY service
| SORT error_rate DESC

-- 시계열 분석: 5분 단위 요청 추이
FROM app-logs-*
| WHERE @timestamp > NOW() - 24 hours
| EVAL time_bucket = DATE_TRUNC(5 minutes, @timestamp)
| STATS request_count = COUNT(*),
        avg_response = AVG(response_time),
        p99_response = PERCENTILE(response_time, 99)
  BY time_bucket
| SORT time_bucket
```

### Lens 시각화 타입별 활용 가이드

| 시각화 타입 | 적합한 데이터 | 예시 |
|------------|-------------|------|
| **Line** | 시계열 트렌드 | 요청 수 추이, 응답 시간 변화 |
| **Bar** | 카테고리 비교 | 서비스별 에러 수, HTTP 상태 코드 분포 |
| **Area** | 누적/비율 트렌드 | 트래픽 구성, 에러율 추이 |
| **Pie / Donut** | 비율 분포 | 상태 코드 비율, 서비스 트래픽 비중 |
| **Metric** | 단일 KPI | 총 요청 수, 에러율, P99 응답시간 |
| **Table** | 상세 데이터 비교 | Top 10 느린 엔드포인트 |
| **Heatmap** | 2차원 분포 | 시간대별/서비스별 에러 히트맵 |
| **Gauge** | 임계값 기반 상태 | SLA 달성률, 디스크 사용량 |

### Dashboard 설계 원칙

```
+------------------------------------------------------------------+
|  [KQL Filter Bar]    Time Range: Last 24 hours    Auto-refresh: 30s |
+------------------------------------------------------------------+
|                                                                    |
|  [Metric]     [Metric]      [Metric]       [Metric]               |
|  Total Req    Error Rate    P99 Latency    Active Users            |
|  1.2M         0.3%          245ms          8,432                   |
|                                                                    |
+------------------------------------------------------------------+
|                                                                    |
|  [Line Chart - 전체 트래픽 추이]                                    |
|                                                                    |
+----------------------------------+-------------------------------+
|                                  |                               |
|  [Bar - 서비스별 에러]            |  [Pie - HTTP 상태 코드]        |
|                                  |                               |
+----------------------------------+-------------------------------+
|                                                                    |
|  [Table - Top 10 Slow Endpoints]                                   |
|                                                                    |
+------------------------------------------------------------------+
```

**Dashboard 설계 3원칙**:

1. **위에서 아래로**: 요약(Metric) -> 트렌드(Line) -> 상세(Table) 순서
2. **왼쪽에서 오른쪽으로**: 중요도 높은 시각화를 왼쪽 상단에 배치
3. **필터 연동**: 대시보드 레벨 필터가 모든 패널에 전파되도록 설정

### Saved Objects 관리

```bash
# Dashboard Export (NDJSON 형식)
curl -X POST "http://localhost:5601/api/saved_objects/_export" \
  -H "kbn-xsrf: true" \
  -H "Content-Type: application/json" \
  -d '{
    "type": ["dashboard", "visualization", "lens", "search"],
    "includeReferencesDeep": true
  }' \
  --output dashboards-export.ndjson

# Dashboard Import
curl -X POST "http://localhost:5601/api/saved_objects/_import?overwrite=true" \
  -H "kbn-xsrf: true" \
  --form file=@dashboards-export.ndjson
```

### Kibana Spaces를 활용한 다중 팀 대시보드

```bash
# Space 생성
curl -X POST "http://localhost:5601/api/spaces/space" \
  -H "kbn-xsrf: true" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "platform-team",
    "name": "Platform Team",
    "description": "인프라 및 플랫폼 모니터링",
    "disabledFeatures": ["canvas", "maps"],
    "color": "#2196F3"
  }'

# Space 간 Saved Objects 복사
curl -X POST "http://localhost:5601/api/spaces/_copy_saved_objects" \
  -H "kbn-xsrf: true" \
  -H "Content-Type: application/json" \
  -d '{
    "spaces": ["platform-team"],
    "objects": [
      { "type": "dashboard", "id": "overview-dashboard" }
    ],
    "includeReferences": true,
    "overwrite": true
  }'
```

### kibana.yml 프로덕션 설정

```yaml
server.host: "0.0.0.0"
server.port: 5601
server.name: "kibana-prod"

# Elasticsearch 연결
elasticsearch.hosts: ["https://es-node-1:9200", "https://es-node-2:9200"]
elasticsearch.username: "kibana_system"
elasticsearch.password: "${KIBANA_ES_PASSWORD}"
elasticsearch.ssl.certificateAuthorities: ["/etc/kibana/certs/ca.crt"]

# 보안
server.ssl.enabled: true
server.ssl.certificate: /etc/kibana/certs/kibana.crt
server.ssl.key: /etc/kibana/certs/kibana.key
xpack.security.encryptionKey: "min-32-byte-encryption-key-here!!"
xpack.encryptedSavedObjects.encryptionKey: "another-32-byte-key-for-objects!!"
xpack.reporting.encryptionKey: "reporting-32-byte-key-here-now!!"

# 성능
elasticsearch.requestTimeout: 60000
elasticsearch.shardTimeout: 30000
```

### Kibana 대시보드 정리

| 기능 | 용도 | 핵심 포인트 |
|------|------|------------|
| Discover | 실시간 로그 탐색 | KQL 필터 + 시간 범위로 빠른 검색 |
| Lens | 시각화 생성 | 드래그 앤 드롭, 자동 차트 추천 |
| Dashboard | 모니터링 화면 | 요약->트렌드->상세 순서 배치 |
| KQL | 기본 쿼리 | 자동완성, 간결한 문법 |
| ES\|QL | 고급 집계 쿼리 | 파이프 기반, SQL과 유사 |
| Saved Objects | 설정 관리 | NDJSON export/import로 환경 이관 |
| Spaces | 팀별 격리 | 팀별 대시보드, 기능 제한 |

---
*참고: Elasticsearch 8.x / Logstash 8.x / Kibana 8.x 기준*

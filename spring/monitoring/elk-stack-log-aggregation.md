# ELK 스택 로그 집계 (Elasticsearch + Logstash + Kibana)

Spring Boot 애플리케이션의 로그를 ELK 스택으로 중앙 집계하여 검색, 분석, 시각화하는 방법을 정리한다. 메트릭은 "무엇이 일어나고 있는가"를 알려주고, 로그는 "왜 일어나고 있는가"를 알려준다.

## 목차

1. [ELK 아키텍처와 구성 요소](#1-elk-아키텍처와-구성-요소)
2. [Spring Boot 구조화 로깅](#2-spring-boot-구조화-로깅)
3. [로그 수집 파이프라인](#3-로그-수집-파이프라인)
4. [Logstash 파이프라인 설계](#4-logstash-파이프라인-설계)
5. [Elasticsearch 인덱스 설계](#5-elasticsearch-인덱스-설계)
6. [Kibana 활용](#6-kibana-활용)
7. [Loki 대안 (경량 로그 집계)](#7-loki-대안-경량-로그-집계)

---

## 1. ELK 아키텍처와 구성 요소

### 1.1 전체 아키텍처

```
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│ Spring Boot 1 │   │ Spring Boot 2 │   │ Spring Boot 3 │
│  (Logback)    │   │  (Logback)    │   │  (Logback)    │
└──────┬────────┘   └──────┬────────┘   └──────┬────────┘
       │                   │                    │
       │ JSON Log          │ JSON Log           │ JSON Log
       ▼                   ▼                    ▼
┌─────────────────────────────────────────────────────┐
│                    Filebeat                           │
│          (경량 로그 수집기, 각 노드에 배포)              │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│                    Logstash                           │
│          (파싱, 변환, 필터링, 라우팅)                   │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│                  Elasticsearch                        │
│          (인덱싱, 검색, 저장)                           │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│                    Kibana                             │
│          (검색, 시각화, 대시보드)                        │
└─────────────────────────────────────────────────────┘
```

### 1.2 각 컴포넌트 역할

| 컴포넌트 | 역할 | 핵심 특징 |
|---------|------|----------|
| **Filebeat** | 로그 파일 수집/전송 | 경량, 각 노드에 사이드카 배포, 백프레셔 지원 |
| **Logstash** | 로그 파싱/변환 | Grok 패턴, 필터 플러그인, 다양한 입출력 |
| **Elasticsearch** | 로그 저장/검색 | 풀텍스트 검색, 분산 저장, 집계 기능 |
| **Kibana** | 시각화/분석 | Discover, Dashboard, Lens, Alerting |

### 1.3 Docker Compose

```yaml
version: '3.8'

services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.12.0
    container_name: elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms1g -Xmx1g"
    ports:
      - "9200:9200"
    volumes:
      - es_data:/usr/share/elasticsearch/data
    networks:
      - elk

  logstash:
    image: docker.elastic.co/logstash/logstash:8.12.0
    container_name: logstash
    ports:
      - "5044:5044"   # Beats input
      - "5000:5000"   # TCP input
    volumes:
      - ./logstash/pipeline:/usr/share/logstash/pipeline
      - ./logstash/config/logstash.yml:/usr/share/logstash/config/logstash.yml
    depends_on:
      - elasticsearch
    networks:
      - elk

  kibana:
    image: docker.elastic.co/kibana/kibana:8.12.0
    container_name: kibana
    ports:
      - "5601:5601"
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    depends_on:
      - elasticsearch
    networks:
      - elk

  filebeat:
    image: docker.elastic.co/beats/filebeat:8.12.0
    container_name: filebeat
    volumes:
      - ./filebeat/filebeat.yml:/usr/share/filebeat/filebeat.yml:ro
      - /var/log/app:/var/log/app:ro  # 애플리케이션 로그 디렉토리
    depends_on:
      - logstash
    networks:
      - elk

volumes:
  es_data:

networks:
  elk:
    driver: bridge
```

---

## 2. Spring Boot 구조화 로깅

### 2.1 JSON 로그 포맷 (Logback)

구조화 로깅(Structured Logging)은 ELK 파이프라인에서 파싱 없이 바로 인덱싱할 수 있게 해준다.

```xml
<!-- logback-spring.xml -->
<configuration>
    <springProperty scope="context" name="appName"
                    source="spring.application.name" defaultValue="unknown"/>

    <!-- JSON 인코더 (logstash-logback-encoder) -->
    <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>
                {"service":"${appName}","environment":"${ENVIRONMENT:-local}"}</customFields>
            <fieldNames>
                <timestamp>@timestamp</timestamp>
                <version>[ignore]</version>
                <levelValue>[ignore]</levelValue>
            </fieldNames>
            <throwableConverter
                class="net.logstash.logback.stacktrace.ShortenedThrowableConverter">
                <maxDepthPerThrowable>30</maxDepthPerThrowable>
                <shortenedClassNameLength>20</shortenedClassNameLength>
            </throwableConverter>
        </encoder>
    </appender>

    <!-- 파일 출력 (Filebeat 수집용) -->
    <appender name="JSON_FILE"
              class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/var/log/app/${appName}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>/var/log/app/${appName}.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>7</maxHistory>
            <totalSizeCap>1GB</totalSizeCap>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>
                {"service":"${appName}","environment":"${ENVIRONMENT:-local}"}</customFields>
        </encoder>
    </appender>

    <springProfile name="local">
        <root level="INFO">
            <appender-ref ref="JSON_CONSOLE" />
        </root>
    </springProfile>

    <springProfile name="prod">
        <root level="INFO">
            <appender-ref ref="JSON_FILE" />
        </root>
    </springProfile>
</configuration>
```

**의존성 추가**:

```gradle
// build.gradle
dependencies {
    implementation 'net.logstash.logback:logstash-logback-encoder:7.4'
}
```

### 2.2 출력 예시

```json
{
  "@timestamp": "2026-02-16T10:30:45.123+09:00",
  "level": "ERROR",
  "logger_name": "c.e.o.OrderService",
  "thread_name": "http-nio-8080-exec-3",
  "message": "주문 처리 실패",
  "service": "order-service",
  "environment": "production",
  "traceId": "abc123def456",
  "spanId": "789ghi",
  "orderId": "ORD-2026-001",
  "userId": "user-123",
  "stack_trace": "java.lang.RuntimeException: Payment timeout..."
}
```

### 2.3 MDC(Mapped Diagnostic Context) 활용

```java
@Component
public class MDCFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        try {
            MDC.put("requestId", UUID.randomUUID().toString());
            MDC.put("clientIp", httpRequest.getRemoteAddr());
            MDC.put("method", httpRequest.getMethod());
            MDC.put("uri", httpRequest.getRequestURI());

            // 사용자 인증 정보가 있으면 추가
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                MDC.put("userId", auth.getName());
            }

            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
```

```java
// 비즈니스 로직에서 구조화된 로그
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    public Order processOrder(OrderRequest request) {
        MDC.put("orderId", request.getOrderId());
        MDC.put("orderType", request.getType().name());

        log.info("주문 처리 시작");

        try {
            Order order = createOrder(request);
            log.info("주문 처리 완료, amount={}", order.getTotalAmount());
            return order;
        } catch (Exception e) {
            log.error("주문 처리 실패, reason={}", e.getMessage(), e);
            throw e;
        } finally {
            MDC.remove("orderId");
            MDC.remove("orderType");
        }
    }
}
```

### 2.4 StructuredArguments 활용

```java
import static net.logstash.logback.argument.StructuredArguments.*;

// JSON 필드로 자동 변환
log.info("Order processed", keyValue("orderId", orderId), keyValue("amount", amount));
// 출력: {"message":"Order processed","orderId":"ORD-001","amount":50000}

log.info("Payment completed for {}", value("orderId", orderId));
// 출력: {"message":"Payment completed for ORD-001","orderId":"ORD-001"}
```

---

## 3. 로그 수집 파이프라인

### 3.1 Filebeat 설정

```yaml
# filebeat.yml
filebeat.inputs:
  - type: log
    enabled: true
    paths:
      - /var/log/app/*.log
    json.keys_under_root: true    # JSON 필드를 최상위로
    json.overwrite_keys: true
    json.add_error_key: true      # 파싱 에러 시 error 필드 추가
    json.expand_keys: true

    # 멀티라인 설정 (Java 스택 트레이스)
    # JSON 로그 사용 시 불필요 (logstash-logback-encoder가 처리)

    fields:
      log_type: application
    fields_under_root: true

  # 액세스 로그 (별도 수집)
  - type: log
    enabled: true
    paths:
      - /var/log/app/access*.log
    fields:
      log_type: access
    fields_under_root: true

# 프로세서
processors:
  - add_host_metadata: ~
  - add_cloud_metadata: ~
  - add_kubernetes_metadata:
      host: ${NODE_NAME}
      matchers:
        - logs_path:
            logs_path: "/var/log/containers/"

# 출력 설정
output.logstash:
  hosts: ["logstash:5044"]
  loadbalance: true

# 버퍼/큐 설정
queue.mem:
  events: 4096
  flush.min_events: 2048
  flush.timeout: 1s
```

### 3.2 직접 전송 (Logstash 없이)

소규모 환경에서는 Filebeat에서 Elasticsearch로 직접 전송할 수 있다:

```yaml
# Logstash 없이 Filebeat -> Elasticsearch 직접 전송
output.elasticsearch:
  hosts: ["elasticsearch:9200"]
  index: "app-logs-%{+yyyy.MM.dd}"
  pipeline: "spring-boot-pipeline"  # Ingest Pipeline 사용

# Elasticsearch Ingest Pipeline으로 변환 처리
# PUT _ingest/pipeline/spring-boot-pipeline
```

### 3.3 Kubernetes 환경: DaemonSet 배포

```yaml
# filebeat-daemonset.yaml
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: filebeat
  namespace: monitoring
spec:
  selector:
    matchLabels:
      app: filebeat
  template:
    metadata:
      labels:
        app: filebeat
    spec:
      serviceAccountName: filebeat
      containers:
        - name: filebeat
          image: docker.elastic.co/beats/filebeat:8.12.0
          args: ["-c", "/etc/filebeat/filebeat.yml", "-e"]
          volumeMounts:
            - name: config
              mountPath: /etc/filebeat
            - name: varlog
              mountPath: /var/log
              readOnly: true
            - name: containers
              mountPath: /var/lib/docker/containers
              readOnly: true
      volumes:
        - name: config
          configMap:
            name: filebeat-config
        - name: varlog
          hostPath:
            path: /var/log
        - name: containers
          hostPath:
            path: /var/lib/docker/containers
```

---

## 4. Logstash 파이프라인 설계

### 4.1 기본 파이프라인

```ruby
# logstash/pipeline/spring-boot.conf

input {
  beats {
    port => 5044
  }

  # TCP 직접 전송 (logback-logstash-appender)
  tcp {
    port => 5000
    codec => json_lines
  }
}

filter {
  # 이미 JSON 구조화 로그인 경우 추가 파싱 불필요
  # 비구조화 로그인 경우 Grok 파싱
  if [log_type] == "access" {
    grok {
      match => {
        "message" => '%{IPORHOST:clientip} - %{USER:ident} \[%{HTTPDATE:timestamp}\] "%{WORD:method} %{URIPATHPARAM:request} HTTP/%{NUMBER:httpversion}" %{NUMBER:response} %{NUMBER:bytes}'
      }
    }
    date {
      match => ["timestamp", "dd/MMM/yyyy:HH:mm:ss Z"]
      target => "@timestamp"
    }
  }

  # 공통 필드 정리
  mutate {
    remove_field => ["agent", "ecs", "input", "host.name"]
    # 민감 정보 제거
    remove_field => ["password", "token", "secret"]
  }

  # GeoIP (클라이언트 IP 기반 위치 정보)
  if [clientIp] {
    geoip {
      source => "clientIp"
      target => "geoip"
    }
  }

  # 로그 레벨 기반 인덱스 분리
  if [level] == "ERROR" or [level] == "WARN" {
    mutate {
      add_field => { "[@metadata][index_suffix]" => "errors" }
    }
  } else {
    mutate {
      add_field => { "[@metadata][index_suffix]" => "logs" }
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "app-%{[@metadata][index_suffix]}-%{+YYYY.MM.dd}"
    # ILM(Index Lifecycle Management) 사용 시
    # ilm_enabled => true
    # ilm_rollover_alias => "app-logs"
    # ilm_pattern => "000001"
    # ilm_policy => "app-logs-policy"
  }

  # 디버깅용 stdout
  # stdout { codec => rubydebug }
}
```

### 4.2 민감 정보 마스킹

```ruby
filter {
  # 이메일 마스킹
  mutate {
    gsub => [
      "message", "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}", "[MASKED_EMAIL]"
    ]
  }

  # 카드번호 마스킹
  mutate {
    gsub => [
      "message", "\b\d{4}[\s-]?\d{4}[\s-]?\d{4}[\s-]?\d{4}\b", "[MASKED_CARD]"
    ]
  }

  # 특정 필드 제거
  prune {
    blacklist_names => ["password", "token", "secret", "authorization"]
  }
}
```

---

## 5. Elasticsearch 인덱스 설계

### 5.1 Index Lifecycle Management (ILM)

```json
// PUT _ilm/policy/app-logs-policy
{
  "policy": {
    "phases": {
      "hot": {
        "min_age": "0ms",
        "actions": {
          "rollover": {
            "max_primary_shard_size": "50gb",
            "max_age": "1d"
          },
          "set_priority": { "priority": 100 }
        }
      },
      "warm": {
        "min_age": "3d",
        "actions": {
          "shrink": { "number_of_shards": 1 },
          "forcemerge": { "max_num_segments": 1 },
          "set_priority": { "priority": 50 }
        }
      },
      "cold": {
        "min_age": "14d",
        "actions": {
          "set_priority": { "priority": 0 },
          "freeze": {}
        }
      },
      "delete": {
        "min_age": "30d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}
```

### 5.2 인덱스 템플릿

```json
// PUT _index_template/app-logs
{
  "index_patterns": ["app-logs-*"],
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 1,
      "index.lifecycle.name": "app-logs-policy",
      "index.lifecycle.rollover_alias": "app-logs"
    },
    "mappings": {
      "properties": {
        "@timestamp": { "type": "date" },
        "level": { "type": "keyword" },
        "logger_name": { "type": "keyword" },
        "thread_name": { "type": "keyword" },
        "message": { "type": "text" },
        "service": { "type": "keyword" },
        "environment": { "type": "keyword" },
        "traceId": { "type": "keyword" },
        "spanId": { "type": "keyword" },
        "userId": { "type": "keyword" },
        "orderId": { "type": "keyword" },
        "stack_trace": {
          "type": "text",
          "fields": {
            "keyword": { "type": "keyword", "ignore_above": 256 }
          }
        },
        "duration_ms": { "type": "long" }
      }
    }
  }
}
```

### 5.3 저장 비용 최적화

| 전략 | 설명 | 절감 효과 |
|-----|------|----------|
| **JSON 구조화** | 파싱 불필요, 불필요 필드 제외 | 저장량 20~30% 절감 |
| **ILM** | Hot -> Warm -> Cold -> Delete | 오래된 로그 자동 정리 |
| **로그 레벨 분리** | ERROR 로그만 장기 보관 | 보관 비용 절감 |
| **인덱스 샤드 최적화** | 데이터양에 맞는 샤드 수 | 클러스터 오버헤드 감소 |
| **샘플링** | 정상 요청 로그를 10~50%만 수집 | 저장량 대폭 절감 |

---

## 6. Kibana 활용

### 6.1 Discover에서 로그 검색

```
# KQL (Kibana Query Language) 예시

# 특정 서비스의 에러 로그
service: "order-service" and level: "ERROR"

# 특정 traceId 추적
traceId: "abc123def456"

# 특정 시간대 에러 (메시지 내용으로 검색)
level: "ERROR" and message: "timeout"

# 특정 사용자의 모든 활동
userId: "user-123" and (level: "ERROR" or level: "WARN")

# 응답 시간이 느린 요청
duration_ms > 5000
```

### 6.2 유용한 시각화

```
1. Error Rate Over Time (Line Chart)
   - X축: @timestamp
   - Y축: Count of level:"ERROR"
   - Split by: service

2. Top Error Messages (Data Table)
   - Columns: message, count, service
   - Filter: level:"ERROR"
   - Sort: count DESC

3. Log Volume by Service (Area Chart)
   - X축: @timestamp
   - Y축: Count
   - Split by: service, level

4. Slow Request Analysis (Histogram)
   - Field: duration_ms
   - Filter: duration_ms > 1000
```

### 6.3 Kibana Alert 설정

```yaml
# 에러 로그 급증 알림
Rule type: Elasticsearch query
Index: app-logs-*
Query: level:"ERROR"
Threshold: > 50 errors in 5 minutes
Action: Slack notification
```

---

## 7. Loki 대안 (경량 로그 집계)

ELK가 무겁다면, Grafana Loki를 대안으로 고려할 수 있다.

### 7.1 ELK vs Loki 비교

| 항목 | ELK Stack | Grafana Loki |
|-----|-----------|-------------|
| **인덱싱** | 풀텍스트 인덱싱 | 레이블만 인덱싱 (메타데이터) |
| **검색 성능** | 매우 빠름 (인덱스 기반) | 느림 (grep 방식) |
| **저장 비용** | 높음 | 매우 낮음 (Object Storage 활용) |
| **운영 복잡도** | 높음 (JVM 튜닝, 샤드 관리) | 낮음 |
| **Grafana 통합** | 별도 Kibana | 네이티브 통합 |
| **적합한 규모** | 대규모 (일 TB 이상) | 중소규모 ~ 대규모 |

### 7.2 Loki + Spring Boot 구성

```yaml
# docker-compose.yml (Loki)
services:
  loki:
    image: grafana/loki:2.9.0
    ports:
      - "3100:3100"
    volumes:
      - ./loki-config.yml:/etc/loki/config.yml
    command: -config.file=/etc/loki/config.yml

  promtail:
    image: grafana/promtail:2.9.0
    volumes:
      - /var/log/app:/var/log/app
      - ./promtail-config.yml:/etc/promtail/config.yml
    command: -config.file=/etc/promtail/config.yml
```

```yaml
# promtail-config.yml
scrape_configs:
  - job_name: spring-boot
    static_configs:
      - targets: [localhost]
        labels:
          job: spring-boot
          __path__: /var/log/app/*.log
    pipeline_stages:
      - json:
          expressions:
            level: level
            service: service
            traceId: traceId
      - labels:
          level:
          service:
      - timestamp:
          source: "@timestamp"
          format: "2006-01-02T15:04:05.000Z07:00"
```

```promql
# LogQL 쿼리 예시
{service="order-service"} |= "ERROR"
{service="order-service"} | json | level="ERROR" | line_format "{{.message}}"
sum(rate({service="order-service"} |= "ERROR" [5m])) by (service)
```

---

## 요약

| 단계 | 작업 | 핵심 포인트 |
|-----|------|------------|
| 1 | 구조화 로깅 | logstash-logback-encoder로 JSON 출력, MDC 활용 |
| 2 | 수집 | Filebeat DaemonSet 배포, 백프레셔 설정 |
| 3 | 변환 | Logstash에서 필터링, 민감 정보 마스킹 |
| 4 | 저장 | ILM으로 Hot/Warm/Cold 라이프사이클 관리 |
| 5 | 검색 | Kibana Discover + KQL, traceId 기반 추적 |
| 6 | 경량 대안 | Grafana Loki로 비용 절감 가능 |

*마지막 업데이트: 2026년 02월*

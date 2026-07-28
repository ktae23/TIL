# 분산 추적 (Distributed Tracing) - OpenTelemetry와 Zipkin

마이크로서비스 환경에서 요청 흐름을 추적하는 분산 추적 시스템의 구축 방법을 정리한다. 메트릭은 "무엇이 느린가"를, 로그는 "왜 느린가"를, 트레이스는 "어디서 느린가"를 알려준다.

## 목차

1. [분산 추적 개념](#1-분산-추적-개념)
2. [OpenTelemetry 소개](#2-opentelemetry-소개)
3. [Spring Boot + OpenTelemetry 설정](#3-spring-boot--opentelemetry-설정)
4. [Zipkin 구축](#4-zipkin-구축)
5. [Jaeger 대안](#5-jaeger-대안)
6. [Trace Context 전파](#6-trace-context-전파)
7. [실전 활용 패턴](#7-실전-활용-패턴)

---

## 1. 분산 추적 개념

### 1.1 Trace, Span, Context

```
Trace (전체 요청 흐름)
TraceID: abc-123
│
├── Span A: API Gateway (100ms)
│   Parent: none
│   ├── Span B: Order Service (80ms)
│   │   Parent: A
│   │   ├── Span C: Payment Service (40ms)
│   │   │   Parent: B
│   │   │   └── Span D: External PG API (35ms)
│   │   │       Parent: C
│   │   └── Span E: Inventory Service (20ms)
│   │       Parent: B
│   └── Span F: Notification Service (10ms)
│       Parent: B (async)
│
Timeline:
|--A (100ms)------------------------------------------|
  |--B (80ms)--------------------------------------|
    |--C (40ms)----------------|  |--E (20ms)---|
      |--D (35ms)------------|
                                    |--F (10ms)--|
```

### 1.2 핵심 용어

| 용어 | 설명 |
|-----|------|
| **Trace** | 하나의 요청이 여러 서비스를 거치는 전체 경로. 고유한 TraceID로 식별 |
| **Span** | Trace 내의 하나의 작업 단위. 시작 시간, 종료 시간, 속성(attributes) 포함 |
| **SpanContext** | TraceID, SpanID, TraceFlags를 포함하는 전파(propagation) 데이터 |
| **Baggage** | Trace 전체에 걸쳐 전파되는 키-값 쌍 (예: userId, tenantId) |
| **Sampling** | 모든 트레이스를 수집하면 비용이 크므로, 일부만 샘플링 |

### 1.3 Three Pillars of Observability

```
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│   Metrics   │  │    Logs     │  │   Traces    │
│  (무엇이)    │  │  (왜)       │  │  (어디서)    │
│             │  │             │  │             │
│ Prometheus  │  │ ELK / Loki  │  │ Zipkin /    │
│ Grafana     │  │ Kibana      │  │ Jaeger      │
└─────────────┘  └─────────────┘  └─────────────┘
       │                │                │
       └────────────────┼────────────────┘
                        │
                 OpenTelemetry
              (통합 수집 프레임워크)
```

---

## 2. OpenTelemetry 소개

### 2.1 OpenTelemetry란

OpenTelemetry(OTel)는 CNCF에서 관리하는 Observability 표준 프레임워크다. Metrics, Logs, Traces를 하나의 SDK로 통합 수집한다.

### 2.2 아키텍처

```
┌──────────────────────────────────────────────────────────┐
│                    Application                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐               │
│  │  OTel    │  │  OTel    │  │  OTel    │               │
│  │  Traces  │  │  Metrics │  │  Logs    │               │
│  │  SDK     │  │  SDK     │  │  SDK     │               │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘               │
│       └──────────────┼─────────────┘                     │
│                      │ OTLP (OpenTelemetry Protocol)     │
└──────────────────────┼───────────────────────────────────┘
                       ▼
┌──────────────────────────────────────────────────────────┐
│              OpenTelemetry Collector                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐               │
│  │ Receivers│  │Processors│  │ Exporters│               │
│  │ (OTLP,   │─>│ (Batch,  │─>│ (Zipkin, │               │
│  │  Zipkin) │  │  Filter) │  │  Jaeger, │               │
│  └──────────┘  └──────────┘  │  Prom)   │               │
│                              └──────────┘               │
└──────────────────────────────────────────────────────────┘
         │              │              │
         ▼              ▼              ▼
      Zipkin        Prometheus      Elasticsearch
```

### 2.3 계측 방식

| 방식 | 설명 | 장단점 |
|-----|------|--------|
| **Auto Instrumentation** | Java Agent로 자동 계측 | 코드 변경 없음, 세밀한 제어 어려움 |
| **Manual Instrumentation** | SDK로 직접 Span 생성 | 세밀한 제어 가능, 코드 수정 필요 |
| **Spring Boot Starter** | Micrometer Tracing 통합 | Spring 생태계 자연 통합 |

---

## 3. Spring Boot + OpenTelemetry 설정

### 3.1 방법 1: Micrometer Tracing (권장)

Spring Boot 3.x부터 Micrometer Tracing이 표준이다.

```gradle
// build.gradle
dependencies {
    // Micrometer Tracing + Bridge
    implementation 'io.micrometer:micrometer-tracing-bridge-otel'

    // OpenTelemetry Exporter (Zipkin)
    implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'

    // 또는 OTLP Exporter (OTel Collector로 전송)
    // implementation 'io.opentelemetry:opentelemetry-exporter-otlp'

    // Spring Boot Actuator
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
}
```

```yaml
# application.yml
management:
  tracing:
    sampling:
      probability: 1.0  # 개발: 100%, 프로덕션: 0.1 (10%)
    propagation:
      type: w3c  # W3C Trace Context (기본값)

  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans

  # OTLP 사용 시
  # otlp:
  #   tracing:
  #     endpoint: http://otel-collector:4318/v1/traces

spring:
  application:
    name: order-service

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

### 3.2 방법 2: OTel Java Agent (Zero-Code)

코드 변경 없이 Java Agent만으로 자동 계측한다.

```bash
# Agent 다운로드
curl -L -o opentelemetry-javaagent.jar \
  https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

# 실행
java -javaagent:opentelemetry-javaagent.jar \
  -Dotel.service.name=order-service \
  -Dotel.traces.exporter=zipkin \
  -Dotel.exporter.zipkin.endpoint=http://zipkin:9411/api/v2/spans \
  -Dotel.metrics.exporter=prometheus \
  -Dotel.logs.exporter=none \
  -jar app.jar
```

```yaml
# Docker 환경
services:
  order-service:
    image: order-service:latest
    environment:
      JAVA_TOOL_OPTIONS: "-javaagent:/opt/opentelemetry-javaagent.jar"
      OTEL_SERVICE_NAME: "order-service"
      OTEL_TRACES_EXPORTER: "otlp"
      OTEL_EXPORTER_OTLP_ENDPOINT: "http://otel-collector:4317"
      OTEL_METRICS_EXPORTER: "prometheus"
      OTEL_RESOURCE_ATTRIBUTES: "deployment.environment=production"
    volumes:
      - ./opentelemetry-javaagent.jar:/opt/opentelemetry-javaagent.jar
```

### 3.3 커스텀 Span 생성

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final Tracer tracer;  // io.micrometer.tracing.Tracer

    public Order processOrder(OrderRequest request) {
        // 커스텀 Span 생성
        Span newSpan = tracer.nextSpan().name("processOrder");

        try (Tracer.SpanInScope ws = tracer.withSpan(newSpan.start())) {
            // Span에 속성 추가
            newSpan.tag("order.id", request.getOrderId());
            newSpan.tag("order.type", request.getType().name());
            newSpan.tag("order.amount", String.valueOf(request.getAmount()));

            // 비즈니스 로직
            Order order = validateAndCreate(request);

            // 이벤트 추가
            newSpan.event("order.validated");

            processPayment(order);
            newSpan.event("payment.completed");

            return order;
        } catch (Exception e) {
            newSpan.error(e);
            throw e;
        } finally {
            newSpan.end();
        }
    }

    // @NewSpan 어노테이션 활용
    @NewSpan("validateOrder")
    public void validateAndCreate(
            @SpanTag("order.id") String orderId,
            OrderRequest request) {
        // 자동으로 Span 생성 및 종료
    }
}
```

### 3.4 비동기/메시징 환경 트레이스 전파

```java
// Kafka Producer: 트레이스 컨텍스트를 헤더에 주입
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final Tracer tracer;

    public void sendOrderEvent(OrderEvent event) {
        // Micrometer Tracing이 KafkaTemplate을 자동 계측
        // 별도 설정 없이 트레이스 컨텍스트가 Kafka 헤더에 전파됨
        kafkaTemplate.send("order-events", event.getOrderId(), event);
    }
}

// Kafka Consumer: 헤더에서 트레이스 컨텍스트를 추출
@KafkaListener(topics = "order-events")
public void handleOrderEvent(OrderEvent event) {
    // Micrometer Tracing이 자동으로 컨텍스트 복원
    // Consumer Span이 Producer Span의 자식으로 연결됨
    log.info("Processing order event: {}", event.getOrderId());
}
```

---

## 4. Zipkin 구축

### 4.1 Docker 구성

```yaml
services:
  zipkin:
    image: openzipkin/zipkin:latest
    container_name: zipkin
    ports:
      - "9411:9411"
    environment:
      # 저장소 설정 (기본: in-memory)
      - STORAGE_TYPE=elasticsearch
      - ES_HOSTS=http://elasticsearch:9200
      - ES_INDEX=zipkin
      # 또는 MySQL
      # - STORAGE_TYPE=mysql
      # - MYSQL_HOST=mysql
      # - MYSQL_TCP_PORT=3306
      # - MYSQL_USER=zipkin
      # - MYSQL_PASS=zipkin
    networks:
      - monitoring
```

### 4.2 Zipkin UI 활용

```
1. Find Traces: 서비스명, 시간 범위, 최소 지속 시간으로 검색
2. Trace Detail: 각 Span의 시간, 태그, 어노테이션 확인
3. Dependencies: 서비스 간 의존성 그래프 자동 생성
4. Error Traces: 에러가 발생한 Trace만 필터링
```

### 4.3 Zipkin에서 병목 분석

```
TraceID: abc-123 (총 350ms)

order-service: processOrder          |███████████████████████████████| 350ms
  payment-service: chargePayment     |  ████████████████████|          200ms  <- 병목!
    pg-api: external call            |    ██████████████████|          180ms  <- 외부 API
  inventory-service: deductStock     |                       ██|       20ms
  notification-service: sendEmail    |                         █|      10ms (async)

분석:
- 전체 350ms 중 payment-service가 200ms (57%)
- 외부 PG API 호출이 180ms로 실제 병목
- 대응: PG API 타임아웃 설정, 캐싱, 비동기 처리 검토
```

---

## 5. Jaeger 대안

### 5.1 Zipkin vs Jaeger

| 항목 | Zipkin | Jaeger |
|-----|--------|--------|
| **개발** | Twitter 출신 | Uber 출신 (CNCF Graduated) |
| **아키텍처** | 단일 바이너리 | 컴포넌트 분리 (Agent, Collector, Query) |
| **저장소** | Elasticsearch, MySQL, Cassandra | Elasticsearch, Cassandra, ClickHouse |
| **UI** | 심플 | 풍부한 비교/분석 기능 |
| **적합한 환경** | 소규모~중규모 | 중규모~대규모 |
| **OTLP 지원** | 제한적 | 네이티브 |

### 5.2 Jaeger 구성

```yaml
services:
  jaeger:
    image: jaegertracing/all-in-one:latest
    ports:
      - "16686:16686"  # UI
      - "4317:4317"    # OTLP gRPC
      - "4318:4318"    # OTLP HTTP
      - "14250:14250"  # gRPC (Collector)
    environment:
      - SPAN_STORAGE_TYPE=elasticsearch
      - ES_SERVER_URLS=http://elasticsearch:9200
```

---

## 6. Trace Context 전파

### 6.1 W3C Trace Context (표준)

```
# HTTP 헤더로 전파
traceparent: 00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01
             │   │                                │                │
             │   TraceID (128bit)                 SpanID (64bit)   Flags
             Version                                               (sampled)

tracestate: vendor1=value1,vendor2=value2
```

### 6.2 전파 방식 비교

| 방식 | 헤더 | 특징 |
|-----|------|------|
| **W3C Trace Context** | `traceparent`, `tracestate` | 표준, 권장 |
| **B3 (Zipkin)** | `X-B3-TraceId`, `X-B3-SpanId` 등 | 레거시 호환 |
| **B3 Single** | `b3` | B3 단일 헤더 버전 |

```yaml
# application.yml - 여러 전파 방식 동시 지원 (마이그레이션 시)
management:
  tracing:
    propagation:
      type: w3c, b3  # W3C 우선, B3도 지원
```

### 6.3 서비스 간 컨텍스트 전파 확인

```java
// RestTemplate은 자동 계측됨 (별도 설정 불필요)
@Bean
public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder.build();
    // Micrometer Tracing이 자동으로 traceparent 헤더 주입
}

// WebClient도 자동 계측
@Bean
public WebClient webClient(WebClient.Builder builder) {
    return builder.build();
}

// OpenFeign도 자동 계측
@FeignClient(name = "payment-service")
public interface PaymentClient {
    @PostMapping("/api/payments")
    PaymentResponse charge(@RequestBody PaymentRequest request);
}
```

---

## 7. 실전 활용 패턴

### 7.1 샘플링 전략

```yaml
# 프로덕션 환경 샘플링 설정
management:
  tracing:
    sampling:
      probability: 0.1  # 10% 샘플링 (기본)
```

```java
// 조건부 샘플링: 에러 발생 시 100% 수집
@Bean
public Sampler customSampler() {
    return new Sampler() {
        @Override
        public SamplingResult shouldSample(
                Context parentContext,
                String traceId,
                String name,
                SpanKind spanKind,
                Attributes attributes,
                List<LinkData> parentLinks) {

            // 특정 엔드포인트는 항상 수집
            if (name.contains("payment") || name.contains("checkout")) {
                return SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE);
            }

            // 나머지는 10% 샘플링
            return Math.random() < 0.1
                ? SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE)
                : SamplingResult.create(SamplingDecision.DROP);
        }

        @Override
        public String getDescription() {
            return "CustomSampler";
        }
    };
}
```

### 7.2 OTel Collector 설정

```yaml
# otel-collector-config.yml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

processors:
  batch:
    timeout: 5s
    send_batch_size: 1024

  # 테일 샘플링: 에러 트레이스는 100% 수집
  tail_sampling:
    decision_wait: 10s
    policies:
      - name: errors-policy
        type: status_code
        status_code: { status_codes: [ERROR] }
      - name: slow-traces-policy
        type: latency
        latency: { threshold_ms: 5000 }
      - name: default-rate
        type: probabilistic
        probabilistic: { sampling_percentage: 10 }

  # 속성 추가
  attributes:
    actions:
      - key: environment
        value: production
        action: upsert

exporters:
  zipkin:
    endpoint: http://zipkin:9411/api/v2/spans

  otlp/jaeger:
    endpoint: jaeger:4317
    tls:
      insecure: true

  prometheus:
    endpoint: 0.0.0.0:8889

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch, tail_sampling, attributes]
      exporters: [zipkin]
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [prometheus]
```

### 7.3 Trace와 Log 연관

로그에 TraceID를 포함하면 Trace <-> Log 간 빠른 이동이 가능하다.

```yaml
# application.yml - 로그에 traceId 자동 포함
logging:
  pattern:
    level: "%5p [${spring.application.name},%X{traceId},%X{spanId}]"
```

```
# 로그 출력 예시
INFO [order-service,abc123def456,789ghi012] - 주문 처리 시작

# Kibana에서 traceId로 검색 -> Zipkin 링크로 바로 이동
# Zipkin에서 Trace 확인 -> 로그 검색으로 바로 이동
```

### 7.4 Grafana에서 통합 뷰

Grafana의 Explore 기능으로 Metrics -> Traces -> Logs를 하나의 화면에서 연결한다.

```
1. Grafana에 Zipkin/Jaeger 데이터소스 추가
2. Prometheus 패널에서 Exemplar 활성화 -> 메트릭 점에서 Trace로 바로 이동
3. Trace에서 "Logs for this span" -> Loki/Elasticsearch 로그 연결
4. 로그에서 TraceID 클릭 -> 전체 Trace 뷰로 이동
```

---

## 요약

| 항목 | 핵심 포인트 |
|-----|------------|
| 개념 | Trace = 전체 요청 흐름, Span = 개별 작업 단위 |
| 프레임워크 | OpenTelemetry = Metrics + Logs + Traces 통합 표준 |
| Spring Boot 통합 | Micrometer Tracing + OTel Bridge (권장) |
| 백엔드 | Zipkin (심플), Jaeger (대규모) |
| 전파 | W3C Trace Context 표준 사용 |
| 샘플링 | 프로덕션 10%, 에러/느린 요청은 100% |
| 통합 | Trace <-> Log (traceId), Trace <-> Metric (Exemplar) |

*마지막 업데이트: 2026년 02월*

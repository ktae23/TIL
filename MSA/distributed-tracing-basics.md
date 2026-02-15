# 분산 추적 기초

## 목차
1. [분산 추적 개념](#분산-추적-개념)
2. [TraceId와 SpanId](#traceid와-spanid)
3. [Zipkin](#zipkin)
4. [OpenTelemetry](#opentelemetry)
5. [Spring Boot 통합](#spring-boot-통합)
6. [핵심 정리](#핵심-정리)

---

## 분산 추적 개념

### 왜 분산 추적이 필요한가?

```
┌──────────────────────────────────────────────────────────────────┐
│                    분산 시스템 디버깅 문제                        │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  단일 요청이 여러 서비스를 거침:                                  │
│                                                                   │
│  Client ─► API Gateway ─► Order Service ─► Inventory Service    │
│                              │                                    │
│                              └──► Payment Service ─► Bank API    │
│                                                                   │
│  문제 발생 시:                                                    │
│  - 어느 서비스에서 지연이 발생했는가?                            │
│  - 에러의 근본 원인은 어디인가?                                  │
│  - 요청 흐름을 어떻게 추적하는가?                                │
│                                                                   │
│  해결: 분산 추적 (Distributed Tracing)                           │
│  - 단일 요청에 고유 ID 부여                                      │
│  - 서비스 간 ID 전파                                             │
│  - 중앙에서 수집 및 시각화                                       │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 분산 추적의 3대 축

```
┌──────────────────────────────────────────────────────────────────┐
│                    Observability 3 Pillars                        │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  1. Logs (로그)                                                  │
│     - 개별 이벤트 기록                                           │
│     - 상세 컨텍스트 정보                                         │
│     - 예: "User 123 order created at 2025-01-15 10:30:00"       │
│                                                                   │
│  2. Metrics (메트릭)                                             │
│     - 수치화된 측정값                                            │
│     - 시간에 따른 집계                                           │
│     - 예: request_count, latency_p99                             │
│                                                                   │
│  3. Traces (추적)                                                │
│     - 요청의 전체 경로 추적                                      │
│     - 서비스 간 관계 시각화                                      │
│     - 예: Request A → Service B → Service C                      │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

---

## TraceId와 SpanId

### 개념 설명

```
┌──────────────────────────────────────────────────────────────────┐
│                    Trace와 Span 구조                              │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Trace: 하나의 요청에 대한 전체 경로                             │
│  Span: Trace 내의 개별 작업 단위                                 │
│                                                                   │
│  TraceId: abc123 (전체 요청에서 동일)                            │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                                                              │  │
│  │  Span A (SpanId: 001, ParentId: null) - API Gateway        │  │
│  │  ├─────────────────────────────────────────────────────────│  │
│  │  │                                                          │  │
│  │  │  Span B (SpanId: 002, ParentId: 001) - Order Service    │  │
│  │  │  ├──────────────────────────────────────────────────────│  │
│  │  │  │                                                       │  │
│  │  │  │  Span C (SpanId: 003, ParentId: 002) - DB Query      │  │
│  │  │  │  └──────────────────────────────────────────────────│  │
│  │  │  │                                                       │  │
│  │  │  │  Span D (SpanId: 004, ParentId: 002) - Inventory RPC │  │
│  │  │  │  └──────────────────────────────────────────────────│  │
│  │  │  └──────────────────────────────────────────────────────│  │
│  │  │                                                          │  │
│  │  │  Span E (SpanId: 005, ParentId: 001) - Payment Service  │  │
│  │  │  └──────────────────────────────────────────────────────│  │
│  │  └─────────────────────────────────────────────────────────│  │
│  │                                                              │  │
│  └────────────────────────────────────────────────────────────┘  │
│  ├──────────────────────────────────────────────────────────────►│
│  0ms               50ms             100ms            150ms       │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Span 속성

```java
// Span의 주요 속성
public class Span {
    private String traceId;      // 전체 추적 ID
    private String spanId;       // 현재 Span ID
    private String parentSpanId; // 부모 Span ID
    private String name;         // Span 이름 (예: "HTTP GET /orders")
    private long startTime;      // 시작 시간 (마이크로초)
    private long duration;       // 소요 시간
    private SpanKind kind;       // CLIENT, SERVER, PRODUCER, CONSUMER
    private Map<String, String> tags;      // 태그 (key-value)
    private List<LogEntry> logs;           // 로그 이벤트
    private SpanStatus status;             // OK, ERROR
}

// SpanKind
public enum SpanKind {
    CLIENT,    // RPC 클라이언트 측
    SERVER,    // RPC 서버 측
    PRODUCER,  // 메시지 발행
    CONSUMER,  // 메시지 소비
    INTERNAL   // 내부 작업
}
```

### Context Propagation (컨텍스트 전파)

```
HTTP 헤더로 전파 (W3C Trace Context 표준):

Request Headers:
┌──────────────────────────────────────────────────────────────┐
│ traceparent: 00-abc123def456-001-01                          │
│              │  │              │   │                          │
│              │  │              │   └── flags (sampled)        │
│              │  │              └────── parent-id              │
│              │  └───────────────────── trace-id               │
│              └──────────────────────── version                │
│                                                               │
│ tracestate: vendor1=value1,vendor2=value2                     │
│             (벤더별 추가 정보)                                 │
└──────────────────────────────────────────────────────────────┘

Kafka 메시지로 전파:
┌──────────────────────────────────────────────────────────────┐
│ Headers:                                                      │
│   traceparent: 00-abc123def456-001-01                        │
│   b3: abc123def456-001-1                                     │
└──────────────────────────────────────────────────────────────┘
```

---

## Zipkin

### Zipkin 아키텍처

```
┌──────────────────────────────────────────────────────────────────┐
│                    Zipkin 아키텍처                                │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌───────────────────────────────────────────────────────────┐   │
│  │                     Applications                           │   │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐       │   │
│  │  │Service A│  │Service B│  │Service C│  │Service D│       │   │
│  │  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘       │   │
│  └───────┼────────────┼────────────┼────────────┼────────────┘   │
│          │            │            │            │                 │
│          └────────────┴──────┬─────┴────────────┘                 │
│                              ▼                                    │
│                    ┌─────────────────┐                           │
│                    │   Collector     │  ← 데이터 수집             │
│                    └────────┬────────┘                           │
│                             │                                     │
│                             ▼                                     │
│                    ┌─────────────────┐                           │
│                    │    Storage      │  ← 저장소                  │
│                    │ (Memory/MySQL/  │    (Elasticsearch,        │
│                    │  Elasticsearch) │     Cassandra 등)         │
│                    └────────┬────────┘                           │
│                             │                                     │
│                             ▼                                     │
│                    ┌─────────────────┐                           │
│                    │   Zipkin UI     │  ← 시각화                  │
│                    └─────────────────┘                           │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Spring Boot + Zipkin 설정

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  application:
    name: order-service

management:
  tracing:
    sampling:
      probability: 1.0  # 100% 샘플링 (운영에서는 낮게)
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

```java
// 로그에 TraceId 포함
@Slf4j
@RestController
public class OrderController {

    @GetMapping("/orders/{id}")
    public Order getOrder(@PathVariable Long id) {
        log.info("Getting order: {}", id);
        // 로그 출력: INFO [order-service,abc123,001] Getting order: 123
        return orderService.findById(id);
    }
}
```

---

## OpenTelemetry

### OpenTelemetry 개요

```
┌──────────────────────────────────────────────────────────────────┐
│                    OpenTelemetry 구조                             │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  OpenTelemetry = 통합 Observability 표준                         │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    OTel SDK                                  │ │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐                      │ │
│  │  │ Traces  │  │ Metrics │  │  Logs   │                      │ │
│  │  └────┬────┘  └────┬────┘  └────┬────┘                      │ │
│  │       │            │            │                            │ │
│  │       └────────────┼────────────┘                            │ │
│  │                    ▼                                         │ │
│  │           ┌────────────────┐                                 │ │
│  │           │   Exporters    │                                 │ │
│  │           └────────────────┘                                 │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                         │                                         │
│         ┌───────────────┼───────────────┐                        │
│         ▼               ▼               ▼                        │
│    ┌─────────┐    ┌─────────┐    ┌─────────┐                    │
│    │ Jaeger  │    │ Zipkin  │    │Prometheus│                   │
│    └─────────┘    └─────────┘    └─────────┘                    │
│                                                                   │
│  벤더 중립적: 다양한 백엔드로 내보내기 가능                       │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Spring Boot 3 + OpenTelemetry

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

```yaml
# application.yml
management:
  tracing:
    sampling:
      probability: 1.0

  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces

otel:
  exporter:
    otlp:
      endpoint: http://localhost:4317
  resource:
    attributes:
      service.name: order-service
      service.version: 1.0.0
      deployment.environment: production
```

### 커스텀 Span 생성

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final Tracer tracer;  // io.micrometer.tracing.Tracer
    private final OrderRepository orderRepository;

    public Order createOrder(OrderRequest request) {
        // 수동 Span 생성
        Span newSpan = tracer.nextSpan().name("createOrder");

        try (Tracer.SpanInScope ws = tracer.withSpan(newSpan.start())) {
            // Span에 태그 추가
            newSpan.tag("order.customer_id", request.getCustomerId().toString());
            newSpan.tag("order.item_count", String.valueOf(request.getItems().size()));

            // 이벤트 기록
            newSpan.event("Validating order");

            validateOrder(request);

            newSpan.event("Creating order entity");

            Order order = Order.create(request);
            Order saved = orderRepository.save(order);

            newSpan.tag("order.id", saved.getId().toString());

            return saved;

        } catch (Exception e) {
            newSpan.error(e);
            throw e;
        } finally {
            newSpan.end();
        }
    }

    // 어노테이션 기반 Span (Spring)
    @NewSpan("validateOrder")
    public void validateOrder(@SpanTag("request") OrderRequest request) {
        // 자동으로 Span 생성
        if (request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must have items");
        }
    }
}
```

---

## Spring Boot 통합

### 완전한 설정 예시

```yaml
# application.yml
spring:
  application:
    name: order-service

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  tracing:
    sampling:
      probability: 1.0  # 개발: 1.0, 운영: 0.1
    propagation:
      type: w3c  # W3C Trace Context

  # Zipkin 사용 시
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
  level:
    io.micrometer.tracing: DEBUG
```

### RestTemplate/WebClient 자동 추적

```java
@Configuration
public class TracingConfig {

    // RestTemplate - 자동으로 헤더 전파
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
        // Spring Boot 3.x에서 자동으로 TracingInterceptor 적용
    }

    // WebClient - 자동으로 헤더 전파
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
        // 자동으로 TracingExchangeFilterFunction 적용
    }
}
```

### Kafka 메시지 추적

```java
@Configuration
public class KafkaTracingConfig {

    @Bean
    public ProducerFactory<String, Object> producerFactory(
            ObservationRegistry observationRegistry) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, Object> factory =
            new DefaultKafkaProducerFactory<>(config);

        // 추적 활성화
        factory.addListener(new MicrometerProducerListener<>(meterRegistry));

        return factory;
    }
}

// 메시지 발행 시 자동으로 추적 헤더 추가
@Service
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void publish(OrderEvent event) {
        // 자동으로 traceparent 헤더가 추가됨
        kafkaTemplate.send("order-events", event.getOrderId(), event);
    }
}
```

### 로그 상관관계 (Log Correlation)

```java
// MDC에 자동으로 traceId, spanId 추가됨
// Logback 설정
// logback-spring.xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>
                %d{HH:mm:ss.SSS} [%thread] %-5level [%X{traceId:-},%X{spanId:-}]
                %logger{36} - %msg%n
            </pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
    </root>
</configuration>

// 출력 예시:
// 10:30:15.123 [http-nio-8080-exec-1] INFO  [abc123,001] c.e.OrderController - Creating order
// 10:30:15.150 [http-nio-8080-exec-1] INFO  [abc123,002] c.e.OrderService - Order validated
// 10:30:15.200 [http-nio-8080-exec-1] INFO  [abc123,003] c.e.PaymentClient - Payment processed
```

---

## 핵심 정리

### 분산 추적 도구 비교

| 도구 | 특징 | 언어 지원 | 저장소 |
|------|------|----------|--------|
| Zipkin | 간단, 가벼움 | 다양 | Memory, MySQL, ES |
| Jaeger | 대규모, 고성능 | 다양 | Cassandra, ES |
| OpenTelemetry | 표준, 벤더 중립 | 다양 | 다양한 백엔드 지원 |

### 설정 체크리스트

```
□ 서비스 이름 설정 (spring.application.name)
□ 샘플링 비율 설정 (운영: 0.1~0.5)
□ 추적 백엔드 연결 (Zipkin/Jaeger)
□ 로그 패턴에 traceId 포함
□ HTTP 클라이언트 추적 설정
□ 메시지 큐 추적 설정
□ 커스텀 Span 필요 시 추가
```

### 실무 기반 핵심 질문

1. **Q: TraceId와 SpanId의 차이점은?**
   - A: TraceId는 전체 요청 흐름을 식별하는 고유 ID (모든 서비스에서 동일). SpanId는 Trace 내 개별 작업 단위 식별. ParentSpanId로 계층 구조 표현

2. **Q: 분산 추적에서 Context Propagation이란?**
   - A: 서비스 간 요청 시 추적 정보(TraceId, SpanId)를 전달하는 것. HTTP 헤더(traceparent), Kafka 헤더 등으로 전파. W3C Trace Context가 표준

3. **Q: 샘플링이 필요한 이유는?**
   - A: 모든 요청 추적 시 스토리지/네트워크 비용 증가. 운영 환경에서는 10~50% 샘플링으로 충분한 가시성 확보하면서 비용 절감

4. **Q: OpenTelemetry의 장점은?**
   - A: 벤더 중립적 표준으로 다양한 백엔드(Zipkin, Jaeger, Datadog 등) 지원. Traces, Metrics, Logs 통합. 한 번 계측으로 여러 백엔드 사용 가능

---

*마지막 업데이트: 2026년 01월*

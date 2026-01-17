# MSA 트러블슈팅 가이드

마이크로서비스 아키텍처에서 발생하는 문제를 디버깅하고 해결하는 방법을 정리합니다.

## 목차

1. [분산 시스템 디버깅](#1-분산-시스템-디버깅)
2. [로그 집계 및 분석](#2-로그-집계-및-분석)
3. [분산 트레이싱](#3-분산-트레이싱)
4. [서비스 장애 대응](#4-서비스-장애-대응)
5. [카오스 엔지니어링](#5-카오스-엔지니어링)
6. [모니터링 및 알림](#6-모니터링-및-알림)

---

## 1. 분산 시스템 디버깅

### 분산 시스템의 어려움

```
단일 서버:
사용자 → 서버 → DB
로그: 한 곳에서 확인

마이크로서비스:
사용자 → API Gateway → Order Service → Stock Service → Payment Service
                           ↓               ↓              ↓
                        Order DB        Stock DB      Payment DB

문제: 어느 서비스에서 오류가 발생했는지 추적 어려움
```

### Correlation ID 패턴

```java
// Correlation ID 필터
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        // MDC에 설정 (로그에 자동 포함)
        MDC.put("correlationId", correlationId);

        // 응답 헤더에 추가
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
        }
    }
}

// 다른 서비스 호출 시 전달
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            String correlationId = MDC.get("correlationId");
            if (correlationId != null) {
                request.getHeaders().set("X-Correlation-ID", correlationId);
            }
            return execution.execute(request, body);
        });
        return restTemplate;
    }
}
```

### Logback 설정

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>
                %d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{correlationId}] %-5level %logger{36} - %msg%n
            </pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

### 로그 예시

```
// Order Service
2024-01-15 10:30:00.123 [http-nio-8080-exec-1] [abc-123-def] INFO OrderController - 주문 생성 시작

// Stock Service
2024-01-15 10:30:00.456 [http-nio-8081-exec-3] [abc-123-def] INFO StockService - 재고 확인

// Payment Service
2024-01-15 10:30:00.789 [http-nio-8082-exec-2] [abc-123-def] ERROR PaymentService - 결제 실패

// 같은 correlationId로 요청 추적 가능!
```

---

## 2. 로그 집계 및 분석

### ELK Stack 구성

```
┌─────────────────────────────────────────────────────────────┐
│                        ELK Stack                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌──────────┐     ┌──────────────┐     ┌──────────────┐   │
│   │ Service  │────→│   Logstash   │────→│ Elasticsearch│   │
│   │  Logs    │     │  (수집/변환) │     │   (저장)     │   │
│   └──────────┘     └──────────────┘     └──────────────┘   │
│                                                  │          │
│                                           ┌──────▼──────┐   │
│                                           │   Kibana    │   │
│                                           │   (시각화)  │   │
│                                           └─────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Filebeat 설정 (로그 수집)

```yaml
# filebeat.yml
filebeat.inputs:
- type: container
  paths:
    - '/var/lib/docker/containers/*/*.log'
  processors:
  - add_kubernetes_metadata:
      host: ${NODE_NAME}

output.elasticsearch:
  hosts: ["elasticsearch:9200"]
  indices:
    - index: "logs-%{[kubernetes.namespace]}-%{+yyyy.MM.dd}"
```

### Logstash 필터 설정

```ruby
# logstash.conf
input {
  beats {
    port => 5044
  }
}

filter {
  grok {
    match => {
      "message" => "%{TIMESTAMP_ISO8601:timestamp} \[%{DATA:thread}\] \[%{DATA:correlationId}\] %{LOGLEVEL:level} %{DATA:logger} - %{GREEDYDATA:msg}"
    }
  }

  date {
    match => [ "timestamp", "yyyy-MM-dd HH:mm:ss.SSS" ]
    target => "@timestamp"
  }

  # 서비스 이름 추출
  mutate {
    add_field => { "service" => "%{[kubernetes][labels][app]}" }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "logs-%{service}-%{+YYYY.MM.dd}"
  }
}
```

### Kibana 쿼리 예시

```
# Correlation ID로 요청 추적
correlationId: "abc-123-def"

# 에러 로그만 필터
level: ERROR AND service: payment-service

# 특정 시간대 슬로우 로그
level: WARN AND msg: "slow" AND @timestamp >= "2024-01-15T10:00:00"

# 특정 사용자 요청 추적
msg: "userId=12345"
```

---

## 3. 분산 트레이싱

### OpenTelemetry + Jaeger 구성

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  User Request (TraceId: T1)                                 │
│       │                                                     │
│  ┌────▼────────────────────────────────────────────────┐   │
│  │  API Gateway                                         │   │
│  │  SpanId: A, ParentSpan: null                        │   │
│  └────┬────────────────────────────────────────────────┘   │
│       │                                                     │
│  ┌────▼────────────────────────────────────────────────┐   │
│  │  Order Service                                       │   │
│  │  SpanId: B, ParentSpan: A                           │   │
│  └────┬──────────────────┬─────────────────────────────┘   │
│       │                  │                                  │
│  ┌────▼───────┐    ┌─────▼─────────┐                       │
│  │ Stock Svc  │    │ Payment Svc   │                       │
│  │ SpanId: C  │    │ SpanId: D     │                       │
│  │ Parent: B  │    │ Parent: B     │                       │
│  └────────────┘    └───────────────┘                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │     Jaeger      │
                    │  (Trace 저장)   │
                    └─────────────────┘
```

### Spring Boot + Micrometer Tracing 설정

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
      probability: 1.0  # 100% 샘플링 (운영에서는 낮춤)
  otlp:
    tracing:
      endpoint: http://jaeger:4318/v1/traces
```

### 커스텀 Span 추가

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final Tracer tracer;

    public Order createOrder(OrderRequest request) {
        // 새 Span 시작
        Span span = tracer.nextSpan().name("processOrder").start();

        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            // 태그 추가
            span.tag("order.userId", request.getUserId().toString());
            span.tag("order.amount", request.getAmount().toString());

            // 비즈니스 로직
            Order order = processOrderInternal(request);

            // 이벤트 기록
            span.event("order.created");

            return order;

        } catch (Exception e) {
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### Jaeger UI 활용

```
1. Trace 검색
   - Service: order-service
   - Operation: POST /api/orders
   - Tags: error=true

2. Trace 상세 보기
   - 각 Span의 시작/종료 시간
   - Span 간 호출 관계
   - 오류 발생 지점
   - 지연 시간 분석

3. 서비스 의존성 맵
   - 서비스 간 호출 관계 시각화
   - 호출 빈도, 오류율 표시
```

---

## 4. 서비스 장애 대응

### 장애 패턴별 대응

**1. 단일 서비스 장애**
```
증상: 특정 서비스 응답 없음
원인: OOM, 무한 루프, DB 연결 실패

대응:
1. Health Check 확인
   curl http://service:8080/actuator/health

2. 로그 확인
   kubectl logs pod-name -f

3. 리소스 확인
   kubectl top pod pod-name

4. Pod 재시작 (임시 조치)
   kubectl rollout restart deployment/service-name
```

**2. 연쇄 장애 (Cascading Failure)**
```
증상: 하나의 서비스 장애가 다른 서비스로 전파
원인: 타임아웃 미설정, Circuit Breaker 없음

대응:
1. 문제 서비스 격리
   - 트래픽 차단
   - 해당 서비스로의 요청 Fallback 처리

2. Circuit Breaker 상태 확인
   /actuator/circuitbreakers

3. 부하 분산
   - 스케일 아웃
   - Rate Limiting 강화
```

**3. 데이터 정합성 문제**
```
증상: 서비스 간 데이터 불일치
원인: 분산 트랜잭션 실패, 이벤트 유실

대응:
1. 이벤트 재처리
   - Dead Letter Queue 확인
   - 수동 이벤트 재발행

2. 데이터 보정
   - 보상 트랜잭션 실행
   - 수동 데이터 동기화

3. 원인 분석
   - 트레이스 로그 확인
   - 이벤트 순서 확인
```

### 장애 대응 체크리스트

```bash
# 1. 현재 상태 파악
kubectl get pods -A | grep -v Running
kubectl get events --sort-by='.lastTimestamp'

# 2. 로그 수집
kubectl logs -l app=order-service --tail=1000 > order-logs.txt

# 3. 리소스 확인
kubectl top pods
kubectl describe pod <pod-name>

# 4. 네트워크 확인
kubectl exec -it <pod-name> -- curl -v http://other-service:8080/health

# 5. DB 연결 확인
kubectl exec -it <pod-name> -- nc -zv database-host 3306
```

---

## 5. 카오스 엔지니어링

### 목적

```
"시스템이 예상대로 동작하는지 실험으로 검증"

예:
- 서비스 하나가 죽으면 어떻게 되나?
- 네트워크 지연이 발생하면?
- CPU/메모리가 부족하면?
```

### Chaos Mesh (Kubernetes)

```yaml
# pod-kill-experiment.yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: PodChaos
metadata:
  name: pod-failure-example
spec:
  action: pod-kill
  mode: one
  selector:
    namespaces:
      - production
    labelSelectors:
      "app": "order-service"
  scheduler:
    cron: "@every 1h"
```

```yaml
# network-delay-experiment.yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: NetworkChaos
metadata:
  name: network-delay
spec:
  action: delay
  mode: all
  selector:
    namespaces:
      - production
    labelSelectors:
      "app": "payment-service"
  delay:
    latency: "500ms"
    jitter: "100ms"
  duration: "5m"
```

### Chaos 실험 시나리오

```
1. Pod 장애
   - 랜덤 Pod 종료
   - 예상: Auto-restart, 요청 재라우팅

2. 네트워크 지연
   - 특정 서비스 간 500ms 지연
   - 예상: Timeout 처리, Circuit Breaker 동작

3. CPU/Memory 스트레스
   - 리소스 고갈 상황
   - 예상: 스케일 아웃, 알림 발생

4. DNS 장애
   - 서비스 디스커버리 실패
   - 예상: Fallback, 캐시된 엔드포인트 사용
```

### 실험 결과 분석

```
실험 전:
□ 성공 기준 정의
□ 모니터링 대시보드 준비
□ 롤백 절차 확인

실험 중:
□ 메트릭 모니터링 (지연시간, 에러율)
□ 로그 확인
□ 사용자 영향 확인

실험 후:
□ 발견된 문제점 기록
□ 개선 사항 도출
□ 시스템 강화 계획
```

---

## 6. 모니터링 및 알림

### 핵심 메트릭 (RED Method)

```
R - Rate: 초당 요청 수
E - Errors: 에러율
D - Duration: 응답 시간

+ 리소스 메트릭:
- CPU 사용률
- 메모리 사용률
- 네트워크 I/O
```

### Prometheus + Grafana

```yaml
# 서비스 메트릭 노출
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    tags:
      application: ${spring.application.name}
```

### 알림 규칙 예시

```yaml
# prometheus-rules.yml
groups:
- name: microservices
  rules:
  # 에러율 높음
  - alert: HighErrorRate
    expr: |
      sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
      / sum(rate(http_server_requests_seconds_count[5m])) > 0.05
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "High error rate detected"
      description: "Error rate is {{ $value | humanizePercentage }}"

  # 응답 시간 느림
  - alert: SlowResponseTime
    expr: |
      histogram_quantile(0.95,
        sum(rate(http_server_requests_seconds_bucket[5m])) by (le, service)
      ) > 2
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "Slow response time"

  # 서비스 다운
  - alert: ServiceDown
    expr: up == 0
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "Service {{ $labels.instance }} is down"

  # Circuit Breaker Open
  - alert: CircuitBreakerOpen
    expr: resilience4j_circuitbreaker_state{state="open"} == 1
    for: 1m
    labels:
      severity: warning
    annotations:
      summary: "Circuit breaker is open for {{ $labels.name }}"
```

### 대시보드 구성

```
┌─────────────────────────────────────────────────────────────┐
│                    Service Overview                         │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │ Request/sec │  │  Error Rate │  │   P95 시간  │        │
│  │   1,234     │  │    0.5%     │  │   120ms     │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Request Rate Over Time                  │   │
│  │  📈 ~~~~~~~~~~~~~~~~~~~~~~~~~~~                      │   │
│  └─────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Error Rate by Service                   │   │
│  │  order-service:  ██░░░ 2%                           │   │
│  │  stock-service:  █░░░░ 0.5%                         │   │
│  │  payment-service: ███░░ 3%                          │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## 트러블슈팅 플로우차트

```
문제 발생
    │
    ├── 알림 확인 (어떤 서비스?)
    │
    ├── 메트릭 확인 (에러율, 지연시간)
    │
    ├── 로그 검색 (Correlation ID로 추적)
    │       │
    │       └── ELK에서 correlationId 검색
    │
    ├── 트레이스 분석 (Jaeger)
    │       │
    │       └── 어느 Span에서 오류?
    │
    ├── 근본 원인 파악
    │       │
    │       ├── 코드 문제?
    │       ├── 인프라 문제?
    │       └── 외부 의존성?
    │
    └── 해결 및 모니터링
```

---

*마지막 업데이트: 2025년 01월*

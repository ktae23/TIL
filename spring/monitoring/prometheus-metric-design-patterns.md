# Prometheus 메트릭 설계 패턴

Spring Boot 애플리케이션에서 Prometheus 메트릭을 효과적으로 설계하고 운영하기 위한 패턴과 안티패턴을 정리한다.

## 목차

1. [메트릭 타입 심화](#1-메트릭-타입-심화)
2. [네이밍 컨벤션](#2-네이밍-컨벤션)
3. [레이블 설계 전략](#3-레이블-설계-전략)
4. [카디널리티 관리](#4-카디널리티-관리)
5. [비즈니스 메트릭 설계](#5-비즈니스-메트릭-설계)
6. [PromQL 고급 패턴](#6-promql-고급-패턴)
7. [안티패턴과 회피 방법](#7-안티패턴과-회피-방법)

---

## 1. 메트릭 타입 심화

### 1.1 각 타입의 올바른 사용 시나리오

| 타입 | 특성 | 올바른 사용 | 잘못된 사용 |
|-----|------|------------|------------|
| **Counter** | 단조 증가만 가능 | 요청 수, 에러 수, 처리된 바이트 | 현재 큐 크기, 온도 |
| **Gauge** | 증가/감소 모두 가능 | 현재 연결 수, 메모리 사용량, 큐 크기 | 총 요청 수 |
| **Histogram** | 값 분포를 버킷으로 관찰 | 응답 시간, 요청 크기 | 고카디널리티 이벤트 |
| **Summary** | 클라이언트 측 분위수 계산 | 정확한 분위수가 필요할 때 | 집계가 필요한 분산 환경 |

### 1.2 Histogram vs Summary 선택 기준

```java
// Histogram: 서버 측에서 버킷 기반 집계 가능, 여러 인스턴스 합산 가능
Timer.builder("http.request.duration")
    .publishPercentileHistogram()  // Histogram 사용
    .serviceLevelObjectives(
        Duration.ofMillis(100),
        Duration.ofMillis(250),
        Duration.ofMillis(500),
        Duration.ofSeconds(1),
        Duration.ofSeconds(5)
    )
    .register(registry);

// Summary: 클라이언트에서 정확한 분위수 계산, 인스턴스 간 합산 불가
Timer.builder("http.request.duration")
    .publishPercentiles(0.5, 0.95, 0.99)  // Summary 사용
    .register(registry);
```

**실무 권장**: 대부분의 경우 **Histogram**을 선택한다.
- 여러 인스턴스의 메트릭을 `histogram_quantile()`로 합산 가능
- SLO 기반 버킷을 미리 정의하여 `rate()` 기반 쿼리 가능
- Summary는 단일 인스턴스에서 정확한 분위수가 필요한 특수한 경우에만 사용

### 1.3 Histogram 버킷 설계

```java
@Bean
public MeterRegistryCustomizer<PrometheusMeterRegistry> histogramConfig() {
    return registry -> registry.config()
        .meterFilter(new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id,
                    DistributionStatisticConfig config) {
                if (id.getName().startsWith("http.server.requests")) {
                    return DistributionStatisticConfig.builder()
                        // API 응답 시간에 적합한 버킷
                        .serviceLevelObjectives(
                            Duration.ofMillis(50).toNanos(),
                            Duration.ofMillis(100).toNanos(),
                            Duration.ofMillis(250).toNanos(),
                            Duration.ofMillis(500).toNanos(),
                            Duration.ofSeconds(1).toNanos(),
                            Duration.ofSeconds(2).toNanos(),
                            Duration.ofSeconds(5).toNanos()
                        )
                        .build()
                        .merge(config);
                }
                return config;
            }
        });
}
```

**버킷 설계 원칙**:
- SLO 경계값을 반드시 포함한다 (예: SLO가 500ms이면 500ms 버킷 필수)
- 버킷 수는 10~15개 이내로 유지한다 (더 많으면 메모리/저장 비용 증가)
- 지수 간격(exponential)으로 배치하되, SLO 근처에서 더 세밀하게 설정한다

---

## 2. 네이밍 컨벤션

### 2.1 Prometheus 공식 네이밍 규칙

```
# 형식: <namespace>_<subsystem>_<name>_<unit>

# 좋은 예
http_requests_total            # Counter: _total 접미사
http_request_duration_seconds  # Histogram: 단위 접미사
node_memory_usage_bytes        # Gauge: 단위 접미사
process_open_fds               # Gauge: 단위 없는 경우 생략

# 나쁜 예
http_requests                  # Counter에 _total 누락
request_latency_ms             # 밀리초 대신 초(seconds)를 사용
httpRequestDuration            # camelCase 사용 금지
```

### 2.2 Micrometer에서의 네이밍

Micrometer는 자동으로 Prometheus 네이밍 컨벤션으로 변환한다:

```java
// Micrometer 코드 (dot.case)
Timer.builder("order.processing.duration")
    .register(registry);

// Prometheus에서 노출되는 이름 (snake_case + 단위)
// order_processing_duration_seconds_bucket
// order_processing_duration_seconds_count
// order_processing_duration_seconds_sum
```

### 2.3 네임스페이스 전략

```java
// 인프라 메트릭: 시스템 이름 기반
"jvm.memory.used"          // jvm_memory_used_bytes
"system.cpu.usage"         // system_cpu_usage

// 애플리케이션 메트릭: 도메인 기반
"order.created.total"      // order_created_total
"payment.processed.total"  // payment_processed_total
"inventory.stock.current"  // inventory_stock_current

// 외부 연동 메트릭: 대상 시스템 기반
"external.api.call.duration"   // external_api_call_duration_seconds
"cache.redis.hit.total"        // cache_redis_hit_total
```

---

## 3. 레이블 설계 전략

### 3.1 효과적인 레이블 설계

```java
// 좋은 레이블 설계: 낮은 카디널리티, 의미 있는 분류
Counter.builder("http.requests")
    .tag("method", "GET")              // HTTP 메서드 (5~7개 값)
    .tag("status", "200")             // 상태 코드 그룹 (5개 값)
    .tag("uri", "/api/orders")        // 정규화된 URI 패턴
    .tag("outcome", "SUCCESS")        // 결과 (SUCCESS, CLIENT_ERROR, SERVER_ERROR)
    .register(registry);

// 나쁜 레이블 설계: 높은 카디널리티
Counter.builder("http.requests")
    .tag("userId", userId)            // 사용자별 고유값 -> 카디널리티 폭발!
    .tag("requestId", requestId)      // 요청별 고유값 -> 절대 금지!
    .tag("uri", "/api/users/12345")   // 정규화되지 않은 URI -> 경로변수 포함!
    .register(registry);
```

### 3.2 URI 정규화 패턴

```java
@Bean
public WebMvcTagsContributor customTagsContributor() {
    return new WebMvcTagsContributor() {
        @Override
        public Iterable<Tag> getTags(HttpServletRequest request,
                HttpServletResponse response, Object handler, Throwable exception) {
            // Spring MVC가 자동으로 URI 패턴 추출
            // /api/users/12345 -> /api/users/{id}
            return Tags.empty();
        }

        @Override
        public Iterable<Tag> getLongRequestTags(HttpServletRequest request,
                Object handler) {
            return Tags.empty();
        }
    };
}

// 고카디널리티 URI를 그룹핑하는 필터
@Bean
public MeterFilter uriNormalizationFilter() {
    return MeterFilter.replaceTagValues("uri", actualUri -> {
        if (actualUri.startsWith("/api/users/")) {
            return "/api/users/{id}";
        }
        if (actualUri.equals("UNKNOWN") || actualUri.equals("root")) {
            return "UNKNOWN";
        }
        return actualUri;
    });
}
```

### 3.3 상태 코드 그룹핑

```java
@Bean
public MeterFilter statusGroupFilter() {
    return MeterFilter.replaceTagValues("status", status -> {
        // 개별 상태 코드 대신 그룹으로 집계할 수도 있음
        if (status.startsWith("2")) return "2xx";
        if (status.startsWith("3")) return "3xx";
        if (status.startsWith("4")) return "4xx";
        if (status.startsWith("5")) return "5xx";
        return status;
    });
}
```

---

## 4. 카디널리티 관리

### 4.1 카디널리티 폭발의 위험

카디널리티 = 레이블 값 조합의 총 수

```
# 예시: 3개 레이블
method: 5개 값 (GET, POST, PUT, DELETE, PATCH)
uri: 20개 값 (정규화된 엔드포인트)
status: 5개 값 (200, 400, 404, 500, 503)

총 시계열 수 = 5 x 20 x 5 = 500개 -> 관리 가능

# 위험한 예시
method: 5개 값
uri: 1000개 값 (정규화 안 된 경우)
userId: 100,000개 값

총 시계열 수 = 5 x 1000 x 100,000 = 500,000,000개 -> 시스템 장애!
```

### 4.2 카디널리티 제한 설정

```java
@Bean
public MeterFilter cardinalityLimiter() {
    return MeterFilter.maximumAllowableTags("http.server.requests", "uri", 100,
        MeterFilter.deny());
}

// 특정 메트릭 완전 차단
@Bean
public MeterFilter denyFilter() {
    return MeterFilter.deny(id -> {
        String name = id.getName();
        // 불필요한 메트릭 차단
        return name.startsWith("jvm.gc.memory") ||
               name.startsWith("logback.events");
    });
}
```

### 4.3 카디널리티 모니터링

```promql
# 메트릭별 시계열 수 확인
count by (__name__) ({__name__=~".+"})

# 카디널리티가 높은 상위 10개 메트릭
topk(10, count by (__name__) ({__name__=~".+"}))

# 특정 메트릭의 레이블 조합 수
count(http_server_requests_seconds_count)
```

---

## 5. 비즈니스 메트릭 설계

### 5.1 도메인 이벤트 기반 메트릭

```java
@Component
@RequiredArgsConstructor
public class OrderMetrics {

    private final MeterRegistry registry;

    // 주문 생성 - Counter
    public void orderCreated(Order order) {
        registry.counter("order.created.total",
            "type", order.getType().name(),           // NORMAL, SUBSCRIPTION, GIFT
            "channel", order.getChannel().name(),     // WEB, MOBILE, API
            "payment", order.getPaymentMethod().name() // CARD, BANK, POINT
        ).increment();
    }

    // 주문 금액 분포 - Distribution Summary
    public void orderAmountRecorded(Order order) {
        DistributionSummary.builder("order.amount")
            .baseUnit("won")
            .tag("type", order.getType().name())
            .publishPercentileHistogram()
            .register(registry)
            .record(order.getTotalAmount().doubleValue());
    }

    // 주문 처리 시간 - Timer
    public void orderProcessingCompleted(Order order, long startTimeMs) {
        Timer.builder("order.processing.duration")
            .tag("type", order.getType().name())
            .tag("result", order.getStatus().name())  // COMPLETED, FAILED, CANCELLED
            .publishPercentileHistogram()
            .register(registry)
            .record(Duration.ofMillis(System.currentTimeMillis() - startTimeMs));
    }

    // 재고 현황 - Gauge
    public void registerStockGauge(String productId,
            Supplier<Number> stockSupplier) {
        Gauge.builder("inventory.stock.current", stockSupplier)
            .tag("product_category", getCategoryFromProduct(productId))
            .register(registry);
    }
}
```

### 5.2 SLI/SLO 기반 메트릭

```java
// SLO: 주문 API 99%가 500ms 이내 응답
Timer.builder("order.api.duration")
    .serviceLevelObjectives(
        Duration.ofMillis(100),
        Duration.ofMillis(250),
        Duration.ofMillis(500)  // SLO 경계
    )
    .register(registry);
```

```promql
# SLO 달성률 계산: 500ms 이내 응답 비율
sum(rate(order_api_duration_seconds_bucket{le="0.5"}[1h]))
/ sum(rate(order_api_duration_seconds_count[1h])) * 100

# Error Budget 소진율
1 - (
  sum(rate(order_api_duration_seconds_bucket{le="0.5"}[1h]))
  / sum(rate(order_api_duration_seconds_count[1h]))
) / (1 - 0.99)
```

---

## 6. PromQL 고급 패턴

### 6.1 rate() vs irate()

```promql
# rate(): 시간 범위 전체의 평균 변화율 -> 안정적, 알림에 적합
rate(http_requests_total[5m])

# irate(): 마지막 두 데이터 포인트 간 순간 변화율 -> 민감, 그래프에 적합
irate(http_requests_total[5m])
```

### 6.2 멀티 윈도우 멀티 번인 알림

```promql
# Google SRE 방식: 빠른 감지 + 오탐 방지
# 짧은 윈도우 (빠른 감지)
(
  sum(rate(http_requests_total{status=~"5.."}[1m]))
  / sum(rate(http_requests_total[1m]))
) > 14.4 * 0.001  # 에러 예산의 14.4배 소진 속도

AND

# 긴 윈도우 (오탐 방지)
(
  sum(rate(http_requests_total{status=~"5.."}[1h]))
  / sum(rate(http_requests_total[1h]))
) > 14.4 * 0.001
```

### 6.3 유용한 집계 패턴

```promql
# 서비스별 에러율 Top 5
topk(5,
  sum by (service) (rate(http_requests_total{status=~"5.."}[5m]))
  / sum by (service) (rate(http_requests_total[5m]))
)

# 특정 백분위수 이상인 엔드포인트 찾기
histogram_quantile(0.99,
  sum by (uri, le) (rate(http_request_duration_seconds_bucket[5m]))
) > 1.0

# 전주 대비 트래픽 변화율
sum(rate(http_requests_total[1h]))
/ sum(rate(http_requests_total[1h] offset 7d)) - 1
```

---

## 7. 안티패턴과 회피 방법

### 7.1 주요 안티패턴

| 안티패턴 | 문제 | 해결책 |
|---------|------|--------|
| **고카디널리티 레이블** | 메모리 폭발, 쿼리 성능 저하 | 레이블 값을 그룹핑, 최대 카디널리티 제한 |
| **메트릭 이름에 레이블 값 포함** | `order_status_pending` 같은 이름 | 레이블로 분리: `order{status="pending"}` |
| **모든 것을 메트릭으로** | 저장 비용 증가, 노이즈 | 실제 알림/대시보드에 사용할 메트릭만 수집 |
| **너무 짧은 scrape_interval** | Prometheus 부하 증가 | 15s 기본값 유지, 정말 필요한 경우만 5s |
| **Counter를 Gauge처럼 사용** | 재시작 시 0으로 초기화되어 데이터 손실 | Counter는 반드시 `rate()`/`increase()`와 함께 사용 |
| **Histogram 버킷 과다** | 시계열 수 = 버킷 수 x 레이블 조합 | 버킷 10~15개 이내, SLO 기반 설계 |

### 7.2 메트릭 리팩토링 체크리스트

```
[ ] 모든 Counter에 _total 접미사가 있는가?
[ ] 시간 단위는 seconds를 사용하는가?
[ ] 크기 단위는 bytes를 사용하는가?
[ ] 레이블 카디널리티가 100 이하인가?
[ ] URI 패턴이 정규화되어 있는가?
[ ] 사용하지 않는 메트릭은 비활성화했는가?
[ ] Histogram 버킷이 SLO 경계를 포함하는가?
[ ] 비즈니스 메트릭이 도메인 언어로 명명되어 있는가?
```

---

## 참고 자료

- [Prometheus Naming Best Practices](https://prometheus.io/docs/practices/naming/)
- [Prometheus Histograms and Summaries](https://prometheus.io/docs/practices/histograms/)
- [Micrometer Concepts](https://micrometer.io/docs/concepts)

*마지막 업데이트: 2026년 02월*

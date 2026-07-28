# Circuit Breaker 구현 가이드

Resilience4j를 활용한 Circuit Breaker 패턴 구현과 장애 격리 전략을 정리합니다.

## 목차

1. [Circuit Breaker 개념](#1-circuit-breaker-개념)
2. [Resilience4j 설정](#2-resilience4j-설정)
3. [Fallback 전략](#3-fallback-전략)
4. [Bulkhead 패턴](#4-bulkhead-패턴)
5. [Retry 패턴](#5-retry-패턴)
6. [Rate Limiter](#6-rate-limiter)

---

## 1. Circuit Breaker 개념

### 상태 전이

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│     ┌──────────┐                      ┌──────────┐         │
│     │  CLOSED  │─────실패율 초과─────→│   OPEN   │         │
│     │ (정상)   │                      │ (차단)   │         │
│     └────▲─────┘                      └────┬─────┘         │
│          │                                  │               │
│          │                           대기 시간 경과         │
│          │                                  │               │
│          │     ┌──────────────┐            │               │
│          └─────│  HALF_OPEN   │←───────────┘               │
│         성공   │ (테스트 호출) │                            │
│                └──────┬───────┘                            │
│                       │                                     │
│                   실패 → OPEN으로 복귀                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 동작 흐름

```java
// CLOSED 상태: 정상 호출
Request → Circuit Breaker → External Service → Response
                 │
          성공/실패 기록

// 실패율 50% 초과 시 OPEN 상태로 전환
Request → Circuit Breaker → 즉시 예외 반환 (Fallback)
                 │
           서비스 호출 안 함

// 대기 시간 후 HALF_OPEN 상태
Request → Circuit Breaker → 제한된 호출 허용
                 │
          성공하면 CLOSED, 실패하면 OPEN
```

---

## 2. Resilience4j 설정

### 의존성 추가

```xml
<!-- Spring Boot Starter -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>

<!-- AOP for annotations -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### 설정 파일

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    configs:
      default:
        # 슬라이딩 윈도우 설정
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10              # 최근 10개 요청 기준
        minimumNumberOfCalls: 5            # 최소 5개 호출 후 판단

        # 실패 임계치
        failureRateThreshold: 50           # 50% 실패 시 OPEN
        slowCallRateThreshold: 80          # 80% 느린 호출 시 OPEN
        slowCallDurationThreshold: 3s      # 3초 이상이면 느린 호출

        # 상태 전이
        waitDurationInOpenState: 30s       # OPEN 유지 시간
        permittedNumberOfCallsInHalfOpenState: 3  # HALF_OPEN에서 허용 호출 수
        automaticTransitionFromOpenToHalfOpenEnabled: true

        # 예외 처리
        recordExceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
          - org.springframework.web.client.HttpServerErrorException
        ignoreExceptions:
          - com.example.BusinessException

    instances:
      payment-service:
        baseConfig: default
        failureRateThreshold: 30           # 결제는 더 민감하게

      stock-service:
        baseConfig: default
        slowCallDurationThreshold: 5s      # 재고는 느려도 괜찮음
```

### 프로그래밍 방식 설정

```java
@Configuration
public class CircuitBreakerConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .slidingWindowType(SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .failureRateThreshold(50)
            .slowCallRateThreshold(80)
            .slowCallDurationThreshold(Duration.ofSeconds(3))
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(3)
            .recordExceptions(IOException.class, TimeoutException.class)
            .ignoreExceptions(BusinessException.class)
            .build();

        return CircuitBreakerRegistry.of(config);
    }
}
```

### 어노테이션 사용

```java
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentClient paymentClient;

    @CircuitBreaker(name = "payment-service", fallbackMethod = "paymentFallback")
    public PaymentResponse processPayment(PaymentRequest request) {
        return paymentClient.charge(request);
    }

    // Fallback 메서드 (같은 파라미터 + Exception)
    private PaymentResponse paymentFallback(PaymentRequest request, Exception ex) {
        log.error("Payment circuit breaker triggered: {}", ex.getMessage());

        return PaymentResponse.builder()
            .status(PaymentStatus.PENDING)
            .message("결제 처리 중입니다. 잠시 후 확인해주세요.")
            .build();
    }
}
```

### 프로그래밍 방식 사용

```java
@Service
@RequiredArgsConstructor
public class StockService {

    private final CircuitBreakerRegistry registry;
    private final StockClient stockClient;

    public StockResponse getStock(Long productId) {
        CircuitBreaker circuitBreaker = registry.circuitBreaker("stock-service");

        return circuitBreaker.executeSupplier(() -> {
            return stockClient.getStock(productId);
        });
    }

    // 또는 Try 모나드 사용
    public Try<StockResponse> getStockWithTry(Long productId) {
        CircuitBreaker circuitBreaker = registry.circuitBreaker("stock-service");

        return Try.ofSupplier(
            CircuitBreaker.decorateSupplier(circuitBreaker,
                () -> stockClient.getStock(productId))
        );
    }
}
```

---

## 3. Fallback 전략

### Fallback 유형

```java
@Service
public class ProductService {

    // 1. 기본값 반환
    @CircuitBreaker(name = "product", fallbackMethod = "getProductFallback")
    public Product getProduct(Long id) {
        return productClient.getProduct(id);
    }

    private Product getProductFallback(Long id, Exception ex) {
        return Product.builder()
            .id(id)
            .name("상품 정보를 불러올 수 없습니다")
            .available(false)
            .build();
    }

    // 2. 캐시에서 반환
    @CircuitBreaker(name = "product", fallbackMethod = "getProductFromCache")
    public Product getProductWithCache(Long id) {
        Product product = productClient.getProduct(id);
        cacheManager.put(id, product);  // 성공 시 캐시 저장
        return product;
    }

    private Product getProductFromCache(Long id, Exception ex) {
        return cacheManager.get(id)
            .orElse(Product.unavailable(id));
    }

    // 3. 대체 서비스 호출
    @CircuitBreaker(name = "primary-stock", fallbackMethod = "getStockFromSecondary")
    public Stock getStock(Long productId) {
        return primaryStockClient.getStock(productId);
    }

    private Stock getStockFromSecondary(Long productId, Exception ex) {
        log.warn("Falling back to secondary stock service");
        return secondaryStockClient.getStock(productId);
    }

    // 4. 비동기 처리로 전환
    @CircuitBreaker(name = "order", fallbackMethod = "processOrderAsync")
    public OrderResponse createOrder(OrderRequest request) {
        return orderClient.create(request);
    }

    private OrderResponse processOrderAsync(OrderRequest request, Exception ex) {
        // 메시지 큐에 저장하고 나중에 처리
        messageQueue.send(new OrderMessage(request));

        return OrderResponse.builder()
            .status(OrderStatus.QUEUED)
            .message("주문이 접수되었습니다. 처리 후 알림드리겠습니다.")
            .build();
    }
}
```

### Fallback 체인

```java
@Service
public class RecommendationService {

    // 1차 Fallback
    @CircuitBreaker(name = "ml-recommendation", fallbackMethod = "fallbackToRules")
    public List<Product> getRecommendations(Long userId) {
        return mlRecommendationClient.getRecommendations(userId);
    }

    // 2차 Fallback: 규칙 기반
    private List<Product> fallbackToRules(Long userId, Exception ex) {
        log.warn("ML 추천 실패, 규칙 기반으로 전환");
        return ruleBasedRecommendation.getRecommendations(userId);
    }

    // 3차 Fallback: 인기 상품
    @CircuitBreaker(name = "rule-recommendation", fallbackMethod = "fallbackToPopular")
    private List<Product> ruleBasedRecommendationWithCB(Long userId) {
        return ruleBasedRecommendation.getRecommendations(userId);
    }

    private List<Product> fallbackToPopular(Long userId, Exception ex) {
        log.warn("규칙 기반 추천도 실패, 인기 상품 반환");
        return popularProductCache.getTopProducts(10);
    }
}
```

---

## 4. Bulkhead 패턴

### 개념

```
Bulkhead: 격벽 (선박의 격실처럼 장애 격리)

┌────────────────────────────────────────────────────────────┐
│                    Thread Pool                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Payment Pool │  │  Stock Pool  │  │  Order Pool  │     │
│  │  (10 threads)│  │  (5 threads) │  │  (8 threads) │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│                                                            │
│  Payment 서비스 장애 시 Stock, Order는 영향 없음          │
└────────────────────────────────────────────────────────────┘
```

### 설정

```yaml
# application.yml
resilience4j:
  bulkhead:
    configs:
      default:
        maxConcurrentCalls: 10           # 동시 호출 수
        maxWaitDuration: 500ms           # 대기 시간

    instances:
      payment-service:
        maxConcurrentCalls: 5            # 결제는 더 적게
        maxWaitDuration: 1s

  thread-pool-bulkhead:
    configs:
      default:
        maxThreadPoolSize: 10
        coreThreadPoolSize: 5
        queueCapacity: 100
        keepAliveDuration: 20ms

    instances:
      stock-service:
        maxThreadPoolSize: 8
        coreThreadPoolSize: 4
        queueCapacity: 50
```

### 사용

```java
@Service
public class OrderService {

    // Semaphore Bulkhead (동기)
    @Bulkhead(name = "payment-service", type = Bulkhead.Type.SEMAPHORE)
    @CircuitBreaker(name = "payment-service")
    public PaymentResponse processPayment(PaymentRequest request) {
        return paymentClient.charge(request);
    }

    // Thread Pool Bulkhead (비동기)
    @Bulkhead(name = "stock-service", type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<StockResponse> checkStock(Long productId) {
        return CompletableFuture.supplyAsync(() ->
            stockClient.getStock(productId)
        );
    }
}
```

### Semaphore vs Thread Pool

| 특성 | Semaphore | Thread Pool |
|------|-----------|-------------|
| 방식 | 동시 호출 수 제한 | 별도 스레드 풀 |
| 스레드 | 호출자 스레드 사용 | 별도 스레드 사용 |
| 격리 수준 | 낮음 | 높음 |
| 오버헤드 | 낮음 | 높음 (스레드 생성) |
| 사용 시점 | 동기 호출 | 비동기 호출 |

---

## 5. Retry 패턴

### 설정

```yaml
# application.yml
resilience4j:
  retry:
    configs:
      default:
        maxAttempts: 3                   # 최대 재시도 횟수
        waitDuration: 500ms              # 재시도 간격
        enableExponentialBackoff: true   # 지수 백오프
        exponentialBackoffMultiplier: 2  # 배수
        retryExceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
        ignoreExceptions:
          - com.example.BusinessException

    instances:
      payment-service:
        maxAttempts: 2                   # 결제는 적게 재시도
        waitDuration: 1s

      notification-service:
        maxAttempts: 5                   # 알림은 많이 재시도
        waitDuration: 2s
```

### 사용

```java
@Service
public class NotificationService {

    @Retry(name = "notification-service", fallbackMethod = "notifyFallback")
    @CircuitBreaker(name = "notification-service")
    public void sendNotification(Notification notification) {
        notificationClient.send(notification);
    }

    private void notifyFallback(Notification notification, Exception ex) {
        log.error("알림 발송 실패, 재시도 큐에 저장: {}", ex.getMessage());
        retryQueue.add(notification);
    }
}
```

### 패턴 조합 순서

```java
// 권장 순서: Retry → CircuitBreaker → Bulkhead → RateLimiter

@Bulkhead(name = "service")
@CircuitBreaker(name = "service")
@Retry(name = "service")
@RateLimiter(name = "service")
public Response callExternalService() {
    return externalClient.call();
}

// 실행 순서:
// 1. RateLimiter: 요청 수 제한 확인
// 2. Bulkhead: 동시 실행 수 확인
// 3. CircuitBreaker: 서킷 상태 확인
// 4. Retry: 실패 시 재시도
// 5. 실제 호출
```

---

## 6. Rate Limiter

### 설정

```yaml
# application.yml
resilience4j:
  ratelimiter:
    configs:
      default:
        limitRefreshPeriod: 1s           # 갱신 주기
        limitForPeriod: 100              # 주기당 허용 요청 수
        timeoutDuration: 500ms           # 허용 대기 시간

    instances:
      external-api:
        limitForPeriod: 50               # 초당 50개
        timeoutDuration: 0               # 즉시 거부

      user-api:
        limitRefreshPeriod: 1m           # 분당
        limitForPeriod: 1000             # 분당 1000개
```

### 사용

```java
@RestController
public class ApiController {

    @RateLimiter(name = "user-api", fallbackMethod = "rateLimitFallback")
    @GetMapping("/api/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    private User rateLimitFallback(Long id, RequestNotPermitted ex) {
        throw new TooManyRequestsException("요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
    }
}
```

### 사용자별 Rate Limiting

```java
@Component
public class UserRateLimiter {

    private final Map<Long, RateLimiter> limiters = new ConcurrentHashMap<>();
    private final RateLimiterConfig config;

    public UserRateLimiter() {
        this.config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .limitForPeriod(100)
            .timeoutDuration(Duration.ZERO)
            .build();
    }

    public void checkLimit(Long userId) {
        RateLimiter limiter = limiters.computeIfAbsent(userId,
            id -> RateLimiter.of("user-" + id, config));

        if (!limiter.acquirePermission()) {
            throw new TooManyRequestsException("Rate limit exceeded for user: " + userId);
        }
    }
}
```

---

## 모니터링

### Actuator 엔드포인트

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,circuitbreakers,ratelimiters,bulkheads,retries
  health:
    circuitbreakers:
      enabled: true
```

```bash
# Circuit Breaker 상태 확인
curl http://localhost:8080/actuator/circuitbreakers

# 특정 서킷 상태
curl http://localhost:8080/actuator/circuitbreakers/payment-service

# 상태 변경 이벤트
curl http://localhost:8080/actuator/circuitbreakerevents
```

### 이벤트 리스너

```java
@Component
public class CircuitBreakerEventListener {

    @EventListener
    public void onStateTransition(CircuitBreakerOnStateTransitionEvent event) {
        log.warn("Circuit breaker '{}' state changed: {} → {}",
            event.getCircuitBreakerName(),
            event.getStateTransition().getFromState(),
            event.getStateTransition().getToState());

        if (event.getStateTransition().getToState() == CircuitBreaker.State.OPEN) {
            alertService.sendAlert("Circuit breaker OPEN: " + event.getCircuitBreakerName());
        }
    }

    @EventListener
    public void onFailure(CircuitBreakerOnErrorEvent event) {
        log.error("Circuit breaker '{}' recorded error: {}",
            event.getCircuitBreakerName(),
            event.getThrowable().getMessage());
    }
}
```

### Prometheus 메트릭

```java
// 자동 등록됨
// resilience4j_circuitbreaker_state
// resilience4j_circuitbreaker_calls_total
// resilience4j_circuitbreaker_failure_rate
// resilience4j_bulkhead_available_concurrent_calls
// resilience4j_ratelimiter_available_permissions
```

---

## 체크리스트

```
□ 실패율 임계치 설정 (서비스 특성 고려)
□ 슬로우 콜 기준 설정
□ OPEN 상태 유지 시간 설정
□ Fallback 전략 구현
□ 적절한 예외 처리 (recordExceptions/ignoreExceptions)
□ Bulkhead로 리소스 격리
□ Rate Limiter로 과부하 방지
□ 모니터링 및 알림 설정
□ 테스트 (장애 상황 시뮬레이션)
```

---

*마지막 업데이트: 2026년 01월*

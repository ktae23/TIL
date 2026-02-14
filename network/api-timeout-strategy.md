# API 타임아웃 전략과 복원력 패턴

## 목차
1. [타임아웃 기본 개념](#타임아웃-기본-개념)
2. [서킷 브레이커 패턴](#서킷-브레이커-패턴)
3. [Retry 전략](#retry-전략)
4. [백프레셔 (Backpressure)](#백프레셔-backpressure)
5. [Resilience4j 실전 적용](#resilience4j-실전-적용)
6. [핵심 정리](#핵심-정리)

---

## 타임아웃 기본 개념

### 타임아웃 유형

```
┌──────────────────────────────────────────────────────────────────┐
│                      타임아웃 유형                                │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  1. Connection Timeout (연결 타임아웃)                           │
│     └── TCP 3-way handshake 완료까지 대기 시간                   │
│     └── 권장: 1-5초                                              │
│                                                                   │
│  2. Read Timeout (읽기 타임아웃)                                 │
│     └── 응답 데이터 수신까지 대기 시간                            │
│     └── 권장: 서비스 특성에 따라 (보통 5-30초)                    │
│                                                                   │
│  3. Write Timeout (쓰기 타임아웃)                                │
│     └── 요청 데이터 전송까지 대기 시간                            │
│     └── 권장: 1-10초                                             │
│                                                                   │
│  4. Request Timeout (전체 타임아웃)                              │
│     └── 요청 시작부터 응답 완료까지 전체 시간                     │
│     └── 권장: Connection + Read + 여유 시간                      │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Spring Boot 타임아웃 설정

```java
// RestTemplate 설정
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory =
            new HttpComponentsClientHttpRequestFactory();

        factory.setConnectTimeout(3000);      // 연결 타임아웃: 3초
        factory.setReadTimeout(10000);        // 읽기 타임아웃: 10초

        return new RestTemplate(factory);
    }
}

// WebClient 설정 (Reactive)
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
            .responseTimeout(Duration.ofSeconds(10))
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)));

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}

// Feign Client 설정
@Configuration
public class FeignConfig {

    @Bean
    public Request.Options options() {
        return new Request.Options(
            3, TimeUnit.SECONDS,     // connectTimeout
            10, TimeUnit.SECONDS,    // readTimeout
            true                      // followRedirects
        );
    }
}
```

### 타임아웃 설정 가이드라인

```yaml
# application.yml
feign:
  client:
    config:
      default:
        connectTimeout: 3000
        readTimeout: 10000

      # 서비스별 개별 설정
      payment-service:
        connectTimeout: 3000
        readTimeout: 30000  # 결제는 더 긴 타임아웃

      inventory-service:
        connectTimeout: 2000
        readTimeout: 5000   # 재고 조회는 빠른 응답 기대
```

---

## 서킷 브레이커 패턴

### 개념

```
┌──────────────────────────────────────────────────────────────────┐
│                    서킷 브레이커 상태                             │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│       CLOSED                 OPEN                  HALF-OPEN     │
│    (정상 동작)            (차단 상태)            (테스트 상태)    │
│         │                     │                       │          │
│    ┌────┴────┐           ┌────┴────┐            ┌────┴────┐     │
│    │ 요청 통과 │           │ 요청 차단 │            │ 일부 통과 │    │
│    │ 실패 카운트 │          │ Fallback │            │ 결과 확인 │    │
│    └────┬────┘           └────┬────┘            └────┬────┘     │
│         │                     │                       │          │
│    실패율 초과 ──────────►    │    ◄──── 대기 시간 후 │          │
│                              │                       │          │
│         │    ◄───── 성공 시 ─┼───────────────────────┘          │
│         │                    │                                   │
│         └──── 실패 시 ───────┘                                   │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Resilience4j 서킷 브레이커 구현

```java
// 의존성 (build.gradle)
implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.2.0'
implementation 'org.springframework.boot:spring-boot-starter-aop'

// application.yml
resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        registerHealthIndicator: true
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10                    # 최근 10개 요청 기준
        minimumNumberOfCalls: 5                  # 최소 5개 요청 후 계산
        failureRateThreshold: 50                 # 실패율 50% 초과 시 OPEN
        waitDurationInOpenState: 30s             # OPEN 상태 유지 시간
        permittedNumberOfCallsInHalfOpenState: 3 # HALF-OPEN에서 테스트 요청 수
        slowCallDurationThreshold: 2s            # 느린 호출 기준
        slowCallRateThreshold: 80                # 느린 호출 비율 임계치

      inventoryService:
        slidingWindowType: TIME_BASED
        slidingWindowSize: 60                    # 최근 60초 기준
        failureRateThreshold: 60
        waitDurationInOpenState: 10s
```

```java
// 서비스 구현
@Service
@Slf4j
public class PaymentService {

    private final PaymentClient paymentClient;

    @CircuitBreaker(name = "paymentService", fallbackMethod = "processPaymentFallback")
    public PaymentResult processPayment(PaymentRequest request) {
        log.info("Processing payment: {}", request.getOrderId());
        return paymentClient.pay(request);
    }

    // Fallback 메서드 - 파라미터 + Exception
    private PaymentResult processPaymentFallback(PaymentRequest request, Exception e) {
        log.warn("Payment circuit breaker activated for order: {}, error: {}",
                 request.getOrderId(), e.getMessage());

        // 대체 응답 또는 큐잉
        return PaymentResult.builder()
            .status(PaymentStatus.PENDING)
            .message("결제 시스템 일시 장애. 잠시 후 자동 재시도됩니다.")
            .build();
    }
}
```

### 서킷 브레이커 모니터링

```java
// 이벤트 리스너
@Component
@Slf4j
public class CircuitBreakerEventListener {

    @Autowired
    public CircuitBreakerEventListener(CircuitBreakerRegistry registry) {
        registry.circuitBreaker("paymentService")
            .getEventPublisher()
            .onStateTransition(event ->
                log.warn("Circuit Breaker State Transition: {} -> {}",
                         event.getStateTransition().getFromState(),
                         event.getStateTransition().getToState()))
            .onFailureRateExceeded(event ->
                log.error("Failure rate exceeded: {}%", event.getFailureRate()))
            .onSlowCallRateExceeded(event ->
                log.warn("Slow call rate exceeded: {}%", event.getSlowCallRate()));
    }
}

// Actuator 엔드포인트
// GET /actuator/circuitbreakers
// GET /actuator/circuitbreakerevents
```

---

## Retry 전략

### Retry 패턴 유형

```
┌──────────────────────────────────────────────────────────────────┐
│                      Retry 전략                                   │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  1. Fixed Delay (고정 대기)                                      │
│     시도 1 ──[2초]── 시도 2 ──[2초]── 시도 3                      │
│                                                                   │
│  2. Exponential Backoff (지수 백오프)                            │
│     시도 1 ──[1초]── 시도 2 ──[2초]── 시도 3 ──[4초]── 시도 4    │
│                                                                   │
│  3. Exponential Backoff with Jitter (지터 추가)                  │
│     시도 1 ──[1±0.5초]── 시도 2 ──[2±1초]── 시도 3              │
│     → 동시 재시도로 인한 Thundering Herd 방지                    │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Resilience4j Retry 구현

```yaml
# application.yml
resilience4j:
  retry:
    instances:
      externalApi:
        maxAttempts: 3                          # 최대 3회 시도
        waitDuration: 1s                        # 기본 대기 시간
        enableExponentialBackoff: true          # 지수 백오프 활성화
        exponentialBackoffMultiplier: 2         # 배수 (1초 → 2초 → 4초)
        exponentialMaxWaitDuration: 10s         # 최대 대기 시간
        retryExceptions:                        # 재시도할 예외
          - java.io.IOException
          - java.net.SocketTimeoutException
          - org.springframework.web.client.HttpServerErrorException
        ignoreExceptions:                       # 재시도하지 않을 예외
          - java.lang.IllegalArgumentException
          - com.example.BusinessException
```

```java
@Service
@Slf4j
public class ExternalApiService {

    private final ExternalApiClient client;

    @Retry(name = "externalApi", fallbackMethod = "getDataFallback")
    public ApiResponse getData(String id) {
        log.info("Fetching data for id: {}", id);
        return client.fetch(id);
    }

    private ApiResponse getDataFallback(String id, Exception e) {
        log.error("All retries failed for id: {}, error: {}", id, e.getMessage());

        // 캐시된 데이터 반환 또는 기본값
        return cacheService.getFromCache(id)
            .orElse(ApiResponse.defaultResponse());
    }
}

// 프로그래밍 방식 Retry
@Service
public class CustomRetryService {

    private final RetryRegistry retryRegistry;

    public ApiResponse fetchWithCustomRetry(String id) {
        Retry retry = retryRegistry.retry("externalApi");

        return Retry.decorateSupplier(retry, () -> {
            log.info("Attempting to fetch: {}", id);
            return externalClient.fetch(id);
        }).get();
    }
}
```

### Retry + Circuit Breaker 조합

```java
@Service
public class ResilientService {

    // Retry가 먼저 적용되고, 모든 재시도 실패 시 Circuit Breaker 카운트
    @CircuitBreaker(name = "backend", fallbackMethod = "fallback")
    @Retry(name = "backend")
    @TimeLimiter(name = "backend")  // 전체 타임아웃
    public CompletableFuture<Response> callBackend(Request request) {
        return CompletableFuture.supplyAsync(() ->
            backendClient.call(request));
    }

    private CompletableFuture<Response> fallback(Request request, Exception e) {
        log.warn("Fallback activated: {}", e.getMessage());
        return CompletableFuture.completedFuture(Response.defaultResponse());
    }
}

// application.yml - 조합 설정
resilience4j:
  timelimiter:
    instances:
      backend:
        timeoutDuration: 10s
        cancelRunningFuture: true

  retry:
    instances:
      backend:
        maxAttempts: 3
        waitDuration: 500ms

  circuitbreaker:
    instances:
      backend:
        failureRateThreshold: 50
        slidingWindowSize: 10
```

---

## 백프레셔 (Backpressure)

### 개념

```
┌──────────────────────────────────────────────────────────────────┐
│                     백프레셔 필요 상황                            │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Producer (빠름)         Consumer (느림)                          │
│       │                       │                                   │
│       │  ───────────────►    │  처리 불가!                       │
│       │  100 req/s           │  50 req/s                         │
│       │                       │                                   │
│       ▼                       ▼                                   │
│  ┌─────────────────────────────────────────┐                     │
│  │           버퍼 (Queue)                   │                     │
│  │  [●][●][●][●][●][●][●][●][●][●]        │ ← 오버플로우!        │
│  └─────────────────────────────────────────┘                     │
│                                                                   │
│  해결책: 백프레셔 적용                                            │
│  - 생산 속도 조절                                                │
│  - 버퍼링 + 드롭 정책                                            │
│  - Rate Limiting                                                  │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Rate Limiter 구현

```yaml
# application.yml
resilience4j:
  ratelimiter:
    instances:
      api:
        limitForPeriod: 100              # 주기당 허용 요청 수
        limitRefreshPeriod: 1s           # 주기 (1초당 100개 = 100 TPS)
        timeoutDuration: 5s              # 대기 시간 (초과 시 예외)
        registerHealthIndicator: true
        eventConsumerBufferSize: 100
```

```java
@Service
public class RateLimitedService {

    @RateLimiter(name = "api", fallbackMethod = "rateLimitFallback")
    public Response processRequest(Request request) {
        return backendService.process(request);
    }

    private Response rateLimitFallback(Request request,
                                       RequestNotPermitted exception) {
        log.warn("Rate limit exceeded for request: {}", request.getId());
        throw new TooManyRequestsException("요청이 너무 많습니다. 잠시 후 시도해주세요.");
    }
}

// 컨트롤러에서 429 응답
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(TooManyRequestsException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", "60")  // 60초 후 재시도 권장
            .body(new ErrorResponse(e.getMessage()));
    }
}
```

### Bulkhead (격벽) 패턴

```yaml
# application.yml
resilience4j:
  bulkhead:
    instances:
      backend:
        maxConcurrentCalls: 10           # 동시 호출 제한
        maxWaitDuration: 500ms           # 대기 시간

  thread-pool-bulkhead:
    instances:
      backend:
        maxThreadPoolSize: 10            # 스레드 풀 최대 크기
        coreThreadPoolSize: 5            # 코어 스레드 수
        queueCapacity: 100               # 큐 용량
        keepAliveDuration: 20ms
```

```java
@Service
public class IsolatedService {

    // Semaphore Bulkhead (동기 호출)
    @Bulkhead(name = "backend", type = Bulkhead.Type.SEMAPHORE)
    public Response callBackendSync(Request request) {
        return backendClient.call(request);
    }

    // ThreadPool Bulkhead (비동기 호출)
    @Bulkhead(name = "backend", type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<Response> callBackendAsync(Request request) {
        return CompletableFuture.supplyAsync(() ->
            backendClient.call(request));
    }
}
```

### WebFlux 백프레셔

```java
@Service
public class ReactiveService {

    public Flux<Data> streamData() {
        return dataSource.getDataStream()
            // 백프레셔 전략
            .onBackpressureBuffer(1000)      // 버퍼링 (1000개까지)
            // .onBackpressureDrop()          // 드롭
            // .onBackpressureLatest()        // 최신 값만 유지
            // .onBackpressureError()         // 에러 발생
            .limitRate(100)                   // 다운스트림 요청 제한
            .delayElements(Duration.ofMillis(10));  // 속도 조절
    }
}
```

---

## Resilience4j 실전 적용

### 통합 설정 예시

```yaml
# application.yml - 실전 설정
resilience4j:
  circuitbreaker:
    configs:
      default:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 3
    instances:
      payment:
        baseConfig: default
        slowCallDurationThreshold: 3s
        slowCallRateThreshold: 80
      inventory:
        baseConfig: default
        failureRateThreshold: 70
        waitDurationInOpenState: 10s

  retry:
    configs:
      default:
        maxAttempts: 3
        waitDuration: 1s
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
    instances:
      payment:
        baseConfig: default
        maxAttempts: 2  # 결제는 2번만
      inventory:
        baseConfig: default

  ratelimiter:
    instances:
      public-api:
        limitForPeriod: 1000
        limitRefreshPeriod: 1s
        timeoutDuration: 0s  # 즉시 거부

  bulkhead:
    instances:
      payment:
        maxConcurrentCalls: 20
        maxWaitDuration: 0s

management:
  endpoints:
    web:
      exposure:
        include: health,circuitbreakers,retries,ratelimiters
  health:
    circuitbreakers:
      enabled: true
```

### 완전한 서비스 구현

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final PaymentClient paymentClient;
    private final InventoryClient inventoryClient;
    private final NotificationClient notificationClient;

    @CircuitBreaker(name = "payment", fallbackMethod = "paymentFallback")
    @Retry(name = "payment")
    @Bulkhead(name = "payment")
    public PaymentResult processPayment(Order order) {
        log.info("Processing payment for order: {}", order.getId());
        return paymentClient.charge(order.getPaymentInfo());
    }

    @CircuitBreaker(name = "inventory", fallbackMethod = "inventoryFallback")
    @Retry(name = "inventory")
    public InventoryResult reserveInventory(Order order) {
        log.info("Reserving inventory for order: {}", order.getId());
        return inventoryClient.reserve(order.getItems());
    }

    // 알림은 실패해도 주문에 영향 없음
    @CircuitBreaker(name = "notification")
    @Retry(name = "notification")
    public void sendNotification(Order order, String message) {
        try {
            notificationClient.send(order.getCustomerEmail(), message);
        } catch (Exception e) {
            log.warn("Notification failed, but order continues: {}", e.getMessage());
        }
    }

    private PaymentResult paymentFallback(Order order, Exception e) {
        log.error("Payment failed for order: {}, error: {}", order.getId(), e.getMessage());

        // 나중에 처리하도록 큐에 저장
        messageQueue.send("payment-retry-queue", order);

        return PaymentResult.pending("결제 처리 중. 잠시 후 확인해주세요.");
    }

    private InventoryResult inventoryFallback(Order order, Exception e) {
        log.error("Inventory check failed: {}", e.getMessage());
        throw new ServiceUnavailableException("재고 확인 서비스 일시 장애");
    }
}
```

---

## 핵심 정리

### 패턴 선택 가이드

| 상황 | 적용 패턴 |
|------|----------|
| 일시적 네트워크 오류 | Retry (지수 백오프) |
| 외부 서비스 장애 | Circuit Breaker |
| 트래픽 급증 | Rate Limiter |
| 리소스 보호 | Bulkhead |
| 응답 시간 보장 | Time Limiter |

### 패턴 조합 순서

```
요청 → RateLimiter → Bulkhead → CircuitBreaker → Retry → TimeLimiter → 실제 호출

1. RateLimiter: 초당 요청 수 제한
2. Bulkhead: 동시 호출 수 제한
3. CircuitBreaker: 실패율 기반 차단
4. Retry: 일시적 오류 재시도
5. TimeLimiter: 전체 타임아웃
```

### 면접 대비 핵심 질문

1. **Q: 서킷 브레이커의 상태와 전이 조건을 설명해주세요**
   - A: CLOSED(정상) → 실패율 초과 → OPEN(차단) → 대기 시간 후 → HALF-OPEN(테스트) → 성공 시 CLOSED, 실패 시 OPEN

2. **Q: Exponential Backoff with Jitter를 사용하는 이유는?**
   - A: 지수 백오프로 재시도 간격을 늘리고, Jitter(랜덤 지연)로 동시 재시도로 인한 Thundering Herd 문제 방지

3. **Q: Rate Limiter와 Bulkhead의 차이점은?**
   - A: Rate Limiter는 시간당 요청 수 제한(TPS), Bulkhead는 동시 실행 수 제한(Concurrency). 둘 다 사용하면 더 정교한 제어 가능

4. **Q: Fallback에서 고려해야 할 사항은?**
   - A: 캐시된 데이터 반환, 기본값 제공, 메시지 큐에 저장 후 재처리, 사용자에게 적절한 메시지 전달. Fallback도 실패할 수 있으므로 간단하게 유지

---

*마지막 업데이트: 2026년 01월*

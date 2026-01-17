# API Gateway 패턴

## 목차
1. [API Gateway 개념](#api-gateway-개념)
2. [라우팅 패턴](#라우팅-패턴)
3. [Rate Limiting](#rate-limiting)
4. [Spring Cloud Gateway](#spring-cloud-gateway)
5. [인증/인가](#인증인가)
6. [핵심 정리](#핵심-정리)

---

## API Gateway 개념

### API Gateway 역할

```
┌──────────────────────────────────────────────────────────────────┐
│                    API Gateway 역할                               │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  클라이언트                                                       │
│      │                                                            │
│      ▼                                                            │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                     API Gateway                             │  │
│  ├────────────────────────────────────────────────────────────┤  │
│  │                                                              │  │
│  │  1. 라우팅 (Routing)                                        │  │
│  │     └── 요청을 적절한 서비스로 전달                          │  │
│  │                                                              │  │
│  │  2. 인증/인가 (Authentication/Authorization)                │  │
│  │     └── JWT 검증, OAuth2 처리                               │  │
│  │                                                              │  │
│  │  3. Rate Limiting                                           │  │
│  │     └── API 호출 제한, 과부하 방지                           │  │
│  │                                                              │  │
│  │  4. 로드 밸런싱                                              │  │
│  │     └── 서비스 인스턴스 간 부하 분산                         │  │
│  │                                                              │  │
│  │  5. 요청/응답 변환                                           │  │
│  │     └── 프로토콜 변환, 데이터 변환                           │  │
│  │                                                              │  │
│  │  6. 캐싱                                                     │  │
│  │     └── 응답 캐싱으로 성능 향상                              │  │
│  │                                                              │  │
│  │  7. 로깅/모니터링                                            │  │
│  │     └── 요청 추적, 메트릭 수집                               │  │
│  │                                                              │  │
│  └────────────────────────────────────────────────────────────┘  │
│      │           │           │           │                        │
│      ▼           ▼           ▼           ▼                        │
│  ┌──────┐   ┌──────┐   ┌──────┐   ┌──────┐                       │
│  │User  │   │Order │   │Product│  │Payment│                      │
│  │Service│  │Service│  │Service│  │Service│                      │
│  └──────┘   └──────┘   └──────┘   └──────┘                       │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### BFF (Backend for Frontend) 패턴

```
┌──────────────────────────────────────────────────────────────────┐
│                    BFF 패턴                                       │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌─────────┐     ┌─────────┐     ┌─────────┐                    │
│  │   Web   │     │  Mobile │     │   3rd   │                    │
│  │  Client │     │   App   │     │  Party  │                    │
│  └────┬────┘     └────┬────┘     └────┬────┘                    │
│       │               │               │                          │
│       ▼               ▼               ▼                          │
│  ┌─────────┐     ┌─────────┐     ┌─────────┐                    │
│  │ Web BFF │     │Mobile   │     │ Public  │                    │
│  │         │     │  BFF    │     │   API   │                    │
│  └────┬────┘     └────┬────┘     └────┬────┘                    │
│       │               │               │                          │
│       └───────────────┼───────────────┘                          │
│                       ▼                                          │
│              ┌─────────────────┐                                │
│              │  Internal APIs  │                                │
│              │  (Microservices)│                                │
│              └─────────────────┘                                │
│                                                                   │
│  장점:                                                           │
│  - 클라이언트별 최적화된 API                                      │
│  - 백엔드 변경이 클라이언트에 미치는 영향 최소화                   │
│  - 클라이언트별 다른 인증 방식 적용 가능                          │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

---

## 라우팅 패턴

### Spring Cloud Gateway 라우팅

```yaml
# application.yml
spring:
  cloud:
    gateway:
      routes:
        # 기본 경로 라우팅
        - id: user-service
          uri: lb://USER-SERVICE  # 서비스 디스커버리 사용
          predicates:
            - Path=/api/users/**

        # 경로 재작성
        - id: order-service
          uri: lb://ORDER-SERVICE
          predicates:
            - Path=/api/v1/orders/**
          filters:
            - RewritePath=/api/v1/orders/(?<segment>.*), /orders/$\{segment}

        # 헤더 기반 라우팅
        - id: admin-service
          uri: lb://ADMIN-SERVICE
          predicates:
            - Path=/api/admin/**
            - Header=X-Admin-Token, .+

        # 호스트 기반 라우팅
        - id: partner-api
          uri: lb://PARTNER-SERVICE
          predicates:
            - Host=partner.api.example.com

        # 메서드 기반 라우팅
        - id: read-replica
          uri: lb://READ-SERVICE
          predicates:
            - Path=/api/products/**
            - Method=GET

        - id: write-primary
          uri: lb://WRITE-SERVICE
          predicates:
            - Path=/api/products/**
            - Method=POST,PUT,DELETE

        # 가중치 기반 라우팅 (카나리 배포)
        - id: service-v1
          uri: lb://SERVICE-V1
          predicates:
            - Path=/api/feature/**
            - Weight=group1, 90

        - id: service-v2
          uri: lb://SERVICE-V2
          predicates:
            - Path=/api/feature/**
            - Weight=group1, 10
```

### 프로그래밍 방식 라우팅

```java
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // 기본 라우팅
            .route("user-service", r -> r
                .path("/api/users/**")
                .uri("lb://USER-SERVICE"))

            // 필터 체인
            .route("order-service", r -> r
                .path("/api/orders/**")
                .filters(f -> f
                    .addRequestHeader("X-Request-Source", "gateway")
                    .addResponseHeader("X-Response-Time", String.valueOf(System.currentTimeMillis()))
                    .rewritePath("/api/orders/(?<segment>.*)", "/orders/${segment}")
                    .circuitBreaker(c -> c
                        .setName("orderCircuitBreaker")
                        .setFallbackUri("forward:/fallback/orders"))
                    .retry(retryConfig -> retryConfig
                        .setRetries(3)
                        .setStatuses(HttpStatus.SERVICE_UNAVAILABLE)))
                .uri("lb://ORDER-SERVICE"))

            // 조건부 라우팅
            .route("conditional-route", r -> r
                .path("/api/conditional/**")
                .and()
                .header("X-Feature-Flag", "enabled")
                .uri("lb://NEW-SERVICE"))

            .build();
    }
}
```

---

## Rate Limiting

### 토큰 버킷 알고리즘

```
┌──────────────────────────────────────────────────────────────────┐
│                    토큰 버킷 알고리즘                             │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌───────────────────────────────────────┐                       │
│  │           Token Bucket                 │                       │
│  │   ┌───┬───┬───┬───┬───┬───┬───┬───┐  │                       │
│  │   │ ● │ ● │ ● │ ● │ ● │   │   │   │  │ ← 버킷 용량: 8       │
│  │   └───┴───┴───┴───┴───┴───┴───┴───┘  │                       │
│  │         현재 토큰: 5개                 │                       │
│  └───────────────────────────────────────┘                       │
│                    ▲                 │                            │
│                    │                 │                            │
│              초당 토큰 추가       요청 시 토큰 소비                │
│              (replenish rate)                                     │
│                                                                   │
│  동작:                                                           │
│  1. 요청이 오면 토큰 1개 소비                                    │
│  2. 토큰이 없으면 요청 거부 (429 Too Many Requests)              │
│  3. 일정 간격으로 토큰 보충 (최대 버킷 용량까지)                  │
│                                                                   │
│  장점:                                                           │
│  - 버스트 트래픽 허용 (버킷에 토큰이 있을 때)                    │
│  - 평균적으로 속도 제한 유지                                     │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Spring Cloud Gateway Rate Limiting

```java
// Redis 기반 Rate Limiter 설정
@Configuration
public class RateLimiterConfig {

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // replenishRate: 초당 토큰 보충 수
        // burstCapacity: 최대 토큰 수 (버킷 용량)
        // requestedTokens: 요청당 소비 토큰 수
        return new RedisRateLimiter(10, 20, 1);
    }

    // 사용자별 키 추출
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.justOrEmpty(
            exchange.getRequest().getHeaders().getFirst("X-User-Id"))
            .defaultIfEmpty("anonymous");
    }

    // IP별 키 추출
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
    }

    // API 경로별 키 추출
    @Bean
    public KeyResolver pathKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getPath().value());
    }
}

// 라우트 설정
@Bean
public RouteLocator rateLimitedRoutes(RouteLocatorBuilder builder,
                                       RedisRateLimiter rateLimiter,
                                       KeyResolver userKeyResolver) {
    return builder.routes()
        .route("rate-limited-route", r -> r
            .path("/api/**")
            .filters(f -> f
                .requestRateLimiter(c -> c
                    .setRateLimiter(rateLimiter)
                    .setKeyResolver(userKeyResolver)
                    .setDenyEmptyKey(false)
                    .setStatusCode(HttpStatus.TOO_MANY_REQUESTS)))
            .uri("lb://API-SERVICE"))
        .build();
}
```

```yaml
# application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: api-route
          uri: lb://API-SERVICE
          predicates:
            - Path=/api/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10   # 초당 10개 토큰
                redis-rate-limiter.burstCapacity: 20   # 최대 20개 버스트
                redis-rate-limiter.requestedTokens: 1  # 요청당 1개 토큰
                key-resolver: "#{@userKeyResolver}"

  redis:
    host: localhost
    port: 6379
```

### 계층별 Rate Limiting

```java
// 다중 레벨 Rate Limiting
@Configuration
public class MultiLevelRateLimiterConfig {

    // 글로벌 Rate Limiter
    @Bean
    public RedisRateLimiter globalRateLimiter() {
        return new RedisRateLimiter(1000, 2000, 1);  // 전체 1000 req/s
    }

    // 테넌트별 Rate Limiter
    @Bean
    public RedisRateLimiter tenantRateLimiter() {
        return new RedisRateLimiter(100, 200, 1);   // 테넌트당 100 req/s
    }

    // 사용자별 Rate Limiter
    @Bean
    public RedisRateLimiter userRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);     // 사용자당 10 req/s
    }
}

// 커스텀 Rate Limiter GatewayFilter
@Component
public class CustomRateLimiterFilter implements GatewayFilter {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-Id");

        // 다중 레벨 체크
        return checkRateLimit("global", 1000)
            .flatMap(allowed -> {
                if (!allowed) return reject(exchange);
                return checkRateLimit("tenant:" + tenantId, 100);
            })
            .flatMap(allowed -> {
                if (!allowed) return reject(exchange);
                return checkRateLimit("user:" + userId, 10);
            })
            .flatMap(allowed -> {
                if (!allowed) return reject(exchange);
                return chain.filter(exchange);
            });
    }

    private Mono<Boolean> checkRateLimit(String key, int limit) {
        // Redis INCR with EXPIRE
        String rateLimitKey = "rate_limit:" + key;
        return redisTemplate.opsForValue()
            .increment(rateLimitKey)
            .flatMap(count -> {
                if (count == 1) {
                    redisTemplate.expire(rateLimitKey, Duration.ofSeconds(1)).subscribe();
                }
                return Mono.just(count <= limit);
            });
    }

    private Mono<Void> reject(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        return exchange.getResponse().setComplete();
    }
}
```

---

## Spring Cloud Gateway

### 필터 체인

```
┌──────────────────────────────────────────────────────────────────┐
│                    Gateway 필터 체인                              │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Request                                                         │
│     │                                                            │
│     ▼                                                            │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ Global Filters (전역 필터)                                   │ │
│  │  - LoadBalancerClientFilter                                 │ │
│  │  - NettyRoutingFilter                                       │ │
│  │  - ForwardRoutingFilter                                     │ │
│  └─────────────────────────────────────────────────────────────┘ │
│     │                                                            │
│     ▼                                                            │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ Route Filters (라우트별 필터)                                │ │
│  │  - AddRequestHeader                                         │ │
│  │  - AddRequestParameter                                      │ │
│  │  - RewritePath                                              │ │
│  │  - CircuitBreaker                                           │ │
│  │  - RequestRateLimiter                                       │ │
│  └─────────────────────────────────────────────────────────────┘ │
│     │                                                            │
│     ▼                                                            │
│  Downstream Service                                              │
│     │                                                            │
│     ▼                                                            │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ Post Filters (응답 필터)                                     │ │
│  │  - ModifyResponseBody                                       │ │
│  │  - AddResponseHeader                                        │ │
│  └─────────────────────────────────────────────────────────────┘ │
│     │                                                            │
│     ▼                                                            │
│  Response                                                        │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### 커스텀 글로벌 필터

```java
// 로깅 필터
@Component
@Slf4j
public class LoggingGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        ServerHttpRequest request = exchange.getRequest().mutate()
            .header("X-Trace-Id", traceId)
            .build();

        log.info("Request: {} {} [{}]",
            request.getMethod(),
            request.getURI(),
            traceId);

        return chain.filter(exchange.mutate().request(request).build())
            .then(Mono.fromRunnable(() -> {
                long duration = System.currentTimeMillis() - startTime;
                log.info("Response: {} {} - {}ms [{}]",
                    request.getMethod(),
                    request.getURI(),
                    duration,
                    traceId);
            }));
    }

    @Override
    public int getOrder() {
        return -1;  // 가장 먼저 실행
    }
}

// 에러 처리 필터
@Component
@Slf4j
public class ErrorHandlingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
            .onErrorResume(Exception.class, e -> {
                log.error("Gateway error: {}", e.getMessage(), e);

                ServerHttpResponse response = exchange.getResponse();

                if (e instanceof ResponseStatusException rse) {
                    response.setStatusCode(rse.getStatusCode());
                } else {
                    response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                }

                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

                ErrorResponse errorResponse = new ErrorResponse(
                    "GATEWAY_ERROR",
                    e.getMessage()
                );

                byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
                DataBuffer buffer = response.bufferFactory().wrap(bytes);
                return response.writeWith(Mono.just(buffer));
            });
    }

    @Override
    public int getOrder() {
        return -2;  // 로깅보다 먼저
    }
}
```

### Fallback 처리

```java
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/orders")
    public ResponseEntity<FallbackResponse> ordersFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new FallbackResponse(
                "ORDER_SERVICE_UNAVAILABLE",
                "주문 서비스를 일시적으로 사용할 수 없습니다."
            ));
    }

    @GetMapping("/users")
    public ResponseEntity<FallbackResponse> usersFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new FallbackResponse(
                "USER_SERVICE_UNAVAILABLE",
                "사용자 서비스를 일시적으로 사용할 수 없습니다."
            ));
    }
}

// 라우트 설정
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: lb://ORDER-SERVICE
          predicates:
            - Path=/api/orders/**
          filters:
            - name: CircuitBreaker
              args:
                name: orderCircuitBreaker
                fallbackUri: forward:/fallback/orders
            - name: Retry
              args:
                retries: 3
                statuses: SERVICE_UNAVAILABLE
                backoff:
                  firstBackoff: 100ms
                  maxBackoff: 500ms
                  factor: 2
```

---

## 인증/인가

### JWT 검증 필터

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GatewayFilter, Ordered {

    private final JwtTokenProvider jwtTokenProvider;
    private final List<String> excludedPaths = List.of(
        "/api/auth/login",
        "/api/auth/register",
        "/api/public/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 인증 제외 경로
        if (isExcluded(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Missing or invalid Authorization header",
                          HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        try {
            if (!jwtTokenProvider.validateToken(token)) {
                return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
            }

            String userId = jwtTokenProvider.getUserId(token);
            List<String> roles = jwtTokenProvider.getRoles(token);

            // 다운스트림 서비스로 사용자 정보 전달
            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", userId)
                .header("X-User-Roles", String.join(",", roles))
                .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (ExpiredJwtException e) {
            return onError(exchange, "Token expired", HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            return onError(exchange, "Authentication failed", HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean isExcluded(String path) {
        return excludedPaths.stream()
            .anyMatch(pattern -> new AntPathMatcher().match(pattern, path));
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format("{\"error\":\"%s\",\"message\":\"%s\"}", status, message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}

// 필터 등록
@Configuration
public class GatewayFilterConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder,
                               JwtAuthenticationFilter jwtFilter) {
        return builder.routes()
            .route("secured-routes", r -> r
                .path("/api/**")
                .filters(f -> f.filter(jwtFilter))
                .uri("lb://API-SERVICE"))
            .build();
    }
}
```

---

## 핵심 정리

### API Gateway 기능 요약

| 기능 | 설명 | 구현 방법 |
|------|------|----------|
| 라우팅 | 요청을 적절한 서비스로 전달 | Predicates (Path, Header, Host) |
| 인증 | JWT/OAuth2 검증 | Global Filter |
| Rate Limiting | 요청 속도 제한 | Redis + Token Bucket |
| 로드 밸런싱 | 서비스 인스턴스 분산 | lb://SERVICE-NAME |
| Circuit Breaker | 장애 격리 | Resilience4j |
| 로깅 | 요청/응답 추적 | Global Filter |

### 설정 체크리스트

```
□ 서비스 디스커버리 연동 (Eureka)
□ Rate Limiting 설정 (Redis)
□ Circuit Breaker 설정
□ JWT 인증 필터
□ CORS 설정
□ 로깅 및 모니터링
□ Fallback 엔드포인트
□ 보안 헤더 추가
```

### 면접 대비 핵심 질문

1. **Q: API Gateway의 역할은 무엇인가요?**
   - A: 단일 진입점, 라우팅, 인증/인가, Rate Limiting, 로드 밸런싱, 프로토콜 변환, 로깅/모니터링 등. 클라이언트와 마이크로서비스 사이의 중간 계층

2. **Q: Rate Limiting 알고리즘을 설명해주세요**
   - A: 토큰 버킷: 일정 속도로 토큰 보충, 요청 시 소비. 버스트 허용하면서 평균 속도 제한. Redis로 분산 환경 지원

3. **Q: BFF 패턴이란?**
   - A: Backend for Frontend. 클라이언트(Web, Mobile)별 최적화된 API 제공. 각 클라이언트 요구사항에 맞춘 응답 형식, 인증 방식 적용 가능

4. **Q: Gateway에서 장애 전파를 어떻게 방지하나요?**
   - A: Circuit Breaker로 실패 서비스 격리, Fallback으로 대체 응답, Timeout 설정, Retry with backoff

---

*마지막 업데이트: 2025년 01월*

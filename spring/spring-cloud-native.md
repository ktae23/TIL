# Spring Cloud Native 종합 가이드

Cloud Native 애플리케이션 개발을 위한 핵심 개념과 Spring Cloud 컴포넌트를 정리합니다.

## 목차

- [1. Cloud Native란?](#1-cloud-native란)
- [2. 12 Factor App](#2-12-factor-app)
- [3. Spring Cloud 컴포넌트](#3-spring-cloud-컴포넌트)
- [4. 실무 적용 패턴](#4-실무-적용-패턴)

---

## 1. Cloud Native란?

### 1.1 정의

**Cloud Native**는 클라우드 환경의 이점을 최대한 활용하도록 설계된 애플리케이션 개발 방식입니다.

CNCF(Cloud Native Computing Foundation)의 정의:
> Cloud Native 기술은 퍼블릭, 프라이빗, 하이브리드 클라우드 환경에서 확장 가능한 애플리케이션을 구축하고 실행할 수 있게 해줍니다.

### 1.2 핵심 특성

| 특성 | 설명 | 예시 |
|------|------|------|
| **컨테이너화** | 애플리케이션을 컨테이너로 패키징 | Docker, Kubernetes |
| **마이크로서비스** | 작은 독립적인 서비스로 분리 | 각 도메인별 서비스 분리 |
| **동적 오케스트레이션** | 자동화된 배포, 스케일링, 관리 | Kubernetes, Docker Swarm |
| **DevOps** | 개발과 운영의 통합 | CI/CD 파이프라인 |

### 1.3 전통적 애플리케이션 vs Cloud Native

```
┌─────────────────────────────────────────────────────────────┐
│                    Traditional                               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Monolithic Application                  │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐   │   │
│  │  │  UI     │ │ Business│ │  Data   │ │  Auth   │   │   │
│  │  │ Layer   │ │  Logic  │ │  Layer  │ │ Module  │   │   │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘   │   │
│  └─────────────────────────────────────────────────────┘   │
│                         │                                   │
│                    Single DB                                │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    Cloud Native                              │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐        │
│  │ Service │  │ Service │  │ Service │  │ Service │        │
│  │    A    │  │    B    │  │    C    │  │    D    │        │
│  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘        │
│       │            │            │            │              │
│    ┌──┴──┐     ┌──┴──┐     ┌──┴──┐     ┌──┴──┐            │
│    │ DB  │     │ DB  │     │Cache│     │ DB  │            │
│    └─────┘     └─────┘     └─────┘     └─────┘            │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. 12 Factor App

Heroku에서 제안한 **SaaS 애플리케이션 개발 방법론**입니다. Cloud Native 앱 설계의 기본 원칙으로 널리 활용됩니다.

### 2.1 12가지 원칙 요약

| # | Factor | 설명 | Spring 적용 |
|---|--------|------|-------------|
| 1 | **Codebase** | 하나의 코드베이스, 여러 배포 | Git + 환경별 설정 분리 |
| 2 | **Dependencies** | 명시적 의존성 선언 | Maven/Gradle |
| 3 | **Config** | 환경에 설정 저장 | Spring Cloud Config |
| 4 | **Backing Services** | 백엔드 서비스를 연결된 리소스로 취급 | DataSource 추상화 |
| 5 | **Build, Release, Run** | 빌드와 실행 단계 분리 | CI/CD 파이프라인 |
| 6 | **Processes** | 무상태 프로세스로 실행 | Stateless 서비스 설계 |
| 7 | **Port Binding** | 포트 바인딩으로 서비스 노출 | Embedded Tomcat |
| 8 | **Concurrency** | 프로세스 모델을 통한 확장 | 수평적 스케일링 |
| 9 | **Disposability** | 빠른 시작과 graceful shutdown | Spring Boot Actuator |
| 10 | **Dev/Prod Parity** | 개발/운영 환경 일치 | Docker, Testcontainers |
| 11 | **Logs** | 로그를 이벤트 스트림으로 취급 | Logback + ELK |
| 12 | **Admin Processes** | 관리 작업을 일회성 프로세스로 실행 | Spring Batch |

### 2.2 주요 Factor 상세

#### Factor 3: Config - 환경 설정 분리

```yaml
# application.yml - 공통 설정
spring:
  application:
    name: order-service

# application-dev.yml - 개발 환경
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/orders

# application-prod.yml - 운영 환경
spring:
  datasource:
    url: ${DB_URL}  # 환경변수에서 주입
```

#### Factor 6: Processes - 무상태 설계

```java
// ❌ Bad: 상태를 메모리에 저장
@Service
public class CartService {
    private Map<String, Cart> cartCache = new HashMap<>();  // 인스턴스 종료 시 유실
}

// ✅ Good: 외부 저장소 사용
@Service
public class CartService {
    private final RedisTemplate<String, Cart> redisTemplate;

    public void saveCart(String userId, Cart cart) {
        redisTemplate.opsForValue().set("cart:" + userId, cart);
    }
}
```

#### Factor 9: Disposability - Graceful Shutdown

```yaml
# application.yml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

```java
@Component
public class GracefulShutdownHandler implements DisposableBean {

    @Override
    public void destroy() throws Exception {
        // 진행 중인 작업 완료 대기
        // 리소스 정리
        log.info("Graceful shutdown completed");
    }
}
```

---

## 3. Spring Cloud 컴포넌트

### 3.1 아키텍처 개요

```
                         ┌─────────────────┐
                         │  Spring Cloud   │
                         │     Gateway     │
                         └────────┬────────┘
                                  │
        ┌─────────────────────────┼─────────────────────────┐
        │                         │                         │
        ▼                         ▼                         ▼
┌───────────────┐       ┌───────────────┐       ┌───────────────┐
│   Service A   │       │   Service B   │       │   Service C   │
│  (Order)      │◄─────►│  (Inventory)  │◄─────►│  (Payment)    │
└───────┬───────┘       └───────┬───────┘       └───────┬───────┘
        │                       │                       │
        └───────────────────────┼───────────────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        │                       │                       │
        ▼                       ▼                       ▼
┌───────────────┐       ┌───────────────┐       ┌───────────────┐
│    Eureka     │       │ Config Server │       │   Zipkin      │
│  (Discovery)  │       │ (설정 관리)    │       │  (Tracing)    │
└───────────────┘       └───────────────┘       └───────────────┘
```

### 3.2 Spring Cloud Config

중앙 집중식 설정 관리 서버입니다.

#### Config Server 설정

```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

```yaml
# Config Server - application.yml
server:
  port: 8888

spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/myorg/config-repo
          default-label: main
          search-paths: '{application}'
```

#### Config Client 설정

```yaml
# Client - application.yml
spring:
  application:
    name: order-service
  config:
    import: "configserver:http://localhost:8888"
```

#### 설정 동적 갱신

```java
@RestController
@RefreshScope  // 설정 변경 시 Bean 재생성
public class OrderController {

    @Value("${order.max-items}")
    private int maxItems;

    // POST /actuator/refresh 호출 시 갱신됨
}
```

### 3.3 Service Discovery (Eureka)

서비스 등록 및 탐색을 담당합니다.

#### Eureka Server

```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

```yaml
# Eureka Server - application.yml
server:
  port: 8761

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
  server:
    enable-self-preservation: false
```

#### Eureka Client

```yaml
# Service - application.yml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${random.value}
```

### 3.4 Spring Cloud Gateway

API Gateway 역할을 수행합니다.

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: lb://ORDER-SERVICE  # Eureka 서비스명
          predicates:
            - Path=/api/orders/**
          filters:
            - StripPrefix=1
            - name: CircuitBreaker
              args:
                name: orderCircuitBreaker
                fallbackUri: forward:/fallback/orders

        - id: inventory-service
          uri: lb://INVENTORY-SERVICE
          predicates:
            - Path=/api/inventory/**
          filters:
            - StripPrefix=1
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
```

#### Custom Filter 구현

```java
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = exchange.getRequest()
            .getHeaders()
            .getFirst("Authorization");

        if (token == null || !validateToken(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1;  // 높은 우선순위
    }
}
```

### 3.5 Resilience4j (Circuit Breaker)

장애 전파 방지를 위한 회로 차단기입니다.

```java
@Service
public class OrderService {

    private final InventoryClient inventoryClient;

    @CircuitBreaker(name = "inventory", fallbackMethod = "fallbackInventory")
    @Retry(name = "inventory")
    @TimeLimiter(name = "inventory")
    public CompletableFuture<InventoryResponse> checkInventory(String productId) {
        return CompletableFuture.supplyAsync(
            () -> inventoryClient.check(productId)
        );
    }

    public CompletableFuture<InventoryResponse> fallbackInventory(
            String productId, Throwable t) {
        log.warn("Fallback for inventory check: {}", t.getMessage());
        return CompletableFuture.completedFuture(
            new InventoryResponse(productId, false, "Service unavailable")
        );
    }
}
```

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      inventory:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 3

  retry:
    instances:
      inventory:
        max-attempts: 3
        wait-duration: 500ms

  timelimiter:
    instances:
      inventory:
        timeout-duration: 3s
```

### 3.6 Spring Cloud Sleuth & Zipkin (분산 추적)

마이크로서비스 간 요청 추적을 담당합니다.

> **참고**: Spring Boot 3.x부터는 Micrometer Tracing으로 대체되었습니다.

```yaml
# Spring Boot 3.x - application.yml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% 샘플링 (운영에서는 조정)
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```

```java
@RestController
@Slf4j
public class OrderController {

    @GetMapping("/orders/{id}")
    public Order getOrder(@PathVariable String id) {
        // traceId, spanId가 자동으로 로그에 포함됨
        log.info("Fetching order: {}", id);
        return orderService.findById(id);
    }
}

// 로그 출력 예시:
// 2024-01-15 10:30:00 [order-service,65abc123,45def789] INFO - Fetching order: 123
```

### 3.7 컴포넌트 비교 요약

| 컴포넌트 | 역할 | 대안 |
|----------|------|------|
| **Config Server** | 중앙 설정 관리 | Consul, Vault, AWS Parameter Store |
| **Eureka** | 서비스 디스커버리 | Consul, Kubernetes Service |
| **Gateway** | API Gateway | Kong, NGINX, AWS API Gateway |
| **Resilience4j** | 회로 차단기 | Sentinel |
| **Sleuth/Micrometer** | 분산 추적 | Jaeger, OpenTelemetry |

---

## 4. 실무 적용 패턴

### 4.1 기본 프로젝트 구조

```
cloud-native-app/
├── config-server/           # 설정 서버
├── discovery-server/        # Eureka 서버
├── gateway-server/          # API Gateway
├── services/
│   ├── order-service/
│   ├── inventory-service/
│   └── payment-service/
├── common/                  # 공통 라이브러리
│   ├── common-dto/
│   └── common-security/
└── docker-compose.yml
```

### 4.2 Docker Compose 예시

```yaml
version: '3.8'
services:
  config-server:
    build: ./config-server
    ports:
      - "8888:8888"

  discovery-server:
    build: ./discovery-server
    ports:
      - "8761:8761"
    depends_on:
      - config-server

  gateway:
    build: ./gateway-server
    ports:
      - "8080:8080"
    depends_on:
      - discovery-server

  order-service:
    build: ./services/order-service
    deploy:
      replicas: 2
    depends_on:
      - discovery-server
      - config-server
```

### 4.3 권장 의존성 (Spring Boot 3.x)

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- Config Client -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-config</artifactId>
    </dependency>

    <!-- Service Discovery -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>

    <!-- Circuit Breaker -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
    </dependency>

    <!-- Distributed Tracing -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-tracing-bridge-brave</artifactId>
    </dependency>
    <dependency>
        <groupId>io.zipkin.reporter2</groupId>
        <artifactId>zipkin-reporter-brave</artifactId>
    </dependency>
</dependencies>
```

---

## 참고 자료

- [Spring Cloud 공식 문서](https://spring.io/projects/spring-cloud)
- [12 Factor App](https://12factor.net/ko/)
- [CNCF Cloud Native Definition](https://github.com/cncf/toc/blob/main/DEFINITION.md)
- [Resilience4j 공식 문서](https://resilience4j.readme.io/)

*마지막 업데이트: 2026년 01월*

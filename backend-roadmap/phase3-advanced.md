# Phase 3: 고급 단계 - 대규모 시스템 설계

대규모 트래픽을 처리하고 안정적인 시스템을 운영하기 위한 아키텍처, 인프라, DevOps 기술을 학습합니다.

## 목차

- [1. 아키텍처 패턴](#1-아키텍처-패턴)
- [2. 메시지 브로커와 이벤트 기반 아키텍처](#2-메시지-브로커와-이벤트-기반-아키텍처)
- [3. 캐싱 전략](#3-캐싱-전략)
- [4. 데이터베이스 확장](#4-데이터베이스-확장)
- [5. 보안](#5-보안)
- [6. DevOps와 인프라](#6-devops와-인프라)
- [7. 시스템 디자인](#7-시스템-디자인)
- [8. 고급 단계 체크리스트](#8-고급-단계-체크리스트)

---

## 1. 아키텍처 패턴

### 모놀리식 아키텍처

```
┌─────────────────────────────────────────┐
│           Monolithic Application         │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐   │
│  │  User   │ │  Order  │ │ Product │   │
│  │ Module  │ │ Module  │ │ Module  │   │
│  └────┬────┘ └────┬────┘ └────┬────┘   │
│       │           │           │         │
│       └───────────┼───────────┘         │
│                   │                      │
│           ┌───────┴───────┐              │
│           │   Database    │              │
│           └───────────────┘              │
└─────────────────────────────────────────┘
```

| 장점 | 단점 |
|------|------|
| 개발/배포 단순 | 규모 커지면 복잡도 증가 |
| 디버깅 용이 | 부분 스케일링 불가 |
| 트랜잭션 관리 쉬움 | 기술 스택 변경 어려움 |
| 초기 개발 빠름 | 배포 위험도 높음 |

**적합한 경우**: 초기 스타트업, 소규모 팀, MVP 개발

### 마이크로서비스 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                      API Gateway                         │
└────────────────────────┬────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
         ▼               ▼               ▼
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│   User      │  │   Order     │  │  Product    │
│  Service    │  │  Service    │  │  Service    │
├─────────────┤  ├─────────────┤  ├─────────────┤
│  User DB    │  │  Order DB   │  │ Product DB  │
└─────────────┘  └─────────────┘  └─────────────┘
         │               │               │
         └───────────────┼───────────────┘
                         │
              ┌──────────┴──────────┐
              │   Message Broker    │
              │   (Kafka/RabbitMQ)  │
              └─────────────────────┘
```

| 장점 | 단점 |
|------|------|
| 독립적 배포 | 분산 시스템 복잡도 |
| 기술 스택 자유 | 네트워크 지연 |
| 장애 격리 | 데이터 일관성 관리 |
| 팀별 독립 개발 | 운영 오버헤드 |

**적합한 경우**: 대규모 서비스, 다양한 팀, 높은 확장성 필요

### 서버리스 아키텍처

```
┌──────────────┐
│   Trigger    │
│ (HTTP, Event)│
└──────┬───────┘
       │
       ▼
┌──────────────┐      ┌──────────────┐
│   Function   │─────>│   Database   │
│  (Lambda)    │      │ (DynamoDB)   │
└──────────────┘      └──────────────┘
       │
       ▼
┌──────────────┐
│   Response   │
└──────────────┘
```

```javascript
// AWS Lambda 예시
exports.handler = async (event) => {
    const userId = event.pathParameters.id;

    const user = await dynamoDB.get({
        TableName: 'users',
        Key: { id: userId }
    }).promise();

    return {
        statusCode: 200,
        body: JSON.stringify(user.Item)
    };
};
```

| 장점 | 단점 |
|------|------|
| 인프라 관리 불필요 | Cold Start 지연 |
| 사용량 기반 과금 | 실행 시간 제한 |
| 자동 스케일링 | 벤더 종속 |
| 빠른 프로토타이핑 | 로컬 테스트 어려움 |

### 아키텍처 선택 가이드

```
프로젝트 시작 단계
└── 모놀리식으로 시작
    │
    ├── 트래픽 < 10K DAU
    │   └── 모놀리식 유지
    │
    ├── 트래픽 10K ~ 100K DAU
    │   └── 모듈화된 모놀리식 또는 서버리스
    │
    └── 트래픽 > 100K DAU, 팀 규모 증가
        └── 점진적 마이크로서비스 전환
```

---

## 2. 메시지 브로커와 이벤트 기반 아키텍처

### 동기 vs 비동기 통신

```
동기 통신 (Synchronous)
┌────────┐  Request  ┌────────┐
│Service │──────────>│Service │
│   A    │<──────────│   B    │
└────────┘  Response └────────┘
(B가 응답할 때까지 대기)

비동기 통신 (Asynchronous)
┌────────┐  Publish  ┌─────────┐  Consume  ┌────────┐
│Service │──────────>│ Message │<──────────│Service │
│   A    │           │ Broker  │           │   B    │
└────────┘           └─────────┘           └────────┘
(A는 발행 후 바로 다음 작업)
```

### 메시지 브로커 비교

| 특성 | RabbitMQ | Apache Kafka | Redis Pub/Sub |
|------|----------|--------------|---------------|
| **처리량** | 중간 | 매우 높음 | 높음 |
| **메시지 보존** | 소비 후 삭제 | 설정 기간 보존 | 보존 안 함 |
| **라우팅** | 복잡한 라우팅 지원 | 토픽 기반 | 단순 채널 |
| **순서 보장** | 큐 내 보장 | 파티션 내 보장 | 보장 안 함 |
| **재처리** | 어려움 | 쉬움 (오프셋) | 불가 |
| **사용 사례** | 작업 큐, 복잡한 라우팅 | 로그, 이벤트 스트리밍 | 실시간 알림 |

### RabbitMQ 사용 예시

```java
// Producer
@Service
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishOrderCreated(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getId(),
            order.getUserId(),
            order.getTotalAmount()
        );

        rabbitTemplate.convertAndSend(
            "order-exchange",
            "order.created",
            event
        );
    }
}

// Consumer
@Component
public class OrderEventConsumer {

    @RabbitListener(queues = "inventory-queue")
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 재고 차감 로직
        inventoryService.decreaseStock(event.getOrderItems());
    }

    @RabbitListener(queues = "notification-queue")
    public void sendOrderNotification(OrderCreatedEvent event) {
        // 알림 발송
        notificationService.sendOrderConfirmation(event.getUserId());
    }
}
```

```yaml
# application.yml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

### Apache Kafka 사용 예시

```java
// Producer
@Service
public class EventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String topic, String key, Object event) {
        kafkaTemplate.send(topic, key, event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send message", ex);
                }
            });
    }
}

// Consumer
@Component
public class OrderEventConsumer {

    @KafkaListener(
        topics = "order-events",
        groupId = "inventory-service"
    )
    public void consume(
            @Payload OrderEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received: partition={}, offset={}", partition, offset);
        processEvent(event);
    }
}
```

### 이벤트 기반 아키텍처 패턴

#### Event Sourcing

```
전통적 방식: 현재 상태만 저장
┌─────────────────┐
│ Account         │
│ balance: 5000   │
└─────────────────┘

Event Sourcing: 모든 변경 이벤트 저장
┌─────────────────────────────────────┐
│ Event Store                          │
├─────────────────────────────────────┤
│ 1. AccountCreated(balance: 10000)   │
│ 2. MoneyWithdrawn(amount: 3000)     │
│ 3. MoneyDeposited(amount: 2000)     │
│ 4. MoneyWithdrawn(amount: 4000)     │
│                                      │
│ Current State: 10000 - 3000 + 2000  │
│              - 4000 = 5000           │
└─────────────────────────────────────┘
```

#### CQRS (Command Query Responsibility Segregation)

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
   ┌───┴───┐
   │       │
   ▼       ▼
┌─────┐  ┌─────┐
│Write│  │Read │
│Model│  │Model│
└──┬──┘  └──┬──┘
   │        │
   ▼        ▼
┌─────┐  ┌─────┐
│Write│  │Read │
│ DB  │  │ DB  │
└─────┘  └─────┘
   │        ▲
   └────────┘
    동기화
```

---

## 3. 캐싱 전략

### 캐시 레이어

```
┌────────┐   ┌────────┐   ┌────────┐   ┌────────┐   ┌────────┐
│ Client │──>│  CDN   │──>│  App   │──>│ Cache  │──>│   DB   │
└────────┘   └────────┘   └────────┘   └────────┘   └────────┘
              정적 콘텐츠    비즈니스 로직   Redis 등    PostgreSQL
```

### 캐싱 패턴

#### Cache-Aside (Lazy Loading)

```java
public User getUser(Long userId) {
    String key = "user:" + userId;

    // 1. 캐시 조회
    User cached = redisTemplate.opsForValue().get(key);
    if (cached != null) {
        return cached;
    }

    // 2. DB 조회
    User user = userRepository.findById(userId)
        .orElseThrow(UserNotFoundException::new);

    // 3. 캐시 저장
    redisTemplate.opsForValue().set(key, user, Duration.ofHours(1));

    return user;
}
```

```
┌────────┐     ┌────────┐     ┌────────┐
│  App   │────>│ Cache  │     │   DB   │
└───┬────┘     └───┬────┘     └───┬────┘
    │              │              │
    │  1. Get      │              │
    │─────────────>│              │
    │              │              │
    │  2. Miss     │              │
    │<─────────────│              │
    │              │              │
    │  3. Get from DB             │
    │────────────────────────────>│
    │              │              │
    │  4. Return data             │
    │<────────────────────────────│
    │              │              │
    │  5. Set cache│              │
    │─────────────>│              │
```

#### Write-Through

```java
public User updateUser(Long userId, UpdateUserRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(UserNotFoundException::new);

    user.update(request);

    // DB 저장
    userRepository.save(user);

    // 캐시도 함께 업데이트
    String key = "user:" + userId;
    redisTemplate.opsForValue().set(key, user, Duration.ofHours(1));

    return user;
}
```

#### Write-Behind (Write-Back)

```
쓰기 요청 → 캐시만 업데이트 → 비동기로 DB에 배치 저장

장점: 쓰기 성능 향상
단점: 데이터 유실 위험
```

### Redis 활용

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}

@Service
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    // String
    public void setString(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    // Hash (객체 필드별 저장)
    public void setHash(String key, Map<String, Object> fields) {
        redisTemplate.opsForHash().putAll(key, fields);
    }

    // Set (고유값 컬렉션)
    public void addToSet(String key, String... values) {
        redisTemplate.opsForSet().add(key, values);
    }

    // Sorted Set (랭킹)
    public void addToRanking(String key, String member, double score) {
        redisTemplate.opsForZSet().add(key, member, score);
    }

    // 조회수 증가 (원자적 연산)
    public Long incrementViewCount(Long postId) {
        String key = "post:views:" + postId;
        return redisTemplate.opsForValue().increment(key);
    }
}
```

### 캐시 무효화 전략

| 전략 | 설명 | 적합한 경우 |
|------|------|------------|
| **TTL** | 일정 시간 후 자동 만료 | 변경이 적은 데이터 |
| **Cache-Aside + 삭제** | 데이터 변경 시 캐시 삭제 | 일반적인 CRUD |
| **이벤트 기반** | 변경 이벤트로 캐시 갱신 | 분산 시스템 |

```java
// 데이터 변경 시 캐시 무효화
@Transactional
public User updateUser(Long userId, UpdateUserRequest request) {
    User user = userRepository.findById(userId).orElseThrow();
    user.update(request);
    userRepository.save(user);

    // 캐시 삭제 (다음 조회 시 갱신)
    redisTemplate.delete("user:" + userId);

    return user;
}
```

---

## 4. 데이터베이스 확장

### 읽기 성능 향상: Read Replica

```
┌────────────┐
│   Client   │
└─────┬──────┘
      │
┌─────┴──────┐
│            │
▼            ▼
WRITE       READ
│            │
▼            ▼
┌────────┐   ┌────────┐ ┌────────┐
│ Master │──>│Replica1│ │Replica2│
│   DB   │   │   DB   │ │   DB   │
└────────┘   └────────┘ └────────┘
   쓰기         읽기       읽기
```

```java
// Spring 읽기/쓰기 분리 설정
@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.master")
    public DataSource masterDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.replica")
    public DataSource replicaDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public DataSource routingDataSource() {
        ReplicationRoutingDataSource routingDataSource =
            new ReplicationRoutingDataSource();

        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put("master", masterDataSource());
        dataSourceMap.put("replica", replicaDataSource());

        routingDataSource.setTargetDataSources(dataSourceMap);
        routingDataSource.setDefaultTargetDataSource(masterDataSource());

        return routingDataSource;
    }
}

// @Transactional(readOnly = true) 사용 시 Replica로 라우팅
@Service
@Transactional(readOnly = true)
public class UserQueryService {

    public List<User> findAll() {
        return userRepository.findAll(); // Replica에서 조회
    }
}
```

### 샤딩 (Sharding)

```
데이터를 여러 DB에 분산 저장

┌─────────────────────────────────────────────────┐
│                  Shard Router                    │
└────────────────────────┬────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
         ▼               ▼               ▼
   ┌──────────┐    ┌──────────┐    ┌──────────┐
   │ Shard 1  │    │ Shard 2  │    │ Shard 3  │
   │ ID 1-100M│    │ID 100M-  │    │ID 200M-  │
   │          │    │   200M   │    │   300M   │
   └──────────┘    └──────────┘    └──────────┘
```

#### 샤딩 전략

| 전략 | 설명 | 장단점 |
|------|------|--------|
| **Range** | ID 범위로 분할 | 구현 쉬움, 핫스팟 발생 가능 |
| **Hash** | 해시값으로 분할 | 균등 분산, 범위 쿼리 어려움 |
| **Directory** | 매핑 테이블 사용 | 유연함, 매핑 관리 필요 |

```java
// 해시 기반 샤딩 예시
public int getShardId(Long userId, int shardCount) {
    return (int) (userId % shardCount);
}

// 또는 Consistent Hashing
public int getShardByConsistentHash(String key) {
    return consistentHashRing.getNode(key);
}
```

### 인덱스 최적화

```sql
-- 복합 인덱스 (순서 중요!)
CREATE INDEX idx_orders_user_status_date
ON orders (user_id, status, created_at DESC);

-- 커버링 인덱스 (SELECT 컬럼이 모두 인덱스에 포함)
CREATE INDEX idx_users_email_name
ON users (email, name);

SELECT email, name FROM users WHERE email = 'test@example.com';
-- 테이블 접근 없이 인덱스만으로 결과 반환

-- 부분 인덱스 (특정 조건만)
CREATE INDEX idx_orders_pending
ON orders (created_at)
WHERE status = 'PENDING';

-- 실행 계획 확인
EXPLAIN ANALYZE
SELECT * FROM orders
WHERE user_id = 1 AND status = 'COMPLETED'
ORDER BY created_at DESC
LIMIT 10;
```

### 쿼리 최적화

```sql
-- ❌ N+1 문제
SELECT * FROM users WHERE id = 1;
SELECT * FROM orders WHERE user_id = 1;  -- 반복 쿼리

-- ✅ JOIN으로 해결
SELECT u.*, o.*
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
WHERE u.id = 1;

-- ✅ Batch 조회
SELECT * FROM users WHERE id IN (1, 2, 3, 4, 5);
SELECT * FROM orders WHERE user_id IN (1, 2, 3, 4, 5);
```

---

## 5. 보안

### OWASP Top 10 대응

#### SQL Injection 방지

```java
// ❌ 취약한 코드
String query = "SELECT * FROM users WHERE email = '" + email + "'";

// ✅ PreparedStatement 사용
@Query("SELECT u FROM User u WHERE u.email = :email")
User findByEmail(@Param("email") String email);

// ✅ Criteria API
CriteriaBuilder cb = em.getCriteriaBuilder();
CriteriaQuery<User> query = cb.createQuery(User.class);
Root<User> root = query.from(User.class);
query.where(cb.equal(root.get("email"), email));
```

#### XSS 방지

```java
// 입력값 이스케이프
import org.apache.commons.text.StringEscapeUtils;

String escaped = StringEscapeUtils.escapeHtml4(userInput);

// Spring Security 기본 헤더
// X-XSS-Protection: 1; mode=block
// X-Content-Type-Options: nosniff
// Content-Security-Policy: default-src 'self'
```

#### CSRF 방지

```java
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // REST API는 보통 비활성화 (JWT 사용 시)
            .csrf(csrf -> csrf.disable())
            // 또는 토큰 기반 CSRF
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            );
        return http.build();
    }
}
```

### 비밀번호 보안

```java
@Service
public class PasswordService {

    private final PasswordEncoder passwordEncoder;

    public PasswordService() {
        // BCrypt 사용 (권장)
        this.passwordEncoder = new BCryptPasswordEncoder(12);
    }

    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
```

### Rate Limiting

```java
// Bucket4j를 사용한 Rate Limiting
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String clientIp = request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(clientIp, this::createBucket);

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.getWriter().write("Too Many Requests");
        }
    }

    private Bucket createBucket(String key) {
        // 분당 100 요청 제한
        return Bucket.builder()
            .addLimit(Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1))))
            .build();
    }
}
```

---

## 6. DevOps와 인프라

### Docker

```dockerfile
# 멀티 스테이지 빌드
FROM gradle:8-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle build -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# 보안: non-root 사용자
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```yaml
# docker-compose.yml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DATABASE_URL=jdbc:postgresql://db:5432/myapp
    depends_on:
      - db
      - redis

  db:
    image: postgres:15
    environment:
      POSTGRES_DB: myapp
      POSTGRES_USER: user
      POSTGRES_PASSWORD: password
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

volumes:
  postgres_data:
```

### Kubernetes 기초

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: backend
  template:
    metadata:
      labels:
        app: backend
    spec:
      containers:
      - name: app
        image: myapp:1.0.0
        ports:
        - containerPort: 8080
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: backend-service
spec:
  selector:
    app: backend
  ports:
  - port: 80
    targetPort: 8080
  type: LoadBalancer
```

### CI/CD 파이프라인

```yaml
# .github/workflows/ci-cd.yml
name: CI/CD Pipeline

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Run tests
        run: ./gradlew test

      - name: Upload coverage
        uses: codecov/codecov-action@v3

  build:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4

      - name: Build Docker image
        run: docker build -t myapp:${{ github.sha }} .

      - name: Push to registry
        run: |
          docker tag myapp:${{ github.sha }} registry/myapp:${{ github.sha }}
          docker push registry/myapp:${{ github.sha }}

  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to Kubernetes
        run: |
          kubectl set image deployment/backend-app \
            app=registry/myapp:${{ github.sha }}
```

### 모니터링

```yaml
# Prometheus + Grafana 스택
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'spring-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['app:8080']
```

```java
// Spring Actuator + Micrometer 설정
// application.yml
management:
  endpoints:
    web:
      exposure:
        include: health, metrics, prometheus
  metrics:
    tags:
      application: ${spring.application.name}
```

---

## 7. 시스템 디자인

### 설계 접근법

```
1. 요구사항 명확화 (5분)
   - 기능 요구사항
   - 비기능 요구사항 (트래픽, 지연시간, 가용성)

2. 추정 (5분)
   - DAU, QPS 계산
   - 스토리지 용량

3. 고수준 설계 (10분)
   - 핵심 컴포넌트 다이어그램
   - API 엔드포인트

4. 상세 설계 (15분)
   - DB 스키마
   - 주요 알고리즘
   - 확장성 고려

5. 병목점 해결 (10분)
   - 성능 최적화
   - 장애 대응
```

### URL 단축 서비스 설계 예시

```
요구사항:
- 긴 URL → 짧은 URL 변환
- 짧은 URL → 원본 URL 리다이렉트
- 1억 URL/월 생성
- 10:1 읽기/쓰기 비율

추정:
- 쓰기: 100M / 30 / 24 / 3600 ≈ 40 QPS
- 읽기: 400 QPS
- 5년 저장: 100M × 12 × 5 = 6B records
- 스토리지: 6B × 500B = 3TB

설계:
┌────────────┐     ┌────────────┐     ┌────────────┐
│   Client   │────>│  LB + CDN  │────>│ API Server │
└────────────┘     └────────────┘     └─────┬──────┘
                                            │
                        ┌───────────────────┼───────────────────┐
                        │                   │                   │
                        ▼                   ▼                   ▼
                  ┌──────────┐        ┌──────────┐        ┌──────────┐
                  │  Redis   │        │ ID 생성기 │        │    DB    │
                  │  Cache   │        │(Snowflake)│        │  (Shard) │
                  └──────────┘        └──────────┘        └──────────┘
```

---

## 8. 고급 단계 체크리스트

### 아키텍처
- [ ] 모놀리식 vs 마이크로서비스 트레이드오프 이해
- [ ] API Gateway 개념
- [ ] 서비스 간 통신 패턴

### 메시지 브로커
- [ ] RabbitMQ 또는 Kafka 사용 경험
- [ ] 이벤트 기반 아키텍처 이해
- [ ] 비동기 처리 구현

### 캐싱
- [ ] Redis 기본 자료구조 활용
- [ ] Cache-Aside 패턴 구현
- [ ] 캐시 무효화 전략

### 데이터베이스
- [ ] Read Replica 구성
- [ ] 인덱스 최적화
- [ ] 쿼리 성능 분석

### 보안
- [ ] OWASP Top 10 대응
- [ ] Rate Limiting 구현
- [ ] 안전한 인증/인가

### DevOps
- [ ] Docker 이미지 빌드
- [ ] CI/CD 파이프라인 구축
- [ ] 기본적인 모니터링 설정
- [ ] Kubernetes 기초 (선택)

### 시스템 디자인
- [ ] 시스템 설계 접근 방법론
- [ ] URL 단축 서비스 설계
- [ ] SNS 피드 시스템 설계 (선택)

---

*마지막 업데이트: 2026년 01월*

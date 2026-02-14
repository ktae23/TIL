# Redis 분산 락 구현 가이드

분산 환경에서 동시성을 제어하기 위한 Redis 기반 분산 락 구현 방법을 정리합니다.

## 목차

1. [분산 락이 필요한 상황](#1-분산-락이-필요한-상황)
2. [SETNX 기반 단순 구현](#2-setnx-기반-단순-구현)
3. [Redisson을 활용한 구현](#3-redisson을-활용한-구현)
4. [Redlock 알고리즘](#4-redlock-알고리즘)
5. [실무 활용 패턴](#5-실무-활용-패턴)
6. [주의사항 및 Best Practices](#6-주의사항-및-best-practices)

---

## 1. 분산 락이 필요한 상황

### 문제 상황: 동시성 이슈

```
[서버 A]                    [서버 B]
    │                           │
    ├── 재고 조회: 100개        │
    │                           ├── 재고 조회: 100개
    ├── 주문 처리               │
    │                           ├── 주문 처리
    ├── 재고 차감: 99개         │
    │                           ├── 재고 차감: 99개 (!!!)
    │                           │
    └── 실제 주문: 2건, 재고: 99개 (1개 오버셀링)
```

### 분산 락으로 해결

```
[서버 A]                    [서버 B]
    │                           │
    ├── 락 획득 시도            │
    ├── 락 획득 성공 ✓         │
    │                           ├── 락 획득 시도
    ├── 재고 조회: 100개        │   (대기...)
    ├── 주문 처리               │
    ├── 재고 차감: 99개         │
    ├── 락 해제                 │
    │                           ├── 락 획득 성공 ✓
    │                           ├── 재고 조회: 99개
    │                           ├── 주문 처리
    │                           ├── 재고 차감: 98개
    │                           ├── 락 해제
```

---

## 2. SETNX 기반 단순 구현

### 기본 구현

```java
@Component
@RequiredArgsConstructor
public class SimpleRedisLock {

    private final StringRedisTemplate redisTemplate;

    /**
     * 락 획득 시도
     * @return 락 획득 성공 여부
     */
    public boolean tryLock(String key, String value, Duration timeout) {
        Boolean result = redisTemplate.opsForValue()
            .setIfAbsent(key, value, timeout);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 락 해제 (본인이 획득한 락만 해제)
     */
    public boolean unlock(String key, String value) {
        String script = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            else
                return 0
            end
            """;

        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            List.of(key),
            value
        );
        return Long.valueOf(1L).equals(result);
    }
}
```

### 사용 예시

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final SimpleRedisLock redisLock;
    private final StockRepository stockRepository;

    public void createOrder(Long productId, int quantity) {
        String lockKey = "lock:stock:" + productId;
        String lockValue = UUID.randomUUID().toString();

        try {
            // 락 획득 시도 (최대 5초 대기, 락 만료 10초)
            boolean acquired = tryLockWithRetry(lockKey, lockValue,
                Duration.ofSeconds(10), Duration.ofSeconds(5));

            if (!acquired) {
                throw new LockAcquisitionException("락 획득 실패");
            }

            // 비즈니스 로직 수행
            Stock stock = stockRepository.findByProductId(productId);
            stock.decrease(quantity);
            stockRepository.save(stock);

        } finally {
            redisLock.unlock(lockKey, lockValue);
        }
    }

    private boolean tryLockWithRetry(String key, String value,
            Duration lockTimeout, Duration waitTimeout) {
        long endTime = System.currentTimeMillis() + waitTimeout.toMillis();

        while (System.currentTimeMillis() < endTime) {
            if (redisLock.tryLock(key, value, lockTimeout)) {
                return true;
            }
            try {
                Thread.sleep(100);  // 100ms 대기 후 재시도
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
```

### 단순 구현의 한계

```
1. 스핀락으로 인한 리소스 낭비
   - 락 획득까지 계속 재시도

2. 락 타임아웃 설정 어려움
   - 너무 짧으면: 작업 완료 전 락 만료
   - 너무 길면: 장애 시 오래 대기

3. 단일 Redis 장애 시 문제
   - 마스터 장애 → 락 정보 유실 가능
```

---

## 3. Redisson을 활용한 구현

### 의존성 추가

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.24.3</version>
</dependency>
```

### Redisson 설정

```yaml
# application.yml
spring:
  redis:
    host: localhost
    port: 6379
```

```java
@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
            .setAddress("redis://" + host + ":" + port)
            .setConnectionPoolSize(10)
            .setConnectionMinimumIdleSize(5);
        return Redisson.create(config);
    }
}
```

### RLock 기본 사용

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final RedissonClient redissonClient;
    private final StockRepository stockRepository;

    public void createOrder(Long productId, int quantity) {
        RLock lock = redissonClient.getLock("lock:stock:" + productId);

        try {
            // 락 획득 (최대 5초 대기, 락 10초 유지)
            boolean acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);

            if (!acquired) {
                throw new LockAcquisitionException("락 획득 실패");
            }

            // 비즈니스 로직
            Stock stock = stockRepository.findByProductId(productId);
            stock.decrease(quantity);
            stockRepository.save(stock);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            // 락을 가진 스레드만 해제 가능
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### AOP 기반 분산 락 구현

```java
// 어노테이션 정의
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    String key();                    // 락 키 (SpEL 지원)
    long waitTime() default 5000;    // 대기 시간 (ms)
    long leaseTime() default 10000;  // 락 유지 시간 (ms)
}

// AOP 구현
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint pjp, DistributedLock distributedLock)
            throws Throwable {

        String lockKey = resolveLockKey(pjp, distributedLock.key());
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(
                distributedLock.waitTime(),
                distributedLock.leaseTime(),
                TimeUnit.MILLISECONDS
            );

            if (!acquired) {
                throw new LockAcquisitionException("락 획득 실패: " + lockKey);
            }

            return pjp.proceed();

        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String resolveLockKey(ProceedingJoinPoint pjp, String keyExpression) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = pjp.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        return parser.parseExpression(keyExpression).getValue(context, String.class);
    }
}
```

### AOP 락 사용 예시

```java
@Service
public class StockService {

    @DistributedLock(key = "'lock:stock:' + #productId", waitTime = 5000, leaseTime = 10000)
    public void decrease(Long productId, int quantity) {
        Stock stock = stockRepository.findByProductId(productId);
        stock.decrease(quantity);
        stockRepository.save(stock);
    }
}
```

---

## 4. Redlock 알고리즘

### 필요성

```
단일 Redis 노드의 문제:
1. 마스터 장애 시 락 정보 유실
2. 마스터 → 슬레이브 복제 지연 중 장애 발생 시 중복 락

해결: 다수의 독립적인 Redis 인스턴스에 락 획득
```

### Redlock 동작 원리

```
5개의 독립된 Redis 인스턴스

[Redis 1] [Redis 2] [Redis 3] [Redis 4] [Redis 5]
    ↓         ↓         ↓         ↓         ↓
   락 O      락 O      락 O      락 X      락 O

과반수(3개) 이상 락 획득 성공 → 전체 락 획득 성공
락 획득에 걸린 시간이 락 유효시간보다 짧아야 함
```

### Redisson Redlock 구현

```java
@Configuration
public class RedlockConfig {

    @Bean
    public RedissonClient redissonClient1() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://redis1:6379");
        return Redisson.create(config);
    }

    @Bean
    public RedissonClient redissonClient2() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://redis2:6379");
        return Redisson.create(config);
    }

    @Bean
    public RedissonClient redissonClient3() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://redis3:6379");
        return Redisson.create(config);
    }
}

@Service
@RequiredArgsConstructor
public class RedlockService {

    private final RedissonClient redissonClient1;
    private final RedissonClient redissonClient2;
    private final RedissonClient redissonClient3;

    public void executeWithLock(String lockKey, Runnable task) {
        RLock lock1 = redissonClient1.getLock(lockKey);
        RLock lock2 = redissonClient2.getLock(lockKey);
        RLock lock3 = redissonClient3.getLock(lockKey);

        // RedissonRedLock 사용
        RedissonRedLock redLock = new RedissonRedLock(lock1, lock2, lock3);

        try {
            boolean acquired = redLock.tryLock(5, 10, TimeUnit.SECONDS);

            if (!acquired) {
                throw new LockAcquisitionException("Redlock 획득 실패");
            }

            task.run();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            redLock.unlock();
        }
    }
}
```

### Redlock 논쟁

```
장점:
- 단일 Redis 장애에 대한 내결함성
- 분산 환경에서 더 안전

단점:
- 복잡성 증가
- 네트워크 지연에 민감
- 시계 동기화 문제

대안:
- Zookeeper 기반 분산 락
- etcd 기반 분산 락
- 데이터베이스 기반 락 (SELECT FOR UPDATE)
```

---

## 5. 실무 활용 패턴

### 재고 감소 (동시성 제어)

```java
@Service
@RequiredArgsConstructor
public class StockService {

    private final RedissonClient redissonClient;
    private final StockRepository stockRepository;

    @Transactional
    public void decrease(Long productId, int quantity) {
        RLock lock = redissonClient.getLock("lock:stock:" + productId);

        try {
            if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                throw new BusinessException("재고 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }

            Stock stock = stockRepository.findByProductIdWithPessimisticLock(productId)
                .orElseThrow(() -> new NotFoundException("상품 없음"));

            if (stock.getQuantity() < quantity) {
                throw new BusinessException("재고 부족");
            }

            stock.decrease(quantity);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### 중복 요청 방지 (멱등성)

```java
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private final RedissonClient redissonClient;

    @Around("@annotation(idempotent)")
    public Object checkIdempotent(ProceedingJoinPoint pjp, Idempotent idempotent)
            throws Throwable {

        String idempotencyKey = extractIdempotencyKey(pjp);
        String lockKey = "idempotent:" + idempotencyKey;

        RLock lock = redissonClient.getLock(lockKey);
        RBucket<String> bucket = redissonClient.getBucket("result:" + idempotencyKey);

        try {
            if (!lock.tryLock(3, 30, TimeUnit.SECONDS)) {
                // 이미 처리 중 → 기존 결과 반환
                String cachedResult = bucket.get();
                if (cachedResult != null) {
                    return deserializeResult(cachedResult, pjp);
                }
                throw new DuplicateRequestException("요청 처리 중");
            }

            // 이미 처리된 요청인지 확인
            String cachedResult = bucket.get();
            if (cachedResult != null) {
                return deserializeResult(cachedResult, pjp);
            }

            // 새로운 요청 처리
            Object result = pjp.proceed();

            // 결과 캐싱 (24시간)
            bucket.set(serializeResult(result), Duration.ofHours(24));

            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### 스케줄러 중복 실행 방지

```java
@Service
@RequiredArgsConstructor
public class SchedulerLockService {

    private final RedissonClient redissonClient;

    @Scheduled(cron = "0 0 * * * *")  // 매시간
    public void processHourlyTask() {
        String lockKey = "scheduler:hourly-task";
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 5분 동안 락 유지 (작업 시간 고려)
            if (!lock.tryLock(0, 5, TimeUnit.MINUTES)) {
                log.info("다른 인스턴스에서 스케줄러 실행 중");
                return;
            }

            log.info("스케줄러 작업 시작");
            // 작업 수행

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

---

## 6. 주의사항 및 Best Practices

### 락 타임아웃 설정

```java
// 작업 시간을 충분히 고려
long leaseTime = estimatedTaskTime * 2;  // 여유 있게

// 하지만 너무 길면 장애 시 대기 시간 증가
// 적절한 균형 필요
```

### 락 갱신 (Watchdog)

```java
// Redisson은 leaseTime을 -1로 설정하면 자동 갱신
RLock lock = redissonClient.getLock("myLock");
lock.lock();  // leaseTime 미지정 → 30초마다 자동 갱신
try {
    // 작업
} finally {
    lock.unlock();
}
```

### 락 획득 실패 처리

```java
public void processWithLock(Long id) {
    RLock lock = redissonClient.getLock("lock:" + id);

    try {
        if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
            // 방법 1: 즉시 실패
            throw new LockAcquisitionException("잠시 후 다시 시도해주세요");

            // 방법 2: 큐에 넣고 나중에 처리
            // messageQueue.send(new RetryMessage(id));

            // 방법 3: 폴백 로직
            // fallbackProcess(id);
        }
        // ...
    } finally {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

### 트랜잭션과 락 순서

```java
// 권장: 락 먼저, 그 안에서 트랜잭션
public void createOrder() {
    RLock lock = getLock();
    try {
        lock.lock();
        orderTransaction();  // @Transactional 메서드 호출
    } finally {
        lock.unlock();
    }
}

// 주의: 트랜잭션 안에서 락 해제 시
// 트랜잭션 커밋 전에 다른 스레드가 락 획득 가능
@Transactional
public void badPattern() {
    lock.lock();
    // DB 작업
    lock.unlock();  // 트랜잭션 커밋 전!
    // 다른 스레드가 락 획득 → 커밋 안 된 데이터로 작업
}
```

---

## 체크리스트

```
□ 락 키 네이밍 규칙 정의 (lock:resource:id)
□ 적절한 대기 시간 설정
□ 적절한 락 유지 시간 설정
□ 락 해제 로직 (finally 블록)
□ 본인 락만 해제하는지 확인
□ 락 획득 실패 시 처리 로직
□ 모니터링 및 알림 설정
□ 단일 Redis 장애 대응 (필요시 Redlock)
```

---

*마지막 업데이트: 2026년 01월*

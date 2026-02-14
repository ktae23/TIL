# 캐시 스탬피드 방지

Thundering Herd 문제와 핫키 처리 등 캐시 관련 문제와 해결책을 정리합니다.

## 목차

1. [Cache Stampede란](#1-cache-stampede란)
2. [락 기반 해결](#2-락-기반-해결)
3. [확률적 조기 갱신](#3-확률적-조기-갱신)
4. [핫키 처리](#4-핫키-처리)
5. [캐시 워밍업](#5-캐시-워밍업)
6. [모니터링 및 알림](#6-모니터링-및-알림)

---

## 1. Cache Stampede란

### 문제 상황

```
Cache Stampede (Thundering Herd):
캐시 만료 시 동시에 많은 요청이 DB로 몰리는 현상

시간 T: 캐시 만료
┌──────────┐    ┌──────────┐    ┌──────────┐
│ Request1 │    │ Request2 │    │ Request3 │
│  Miss!   │    │  Miss!   │    │  Miss!   │
└────┬─────┘    └────┬─────┘    └────┬─────┘
     │               │               │
     └───────────────┼───────────────┘
                     ▼
              ┌──────────────┐
              │   Database   │  ← 동시에 N개 쿼리!
              │   (과부하)   │
              └──────────────┘
```

### 발생 시나리오

```
1. 인기 상품 캐시 만료
   - 동시 접속자 1000명이 같은 상품 조회
   - 1000개의 동일 쿼리가 DB에 전달

2. 캐시 서버 재시작
   - 모든 캐시 비어있음
   - Cold Start 문제

3. 일괄 캐시 만료
   - 같은 시간에 생성된 캐시가 동시 만료
```

---

## 2. 락 기반 해결

### 분산 락 (Mutex)

```java
@Service
public class ProductService {

    private final RedissonClient redisson;
    private final RedisTemplate<String, Product> cache;
    private final ProductRepository repository;

    public Product getProduct(Long id) {
        String cacheKey = "product:" + id;
        String lockKey = "lock:product:" + id;

        // 1. 캐시 조회
        Product cached = cache.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 2. 락 획득 시도
        RLock lock = redisson.getLock(lockKey);
        try {
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                // 락 획득 성공

                // Double-check: 다른 스레드가 이미 캐싱했는지
                cached = cache.opsForValue().get(cacheKey);
                if (cached != null) {
                    return cached;
                }

                // DB 조회 및 캐싱
                Product product = repository.findById(id);
                cache.opsForValue().set(cacheKey, product, Duration.ofHours(1));
                return product;
            } else {
                // 락 획득 실패 → 잠시 대기 후 캐시 재조회
                Thread.sleep(100);
                return cache.opsForValue().get(cacheKey);
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### SETNX 기반 간단 구현

```java
public Product getProductWithSetnx(Long id) {
    String cacheKey = "product:" + id;
    String lockKey = "lock:" + cacheKey;

    Product cached = cache.get(cacheKey);
    if (cached != null) {
        return cached;
    }

    // SETNX로 락 시도
    Boolean acquired = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, "1", Duration.ofSeconds(10));

    if (Boolean.TRUE.equals(acquired)) {
        try {
            // DB 조회 및 캐싱
            Product product = repository.findById(id);
            cache.set(cacheKey, product, Duration.ofHours(1));
            return product;
        } finally {
            redisTemplate.delete(lockKey);
        }
    } else {
        // 대기 후 재시도
        sleep(50);
        return cache.get(cacheKey);  // 다른 스레드가 캐싱했을 것
    }
}
```

---

## 3. 확률적 조기 갱신

### PER (Probabilistic Early Recomputation)

```
만료 전에 확률적으로 미리 갱신
만료 시점이 가까울수록 갱신 확률 증가

┌─────────────────────────────────────────────────┐
│ 캐시 생성        │        만료 임박  │ 만료    │
│    T=0          │          T=55분   │ T=60분  │
│                 │                    │         │
│  갱신 확률 0%   │     갱신 확률 30%  │  Miss   │
└─────────────────────────────────────────────────┘
```

### 구현

```java
public Product getProductWithPER(Long id) {
    String cacheKey = "product:" + id;

    // 캐시 조회 (TTL과 함께)
    ValueOperations<String, Product> ops = redisTemplate.opsForValue();
    Product cached = ops.get(cacheKey);

    if (cached == null) {
        return fetchAndCache(id);
    }

    // 남은 TTL 확인
    Long ttl = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
    long totalTtl = 3600;  // 1시간

    // 확률적 조기 갱신 결정
    if (shouldRefresh(ttl, totalTtl)) {
        // 비동기로 갱신 (현재 요청은 기존 캐시 사용)
        CompletableFuture.runAsync(() -> fetchAndCache(id));
    }

    return cached;
}

private boolean shouldRefresh(long remainingTtl, long totalTtl) {
    if (remainingTtl > totalTtl * 0.2) {
        return false;  // TTL 80% 이상 남음 → 갱신 안 함
    }

    // 만료에 가까울수록 갱신 확률 증가
    double probability = 1.0 - (double) remainingTtl / (totalTtl * 0.2);
    return Math.random() < probability;
}
```

### XFetch 알고리즘

```java
// 논문: Optimal Probabilistic Cache Stampede Prevention

public Product xfetch(Long id, double beta) {
    String key = "product:" + id;
    CachedValue cached = cache.getWithMetadata(key);

    if (cached == null) {
        return fetchAndCache(id);
    }

    // delta: 재계산 시간 (예상)
    double delta = cached.getComputeTime();

    // 현재 시간과 만료 시간
    double now = System.currentTimeMillis();
    double expiry = cached.getExpiryTime();

    // XFetch 공식
    // expiry - delta * beta * log(random) < now
    if (expiry - delta * beta * Math.log(Math.random()) < now) {
        return fetchAndCache(id);
    }

    return cached.getValue();
}
```

---

## 4. 핫키 처리

### 핫키란

```
핫키 (Hot Key):
특정 키에 요청이 집중되는 현상

예:
- 인기 상품 상세
- 실시간 검색어 1위
- 이벤트 상품

문제:
- 단일 Redis 노드에 부하 집중
- 네트워크 병목
```

### 해결책 1: 로컬 캐시 (L1 Cache)

```java
@Service
public class ProductService {

    // L1: 로컬 캐시 (Caffeine)
    private final Cache<Long, Product> localCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(Duration.ofSeconds(10))  // 짧은 TTL
        .build();

    // L2: Redis
    @Autowired
    private RedisTemplate<String, Product> redis;

    public Product getProduct(Long id) {
        // 1. L1 조회
        Product local = localCache.getIfPresent(id);
        if (local != null) {
            return local;
        }

        // 2. L2 조회
        Product remote = redis.opsForValue().get("product:" + id);
        if (remote != null) {
            localCache.put(id, remote);  // L1에 캐싱
            return remote;
        }

        // 3. DB 조회
        Product product = repository.findById(id);
        redis.opsForValue().set("product:" + id, product);
        localCache.put(id, product);
        return product;
    }
}
```

### 해결책 2: 키 분산 (Key Replication)

```java
public class HotKeyHandler {

    private static final int REPLICA_COUNT = 10;
    private final Random random = new Random();

    // 쓰기: 모든 복제본에 저장
    public void setHotKey(String key, Object value) {
        for (int i = 0; i < REPLICA_COUNT; i++) {
            redis.opsForValue().set(key + ":replica:" + i, value);
        }
    }

    // 읽기: 랜덤 복제본에서 조회
    public Object getHotKey(String key) {
        int replica = random.nextInt(REPLICA_COUNT);
        return redis.opsForValue().get(key + ":replica:" + replica);
    }
}
```

### 해결책 3: 읽기 복제본 활용

```java
// Redis Cluster에서 READONLY 명령으로 복제본에서 읽기
// Lettuce 설정
@Bean
public LettuceClientConfiguration lettuceConfig() {
    return LettuceClientConfiguration.builder()
        .readFrom(ReadFrom.REPLICA_PREFERRED)  // 복제본 우선 읽기
        .build();
}
```

---

## 5. 캐시 워밍업

### 애플리케이션 시작 시 워밍업

```java
@Component
public class CacheWarmer implements ApplicationRunner {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository repository;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Cache warming up started");

        // 인기 상품 미리 캐싱
        List<Product> popular = repository.findPopular(100);
        for (Product product : popular) {
            productService.cacheProduct(product);
        }

        log.info("Cache warming up completed: {} products", popular.size());
    }
}
```

### 점진적 워밍업

```java
@Scheduled(fixedRate = 60000)  // 1분마다
public void gradualWarmUp() {
    // TTL 만료 임박한 인기 캐시 갱신
    Set<String> keys = redisTemplate.keys("product:popular:*");

    for (String key : keys) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ttl != null && ttl < 300) {  // 5분 미만
            // 미리 갱신
            Long productId = extractProductId(key);
            Product product = repository.findById(productId);
            redisTemplate.opsForValue().set(key, product, Duration.ofHours(1));
        }
    }
}
```

---

## 6. 모니터링 및 알림

### 핵심 메트릭

```java
@Component
public class CacheMetrics {

    private final MeterRegistry registry;
    private final Counter stampedePrevented;
    private final Counter hotKeyAccess;

    public CacheMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.stampedePrevented = Counter.builder("cache.stampede.prevented")
            .description("Number of stampede prevented")
            .register(registry);
        this.hotKeyAccess = Counter.builder("cache.hotkey.access")
            .description("Hot key access count")
            .tag("key", "unknown")
            .register(registry);
    }

    public void recordStampedePrevented() {
        stampedePrevented.increment();
    }

    public void recordHotKeyAccess(String key) {
        Counter.builder("cache.hotkey.access")
            .tag("key", key)
            .register(registry)
            .increment();
    }
}
```

### 알림 설정

```yaml
# Prometheus alerting rules
groups:
- name: cache
  rules:
  - alert: CacheStampedeRisk
    expr: rate(cache_miss_total[1m]) > 1000
    for: 1m
    labels:
      severity: warning
    annotations:
      summary: "High cache miss rate detected"

  - alert: HotKeyDetected
    expr: topk(1, rate(cache_access_total[5m])) > 10000
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "Hot key detected"
```

---

## 핵심 정리

| 문제 | 해결책 |
|------|--------|
| Cache Stampede | 락, PER, Double-check |
| 핫키 | L1 캐시, 키 복제, 읽기 복제본 |
| Cold Start | 워밍업, 점진적 갱신 |

| 기법 | 장점 | 단점 |
|------|------|------|
| 분산 락 | 확실한 방지 | 지연 증가 |
| PER | 락 없음 | 일부 중복 가능 |
| L1 캐시 | 빠른 응답 | 일관성 주의 |

---

*마지막 업데이트: 2026년 01월*

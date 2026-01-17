# 캐시 무효화 전략

캐시 데이터의 일관성을 유지하기 위한 무효화 전략을 정리합니다.

## 목차

1. [TTL (Time-To-Live)](#1-ttl-time-to-live)
2. [이벤트 기반 무효화](#2-이벤트-기반-무효화)
3. [버전 기반 캐시 키](#3-버전-기반-캐시-키)
4. [Write-Invalidate vs Write-Update](#4-write-invalidate-vs-write-update)
5. [캐시 무효화 패턴](#5-캐시-무효화-패턴)
6. [분산 환경에서의 무효화](#6-분산-환경에서의-무효화)

---

## 1. TTL (Time-To-Live)

### 기본 개념

```
TTL: 캐시 데이터의 유효 기간
만료 후 자동 삭제되어 다음 조회 시 새 데이터 캐싱

장점:
- 구현 간단
- 자동 갱신
- 메모리 관리 용이

단점:
- 만료 전까지 오래된 데이터 반환 가능
- 정확한 TTL 설정 어려움
```

### 구현

```java
// Redis
redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(30));

// Spring Cache
@Cacheable(value = "products", key = "#id")
public Product getProduct(Long id) {
    return productRepository.findById(id);
}

// Caffeine Cache 설정
Caffeine.newBuilder()
    .expireAfterWrite(Duration.ofMinutes(30))
    .expireAfterAccess(Duration.ofMinutes(10))  // 마지막 접근 기준
    .build();
```

### TTL 설정 가이드

```
데이터 유형별 권장 TTL:

사용자 세션: 30분 ~ 24시간
상품 정보: 5분 ~ 1시간
재고 정보: 30초 ~ 1분 (변동 빈번)
정적 설정: 1시간 ~ 24시간
순위/통계: 5분 ~ 15분
```

---

## 2. 이벤트 기반 무효화

### 동작 방식

```
데이터 변경 → 이벤트 발행 → 캐시 무효화

┌──────────┐   Event    ┌──────────┐   Invalidate  ┌──────────┐
│ Service  │───────────→│ Message  │──────────────→│  Cache   │
│          │            │  Queue   │               │          │
└──────────┘            └──────────┘               └──────────┘
```

### Spring Events 활용

```java
// 이벤트 정의
public class ProductUpdatedEvent {
    private final Long productId;
    private final String action;

    public ProductUpdatedEvent(Long productId, String action) {
        this.productId = productId;
        this.action = action;
    }
}

// 이벤트 발행
@Service
public class ProductService {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public void updateProduct(Long id, ProductRequest request) {
        productRepository.update(id, request);
        eventPublisher.publishEvent(new ProductUpdatedEvent(id, "UPDATE"));
    }
}

// 이벤트 수신 및 캐시 무효화
@Component
public class CacheInvalidationHandler {

    @Autowired
    private CacheManager cacheManager;

    @EventListener
    @Async
    public void handleProductUpdate(ProductUpdatedEvent event) {
        Cache cache = cacheManager.getCache("products");
        if (cache != null) {
            cache.evict(event.getProductId());
        }
    }
}
```

### Kafka를 이용한 분산 환경 무효화

```java
// Producer: 변경 이벤트 발행
@Service
public class ProductService {

    @Autowired
    private KafkaTemplate<String, CacheInvalidationEvent> kafkaTemplate;

    public void updateProduct(Long id, ProductRequest request) {
        productRepository.update(id, request);

        kafkaTemplate.send("cache-invalidation",
            new CacheInvalidationEvent("product", id));
    }
}

// Consumer: 각 인스턴스에서 캐시 무효화
@Component
public class CacheInvalidationConsumer {

    @KafkaListener(topics = "cache-invalidation")
    public void handleInvalidation(CacheInvalidationEvent event) {
        String cacheKey = event.getType() + ":" + event.getId();
        redisTemplate.delete(cacheKey);
    }
}
```

---

## 3. 버전 기반 캐시 키

### 개념

```
캐시 키에 버전 정보 포함
데이터 변경 시 버전 증가 → 새 캐시 키 사용

기존: product:123
버전: product:123:v2
```

### 구현

```java
@Service
public class ProductService {

    private final AtomicLong version = new AtomicLong(0);

    // 또는 Redis에서 버전 관리
    private long getVersion() {
        return redisTemplate.opsForValue().get("product:version");
    }

    private void incrementVersion() {
        redisTemplate.opsForValue().increment("product:version");
    }

    public Product getProduct(Long id) {
        String key = String.format("product:%d:v%d", id, getVersion());

        Product cached = cache.get(key);
        if (cached != null) return cached;

        Product product = repository.findById(id);
        cache.set(key, product, Duration.ofHours(1));
        return product;
    }

    public void updateProduct(Long id, ProductRequest request) {
        repository.update(id, request);
        incrementVersion();  // 버전 증가 → 새 캐시 키 사용
    }
}
```

### 장단점

```
장점:
- 무효화 없이 새 데이터 사용
- 롤백 가능 (이전 버전 참조)

단점:
- 오래된 버전 데이터가 메모리에 남음
- TTL과 함께 사용 권장
```

---

## 4. Write-Invalidate vs Write-Update

### Write-Invalidate (캐시 삭제)

```java
public void updateProduct(Long id, ProductRequest request) {
    repository.update(id, request);
    cache.evict("product:" + id);  // 삭제
}

// 다음 조회 시 새 데이터 캐싱

장점:
- 구현 간단
- 불필요한 캐시 업데이트 방지

단점:
- 다음 조회 시 Cache Miss
```

### Write-Update (캐시 업데이트)

```java
public void updateProduct(Long id, ProductRequest request) {
    Product updated = repository.update(id, request);
    cache.set("product:" + id, updated);  // 즉시 업데이트
}

장점:
- 항상 Cache Hit 가능

단점:
- 사용 안 할 데이터도 업데이트
- 레이스 컨디션 위험
```

### 레이스 컨디션 문제

```
시간  스레드 A (업데이트)         스레드 B (업데이트)
T1    DB 업데이트: value=1
T2                               DB 업데이트: value=2
T3                               캐시 업데이트: value=2
T4    캐시 업데이트: value=1     ← 오래된 값으로 덮어씀!

결과: DB=2, Cache=1 (불일치!)
```

### 해결책

```java
// 1. 낙관적 락 사용
public void updateProduct(Long id, ProductRequest request) {
    Product updated = repository.updateWithVersion(id, request);
    cache.setIfVersionMatch("product:" + id, updated, updated.getVersion());
}

// 2. Write-Invalidate 사용 (권장)
// 대부분의 경우 삭제가 더 안전
```

---

## 5. 캐시 무효화 패턴

### 패턴 1: Delete-On-Write

```java
@Transactional
public void updateProduct(Long id, ProductRequest request) {
    productRepository.update(id, request);
    cacheManager.evict("product:" + id);
}
```

### 패턴 2: Scheduled Refresh

```java
@Scheduled(fixedRate = 60000)  // 1분마다
public void refreshPopularProducts() {
    List<Product> popular = repository.findPopular();
    for (Product p : popular) {
        cache.set("product:" + p.getId(), p, Duration.ofMinutes(5));
    }
}
```

### 패턴 3: Cache-Control Headers (HTTP)

```java
@GetMapping("/products/{id}")
public ResponseEntity<Product> getProduct(@PathVariable Long id) {
    Product product = productService.getProduct(id);

    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES))
        .eTag(product.getVersion().toString())
        .body(product);
}
```

### 패턴 4: Tag-Based Invalidation

```java
// 태그로 관련 캐시 그룹화
@Cacheable(value = "products", key = "#id", tags = {"category:" + #categoryId})
public Product getProduct(Long id, Long categoryId) {
    return repository.findById(id);
}

// 카테고리 변경 시 관련 캐시 모두 삭제
public void updateCategory(Long categoryId) {
    categoryRepository.update(categoryId);
    cacheManager.evictByTag("category:" + categoryId);
}
```

---

## 6. 분산 환경에서의 무효화

### 문제점

```
┌──────────┐    ┌──────────┐    ┌──────────┐
│ Instance │    │ Instance │    │ Instance │
│    A     │    │    B     │    │    C     │
├──────────┤    ├──────────┤    ├──────────┤
│ L1 Cache │    │ L1 Cache │    │ L1 Cache │
└────┬─────┘    └────┬─────┘    └────┬─────┘
     │               │               │
     └───────────────┼───────────────┘
                     │
              ┌──────┴──────┐
              │   Redis     │
              │ (L2 Cache)  │
              └─────────────┘

Instance A에서 캐시 무효화 시
→ Redis는 갱신
→ B, C의 L1 캐시는 그대로! (불일치)
```

### 해결 방법 1: L1 캐시 짧은 TTL

```java
// L1: 매우 짧은 TTL (10초)
Caffeine.newBuilder()
    .expireAfterWrite(Duration.ofSeconds(10))
    .build();

// L2: 긴 TTL (30분)
// Redis 기본 설정
```

### 해결 방법 2: Pub/Sub 무효화

```java
@Component
public class CacheInvalidationPubSub {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private Cache localCache;

    // 캐시 무효화 알림 발행
    public void invalidate(String key) {
        redisTemplate.delete(key);  // L2 삭제
        redisTemplate.convertAndSend("cache:invalidation", key);  // 알림
    }

    // 알림 수신 및 L1 삭제
    @PostConstruct
    public void subscribe() {
        redisTemplate.getConnectionFactory().getConnection()
            .subscribe((message, pattern) -> {
                String key = new String(message.getBody());
                localCache.evict(key);  // 로컬 캐시 삭제
            }, "cache:invalidation".getBytes());
    }
}
```

### 해결 방법 3: 버전 체크

```java
public Product getProduct(Long id) {
    // 1. L1에서 조회
    Product l1Cached = l1Cache.get(id);

    // 2. Redis에서 최신 버전 확인
    Long latestVersion = redisTemplate.opsForValue().get("product:version:" + id);

    // 3. 버전 비교
    if (l1Cached != null && l1Cached.getVersion().equals(latestVersion)) {
        return l1Cached;  // L1 사용
    }

    // 4. 버전 다르면 L2에서 조회
    Product l2Cached = l2Cache.get(id);
    l1Cache.set(id, l2Cached);  // L1 갱신
    return l2Cached;
}
```

---

## 핵심 정리

| 전략 | 장점 | 단점 | 사용 시점 |
|------|------|------|----------|
| TTL | 구현 간단 | 지연된 갱신 | 일반적인 경우 |
| 이벤트 기반 | 즉각 반영 | 복잡도 높음 | 실시간 필요 |
| 버전 기반 | 롤백 가능 | 메모리 낭비 | 히스토리 필요 |
| Write-Invalidate | 안전함 | Cache Miss | 권장 |
| Write-Update | 항상 Hit | 레이스 컨디션 | 단순한 경우 |

---

*마지막 업데이트: 2025년 01월*

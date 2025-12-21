# 캐시 전략 (Caching Strategies)

캐시를 효과적으로 사용하기 위한 다양한 읽기/쓰기 전략을 알아봅니다. 각 전략의 특징과 적합한 사용 사례를 이해하면 시스템에 맞는 최적의 캐싱 방식을 선택할 수 있습니다.

## 목차

- [읽기 전략](#읽기-전략)
  - [Cache-Aside (Lazy Loading)](#1-cache-aside-lazy-loading)
  - [Read-Through](#2-read-through)
- [쓰기 전략](#쓰기-전략)
  - [Write-Through](#3-write-through)
  - [Write-Behind (Write-Back)](#4-write-behind-write-back)
  - [Write-Around](#5-write-around)
- [전략 비교](#전략-비교)
- [캐시 무효화](#캐시-무효화)

---

## 읽기 전략

### 1. Cache-Aside (Lazy Loading)

**가장 널리 사용되는 전략.** 애플리케이션이 캐시와 DB를 직접 관리합니다.

```
[읽기 흐름]
1. 캐시에서 데이터 조회
2. 캐시 히트 → 데이터 반환
3. 캐시 미스 → DB에서 조회 → 캐시에 저장 → 데이터 반환
```

```java
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Product> redisTemplate;

    public Product findById(Long id) {
        String cacheKey = "product:" + id;

        // 1. 캐시 조회
        Product cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;  // 캐시 히트
        }

        // 2. 캐시 미스 → DB 조회
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));

        // 3. 캐시에 저장 (TTL 30분)
        redisTemplate.opsForValue().set(cacheKey, product, Duration.ofMinutes(30));

        return product;
    }

    // 데이터 변경 시 캐시 무효화
    public Product update(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));

        product.update(request);
        Product saved = productRepository.save(product);

        // 캐시 삭제 (다음 조회 시 새로 캐싱)
        redisTemplate.delete("product:" + id);

        return saved;
    }
}
```

**Spring @Cacheable 사용 시:**

```java
@Service
public class ProductService {

    @Cacheable(value = "products", key = "#id")
    public Product findById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @CacheEvict(value = "products", key = "#id")
    public Product update(Long id, ProductUpdateRequest request) {
        // 업데이트 로직...
    }
}
```

| 장점 | 단점 |
|------|------|
| 구현이 간단함 | 첫 요청 시 지연 발생 (Cache Miss) |
| 필요한 데이터만 캐싱 | 데이터 불일치 가능 (캐시 만료 전 DB 변경 시) |
| 캐시 장애 시에도 서비스 가능 | 애플리케이션에서 캐시 로직 관리 필요 |

---

### 2. Read-Through

캐시 라이브러리가 DB 로딩을 담당. 애플리케이션은 캐시만 바라봅니다.

```
[읽기 흐름]
1. 애플리케이션 → 캐시에 데이터 요청
2. 캐시 히트 → 데이터 반환
3. 캐시 미스 → 캐시가 직접 DB에서 로드 → 저장 → 반환
```

```java
// Caffeine + CacheLoader 예시
@Configuration
public class CacheConfig {

    @Bean
    public LoadingCache<Long, Product> productCache(ProductRepository repository) {
        return Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .build(id -> repository.findById(id).orElse(null));  // CacheLoader
    }
}

@Service
@RequiredArgsConstructor
public class ProductService {

    private final LoadingCache<Long, Product> productCache;

    public Product findById(Long id) {
        // 캐시가 알아서 로딩 처리
        return productCache.get(id);
    }
}
```

| 장점 | 단점 |
|------|------|
| 애플리케이션 코드가 단순 | 캐시 라이브러리 지원 필요 |
| 로딩 로직 중앙화 | 첫 요청 시 지연 발생 |
| 중복 DB 조회 방지 | 캐시 장애 시 서비스 불가 |

---

## 쓰기 전략

### 3. Write-Through

데이터를 캐시와 DB에 **동시에** 씁니다. 항상 일관성 보장.

```
[쓰기 흐름]
1. 애플리케이션 → 캐시에 쓰기 요청
2. 캐시 → DB에 저장 (동기)
3. 캐시 → 캐시에 저장
4. 성공 응답
```

```java
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Product> redisTemplate;

    @Transactional
    public Product create(ProductCreateRequest request) {
        Product product = Product.from(request);

        // 1. DB에 저장
        Product saved = productRepository.save(product);

        // 2. 캐시에도 저장 (동기)
        String cacheKey = "product:" + saved.getId();
        redisTemplate.opsForValue().set(cacheKey, saved, Duration.ofMinutes(30));

        return saved;
    }

    @Transactional
    public Product update(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));

        product.update(request);

        // 1. DB 업데이트
        Product saved = productRepository.save(product);

        // 2. 캐시 업데이트 (동기)
        String cacheKey = "product:" + id;
        redisTemplate.opsForValue().set(cacheKey, saved, Duration.ofMinutes(30));

        return saved;
    }
}
```

**@CachePut 사용:**

```java
@CachePut(value = "products", key = "#result.id")
public Product create(ProductCreateRequest request) {
    return productRepository.save(Product.from(request));
}

@CachePut(value = "products", key = "#id")
public Product update(Long id, ProductUpdateRequest request) {
    Product product = productRepository.findById(id).orElseThrow();
    product.update(request);
    return productRepository.save(product);
}
```

| 장점 | 단점 |
|------|------|
| 캐시-DB 항상 일관성 유지 | 쓰기 지연 증가 (두 곳에 저장) |
| 읽기 시 캐시 미스 적음 | 사용되지 않는 데이터도 캐싱 |

---

### 4. Write-Behind (Write-Back)

캐시에 먼저 쓰고, DB 저장은 **비동기로** 나중에 처리.

```
[쓰기 흐름]
1. 애플리케이션 → 캐시에 저장
2. 즉시 성공 응답
3. (비동기) 캐시 → 일정 시간/조건 후 DB에 일괄 저장
```

```java
@Service
@RequiredArgsConstructor
public class ViewCountService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ViewCountRepository viewCountRepository;

    // 조회수 증가 (캐시에만 저장 - 빠름)
    public void incrementViewCount(Long postId) {
        String key = "viewcount:" + postId;
        redisTemplate.opsForValue().increment(key);
    }

    // 주기적으로 DB에 동기화 (스케줄러)
    @Scheduled(fixedRate = 60000)  // 1분마다
    public void syncToDatabase() {
        Set<String> keys = redisTemplate.keys("viewcount:*");
        if (keys == null || keys.isEmpty()) return;

        List<ViewCount> updates = new ArrayList<>();

        for (String key : keys) {
            Long postId = Long.parseLong(key.replace("viewcount:", ""));
            String countStr = redisTemplate.opsForValue().getAndDelete(key);

            if (countStr != null) {
                updates.add(new ViewCount(postId, Long.parseLong(countStr)));
            }
        }

        // 일괄 업데이트
        viewCountRepository.batchUpdate(updates);
    }
}
```

**메시지 큐 활용:**

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final RedisTemplate<String, Order> redisTemplate;
    private final KafkaTemplate<String, Order> kafkaTemplate;

    public Order create(OrderCreateRequest request) {
        Order order = Order.from(request);
        String cacheKey = "order:" + order.getId();

        // 1. 캐시에 저장 (즉시)
        redisTemplate.opsForValue().set(cacheKey, order, Duration.ofHours(1));

        // 2. 메시지 큐로 DB 저장 요청 (비동기)
        kafkaTemplate.send("order-events", order);

        return order;
    }
}

// 컨슈머: DB 저장 처리
@KafkaListener(topics = "order-events")
public void saveOrder(Order order) {
    orderRepository.save(order);
}
```

| 장점 | 단점 |
|------|------|
| 쓰기 성능 매우 빠름 | 데이터 유실 위험 (캐시 장애 시) |
| DB 부하 감소 (일괄 처리) | 복잡한 구현 |
| 쓰기 집중 워크로드에 적합 | 일시적 데이터 불일치 |

---

### 5. Write-Around

DB에만 쓰고, 캐시는 읽기 시에 로드. 자주 읽히지 않는 데이터에 적합.

```
[쓰기 흐름]
1. 애플리케이션 → DB에만 저장
2. 캐시는 건드리지 않음 (또는 삭제)

[읽기 흐름]
Cache-Aside와 동일
```

```java
@Service
@RequiredArgsConstructor
public class LogService {

    private final LogRepository logRepository;
    private final RedisTemplate<String, Log> redisTemplate;

    // 로그 저장: DB에만 (자주 읽히지 않음)
    public Log save(Log log) {
        return logRepository.save(log);
        // 캐시에 저장하지 않음
    }

    // 조회 시에만 캐싱 (필요할 때)
    public Log findById(Long id) {
        String cacheKey = "log:" + id;

        Log cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Log log = logRepository.findById(id).orElseThrow();
        redisTemplate.opsForValue().set(cacheKey, log, Duration.ofMinutes(10));

        return log;
    }
}
```

| 장점 | 단점 |
|------|------|
| 불필요한 캐시 쓰기 방지 | 읽기 시 캐시 미스 발생 |
| 쓰기 성능 유지 | 자주 읽는 데이터에는 부적합 |
| 저장 공간 효율적 | |

---

## 전략 비교

| 전략 | 읽기 성능 | 쓰기 성능 | 일관성 | 복잡도 | 적합한 케이스 |
|------|----------|----------|--------|--------|--------------|
| **Cache-Aside** | 높음 (캐시 히트 시) | 보통 | 보통 | 낮음 | 범용, 읽기 많은 경우 |
| **Read-Through** | 높음 | 보통 | 보통 | 보통 | 읽기 패턴 단순화 |
| **Write-Through** | 매우 높음 | 낮음 | 높음 | 보통 | 일관성 중요한 경우 |
| **Write-Behind** | 높음 | 매우 높음 | 낮음 | 높음 | 쓰기 집중, 성능 중요 |
| **Write-Around** | 보통 | 높음 | 보통 | 낮음 | 쓰기 많고 읽기 적은 경우 |

### 조합 예시

```
읽기: Cache-Aside + 쓰기: Write-Through
→ 읽기 성능 좋고, 데이터 일관성도 보장

읽기: Read-Through + 쓰기: Write-Behind
→ 고성능 필요한 경우, 일시적 불일치 허용
```

---

## 캐시 무효화

### 1. TTL 기반 (Time-To-Live)

```java
// 30분 후 자동 만료
redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(30));
```

### 2. 이벤트 기반 무효화

```java
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Product update(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id).orElseThrow();
        product.update(request);
        Product saved = productRepository.save(product);

        // 이벤트 발행
        eventPublisher.publishEvent(new ProductUpdatedEvent(id));

        return saved;
    }
}

@Component
@RequiredArgsConstructor
public class CacheInvalidationHandler {

    private final RedisTemplate<String, Object> redisTemplate;

    @EventListener
    public void handleProductUpdate(ProductUpdatedEvent event) {
        redisTemplate.delete("product:" + event.getProductId());
        redisTemplate.delete("product:list");  // 관련 캐시도 삭제
    }
}
```

### 3. 패턴 기반 삭제

```java
// 특정 패턴의 모든 키 삭제
public void invalidateByPattern(String pattern) {
    Set<String> keys = redisTemplate.keys(pattern);
    if (keys != null && !keys.isEmpty()) {
        redisTemplate.delete(keys);
    }
}

// 사용 예: 카테고리 변경 시 관련 상품 캐시 모두 삭제
invalidateByPattern("product:category:123:*");
```

---

*마지막 업데이트: 2025년 12월*

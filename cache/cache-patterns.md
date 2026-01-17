# 캐시 패턴 정리

애플리케이션에서 사용되는 주요 캐싱 패턴을 정리합니다.

## 목차

1. [Cache-Aside (Lazy Loading)](#1-cache-aside-lazy-loading)
2. [Read-Through](#2-read-through)
3. [Write-Through](#3-write-through)
4. [Write-Behind (Write-Back)](#4-write-behind-write-back)
5. [패턴 비교](#5-패턴-비교)
6. [패턴 선택 가이드](#6-패턴-선택-가이드)

---

## 1. Cache-Aside (Lazy Loading)

### 동작 방식

```
읽기:
┌──────────┐         ┌──────────┐         ┌──────────┐
│   App    │──1.GET──│  Cache   │         │    DB    │
│          │←─2.Miss─│          │         │          │
│          │─────────3.SELECT───────────→│          │
│          │←────────4.Data──────────────│          │
│          │──5.SET──│          │         │          │
└──────────┘         └──────────┘         └──────────┘

쓰기:
App → DB 직접 쓰기 → 캐시 삭제 (또는 업데이트)
```

### 구현

```java
@Service
public class UserService {

    @Autowired
    private RedisTemplate<String, User> redisTemplate;

    @Autowired
    private UserRepository userRepository;

    public User getUser(Long id) {
        String key = "user:" + id;

        // 1. 캐시 조회
        User cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;  // Cache Hit
        }

        // 2. Cache Miss → DB 조회
        User user = userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found"));

        // 3. 캐시에 저장
        redisTemplate.opsForValue().set(key, user, Duration.ofHours(1));

        return user;
    }

    public void updateUser(Long id, UserUpdateRequest request) {
        // 1. DB 업데이트
        userRepository.update(id, request);

        // 2. 캐시 삭제 (다음 조회 시 새 데이터 캐싱)
        redisTemplate.delete("user:" + id);
    }
}
```

### 장단점

```
장점:
- 구현 간단
- 필요한 데이터만 캐싱 (메모리 효율)
- 캐시 장애 시에도 DB에서 조회 가능

단점:
- 첫 요청 느림 (Cache Miss)
- 캐시/DB 불일치 가능 (업데이트 시)
```

---

## 2. Read-Through

### 동작 방식

```
┌──────────┐         ┌──────────┐         ┌──────────┐
│   App    │──1.GET──│  Cache   │──Miss──→│    DB    │
│          │         │ (with    │←─Data───│          │
│          │←─2.Data─│  loader) │         │          │
└──────────┘         └──────────┘         └──────────┘

Cache가 직접 DB에서 데이터 로드
앱은 캐시만 바라봄
```

### 구현 (Spring Cache + Caffeine)

```java
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .recordStats());
        return manager;
    }
}

@Service
public class UserService {

    @Cacheable(value = "users", key = "#id")
    public User getUser(Long id) {
        // 캐시 미스 시에만 실행
        return userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @CacheEvict(value = "users", key = "#id")
    public void updateUser(Long id, UserUpdateRequest request) {
        userRepository.update(id, request);
    }
}
```

### 장단점

```
장점:
- 애플리케이션 코드 단순화
- 캐시 로직 중앙화

단점:
- 캐시 라이브러리/제품에 의존
- 커스터마이징 제한적
```

---

## 3. Write-Through

### 동작 방식

```
┌──────────┐         ┌──────────┐         ┌──────────┐
│   App    │──1.SET──│  Cache   │──2.SET──│    DB    │
│          │         │          │         │          │
│          │←──3.OK──│          │←──OK────│          │
└──────────┘         └──────────┘         └──────────┘

쓰기 요청 시 캐시와 DB 동시 업데이트
캐시 쓰기 완료 후 응답
```

### 구현

```java
@Service
public class ProductService {

    public void updateStock(Long productId, int quantity) {
        String key = "stock:" + productId;

        // 1. DB 업데이트
        stockRepository.update(productId, quantity);

        // 2. 캐시 업데이트 (동기)
        redisTemplate.opsForValue().set(key, quantity, Duration.ofHours(1));

        // 둘 다 성공해야 완료
    }

    // 트랜잭션 적용
    @Transactional
    public void updateStockWithTransaction(Long productId, int quantity) {
        stockRepository.update(productId, quantity);

        try {
            redisTemplate.opsForValue().set("stock:" + productId, quantity);
        } catch (Exception e) {
            // 캐시 실패 시 트랜잭션 롤백 또는 로깅
            throw new CacheUpdateException(e);
        }
    }
}
```

### 장단점

```
장점:
- 캐시/DB 일관성 보장
- 읽기 시 항상 Cache Hit 가능

단점:
- 쓰기 지연 (DB + 캐시)
- 사용하지 않는 데이터도 캐싱
```

---

## 4. Write-Behind (Write-Back)

### 동작 방식

```
┌──────────┐         ┌──────────┐         ┌──────────┐
│   App    │──1.SET──│  Cache   │         │    DB    │
│          │←──2.OK──│          │         │          │
│          │         │  (Queue) │──Later──│          │
└──────────┘         └──────────┘         └──────────┘

캐시에 먼저 쓰고 즉시 응답
DB에는 비동기로 나중에 쓰기
```

### 구현

```java
@Service
public class ViewCountService {

    private final RedisTemplate<String, Long> redisTemplate;
    private final ViewCountRepository repository;

    // 조회수 증가 (캐시만)
    public void incrementView(Long articleId) {
        String key = "view:" + articleId;
        redisTemplate.opsForValue().increment(key);
    }

    // 주기적으로 DB에 반영
    @Scheduled(fixedRate = 60000)  // 1분마다
    public void flushToDatabase() {
        Set<String> keys = redisTemplate.keys("view:*");

        for (String key : keys) {
            Long articleId = Long.parseLong(key.split(":")[1]);
            Long count = redisTemplate.opsForValue().get(key);

            if (count != null && count > 0) {
                // DB 업데이트
                repository.incrementView(articleId, count);

                // 캐시 카운트 리셋
                redisTemplate.opsForValue().decrement(key, count);
            }
        }
    }
}
```

### 장단점

```
장점:
- 쓰기 성능 우수 (즉시 응답)
- 대량 쓰기를 배치 처리 가능
- DB 부하 감소

단점:
- 데이터 유실 위험 (캐시 장애 시)
- 일시적 불일치 발생
- 구현 복잡도 높음
```

---

## 5. 패턴 비교

| 패턴 | 읽기 | 쓰기 | 일관성 | 성능 | 복잡도 |
|------|------|------|--------|------|--------|
| Cache-Aside | 앱이 관리 | 앱이 삭제 | 중간 | 좋음 | 낮음 |
| Read-Through | 캐시가 로드 | - | 중간 | 좋음 | 중간 |
| Write-Through | - | 캐시+DB 동시 | 높음 | 보통 | 중간 |
| Write-Behind | - | 캐시 먼저, DB 나중 | 낮음 | 최고 | 높음 |

---

## 6. 패턴 선택 가이드

### 사용 사례별 권장

```
읽기 빈번, 쓰기 적음:
→ Cache-Aside 또는 Read-Through

읽기/쓰기 비슷:
→ Write-Through

쓰기 빈번 (로그, 조회수 등):
→ Write-Behind

강한 일관성 필요:
→ Write-Through

고성능 필요:
→ Cache-Aside + Write-Behind 조합
```

### 조합 패턴

```java
// Cache-Aside + Write-Behind 조합
@Service
public class ProductService {

    // 읽기: Cache-Aside
    public Product getProduct(Long id) {
        Product cached = cache.get(id);
        if (cached != null) return cached;

        Product product = repository.findById(id);
        cache.set(id, product);
        return product;
    }

    // 쓰기: Write-Behind (조회수 등)
    public void incrementView(Long id) {
        viewCountCache.increment(id);  // 캐시만
    }

    // 쓰기: Write-Through (재고 등 중요 데이터)
    public void updateStock(Long id, int qty) {
        repository.updateStock(id, qty);
        cache.set("stock:" + id, qty);
    }
}
```

---

*마지막 업데이트: 2025년 01월*

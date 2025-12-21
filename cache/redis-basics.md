# Redis 기초

Redis(Remote Dictionary Server)는 인메모리 데이터 구조 저장소로, 캐시, 메시지 브로커, 세션 저장소 등 다양한 용도로 사용됩니다.

## 목차

- [설치 및 실행](#설치-및-실행)
- [기본 명령어](#기본-명령어)
- [데이터 타입](#데이터-타입)
- [TTL (Time To Live)](#ttl-time-to-live)
- [Spring Boot 연동](#spring-boot-연동)

---

## 설치 및 실행

### Docker로 설치 (권장)

```bash
# Redis 컨테이너 실행
docker run -d --name redis -p 6379:6379 redis:latest

# Redis CLI 접속
docker exec -it redis redis-cli
```

### macOS (Homebrew)

```bash
# 설치
brew install redis

# 서비스 시작
brew services start redis

# CLI 접속
redis-cli
```

### 연결 확인

```bash
redis-cli ping
# 응답: PONG
```

---

## 기본 명령어

### 키-값 조작

```bash
# 값 저장
SET user:1:name "홍길동"

# 값 조회
GET user:1:name
# 결과: "홍길동"

# 값 삭제
DEL user:1:name
# 결과: (integer) 1

# 키 존재 확인
EXISTS user:1:name
# 결과: (integer) 0 (없음) / 1 (있음)

# 여러 키-값 한번에 저장
MSET user:1:name "홍길동" user:1:age "25" user:1:city "서울"

# 여러 값 한번에 조회
MGET user:1:name user:1:age user:1:city
```

### 키 관리

```bash
# 패턴으로 키 검색 (프로덕션에서는 SCAN 사용 권장)
KEYS user:*

# 키 타입 확인
TYPE user:1:name
# 결과: string

# 모든 키 삭제 (주의!)
FLUSHDB  # 현재 DB만
FLUSHALL # 모든 DB
```

---

## 데이터 타입

### 1. String

가장 기본적인 타입. 최대 512MB까지 저장 가능.

```bash
# 기본 저장/조회
SET counter 100
GET counter

# 숫자 증가/감소
INCR counter      # 101
INCRBY counter 5  # 106
DECR counter      # 105
DECRBY counter 10 # 95

# 문자열 추가
APPEND greeting "Hello"
APPEND greeting " World"
GET greeting  # "Hello World"
```

### 2. List

순서가 있는 문자열 리스트. 양방향 삽입/삭제 가능.

```bash
# 왼쪽에 추가 (최신 항목이 앞으로)
LPUSH recent:products "상품A" "상품B" "상품C"

# 오른쪽에 추가
RPUSH queue:tasks "task1" "task2"

# 범위 조회 (0부터 시작, -1은 마지막)
LRANGE recent:products 0 -1
# 결과: "상품C", "상품B", "상품A"

# 인덱스로 조회
LINDEX recent:products 0  # "상품C"

# 길이 확인
LLEN recent:products  # 3

# 왼쪽/오른쪽에서 꺼내기 (큐/스택)
LPOP queue:tasks  # "task1" (큐)
RPOP queue:tasks  # "task2" (스택)
```

### 3. Set

중복 없는 문자열 집합. 순서 보장 안됨.

```bash
# 멤버 추가
SADD tags:post:1 "java" "spring" "redis"
SADD tags:post:2 "java" "docker" "kubernetes"

# 모든 멤버 조회
SMEMBERS tags:post:1

# 멤버 존재 확인
SISMEMBER tags:post:1 "java"  # 1 (있음)
SISMEMBER tags:post:1 "python"  # 0 (없음)

# 집합 연산
SINTER tags:post:1 tags:post:2  # 교집합: "java"
SUNION tags:post:1 tags:post:2  # 합집합
SDIFF tags:post:1 tags:post:2   # 차집합

# 멤버 수
SCARD tags:post:1  # 3
```

### 4. Hash

필드-값 쌍의 컬렉션. 객체 저장에 적합.

```bash
# 필드 설정
HSET user:100 name "김철수" age 30 city "서울"

# 필드 조회
HGET user:100 name  # "김철수"

# 전체 조회
HGETALL user:100
# name, 김철수, age, 30, city, 서울

# 여러 필드 조회
HMGET user:100 name age

# 필드 존재 확인
HEXISTS user:100 email  # 0

# 숫자 필드 증가
HINCRBY user:100 age 1  # 31

# 필드 삭제
HDEL user:100 city
```

### 5. Sorted Set (ZSet)

점수(score)로 정렬되는 Set. 랭킹 시스템에 적합.

```bash
# 멤버 추가 (점수, 멤버)
ZADD leaderboard 1500 "player1" 2000 "player2" 1800 "player3"

# 점수순 조회 (오름차순)
ZRANGE leaderboard 0 -1 WITHSCORES

# 점수순 조회 (내림차순)
ZREVRANGE leaderboard 0 2 WITHSCORES
# player2 (2000), player3 (1800), player1 (1500)

# 순위 조회 (0부터 시작)
ZRANK leaderboard "player1"    # 0 (오름차순)
ZREVRANK leaderboard "player2" # 0 (내림차순 1위)

# 점수 조회
ZSCORE leaderboard "player2"  # 2000

# 점수 증가
ZINCRBY leaderboard 300 "player1"  # 1800

# 점수 범위로 조회
ZRANGEBYSCORE leaderboard 1500 2000 WITHSCORES
```

---

## TTL (Time To Live)

키의 만료 시간을 설정하여 자동 삭제.

```bash
# 저장 시 만료 시간 설정 (초 단위)
SET session:abc123 "user_data" EX 3600  # 1시간

# 밀리초 단위
SET session:abc123 "user_data" PX 60000  # 1분

# 이미 있는 키에 만료 시간 설정
EXPIRE user:1:token 300  # 5분

# 남은 시간 확인 (초)
TTL user:1:token  # 남은 초, -1(만료없음), -2(키없음)

# 남은 시간 확인 (밀리초)
PTTL user:1:token

# 만료 시간 제거 (영구 보관)
PERSIST user:1:token
```

---

## Spring Boot 연동

### 의존성 추가

```gradle
// build.gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
}
```

### 설정

```yaml
# application.yml
spring:
  redis:
    host: localhost
    port: 6379
    # password: your-password  # 비밀번호 설정 시
```

### RedisTemplate 사용

```java
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // String 저장/조회
    public void saveString(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public String getString(String key) {
        return (String) redisTemplate.opsForValue().get(key);
    }

    // TTL과 함께 저장
    public void saveWithTTL(String key, String value, long seconds) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(seconds));
    }

    // Hash 저장/조회
    public void saveHash(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    public Object getHashField(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    // List 조작
    public void addToList(String key, String value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    public List<Object> getList(String key) {
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    // 키 삭제
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    // 키 존재 확인
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
```

### @Cacheable 사용 (Spring Cache)

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}

@Service
public class ProductService {

    @Cacheable(value = "products", key = "#id")
    public Product findById(Long id) {
        // DB 조회 로직 (캐시 미스 시에만 실행)
        return productRepository.findById(id).orElseThrow();
    }

    @CacheEvict(value = "products", key = "#product.id")
    public Product update(Product product) {
        return productRepository.save(product);
    }

    @CacheEvict(value = "products", allEntries = true)
    public void clearCache() {
        // 모든 products 캐시 삭제
    }
}
```

---

*마지막 업데이트: 2025년 12월*

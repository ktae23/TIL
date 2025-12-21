# Java Redis 클라이언트 라이브러리 비교: Redisson, Lettuce, Jedis

Java 애플리케이션에서 Redis를 사용할 때 선택할 수 있는 주요 클라이언트 라이브러리들의 특징과 차이점을 정리합니다.

## 목차

1. [개요](#개요)
2. [라이브러리별 특징](#라이브러리별-특징)
3. [비교 표](#비교-표)
4. [사용 예제](#사용-예제)
5. [선택 가이드](#선택-가이드)

---

## 개요

Redis는 인메모리 데이터 저장소로, Java 애플리케이션에서 캐시, 세션 저장소, 분산 락 등 다양한 용도로 사용됩니다. Java에서 Redis를 사용하기 위한 대표적인 클라이언트 라이브러리는 다음과 같습니다:

| 라이브러리 | 개발사 | 첫 릴리즈 | 라이선스 |
|-----------|--------|----------|---------|
| **Jedis** | Redis | 2010 | MIT |
| **Lettuce** | Lettuce.io | 2011 | Apache 2.0 |
| **Redisson** | Redisson | 2014 | Apache 2.0 |

---

## 라이브러리별 특징

### 1. Jedis

**가장 오래되고 단순한 Redis 클라이언트**

- **동기 방식**: 기본적으로 동기(blocking) 방식으로 동작
- **경량**: 의존성이 적고 라이브러리 크기가 작음
- **직관적 API**: Redis 명령어와 1:1 대응되는 직관적인 API
- **스레드 안전하지 않음**: 멀티스레드 환경에서 커넥션 풀(JedisPool) 필요

```java
// Jedis 기본 사용 예제
try (Jedis jedis = jedisPool.getResource()) {
    jedis.set("key", "value");
    String value = jedis.get("key");
}
```

### 2. Lettuce

**고성능 비동기/반응형 Redis 클라이언트**

- **비동기/반응형 지원**: 동기, 비동기(Future), 반응형(Reactive) 모두 지원
- **Netty 기반**: Netty를 사용한 non-blocking I/O
- **스레드 안전**: 단일 커넥션을 여러 스레드에서 공유 가능
- **Spring Data Redis 기본 클라이언트**: Spring Boot 2.0부터 기본 클라이언트

```java
// Lettuce 비동기 사용 예제
RedisAsyncCommands<String, String> async = connection.async();
RedisFuture<String> future = async.get("key");
future.thenAccept(value -> System.out.println(value));
```

### 3. Redisson

**분산 시스템을 위한 고수준 Redis 클라이언트**

- **분산 자료구조**: Map, Set, List, Queue 등 Java 컬렉션 인터페이스 구현
- **분산 락**: RLock, ReadWriteLock, Semaphore 등 동기화 도구 제공
- **분산 서비스**: ExecutorService, ScheduledExecutorService 지원
- **Spring 통합**: Spring Cache, Spring Session 완벽 지원

```java
// Redisson 분산 락 사용 예제
RLock lock = redisson.getLock("myLock");
try {
    lock.lock();
    // 임계 영역
} finally {
    lock.unlock();
}
```

---

## 비교 표

### 기능 비교

| 기능 | Jedis | Lettuce | Redisson |
|------|:-----:|:-------:|:--------:|
| 동기 API | O | O | O |
| 비동기 API | X | O | O |
| 반응형(Reactive) API | X | O | O |
| 클러스터 지원 | O | O | O |
| Sentinel 지원 | O | O | O |
| 파이프라이닝 | O | O | O |
| Pub/Sub | O | O | O |
| Lua 스크립트 | O | O | O |
| 분산 자료구조 | X | X | O |
| 분산 락 | X | X | O |
| 분산 서비스 | X | X | O |
| Spring Cache 통합 | X | O | O |
| Spring Session 통합 | X | O | O |

### 성능 비교

| 항목 | Jedis | Lettuce | Redisson |
|------|-------|---------|----------|
| 처리량 (ops/sec) | 중간 | 높음 | 중간~높음 |
| 지연 시간 | 낮음 | 매우 낮음 | 중간 |
| 메모리 사용량 | 낮음 | 중간 | 높음 |
| 커넥션 효율성 | 낮음 (풀 필요) | 높음 (공유 가능) | 높음 |

### 개발 편의성 비교

| 항목 | Jedis | Lettuce | Redisson |
|------|-------|---------|----------|
| 학습 곡선 | 낮음 | 중간 | 중간~높음 |
| API 추상화 수준 | 낮음 | 중간 | 높음 |
| 문서화 | 보통 | 좋음 | 매우 좋음 |
| 커뮤니티 | 큼 | 큼 | 중간 |

---

## 사용 예제

### Spring Boot 의존성 설정

```xml
<!-- Jedis -->
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>5.1.0</version>
</dependency>

<!-- Lettuce (Spring Data Redis에 포함) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Redisson -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.25.0</version>
</dependency>
```

### 분산 락 구현 비교

```java
// Jedis - 직접 구현 필요
public boolean acquireLock(Jedis jedis, String lockKey, String requestId, int expireTime) {
    String result = jedis.set(lockKey, requestId, SetParams.setParams().nx().ex(expireTime));
    return "OK".equals(result);
}

// Lettuce - 직접 구현 필요
public Mono<Boolean> acquireLock(RedisReactiveCommands<String, String> commands,
                                  String lockKey, String requestId, int expireTime) {
    return commands.set(lockKey, requestId, SetArgs.Builder.nx().ex(expireTime))
                   .map("OK"::equals);
}

// Redisson - 내장 지원
RLock lock = redisson.getLock("myLock");
boolean acquired = lock.tryLock(10, 30, TimeUnit.SECONDS);
```

---

## 선택 가이드

### Jedis를 선택해야 할 때

- 단순한 캐시 용도로만 사용
- 기존 레거시 시스템과의 호환성 필요
- 최소한의 의존성을 원할 때

### Lettuce를 선택해야 할 때

- Spring Boot 프로젝트에서 기본 Redis 클라이언트로 사용
- 비동기/반응형 프로그래밍 필요
- 높은 처리량이 필요한 경우
- 커넥션 수를 최소화하고 싶을 때

### Redisson을 선택해야 할 때

- 분산 락이 필요한 경우
- 분산 자료구조(Map, Set, Queue 등) 활용이 필요한 경우
- Java 컬렉션처럼 Redis를 사용하고 싶을 때
- 분산 스케줄러, 분산 서비스 등 고급 기능이 필요한 경우

### 의사결정 플로우

```
분산 락/자료구조 필요?
    └─ Yes → Redisson
    └─ No → 비동기/반응형 필요?
              └─ Yes → Lettuce
              └─ No → 단순 캐시만?
                        └─ Yes → Jedis 또는 Lettuce
                        └─ No → Lettuce
```

---

## 참고 자료

- [Jedis GitHub](https://github.com/redis/jedis)
- [Lettuce Reference Guide](https://lettuce.io/core/release/reference/)
- [Redisson Documentation](https://redisson.org/documentation.html)
- [Spring Data Redis Reference](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)

*마지막 업데이트: 2025년 12월*

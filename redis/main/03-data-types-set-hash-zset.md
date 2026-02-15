# 데이터 타입 (2): Set, Hash, Sorted Set

Redis의 Set, Hash, Sorted Set은 각각 고유한 내부 인코딩 전략을 통해 소규모 데이터에서는 메모리 효율적인 Listpack을, 대규모 데이터에서는 Hashtable이나 Skiplist를 사용한다. 이 문서에서는 세 자료구조의 내부 구현, 인코딩 전환 조건, 그리고 실전 활용 패턴을 분석한다.

## 목차

1. [핵심 개념 (What)](#1-핵심-개념-what)
2. [왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
3. [내부 구현 분석 (How)](#3-내부-구현-분석-how)
4. [실전 예제](#4-실전-예제)
5. [정리](#5-정리)

---

## 1. 핵심 개념 (What)

### 세 자료구조 개요

| 타입 | 설명 | 인코딩 | 핵심 특성 |
|------|------|--------|----------|
| **Set** | 중복 없는 문자열 집합 | listpack / hashtable | 교집합, 합집합, 차집합 연산 지원 |
| **Hash** | 필드-값 쌍의 컬렉션 | listpack / hashtable | 객체 모델링에 적합, 개별 필드 접근 가능 |
| **Sorted Set** | score로 정렬되는 유니크 집합 | listpack / skiplist+hashtable | 범위 쿼리와 순위 조회 O(log N) |

### 주요 명령어

| 타입 | 명령어 | 시간 복잡도 | 설명 |
|------|--------|------------|------|
| Set | `SADD key member` | O(1) | 멤버 추가 |
| Set | `SISMEMBER key member` | O(1) | 멤버 존재 확인 |
| Set | `SINTER key1 key2` | O(N*M) | 교집합 |
| Set | `SUNION key1 key2` | O(N) | 합집합 |
| Set | `SDIFF key1 key2` | O(N) | 차집합 |
| Hash | `HSET key field value` | O(1) | 필드 설정 |
| Hash | `HGET key field` | O(1) | 필드 조회 |
| Hash | `HGETALL key` | O(N) | 전체 조회 |
| Hash | `HINCRBY key field n` | O(1) | 정수 필드 증가 |
| Sorted Set | `ZADD key score member` | O(log N) | 멤버 추가 |
| Sorted Set | `ZRANGE key start stop` | O(log N + M) | 순위 범위 조회 |
| Sorted Set | `ZRANK key member` | O(log N) | 순위 조회 |
| Sorted Set | `ZRANGEBYSCORE key min max` | O(log N + M) | 점수 범위 조회 |

## 2. 왜 알아야 하는가 (Why)

### 실무에서 만나는 상황

1. **태그/카테고리 시스템 설계 시**: Set의 집합 연산(`SINTER`, `SUNION`)을 활용하면 "Java AND Spring" 같은 다중 태그 필터링을 DB 쿼리 없이 수행할 수 있다. 단, 대규모 Set 간의 `SINTER`는 O(N*M)이므로 성능 영향을 미리 파악해야 한다.

2. **사용자 프로필 캐싱 시**: Hash를 사용하면 사용자 객체의 개별 필드를 독립적으로 읽고 쓸 수 있어, JSON 전체를 직렬화/역직렬화하는 String 방식보다 효율적이다. 인코딩 전환 임계값을 이해해야 메모리 최적화가 가능하다.

3. **실시간 리더보드 구현 시**: Sorted Set은 skiplist 덕분에 `ZRANK`가 O(log N)으로 동작하여, 수백만 명 중 특정 사용자의 실시간 순위를 밀리초 내에 반환할 수 있다.

4. **인코딩 전환에 의한 메모리 급증 대응**: 128개 이하의 요소는 Listpack으로 저장되어 메모리 효율이 좋지만, 임계값을 초과하면 Hashtable로 전환되어 메모리 사용량이 크게 증가한다. 이 경계를 이해해야 용량 계획이 가능하다.

## 3. 내부 구현 분석 (How)

### 3.1 인코딩 전환 아키텍처

```mermaid
flowchart TD
    subgraph Set
        S1{"요소 수 <= set-max-listpack-entries<br/>AND 모든 요소 <= set-max-listpack-value"}
        S1 -->|"예"| S2["listpack 인코딩"]
        S1 -->|"아니오"| S3["hashtable 인코딩"]
    end

    subgraph Hash
        H1{"필드 수 <= hash-max-listpack-entries<br/>AND 모든 값 <= hash-max-listpack-value"}
        H1 -->|"예"| H2["listpack 인코딩"]
        H1 -->|"아니오"| H3["hashtable 인코딩"]
    end

    subgraph "Sorted Set"
        Z1{"요소 수 <= zset-max-listpack-entries<br/>AND 모든 값 <= zset-max-listpack-value"}
        Z1 -->|"예"| Z2["listpack 인코딩"]
        Z1 -->|"아니오"| Z3["skiplist + hashtable<br/>이중 인코딩"]
    end

    style S2 fill:#e8f5e9
    style H2 fill:#e8f5e9
    style Z2 fill:#e8f5e9
    style S3 fill:#fff3e0
    style H3 fill:#fff3e0
    style Z3 fill:#fff3e0
```

### 3.2 인코딩 전환 설정값

```conf
# redis.conf - 기본값 (Redis 7.x)

# Set 인코딩 전환
set-max-listpack-entries 128    # 요소 수 임계값
set-max-listpack-value 64      # 요소 크기 임계값 (바이트)

# Hash 인코딩 전환
hash-max-listpack-entries 128  # 필드 수 임계값
hash-max-listpack-value 64     # 값 크기 임계값 (바이트)

# Sorted Set 인코딩 전환
zset-max-listpack-entries 128  # 요소 수 임계값
zset-max-listpack-value 64     # 값 크기 임계값 (바이트)
```

**Listpack vs Hashtable 메모리 비교:**

| 요소 수 | Listpack | Hashtable | 절약률 |
|---------|----------|-----------|--------|
| 10 | ~200B | ~800B | 약 75% |
| 50 | ~900B | ~3.5KB | 약 74% |
| 100 | ~1.8KB | ~7KB | 약 74% |
| 128 초과 | (전환됨) | ~9KB+ | - |

### 3.3 Set 내부 구현

**Listpack 인코딩** (소규모): 연속된 메모리 블록에 요소를 순차 저장한다. 조회 시 선형 탐색(O(N))이지만, 128개 이하에서는 CPU 캐시 효율 덕분에 Hashtable보다 빠르다.

**Hashtable 인코딩** (대규모): Redis 내장 dict 구조를 사용한다. O(1) 조회를 보장하며, 점진적 리해싱(incremental rehashing)으로 해시 테이블 확장 시에도 블로킹이 발생하지 않는다.

```c
// t_set.c - SADD 구현 (핵심 로직)
void saddCommand(client *c) {
    robj *set = lookupKeyWrite(c->db, c->argv[1]);

    if (set == NULL) {
        // 새 Set 생성: 요소 수에 따라 인코딩 결정
        if (/* 조건 충족 */)
            set = createSetListpackObject();   // listpack
        else
            set = createSetObject();           // hashtable
        dbAdd(c->db, c->argv[1], set);
    }

    // 요소 추가 후 인코딩 전환 필요 여부 확인
    for (j = 2; j < c->argc; j++) {
        if (setTypeAdd(set, c->argv[j]->ptr)) {
            added++;
        }
    }
    // listpack -> hashtable 자동 전환
    setTypeMaybeConvert(set);
}
```

### 3.4 Hash 내부 구현

Hash의 Listpack 인코딩에서는 `[field1][value1][field2][value2]...` 형태로 필드-값 쌍이 연속 저장된다.

```
Listpack 인코딩의 Hash:
┌──────┬────────┬──────┬────────┬──────┬────────┐
│field1│ value1 │field2│ value2 │field3│ value3 │
└──────┴────────┴──────┴────────┴──────┴────────┘
  → 연속 메모리, 선형 탐색 O(N)

Hashtable 인코딩의 Hash:
┌─────────────────────────┐
│  dict (해시 테이블)       │
│  ├─ bucket[0] → entry   │
│  ├─ bucket[1] → entry   │
│  ├─ bucket[2] → NULL    │
│  └─ ...                 │
└─────────────────────────┘
  → 해시 기반, O(1) 조회
```

```bash
# 인코딩 확인 방법
127.0.0.1:6379> HSET user:1 name "Kim" age "30"
(integer) 2
127.0.0.1:6379> OBJECT ENCODING user:1
"listpack"

# 128개 필드 초과 시 전환
127.0.0.1:6379> OBJECT ENCODING user:1
"hashtable"
```

### 3.5 Sorted Set 내부 구현: Skiplist + Hashtable 이중 구조

Sorted Set은 대규모일 때 **두 가지 자료구조를 동시에** 유지한다.

- **Skiplist**: score 기반 범위 쿼리와 순위 조회 (O(log N))
- **Hashtable**: member에서 score로의 O(1) 직접 조회

```c
// server.h - Sorted Set의 이중 구조
typedef struct zset {
    dict *dict;       // member -> score 매핑 (O(1) 조회)
    zskiplist *zsl;   // score 기반 정렬 (범위 쿼리, 순위)
} zset;

// server.h - Skiplist 구조
typedef struct zskiplist {
    struct zskiplistNode *header, *tail;
    unsigned long length;     // 노드 수
    int level;                // 현재 최대 레벨
} zskiplist;

typedef struct zskiplistNode {
    sds ele;                  // member 값
    double score;             // 정렬 점수
    struct zskiplistNode *backward;  // 역방향 포인터
    struct zskiplistLevel {
        struct zskiplistNode *forward; // 전방 포인터
        unsigned long span;            // 스팬 (순위 계산용)
    } level[];                // 유연한 배열 (레벨별 포인터)
} zskiplistNode;
```

```
Skiplist 구조 (ZADD leaderboard 100 A 200 B 300 C 400 D):

Level 3:  HEAD ─────────────────────────────────→ D(400) → NULL
Level 2:  HEAD ───────────→ B(200) ──────────────→ D(400) → NULL
Level 1:  HEAD → A(100) → B(200) → C(300) → D(400) → NULL

  + dict: { "A":100, "B":200, "C":300, "D":400 }

  ZRANK "C"  → skiplist에서 O(log N)으로 순위 계산 (span 합산)
  ZSCORE "C" → dict에서 O(1)으로 score 직접 조회
```

**이중 구조를 사용하는 이유:**

| 연산 | Skiplist만 | Hashtable만 | 이중 구조 |
|------|-----------|-------------|----------|
| `ZADD` | O(log N) | O(1) | O(log N) |
| `ZSCORE` | O(log N) | **O(1)** | **O(1)** - dict 사용 |
| `ZRANK` | **O(log N)** | O(N) | **O(log N)** - skiplist 사용 |
| `ZRANGE` | **O(log N + M)** | O(N log N) | **O(log N + M)** - skiplist 사용 |

### 3.6 시간 복잡도 종합 분석

| 명령 | Set | Hash | Sorted Set |
|------|-----|------|------------|
| 추가 | `SADD` O(1) | `HSET` O(1) | `ZADD` O(log N) |
| 조회 | `SISMEMBER` O(1) | `HGET` O(1) | `ZSCORE` O(1) |
| 삭제 | `SREM` O(1) | `HDEL` O(1) | `ZREM` O(log N) |
| 전체 조회 | `SMEMBERS` O(N) | `HGETALL` O(N) | `ZRANGE 0 -1` O(N) |
| 개수 | `SCARD` O(1) | `HLEN` O(1) | `ZCARD` O(1) |
| 범위 조회 | - | - | `ZRANGEBYSCORE` O(log N + M) |
| 순위 | - | - | `ZRANK` O(log N) |

## 4. 실전 예제

### 4.1 태그 시스템 (Set)

```java
@Service
@RequiredArgsConstructor
public class TagService {

    private final StringRedisTemplate redisTemplate;

    /** 게시글에 태그를 추가한다. 양방향 인덱싱: post->tags, tag->posts */
    public void addTags(Long postId, Set<String> tags) {
        String postKey = "post:tags:" + postId;
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String tag : tags) {
                connection.setCommands().sAdd(postKey.getBytes(), tag.getBytes());
                connection.setCommands().sAdd(
                    ("tag:posts:" + tag).getBytes(),
                    String.valueOf(postId).getBytes());
            }
            return null;
        });
    }

    /** 여러 태그를 모두 가진 게시글 조회 (SINTER = 교집합) */
    public Set<String> findPostsByAllTags(String... tags) {
        List<String> keys = Arrays.stream(tags)
            .map(tag -> "tag:posts:" + tag).toList();
        return redisTemplate.opsForSet()
            .intersect(keys.get(0), keys.subList(1, keys.size()));
    }

    /** 하나 이상의 태그를 가진 게시글 조회 (SUNION = 합집합) */
    public Set<String> findPostsByAnyTag(String... tags) {
        List<String> keys = Arrays.stream(tags)
            .map(tag -> "tag:posts:" + tag).toList();
        return redisTemplate.opsForSet()
            .union(keys.get(0), keys.subList(1, keys.size()));
    }
}
```

### 4.2 사용자 프로필 캐시 (Hash)

```java
@Service
@RequiredArgsConstructor
public class UserProfileCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String PREFIX = "user:profile:";

    /** 사용자 프로필을 Hash로 캐싱한다. 개별 필드 단위 읽기/쓰기 가능. */
    public void cacheProfile(UserProfile profile) {
        String key = PREFIX + profile.getId();
        Map<String, Object> fields = Map.of(
            "name", profile.getName(), "email", profile.getEmail(),
            "age", String.valueOf(profile.getAge()),
            "loginCount", String.valueOf(profile.getLoginCount()));
        redisTemplate.opsForHash().putAll(key, fields);
        redisTemplate.expire(key, Duration.ofHours(2));
    }

    /** HINCRBY로 로그인 횟수를 원자적으로 증가. 전체 프로필 재기록 불필요. */
    public long incrementLoginCount(Long userId) {
        return redisTemplate.opsForHash()
            .increment(PREFIX + userId, "loginCount", 1);
    }

    /** HGETALL로 전체 프로필 조회. O(N)이지만 필드 수가 적으면 빠르다. */
    public Map<Object, Object> getFullProfile(Long userId) {
        return redisTemplate.opsForHash().entries(PREFIX + userId);
    }
}
```

### 4.3 실시간 리더보드 (Sorted Set)

```java
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final StringRedisTemplate redisTemplate;
    private static final String KEY = "leaderboard:game";

    /** ZADD O(log N)으로 점수 갱신 */
    public void updateScore(String playerId, double score) {
        redisTemplate.opsForZSet().add(KEY, playerId, score);
    }

    /** ZINCRBY로 원자적 점수 증가 */
    public double addScore(String playerId, double delta) {
        return redisTemplate.opsForZSet().incrementScore(KEY, playerId, delta);
    }

    /** ZREVRANGE로 상위 N명 조회 (내림차순). O(log N + M) */
    public List<RankEntry> getTopPlayers(int count) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
            redisTemplate.opsForZSet()
                .reverseRangeWithScores(KEY, 0, count - 1);
        if (tuples == null) return List.of();

        List<RankEntry> result = new ArrayList<>();
        int rank = 1;
        for (var tuple : tuples) {
            result.add(new RankEntry(rank++, tuple.getValue(), tuple.getScore()));
        }
        return result;
    }

    /** ZREVRANK + ZSCORE로 특정 플레이어의 순위와 점수 조회. O(log N) */
    public RankEntry getPlayerRank(String playerId) {
        Long rank = redisTemplate.opsForZSet().reverseRank(KEY, playerId);
        Double score = redisTemplate.opsForZSet().score(KEY, playerId);
        if (rank == null || score == null) return null;
        return new RankEntry(rank.intValue() + 1, playerId, score);
    }

    public record RankEntry(int rank, String playerId, double score) {}
}
```

## 5. 정리

| 항목 | Set | Hash | Sorted Set |
|-----|-----|------|------------|
| **내부 구조** | listpack / hashtable | listpack / hashtable | listpack / skiplist+hashtable |
| **전환 임계값** | 128개 / 64바이트 | 128개 / 64바이트 | 128개 / 64바이트 |
| **핵심 연산** | 집합 연산 (교/합/차) | 필드 단위 읽기/쓰기 | 범위 쿼리, 순위 조회 |
| **조회 복잡도** | O(1) | O(1) | O(log N) |
| **고유 특성** | 중복 자동 제거, 랜덤 추출 | 부분 갱신 가능, 메모리 효율 | score 기반 자동 정렬 |
| **주요 용도** | 태그, 좋아요, 고유 방문자 | 사용자 프로필, 설정 저장 | 리더보드, 타임라인, 우선순위 큐 |
| **메모리 주의사항** | 128개 초과 시 hashtable 전환으로 메모리 증가 | 128개 초과 시 hashtable 전환 | 128개 초과 시 skiplist+dict 이중 구조로 메모리 증가 |

---
*참고: Redis 7.x 기준*

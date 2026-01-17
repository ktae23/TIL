# Redis 면접 핵심 질문 정리

5년차 백엔드 개발자 면접에서 자주 등장하는 Redis 핵심 질문과 답변을 정리합니다.

## 목차

1. [싱글 스레드 성능](#1-싱글-스레드-성능)
2. [RDB vs AOF 영속성](#2-rdb-vs-aof-영속성)
3. [Redis Cluster 샤딩](#3-redis-cluster-샤딩)
4. [데이터 타입과 활용](#4-데이터-타입과-활용)
5. [캐시 전략](#5-캐시-전략)
6. [메모리 관리](#6-메모리-관리)

---

## 1. 싱글 스레드 성능

### Q: Redis가 싱글 스레드인데 어떻게 빠른 성능을 낼 수 있나요?

**싱글 스레드의 의미**
```
Redis 6.0 이전:
- 모든 클라이언트 명령 처리: 메인 스레드 1개
- 백그라운드 작업 (persistence, 삭제): 별도 스레드

Redis 6.0 이후:
- I/O 스레드 도입 (네트워크 I/O 분산)
- 명령 실행 자체는 여전히 싱글 스레드
```

**빠른 이유**

1. **In-Memory 저장**
```
┌───────────────────────────────────────────────────┐
│               응답 시간 비교                       │
├───────────────────────────────────────────────────┤
│  메모리 접근:   ~100 나노초                        │
│  SSD 접근:     ~100 마이크로초 (1,000배 느림)      │
│  HDD 접근:     ~10 밀리초 (100,000배 느림)         │
└───────────────────────────────────────────────────┘
```

2. **I/O Multiplexing (epoll/kqueue)**
```
전통적 방식 (Blocking I/O):
Thread 1 → Client 1 대기
Thread 2 → Client 2 대기
Thread 3 → Client 3 대기
→ 클라이언트마다 스레드 필요

Redis (Non-blocking I/O):
Single Thread → epoll → 수천 개 클라이언트 동시 처리
                   ├── Client 1 (준비되면 처리)
                   ├── Client 2 (준비되면 처리)
                   └── Client N (준비되면 처리)
```

3. **Lock 오버헤드 없음**
```java
// 멀티스레드 (Lock 필요)
synchronized(this) {
    value = map.get(key);  // 컨텍스트 스위칭, Lock 경합
}

// Redis 싱글스레드 (Lock 불필요)
GET key  // 다른 명령과 경합 없이 바로 실행
```

4. **효율적인 자료구조**
```
String: SDS (Simple Dynamic String) - 길이 캐싱, Binary Safe
List: Quick List (Linked List + Ziplist 혼합)
Hash: Ziplist (작을 때) / Hashtable (클 때)
Set: Intset (정수) / Hashtable
Sorted Set: Skiplist + Hashtable
```

### Q: 싱글 스레드의 단점과 주의사항은?

**문제가 되는 O(N) 명령어**
```bash
# 위험! 전체 키 스캔
KEYS *                  # O(N) - 사용 금지

# 안전한 대안: 커서 기반 스캔
SCAN 0 MATCH user:* COUNT 100  # O(1) per iteration

# 위험! 전체 요소 반환
SMEMBERS huge_set       # O(N) - 요소가 많으면 위험

# 안전한 대안
SSCAN huge_set 0 COUNT 100

# 위험! 긴 리스트 조회
LRANGE mylist 0 -1      # O(N)

# 안전한 대안: 범위 제한
LRANGE mylist 0 100
```

**Lua 스크립트 주의**
```lua
-- 긴 스크립트는 전체 Redis 블로킹
-- Bad: 복잡한 연산
EVAL "for i=1,1000000 do redis.call('GET','key') end" 0

-- Good: 간단한 원자적 연산
EVAL "local v = redis.call('GET',KEYS[1])
      return redis.call('SET',KEYS[1], v+1)" 1 counter
```

---

## 2. RDB vs AOF 영속성

### Q: RDB와 AOF의 차이점과 각각 언제 사용하나요?

**RDB (Redis Database)**
```
┌─────────────────────────────────────────────────────────┐
│  특정 시점의 메모리 스냅샷 저장 (dump.rdb)               │
├─────────────────────────────────────────────────────────┤
│  장점:                                                  │
│  - 단일 파일로 백업/복구 용이                           │
│  - 로딩 속도 빠름 (바이너리 포맷)                       │
│  - fork() 후 자식 프로세스가 저장 (성능 영향 적음)      │
├─────────────────────────────────────────────────────────┤
│  단점:                                                  │
│  - 스냅샷 간격 사이 데이터 유실 가능                    │
│  - fork() 시 메모리 사용량 일시적 증가                  │
└─────────────────────────────────────────────────────────┘
```

```conf
# redis.conf RDB 설정
save 900 1      # 900초(15분) 동안 1개 이상 변경 시
save 300 10     # 300초(5분) 동안 10개 이상 변경 시
save 60 10000   # 60초 동안 10000개 이상 변경 시
```

**AOF (Append Only File)**
```
┌─────────────────────────────────────────────────────────┐
│  모든 쓰기 명령을 순차적으로 기록                        │
├─────────────────────────────────────────────────────────┤
│  장점:                                                  │
│  - 데이터 유실 최소화 (fsync 정책에 따라)               │
│  - 사람이 읽을 수 있는 포맷                             │
│  - 명령 재실행으로 복구                                 │
├─────────────────────────────────────────────────────────┤
│  단점:                                                  │
│  - 파일 크기가 커짐 (rewrite 필요)                      │
│  - 복구 시간이 RDB보다 느림                             │
│  - 디스크 I/O 부하                                      │
└─────────────────────────────────────────────────────────┘
```

```conf
# redis.conf AOF 설정
appendonly yes
appendfsync everysec    # 매초 fsync (권장, 최대 1초 유실)
# appendfsync always    # 매 명령마다 fsync (느림, 유실 없음)
# appendfsync no        # OS에 맡김 (빠름, 유실 가능)
```

**비교 및 권장 사용법**

| 특성 | RDB | AOF |
|------|-----|-----|
| 복구 속도 | 빠름 | 느림 |
| 파일 크기 | 작음 | 큼 |
| 데이터 안정성 | 중간 | 높음 |
| 디스크 I/O | 적음 | 많음 |

```conf
# 권장: 둘 다 사용 (Redis 4.0+)
appendonly yes
appendfsync everysec

# RDB도 백업용으로 유지
save 900 1

# AOF+RDB 혼합 포맷 (Redis 4.0+)
aof-use-rdb-preamble yes
# AOF 파일 시작 부분에 RDB 스냅샷 포함
# → 빠른 로딩 + 낮은 데이터 유실
```

---

## 3. Redis Cluster 샤딩

### Q: Redis Cluster의 동작 방식과 해시 슬롯을 설명해주세요.

**해시 슬롯 (Hash Slot)**
```
전체 16384개 슬롯을 노드에 분배

┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│   Node A    │   │   Node B    │   │   Node C    │
│ Slot 0-5460 │   │ Slot 5461-  │   │ Slot 10923- │
│             │   │    10922    │   │    16383    │
└─────────────┘   └─────────────┘   └─────────────┘

키 → 슬롯 매핑:
CRC16(key) mod 16384 = slot number

예시:
SET user:1 "Kim"
→ CRC16("user:1") mod 16384 = 5462
→ Node B로 라우팅
```

**해시 태그 (Hash Tag)**
```bash
# 같은 슬롯에 저장하려면 {} 사용
SET {user:1}:profile "data1"
SET {user:1}:orders "data2"
# 둘 다 CRC16("user:1")로 계산 → 같은 슬롯

# MGET 같은 다중 키 연산 가능
MGET {user:1}:profile {user:1}:orders
```

**클러스터 구성**
```
┌─────────────────────────────────────────────────────────┐
│                    Redis Cluster                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐          │
│  │ Master A │    │ Master B │    │ Master C │          │
│  │ 0-5460   │    │5461-10922│    │10923-    │          │
│  └────┬─────┘    └────┬─────┘    └────┬─────┘          │
│       │               │               │                 │
│  ┌────▼─────┐    ┌────▼─────┐    ┌────▼─────┐          │
│  │ Replica  │    │ Replica  │    │ Replica  │          │
│  │   A1     │    │   B1     │    │   C1     │          │
│  └──────────┘    └──────────┘    └──────────┘          │
│                                                         │
│  - 최소 3개 마스터 권장                                 │
│  - 각 마스터당 1개 이상 레플리카                        │
│  - Gossip 프로토콜로 상태 공유                          │
└─────────────────────────────────────────────────────────┘
```

**장애 조치 (Failover)**
```
1. 마스터 장애 감지 (Gossip)
2. 레플리카가 마스터로 승격
3. 슬롯 재할당
4. 클라이언트 리다이렉션

# MOVED 응답
GET user:1
→ MOVED 5462 192.168.1.2:6379
→ 클라이언트가 새 노드로 재요청
```

### Q: Sentinel과 Cluster의 차이점은?

| 특성 | Sentinel | Cluster |
|------|----------|---------|
| 목적 | 고가용성 (HA) | 샤딩 + HA |
| 구조 | Master-Replica | Multi-Master |
| 용량 확장 | X (단일 마스터) | O (샤딩) |
| 복잡도 | 낮음 | 높음 |
| 사용 시점 | 데이터 < 수십 GB | 데이터 > 수십 GB |

---

## 4. 데이터 타입과 활용

### Q: Redis의 주요 데이터 타입과 사용 사례를 설명해주세요.

**String**
```bash
# 기본 캐시
SET user:1:name "Kim" EX 3600

# 카운터 (원자적 연산)
INCR page:views
INCRBY user:1:points 10

# 분산 락
SET lock:resource "owner" NX EX 30
```

**Hash**
```bash
# 객체 저장 (필드별 접근)
HSET user:1 name "Kim" email "kim@test.com" age 30
HGET user:1 name
HINCRBY user:1 age 1

# 장점: 전체 객체를 한 번에 읽거나 필드별 수정 가능
```

**List**
```bash
# 큐 (FIFO)
LPUSH queue:jobs "job1"
RPOP queue:jobs

# 스택 (LIFO)
LPUSH stack "item"
LPOP stack

# 최근 N개 유지
LPUSH recent:views "page1"
LTRIM recent:views 0 99  # 최근 100개만 유지
```

**Set**
```bash
# 태그, 좋아요 사용자
SADD post:1:likes "user:1" "user:2"
SISMEMBER post:1:likes "user:1"  # O(1)
SCARD post:1:likes  # 좋아요 수

# 교집합/합집합
SINTER user:1:followers user:2:followers  # 공통 팔로워
```

**Sorted Set (ZSet)**
```bash
# 랭킹보드
ZADD leaderboard 1000 "user:1" 950 "user:2" 900 "user:3"
ZREVRANK leaderboard "user:1"  # 순위 (0부터)
ZREVRANGE leaderboard 0 9 WITHSCORES  # 상위 10명

# 시간 기반 데이터 (타임라인)
ZADD user:1:timeline 1704067200 "post:1"  # Unix timestamp
ZRANGEBYSCORE user:1:timeline 1704000000 1704100000  # 범위 조회
```

**HyperLogLog (근사 카운팅)**
```bash
# 고유 방문자 수 (메모리 효율적, ~0.81% 오차)
PFADD visitors:today "user:1" "user:2" "user:3"
PFCOUNT visitors:today  # 약 3

# 1억 개 요소도 12KB만 사용
```

---

## 5. 캐시 전략

### Q: Cache Aside (Lazy Loading) 패턴을 설명해주세요.

**동작 방식**
```java
public User getUser(Long id) {
    String key = "user:" + id;

    // 1. 캐시 조회
    User cached = redisTemplate.opsForValue().get(key);
    if (cached != null) {
        return cached;  // Cache Hit
    }

    // 2. Cache Miss → DB 조회
    User user = userRepository.findById(id).orElseThrow();

    // 3. 캐시에 저장
    redisTemplate.opsForValue().set(key, user, Duration.ofHours(1));

    return user;
}

public void updateUser(Long id, UserUpdateRequest request) {
    // 1. DB 업데이트
    userRepository.update(id, request);

    // 2. 캐시 삭제 (invalidation)
    redisTemplate.delete("user:" + id);
    // 다음 조회 시 새로운 값이 캐시됨
}
```

**패턴 비교**

| 패턴 | 읽기 | 쓰기 | 장점 | 단점 |
|------|------|------|------|------|
| Cache Aside | 앱이 캐시/DB 관리 | 앱이 DB 쓰기 → 캐시 삭제 | 간단, 캐시 장애에 강함 | 첫 요청 느림 |
| Read Through | 캐시가 DB 조회 | - | 코드 단순화 | 캐시 라이브러리 필요 |
| Write Through | - | 캐시 → DB 동기 쓰기 | 일관성 높음 | 쓰기 지연 |
| Write Behind | - | 캐시 → DB 비동기 쓰기 | 쓰기 빠름 | 데이터 유실 가능 |

---

## 6. 메모리 관리

### Q: Redis 메모리 부족 시 어떻게 처리하나요?

**Eviction 정책**
```conf
# redis.conf
maxmemory 2gb
maxmemory-policy allkeys-lru  # 가장 많이 사용
```

| 정책 | 설명 |
|------|------|
| noeviction | 메모리 부족 시 에러 (기본값) |
| allkeys-lru | 모든 키 중 LRU 제거 |
| volatile-lru | TTL 설정된 키 중 LRU 제거 |
| allkeys-lfu | 모든 키 중 LFU 제거 (4.0+) |
| volatile-lfu | TTL 설정된 키 중 LFU 제거 |
| allkeys-random | 무작위 제거 |
| volatile-random | TTL 설정된 키 중 무작위 제거 |
| volatile-ttl | TTL 짧은 키 먼저 제거 |

**메모리 최적화**
```bash
# 메모리 사용량 확인
INFO memory
# used_memory: 실제 사용량
# used_memory_rss: OS가 할당한 메모리 (fragmentation 포함)

# 큰 키 찾기
redis-cli --bigkeys

# 메모리 분석
MEMORY USAGE key_name
MEMORY DOCTOR
```

```java
// 코드 레벨 최적화
// 1. 적절한 데이터 타입 선택
// Hash가 String보다 메모리 효율적 (작은 객체일 때)

// 2. 키 이름 축약
"user:profile:1" → "u:p:1"

// 3. 불필요한 데이터 정리
// TTL 설정 필수!
redisTemplate.opsForValue().set(key, value, Duration.ofHours(24));
```

---

## 핵심 정리

| 주제 | 핵심 키워드 |
|------|-------------|
| 싱글스레드 | In-Memory, epoll, Lock-free, O(N) 명령 주의 |
| 영속성 | RDB(스냅샷), AOF(로그), 혼합 사용 권장 |
| Cluster | 16384 슬롯, 해시 태그, Gossip, MOVED |
| 데이터 타입 | String/Hash/List/Set/ZSet/HyperLogLog |
| 캐시 전략 | Cache Aside, TTL, Invalidation |
| 메모리 | maxmemory, Eviction 정책, LRU/LFU |

---

*마지막 업데이트: 2025년 01월*

# 특수 데이터 타입: Bitmap, HyperLogLog, Stream

Redis는 기본 자료구조 외에도 특수 목적에 최적화된 데이터 타입을 제공한다. Bitmap은 비트 단위 연산으로 메모리를 극도로 절약하고, HyperLogLog는 12KB 고정 메모리로 수억 개의 고유 원소 수를 추정하며, Stream은 Kafka와 유사한 append-only 로그 구조로 Consumer Group 기반의 메시지 처리를 지원한다.

## 목차

1. [핵심 개념 (What)](#1-핵심-개념-what)
2. [왜 알아야 하는가 (Why)](#2-왜-알아야-하는가-why)
3. [내부 구현 분석 (How)](#3-내부-구현-분석-how)
4. [실전 예제](#4-실전-예제)
5. [정리](#5-정리)

---

## 1. 핵심 개념 (What)

### 특수 데이터 타입 개요

| 타입 | 설명 | 메모리 특성 | 정확도 |
|------|------|-----------|--------|
| **Bitmap** | String 위에 구현된 비트 단위 연산 | 1억 비트 = 약 12MB | 정확 (100%) |
| **HyperLogLog** | 카디널리티(고유 원소 수) 추정 | 고정 12KB | 근사값 (오차율 0.81%) |
| **Stream** | append-only 로그 + Consumer Group | 가변 (Radix Tree + Listpack) | 정확 (100%) |
| **Geospatial** | 좌표 기반 위치 데이터 | Sorted Set 기반 | Geohash 정밀도 |

### 주요 명령어

| 타입 | 명령어 | 시간 복잡도 | 설명 |
|------|--------|------------|------|
| Bitmap | `SETBIT key offset value` | O(1) | 특정 비트 설정 |
| Bitmap | `GETBIT key offset` | O(1) | 특정 비트 조회 |
| Bitmap | `BITCOUNT key [start end]` | O(N) | 1인 비트 개수 |
| Bitmap | `BITOP op dest key1 key2` | O(N) | 비트 논리 연산 |
| HyperLogLog | `PFADD key element` | O(1) | 원소 추가 |
| HyperLogLog | `PFCOUNT key` | O(1) | 고유 원소 수 추정 |
| HyperLogLog | `PFMERGE dest key1 key2` | O(N) | HLL 병합 |
| Stream | `XADD key * field value` | O(1) | 메시지 추가 |
| Stream | `XREAD COUNT n STREAMS key id` | O(N) | 메시지 읽기 |
| Stream | `XREADGROUP GROUP g c STREAMS key >` | O(1) | Consumer Group 읽기 |
| Stream | `XACK key group id` | O(1) | 메시지 처리 확인 |
| Geospatial | `GEOADD key lng lat member` | O(log N) | 위치 추가 |
| Geospatial | `GEOSEARCH key ... BYRADIUS` | O(N+log M) | 반경 내 검색 |

## 2. 왜 알아야 하는가 (Why)

### 실무에서 만나는 상황

1. **일일 활성 사용자(DAU) 추적**: 수천만 사용자의 출석 체크를 DB에 기록하면 테이블이 급격히 커진다. Bitmap을 사용하면 1억 명의 출석을 단 12MB로 추적할 수 있고, `BITCOUNT`로 즉시 DAU를 계산할 수 있다.

2. **고유 방문자(UV) 카운팅**: Set으로 UV를 추적하면 방문자 ID마다 수십 바이트씩 메모리를 차지한다. HyperLogLog를 사용하면 고정 12KB로 수억 명의 UV를 0.81% 오차 내로 추정할 수 있다.

3. **이벤트 소싱/로그 시스템**: 주문, 결제 등의 이벤트를 순서대로 기록하고 여러 Consumer가 독립적으로 처리해야 할 때, Stream의 Consumer Group이 Kafka 없이도 이를 지원한다.

4. **주변 매장 검색**: 사용자의 현재 위치에서 반경 5km 내의 매장을 검색하는 기능을 Geospatial 명령으로 간단하게 구현할 수 있다.

## 3. 내부 구현 분석 (How)

### 3.1 Bitmap: String 위의 비트 연산

Bitmap은 별도의 데이터 타입이 아니라 **String 타입 위에 비트 단위 연산을 수행하는 인터페이스**다. 내부적으로는 SDS(Simple Dynamic String)에 바이트 배열로 저장된다.

```
SETBIT attendance:2025-01-15 42 1

  → String 키의 바이트 5 (42 / 8 = 5), 비트 2 (42 % 8 = 2)에 1을 설정

  바이트 인덱스:  [0]  [1]  [2]  [3]  [4]  [5]  ...
  비트:          00000000 00000000 00000000 00000000 00000000 00100000
                                                            ↑
                                                         offset=42
```

```c
// bitops.c - SETBIT 구현 (핵심)
void setbitCommand(client *c) {
    robj *o;
    size_t bitoffset;
    ssize_t byte, bit;

    bitoffset = (size_t)getLongLongFromObject(c->argv[2]);

    // 바이트 위치와 비트 위치 계산
    byte = bitoffset >> 3;       // bitoffset / 8
    bit = 7 - (bitoffset & 0x7); // 비트 위치 (MSB first)

    // 필요시 String 확장 (자동으로 0으로 채움)
    o = lookupStringForBitCommand(c, bitoffset);

    // 비트 설정
    byteval = ((uint8_t*)o->ptr)[byte];
    bitval = byteval & (1 << bit);
    byteval &= ~(1 << bit);
    byteval |= ((on & 0x1) << bit);
    ((uint8_t*)o->ptr)[byte] = byteval;
}
```

**Bitmap 메모리 효율:**

| 사용자 수 | Set (ID 저장) | Bitmap | 절약률 |
|----------|---------------|--------|--------|
| 100만 | ~40MB | 125KB | 99.7% |
| 1000만 | ~400MB | 1.25MB | 99.7% |
| 1억 | ~4GB | 12.5MB | 99.7% |

### 3.2 HyperLogLog: 확률적 카디널리티 추정

HyperLogLog(HLL)는 고유 원소의 수(카디널리티)를 확률적으로 추정하는 알고리즘이다. Redis에서는 **12KB 고정 메모리**로 2^64개의 고유 원소 수를 0.81% 표준 오차로 추정한다.

```mermaid
flowchart LR
    A["PFADD key elem"] --> B["Hash(elem)<br/>64비트 해시값"]
    B --> C["앞 14비트<br/>→ 레지스터 인덱스<br/>(16384개)"]
    B --> D["나머지 50비트<br/>→ 선행 0의 개수 + 1<br/>(run length)"]
    C --> E["registers[index]<br/>= max(현재값, run)"]
    D --> E

    F["PFCOUNT key"] --> G["16384개 레지스터의<br/>조화평균 계산"]
    G --> H["추정 카디널리티<br/>(오차 ±0.81%)"]

    style E fill:#e8f5e9
    style H fill:#e1f5fe
```

**HLL 내부 구조:**

```
Redis HLL 구조 (12KB):
┌────────────────────────────────────────────────────────┐
│ HLL Header (16바이트)                                    │
│  - magic: "HYLL"                                        │
│  - encoding: DENSE(0) 또는 SPARSE(1)                     │
│  - cardinality cache                                     │
├────────────────────────────────────────────────────────┤
│ 16384개 레지스터 (각 6비트 = 12288바이트 ≈ 12KB)          │
│  [reg0: 5][reg1: 3][reg2: 0][reg3: 7]...[reg16383: 2]  │
└────────────────────────────────────────────────────────┘

SPARSE 인코딩: 대부분의 레지스터가 0일 때 RLE 압축으로 메모리 절약
DENSE 인코딩:  레지스터 값이 많아지면 16384*6bit = 12KB 고정
```

```bash
# HyperLogLog 사용 예시
127.0.0.1:6379> PFADD visitors:2025-01-15 "user1" "user2" "user3"
(integer) 1

127.0.0.1:6379> PFADD visitors:2025-01-15 "user1" "user4"  # user1 중복
(integer) 1

127.0.0.1:6379> PFCOUNT visitors:2025-01-15
(integer) 4  # 고유 원소 4개

# 여러 날의 UV 병합
127.0.0.1:6379> PFMERGE visitors:week visitors:mon visitors:tue visitors:wed
OK
127.0.0.1:6379> PFCOUNT visitors:week
(integer) 1530  # 주간 UV 추정값

# 메모리 사용량 확인
127.0.0.1:6379> MEMORY USAGE visitors:2025-01-15
(integer) 176   # SPARSE: 매우 작음
# 원소가 많아지면 최대 12KB (DENSE)
```

### 3.3 Stream: Append-only 로그와 Consumer Group

Stream은 Redis 5.0에서 도입된 Kafka-like 메시지 스트림이다. 내부적으로 Radix Tree + Listpack 구조를 사용하여 메시지 ID 기반의 효율적인 저장과 범위 조회를 지원한다.

```
Stream 내부 구조:

  Radix Tree (메시지 ID 인덱스)
  ├── 1674000000000-0 ──→ Listpack [field1,val1,field2,val2]
  ├── 1674000000001-0 ──→ Listpack [field1,val1,field2,val2]
  ├── 1674000000002-0 ──→ Listpack [field1,val1,field2,val2]
  └── ...

  메시지 ID 형식: <밀리초 타임스탬프>-<시퀀스 번호>
```

**Consumer Group 개념:**

```mermaid
flowchart TD
    P["Producer<br/>XADD mystream * ..."] --> S["Stream: mystream<br/>(append-only 로그)"]

    S --> CG1["Consumer Group: order-service"]
    S --> CG2["Consumer Group: notification-service"]

    CG1 --> C1["Consumer A<br/>XREADGROUP ... >"]
    CG1 --> C2["Consumer B<br/>XREADGROUP ... >"]

    CG2 --> C3["Consumer C<br/>XREADGROUP ... >"]
    CG2 --> C4["Consumer D<br/>XREADGROUP ... >"]

    C1 -->|"XACK"| CG1
    C2 -->|"XACK"| CG1
    C3 -->|"XACK"| CG2

    style S fill:#e1f5fe
    style CG1 fill:#e8f5e9
    style CG2 fill:#fff3e0
```

**핵심 개념:**
- **Consumer Group**: 같은 Stream을 구독하는 Consumer의 논리적 그룹. 각 메시지는 그룹 내 하나의 Consumer에게만 전달된다.
- **PEL (Pending Entries List)**: 전달되었지만 아직 ACK되지 않은 메시지 목록. 장애 복구에 사용된다.
- **XACK**: Consumer가 메시지 처리 완료를 확인하는 명령. ACK 후 PEL에서 제거된다.

```bash
# Stream 기본 사용법
# 메시지 추가 (* = 자동 ID 생성)
127.0.0.1:6379> XADD orders * product "laptop" quantity "1" price "1200"
"1674000000000-0"

127.0.0.1:6379> XADD orders * product "mouse" quantity "2" price "25"
"1674000000001-0"

# 메시지 읽기 (처음부터)
127.0.0.1:6379> XRANGE orders - +
1) 1) "1674000000000-0"
   2) 1) "product" 2) "laptop" 3) "quantity" 4) "1" 5) "price" 6) "1200"
2) 1) "1674000000001-0"
   2) 1) "product" 2) "mouse" 3) "quantity" 4) "2" 5) "price" 6) "25"

# Consumer Group 생성 (0 = 처음부터, $ = 새 메시지부터)
127.0.0.1:6379> XGROUP CREATE orders order-processors 0

# Consumer Group으로 읽기 (> = 아직 전달되지 않은 메시지)
127.0.0.1:6379> XREADGROUP GROUP order-processors consumer-1 COUNT 1 STREAMS orders >
1) 1) "orders"
   2) 1) 1) "1674000000000-0"
         2) 1) "product" 2) "laptop" ...

# 처리 완료 확인
127.0.0.1:6379> XACK orders order-processors 1674000000000-0
(integer) 1

# 미처리 메시지 확인 (PEL)
127.0.0.1:6379> XPENDING orders order-processors - + 10
```

### 3.4 Geospatial: 위치 기반 데이터

Geospatial은 내부적으로 **Sorted Set** 위에 구현된다. 좌표를 Geohash로 변환하여 score로 저장하므로, Sorted Set의 모든 장점(O(log N) 삽입/조회)을 그대로 활용한다.

```bash
# 위치 추가
127.0.0.1:6379> GEOADD stores 126.9780 37.5665 "강남점"
127.0.0.1:6379> GEOADD stores 126.9770 37.5726 "종로점"
127.0.0.1:6379> GEOADD stores 127.0276 37.4979 "잠실점"

# 두 지점 사이의 거리
127.0.0.1:6379> GEODIST stores "강남점" "종로점" km
"0.6812"

# 반경 내 검색 (Redis 6.2+)
127.0.0.1:6379> GEOSEARCH stores FROMLONLAT 126.978 37.566 BYRADIUS 5 km ASC
1) "강남점"
2) "종로점"
3) "잠실점"

# 좌표 조회
127.0.0.1:6379> GEOPOS stores "강남점"
1) 1) "126.97800070047378540"
   2) "37.56649888644025041"
```

## 4. 실전 예제

### 4.1 출석 체크 시스템 (Bitmap)

```java
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final StringRedisTemplate redisTemplate;

    private static final String ATTENDANCE_PREFIX = "attendance:";

    /**
     * 출석 체크를 기록한다.
     * 사용자 ID를 비트 오프셋으로 사용하여 O(1) 시간에 기록.
     * 1억 명의 출석을 12.5MB로 관리할 수 있다.
     */
    public void checkIn(LocalDate date, long userId) {
        String key = ATTENDANCE_PREFIX + date;
        redisTemplate.opsForValue().setBit(key, userId, true);
    }

    /**
     * 특정 날짜의 출석 여부를 확인한다. O(1).
     */
    public boolean isCheckedIn(LocalDate date, long userId) {
        String key = ATTENDANCE_PREFIX + date;
        Boolean result = redisTemplate.opsForValue().getBit(key, userId);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 특정 날짜의 총 출석자 수를 조회한다.
     * BITCOUNT로 1인 비트의 개수를 센다.
     */
    public long getDailyAttendanceCount(LocalDate date) {
        String key = ATTENDANCE_PREFIX + date;
        // Jedis/Lettuce를 통한 BITCOUNT 실행
        return redisTemplate.execute((RedisCallback<Long>) connection ->
            connection.stringCommands().bitCount(key.getBytes())
        );
    }

    /**
     * 연속 출석 일수를 계산한다.
     * 각 날짜의 Bitmap을 AND 연산하여 연속 출석자를 구한다.
     */
    public long getConsecutiveAttendance(
            LocalDate startDate, int days, long userId) {

        int count = 0;
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.minusDays(i);
            if (isCheckedIn(date, userId)) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    /**
     * 두 날짜 모두 출석한 사용자의 수를 계산한다.
     * BITOP AND로 교집합을 구한 후 BITCOUNT로 개수를 센다.
     */
    public long getCommonAttendanceCount(
            LocalDate date1, LocalDate date2) {

        String key1 = ATTENDANCE_PREFIX + date1;
        String key2 = ATTENDANCE_PREFIX + date2;
        String destKey = "attendance:temp:and";

        redisTemplate.execute((RedisCallback<Long>) connection -> {
            connection.stringCommands().bitOp(
                RedisStringCommands.BitOperation.AND,
                destKey.getBytes(),
                key1.getBytes(), key2.getBytes());
            return null;
        });

        Long count = redisTemplate.execute(
            (RedisCallback<Long>) connection ->
                connection.stringCommands().bitCount(destKey.getBytes())
        );

        redisTemplate.delete(destKey);
        return count != null ? count : 0L;
    }
}
```

### 4.2 UV(Unique Visitor) 카운팅 (HyperLogLog)

```java
@Service
@RequiredArgsConstructor
public class UniqueVisitorService {

    private final StringRedisTemplate redisTemplate;

    private static final String UV_PREFIX = "uv:";

    /**
     * 페이지 방문을 기록한다.
     * PFADD는 O(1)이며, 중복 방문은 자동으로 무시된다.
     * 12KB 고정 메모리로 수억 명의 UV를 추적할 수 있다.
     */
    public void recordVisit(String page, String visitorId) {
        String dailyKey = UV_PREFIX + "daily:" + LocalDate.now()
            + ":" + page;
        String monthlyKey = UV_PREFIX + "monthly:"
            + YearMonth.now() + ":" + page;

        // 일별, 월별 동시 기록 (파이프라인)
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            connection.hyperLogLogCommands().pfAdd(
                dailyKey.getBytes(), visitorId.getBytes());
            connection.hyperLogLogCommands().pfAdd(
                monthlyKey.getBytes(), visitorId.getBytes());

            // 일별 키는 2일 후 만료
            connection.keyCommands().expire(
                dailyKey.getBytes(), 2 * 24 * 3600L);
            return null;
        });
    }

    /**
     * 특정 페이지의 일별 UV를 조회한다.
     * PFCOUNT는 O(1)로 즉시 추정값을 반환한다.
     * 오차율: 표준 오차 0.81%
     */
    public long getDailyUV(LocalDate date, String page) {
        String key = UV_PREFIX + "daily:" + date + ":" + page;
        Long count = redisTemplate.opsForHyperLogLog().size(key);
        return count != null ? count : 0L;
    }

    /**
     * 여러 페이지의 합산 UV를 조회한다.
     * PFMERGE로 여러 HLL을 병합한 후 PFCOUNT로 카운트.
     * 중복 방문자는 자동으로 제거된다.
     */
    public long getTotalUV(LocalDate date, List<String> pages) {
        String destKey = UV_PREFIX + "temp:merge:" + UUID.randomUUID();

        String[] sourceKeys = pages.stream()
            .map(page -> UV_PREFIX + "daily:" + date + ":" + page)
            .toArray(String[]::new);

        redisTemplate.opsForHyperLogLog()
            .union(destKey, sourceKeys);

        Long count = redisTemplate.opsForHyperLogLog().size(destKey);
        redisTemplate.delete(destKey);

        return count != null ? count : 0L;
    }

    /**
     * 주간 UV 트렌드를 조회한다.
     */
    public Map<LocalDate, Long> getWeeklyTrend(String page) {
        Map<LocalDate, Long> trend = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            trend.put(date, getDailyUV(date, page));
        }
        return trend;
    }
}
```

### 4.3 이벤트 로그 시스템 (Stream + Consumer Group)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class EventStreamService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String STREAM_KEY = "events:orders";
    private static final String GROUP_NAME = "order-processors";

    /**
     * 이벤트를 Stream에 발행한다 (Producer).
     * XADD는 O(1)로 메시지를 추가하며, 자동으로 고유 ID를 생성한다.
     * MAXLEN ~으로 Stream 크기를 제한하여 메모리를 관리한다.
     */
    public String publishEvent(OrderEvent event) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("type", event.type());
        fields.put("orderId", event.orderId());
        fields.put("amount", String.valueOf(event.amount()));
        fields.put("timestamp", Instant.now().toString());

        try {
            // MAXLEN ~10000: 약 10000개로 제한 (~ = 성능 최적화)
            StringRecord record = StreamRecords.string(fields)
                .withStreamKey(STREAM_KEY);

            RecordId recordId = redisTemplate.opsForStream()
                .add(record);

            // Stream 크기 제한 (트리밍)
            redisTemplate.opsForStream()
                .trim(STREAM_KEY, 10000, true);

            return recordId != null ? recordId.getValue() : null;
        } catch (Exception e) {
            log.error("이벤트 발행 실패: {}", event, e);
            throw new RuntimeException("이벤트 발행 실패", e);
        }
    }

    /**
     * Consumer Group을 생성한다.
     * 0 = Stream 처음부터, $ = 새 메시지부터
     */
    public void createConsumerGroup() {
        try {
            redisTemplate.opsForStream()
                .createGroup(STREAM_KEY, GROUP_NAME);
        } catch (Exception e) {
            // 이미 존재하는 경우 무시
            log.info("Consumer Group이 이미 존재합니다: {}", GROUP_NAME);
        }
    }

    /**
     * Consumer Group으로 메시지를 소비한다 (Consumer).
     * XREADGROUP으로 아직 전달되지 않은 메시지를 가져오고,
     * 처리 완료 후 XACK로 확인한다.
     */
    public List<OrderEvent> consumeEvents(String consumerId, int count) {
        List<MapRecord<String, Object, Object>> records =
            redisTemplate.opsForStream().read(
                Consumer.from(GROUP_NAME, consumerId),
                StreamReadOptions.empty().count(count),
                StreamOffset.create(STREAM_KEY,
                    ReadOffset.lastConsumed())
            );

        if (records == null || records.isEmpty()) {
            return List.of();
        }

        List<OrderEvent> events = new ArrayList<>();
        List<RecordId> processedIds = new ArrayList<>();

        for (MapRecord<String, Object, Object> record : records) {
            try {
                Map<Object, Object> body = record.getValue();
                OrderEvent event = new OrderEvent(
                    (String) body.get("type"),
                    (String) body.get("orderId"),
                    Double.parseDouble((String) body.get("amount")),
                    Instant.parse((String) body.get("timestamp"))
                );
                events.add(event);
                processedIds.add(record.getId());
            } catch (Exception e) {
                log.error("메시지 파싱 실패: {}", record.getId(), e);
            }
        }

        // 처리 완료된 메시지 ACK
        if (!processedIds.isEmpty()) {
            redisTemplate.opsForStream().acknowledge(
                STREAM_KEY, GROUP_NAME,
                processedIds.toArray(new RecordId[0])
            );
        }

        return events;
    }

    /**
     * 미처리(Pending) 메시지를 조회한다.
     * 장애 복구 시 처리되지 않은 메시지를 재처리하는 데 사용한다.
     */
    public long getPendingCount() {
        PendingMessagesSummary summary = redisTemplate.opsForStream()
            .pending(STREAM_KEY, GROUP_NAME);
        return summary != null ? summary.getTotalPendingMessages() : 0L;
    }

    public record OrderEvent(
        String type,
        String orderId,
        double amount,
        Instant timestamp
    ) {}
}
```

## 5. 정리

| 항목 | Bitmap | HyperLogLog | Stream | Geospatial |
|-----|--------|-------------|--------|------------|
| **기반 구조** | String (SDS) | 자체 구현 (16384 레지스터) | Radix Tree + Listpack | Sorted Set (Geohash) |
| **메모리 특성** | N비트 = N/8 바이트 | 고정 12KB (DENSE) | 가변 (메시지 수 비례) | Sorted Set과 동일 |
| **정확도** | 100% (정확) | 0.81% 표준 오차 | 100% (정확) | Geohash 정밀도 |
| **핵심 연산** | SETBIT/GETBIT O(1), BITCOUNT O(N) | PFADD/PFCOUNT O(1) | XADD O(1), XREADGROUP O(1) | GEOADD O(log N), GEOSEARCH O(N+log M) |
| **주요 용도** | 출석 체크, 기능 플래그, 불룸 필터 | UV 카운팅, 카디널리티 추정 | 이벤트 소싱, 메시지 큐 | 위치 기반 검색, 거리 계산 |
| **장점** | 극한의 메모리 효율 | 고정 메모리로 대규모 처리 | Consumer Group으로 분산 처리 | Sorted Set 재활용 |
| **주의사항** | 희소 데이터 시 낭비 (offset이 클 때) | 근사값이므로 정확한 카운트 불가 | 메시지 증가 시 트리밍 필요 | 2D 평면 근사 (지구 곡률 오차) |

---
*참고: Redis 7.x 기준*

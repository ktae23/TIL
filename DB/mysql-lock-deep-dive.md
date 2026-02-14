# Lock 완전 정리

동시성 제어를 위한 Lock의 모든 것 — MySQL 내부 Lock, Named Lock, 비관적/낙관적 Lock, 분산 Lock까지 체계적으로 정리한다.

## 목차

1. [Lock 개요와 분류 체계](#1-lock-개요와-분류-체계)
2. [MySQL Internal Lock](#2-mysql-internal-lock)
   - [Shared Lock vs Exclusive Lock](#shared-lock-vs-exclusive-lock)
   - [Intention Lock](#intention-lock)
   - [Record Lock / Gap Lock / Next-Key Lock](#record-lock--gap-lock--next-key-lock)
   - [Insert Intention Lock](#insert-intention-lock)
   - [Auto-Increment Lock](#auto-increment-lock)
3. [Named Lock (GET_LOCK)](#3-named-lock-get_lock)
4. [비관적 Lock (Pessimistic Lock)](#4-비관적-lock-pessimistic-lock)
5. [낙관적 Lock (Optimistic Lock)](#5-낙관적-lock-optimistic-lock)
6. [비관적 vs 낙관적 비교](#6-비관적-vs-낙관적-비교)
7. [분산 Lock](#7-분산-lock)
8. [전체 Lock 비교표](#8-전체-lock-비교표)
9. [실무 시나리오별 Lock 선택 가이드](#9-실무-시나리오별-lock-선택-가이드)
10. [데드락과 트러블슈팅](#10-데드락과-트러블슈팅)

---

## 1. Lock 개요와 분류 체계

Lock은 여러 트랜잭션(또는 프로세스)이 동시에 같은 자원에 접근할 때 **데이터 일관성**을 보장하기 위한 메커니즘이다.

### Lock 분류 체계

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Lock 분류 체계                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ① MySQL Internal Lock (DB 엔진 레벨)                               │
│  ├── Row Lock: Record / Gap / Next-Key / Insert Intention           │
│  ├── Table Lock: Table Lock / Intention Lock / Auto-Inc Lock        │
│  └── Global Lock: FLUSH TABLES WITH READ LOCK                      │
│                                                                      │
│  ② Named Lock (사용자 정의 레벨)                                    │
│  └── GET_LOCK(): 임의의 문자열을 키로 하는 사용자 정의 Lock          │
│                                                                      │
│  ③ Application Lock (애플리케이션 레벨)                              │
│  ├── 비관적 Lock: SELECT ... FOR UPDATE                             │
│  └── 낙관적 Lock: version 컬럼 기반 CAS                             │
│                                                                      │
│  ④ Distributed Lock (분산 시스템 레벨)                               │
│  ├── Redis: Redisson, SETNX                                        │
│  ├── Zookeeper: Curator                                             │
│  └── DB: Named Lock, ShedLock                                      │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Lock 레벨별 특성

| Lock 레벨 | 범위 | 동시성 | 오버헤드 | 데드락 위험 |
|-----------|------|--------|---------|------------|
| **Global Lock** | DB 전체 | 매우 낮음 | 최소 | 없음 |
| **Table Lock** | 테이블 단위 | 낮음 | 작음 | 낮음 |
| **Row Lock** | 행 단위 | 높음 | 큼 | 있음 |
| **Named Lock** | 논리적 단위 | 유연 | 작음 | 있음 |

---

## 2. MySQL Internal Lock

### Shared Lock vs Exclusive Lock

| 구분 | Shared Lock (S Lock) | Exclusive Lock (X Lock) |
|------|---------------------|------------------------|
| **용도** | 읽기 작업 | 쓰기 작업 |
| **다른 S Lock과 호환** | O (호환) | X (비호환) |
| **다른 X Lock과 호환** | X (비호환) | X (비호환) |
| **발생 시점** | `SELECT ... FOR SHARE` | `SELECT ... FOR UPDATE`, `UPDATE`, `DELETE` |
| **특징** | 여러 트랜잭션이 동시에 읽기 가능 | 하나의 트랜잭션만 쓰기 가능 |

```sql
-- Shared Lock (S Lock): 읽기 잠금
SELECT * FROM users WHERE id = 1 FOR SHARE;
-- 다른 트랜잭션도 S Lock 획득 가능, X Lock 획득은 불가

-- Exclusive Lock (X Lock): 쓰기 잠금
SELECT * FROM users WHERE id = 1 FOR UPDATE;
UPDATE users SET name = 'Kim' WHERE id = 1;
-- 다른 트랜잭션 S Lock, X Lock 모두 불가
```

### Intention Lock

테이블 레벨에서 행 잠금 의도를 표시하는 Lock이다. 행 Lock을 걸기 전에 테이블에 먼저 Intention Lock을 건다.

```
IS: 테이블 내 특정 행에 S Lock을 걸겠다는 의도
IX: 테이블 내 특정 행에 X Lock을 걸겠다는 의도
```

**왜 필요한가?** 테이블 Lock을 걸려는 트랜잭션이 모든 행을 스캔하지 않고도 행 Lock 존재 여부를 빠르게 확인할 수 있다.

#### Lock 호환성 매트릭스

|  | IS | IX | S | X |
|--|----|----|---|---|
| **IS** | O | O | O | X |
| **IX** | O | O | X | X |
| **S** | O | X | O | X |
| **X** | X | X | X | X |

### Record Lock / Gap Lock / Next-Key Lock

```
┌─────────────────────────────────────────────────────────────┐
│                    InnoDB Row Lock Types                      │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  레코드: 10, 20, 30                                          │
│                                                              │
│  Record Lock:  정확히 해당 레코드만 잠금                      │
│       [10]     [20]     [30]                                 │
│        ↑                                                     │
│                                                              │
│  Gap Lock:  레코드 사이 간격 잠금 (Phantom Read 방지)         │
│       (  10  ···  20  )                                      │
│                                                              │
│  Next-Key Lock:  Record + Gap Lock (InnoDB 기본)             │
│       (  10  ···  20  ]                                      │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

#### Record Lock

- **정의**: 인덱스 레코드에 거는 Lock
- **특징**: 테이블에 인덱스가 없어도 숨겨진 Clustered Index를 사용

```sql
SELECT * FROM users WHERE id = 1 FOR UPDATE;
-- id=1 레코드에만 X Lock
```

#### Gap Lock

- **정의**: 인덱스 레코드 사이의 간격(Gap)에 거는 Lock
- **용도**: Phantom Read 방지
- **특징**: 범위 내 새로운 레코드 삽입 방지, **REPEATABLE READ에서만 동작**

```sql
-- 테이블 상태: id = 10, 20, 30

-- 트랜잭션 A
BEGIN;
SELECT * FROM t WHERE id BETWEEN 15 AND 25 FOR UPDATE;
-- Gap Lock: (10, 20), (20, 30) 잠금
-- Record Lock: id=20 잠금

-- 트랜잭션 B
INSERT INTO t (id) VALUES (17);  -- 대기! Gap Lock에 의해 차단
INSERT INTO t (id) VALUES (22);  -- 대기! Gap Lock에 의해 차단
INSERT INTO t (id) VALUES (5);   -- 성공! 범위 밖
```

#### Gap Lock 발생 조건

```sql
-- 1. 범위 조건 + FOR UPDATE/FOR SHARE
SELECT * FROM t WHERE id > 10 FOR UPDATE;

-- 2. 유니크하지 않은 인덱스 검색
SELECT * FROM t WHERE status = 'PENDING' FOR UPDATE;

-- 3. 존재하지 않는 레코드 검색
SELECT * FROM t WHERE id = 15 FOR UPDATE;  -- id=15 없음 → Gap Lock

-- READ COMMITTED에서는 Gap Lock 없음 (Record Lock만)
```

#### Next-Key Lock

- **정의**: Record Lock + Gap Lock의 조합
- **특징**: InnoDB의 기본 Lock 방식 (REPEATABLE READ)

```
예시: id = 10, 20, 30 존재
SELECT * FROM t WHERE id = 20 FOR UPDATE;

잠금 범위:
- Record Lock: id = 20
- Gap Lock: (10, 20)
- 총: (10, 20] ← Next-Key Lock
```

| Lock 종류 | 잠금 범위 | Phantom Read 방지 |
|-----------|----------|------------------|
| Record Lock | 해당 레코드만 | X |
| Gap Lock | 레코드 사이 간격 | O |
| Next-Key Lock | 레코드 + 앞쪽 간격 | O |

### Insert Intention Lock

- **정의**: INSERT 전에 획득하는 특수한 Gap Lock
- **특징**: 같은 Gap 내 서로 다른 위치에 INSERT하는 경우 서로 대기하지 않음

```sql
-- id = 10, 20 존재하는 상태에서

-- 트랜잭션 A: INSERT INTO t (id) VALUES (13);
-- 트랜잭션 B: INSERT INTO t (id) VALUES (17);
-- 같은 Gap (10, 20) 내이지만, 위치가 다르므로 동시 INSERT 가능!
```

### Auto-Increment Lock

- **정의**: AUTO_INCREMENT 컬럼 값 생성 시 사용되는 테이블 레벨 Lock
- **설정**: `innodb_autoinc_lock_mode` 설정으로 제어

| 모드 | 값 | 설명 | 성능 |
|------|---|------|------|
| Traditional | 0 | 모든 INSERT에 테이블 Lock | 가장 느림 |
| Consecutive | 1 | 단순 INSERT는 Mutex, Bulk INSERT는 테이블 Lock | 중간 |
| Interleaved | 2 | Lock 없이 Mutex만 사용 (MySQL 8.0 기본) | 가장 빠름 |

#### 잠금 에스컬레이션 주의

```sql
-- 인덱스 없는 컬럼 조건
SELECT * FROM users WHERE age = 25 FOR UPDATE;

-- age에 인덱스가 없으면:
-- 테이블 풀 스캔 → 모든 레코드에 Next-Key Lock → 사실상 테이블 잠금!

-- 해결: 인덱스 추가
CREATE INDEX idx_age ON users(age);
```

---

## 3. Named Lock (GET_LOCK)

### 개념

Named Lock은 MySQL이 제공하는 **사용자 정의 Lock**이다. 테이블이나 행이 아닌 **임의의 문자열**을 키로 잠금을 건다. 메타데이터 Lock과 유사하게 MySQL 서버 레벨에서 관리된다.

```
┌─────────────────────────────────────────────────────────────┐
│                    Named Lock 동작 원리                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  일반 Lock:  테이블/행 → Lock                                │
│  Named Lock: 임의 문자열 → Lock                              │
│                                                              │
│  [세션 A] GET_LOCK('order_123', 10)  → 1 (획득 성공)        │
│  [세션 B] GET_LOCK('order_123', 10)  → 대기... (타임아웃)    │
│  [세션 A] RELEASE_LOCK('order_123') → 1 (해제 성공)         │
│  [세션 B] GET_LOCK('order_123', 10)  → 1 (획득 성공)        │
│                                                              │
│  특징: 트랜잭션과 독립적! COMMIT/ROLLBACK으로 해제 안 됨      │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 핵심 함수

```sql
-- 락 획득: 문자열 키, 타임아웃(초)
-- 반환값: 1(성공), 0(타임아웃), NULL(에러)
SELECT GET_LOCK('lock_key', 10);

-- 락 해제
-- 반환값: 1(성공), 0(본인 것 아님), NULL(존재하지 않음)
SELECT RELEASE_LOCK('lock_key');

-- 락 보유 확인
-- 반환값: 1(현재 세션 보유), 0(다른 세션 보유 또는 미보유), NULL(존재 안 함)
SELECT IS_USED_LOCK('lock_key');   -- 보유 세션 ID 반환 (없으면 NULL)
SELECT IS_FREE_LOCK('lock_key');   -- 1(사용 가능), 0(사용 중)

-- 모든 Named Lock 해제 (MySQL 5.7.5+)
SELECT RELEASE_ALL_LOCKS();
```

### Named Lock의 핵심 특성

| 특성 | 설명 |
|------|------|
| **트랜잭션 독립** | COMMIT/ROLLBACK으로 해제되지 않음. 반드시 RELEASE_LOCK() 호출 필요 |
| **세션 기반** | 세션 종료 시 자동 해제됨 |
| **다중 획득** | MySQL 5.7.5+에서 동일 세션이 여러 Named Lock 동시 보유 가능 |
| **재진입 가능** | 동일 세션에서 같은 Lock을 중복 획득 가능 (해제도 여러 번 필요) |
| **타임아웃 지원** | GET_LOCK 호출 시 대기 시간 설정 가능 |
| **전역 범위** | 동일 MySQL 서버의 모든 데이터베이스에서 공유 |

### Named Lock vs 행 Lock 비교

| 비교 항목 | Named Lock | 행 Lock (FOR UPDATE) |
|-----------|-----------|---------------------|
| **잠금 대상** | 임의의 문자열 | 테이블의 특정 행 |
| **해제 시점** | RELEASE_LOCK() 호출 | 트랜잭션 종료 (COMMIT/ROLLBACK) |
| **트랜잭션 의존** | 독립적 | 의존적 |
| **데드락 감지** | 없음 (타임아웃으로 처리) | InnoDB 자동 감지 |
| **성능 영향** | 행 잠금 없음, DB 부하 적음 | 해당 행 잠금, 인덱스 영향 |
| **사용 난이도** | 간단 | 인덱스/격리수준 이해 필요 |

### Spring + JPA에서 Named Lock 구현

```java
// Repository 인터페이스 - 네이티브 쿼리 사용
public interface LockRepository extends JpaRepository<Lock, Long> {

    @Query(value = "SELECT GET_LOCK(:key, :timeoutSeconds)", nativeQuery = true)
    Integer getLock(@Param("key") String key, @Param("timeoutSeconds") int timeoutSeconds);

    @Query(value = "SELECT RELEASE_LOCK(:key)", nativeQuery = true)
    Integer releaseLock(@Param("key") String key);
}
```

```java
// Named Lock Facade
@Component
@RequiredArgsConstructor
public class NamedLockFacade {

    private final LockRepository lockRepository;
    private final StockService stockService;  // @Transactional 비즈니스 로직

    public void decrease(Long productId, int quantity) {
        String lockKey = "stock_" + productId;

        try {
            // Named Lock 획득 (10초 타임아웃)
            Integer result = lockRepository.getLock(lockKey, 10);
            if (result == null || result != 1) {
                throw new RuntimeException("Named Lock 획득 실패: " + lockKey);
            }

            // 비즈니스 로직 수행 (별도 트랜잭션)
            stockService.decrease(productId, quantity);

        } finally {
            // 반드시 해제! (트랜잭션과 무관하게 명시적 호출)
            lockRepository.releaseLock(lockKey);
        }
    }
}
```

```java
// 비즈니스 로직 - 별도 트랜잭션으로 분리
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decrease(Long productId, int quantity) {
        Stock stock = stockRepository.findByProductId(productId)
            .orElseThrow(() -> new NotFoundException("상품 없음"));
        stock.decrease(quantity);
    }
}
```

### Named Lock — 커넥션 풀 분리 (핵심!)

Named Lock은 **트랜잭션과 독립적**이므로, Lock 획득/해제와 비즈니스 로직이 서로 다른 커넥션을 사용해야 한다. 같은 커넥션을 쓰면 Lock 해제 전에 커넥션이 반환되거나, 커넥션 풀이 고갈될 수 있다.

```yaml
# application.yml - 커넥션 풀 분리
spring:
  datasource:
    # 메인 커넥션 풀 (비즈니스 로직용)
    hikari:
      maximum-pool-size: 20
      pool-name: MainPool

  # Named Lock 전용 커넥션 풀
  named-lock-datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: user
    password: pass
    hikari:
      maximum-pool-size: 10
      pool-name: NamedLockPool
      connection-timeout: 15000
```

```java
// 커넥션 풀 분리 설정
@Configuration
public class DataSourceConfig {

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSource mainDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean("namedLockDataSource")
    @ConfigurationProperties("spring.named-lock-datasource")
    public DataSource namedLockDataSource() {
        return DataSourceBuilder.create().build();
    }
}
```

```java
// Named Lock 전용 커넥션 사용
@Component
@RequiredArgsConstructor
public class NamedLockManager {

    @Qualifier("namedLockDataSource")
    private final DataSource namedLockDataSource;

    public boolean acquireLock(String key, int timeoutSeconds) {
        try (Connection conn = namedLockDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT GET_LOCK(?, ?)")) {
            ps.setString(1, key);
            ps.setInt(2, timeoutSeconds);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Named Lock 획득 실패", e);
        }
    }

    public void releaseLock(String key) {
        try (Connection conn = namedLockDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            ps.setString(1, key);
            ps.executeQuery();
        } catch (SQLException e) {
            throw new RuntimeException("Named Lock 해제 실패", e);
        }
    }
}
```

### Named Lock 실무 사례

#### 1. 쿠폰 발급 동시성 제어

```java
public void issueCoupon(Long userId, Long couponId) {
    String lockKey = "coupon_issue_" + couponId;

    try {
        namedLockManager.acquireLock(lockKey, 5);

        // 쿠폰 잔여 수량 확인 및 발급
        Coupon coupon = couponRepository.findById(couponId).orElseThrow();
        if (coupon.getRemainingCount() <= 0) {
            throw new BusinessException("쿠폰 소진");
        }
        coupon.decreaseCount();
        couponIssueRepository.save(new CouponIssue(userId, couponId));

    } finally {
        namedLockManager.releaseLock(lockKey);
    }
}
```

#### 2. 주문번호 채번 (Sequential ID)

```java
public String generateOrderNumber(String prefix) {
    String lockKey = "order_seq_" + prefix;

    try {
        namedLockManager.acquireLock(lockKey, 3);

        // 마지막 번호 조회 후 +1
        int lastSeq = sequenceRepository.getLastSequence(prefix);
        int newSeq = lastSeq + 1;
        sequenceRepository.updateSequence(prefix, newSeq);

        return prefix + String.format("%06d", newSeq);
    } finally {
        namedLockManager.releaseLock(lockKey);
    }
}
```

#### 3. 외부 API 중복 호출 방지

```java
public PaymentResult processPayment(String orderId) {
    String lockKey = "payment_" + orderId;

    try {
        if (!namedLockManager.acquireLock(lockKey, 0)) {
            // 이미 다른 세션에서 결제 처리 중
            throw new DuplicatePaymentException("결제 처리 중입니다");
        }

        // 멱등성 확인
        Optional<Payment> existing = paymentRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            return PaymentResult.of(existing.get());
        }

        // PG사 API 호출 (외부 API이므로 중복 호출 치명적)
        PaymentResult result = pgClient.requestPayment(orderId);
        paymentRepository.save(Payment.from(result));
        return result;

    } finally {
        namedLockManager.releaseLock(lockKey);
    }
}
```

### Named Lock 주의사항

```
⚠️ 반드시 RELEASE_LOCK() 호출 — 트랜잭션 종료로 해제되지 않음
⚠️ 커넥션 풀 분리 — 같은 풀 사용 시 데드락/고갈 위험
⚠️ 타임아웃 설정 — 무한 대기 방지 (0초 = 즉시 실패)
⚠️ Lock 키 길이 — 64바이트 제한
⚠️ 단일 MySQL 인스턴스 범위 — 다른 서버 간 공유 불가 (분산 환경 한계)
```

---

## 4. 비관적 Lock (Pessimistic Lock)

### 개념

**"충돌이 반드시 발생한다"**고 가정하고, 데이터를 읽는 시점에 Lock을 걸어 다른 트랜잭션의 접근을 차단하는 방식이다.

```
┌───────────────────────────────────────────────────────────────┐
│                  비관적 Lock 동작 흐름                          │
├───────────────────────────────────────────────────────────────┤
│                                                                │
│  TX-A                          TX-B                           │
│   │                             │                             │
│   ├── SELECT ... FOR UPDATE     │                             │
│   │   (X Lock 획득)             │                             │
│   │                             ├── SELECT ... FOR UPDATE     │
│   │                             │   (대기... Lock 해제까지)    │
│   ├── UPDATE ...                │                             │
│   ├── COMMIT (Lock 해제)        │                             │
│   │                             ├── Lock 획득!                │
│   │                             ├── UPDATE ...                │
│   │                             ├── COMMIT                    │
│                                                                │
└───────────────────────────────────────────────────────────────┘
```

### SQL 문법

```sql
-- Exclusive Lock (배타적 잠금) — 가장 많이 사용
SELECT * FROM stock WHERE product_id = 1 FOR UPDATE;

-- Shared Lock (공유 잠금) — 읽기만 보호
SELECT * FROM stock WHERE product_id = 1 FOR SHARE;

-- NOWAIT — 락 획득 불가 시 즉시 에러 (MySQL 8.0+)
SELECT * FROM stock WHERE product_id = 1 FOR UPDATE NOWAIT;
-- ERROR 3572: Statement aborted because lock(s) could not be acquired immediately

-- SKIP LOCKED — 잠긴 행은 건너뜀 (MySQL 8.0+)
SELECT * FROM stock WHERE status = 'PENDING'
FOR UPDATE SKIP LOCKED LIMIT 10;
```

### JPA에서 비관적 Lock

```java
// Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stock s WHERE s.productId = :productId")
    Optional<Stock> findByProductIdWithPessimisticLock(@Param("productId") Long productId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT s FROM Stock s WHERE s.productId = :productId")
    Optional<Stock> findByProductIdWithSharedLock(@Param("productId") Long productId);
}
```

```java
// Service
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    @Transactional
    public void decrease(Long productId, int quantity) {
        Stock stock = stockRepository.findByProductIdWithPessimisticLock(productId)
            .orElseThrow(() -> new NotFoundException("상품 없음"));

        if (stock.getQuantity() < quantity) {
            throw new InsufficientStockException("재고 부족");
        }

        stock.decrease(quantity);
        // 트랜잭션 종료 시 Lock 자동 해제
    }
}
```

### JPA Lock 모드 종류

| LockModeType | SQL | 설명 |
|-------------|-----|------|
| `PESSIMISTIC_READ` | FOR SHARE | 공유 잠금, 동시 읽기 가능 |
| `PESSIMISTIC_WRITE` | FOR UPDATE | 배타 잠금, 읽기/쓰기 모두 차단 |
| `PESSIMISTIC_FORCE_INCREMENT` | FOR UPDATE + version++ | 배타 잠금 + 버전 증가 |

### 락 타임아웃 설정

```java
// 방법 1: @QueryHints 사용
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
@Query("SELECT s FROM Stock s WHERE s.productId = :productId")
Optional<Stock> findWithLock(@Param("productId") Long productId);

// 방법 2: EntityManager 직접 사용
Map<String, Object> hints = Map.of("jakarta.persistence.lock.timeout", 3000);
Stock stock = entityManager.find(Stock.class, id, LockModeType.PESSIMISTIC_WRITE, hints);
```

### SKIP LOCKED 활용 — 큐 워커 패턴

```java
// 여러 워커가 동시에 미처리 작업을 가져가는 패턴
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = """
        SELECT * FROM tasks
        WHERE status = 'PENDING'
        ORDER BY created_at
        FOR UPDATE SKIP LOCKED
        LIMIT :batchSize
        """, nativeQuery = true)
    List<Task> findPendingTasksForProcessing(@Param("batchSize") int batchSize);
}

// 워커 서비스 — 각 인스턴스가 서로 다른 작업을 잠금
@Transactional
public void processNextBatch() {
    List<Task> tasks = taskRepository.findPendingTasksForProcessing(10);
    // 이미 잠긴 행은 건너뛰므로, 여러 워커가 동시에 실행해도 중복 없음
    for (Task task : tasks) {
        task.process();
        task.markCompleted();
    }
}
```

---

## 5. 낙관적 Lock (Optimistic Lock)

### 개념

**"충돌이 거의 발생하지 않을 것"**이라고 가정하고, DB Lock 없이 **version 컬럼**으로 충돌을 감지하는 방식이다. 실제 Lock을 걸지 않으므로 "Lock"이라는 이름은 관례적 표현이다.

```
┌───────────────────────────────────────────────────────────────┐
│                  낙관적 Lock 동작 흐름                          │
├───────────────────────────────────────────────────────────────┤
│                                                                │
│  TX-A                          TX-B                           │
│   │                             │                             │
│   ├── SELECT (version=1)        │                             │
│   │                             ├── SELECT (version=1)        │
│   ├── UPDATE ... WHERE          │                             │
│   │   version = 1               │                             │
│   │   SET version = 2           │                             │
│   ├── COMMIT ✓ (version 1→2)   │                             │
│   │                             ├── UPDATE ... WHERE          │
│   │                             │   version = 1               │
│   │                             │   → 0 rows affected!       │
│   │                             ├── 충돌 감지! 재시도 필요     │
│                                                                │
└───────────────────────────────────────────────────────────────┘
```

### JPA에서 낙관적 Lock

```java
// Entity - @Version 어노테이션
@Entity
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;
    private int quantity;

    @Version  // 핵심! JPA가 자동으로 version 관리
    private Long version;

    public void decrease(int quantity) {
        if (this.quantity < quantity) {
            throw new InsufficientStockException("재고 부족");
        }
        this.quantity -= quantity;
    }
}
```

```java
// JPA가 생성하는 SQL
UPDATE stock
SET quantity = ?, version = version + 1
WHERE id = ? AND version = ?
-- version이 다르면 0 rows affected → OptimisticLockException 발생
```

### 재시도 로직 구현

```java
// Facade — 재시도 담당
@Component
@RequiredArgsConstructor
public class OptimisticLockFacade {

    private final StockService stockService;

    public void decrease(Long productId, int quantity) {
        int maxRetries = 50;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                stockService.decrease(productId, quantity);
                return;  // 성공
            } catch (OptimisticLockingFailureException e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    throw new RuntimeException("재시도 횟수 초과", e);
                }
                try {
                    Thread.sleep(50);  // 잠시 대기 후 재시도
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            }
        }
    }
}
```

```java
// Spring Retry 활용
@Service
public class StockService {

    @Retryable(
        retryFor = OptimisticLockingFailureException.class,
        maxAttempts = 5,
        backoff = @Backoff(delay = 100, multiplier = 2, maxDelay = 1000)
    )
    @Transactional
    public void decrease(Long productId, int quantity) {
        Stock stock = stockRepository.findByProductId(productId)
            .orElseThrow();
        stock.decrease(quantity);
    }

    @Recover
    public void recoverDecrease(OptimisticLockingFailureException e,
                                Long productId, int quantity) {
        log.error("낙관적 락 재시도 실패: productId={}, quantity={}", productId, quantity);
        throw new BusinessException("일시적인 충돌이 발생했습니다. 잠시 후 다시 시도해주세요.");
    }
}
```

### 낙관적 Lock 실무 사례

#### 1. 게시글 동시 수정 방지

```java
@Entity
public class Post {
    @Id private Long id;
    private String title;
    private String content;

    @Version
    private Long version;
}

// 프론트엔드에서 version 값을 함께 전송
@PutMapping("/posts/{id}")
public PostResponse updatePost(@PathVariable Long id,
                                @RequestBody PostUpdateRequest request) {
    try {
        return postService.update(id, request.getTitle(),
            request.getContent(), request.getVersion());
    } catch (OptimisticLockingFailureException e) {
        throw new ConflictException("다른 사용자가 수정 중입니다. 새로고침 후 다시 시도해주세요.");
    }
}
```

#### 2. 포인트 적립/차감

```java
@Entity
public class UserPoint {
    @Id private Long userId;
    private int balance;
    @Version private Long version;
}

// 충돌이 드문 경우에 적합 (사용자별 포인트 → 동일 사용자가 동시에 조작할 확률 낮음)
@Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3)
@Transactional
public void addPoint(Long userId, int amount) {
    UserPoint point = userPointRepository.findById(userId).orElseThrow();
    point.addBalance(amount);
}
```

### 낙관적 Lock 주의사항

```
⚠️ 충돌 빈도가 높으면 재시도 비용이 증가 → 비관적 Lock이 유리
⚠️ 재시도 로직 필수 — @Version만 선언하면 예외만 발생하고 끝
⚠️ 벌크 업데이트 시 @Version 무시됨 — JPQL UPDATE는 version 체크 안 함
⚠️ 프론트엔드 연동 시 version 값 전달 필요
```

---

## 6. 비관적 vs 낙관적 비교

| 비교 항목 | 비관적 Lock | 낙관적 Lock |
|-----------|-----------|-----------|
| **Lock 시점** | 데이터 읽기 시 즉시 | 데이터 수정 시 |
| **충돌 가정** | 충돌이 자주 발생 | 충돌이 드물게 발생 |
| **DB Lock** | 실제 Lock 사용 (X Lock) | Lock 없음 (version 비교) |
| **대기 방식** | 다른 트랜잭션이 대기 | 충돌 시 예외 발생 + 재시도 |
| **성능** | 동시성 낮음, 대기 시간 발생 | 동시성 높음, 재시도 비용 |
| **데드락** | 가능 | 불가 |
| **구현 복잡도** | 낮음 (FOR UPDATE만) | 중간 (재시도 로직 필요) |
| **적합 상황** | 충돌 빈번, 짧은 트랜잭션 | 충돌 드묾, 읽기 위주 |

### 상황별 선택 기준

```
충돌 빈도 높음 (재고 차감, 선착순 이벤트)
 → 비관적 Lock ✓
 → 낙관적 Lock은 재시도가 폭증하여 성능 저하

충돌 빈도 낮음 (게시글 수정, 설정 변경)
 → 낙관적 Lock ✓
 → 비관적 Lock은 불필요한 대기 시간 발생

분산 환경 (다중 서버)
 → 분산 Lock (Redis, Named Lock) ✓
 → 단일 DB Lock으로는 서버 간 동기화 불가... 는 아님!
   DB Lock은 DB 서버가 하나이면 분산 환경에서도 동작

긴 트랜잭션 (외부 API 호출 포함)
 → Named Lock 또는 Redis 분산 Lock ✓
 → 비관적 Lock은 행 잠금 시간이 길어져 병목
```

### 성능 벤치마크 (참고용)

```
[시나리오] 동시 100 스레드, 같은 재고 1개 차감

비관적 Lock (FOR UPDATE)
  총 소요 시간: ~3.5초
  성공률: 100%
  평균 대기: 35ms/요청

낙관적 Lock (@Version + 재시도)
  총 소요 시간: ~8.2초
  재시도 횟수: ~4,950회 (100개 중 평균 49.5회 충돌)
  성공률: 100% (충분한 재시도 시)

Named Lock (GET_LOCK)
  총 소요 시간: ~4.0초
  성공률: 100%
  커넥션 풀 분리 필요

Redis 분산 Lock (Redisson)
  총 소요 시간: ~3.8초
  성공률: 100%
  네트워크 홉 추가

※ 충돌이 빈번한 시나리오에서는 비관적 Lock > Named Lock > Redis > 낙관적 Lock 순으로 유리
※ 충돌이 드문 시나리오에서는 낙관적 Lock이 가장 효율적 (Lock 오버헤드 제로)
```

---

## 7. 분산 Lock

### 왜 분산 Lock이 필요한가?

```
[서버 A] → DB Lock → MySQL (단일 인스턴스) ← DB Lock ← [서버 B]
→ 단일 DB이면 FOR UPDATE로도 분산 환경 동시성 제어 가능!

그런데...
- DB 부하를 줄이고 싶다면?
- 트랜잭션 범위 밖에서 Lock이 필요하다면?
- DB가 여러 개(샤딩)라면?
→ 별도 분산 Lock 인프라 필요
```

### 분산 Lock 구현 방식 비교

| 방식 | 장점 | 단점 | 적합 상황 |
|------|------|------|----------|
| **MySQL Named Lock** | 추가 인프라 불필요 | DB 부하 증가, 단일 MySQL 한정 | 소규모, 이미 MySQL 사용 |
| **Redis (Redisson)** | 빠른 성능, Pub/Sub 대기 | Redis 의존성 추가 | 고성능 요구, 이미 Redis 사용 |
| **Zookeeper** | 높은 신뢰성, 강한 일관성 | 운영 복잡, 상대적으로 느림 | 금융 등 높은 신뢰성 요구 |
| **etcd** | 경량, Kubernetes 친화적 | 생태계 작음 | 클라우드 네이티브 환경 |

### Redis 분산 Lock (Redisson)

```java
@Service
@RequiredArgsConstructor
public class StockService {

    private final RedissonClient redissonClient;
    private final StockRepository stockRepository;

    public void decrease(Long productId, int quantity) {
        RLock lock = redissonClient.getLock("lock:stock:" + productId);

        try {
            // waitTime: 락 획득 대기 시간, leaseTime: 락 유지 시간
            boolean acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!acquired) {
                throw new LockAcquisitionException("락 획득 실패");
            }

            // 비즈니스 로직
            Stock stock = stockRepository.findByProductId(productId).orElseThrow();
            stock.decrease(quantity);
            stockRepository.save(stock);

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

### Redisson vs SETNX 비교

| 비교 항목 | SETNX (직접 구현) | Redisson |
|-----------|------------------|---------|
| **대기 방식** | 스핀락 (polling) | Pub/Sub (이벤트 기반) |
| **CPU 낭비** | O (반복 조회) | X (구독 대기) |
| **Watchdog** | 직접 구현 필요 | 자동 갱신 (30초) |
| **재진입** | 미지원 | 지원 |
| **공정성** | 미보장 | 공정 Lock 옵션 |

### AOP 기반 분산 Lock 어노테이션

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    String key();                    // 락 키 (SpEL 지원)
    long waitTime() default 5000;    // 대기 시간 (ms)
    long leaseTime() default 10000;  // 락 유지 시간 (ms)
}

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

```java
// 사용 예시 — 어노테이션 하나로 분산 Lock
@Service
public class CouponService {

    @DistributedLock(key = "'lock:coupon:' + #couponId", waitTime = 3000)
    public void issue(Long couponId, Long userId) {
        Coupon coupon = couponRepository.findById(couponId).orElseThrow();
        coupon.issue(userId);
    }
}
```

---

## 8. 전체 Lock 비교표

| Lock 종류 | 레벨 | 잠금 대상 | 성능 | 데드락 | 분산 지원 | 적합 상황 |
|-----------|------|----------|------|--------|----------|----------|
| **Record Lock** | DB 엔진 | 인덱스 레코드 | 높음 | 가능 | 단일 DB | 특정 행 보호 |
| **Gap Lock** | DB 엔진 | 인덱스 간격 | 중간 | 가능 | 단일 DB | Phantom Read 방지 |
| **Next-Key Lock** | DB 엔진 | 레코드+간격 | 중간 | 가능 | 단일 DB | InnoDB 기본 |
| **Table Lock** | DB 엔진 | 테이블 전체 | 낮음 | 낮음 | 단일 DB | DDL, 백업 |
| **Named Lock** | DB 사용자 | 임의 문자열 | 높음 | 가능 | 단일 MySQL | 논리적 자원 보호 |
| **비관적 Lock** | 애플리케이션 | DB 행 | 중간 | 가능 | 단일 DB | 충돌 빈번 |
| **낙관적 Lock** | 애플리케이션 | version 컬럼 | 높음 | 없음 | 단일 DB | 충돌 드묾 |
| **Redis Lock** | 분산 | Redis Key | 높음 | 설계 의존 | O | 분산 환경 고성능 |
| **Zookeeper Lock** | 분산 | ZNode | 중간 | 낮음 | O | 높은 신뢰성 요구 |

---

## 9. 실무 시나리오별 Lock 선택 가이드

### 시나리오 1: 재고 차감 (선착순)

```
동시 요청 수: 높음  |  충돌 빈도: 매우 높음  |  데이터 정합성: 필수

단일 서버: 비관적 Lock (FOR UPDATE)
다중 서버 + 단일 DB: 비관적 Lock (FOR UPDATE) — DB가 하나면 충분
다중 서버 + Redis 있음: Redis 분산 Lock (Redisson)
```

### 시나리오 2: 게시글/설정 수정

```
동시 요청 수: 낮음  |  충돌 빈도: 매우 낮음  |  UX 중요

→ 낙관적 Lock (@Version)
→ 충돌 시 "다른 사용자가 수정 중" 안내
```

### 시나리오 3: 쿠폰 발급 (한정 수량)

```
동시 요청 수: 폭주  |  충돌 빈도: 극히 높음  |  정확성 필수

→ Redis 분산 Lock + 카운터
→ 또는 Named Lock (MySQL만으로 해결 가능)
```

### 시나리오 4: 스케줄러 중복 실행 방지

```
인스턴스 수: 2~5개  |  실행 주기: 분/시간 단위  |  한 번만 실행

→ ShedLock (DB 테이블 기반) — 가장 간단
→ 또는 Redis 분산 Lock (인프라 있을 때)
→ 또는 Named Lock (MySQL 단독 환경)
```

### 시나리오 5: 결제/정산 (외부 API 연동)

```
요청 수: 중간  |  정합성: 최우선  |  트랜잭션 시간: 긴 편

→ Named Lock 또는 Redis 분산 Lock
→ 비관적 Lock은 행 잠금 시간이 길어서 부적합
→ 멱등성 키 조합으로 중복 방지 강화
```

### 시나리오 6: 세무 기장 — 신고서 동시 작성 방지

```
사용자: 세무사 + AI 동시 작업 가능  |  문서: 장시간 편집

→ 낙관적 Lock (version) + 편집 세션 관리
→ 또는 Named Lock으로 문서 단위 잠금
→ 긴 편집 시간 → 비관적 Lock은 부적합
```

---

## 10. 데드락과 트러블슈팅

### 데드락 4가지 필요조건

```
1. 상호 배제 (Mutual Exclusion)   — 리소스는 한 번에 하나의 트랜잭션만 사용
2. 점유 대기 (Hold and Wait)      — 리소스를 점유한 채 다른 리소스 대기
3. 비선점 (No Preemption)         — 다른 트랜잭션의 리소스를 강제로 빼앗을 수 없음
4. 순환 대기 (Circular Wait)      — 트랜잭션들이 순환 형태로 서로의 리소스 대기
```

### 데드락 예시: 기본 패턴

```sql
-- 트랜잭션 A
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 1;  -- Lock id=1
UPDATE accounts SET balance = balance + 100 WHERE id = 2;  -- Wait for id=2

-- 트랜잭션 B (동시 실행)
BEGIN;
UPDATE accounts SET balance = balance - 50 WHERE id = 2;   -- Lock id=2
UPDATE accounts SET balance = balance + 50 WHERE id = 1;   -- Wait for id=1

-- 데드락! A는 B를, B는 A를 기다림
```

### 데드락 예시: Gap Lock 데드락

```sql
-- 테이블: id = 10, 20

-- 트랜잭션 A
BEGIN;
SELECT * FROM t WHERE id = 15 FOR UPDATE;  -- Gap Lock (10, 20)

-- 트랜잭션 B
BEGIN;
SELECT * FROM t WHERE id = 17 FOR UPDATE;  -- Gap Lock (10, 20) — 같은 Gap이지만 호환

-- 트랜잭션 A
INSERT INTO t (id) VALUES (16);  -- B의 Gap Lock 대기

-- 트랜잭션 B
INSERT INTO t (id) VALUES (18);  -- A의 Gap Lock 대기

-- 데드락!
```

### 락 모니터링

```sql
-- MySQL 8.0+ 현재 락 상태
SELECT * FROM performance_schema.data_locks;
SELECT * FROM performance_schema.data_lock_waits;

-- 현재 트랜잭션
SELECT * FROM information_schema.INNODB_TRX;

-- InnoDB 상태 (데드락 정보 포함)
SHOW ENGINE INNODB STATUS\G

-- 락 타임아웃 설정 (기본 50초)
SHOW VARIABLES LIKE 'innodb_lock_wait_timeout';
SET innodb_lock_wait_timeout = 10;
```

### 데드락 방지 전략

```sql
-- 1. 일관된 락 순서 — 항상 같은 순서로 리소스 접근 (예: ID 오름차순)

-- 2. 짧은 트랜잭션 — 락 보유 시간 최소화

-- 3. 인덱스 활용 — 인덱스 없으면 풀 스캔 → 많은 락
CREATE INDEX idx_status ON orders(status);

-- 4. 낮은 격리 수준 — 필요하다면 READ COMMITTED (Gap Lock 제거)
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;

-- 5. 락 범위 최소화
SELECT * FROM orders WHERE status = 'PENDING'
FOR UPDATE SKIP LOCKED LIMIT 10;
```

### Spring에서 데드락 재시도

```java
@Retryable(
    retryFor = {DeadlockLoserDataAccessException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 100, multiplier = 2)
)
@Transactional
public void processOrder(Long orderId) {
    // 데드락 발생 시 자동 재시도
}
```

---

## 전체 요약

```
Lock 선택 흐름도:

단일 서버인가? ─── No ──→ 분산 Lock 필요
       │                    ├── Redis 있음 → Redisson
       │                    ├── MySQL만 → Named Lock
       │                    └── 스케줄러 → ShedLock
       │
      Yes
       │
충돌이 잦은가? ─── Yes ──→ 비관적 Lock (FOR UPDATE)
       │                    └── SKIP LOCKED로 큐 패턴 고려
       │
      No
       │
트랜잭션 밖에서 ── Yes ──→ Named Lock (GET_LOCK)
Lock이 필요한가?
       │
      No
       │
낙관적 Lock (@Version + 재시도)
```

---

*마지막 업데이트: 2026년 02월*

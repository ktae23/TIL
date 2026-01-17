# MySQL Lock 심화

InnoDB의 잠금 메커니즘인 Gap Lock, Next-Key Lock과 데드락 발생 조건을 정리합니다.

## 목차

1. [InnoDB 잠금 종류](#1-innodb-잠금-종류)
2. [Gap Lock](#2-gap-lock)
3. [Next-Key Lock](#3-next-key-lock)
4. [데드락 발생 조건](#4-데드락-발생-조건)
5. [락 모니터링](#5-락-모니터링)
6. [락 최적화 전략](#6-락-최적화-전략)

---

## 1. InnoDB 잠금 종류

### 잠금 유형 분류

```
┌─────────────────────────────────────────────────────────────┐
│                    InnoDB Lock Types                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Row-Level Locks (행 수준)                                  │
│  ├── Record Lock: 인덱스 레코드 잠금                        │
│  ├── Gap Lock: 레코드 사이 간격 잠금                        │
│  └── Next-Key Lock: Record + Gap Lock                      │
│                                                             │
│  Table-Level Locks (테이블 수준)                            │
│  ├── Table Lock: 전체 테이블 잠금                           │
│  ├── Intention Lock: 행 잠금 의도 표시                      │
│  │   ├── IS (Intention Shared)                             │
│  │   └── IX (Intention Exclusive)                          │
│  └── Auto-increment Lock: AUTO_INCREMENT 값 잠금           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Shared Lock vs Exclusive Lock

```sql
-- Shared Lock (S Lock): 읽기 잠금
SELECT * FROM users WHERE id = 1 FOR SHARE;
-- 다른 트랜잭션도 S Lock 획득 가능
-- X Lock 획득은 불가

-- Exclusive Lock (X Lock): 쓰기 잠금
SELECT * FROM users WHERE id = 1 FOR UPDATE;
UPDATE users SET name = 'Kim' WHERE id = 1;
-- 다른 트랜잭션 S Lock, X Lock 모두 불가
```

### Intention Lock

```
IS: 테이블 내 특정 행에 S Lock을 걸겠다는 의도
IX: 테이블 내 특정 행에 X Lock을 걸겠다는 의도

호환성 매트릭스:
       IS    IX    S     X
IS     O     O     O     X
IX     O     O     X     X
S      O     X     O     X
X      X     X     X     X
```

---

## 2. Gap Lock

### 정의

```
Gap Lock: 인덱스 레코드 사이의 "간격"을 잠금
목적: Phantom Read 방지

예시: id가 10, 20인 레코드가 있을 때
     (-∞, 10), (10, 20), (20, +∞) 간격 존재
```

### 동작 예시

```sql
-- 테이블 상태: id = 10, 20, 30

-- 트랜잭션 A
BEGIN;
SELECT * FROM table WHERE id BETWEEN 15 AND 25 FOR UPDATE;
-- Gap Lock: (10, 20), (20, 30) 잠금
-- Record Lock: id=20 잠금

-- 트랜잭션 B
INSERT INTO table (id) VALUES (17);  -- 대기! Gap Lock에 의해 차단
INSERT INTO table (id) VALUES (22);  -- 대기! Gap Lock에 의해 차단
INSERT INTO table (id) VALUES (5);   -- 성공! 범위 밖
```

### Gap Lock 발생 조건

```sql
-- REPEATABLE READ 격리 수준에서 발생
-- READ COMMITTED에서는 Gap Lock 없음 (Record Lock만)

-- Gap Lock 발생하는 경우:
-- 1. 범위 조건 + FOR UPDATE/FOR SHARE
SELECT * FROM t WHERE id > 10 FOR UPDATE;

-- 2. 유니크하지 않은 인덱스 검색
SELECT * FROM t WHERE status = 'PENDING' FOR UPDATE;

-- 3. 존재하지 않는 레코드 검색
SELECT * FROM t WHERE id = 15 FOR UPDATE;  -- id=15 없음 → Gap Lock
```

---

## 3. Next-Key Lock

### 정의

```
Next-Key Lock = Record Lock + Gap Lock
InnoDB의 기본 잠금 방식 (REPEATABLE READ)

예시: id = 10, 20, 30 존재
SELECT * FROM t WHERE id = 20 FOR UPDATE;

잠금 범위:
- Record Lock: id = 20
- Gap Lock: (10, 20)
- 총: (10, 20] ← Next-Key Lock
```

### 잠금 범위 시각화

```
레코드: 10, 20, 30

┌───────┬───────┬───────┬───────┬───────┐
│ (-∞,10] (10,20] (20,30] (30,+∞) │
├───────┴───────┴───────┴───────┴───────┤
│    Next-Key Lock on id=20:            │
│         (10, 20]                       │
│         = Gap(10,20) + Record(20)     │
└───────────────────────────────────────┘
```

### 잠금 에스컬레이션 예시

```sql
-- 인덱스 없는 컬럼 조건
SELECT * FROM users WHERE age = 25 FOR UPDATE;

-- age에 인덱스가 없으면:
-- 테이블 풀 스캔 → 모든 레코드에 Next-Key Lock
-- 사실상 테이블 잠금!

-- 해결: 인덱스 추가
CREATE INDEX idx_age ON users(age);
```

---

## 4. 데드락 발생 조건

### 데드락 4가지 필요조건

```
1. 상호 배제 (Mutual Exclusion)
   - 리소스는 한 번에 하나의 트랜잭션만 사용

2. 점유 대기 (Hold and Wait)
   - 리소스를 점유한 채 다른 리소스 대기

3. 비선점 (No Preemption)
   - 다른 트랜잭션의 리소스를 강제로 빼앗을 수 없음

4. 순환 대기 (Circular Wait)
   - 트랜잭션들이 순환 형태로 서로의 리소스 대기
```

### 데드락 예시 1: 기본 패턴

```sql
-- 트랜잭션 A
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 1;  -- Lock id=1
-- 잠시 후
UPDATE accounts SET balance = balance + 100 WHERE id = 2;  -- Wait for id=2

-- 트랜잭션 B (동시 실행)
BEGIN;
UPDATE accounts SET balance = balance - 50 WHERE id = 2;   -- Lock id=2
-- 잠시 후
UPDATE accounts SET balance = balance + 50 WHERE id = 1;   -- Wait for id=1

-- 데드락! A는 B를, B는 A를 기다림
```

### 데드락 예시 2: Gap Lock 데드락

```sql
-- 테이블: id = 10, 20

-- 트랜잭션 A
BEGIN;
SELECT * FROM t WHERE id = 15 FOR UPDATE;  -- Gap Lock (10, 20)

-- 트랜잭션 B
BEGIN;
SELECT * FROM t WHERE id = 17 FOR UPDATE;  -- Gap Lock (10, 20) - 같은 Gap이지만 호환

-- 트랜잭션 A
INSERT INTO t (id) VALUES (16);  -- B의 Gap Lock 대기

-- 트랜잭션 B
INSERT INTO t (id) VALUES (18);  -- A의 Gap Lock 대기

-- 데드락!
```

### InnoDB 데드락 처리

```sql
-- InnoDB는 자동으로 데드락 감지
-- 비용이 적은 트랜잭션을 희생양으로 선택하여 롤백

-- 에러 메시지
ERROR 1213 (40001): Deadlock found when trying to get lock;
try restarting transaction
```

---

## 5. 락 모니터링

### 현재 락 상태 확인

```sql
-- 현재 락 대기 상태
SELECT * FROM information_schema.INNODB_LOCK_WAITS;

-- 현재 락 정보
SELECT * FROM information_schema.INNODB_LOCKS;

-- 현재 트랜잭션
SELECT * FROM information_schema.INNODB_TRX;

-- MySQL 8.0+
SELECT * FROM performance_schema.data_locks;
SELECT * FROM performance_schema.data_lock_waits;
```

### InnoDB 상태 확인

```sql
SHOW ENGINE INNODB STATUS\G

-- 출력에서 확인할 섹션:
-- TRANSACTIONS: 현재 트랜잭션 목록
-- LATEST DETECTED DEADLOCK: 최근 데드락 정보
-- SEMAPHORES: 락 대기 정보
```

### 락 타임아웃 설정

```sql
-- 락 대기 타임아웃 (기본 50초)
SHOW VARIABLES LIKE 'innodb_lock_wait_timeout';
SET innodb_lock_wait_timeout = 10;

-- 타임아웃 시 에러
ERROR 1205 (HY000): Lock wait timeout exceeded;
try restarting transaction
```

---

## 6. 락 최적화 전략

### 데드락 방지

```sql
-- 1. 일관된 락 순서
-- 항상 같은 순서로 리소스 접근 (예: ID 오름차순)

-- 2. 짧은 트랜잭션
-- 락 보유 시간 최소화

-- 3. 인덱스 활용
-- 인덱스 없으면 풀 스캔 → 많은 락
CREATE INDEX idx_status ON orders(status);

-- 4. 낮은 격리 수준
-- 필요하다면 READ COMMITTED 사용 (Gap Lock 없음)
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
```

### 락 범위 최소화

```sql
-- Bad: 전체 조회 후 처리
BEGIN;
SELECT * FROM orders WHERE status = 'PENDING' FOR UPDATE;
-- 모든 PENDING 주문에 락

-- Good: 필요한 것만
BEGIN;
SELECT * FROM orders WHERE status = 'PENDING'
FOR UPDATE SKIP LOCKED LIMIT 10;
-- 10개만 락, 이미 잠긴 건 스킵
```

### 낙관적 락 활용

```sql
-- 버전 컬럼 사용
UPDATE orders
SET status = 'PROCESSING', version = version + 1
WHERE id = 1 AND version = 5;

-- 영향받은 행이 0이면 다른 트랜잭션이 먼저 수정
-- 재시도 또는 충돌 처리
```

### 재시도 로직

```java
@Retryable(
    value = {DeadlockLoserDataAccessException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 100, multiplier = 2)
)
public void processOrder(Long orderId) {
    // 데드락 발생 시 자동 재시도
}
```

---

## 핵심 정리

| 락 종류 | 범위 | 목적 |
|--------|------|------|
| Record Lock | 단일 레코드 | 특정 행 보호 |
| Gap Lock | 레코드 간 간격 | Phantom Read 방지 |
| Next-Key Lock | Record + Gap | InnoDB 기본 |

| 데드락 조건 | 설명 |
|------------|------|
| 상호 배제 | 동시 접근 불가 |
| 점유 대기 | 점유하고 대기 |
| 비선점 | 강제 해제 불가 |
| 순환 대기 | 순환 형태 대기 |

---

*마지막 업데이트: 2025년 01월*

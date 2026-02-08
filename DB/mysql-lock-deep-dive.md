# MySQL Lock 완전 정리

MySQL(InnoDB)에서 동시성 제어를 위해 사용되는 Lock의 종류, 동작 원리, 심화 메커니즘, 그리고 실무 트러블슈팅을 체계적으로 정리한다.

## 목차

1. [MySQL Lock 개요](#1-mysql-lock-개요)
2. [Lock 종류](#2-lock-종류)
   - [Shared Lock vs Exclusive Lock](#shared-lock-vs-exclusive-lock)
   - [Lock 레벨별 분류](#lock-레벨별-분류)
   - [InnoDB Lock 종류](#innodb-lock-종류)
3. [심화: Gap Lock, Next-Key Lock](#3-심화-gap-lock-next-key-lock)
   - [Gap Lock](#gap-lock)
   - [Next-Key Lock](#next-key-lock)
4. [실무 예제 및 트러블슈팅](#4-실무-예제-및-트러블슈팅)
   - [데드락 발생 조건](#데드락-발생-조건)
   - [락 모니터링](#락-모니터링)
   - [락 최적화 전략](#락-최적화-전략)

---

## 1. MySQL Lock 개요

Lock은 여러 트랜잭션이 동시에 같은 데이터에 접근할 때 **데이터 일관성**을 보장하기 위한 메커니즘이다.

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

---

## 2. Lock 종류

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
-- 다른 트랜잭션도 S Lock 획득 가능
-- X Lock 획득은 불가

-- Exclusive Lock (X Lock): 쓰기 잠금
SELECT * FROM users WHERE id = 1 FOR UPDATE;
UPDATE users SET name = 'Kim' WHERE id = 1;
-- 다른 트랜잭션 S Lock, X Lock 모두 불가
```

### Intention Lock

테이블 레벨에서 행 잠금 의도를 표시하는 Lock이다.

```
IS: 테이블 내 특정 행에 S Lock을 걸겠다는 의도
IX: 테이블 내 특정 행에 X Lock을 걸겠다는 의도
```

### Lock 호환성 매트릭스

|  | IS | IX | S | X |
|--|----|----|---|---|
| **IS** | O | O | O | X |
| **IX** | O | O | X | X |
| **S** | O | X | O | X |
| **X** | X | X | X | X |

### Lock 레벨별 분류

| Lock 레벨 | 설명 | 장점 | 단점 |
|-----------|------|------|------|
| **Global Lock** | 데이터베이스 전체에 Lock | 단순함 | 동시성 매우 낮음 |
| **Table Lock** | 테이블 단위 Lock | 구현 단순, 오버헤드 적음 | 동시성 낮음 |
| **Row Lock** | 행 단위 Lock | 높은 동시성 | 오버헤드 큼, Deadlock 가능성 |

### InnoDB Lock 종류

#### Record Lock

- **정의**: 인덱스 레코드에 거는 Lock
- **특징**: 테이블에 인덱스가 없어도 숨겨진 Clustered Index를 사용

```sql
-- Record Lock 발생 예시
SELECT * FROM users WHERE id = 1 FOR UPDATE;
```

#### Gap Lock

- **정의**: 인덱스 레코드 사이의 간격(Gap)에 거는 Lock
- **용도**: Phantom Read 방지
- **특징**: 범위 내 새로운 레코드 삽입 방지

```sql
-- Gap Lock 발생 예시 (id가 10~20 사이에 Lock)
SELECT * FROM users WHERE id BETWEEN 10 AND 20 FOR UPDATE;
```

#### Next-Key Lock

- **정의**: Record Lock + Gap Lock의 조합
- **특징**: InnoDB의 기본 Lock 방식 (REPEATABLE READ)

| Lock 종류 | 잠금 범위 | Phantom Read 방지 |
|-----------|----------|------------------|
| Record Lock | 해당 레코드만 | X |
| Gap Lock | 레코드 사이 간격 | O |
| Next-Key Lock | 레코드 + 앞쪽 간격 | O |

#### Insert Intention Lock

- **정의**: INSERT 전에 획득하는 특수한 Gap Lock
- **특징**: 같은 Gap 내 서로 다른 위치에 INSERT하는 경우 서로 대기하지 않음

#### Auto-Increment Lock

- **정의**: AUTO_INCREMENT 컬럼 값 생성 시 사용되는 테이블 레벨 Lock
- **모드**: `innodb_autoinc_lock_mode` 설정으로 제어

| 모드 | 값 | 설명 |
|------|---|------|
| Traditional | 0 | 모든 INSERT에 테이블 Lock |
| Consecutive | 1 | 단순 INSERT는 Mutex, Bulk INSERT는 테이블 Lock |
| Interleaved | 2 | Lock 없이 Mutex만 사용 (가장 빠름) |

---

## 3. 심화: Gap Lock, Next-Key Lock

### Gap Lock

#### 동작 원리

```
Gap Lock: 인덱스 레코드 사이의 "간격"을 잠금
목적: Phantom Read 방지

예시: id가 10, 20인 레코드가 있을 때
     (-∞, 10), (10, 20), (20, +∞) 간격 존재
```

#### 동작 예시

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

#### Gap Lock 발생 조건

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

### Next-Key Lock

#### 동작 원리

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

#### 잠금 범위 시각화

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

#### 잠금 에스컬레이션 예시

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

## 4. 실무 예제 및 트러블슈팅

### 데드락 발생 조건

#### 데드락 4가지 필요조건

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

#### 데드락 예시 1: 기본 패턴

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

#### 데드락 예시 2: Gap Lock 데드락

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

#### InnoDB 데드락 처리

```sql
-- InnoDB는 자동으로 데드락 감지
-- 비용이 적은 트랜잭션을 희생양으로 선택하여 롤백

-- 에러 메시지
ERROR 1213 (40001): Deadlock found when trying to get lock;
try restarting transaction
```

### 락 모니터링

#### 현재 락 상태 확인

```sql
-- MySQL 8.0+
SELECT * FROM performance_schema.data_locks;
SELECT * FROM performance_schema.data_lock_waits;

-- MySQL 5.7 이하
SELECT * FROM information_schema.INNODB_LOCK_WAITS;
SELECT * FROM information_schema.INNODB_LOCKS;

-- 현재 트랜잭션
SELECT * FROM information_schema.INNODB_TRX;
```

#### InnoDB 상태 확인

```sql
SHOW ENGINE INNODB STATUS\G

-- 출력에서 확인할 섹션:
-- TRANSACTIONS: 현재 트랜잭션 목록
-- LATEST DETECTED DEADLOCK: 최근 데드락 정보
-- SEMAPHORES: 락 대기 정보
```

#### 락 타임아웃 설정

```sql
-- 락 대기 타임아웃 (기본 50초)
SHOW VARIABLES LIKE 'innodb_lock_wait_timeout';
SET innodb_lock_wait_timeout = 10;

-- 타임아웃 시 에러
ERROR 1205 (HY000): Lock wait timeout exceeded;
try restarting transaction
```

### 락 최적화 전략

#### 데드락 방지

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

#### 락 범위 최소화

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

#### 낙관적 락 활용

```sql
-- 버전 컬럼 사용
UPDATE orders
SET status = 'PROCESSING', version = version + 1
WHERE id = 1 AND version = 5;

-- 영향받은 행이 0이면 다른 트랜잭션이 먼저 수정
-- 재시도 또는 충돌 처리
```

#### 재시도 로직

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

## 전체 요약

| Lock 종류 | 잠금 대상 | 주요 용도 |
|-----------|----------|----------|
| Shared Lock | 레코드 | 읽기 보호 |
| Exclusive Lock | 레코드 | 쓰기 보호 |
| Intention Lock | 테이블 (의도 표시) | 행 잠금 의도 선언 |
| Record Lock | 인덱스 레코드 | 특정 행 보호 |
| Gap Lock | 인덱스 간격 | Phantom Read 방지 |
| Next-Key Lock | 레코드 + 간격 | InnoDB 기본 Lock 방식 |
| Insert Intention Lock | 간격 | INSERT 동시성 향상 |
| Auto-Increment Lock | 테이블 | AUTO_INCREMENT 값 생성 |

| 데드락 조건 | 설명 |
|------------|------|
| 상호 배제 | 동시 접근 불가 |
| 점유 대기 | 점유하고 대기 |
| 비선점 | 강제 해제 불가 |
| 순환 대기 | 순환 형태 대기 |

---

*마지막 업데이트: 2026년 02월*

# MySQL 트랜잭션 격리 수준

ACID 원칙과 4가지 격리 수준, 발생 가능한 이상 현상을 정리합니다.

## 목차

1. [ACID 원칙](#1-acid-원칙)
2. [격리 수준 개요](#2-격리-수준-개요)
3. [이상 현상 (Anomalies)](#3-이상-현상-anomalies)
4. [각 격리 수준 상세](#4-각-격리-수준-상세)
5. [MySQL InnoDB 특성](#5-mysql-innodb-특성)
6. [격리 수준 선택 가이드](#6-격리-수준-선택-가이드)

---

## 1. ACID 원칙

### Atomicity (원자성)

```
트랜잭션의 모든 연산은 전부 성공하거나 전부 실패

BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
UPDATE accounts SET balance = balance + 100 WHERE id = 2;
COMMIT;  -- 둘 다 성공

-- 중간에 실패하면 둘 다 롤백
```

### Consistency (일관성)

```
트랜잭션 전후로 데이터베이스는 일관된 상태 유지

예: 계좌 이체
- 트랜잭션 전: A + B = 1000
- 트랜잭션 후: A + B = 1000 (변함없음)
```

### Isolation (격리성)

```
동시 실행되는 트랜잭션은 서로 영향을 주지 않음
격리 수준에 따라 다른 트랜잭션의 변경을 얼마나 볼 수 있는지 결정
```

### Durability (지속성)

```
커밋된 트랜잭션은 영구적으로 저장
시스템 장애가 발생해도 데이터 유지 (Redo Log 활용)
```

---

## 2. 격리 수준 개요

### 4가지 격리 수준

```
┌─────────────────────────────────────────────────────────────┐
│   격리 수준         │ Dirty │ Non-Rep │ Phantom │ 성능     │
│                     │ Read  │ Read    │ Read    │          │
├─────────────────────────────────────────────────────────────┤
│ READ UNCOMMITTED    │   O   │    O    │    O    │ 최고     │
│ READ COMMITTED      │   X   │    O    │    O    │ 높음     │
│ REPEATABLE READ     │   X   │    X    │    O*   │ 보통     │
│ SERIALIZABLE        │   X   │    X    │    X    │ 낮음     │
└─────────────────────────────────────────────────────────────┘

* InnoDB의 REPEATABLE READ는 Gap Lock으로 Phantom Read 방지
```

### 설정 방법

```sql
-- 현재 격리 수준 확인
SELECT @@transaction_isolation;

-- 세션 격리 수준 변경
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;

-- 글로벌 격리 수준 변경 (재시작 후 적용)
SET GLOBAL TRANSACTION ISOLATION LEVEL REPEATABLE READ;

-- my.cnf 설정
[mysqld]
transaction-isolation = REPEATABLE-READ
```

---

## 3. 이상 현상 (Anomalies)

### Dirty Read

```sql
-- 커밋되지 않은 데이터를 읽음

-- 트랜잭션 A
BEGIN;
UPDATE users SET name = 'Kim' WHERE id = 1;  -- 아직 커밋 안 함

-- 트랜잭션 B
SELECT name FROM users WHERE id = 1;  -- 'Kim' 읽음

-- 트랜잭션 A
ROLLBACK;  -- 롤백!

-- 트랜잭션 B가 읽은 'Kim'은 존재하지 않는 데이터
```

### Non-Repeatable Read

```sql
-- 같은 쿼리를 두 번 실행했을 때 다른 결과

-- 트랜잭션 A
BEGIN;
SELECT balance FROM accounts WHERE id = 1;  -- 1000

-- 트랜잭션 B
UPDATE accounts SET balance = 500 WHERE id = 1;
COMMIT;

-- 트랜잭션 A
SELECT balance FROM accounts WHERE id = 1;  -- 500 (다른 값!)
COMMIT;
```

### Phantom Read

```sql
-- 같은 조건으로 조회했을 때 행 개수가 다름

-- 트랜잭션 A
BEGIN;
SELECT COUNT(*) FROM users WHERE age > 20;  -- 5

-- 트랜잭션 B
INSERT INTO users (age) VALUES (25);
COMMIT;

-- 트랜잭션 A
SELECT COUNT(*) FROM users WHERE age > 20;  -- 6 (유령 행!)
COMMIT;
```

---

## 4. 각 격리 수준 상세

### READ UNCOMMITTED

```sql
-- 가장 낮은 격리 수준
-- 커밋 안 된 데이터도 읽음 (Dirty Read 허용)
-- 거의 사용하지 않음

SET SESSION TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
```

### READ COMMITTED

```sql
-- 커밋된 데이터만 읽음
-- Oracle, PostgreSQL 기본값
-- 같은 트랜잭션 내에서 다른 결과 가능 (Non-Repeatable Read)

SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;

-- 동작 방식:
-- 각 SELECT 시점의 스냅샷을 읽음
-- 매 쿼리마다 새로운 스냅샷 생성
```

### REPEATABLE READ

```sql
-- MySQL InnoDB 기본값
-- 트랜잭션 시작 시점의 스냅샷을 계속 사용
-- 같은 쿼리는 같은 결과 보장

SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;

-- InnoDB 특성:
-- MVCC로 Non-Repeatable Read 방지
-- Gap Lock으로 Phantom Read도 방지 (대부분의 경우)
```

### SERIALIZABLE

```sql
-- 가장 높은 격리 수준
-- 모든 SELECT가 암묵적으로 SELECT ... FOR SHARE
-- 완전한 직렬화 (동시성 낮음)

SET SESSION TRANSACTION ISOLATION LEVEL SERIALIZABLE;

-- 데드락 위험 높음
-- 성능 저하 심함
-- 꼭 필요한 경우에만 사용
```

---

## 5. MySQL InnoDB 특성

### MVCC (Multi-Version Concurrency Control)

```
각 트랜잭션은 시작 시점의 데이터 스냅샷을 봄

트랜잭션 시작: trx_id = 100
현재 데이터: name = 'Kim', trx_id = 150

→ trx_id 150 > 100 이므로 Undo Log에서 이전 버전 읽음

┌───────────────────────────────────────────────────────┐
│  현재 데이터                                          │
│  name = 'Kim', trx_id = 150                          │
│       ↓ roll_pointer                                 │
│  Undo Log: name = 'Lee', trx_id = 80                 │
│       ↓ roll_pointer                                 │
│  Undo Log: name = 'Park', trx_id = 50                │
└───────────────────────────────────────────────────────┘

트랜잭션 100은 trx_id 80의 'Lee'를 읽음
```

### Gap Lock (REPEATABLE READ)

```sql
-- Gap Lock: 레코드 사이의 간격을 잠금
-- Phantom Read 방지

-- id가 10, 20인 레코드가 있을 때
SELECT * FROM table WHERE id BETWEEN 10 AND 20 FOR UPDATE;

-- Gap Lock 범위:
-- (10, 20) 사이의 간격
-- 다른 트랜잭션이 id=15를 INSERT 불가
```

### Next-Key Lock

```sql
-- Next-Key Lock = Record Lock + Gap Lock
-- InnoDB의 기본 잠금 방식

-- id = 10인 레코드에 Next-Key Lock
-- = id = 10 Record Lock + id < 10 Gap Lock
```

---

## 6. 격리 수준 선택 가이드

### 사용 사례별 권장

```
READ COMMITTED:
- 대부분의 웹 애플리케이션
- 동시성이 중요한 경우
- 긴 트랜잭션이 없는 경우

REPEATABLE READ:
- 보고서 생성 (일관된 데이터 필요)
- 금융 거래
- 데이터 정합성이 중요한 경우

SERIALIZABLE:
- 거의 사용 안 함
- 극도로 정확한 데이터 필요 시
```

### 성능과 일관성 트레이드오프

```
격리 수준 높음 → 일관성 높음, 성능 낮음, 데드락 위험 높음
격리 수준 낮음 → 일관성 낮음, 성능 높음, 데드락 위험 낮음

대부분의 경우:
- MySQL: REPEATABLE READ (기본값) 사용
- 특별한 이유 있으면 READ COMMITTED 고려
```

### 명시적 잠금 활용

```sql
-- 격리 수준과 별개로 필요시 명시적 잠금

-- 읽기 잠금 (다른 트랜잭션 쓰기 차단)
SELECT * FROM accounts WHERE id = 1 FOR SHARE;

-- 쓰기 잠금 (다른 트랜잭션 읽기/쓰기 차단)
SELECT * FROM accounts WHERE id = 1 FOR UPDATE;

-- NOWAIT: 잠금 대기 없이 즉시 실패
SELECT * FROM accounts WHERE id = 1 FOR UPDATE NOWAIT;

-- SKIP LOCKED: 잠긴 행 건너뛰기
SELECT * FROM accounts WHERE status = 'PENDING'
FOR UPDATE SKIP LOCKED LIMIT 10;
```

---

## 핵심 정리

| 격리 수준 | 특징 | 사용 시점 |
|----------|------|----------|
| READ UNCOMMITTED | Dirty Read 허용 | 거의 안 씀 |
| READ COMMITTED | 커밋된 것만 읽음 | 일반 웹앱 |
| REPEATABLE READ | 트랜잭션 내 일관성 | MySQL 기본 |
| SERIALIZABLE | 완전 직렬화 | 극히 드뭄 |

| 이상 현상 | 설명 |
|----------|------|
| Dirty Read | 커밋 안 된 데이터 읽음 |
| Non-Repeatable Read | 같은 쿼리 다른 결과 |
| Phantom Read | 행 개수 다름 |

---

*마지막 업데이트: 2025년 01월*

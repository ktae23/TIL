# MySQL 면접 핵심 질문 정리

5년차 백엔드 개발자 면접에서 자주 등장하는 MySQL 핵심 질문과 답변을 정리합니다.

## 목차

1. [Lock의 종류와 특징](#1-lock의-종류와-특징)
2. [MVCC (Multi-Version Concurrency Control)](#2-mvcc-multi-version-concurrency-control)
3. [Redo Log와 Undo Log](#3-redo-log와-undo-log)
4. [Replication 구조](#4-replication-구조)
5. [인덱스 관련 질문](#5-인덱스-관련-질문)
6. [트랜잭션 격리 수준](#6-트랜잭션-격리-수준)

---

## 1. Lock의 종류와 특징

### Q: MySQL InnoDB의 Lock 종류를 설명해주세요.

**Shared Lock (S Lock, 공유 잠금)**
- 읽기 작업에 사용
- 여러 트랜잭션이 동시에 S Lock 획득 가능
- S Lock이 걸린 레코드에 X Lock 획득 불가

```sql
-- S Lock 획득
SELECT * FROM users WHERE id = 1 LOCK IN SHARE MODE;
-- MySQL 8.0+
SELECT * FROM users WHERE id = 1 FOR SHARE;
```

**Exclusive Lock (X Lock, 배타 잠금)**
- 쓰기 작업에 사용
- X Lock이 걸리면 다른 모든 Lock 획득 불가

```sql
-- X Lock 획득
SELECT * FROM users WHERE id = 1 FOR UPDATE;
```

**Record Lock**
- 인덱스 레코드에 거는 Lock
- 클러스터드 인덱스의 실제 레코드를 잠금

**Gap Lock**
- 인덱스 레코드 사이의 간격을 잠금
- Phantom Read 방지 목적
- REPEATABLE READ 이상에서 동작

```sql
-- id가 10, 20인 레코드가 있을 때
SELECT * FROM users WHERE id BETWEEN 10 AND 20 FOR UPDATE;
-- (10, 20) 사이의 Gap에 Lock 발생
```

**Next-Key Lock**
- Record Lock + Gap Lock의 조합
- InnoDB의 기본 잠금 방식

### Q: 데드락이 발생하는 조건과 해결 방법은?

**데드락 4가지 조건 (모두 충족시 발생)**
1. **상호 배제**: 자원은 한 번에 하나의 트랜잭션만 사용
2. **점유 대기**: 자원을 점유한 채 다른 자원 대기
3. **비선점**: 다른 트랜잭션의 자원을 강제로 빼앗을 수 없음
4. **순환 대기**: 트랜잭션들이 순환 형태로 서로의 자원 대기

**데드락 예시**
```sql
-- 트랜잭션 A
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 1;  -- Lock on id=1
UPDATE accounts SET balance = balance + 100 WHERE id = 2;  -- Wait for id=2

-- 트랜잭션 B (동시 실행)
BEGIN;
UPDATE accounts SET balance = balance - 50 WHERE id = 2;   -- Lock on id=2
UPDATE accounts SET balance = balance + 50 WHERE id = 1;   -- Wait for id=1 → 데드락!
```

**해결 방법**
```sql
-- 1. 잠금 순서 통일 (항상 작은 id부터)
UPDATE accounts SET balance = balance - 100 WHERE id = 1;
UPDATE accounts SET balance = balance + 100 WHERE id = 2;

-- 2. 짧은 트랜잭션 유지
-- 3. 인덱스 활용으로 Lock 범위 최소화
-- 4. 데드락 감지 확인
SHOW ENGINE INNODB STATUS;
```

---

## 2. MVCC (Multi-Version Concurrency Control)

### Q: MVCC가 무엇이고 왜 필요한가요?

**정의**: Lock 없이 읽기 일관성을 제공하는 동시성 제어 기법

**동작 원리**
1. 데이터 변경 시 이전 버전을 Undo 로그에 보관
2. 각 트랜잭션은 시작 시점의 스냅샷을 읽음
3. 트랜잭션 ID를 기반으로 가시성 판단

```
[레코드 상태]
현재값: name='Kim', trx_id=100

[Undo Log]
이전값: name='Lee', trx_id=50

트랜잭션 A (trx_id=80): name='Lee' 읽음 (trx_id 50 < 80)
트랜잭션 B (trx_id=120): name='Kim' 읽음 (trx_id 100 < 120)
```

**장점**
- 읽기 작업이 쓰기를 차단하지 않음
- 쓰기 작업이 읽기를 차단하지 않음
- 높은 동시성 처리 가능

### Q: Undo Log의 버전 체인은 어떻게 동작하나요?

```
현재 레코드 (trx_id=100, name='Park')
    ↓ roll_pointer
Undo Log (trx_id=80, name='Kim')
    ↓ roll_pointer
Undo Log (trx_id=50, name='Lee')
    ↓ roll_pointer
NULL (최초 상태)
```

각 트랜잭션은 자신의 시작 시점보다 작은 trx_id를 가진 버전 중 가장 최신을 읽음

---

## 3. Redo Log와 Undo Log

### Q: Redo Log와 Undo Log의 차이점은?

| 구분 | Redo Log | Undo Log |
|------|----------|----------|
| 목적 | 장애 복구 (Durability) | 트랜잭션 롤백, MVCC |
| 내용 | 변경된 데이터 (물리적 로그) | 변경 전 데이터 (논리적 로그) |
| 저장 위치 | 별도 로그 파일 (ib_logfile) | 시스템 테이블스페이스 |
| 기록 시점 | 변경 발생 시 즉시 | 변경 발생 시 |
| 삭제 시점 | 체크포인트 이후 | 트랜잭션 종료 후 Purge |

### Q: Write-Ahead Logging(WAL)을 설명해주세요.

**원칙**: 데이터 페이지 변경 전에 반드시 로그를 먼저 기록

```
[트랜잭션 커밋 순서]
1. Redo Log Buffer에 변경 내용 기록
2. Redo Log를 디스크에 flush (fsync)
3. 클라이언트에 커밋 성공 응답
4. 나중에 Buffer Pool의 Dirty Page를 디스크에 기록

[장애 복구 시]
1. Redo Log 읽기
2. 체크포인트 이후의 변경 사항 재적용 (Redo)
3. 미완료 트랜잭션 롤백 (Undo)
```

**Double Write Buffer**
```
Page 쓰기 중 장애 → Partial Write 문제
해결: Page를 Double Write Buffer에 먼저 쓰고,
      그 다음 실제 위치에 기록
```

---

## 4. Replication 구조

### Q: MySQL Replication 동작 방식을 설명해주세요.

**구성 요소**
```
[Master]                      [Slave]
    │                             │
Binary Log ──────────────→ Relay Log
    │         (I/O Thread)        │
    │                        SQL Thread
    │                             │
    └──────────────────────→ 데이터 적용
```

**복제 방식**

| 방식 | 설명 | 장단점 |
|------|------|--------|
| 비동기 복제 | Master는 Slave 확인 없이 커밋 | 빠름, 데이터 유실 가능 |
| 반동기 복제 | 최소 1개 Slave 수신 확인 후 커밋 | 약간의 지연, 유실 최소화 |
| 그룹 복제 | 다수 노드 합의 후 커밋 | 강한 일관성, 복잡함 |

**Binary Log 포맷**
```sql
-- Statement 기반: SQL문 자체를 로깅
-- Row 기반: 변경된 행 데이터를 로깅
-- Mixed: 상황에 따라 자동 선택

SET binlog_format = 'ROW';  -- 권장
```

### Q: Replication Lag 대응 방법은?

```sql
-- 1. Slave 지연 상태 확인
SHOW SLAVE STATUS\G
-- Seconds_Behind_Master 값 확인

-- 2. Read/Write 분리 시 주의점
-- 쓰기 직후 읽기는 Master에서 수행
-- 중요 조회는 Master 사용

-- 3. 병렬 복제 설정 (MySQL 5.7+)
slave_parallel_workers = 4
slave_parallel_type = LOGICAL_CLOCK
```

---

## 5. 인덱스 관련 질문

### Q: 클러스터드 인덱스와 세컨더리 인덱스의 차이는?

**클러스터드 인덱스 (Clustered Index)**
- 테이블당 1개만 존재 (보통 PK)
- 리프 노드에 실제 데이터 저장
- 데이터가 인덱스 순서대로 물리적 정렬

**세컨더리 인덱스 (Secondary Index)**
- 테이블당 여러 개 가능
- 리프 노드에 PK 값 저장
- 데이터 조회 시 클러스터드 인덱스 한 번 더 탐색 필요

```
[세컨더리 인덱스로 조회 시]
Secondary Index → PK 값 획득 → Clustered Index → 실제 데이터

-- 커버링 인덱스로 추가 조회 방지
CREATE INDEX idx_name_email ON users(name, email);
SELECT name, email FROM users WHERE name = 'Kim';  -- 인덱스만으로 응답
```

### Q: 복합 인덱스 설계 시 컬럼 순서는 어떻게 정하나요?

**원칙**
1. **카디널리티**: 높은 것부터 (선택도가 좋은 것)
2. **등호 조건**: 등호(=) 조건 컬럼을 앞에
3. **정렬**: ORDER BY 컬럼 고려

```sql
-- 쿼리 패턴
SELECT * FROM orders
WHERE status = 'PAID'
AND user_id = 100
AND created_at > '2024-01-01';

-- 권장 인덱스 (등호 조건들을 앞에)
CREATE INDEX idx_orders ON orders(status, user_id, created_at);

-- 만약 user_id가 더 선택적이라면
CREATE INDEX idx_orders ON orders(user_id, status, created_at);
```

---

## 6. 트랜잭션 격리 수준

### Q: 각 격리 수준에서 발생하는 문제를 설명해주세요.

| 격리 수준 | Dirty Read | Non-Repeatable Read | Phantom Read |
|-----------|------------|---------------------|--------------|
| READ UNCOMMITTED | O | O | O |
| READ COMMITTED | X | O | O |
| REPEATABLE READ | X | X | O (InnoDB는 X) |
| SERIALIZABLE | X | X | X |

**InnoDB의 REPEATABLE READ 특수성**
- Gap Lock으로 Phantom Read 방지
- 실질적으로 SERIALIZABLE에 가까운 일관성 제공

```sql
-- 현재 격리 수준 확인
SELECT @@transaction_isolation;

-- 세션 격리 수준 변경
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
```

### Q: Dirty Read, Non-Repeatable Read, Phantom Read를 설명해주세요.

**Dirty Read**: 커밋되지 않은 데이터를 읽음
```sql
-- TX1: UPDATE users SET name = 'Kim' WHERE id = 1; (커밋 안함)
-- TX2: SELECT name FROM users WHERE id = 1;  → 'Kim' 읽음
-- TX1: ROLLBACK;
-- TX2가 읽은 'Kim'은 존재하지 않는 데이터
```

**Non-Repeatable Read**: 같은 쿼리가 다른 결과 반환
```sql
-- TX1: SELECT name FROM users WHERE id = 1;  → 'Lee'
-- TX2: UPDATE users SET name = 'Kim' WHERE id = 1; COMMIT;
-- TX1: SELECT name FROM users WHERE id = 1;  → 'Kim' (다른 결과!)
```

**Phantom Read**: 같은 조건인데 행 개수가 다름
```sql
-- TX1: SELECT COUNT(*) FROM users WHERE age > 20;  → 5
-- TX2: INSERT INTO users (age) VALUES (25); COMMIT;
-- TX1: SELECT COUNT(*) FROM users WHERE age > 20;  → 6 (유령 행!)
```

---

## 핵심 정리

| 주제 | 핵심 키워드 |
|------|-------------|
| Lock | S/X Lock, Gap Lock, Next-Key Lock, 데드락 |
| MVCC | Undo Log, 트랜잭션 ID, 버전 체인, 스냅샷 |
| 로그 | WAL, Redo(복구)/Undo(롤백), Double Write |
| 복제 | Binary Log, Relay Log, 비동기/반동기, Lag |
| 인덱스 | 클러스터드/세컨더리, 커버링 인덱스, 복합 인덱스 순서 |
| 격리 수준 | READ COMMITTED, REPEATABLE READ, 이상 현상 3가지 |

---

*마지막 업데이트: 2026년 01월*

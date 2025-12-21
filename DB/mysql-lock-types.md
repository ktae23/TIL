# MySQL Lock 종류

MySQL에서 동시성 제어를 위해 사용되는 다양한 Lock의 종류와 특징을 정리한다.

## 목차

- [Lock의 기본 개념](#lock의-기본-개념)
- [Lock 종류 비교](#lock-종류-비교)
- [Lock 레벨별 분류](#lock-레벨별-분류)
- [InnoDB Lock 종류](#innodb-lock-종류)
- [Lock 확인 방법](#lock-확인-방법)

---

## Lock의 기본 개념

Lock은 여러 트랜잭션이 동시에 같은 데이터에 접근할 때 **데이터 일관성**을 보장하기 위한 메커니즘이다.

---

## Lock 종류 비교

### Shared Lock vs Exclusive Lock

| 구분 | Shared Lock (S Lock) | Exclusive Lock (X Lock) |
|------|---------------------|------------------------|
| **용도** | 읽기 작업 | 쓰기 작업 |
| **다른 S Lock과 호환** | O (호환) | X (비호환) |
| **다른 X Lock과 호환** | X (비호환) | X (비호환) |
| **발생 시점** | `SELECT ... FOR SHARE` | `SELECT ... FOR UPDATE`, `UPDATE`, `DELETE` |
| **특징** | 여러 트랜잭션이 동시에 읽기 가능 | 하나의 트랜잭션만 쓰기 가능 |

### Lock 호환성 매트릭스

|  | S Lock | X Lock |
|--|--------|--------|
| **S Lock** | 호환 | 비호환 |
| **X Lock** | 비호환 | 비호환 |

---

## Lock 레벨별 분류

| Lock 레벨 | 설명 | 장점 | 단점 |
|-----------|------|------|------|
| **Global Lock** | 데이터베이스 전체에 Lock | 단순함 | 동시성 매우 낮음 |
| **Table Lock** | 테이블 단위 Lock | 구현 단순, 오버헤드 적음 | 동시성 낮음 |
| **Row Lock** | 행 단위 Lock | 높은 동시성 | 오버헤드 큼, Deadlock 가능성 |

---

## InnoDB Lock 종류

### Record Lock

- **정의**: 인덱스 레코드에 거는 Lock
- **특징**: 테이블에 인덱스가 없어도 숨겨진 Clustered Index를 사용

```sql
-- Record Lock 발생 예시
SELECT * FROM users WHERE id = 1 FOR UPDATE;
```

### Gap Lock

- **정의**: 인덱스 레코드 사이의 간격(Gap)에 거는 Lock
- **용도**: Phantom Read 방지
- **특징**: 범위 내 새로운 레코드 삽입 방지

```sql
-- Gap Lock 발생 예시 (id가 10~20 사이에 Lock)
SELECT * FROM users WHERE id BETWEEN 10 AND 20 FOR UPDATE;
```

### Next-Key Lock

- **정의**: Record Lock + Gap Lock의 조합
- **특징**: InnoDB의 기본 Lock 방식 (REPEATABLE READ)

| Lock 종류 | 잠금 범위 | Phantom Read 방지 |
|-----------|----------|------------------|
| Record Lock | 해당 레코드만 | X |
| Gap Lock | 레코드 사이 간격 | O |
| Next-Key Lock | 레코드 + 앞쪽 간격 | O |

### Insert Intention Lock

- **정의**: INSERT 전에 획득하는 특수한 Gap Lock
- **특징**: 같은 Gap 내 서로 다른 위치에 INSERT하는 경우 서로 대기하지 않음

### Auto-Increment Lock

- **정의**: AUTO_INCREMENT 컬럼 값 생성 시 사용되는 테이블 레벨 Lock
- **모드**: `innodb_autoinc_lock_mode` 설정으로 제어

| 모드 | 값 | 설명 |
|------|---|------|
| Traditional | 0 | 모든 INSERT에 테이블 Lock |
| Consecutive | 1 | 단순 INSERT는 Mutex, Bulk INSERT는 테이블 Lock |
| Interleaved | 2 | Lock 없이 Mutex만 사용 (가장 빠름) |

---

## Lock 확인 방법

### 현재 Lock 상태 확인

```sql
-- InnoDB Lock 정보 조회
SELECT * FROM performance_schema.data_locks;

-- Lock 대기 정보 조회
SELECT * FROM performance_schema.data_lock_waits;

-- InnoDB 상태 확인
SHOW ENGINE INNODB STATUS;
```

### Deadlock 로그 확인

```sql
-- 최근 Deadlock 정보
SHOW ENGINE INNODB STATUS\G
```

---

## 정리

| Lock 종류 | 잠금 대상 | 주요 용도 |
|-----------|----------|----------|
| Shared Lock | 레코드 | 읽기 보호 |
| Exclusive Lock | 레코드 | 쓰기 보호 |
| Record Lock | 인덱스 레코드 | 특정 행 보호 |
| Gap Lock | 인덱스 간격 | Phantom Read 방지 |
| Next-Key Lock | 레코드 + 간격 | 기본 Lock 방식 |
| Insert Intention Lock | 간격 | INSERT 동시성 향상 |
| Auto-Increment Lock | 테이블 | AUTO_INCREMENT 값 생성 |

---

*마지막 업데이트: 2025년 12월*

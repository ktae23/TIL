# MySQL 실행 계획 (EXPLAIN) 분석

MySQL EXPLAIN 명령어를 사용하여 쿼리 실행 계획을 분석하는 방법을 정리합니다.

## 목차

1. [EXPLAIN 기본 사용법](#1-explain-기본-사용법)
2. [주요 컬럼 해석](#2-주요-컬럼-해석)
3. [type 컬럼 상세](#3-type-컬럼-상세)
4. [Extra 컬럼 상세](#4-extra-컬럼-상세)
5. [실행 계획 개선 예시](#5-실행-계획-개선-예시)
6. [EXPLAIN ANALYZE](#6-explain-analyze)

---

## 1. EXPLAIN 기본 사용법

### 기본 형식

```sql
-- 기본 EXPLAIN
EXPLAIN SELECT * FROM users WHERE email = 'test@example.com';

-- 포맷 지정
EXPLAIN FORMAT=JSON SELECT * FROM users WHERE email = 'test@example.com';
EXPLAIN FORMAT=TREE SELECT * FROM users WHERE email = 'test@example.com';

-- 실제 실행 통계 포함 (MySQL 8.0.18+)
EXPLAIN ANALYZE SELECT * FROM users WHERE email = 'test@example.com';
```

### 결과 예시

```
+----+-------------+-------+------+---------------+------+---------+------+------+-------------+
| id | select_type | table | type | possible_keys | key  | key_len | ref  | rows | Extra       |
+----+-------------+-------+------+---------------+------+---------+------+------+-------------+
|  1 | SIMPLE      | users | ref  | idx_email     | idx  | 767     | const|    1 | Using index |
+----+-------------+-------+------+---------------+------+---------+------+------+-------------+
```

---

## 2. 주요 컬럼 해석

### id

```sql
-- 쿼리 실행 순서 (같은 id면 위에서 아래로)
-- 서브쿼리가 있으면 다른 id 부여

EXPLAIN
SELECT * FROM orders o
WHERE o.user_id IN (SELECT id FROM users WHERE status = 'ACTIVE');

-- id=1: users 서브쿼리
-- id=2: orders 메인쿼리 (또는 반대)
```

### select_type

| 값 | 설명 |
|----|------|
| SIMPLE | 단순 SELECT (서브쿼리/UNION 없음) |
| PRIMARY | 가장 바깥 SELECT |
| SUBQUERY | SELECT 절의 서브쿼리 |
| DERIVED | FROM 절의 서브쿼리 (파생 테이블) |
| UNION | UNION의 두 번째 이후 SELECT |
| DEPENDENT SUBQUERY | 외부 쿼리에 의존하는 서브쿼리 |

### table

```sql
-- 접근하는 테이블 이름
-- <derived2>: id=2의 파생 테이블
-- <subquery2>: id=2의 서브쿼리 결과
```

### possible_keys / key

```sql
-- possible_keys: 사용 가능한 인덱스 목록
-- key: 실제로 선택된 인덱스

-- key가 NULL이면 풀 테이블 스캔!
```

### key_len

```sql
-- 사용된 인덱스의 바이트 수
-- 복합 인덱스에서 몇 개의 컬럼이 사용되었는지 판단 가능

-- 예: INT(4) + VARCHAR(255) UTF8MB4 = 4 + (255*4+2) = 1026
-- 실제 key_len이 4면 첫 번째 컬럼만 사용
```

### rows

```sql
-- 예상 조회 행 수 (통계 기반 추정)
-- 실제와 다를 수 있음
-- 큰 값이면 최적화 필요
```

### filtered

```sql
-- 테이블 조건에 의해 필터링될 행의 비율 (%)
-- rows * filtered / 100 = 실제 결과 행 수 추정
```

---

## 3. type 컬럼 상세

### 성능 순서 (좋음 → 나쁨)

```
system > const > eq_ref > ref > range > index > ALL
```

### 각 type 설명

```sql
-- system: 테이블에 1행만 있음 (시스템 테이블)

-- const: PK 또는 Unique 인덱스로 1건 조회
SELECT * FROM users WHERE id = 1;

-- eq_ref: JOIN에서 PK/Unique 사용
SELECT * FROM orders o
JOIN users u ON o.user_id = u.id;  -- users가 eq_ref

-- ref: 인덱스 동등 조건 (여러 행 가능)
SELECT * FROM orders WHERE user_id = 100;

-- range: 인덱스 범위 스캔
SELECT * FROM orders WHERE created_at BETWEEN '2024-01-01' AND '2024-01-31';
SELECT * FROM orders WHERE id IN (1, 2, 3);

-- index: 인덱스 풀 스캔 (ALL보다는 나음)
SELECT email FROM users;  -- email에 인덱스 있으면

-- ALL: 테이블 풀 스캔 (최악!)
SELECT * FROM users WHERE name LIKE '%kim%';
```

### 개선이 필요한 신호

```
❌ ALL: 인덱스 추가 검토
❌ index: 커버링 인덱스 또는 조건 추가 검토
⚠️ range: 범위가 넓으면 주의
✅ ref, eq_ref, const: 양호
```

---

## 4. Extra 컬럼 상세

### 자주 보이는 값

```sql
-- Using index (좋음!)
-- 커버링 인덱스 사용, 테이블 접근 불필요
SELECT email FROM users WHERE email = 'test@example.com';

-- Using where
-- WHERE 조건으로 필터링
-- 인덱스로 필터링 못한 부분이 있음을 의미

-- Using index condition
-- Index Condition Pushdown (ICP)
-- 인덱스 레벨에서 조건 필터링

-- Using temporary (주의!)
-- 임시 테이블 사용
-- GROUP BY, ORDER BY, DISTINCT 등에서 발생
SELECT DISTINCT status FROM orders;

-- Using filesort (주의!)
-- 정렬을 위해 추가 작업 필요
-- ORDER BY가 인덱스를 활용하지 못함
SELECT * FROM orders ORDER BY amount DESC;

-- Using join buffer
-- JOIN시 버퍼 사용
-- 인덱스가 없는 JOIN 시 발생
```

### 개선 포인트

```sql
-- Using filesort 개선
-- 정렬 컬럼을 인덱스에 포함
CREATE INDEX idx_status_created ON orders(status, created_at);
SELECT * FROM orders WHERE status = 'PAID' ORDER BY created_at;

-- Using temporary 개선
-- GROUP BY 컬럼에 인덱스
CREATE INDEX idx_user_id ON orders(user_id);
SELECT user_id, COUNT(*) FROM orders GROUP BY user_id;
```

---

## 5. 실행 계획 개선 예시

### 예시 1: 인덱스 미사용

```sql
-- 문제 쿼리
EXPLAIN SELECT * FROM orders WHERE DATE(created_at) = '2024-01-15';
-- type: ALL (풀 스캔!)
-- Extra: Using where

-- 원인: 함수 사용으로 인덱스 사용 불가

-- 개선
EXPLAIN SELECT * FROM orders
WHERE created_at >= '2024-01-15' AND created_at < '2024-01-16';
-- type: range
-- key: idx_created_at
```

### 예시 2: 복합 인덱스 순서

```sql
-- 인덱스: (status, user_id)

-- 문제 쿼리
EXPLAIN SELECT * FROM orders WHERE user_id = 100;
-- type: ALL (첫 번째 컬럼 조건 없음)

-- 개선 1: 조건 추가
EXPLAIN SELECT * FROM orders WHERE status = 'PAID' AND user_id = 100;
-- type: ref

-- 개선 2: 인덱스 순서 변경
CREATE INDEX idx_user_status ON orders(user_id, status);
```

### 예시 3: 서브쿼리 최적화

```sql
-- 문제: 상관 서브쿼리
EXPLAIN SELECT *
FROM orders o
WHERE o.amount > (SELECT AVG(amount) FROM orders WHERE user_id = o.user_id);
-- select_type: DEPENDENT SUBQUERY (매 행마다 서브쿼리 실행)

-- 개선: JOIN으로 변환
EXPLAIN SELECT o.*
FROM orders o
JOIN (SELECT user_id, AVG(amount) as avg_amount
      FROM orders GROUP BY user_id) avg_t
ON o.user_id = avg_t.user_id AND o.amount > avg_t.avg_amount;
```

---

## 6. EXPLAIN ANALYZE

### 실제 실행 통계 확인 (MySQL 8.0.18+)

```sql
EXPLAIN ANALYZE
SELECT * FROM orders WHERE status = 'PAID' AND created_at > '2024-01-01';

-- 결과
-> Filter: ((orders.status = 'PAID') and (orders.created_at > '2024-01-01'))
    (cost=1.23 rows=5) (actual time=0.045..0.089 rows=3 loops=1)
    -> Index range scan on orders using idx_status_date
        (cost=1.23 rows=5) (actual time=0.040..0.078 rows=3 loops=1)
```

### 해석

```
cost: 예상 비용
rows: 예상 행 수
actual time: 실제 소요 시간 (첫 행..마지막 행)
rows: 실제 반환 행 수
loops: 반복 횟수
```

### FORMAT=TREE

```sql
EXPLAIN FORMAT=TREE
SELECT u.name, COUNT(o.id)
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
GROUP BY u.id;

-- 실행 순서를 트리 구조로 표시
-> Group aggregate: count(o.id)
    -> Nested loop left join
        -> Table scan on u
        -> Index lookup on o using idx_user_id (user_id=u.id)
```

---

## 체크리스트

```
□ type이 ALL 또는 index인가? → 인덱스 추가 검토
□ possible_keys는 있는데 key가 NULL인가? → 인덱스 힌트 또는 쿼리 수정
□ rows가 지나치게 큰가? → 조건 추가 또는 인덱스 최적화
□ Using filesort가 있는가? → 정렬 컬럼 인덱스 추가
□ Using temporary가 있는가? → GROUP BY 최적화
□ DEPENDENT SUBQUERY가 있는가? → JOIN으로 변환 검토
```

---

*마지막 업데이트: 2026년 01월*

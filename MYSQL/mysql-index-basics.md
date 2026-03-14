# MySQL 인덱스 기초

MySQL InnoDB의 인덱스 구조와 활용 방법을 정리합니다.

## 목차

1. [B-Tree 구조](#1-b-tree-구조)
2. [클러스터드 인덱스](#2-클러스터드-인덱스)
3. [세컨더리 인덱스](#3-세컨더리-인덱스)
4. [커버링 인덱스](#4-커버링-인덱스)
5. [복합 인덱스](#5-복합-인덱스)
6. [인덱스 설계 가이드](#6-인덱스-설계-가이드)

---

## 1. B-Tree 구조

### B-Tree란?

```
B-Tree: Balanced Tree (균형 트리)
- 모든 리프 노드가 같은 깊이
- O(log N) 검색 성능 보장
- 디스크 I/O 최적화 (페이지 단위 읽기)

┌─────────────────────────────────────────────┐
│                  Root Node                  │
│              [30] [60] [90]                 │
└────────┬──────┬──────┬──────┬──────────────┘
         │      │      │      │
    ┌────▼──┐ ┌─▼───┐ ┌▼────┐ ┌▼────┐
    │ 10,20 │ │31-59│ │61-89│ │91+  │  Branch Nodes
    └───┬───┘ └──┬──┘ └──┬──┘ └──┬──┘
        │        │       │       │
    ┌───▼───┐ ┌──▼──┐ ┌──▼──┐ ┌──▼──┐
    │ Data  │ │Data │ │Data │ │Data │  Leaf Nodes
    └───────┘ └─────┘ └─────┘ └─────┘
```

### B-Tree vs B+Tree

```
B-Tree:
- 모든 노드에 데이터 저장
- 검색 시 중간 노드에서 종료 가능

B+Tree (MySQL InnoDB):
- 리프 노드에만 데이터 저장
- 리프 노드끼리 연결 (범위 검색 효율적)
- 중간 노드는 인덱스 역할만

┌───────────────────────────────────────────────────┐
│                    B+Tree                         │
│                                                   │
│              [30] [60] [90]  ← 인덱스만           │
│                    │                              │
│    ┌───────────────┼───────────────┐              │
│    │               │               │              │
│  [10,20,30]  →  [31..60]  →  [61..90]  → Leaf    │
│   ↓  ↓  ↓        ↓  ↓         ↓  ↓               │
│  Data Data     Data Data    Data Data             │
└───────────────────────────────────────────────────┘
        └─────────────────────────────┘
              리프 노드끼리 연결
```

---

## 2. 클러스터드 인덱스

### 정의

```
클러스터드 인덱스 (Clustered Index):
- 테이블당 1개만 존재
- 데이터가 인덱스 순서대로 물리적 정렬
- 보통 Primary Key가 클러스터드 인덱스

┌─────────────────────────────────────────────────────┐
│              Clustered Index (PK)                   │
│                                                     │
│  리프 노드 = 실제 데이터 행                          │
│                                                     │
│  [PK:1] → [id:1, name:'Kim', email:'kim@test.com'] │
│  [PK:2] → [id:2, name:'Lee', email:'lee@test.com'] │
│  [PK:3] → [id:3, name:'Park', email:'park@test.com']│
│                                                     │
│  PK 순서대로 데이터 정렬됨                          │
└─────────────────────────────────────────────────────┘
```

### 클러스터드 인덱스 선택 순서

```sql
1. PRIMARY KEY가 있으면 → 클러스터드 인덱스
2. 없으면 첫 번째 UNIQUE NOT NULL 컬럼 → 클러스터드 인덱스
3. 없으면 InnoDB가 내부적으로 GEN_CLUST_INDEX 생성

-- 권장: 명시적으로 PK 지정
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,  -- 클러스터드 인덱스
    email VARCHAR(255) UNIQUE,
    name VARCHAR(100)
);
```

### PK 선택 시 고려사항

```sql
-- Good: 순차적 증가 (AUTO_INCREMENT)
-- 삽입 시 항상 마지막에 추가 → 페이지 분할 적음

-- Bad: UUID (랜덤 값)
-- 삽입 위치가 랜덤 → 빈번한 페이지 분할 → 성능 저하

-- 대안: UUID v7 (시간 순서 정렬)
-- 또는 Snowflake ID (Twitter)
```

---

## 3. 세컨더리 인덱스

### 정의

```
세컨더리 인덱스 (Secondary Index):
- 테이블당 여러 개 가능
- 리프 노드에 PK 값 저장
- 데이터 조회 시 클러스터드 인덱스 추가 탐색 필요

┌─────────────────────────────────────────────────────┐
│           Secondary Index (email)                   │
│                                                     │
│  [email:'kim@'] → [PK:1]                           │
│  [email:'lee@'] → [PK:2]  ──────┐                  │
│  [email:'park@'] → [PK:3]        │                  │
│                                  │                  │
│                                  ▼                  │
│              ┌──────────────────────┐               │
│              │  Clustered Index     │               │
│              │  [PK:2] → 실제 데이터│               │
│              └──────────────────────┘               │
└─────────────────────────────────────────────────────┘
```

### 조회 과정

```sql
-- email로 조회
SELECT * FROM users WHERE email = 'lee@test.com';

-- 실행 순서:
-- 1. Secondary Index에서 email='lee@' 검색 → PK:2 획득
-- 2. Clustered Index에서 PK:2 검색 → 실제 데이터 반환

-- 총 2번의 인덱스 탐색 (Secondary → Clustered)
```

---

## 4. 커버링 인덱스

### 정의

```
커버링 인덱스 (Covering Index):
- 쿼리에 필요한 모든 컬럼이 인덱스에 포함
- 클러스터드 인덱스 추가 탐색 불필요
- EXPLAIN의 Extra: "Using index"
```

### 예시

```sql
-- 인덱스
CREATE INDEX idx_name_email ON users(name, email);

-- 커버링 인덱스 적용 (name, email만 조회)
SELECT name, email FROM users WHERE name = 'Kim';
-- Extra: Using index (클러스터드 인덱스 안 감)

-- 커버링 인덱스 미적용 (age도 필요)
SELECT name, email, age FROM users WHERE name = 'Kim';
-- age가 인덱스에 없음 → 클러스터드 인덱스 추가 탐색
```

### 활용

```sql
-- COUNT 쿼리 최적화
SELECT COUNT(*) FROM orders WHERE status = 'COMPLETED';

-- status만 있는 인덱스면 커버링 인덱스로 동작
CREATE INDEX idx_status ON orders(status);
```

---

## 5. 복합 인덱스

### 정의

```sql
-- 복합 인덱스: 여러 컬럼을 조합한 인덱스
CREATE INDEX idx_status_date ON orders(status, created_at);
```

### 컬럼 순서의 중요성

```sql
-- 인덱스: (status, created_at)

-- 인덱스 사용 O
WHERE status = 'PAID'
WHERE status = 'PAID' AND created_at > '2024-01-01'
WHERE status IN ('PAID', 'SHIPPED') AND created_at > '2024-01-01'

-- 인덱스 사용 X (첫 번째 컬럼 조건 없음)
WHERE created_at > '2024-01-01'

-- 인덱스: (a, b, c)
-- 사용 가능: a / a,b / a,b,c
-- 사용 불가: b / c / b,c
```

### 컬럼 순서 결정 기준

```sql
-- 1. 등호(=) 조건 컬럼을 앞에
WHERE status = 'PAID' AND created_at > '2024-01-01'
-- 인덱스: (status, created_at) ← status가 앞

-- 2. 카디널리티(선택도)가 높은 컬럼을 앞에
-- 단, 등호 조건이 우선

-- 3. 정렬(ORDER BY)에 사용되는 컬럼 고려
SELECT * FROM orders
WHERE status = 'PAID'
ORDER BY created_at DESC;
-- 인덱스: (status, created_at DESC)
```

---

## 6. 인덱스 설계 가이드

### 인덱스 생성이 좋은 경우

```sql
-- 1. WHERE 절에서 자주 사용
-- 2. JOIN 조건에 사용
-- 3. ORDER BY에 사용
-- 4. 카디널리티가 높은 컬럼 (값이 다양함)
-- 5. 범위 검색에 사용 (<, >, BETWEEN)
```

### 인덱스가 불필요한 경우

```sql
-- 1. 테이블 크기가 작음 (수천 건 이하)
-- 2. 자주 변경되는 테이블 (INSERT/UPDATE/DELETE 많음)
-- 3. 카디널리티가 낮은 컬럼 (예: 성별 M/F)
-- 4. 전체 데이터의 대부분을 조회하는 경우
```

### 인덱스 확인

```sql
-- 테이블 인덱스 확인
SHOW INDEX FROM users;

-- 인덱스 사용 통계
SELECT * FROM sys.schema_index_statistics
WHERE table_name = 'users';

-- 미사용 인덱스
SELECT * FROM sys.schema_unused_indexes;

-- 중복 인덱스
SELECT * FROM sys.schema_redundant_indexes;
```

### 인덱스 주의사항

```sql
-- 1. 인덱스도 공간을 차지함 (저장소 비용)
-- 2. INSERT/UPDATE/DELETE 시 인덱스도 업데이트 (쓰기 비용)
-- 3. 너무 많은 인덱스는 오히려 성능 저하
-- 4. 복합 인덱스가 단일 인덱스 여러 개보다 효율적

-- 권장: 실제 쿼리 패턴을 분석하여 필요한 인덱스만 생성
```

---

## 핵심 정리

| 개념 | 설명 |
|------|------|
| B+Tree | 리프 노드에 데이터, 리프끼리 연결 |
| 클러스터드 | 테이블당 1개, PK 순서로 데이터 정렬 |
| 세컨더리 | 리프에 PK 저장, 추가 탐색 필요 |
| 커버링 | 인덱스만으로 쿼리 완료 |
| 복합 | 컬럼 순서 중요, 등호 조건 먼저 |

---

*마지막 업데이트: 2026년 01월*

# 기본 SQL 문법



#### SELECT	

```SQL
SELECT [컬럼 명] FROM [테이블 명] WHERE [조건];
```

- `*`는 전체를 의미

- WHERW [조건]에 연산자 사용 가능

```sql
SELECT [컬럼 명, 컬럼 명] FROM [테이블 명] WHERE [조건 AND 조건];
```

- 조건 연산자 (=, <, >, <=, >=, <>, != 등)
- 관계 연산자(NOT, AND, OR 등)

<br/>

#### BETWEEN [] AND

- 연속 된 값에서만 사용 가능

- 경남, 이름 등 이산적인 값에는 사용 불가

```sql
SELECT [컬럼 명] FROM [테이블 명] WHERE [조건 대상 컬럼 명] BETWEEN [조건 AND 조건];
```

- 조건과 조건 사이의 값이 있으면 요청한 테이블에서 해당 컬럼 값들을 가져와라

<br/>

#### IN()

- 경남, 이름 등 이산적인 값에만 사용 가능

- 연속 된 값에서는 사용 불가

```sql
SELECT [컬럼 명] FROM [테이블 명] WHERE [조건 대상 컬럼 명] IN [조건, 조건, 조건];
```

- 조건들 사이에 원하는 값이 있으면 요청한 테이블에서 해당 컬럼 값들을 가져와라

<br/>

#### LIKE

```sql
SELECT [컬럼 명] FROM [테이블 명] WHERE [조건 대상 컬럼 명] LIKE [조건];
```

- 조건에 해당하는 값이 있으면 요청한 테이블에서 해당 컬럼 값들을 가져와라

<br/>

#### ANY

```sql
SELECT [컬럼 명] FROM [테이블 명] WHERE [조건] 
	ANY [SELECT [컬럼 명] FROM [테이블 명] WHERE [조건]];
```

- ANY 뒤의 조건을 충족하면서 WHERE의 조건도 충족하는 기준이 두개 이상일 경우

- 여러 결과 중 한가지만 만족해도 포함하여 가져 옴

<br/>

- `=ANY`는 정확히 일치하는 값들만 가져오기에 `IN()`과 같은 의미를 갖는다.

<br/>

#### ALL

```sql
SELECT [컬럼 명] FROM [테이블 명] WHERE [조건] 

	ALL [SELECT [컬럼 명] FROM [테이블 명] WHERE [조건]];
```

- ANY 뒤의 조건을 충족하면서 WHERE의 조건도 충족하는 기준이 두개 이상일 경우

- 여러 결과 모두 만족하는 결과를 가져 옴

<br/>

#### OREDER BY

```sql
SELECT [컬럼 명] FROM [테이블 명]  ODER BY [기준 컬럼 명];

SELECT [컬럼 명] FROM [테이블 명]  ODER BY [기준 컬럼 명] DESC; -- 내림 차순
```

- 지정한 컬럼명 기준으로 정렬하여 출력

  - DESC를 뒤에 붙여주면 내림차순으로 정렬

  - ASC를 붙이면 오름차순이지만 기본 값

- 지정 칼럼명이 꼭 SELECT 대상이 아니어도 가능

- 값에는 영향을 주지 않음

<br/>

#### DISTINCT

```sql
SELECT DISTINT [컬럼 명] FROM [테이블 명];
```

- 중복값은 하나만 남기고 가져 옴

<br/>

#### ROWNUM

```sql
SELECT [컬럼 명] FROM [테이블 명] WHERE ROWNUM [조건];
```

- 조건에 `<=5`를 넣을 경우 결과의 앞 5열만 보여줌

- 모든 연산을 한 뒤 5열만 보여주는 것이기 때문에 5개만 연산하는 것이 아님

<br/>

#### SAMPLE

```sql
SELECT [컬럼 명] FROM [테이블 명]  SAMPLE (퍼센트);
```

- 퍼센트는 0초과 100미만 값이어야 함

- 퍼센트 값 에 해당하는 비율의 값을 가져 옴

<br/>

#### 테이블 복사 CREATE TABEL --- AS SELECT

```sql
CREATE TABLE [새로운 테이블 명] 
	AS (SELECT [복사할 컬럼 명] FROM [기존 테이블 명]);
```

- 테이블을 일부 또는 전체 복사 할 때 사용

- 제약조건(PK, FK 등)은 복사되지 않음

<br/>

#### GROUP BY

```sql
SELECT [컬럼 명] FROM [테이블 명] WHERE [조건] GROUP BY [기준 컬럼 명 ];

SELECT [컬럼 명, 집계함수 AS "별칭 지정"] FROM [테이블 명] WHERE [조건] GROUP BY [기준 컬럼 명];
```

- 기준 컬럼명을 중심으로 그룹을 나누어 테이블 보여 줌

<br/>

##### GROUP BY와 함께 자주 사용 되는 집계 함수(집합 함수)

| 함수 명         | 설명                     |
| --------------- | ------------------------ |
| AVG()           | 평균 구하기              |
| MIN()           | 최소값 구하기            |
| MAX()           | 최대값 구하기            |
| COUNT()         | 행의 개수 세기           |
| COUNT(DISTINCT) | 중복 없는 행의 개수 세기 |
| STDEV()         | 표준편차 구하기          |
| VARIANCE()      | 분산 구하기              |

<br/>

```sql
SELECT [집계 함수  AS "별칭 지정"] FROM [테이블 명] GROUP BY [기준 컬럼 명];
```

<br/>

#### HAVING

```sql
SELECT [컬럼 명, 집계 함수 AS "별칭 지정"]  FROM [테이블 명] GROUP BY [기준 컬럼 명] HAVING [조건];
```

- 집계 함수는 WHERE 절에 사용 불가

- 이때 집계 함수에 대한 조건을 제한하는 문법이 HAVING

- 무조건 GROUP BY 다음에 사용 되어야 함

<br/>

#### ROLLUP()

```sql
SELECT [컬럼 명, 집계 함수 AS "별칭 지정"]  FROM [테이블 명] GROUP BY ROLLUP(그룹 이름, 기준 컬럼);
```

- 그룹화가 되지 않는 효과를 원하는 기준 컬럼을 기준으로 소 합계, 총 합계를 제공

- 기준 컬럼을 빼면 소 합계 및 총 합계만 제공

<br/>

#### GROUPING_ID()

```sql
SELECT [컬럼 명, 집계 함수 AS "별칭 지정"], GROUPING_ID(그룹 이름) AS "별칭 지정"

	FROM [테이블 명] GROUP BY ROLLUP(그룹 이름, 기준 컬럼);
```

- GROUPING_ID의 결과가 0이면 데이터 1이면 합계를 위해 추가 된 열

<br/>

#### CUBE()

```sql
SELECT [컬럼 명, 집계 함수 AS "별칭 지정"] FROM [테이블 명] GROUP BY CUBE(기준 컬럼);
```

- 기준 컬럼별 소합계가 구분되어 나온다.

- ROLLUP()과 비슷하지만 다차원 정보의 데이터를 요약하는 데 적합

<br/>

## SQL문은 크게 아래 세가지로 나눈다

### DML

- Data Manipulation Language : 데이터 조작 언어

- 데이터를 조작(선택, 삽입, 수정, 삭제)하는 데 사용되는 언어
- DML 구문이 사용되는 대상은 테이블의 행
- 사용 위해서는 테이블이 정의 되어야 함
- SELECT, INSERT, UPDATE, DELETE
- 트랜잭션(Transaction)이 발생하는 SQL
  - 트랜잭션이란 테이블의 데이터를 변경할 때 완전히 적용하지 않고 임시로 적용시켜 적용의 취소가 가능하도록 제공하는 기능
  - 완전히 적용을 위해서는 COMMIT문을 사용해야 한다
  - 트랜잭션 상태에서 취소하기 위해서는 ROLLBACK문을 사용한다

<br/>

### DDL

- Data Definition Language : 데이터 정의 언어
- 데이터베이스, 테이블, 뷰, 인덱스 등의 데이터베이스 개체를 생성/삭제/변경하는 역할
- CREATE, DROP, ALTER 등을 자주 사용
- 트랜잭션을 발생시키지 않음
  - COMMIT, ROLLBACK 없음
- 실행 즉시 적용

<br/>

### DCL

- Data Control Language : 데이터 제어 언어
- 사용자에게 어떤 권한을 부여하거나 빼앗을 때 주로 사용
- GRANT, REVOKE, DENY 등이 해당

<br/>

### 데이터 삽입 : INSERT

```sql
INSERT INTO [테이블 명] (컬럼 명) VALUES(값);
```

- 컬럼명에 명시한만큼 값을 일치하게 작성해야 함
- 존재하는 컬럼보다 적은 컬럼 명을 명시할 경우 값을 입력하지 않은 컬럼의 기본값이 있다면 기본값이 설정 됨

```sql
INSERT INTO [테이블 명] VALUES(값);
```

- 테이블 명 뒤에 컬럼 명을 적지 않을 경우 테이블에 있는 모든 컬럼에 대한 값을 입력해야 함

<br/>

#### SEQUENCE

- 자동으로 1씩 증가해서 ID값을 넣어주는 시퀀스 개체 사용

```sql
CREATE SEQUENCE idSEQ
	START WITH 1 -- 시작값
	INCREMENT BY 1; -- 증가값
	
-- =============================================

INSERT INTO [테이블 명] VALUES (idSEQ.NEXTVAL, 값2, 값3 ..)
-- 시퀀스.NEXTVAL을 사용하여 입력
```

<br/>

- 시퀀스 시작값 재설정

```sql
CREATE SEQUENCE idSEQ
	START WITH 1 -- 시작값
	INCREMENT BY 1; -- 증가값
	
-- =============================================

INSERT INTO [테이블 명] VALUES (11, 값2, 값3 ..) -- 시퀀스 강제 입력
ALTER SEQUENCE idSEQ
	INCREMENT BY 10; -- 증가값 수정
```

<br/>

- 사이클 시퀀스 예시

```sql
CREATE SEQUENCE cycleSEQ
	START WITH 100	-- 시작값
	INCREMENT BY 100	-- 증가값
	MINVALUE 100	-- 최소값
	MAXVALUE 300	-- 최대값
	CYCLE	-- 반복 설정
	NOCACHE	--캐시 사용 안 함
```

- cycleSEQ.NEXTVAL로 설정한 컬럼의 값은 100, 200,300이 반복되어 적용 된다.

<br/>

##### 대량의 샘플 데이터 생성

```sql
INSERT INTO [테이블 명] (컬럼 명, 컬럼 명..) SELECT [컬럼 명] FROM [테이블 명];
```

<br/>

### 데이터 수정 : UPDATE

```sql
UPDATE [테이블 이름] SET [''컬럼1'='값1', '컬럼2'='값2'] WHERE [조건];
```

- WHERE 절 빼먹으면 전체 행의 값이 변함 주의 필요

<br/>

### 데이터 삭제 : DELETE

```sql
DELET FROM [테이블 이름] WHER [조건];
```

- 조건에 해당하는 값이 있는 행 단위를 테이블에서 삭제

- DML문이기에 트랜잭션 로그를 기록하느라 삭제하느라 오래 걸림

- DDL문인 DROP은 테이블 자체를 삭제하고 트랜잭션을 발생시키지 않음

- DDL문인 TUNCATE는 DELETE와 동일한 효과지만 트랜잭션 로그를 기록하지 않아서 속도가 빠르다

<br/>

### 조건부 데이터 변경 : MERGE

- 하나의 문장에서 경우에 따라  INSERT, UPDATE, DELET를 수행 할 수 있음

```sql
   MERGE INTO [스키마.]테이블명
        USING (update나 insert될 데이터 원천)
             ON (update될 조건)
             
    WHEN MATCHED THEN
        UPDATE SET 컬럼1 = 값1, 컬럼2 = 값2 ...
    	WHERE update [조건]
        DELETE WHERE update_delete [조건]
        
    WHEN NOT MATCHED THEN
           INSERT (컬럼1, 컬럼2, ...) 
           VALUES (값1, 값2,...)
           WHERE insert [조건];
```


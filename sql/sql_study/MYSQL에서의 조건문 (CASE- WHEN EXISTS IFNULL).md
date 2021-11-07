## MYSQL에서의 조건문 (CASE- WHEN/ EXISTS/ IFNULL)

<br/>

DB에서 원하는 값을 조회할 때 값 자체가 아니라 값이 있는지 여부를 확인해야 할 때가 있다.

이때 사용하는 구문은 IFNULL(조회 컬럼, NULL일 경우 반환값)을 넣어 조회한다.

#### 예시 테이블

| user_id | user_name | user_age | phone         |
| ------- | --------- | -------- | ------------- |
| ktae23  | 타태      | 30       | (NULL)        |
| java23  | 자바      | 40       | 010-1111-2222 |

<br />

##### IFNULL 조건문

```sql
SELECT IFNULL(phone,'저장된 번호가 없습니다.') FROM user_tb WHERE user_id = 'java23';

// 조회 값
010-1111-2222

SELECT IFNULL(phone,'저장된 번호가 없습니다.') FROM user_tb WHERE user_id = 'ktae23';

// 조회 값
저장된 번호가 없습니다.

SELECT IFNULL(phone, 0) FROM user_tb WHERE user_id = 'ktae23';

// 조회 값
0
```

<br />

하지만 단지 해당 컬럼의 값이 없는게 아니라 일치하는 행 자체가 없을 수 있다.

이 경우엔 null이 저장된 행을 조회한 것이 아니기 때문에 아무런 값이 반환되지 않는다.

<br />

##### IFNULL 조건문

```sql
SELECT IFNULL(phone,'저장된 번호가 없습니다.') FROM user_tb WHERE user_id = 'kk100';

// 조회 값


SELECT IFNULL(phone,'저장된 번호가 없습니다.') FROM user_tb WHERE user_id = 'python23';

// 조회 값

```

<br />

때문에 이런 경우 EXISTS를 사용해 조회하길 원하는 대상의 ROW가 존재하는지 먼저 조회해야 한다.

<br/>

##### EXISTS조건문

```sql
SELECT EXISTS(SELECT phone FROM user_tb WHERE user_id = 'ktae23');

// 조회 값
1

SELECT EXISTS(SELECT phone FROM user_tb WHERE user_id = 'python23');

// 조회 값
0
```

<br />

지금까지 설명한 조건부 조회를 위해 CASE - WHEN 구문을 이용하여 EXSIST 조건을 걸어주고, 존재할 경우 IFNULL을 사용해준다.

<br/>

##### CASE - WHEN 조건문

```sql
SELECT CASE WHEN (조건문)	THEN (조건이 맞을 경우 실행 할 구문 | 반환값)
            WHEN (조건문)	THEN (조건이 맞을 경우 실행 할 구문 | 반환값)
            WHEN (조건문)	THEN (조건이 맞을 경우 실행 할 구문 | 반환값)
			ELSE (이외의 경우 실행 할 구문)
			END (#조건문 종료)
```

<br />

### 조건부 조회 예제

#### 예시 테이블

| user_id | user_name | user_age | phone         |
| ------- | --------- | -------- | ------------- |
| ktae23  | 타태      | 30       | (NULL)        |
| java23  | 자바      | 40       | 010-1111-2222 |

<br />

```sql
SELECT DISTINCT
	CASE WHEN(	# 1차 조건문 시작
        SELECT EXISTS(
                        SELECT phone
                        FROM user_tb 
                        WHERE user_id = '#{useId}'
                      ) = 1 	# 1차 조건문 종료
            
    THEN	# 1차 조건(해당하는 행이 존재)이 맞을 경우 실행 할 구문 또는 반환 값
    
    	CASE WHEN(	# 2차 조건문 시작 
                    SELECT IFNULL(phone,0)
                    FROM user_tb
                    WHERE user_id = #{useId}
        		  ) = 0 	# 2차 조건문 종료
            
        THEN 0	# 2차 조건(해당하는 행이 존재하고 조회 값이 NULL)이 맞을 경우 실행 할 구문 또는 반환 값
        
        ELSE 1	# 2차 조건 이외(해당하는 행이 존재하고 조회 값이 NULL이 아님)의 경우 실행 할 구문 또는 반환 값
        END    # 2차 조건문 종료
        
    ELSE 2    # 1차 조건 이외(해당하는 행이 존재하지 않음)의 경우 실행 할 구문 또는 반환 값
	END AS phone    # 1차 조건문 종료

FROM user_tb
```

<br/>

```sql
id가 'ktae23'일 경우

// 조회 값
0

id가 'java23'일 경우

// 조회 값
1

id가 'python23'일 경우

// 조회 값
2
```




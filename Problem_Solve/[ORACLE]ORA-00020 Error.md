```java
[Oracle] ORA-00020 maximum number of processes (100) exceeded, ORA-12519 TNS:no appropriate service handler found
```

- 접속 가능한 프로세스 수를 초과하여 생기는 문제
  - EC2에 Docker를 이용해 받았더니 sqlplus에 as sysdba로 로그인이 되지 않아서 권한 문제로 수정이 불가했다.
  - 아래 방법을 이용하니 관리자 권한으로 접속이 가능하여 수정했다.

```sql
# Sql plus 로그인 이후 관리자 권한으로 접속하기
SQL> conn sys/oracle as sysdba
Connected.

# 프로세스 제한 정보 가져오기
SQL> select * from v$resource_limit where resource_name = 'processes';

RESOURCE_NAME                  CURRENT_UTILIZATION MAX_UTILIZATION
------------------------------ ------------------- ---------------
INITIAL_ALLOCATION
----------------------------------------
LIMIT_VALUE
----------------------------------------
processes                                       72             100
       100
       100


# 세션을 300으로 늘리기
SQL> alter system set sessions=300 scope=spfile;

System altered.

# 프로세스를 300으로 늘리기
SQL> alter system set processes=300 scope=spfile;

System altered.

# 커밋
SQL> commit;

Commit complete.

# DB 종료
SQL> shutdown immediate
Database closed.
Database dismounted.
ORACLE instance shut down.

# DB 시작
SQL> startup
ORACLE instance started.

Total System Global Area  601272320 bytes
Fixed Size                  2228848 bytes
Variable Size             260050320 bytes
Database Buffers          335544320 bytes
Redo Buffers                3448832 bytes
Database mounted.
Database opened.

# 프로세스 제한 정보 가져오기
SQL> select * from v$resource_limit where resource_name = 'processes';

RESOURCE_NAME                  CURRENT_UTILIZATION MAX_UTILIZATION
------------------------------ ------------------- ---------------
INITIAL_ALLOCATION
----------------------------------------
LIMIT_VALUE
----------------------------------------
processes                                       31              32
       300
       300
       
# sql plus 종료
SQL> exit
```


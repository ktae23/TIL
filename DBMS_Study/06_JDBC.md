## JDBC(Java DataBase Connectivity)

![Java Database Connectivity - JDBC](https://networkencyclopedia.com/wp-content/uploads/2019/08/java-database-connectivity-jdbc-1024x576.jpg)

- Java Application과 DBMS를 연결하는 용도
- JDBC API는 대부분 Interface로 이루어져 있음
  - DBMS 벤더에서 서브클래스를 구현해서 제공하는 편
  - 서브클래스 집합을 driver라고 부름
- 플랫폼 독립성(Platform Independent)
  - 플랫폼에 무관하게 인터페이스를 보고 작성하면 어떤 DBMS든 해당 드라이버만 연결하면 사용 가능

<br/>

#### 1. 드라이버 등록

<br/>

```java
오라클 XE 받기 
http://dw.hanbit.co.kr/Oracle/11gXE/
//	OracleXe112 Win64
//	sqldeveloper-17.2.0.188.1159-xno-jre.zip

오라클 정보
jdbc.url=jdbc:oracle:thin:@localhost:1521:xe
jdbc.driver = oracle.jdbc.driver.OracleDriver	// jdbc driver 경로
jdbc.username = shop	// DBMS 사용자 
jdbc.password = 1234	// DBMS 비밀번호


MySQL 정보
jdbc.url=jdbc:mysql://127.0.0.1:3306/openeg
jdbc.driver = com.mysql.jdbc.Driver	// jdbc driver 경로
jdbc.username = root	// DBMS 사용자 
jdbc.password = apmsetup	// DBMS 비밀번호
```

<br/>

- Class 클래스 사용

```java
Class.forName("드라이버 경로");    // 드라이버 객체 바로 생성 됨
```

<br/>

`Project` 우클릭 -> `Build path` -> `Configure Build Path`

-> `Libraries` Tap ->`Add External JARs` Button

=>  JAR 파일 등록하여  프로젝트 내에 Referenced Libraries 생성

![image-20210218111150308](C:\Users\zz238\AppData\Roaming\Typora\typora-user-images\image-20210218111150308.png)

<br/>

#### 2. 드라이버 연결

<br/>

```java
public ststic Connection getConnection(String url, String user, String password) throws SQLExecption
```

<br/>

- DriverManager클래스 - getConnection() 메서드 사용

```java
Connection con = DriverManager.getConnection(String url, String user, String password);
```

- Connection은 인터페이스, DBMS와 Driver를 연결하면 Connection 객체가 생성 됨

<br/>

#### 3. Statement 생성

<br/>

- Connection(인터페이스) - createStatement() 메서드 사용

```java
Statement stmt = con.createStatement();
// Connection 객체로부터  Statement 인터페이스 생성하여 반환 받음
```

<br/>

- SQL 전송 역할
- 인터페이스기 때문에 직접 new 생성 불가

```java
Statement createStatement()
                          throws SQLException
Returns:
a new default Statement object
    
Throws:
SQLException - if a database access error occurs or this method is called on a closed connection
```

<br/>

#### 4. SQL 전송

<br/>

```java
ResultSet rs = stmt.executeQuery("SQL")
    // excuteQuery는 Select 문 전용
```

<br/>

- Statement(인터페이스) - executeQuery() 메서드 사용
- ResultSet(인터페이스)  타입 반환

<br/>

```java
ResultSet executeQuery(String sql)
                       throws SQLException
Executes the given SQL statement, which returns a single ResultSet object.

Note:This method cannot be called on a PreparedStatement or CallableStatement.

Parameters:
sql - an SQL statement to be sent to the database, typically a static SQL SELECT statement
Returns:
a ResultSet object that contains the data produced by the given query; never null

Throws:
SQLException - if a database access error occurs, this method is called on a closed Statement, the given SQL statement produces anything other than a single ResultSet object, the method is called on a PreparedStatement or CallableStatement
SQLTimeoutException - when the driver has determined that the timeout value that was specified by the setQueryTimeout method has been exceeded and has at least attempted to cancel the currently running Statement
```

<br/>

#### 5. 결과 얻기

<br/>

```java
while(rs.next()){
    int id = re.getInt(컬럼명)	//get컬럼타입()메서드 사용
    String name = rs.getString(컬럼명);
    String addr = rs.getString(컬럼 순서)	// 이름 검색보다 성능 높아짐
}
```

<br/>

```java
String getString(int columnIndex)
                 throws SQLException
Retrieves the value of the designated column in the current row of this ResultSet object as a String in the Java programming language.
Parameters:
columnIndex - the first column is 1, the second is 2, ...

Returns:
the column value; if the value is SQL NULL, the value returned is null

Throws:
SQLException - if the columnIndex is not valid; if a database access error occurs or this method is called on a closed result set
```

<br/>

#### 6. 자원 닫기 (사용의 역순)

<br/>

```java
rs.close();
stmt.close();
con.close();
```

<br/>

---

<br/>

## 연습 예제

<br/>

```java
package test;

import java.sql.*;

public class Test {
	public static void main(String[] args) {
		Connection con= null;
		Statement stmt= null;
		ResultSet rs= null;
		try {
			// 1. driver 등록
			Class.forName("oracle.jdbc.driver.OracleDriver"); // 오라클 드라이버 경로
			System.out.println("driver ok");
			
			// 2. 연결
			con=DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe", "shop", "1234");
            											//parmeter (String url, String user, String password)
			System.out.println("con ok");
			
			// 3. Statement 생성
			stmt=con.createStatement();
			System.out.println("stmt ok");
			
			// 4. SQL 전송
			rs=stmt.executeQuery("select * from membertbl");
            rs=stmt.executeQuery("select * from membertbl where memberaddress like '%"+args[0]+"%'"); 
                            							// program arguments에  입력 된 값으로 조회 하기
            int i=stmt.executeUpdate("insert into membertbl values('ktae', '박경태', '인천광역시')");	
            											// 변경 된 DML 행의 개수를 int 값으로 return 함 (없으면 0)
			System.out.println(i + "행이 insert 되었습니다.");
			System.out.println("rs ok");

			// 5. 결과 얻기
			while(rs.next()) {
				String id=rs.getString("memberid");
				String name=rs.getString("membername");
				String addr=rs.getString(3);
				
				System.out.println(id + ":"  + name + ":" +addr);
			}
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			// 6. 자원 종료
			try {
				if(rs !=null) rs.close();
				if(stmt !=null) stmt.close();
				if(con !=null) con.close();
				
			} catch (SQLException e) {
				
			}
		}
	}
}
```

<br/>



#### Statement의 확장

```java
# Statement (인터페이스) (일반)

- execute(sql)	- 모두
  		↘ boolean 값으로 반환 


- executeQuery(sql) - select
  		↘ 커서를 가진 Result set으로 반환


- executeUpdate(sql) - select 이외 모두
  		↘ 변경 된 행의 수를 정수 형으로 반환 
```



<center>⬆</center>



```java

# PreparedStatement(인터페이스) extends statement(인터페이스)[전문]

- stmt = con.prepareStatement(지정 SQL);

  	↘ execute() 

  	↘ executeQuery() 

  	↘ executeUpdate() 
  	

- stored procedure (저장 절차) 
        
   	↘ preparedStatement 호출시 DB에 생성 됨

  	↘ 매개 변수 없이 호출해도 저장 절차 메서드를 호출해서 지정 SQL문만을 전달 함
    ↘ 사용성과 보안성 좋아짐
```

<br>

##### 예제

```java
package test;

import java.sql.*;

public class InsertTest {
	public static void main(String[] args) {
		Connection con= null;
		PreparedStatement pstmt=null;
		ResultSet rs= null;
		try {
			// 1. driver 등록
			Class.forName("oracle.jdbc.driver.OracleDriver"); // 오라클 드라이버 경로
			System.out.println("driver ok");
			
			// 2. 연결
			con=DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe", "shop", "1234");	
											//parmeter (String url, String user, String password)
			System.out.println("con ok");
			
			// 3. Statement 생성
			pstmt = con.prepareStatement("insert into member(memid, memname, memdate, phone, point) values(?,?,?,?,?)");	// 삽입
			pstmt.setString(1, args[0]);
			pstmt.setString(2, args[1]);
			java.util.Date today=new java.util.Date();
			Date now= new Date(today.getTime());
			pstmt.setDate(3, now);
			pstmt.setString(4, args[2]);
			pstmt.setInt(5, 0);
			
            
     	    pstmt=con.prepareStatement("delete from member where memid=?");	//컬럼 삭제
			pstmt.setString(1,args[0]);	// id로 조회해서 컬럼 삭제	
            
            pstmt=con.prepareStatement("create table product(pno integer(4), pname varchar(20), price integer(8))"); // 생성
			pstmt=con.prepareStatement("drop table product");	//삭제
            
            System.out.println("pstmt ok");
            
			// 4. SQL 전송
			int j=pstmt.executeUpdate();	//	preparedstatement에 미리 준비해 두었기 때문에 호출만 하면 됨

			// 5. 결과 얻기
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				// 6. 자원 종료
				try {
					if(rs !=null) rs.close();
					//if(stmt !=null) stmt.close();
					if(pstmt !=null) pstmt.close();
					if(con !=null) con.close();
					
			} catch (SQLException e) {
                    
			}
		}
	}
}

```



<center>⬆</center>

```java
# CallableStatement(인터페이스) extends statement(인터페이스) [호출]
    
```




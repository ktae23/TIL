## DAO (Data Access Object)

- 데이터베이스의 데이터에 접근하여 CRUD(Create, Read, Update, Drop) 기능을 제공하는 객체
- 1. 드라이버 등록
  2. 연결
  3. 스테이트먼트 생성
  4. SQL 전송
  5. 결과 확인
  6. 종료(자원 해제)
- 데이터베이스 접근 파트와 비즈니스 로직 파트를 분리하여 사용하는 목적 

<br/>

```java
package web.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import web.util.Member;
import web.util.MyException;

public class MemberDAO {

	public MemberDAO() throws MyException {
		try {
			// 1. 드라이버 등록
			Class.forName("oracle.jdbc.driver.OracleDriver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new MyException("드라이버 등록 오류");
		}
	}
	
	public void memberInsert(Member m) throws MyException {
		Connection con=null;
		PreparedStatement stmt=null;
		try {
			// 2. Connection
			con=DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe","cafe","1234");
			
			// 3. Statement
			stmt=con.prepareStatement("insert into member(memid,pw,memname,subject) values(?,?,?,?)");
			
			// 4. SQL 전송
			stmt.setString(1,  m.getId());
			stmt.setString(2, m.getPw());
			stmt.setString(3, m.getName());
			String subject="";
			for(String s:m.all_subject) {
				subject += s+", ";
			}
			stmt.setString(4, subject);
			
			int i = stmt.executeUpdate();
			
			// 5. 결과 확인
			System.out.println(i+"행이 insert 되었습니다.");
			
		} catch (SQLException e) {
			e.printStackTrace();
			throw new MyException("회원 가입 오류");
		} finally {
			// 6. 종료
			
			try {
				if(stmt!=null) stmt.close();
				if(con!=null) con.close();
			} catch (SQLException e) {
				
			}
		}
	}
}
```

<br/>

## DTO (Data Transfer Object)

- 데이터를 저장하는 클래스로 별다른 메서드나 기능을 갖지 않고 getter, setter, 생성자만 가지는 클래스
  - Object 클래스의 메서드를 오버라이딩하기도 함
- VO와 혼용하여 사용하기도 함
  - DTO는 가변성이 있어 getter, setter 모두 사용

<br/>

```java
package web.util;

public class Member {
	private String id, name,pw;
	public String[] all_subject;
	
	public Member(String id, String pw,String name) {
		super();
		setId(id);
		setPw(pw);
		setName(name);
	}
	
	public Member (String id, String pw, String name, String[] all_subject) {
		this(id,pw, name);
		this.all_subject=all_subject;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		if(id!=null)
			this.id = id;
		else {
			System.out.println("id는 null이 될 수 없습니다.");
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		if(name!=null)
			this.name = name;
		else {
			System.out.println("name은 null이 될 수 없습니다.");
		}
	}

	public String getPw() {
		return pw;
	}

	public void setPw(String pw) {
		if(pw!=null)
			this.pw = pw;
		else {
			System.out.println("pw는 null이 될 수 없습니다.");
		}
	}
}
```

<br/>

## VO (Value Object)

- 데이터를 가지며 다른 로직을 갖지 않는 점에서 DTO와 비슷함
- 값(value)를 쓰기 위해 사용하는 객체인데, 자바에에서 값 타입 표현에는 읽기 전용 불변 클래스를 만들어 사용하는 특징을 지녔기 때문에 값을 바꾸기 위해서는 다시 만들어야 함 
  - getter만 사용

```java
package web.util;

public class Member {
	private String id = "admin";
    private String name = "관리자";
    private String pw = "1234";

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getPw() {
		return pw;
	}

}
```


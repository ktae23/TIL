package web.model;

import java.sql.*;

import web.util.MemberVO;
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
	
	public void memberInsert(MemberVO m) throws MyException {
		Connection con=null;
		PreparedStatement stmt=null;
		try {
			// 2. Connection
			con=DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe","cafe","1234");
			
			// 3. Statement
			stmt=con.prepareStatement("insert into member(memid,memname) values(?,?)");
			
			// 4. SQL 전송
			stmt.setString(1,  m.getId());
			stmt.setString(2, m.getName());
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

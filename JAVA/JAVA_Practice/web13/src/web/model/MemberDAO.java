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
	public List<Member> listMembers() throws MyException{
		List<Member> list=new ArrayList<Member>();
		Connection con=null;
		PreparedStatement stmt=null;
		ResultSet rs=null;
			try {
				con=DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe","cafe","1234");
				
				stmt=con.prepareStatement("select * from member");
				
				rs = stmt.executeQuery();
				while(rs.next()) {
					String id=rs.getString("memid");
					String pw=rs.getString("pw");
					String name=rs.getString("memname");
					Member m=new Member(id,pw,name);
					list.add(m);
				}return list;
			} catch (SQLException e) {
				e.printStackTrace();
				throw new MyException("모든 고객 조회 실패");
			} finally {
				
				try {
					if(rs!=null) rs.close();
					if(stmt!=null) stmt.close();
					if(con!=null) con.close();
				} catch (SQLException e) {
					
				}
			}
			
	
		
	}

	public String login(String id, String pw) throws MyException {
		Connection con=null;
		PreparedStatement stmt=null;
		ResultSet rs=null;
			try {
				con=DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe","cafe","1234");
				
				stmt=con.prepareStatement("select * from member wher memid=? and pw=?");
				stmt.setString(1, id);
				stmt.setString(2, pw);
				
				rs = stmt.executeQuery();
				if(rs.next()) {
					String name=rs.getString("memname");
					return name;
				} return null;
			} catch (SQLException e) {
				e.printStackTrace();
				throw new MyException("로그인 실패");
			} finally {
				
				try {
					if(rs!=null) rs.close();
					if(stmt!=null) stmt.close();
					if(con!=null) con.close();
				} catch (SQLException e) {
					
				}
			}
			
	
	}
}

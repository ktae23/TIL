package web.model;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import web.util.Member;
import web.util.MyException;

public class MemberDAO {
	DataSource dbcp;
	
	public MemberDAO() throws MyException {
		try {
			Context ctx=new InitialContext();
			Context envContext=(Context)ctx.lookup("java:comp/env");
			dbcp=(DataSource) envContext.lookup("jdbc/oracle");
			
		} catch (NamingException e) {			
			e.printStackTrace();
			throw new MyException("자원 찾기 오류");
		}
	}
	
	public void memberInsert(Member m) throws MyException {
		
		Connection con=null;
		PreparedStatement stmt=null;
		try {
			//2. Connection
			con=dbcp.getConnection();
			
			//3. Statement
			stmt=con.prepareStatement("insert into member(memid,memname,subject,pw) values(?,?,?,?)");
			
			//4. SQL전송
			stmt.setString(1, m.getId());
			stmt.setString(2, m.getName());
			String subject="";
			for(String s:m.all_subject) {
				subject +=s+",";
			}
			stmt.setString(3, subject);//java,c,python,
			stmt.setString(4, m.getPw());
			int i=stmt.executeUpdate();
			
			//5. 결과 확인
			System.out.println(i+"행이 insert되었습니다");
			
		} catch (SQLException e) {			
			e.printStackTrace();
			throw new MyException("회원 가입 오류");
		} finally {
			//6. 종료
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
			//2. Connection
			con=dbcp.getConnection();
			
			//3. Statement
			stmt=con.prepareStatement("select * from member");
			
			rs=stmt.executeQuery();
			while(rs.next()) {
				String id=rs.getString("memid");
				String name=rs.getString("memname");
				String pw=rs.getString("pw");
				Member m=new Member(id,pw,name);
				list.add(m);
			}
			return list;
		}catch(SQLException e) {
			e.printStackTrace();
			throw new MyException("모든 고객 조회 실패");
		}finally {
			//6. 종료
			try {
				if(rs!=null) rs.close();
				if(stmt!=null) stmt.close();
				if(con!=null) con.close();
			} catch (SQLException e) {
				
			}
		}
		
	}

	public String login(String id, String pw) throws MyException {
		System.out.println(id+":"+pw);
		
		Connection con=null;
		PreparedStatement stmt=null;
		ResultSet rs=null;
		try {
			//2. Connection
			con=dbcp.getConnection();
			
			//3. Statement
			stmt=con.prepareStatement("select * from member where memid=? and pw=? ");
			stmt.setString(1, id);
			stmt.setString(2, pw);
			
			rs=stmt.executeQuery();
			if(rs.next()) {
				
				String name=rs.getString("memname");
				
				return name;
			}
			return null;
		}catch(SQLException e) {
			e.printStackTrace();
			throw new MyException("로그인 실패");
		}finally {
			//6. 종료
			try {
				if(rs!=null) rs.close();
				if(stmt!=null) stmt.close();
				if(con!=null) con.close();
			} catch (SQLException e) {
				
			}
		}
		
	}

	public void deleteMember(String id) throws MyException {
		Connection con=null;
		PreparedStatement stmt=null;
		try {
			//2. Connection
			con=dbcp.getConnection();
			
			//3. Statement
			stmt=con.prepareStatement("delete from member where memid=? ");
			
			//4. SQL전송
			stmt.setString(1,id);	
			
			int i=stmt.executeUpdate();
			
			//5. 결과 확인
			System.out.println(i+"행이 delete되었습니다");
			
		} catch (SQLException e) {			
			e.printStackTrace();
			throw new MyException("회원 삭제 오류");
		} finally {
			//6. 종료
			try {
				if(stmt!=null) stmt.close();
				if(con!=null) con.close();
			} catch (SQLException e) {
				
			}
		}
	}
}








package server.dao;

import java.sql.*;
import java.util.ArrayList;

import common.entity.Member;
import common.util.CafeException;

public class MemberDAO {
	
	public 	MemberDAO() throws CafeException{		
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
		} catch (ClassNotFoundException e) {
			throw new CafeException("오라클 드라이버 등록 실패");
		}
		
	}
	
	public void insertMember(Member m) throws CafeException {
		Connection con=null;
		PreparedStatement stmt=null;
		
		try {			
			con=DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe","cafe","1234");
			stmt=con.prepareStatement("insert into member values(?,?,?,?,?)");
			stmt.setString(1, m.getMemId());
			stmt.setString(2, m.getName());
			stmt.setDate(3, new Date(m.getmDate().getTime()));
			stmt.setString(4, m.getPhone());
			stmt.setInt(5, m.getPoint());
			
			int i=stmt.executeUpdate();
			System.out.println(i+"행이 insert되었습니다");
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CafeException("insertMember 실패");
		} finally {
			try {
				if(stmt!=null) stmt.close();
				if(con !=null) con.close();
			} catch (SQLException e) {
				
			}
			
		}		
	}
	
	public ArrayList<Member> selectMember() throws CafeException {
		Connection con=null;
		PreparedStatement stmt=null;
		
		try {			
			con=DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe","cafe","1234");
			stmt=con.prepareStatement("select * from member");			
			ResultSet rs=stmt.executeQuery();
			ArrayList<Member> list=new ArrayList<Member>();
			while(rs.next()) {
				String id=rs.getString(1);
				String name=rs.getString(2);
				Date mDate=rs.getDate(3);
				String phone=rs.getString(4);
				int point =rs.getInt(5);
				Member m=new Member(id, name, mDate, phone, point);
				list.add(m);
			}
			return list;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CafeException("selectMember 실패");
		} finally {
			try {
				if(stmt!=null) stmt.close();
				if(con !=null) con.close();
			} catch (SQLException e) {
				
			}
			
		}	
	}
	
	public void updateMember() {
		
	}
	
	public void deleteMember() {
		
	}


}
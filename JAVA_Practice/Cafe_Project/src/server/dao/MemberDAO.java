package server.dao;

import java.sql.*;

import common.entity.Member;
import common.util.CafeException;

public class MemberDAO {
	Connection con = null;
	PreparedStatement pstmt=null;
	
	
	public MemberDAO () throws CafeException {

		try {
			getClass().forName("oracle.jdbc.driver.OracleDriver");
		} catch (ClassNotFoundException e) {
			throw new CafeException("����Ŭ ����̹� ��� ����");
		}

	}

	public void insertMember(Member m) throws CafeException  {
		try {
			con = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe","cafe","1234");
			pstmt = con.prepareStatement("insert into member values(?,?,?,?,?)");
			pstmt.setString(1,  m.getMemId());
			pstmt.setString(2,  m.getName());
			pstmt.setDate(3,  new Date(m.getmDate().getTime()));
			pstmt.setString(4,  m.getPhone());
			pstmt.setInt(5,  m.getPoint());
			
			int i = pstmt.executeUpdate();	//������ ������ ������ ���� ���� ���� �� ���� �� �� ���� ��ȯ
			System.out.println(i + "���� insert �Ǿ����ϴ�.");
			
		} catch (SQLException e) {
			throw new CafeException("insertMember ����");
		} finally {
			try {
				if(pstmt != null) pstmt.close();
				if(con != null) con.close();
			} catch (SQLException e) {
			
			}
			
		}
		
	}
	public void selectMember() {
			
		}
	public void updateMember() {
		
	}
	public void deleteMember() {
		
	}

}

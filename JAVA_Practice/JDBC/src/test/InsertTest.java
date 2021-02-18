package test;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class InsertTest {
	public static void main(String[] args) {
		Connection con= null;
		//Statement stmt= null;
		PreparedStatement pstmt=null;
		ResultSet rs= null;
		try {
			// 1. driver ���
			Class.forName("oracle.jdbc.driver.OracleDriver"); // ����Ŭ ����̹� ���
			System.out.println("driver ok");
			
			// 2. ����
			con=DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe", "cafe", "1234");	
											//parmeter (String url, String user, String password)
			System.out.println("con ok");
			
			// 3. Statement ���� 
			/*stmt=con.createStatement();
			System.out.println("stmt ok");*/
			
			pstmt = con.prepareStatement("insert into member(memid, memname, memdate, phone, point) values(?,?,?,?,?)");
			pstmt.setString(1, args[0]);
			pstmt.setString(2, args[1]);
			java.util.Date today=new java.util.Date();
			Date now= new Date(today.getTime());
			pstmt.setDate(3, now);
			pstmt.setString(4, args[2]);
			pstmt.setInt(5, 0);
			System.out.println("pstmt ok");
			
			
			// 4. SQL ����
			/*int i=stmt.executeUpdate("insert into membertbl values('"+args[0]+"','"+args[1]+"','"+args[2]+"')");
			// ���� �� DML ���� ������ int ������ return �� (������ 0)			//���� ���·� �Է� �޾� insert*/
			int j=pstmt.executeUpdate();	//	preparedstatement�� �̸� �غ��� �ξ��� ������ ȣ�⸸ �ϸ� ��
			
			

			// 5. ��� ���
			//System.out.println(i + "���� insert �Ǿ����ϴ�.");
			System.out.println(j + "���� insert �Ǿ����ϴ�.");
			
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				// 6. �ڿ� ����
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

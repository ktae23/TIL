package test;

import java.sql.*;

public class SelectWhereTest {
	public static void main(String[] args) {
		Connection con= null;
		Statement stmt= null;
		ResultSet rs= null;
		try {
			// 1. driver ���
			Class.forName("oracle.jdbc.driver.OracleDriver"); // ����Ŭ ����̹� ���
			System.out.println("driver ok");
			
			// 2. ����
			con=DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe", "shop", "1234");	
											//parmeter (String url, String user, String password)
			System.out.println("con ok");
		
			// 3. Statement ����
			stmt=con.createStatement();
			System.out.println("stmt ok");
			
			// 4. SQL ����
			rs=stmt.executeQuery("select * from membertbl where memberaddress like '%"+args[0]+"%'"); 
					// data�� '���'�� ���� �� �÷� ��������  // program arguments��  �Է� �� ������ ��ȸ �ϱ�
			System.out.println("rs ok");

			// 5. ��� ���
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
				
			// 6. �ڿ� ����
					try {
						if(rs !=null) rs.close();
						if(stmt !=null) stmt.close();
						if(con !=null) con.close();
						
					} catch (SQLException e) {
						
					}
		}
	}
}

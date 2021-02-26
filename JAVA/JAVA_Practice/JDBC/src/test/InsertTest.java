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
			// 1. driver 등록
			Class.forName("oracle.jdbc.driver.OracleDriver"); // 오라클 드라이버 경로
			System.out.println("driver ok");
			
			// 2. 연결
			con=DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe", "cafe", "1234");	
											//parmeter (String url, String user, String password)
			System.out.println("con ok");
			
			// 3. Statement 생성 
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
			
			
			// 4. SQL 전송
			/*int i=stmt.executeUpdate("insert into membertbl values('"+args[0]+"','"+args[1]+"','"+args[2]+"')");
			// 변경 된 DML 행의 개수를 int 값으로 return 함 (없으면 0)			//변수 형태로 입력 받아 insert*/
			int j=pstmt.executeUpdate();	//	preparedstatement에 미리 준비해 두었기 때문에 호출만 하면 됨
			
			

			// 5. 결과 얻기
			//System.out.println(i + "행이 insert 되었습니다.");
			System.out.println(j + "행이 insert 되었습니다.");
			
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

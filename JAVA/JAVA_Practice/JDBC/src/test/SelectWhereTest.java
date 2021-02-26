package test;

import java.sql.*;

public class SelectWhereTest {
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
			rs=stmt.executeQuery("select * from membertbl where memberaddress like '%"+args[0]+"%'"); 
					// data에 '경기'가 포함 된 컬럼 가져오기  // program arguments에  입력 된 값으로 조회 하기
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

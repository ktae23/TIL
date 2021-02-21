package common.entity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import common.util.StudentException;

public class StudentDTO {
	private int no;
	private String name;
	private String det;
	private String addr;
	private String tel;
	
	
	public StudentDTO() {}
	
	
	public StudentDTO(int no, String name, String det, String addr, String tel) throws StudentException {
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
		} catch (ClassNotFoundException e) {
			throw new StudentException("드라이버 등록 실패");
		}
		setNo(no);
		setName(name);
		setDet(det);
		setAddr(addr);
		setTel(tel);
	}
	


	public int getNo() {
		return no;
	}
	
	
	// 테이블에 no값이 있는지 체크하는 코드
	// DAO에서 전체 값 출력할때 생성자 사용하면서 의도치 않은 결과가 나와서 폐기
	
	/*public void setNo(int no) throws StudentException {
		Connection con=null;
		PreparedStatement pstmt=null;
		ResultSet rs=null;
		
		try {			
			con=DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe","Student","1234");
			pstmt=con.prepareStatement("select * from Student");
			rs=pstmt.executeQuery();
			
			ArrayList<Integer> list=new ArrayList<Integer>();
			 
			while(rs.next()) { 
				int tmpNo=rs.getInt(1);
				list.add(tmpNo);
			}   
			boolean is_no_exit = false;
			for(int i=0; i < list.size(); i++ ) {
				if (list.get(i) == no) {
					System.out.println("이미 존재하는 번호입니다.");
					System.out.println((list.get(list.size()-1) + 1) + "을 입력하세요.");
					is_no_exit = true;
				}
				if(is_no_exit == true)
					break;
			}
			if(is_no_exit == false && no > 0) {
				this.no = no;	
			}else if (no < 1){
				System.out.println("번호는 0보다 커야합니다.");
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			throw new StudentException("no 목록 가져오기 실패");
		} finally {
			try {
				if(rs != null) rs.close();
				if(pstmt!=null) pstmt.close();
				if(con !=null) con.close();
			} catch (SQLException e) {
				
			}
			
		}	
	}*/

	public void setNo(int no) {
		if(no > 0) {
			this.no = no;
		}else {
			System.out.println("번호는 0보다 작을 수 없습니다.");
			}
		}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		byte limit = 10;
		if(name.length() <= limit) {
			this.name = name;
		}else {
			System.out.println("이름은 10byte를 초과 할 수 없습니다.");
			}
		}

	public String getDet() {
		return det;
	}

	public void setDet(String det) {
		byte limit = 20;
		if(det.length() <= limit) {
			this.det = det;
		}else {
			System.out.println("전공명은 20byte를 초과 할 수 없습니다.");
		}
	}

	public String getAddr() {
		return addr;
	}

	public void setAddr(String addr) {
		byte limit = 80;
		if(addr.length() <= limit) {
			this.addr = addr;
		}else {
			System.out.println("주소는 80byte를 초과 할 수 없습니다.");
		}
	}

	public String getTel() {
		return tel;
	}

	public void setTel(String tel) {
		byte limit = 20;
		if(tel.length() <= limit) {
			this.tel = tel;
		}else {
			System.out.println("전화번호는 20byte를 초과 할 수 없습니다.");
		}
	}

 
	@Override
	public String toString() {
		return no + "\t" + name + "\t" + det + "\t" + addr + "\t" + tel;
	}




	

	
	
}
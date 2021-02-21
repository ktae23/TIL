package server.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import common.entity.StudentDTO;
import common.util.StudentException;

public class StudentDAO {
	
	
 
	public StudentDAO() throws StudentException{
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
		} catch (ClassNotFoundException e) {
			throw new StudentException("����̹� ��� ����");
		}
	
	}
	
	public void insertStudent(StudentDTO studentDTO) throws StudentException {
		Connection con=null;
		PreparedStatement pstmt=null;
		
		try {			
			con=DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe","Student","1234");
			pstmt=con.prepareStatement("insert into Student values(?,?,?,?,?)");
			pstmt.setInt(1, studentDTO.getNo());
			pstmt.setString(2, studentDTO.getName());
			pstmt.setString(3, studentDTO.getDet());
			pstmt.setString(4, studentDTO.getAddr());
			pstmt.setString(5, studentDTO.getTel());
			
			int i=pstmt.executeUpdate();
			System.out.println(i+"���� INSERT �Ǿ����ϴ�");
		} catch (SQLException e) {
			throw new StudentException("insert Student ����");
		} finally {
			try {
				if(pstmt!= null) pstmt.close();
				if(con != null) con.close();
			} catch (SQLException e) {
				
			}
			
		}		
	}
	
	public void printAllStudent() throws StudentException {
		Connection con=null;
		PreparedStatement pstmt=null;
		ResultSet rs=null;
		
		try {			
			con=DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe","Student","1234");
			pstmt=con.prepareStatement("select * from Student");	
			rs=pstmt.executeQuery();
			ArrayList<StudentDTO> list=new ArrayList<StudentDTO>();
			while(rs.next()) {
				int no=rs.getInt(1);
				String name=rs.getString(2); 
				String det=rs.getString(3);
				String addr=rs.getString(4);
				String tel =rs.getString(5);
				StudentDTO studentDTOList=new StudentDTO(no, name, det, addr, tel);
				list.add(studentDTOList);
			}  
			if (list.size() != 0 ) {
				for(StudentDTO ls : list) {
					System.out.println(ls.toString());
				}
			}else {
				System.out.println("���� �� ���� �����ϴ�.");
			}
			 
			
		} catch (SQLException e) {
			e.printStackTrace();
			throw new StudentException("select Student ����");
		} finally {
			try {
				if(rs != null) rs.close();
				if(pstmt!=null) pstmt.close();
				if(con !=null) con.close();
			} catch (SQLException e) {
				
			}
			
		}	
	}
	
}
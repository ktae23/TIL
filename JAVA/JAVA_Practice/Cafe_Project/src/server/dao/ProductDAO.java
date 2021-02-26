package server.dao;

import java.sql.*;
import java.util.ArrayList;

import common.entity.MemberDTO;
import common.entity.ProductDTO;
import common.util.CafeException;

public class ProductDAO {

	public ProductDAO() throws CafeException {
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
		} catch (ClassNotFoundException e) {
			throw new CafeException("드라이버 등록 실패");
		}
	}
	
	public void insertProduct(ProductDTO p) throws CafeException {
		Connection con=null;
		PreparedStatement stmt=null;
		try {
			con=DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe","cafe","1234");
			stmt=con.prepareStatement("insert into product(prodCode,prodName,price) values(?,?,?) ");
			stmt.setString(1,p.getProdCode());
			stmt.setString(2, p.getProdName());
			stmt.setInt(3, p.getPrice());
			int i=stmt.executeUpdate();
			System.out.println(i+"행이 insert되었습니다.");
		} catch (SQLException e) {			
			e.printStackTrace();
			throw new CafeException("상품 등록 실패");
		}finally {
			try {
				if(stmt!=null) stmt.close();
				if(con!=null) con.close();
			} catch (SQLException e) {
				
			}
		}
		
	}
	
	public ArrayList<ProductDTO> selectProduct() throws CafeException {
		Connection con=null;
		PreparedStatement stmt=null;
		
		try {			
			con=DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe","cafe","1234");
			stmt=con.prepareStatement("select * from product");			
			ResultSet rs=stmt.executeQuery();
			ArrayList<ProductDTO> list=new ArrayList<ProductDTO>();
			while(rs.next()) {
				String prodCode=rs.getString(1);
				String prodName=rs.getString(2);				
				int price =rs.getInt(3);
				ProductDTO p=new ProductDTO(prodCode, prodName, price);
				list.add(p);
			}
			return list;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CafeException("selectProduct 실패");
		} finally {
			try {
				if(stmt!=null) stmt.close();
				if(con !=null) con.close();
			} catch (SQLException e) {
				
			}
			
		}
	}
	
	public void updateProduct() {
		
	}
	
	public void deleteProduct() {
		
	}

	public String selectProduct(String prodCode) throws CafeException {
		Connection con=null;
		PreparedStatement stmt=null;
		
		try {			
			con=DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe","cafe","1234");
			stmt=con.prepareStatement("select prodName from product where prodCode=?");		
			stmt.setString(1, prodCode);
			ResultSet rs=stmt.executeQuery();
			
			if(rs.next()) {
				return rs.getString(1);				
			}else {
				return null;
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CafeException("selectProduct 실패");
		} finally {
			try {
				if(stmt!=null) stmt.close();
				if(con !=null) con.close();
			} catch (SQLException e) {
				
			}
			
		}
		
	}

}
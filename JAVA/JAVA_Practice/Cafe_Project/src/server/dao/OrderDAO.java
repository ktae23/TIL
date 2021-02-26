package server.dao;

import java.sql.*;

import common.entity.OrderDTO;
import common.util.CafeException;

public class OrderDAO {

	public OrderDAO() throws CafeException {
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
		} catch (ClassNotFoundException e) {
			throw new CafeException("드라이버 등록 실패");
		}
	}
	
	public int insertOrder(OrderDTO o) throws CafeException {
		Connection con=null;
		PreparedStatement stmt=null;
		try {
			con=DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe","cafe","1234");
			stmt=con.prepareStatement(
					"insert into orders(orderdate,ordermethod,memberid,prodcode,quantity) values(?,?,?,?,?)");
			java.util.Date now=new java.util.Date();
			o.setOrderDate(now);
			stmt.setDate(1,new Date(now.getTime()));
			stmt.setString(2, o.getOrderMethod());
			stmt.setString(3, o.getMemberId());
			stmt.setString(4, o.getProdCode());
			stmt.setInt(5, o.getQuantity());
			
			int i=stmt.executeUpdate();
			System.out.println(i+"행이 insert되었습니다.");
			int orderNo=selectOrder(o);
			return orderNo;
		} catch (SQLException e) {			
			e.printStackTrace();
			throw new CafeException("주문 실패");
		} finally {
			try {
				if(stmt!=null)stmt.close();
				if(con!=null)con.close();
			} catch (SQLException e) {
				
			}
		}
	}
	
	public int selectOrder(OrderDTO o) throws CafeException {
		Connection con=null;
		PreparedStatement stmt=null;
		ResultSet rs=null;
		
		try {
			con=DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe","cafe","1234");
			stmt=con.prepareStatement(
					"select orderNo from orders where orderDate=? and memberId=? and prodCode=? and quantity=? and orderMethod=?");
			stmt.setDate(1, new Date(o.getOrderDate().getTime()));
			stmt.setString(2,o.getMemberId());
			stmt.setString(3, o.getProdCode());
			stmt.setInt(4, o.getQuantity());
			stmt.setString(5, o.getOrderMethod());
			
			rs=stmt.executeQuery();
			if(rs.next()) {
				return rs.getInt(1);
			}else {
				return 0;
			}
		
		} catch (SQLException e) {			
			e.printStackTrace();
			throw new CafeException("주문 조회 실패");
		} finally {
			try {
				if(rs!=null)rs.close();
				if(stmt!=null)stmt.close();
				if(con!=null)con.close();
			} catch (SQLException e) {
				
			}
		}
		
	}
	
	
	

}
package kr.or.connect.jdbcexam.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import kr.or.connect.jdbcexam.dto.Role;

public class RoleDao {
	private static String dbUrl = "jdbc:mysql://localhost:3306/connectdb";
	private static String dbUser = "connectuser";
	private static String dbPasswd = "connect123!@#";
	
	public int addRoler(Role role) {
		int insertCount =0;
		
		Connection conn=null;
		PreparedStatement ps=null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(dbUrl, dbUser, dbPasswd);
			String sql= "insert into role (role_id,description) values(?,?)";
			ps = conn.prepareStatement(sql);
			ps.setInt(1, role.getRole_ID());
			ps.setString(2, role.getDescription());
			
			insertCount = ps.executeUpdate();
			
			
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			if(ps!=null)
				try {
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			if(conn!=null)
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
		}
		return insertCount;
	}
	
	
	public Role getRole(Integer role_Id) {
		
		Role role =null;
		Connection conn=null;
		PreparedStatement ps=null;
		ResultSet rs = null;
		
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(dbUrl, dbUser, dbPasswd);
			String sql= "select role_id,description from role where role_id=?";
			ps = conn.prepareStatement(sql);
			ps.setInt(1, role_Id);
			rs = ps.executeQuery();
			
			if(rs.next()) {
				int id = rs.getInt("role_id");
				String des = rs.getString(2);
				role = new Role(id, des);
			}
			
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			if(rs!=null)
				try {
					rs.close();
					
				} catch (SQLException e) {
					e.printStackTrace();
				}
			if(ps!=null)
				try {
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			if(conn!=null)
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			
		}
		
		return role;
	}
}

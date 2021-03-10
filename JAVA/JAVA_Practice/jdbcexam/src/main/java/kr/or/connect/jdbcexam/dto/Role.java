package kr.or.connect.jdbcexam.dto;

public class Role {
	private Integer role_ID;
	private String description;
	
	public Role() {}
	
	public Role(Integer role_ID, String description) {
		super();
		this.role_ID = role_ID;
		this.description = description;
	}

	public Integer getRole_ID() {
		return role_ID;
	}
	public void setRole_ID(Integer role_ID) {
		this.role_ID = role_ID;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return "Role [role_ID=" + role_ID + ", description=" + description + "]";
	}

	
	
	
}

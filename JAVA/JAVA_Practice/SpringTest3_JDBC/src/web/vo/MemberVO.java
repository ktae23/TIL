package web.vo;

import java.util.Date;

public class MemberVO {
	private String id,pw,name;
	private Date date;
		
	
	public MemberVO(String id, String pw, String name, Date date) {
		super();
		this.id = id;
		this.pw = pw;
		this.name = name;
		this.date = date;
	}
	public MemberVO() {
		super();
		// TODO Auto-generated constructor stub
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getPw() {
		return pw;
	}
	public void setPw(String pw) {
		this.pw = pw;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	@Override
	public String toString() {
		return "MemberVO [id=" + id + ", pw=" + pw + ", name=" + name + ", date=" + date + "]";
	}
	
	
	
}
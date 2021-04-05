package kr.co.bookmark.vo;


import java.util.Date;

import kr.co.bookmark.util.MyException;

public class MemberVO {
private String id;
private String pw;
private String name;
private Date date;
	


	@Override
	public String toString() {
	return "MemberVO [id=" + id + ", pw=" + pw + ", name=" + name + ", date=" + date + "]";
}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public MemberVO(String id, String pw, String name) throws MyException {
		
		this(id,pw);
		setName(name);
	}

	public MemberVO(String id, String pw) throws MyException {
		super();
		setId(id);
		setPw(pw);
	}

	public MemberVO() {
		super();
		// TODO Auto-generated constructor stub
	}

	public String getId() {
		return id;
	}

	public void setId(String id) throws MyException {
		if(id!=null) {
			this.id = id;
		}else {
			throw new MyException("id가 입력되지 않았습니다");
		}
	}

	public String getPw() {
		return pw;
	}

	public void setPw(String pw) throws MyException {
		if(pw!=null) {
			this.pw = pw;
		}else {
			throw new MyException("pw가 입력되지 않았습니다");
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) throws MyException {
		if(name!=null) {
			this.name = name;
		}else {
			throw new MyException("로그인 실패");
		}
	}


}

package com.mulcam.ai.web.vo;

import com.mulcam.ai.util.CafeException;

public class MemberVO {
	
	private String id,pw,name;
	
	public MemberVO(String id, String pw, String name) throws CafeException {
		this(id,pw);
		setName(name);
	}

	public MemberVO(String id, String pw) throws CafeException {
		super();
		setId(id);
		setPw(pw);
	}

	public MemberVO() {
		super();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) throws CafeException {
		if(id!=null) {
			this.id = id;
		}else {
			throw new CafeException("id가 입력되지 않았습니다.");
		}
	}

	public String getPw() {
		return pw;
	}

	public void setPw(String pw) throws CafeException {
		if(pw!=null) {
			this.pw = pw;
		}else {
			throw new CafeException("pw가 입력되지 않았습니다.");
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) throws CafeException {
		if(name!=null) {
			this.name = name;
		}else {
			throw new CafeException("name이 입력되지 않았습니다.");
		}
	}

	@Override
	public String toString() {
		return "MemberVO [id=" + id + ", pw=" + pw + ", name=" + name + "]";
	}	

}
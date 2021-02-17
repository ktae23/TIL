package common.entity;

import java.util.Date;

public class Member {
	private String memId;
	private String name;
	private Date mDate;
	private String phone;
	private int point = 0;
	
	public Member() {}
	
	public Member(String memId, String name, Date mDate, String phone) {
		super();
		this.memId = memId;
		this.name = name;
		this.mDate = mDate;
		this.phone = phone;
	}
	public String getMemId() {
		return memId;
	}
	public void setMemId(String memId) {
		if(memId != null) {
			this.memId = memId;	
		}else {
			System.out.println("ID�� null�� �� �� �����ϴ�.");
		}
		
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		if(name != null) {
			this.name = name;	
		}else {
			System.out.println("�̸��� null�� �� �� �����ϴ�.");
		}
	}
	public Date getmDate() {
		return mDate;
	}
	public void setmDate(Date mDate) {
		if(mDate != null) {
			this.mDate = mDate;	
		}else {
			System.out.println("���Խ����� null�� �� �� �����ϴ�.");
		}
	}
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		if(phone != null) {
			this.phone = phone;	
		}else {
			System.out.println("��ȭ��ȣ�� null�� �� �� �����ϴ�.");
		}
	}
	public int getPoint() {
		return point;
	}
	public void setPoint(int point) {
		this.point = point;
	}

	@Override
	public String toString() {
		return "Member [memId=" + memId + ", name=" + name + ", mDate=" + mDate + ", phone=" + phone + ", point="
				+ point + "]";
	}
	

	
	
}

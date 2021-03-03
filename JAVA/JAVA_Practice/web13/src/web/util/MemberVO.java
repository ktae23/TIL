package web.util;

public class MemberVO {
	private String id, name;
	
	public MemberVO(String id, String name) {
		super();
		setId(id);
		setName(name);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		if(id!=null)
			this.id = id;
		else
			System.out.println("id는 null이 될 수 없습니다.");
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		if(name!=null)
			this.name = name;
		else
			System.out.println("name은 null이 될 수 없습니다.");
	}
	
}

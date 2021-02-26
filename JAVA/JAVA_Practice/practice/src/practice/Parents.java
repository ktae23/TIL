package practice;

public class Parents {
	
	public Parents() {
		System.out.println("부모님 소환");
	}
	
	public Parents(int money, String familyname) {
		this.money=money;
		this.familyname=familyname;
		System.out.println("부모님 소환");
	}

	int money = 15000;
	String familyname = "Park";
	
	public void Study() {
		System.out.println("Study Hard");
	}
	
	
	public String showParentsInfo() {
		return familyname + "" + "\n가진 돈은 :" + money;
	}
	
}

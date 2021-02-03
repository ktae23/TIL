package test.encapsulation;

public class MyProfile {
	String name = "전은수";
	Mydate birthday = new Mydate();
	
	public void setBirthday() {
		birthday.setYear(1800000);
		birthday.setMonth(10);
		birthday.setDay(-3);

		System.out.println(birthday.getYear()+ "년" +
		birthday.getMonth() + "월" + birthday.getDay() + "일");
		
	}
}

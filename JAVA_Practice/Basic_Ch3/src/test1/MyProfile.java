package test1;

public class MyProfile {

	String name;
	int age;
	double tall;
	char gender;
	boolean isPretty;
	
	public void setProfile(String name1, int age1, double tall1, 
			char gender1, boolean isPretty1) {  //정의
		name = name1;
		age = age1;
		tall = tall1;
		gender = gender1;
	    isPretty = isPretty1;
	}
	
	public void printProfile() {

		System.out.println(name);
		System.out.println(age);
		System.out.println(tall);
		System.out.println(gender);
		System.out.println(isPretty);
}
/*	
	public void setProfile(String name1, int age1, double tall1, 
			char gender1, boolean isPretty1) {  //정의
		name = name1;
		age = age1;
		tall = tall1;
		gender = gender1;
	    isPretty = isPretty1;
		System.out.println(name);
		System.out.println(age);
		System.out.println(tall);
		System.out.println(gender);
		System.out.println(isPretty);
	}
	*/ //하나의 메서드에서는 하나의 기능만 하도록 구분이 필요. 메서드 이름으로 구분 할 수 있어야 함.

	
	
	MyProfile () {}
	
	MyProfile (String name, int age, double tall, char gender, boolean isPretty){
	this.name = name;
	this.age = age;
	this.tall = tall;
	this.gender = gender;
	this.isPretty = isPretty;
	}
}

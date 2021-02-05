package test.inheritance;

public class People {

	private String name,ssn;
	
	public People() {}
	
	public People(String name, String ssn) {
		super();
		this.name = name;
		this.ssn = ssn;
	}
	
	
class Student extends People{
	
	Student() {
		super();
	}
}
}

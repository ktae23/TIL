package stream.serialization;

import java.io.Serializable;

public class Person implements Serializable {

	private static final long serialVersionUID = -1503252402544036183L; //버전 관리를 위한 정보
	String name;
	String job;
	
	public Person() {}

	public Person(String name, String job) {
		this.name = name;
		this.job = job;
	}
	
	public String toString() {
		return name + "," + job;
	}
}

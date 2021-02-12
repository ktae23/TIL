package equals;

public class Student {
	int studentID;
	String studentName;
	
	public Student() {}
	
	public Student(int studentID, String stuentName) {
		this.studentID=studentID;
		this.studentName=studentName;
	}
	
	public String toStirng() {
		return studentID + "," + studentName;
	}
	
	
}
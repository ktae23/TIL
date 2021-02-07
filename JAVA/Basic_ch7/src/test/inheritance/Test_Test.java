package test.inheritance;

public class Test_Test {
	public static void main(String[] args) {
	Person p = new Student("박경태");
	System.out.println(p.name);
	Student s = (Student)p;
	System.out.println(s.name);
	}
}

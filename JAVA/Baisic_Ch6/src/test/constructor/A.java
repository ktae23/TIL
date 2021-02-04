package test.constructor;

public class A extends Object{
	
	public A() {
		super();
		System.out.println("A() 생성자 호출");
	}
	
	public A(int i) {
		super();
		System.out.println("A(int i) 생성자 호출");
	}
	
	public A(int i, String s) {
		super();
		System.out.println("A(int i, String s) 생성자 호출");
	}

	public static void main(String[] args) {
		A a1 = new A();
		A a2 = new A(10);
		A a3 = new A(20,"java");
		
	}
	
	
}

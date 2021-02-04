package test.inheritance;

public class B extends A{
	
	public int x=1000;
	public int i=100;

	public void printX() {
		System.out.println("B의 x ="+ x);
	}
	
	public void printI() {
		System.out.println("B의 i ="+ i);
	}
	
	public B () {
		super();
	}
}

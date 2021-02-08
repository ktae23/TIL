package test.constructor;

public class A extends Object{
	private int i;
	private String s;		
	public int getI() {
		return i;
	}
	public void setI(int i) {
		this.i = i;
	}
	public String getS() {
		return s;
	}
	public void setS(String s) {
		this.s = s;
	}
	public A() {
		super();
		System.out.println("A() �깮�꽦�옄 �샇異�");
	}	
//	public A(int i) {
//		super();
//		System.out.println("A(int i) �깮�꽦�옄 �샇異�");
//	}	
	public A(int i, String s) {
		super();
		setI(i);
		setS(s);
		System.out.println("A(int i, String s) �깮�꽦�옄 �샇異�");
	}
	public static void main(String[] args) {		
		A a3=new A(10,"java");
		
	}

}

package test.equality;

public class Test {

	public static void main(String[] args) {
		byte b=1;
		int i=1;
		double d=1.0;
		System.out.println(i==d);
		
		Test t1=new Test();
		Test t2=new Test();
		
		System.out.println(t1==t2);
		System.out.println(t1.equals(t2));
		
	}
}

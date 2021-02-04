package test.inheritance;

public class Test extends Object{

	public static void main(String[] args) {
		Object obj_O = new Object();
		System.out.println(obj_O.toString());
		
		
		A obj_A=new A();
		
		obj_A.i++;
		obj_A.printI();
		System.out.println(obj_A);		
		System.out.println(obj_A.toString()); //주소값	
		

		
		B obj_B=new B();
		
		obj_B.x++;
		obj_B.printX();
		obj_B.i++;
		obj_B.printI();
		System.out.println(obj_B);		
		System.out.println(obj_B.toString()); //주소값

	
	}
}

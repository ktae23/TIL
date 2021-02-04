package test.datatype;

public class Test {

	byte bNum;
	short sNum;
	int iNum;
	long lNum;
	float fNum;
	double dNum;
	char C;
	boolean bool;
	String str;
	
	
	
	public static void main(String[] args) {
		
		Test t1 = new Test();
		
		System.out.println(t1.bNum);
		System.out.println(t1.sNum);
		System.out.println(t1.iNum);
		System.out.println(t1.lNum);
		System.out.println(t1.fNum);
		System.out.println(t1.dNum);
		System.out.println(t1.C);
		System.out.println((int)t1.C);
		System.out.println(t1.bool);
		System.out.println(t1.str);
		
		


	}
}

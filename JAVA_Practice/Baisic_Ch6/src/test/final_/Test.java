package test.final_;

public class Test {
	public static void main(String[] args) {
		
	
	Object o1=new Object();
	System.out.println(o1.toString());
	Object o2=new Object();
	System.out.println(o2.toString());
	System.out.println(o1==o2);
	System.out.println(o1.equals(o2));
	
	Test t1=new Test();
	System.out.println(t1.toString());
	Test t2=new Test();
	System.out.println(t2.toString());
	System.out.println(t1==t2);
	System.out.println(t1.equals(t2));
	
	String s1=new String("java");
	System.out.println(s1.toString());
	String s2=new String("java");
	System.out.println(s2.toString());
	System.out.println(s1==s2);
	System.out.println(s1.equals(s2));
	
	String s3="java";
	System.out.println(s3.toString());
	String s4="java";
	System.out.println(s4.toString());
	System.out.println(s3==s4);
	System.out.println(s3.equals(s4));
	
	
	}
}

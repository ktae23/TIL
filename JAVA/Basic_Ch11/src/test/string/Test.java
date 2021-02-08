package test.string;

public class Test {

	public static void main(String[] args) {
		String s1=new String("java");
		String s2=new String("java");
		String s3="java";
		String s4="java";
		String s5=s1.concat("1");
		
		s2.concat("1");
		s3.concat("1");
		s4.concat("1");
		
		System.out.println(s1);
		System.out.println(s2);
		System.out.println(s3);
		System.out.println(s4);
		System.out.println(s5==s1);
	}
}

package test.string;
public class Test {
	public static void main(String[] args) {
		String s1=new String("java");		
		String s3="java";
		
		StringBuffer sb1=new StringBuffer("java");
		StringBuffer sb2=sb1.append("1");
		
		System.out.println(sb1);
		System.out.println(sb2);
		System.out.println(sb1==sb2);
	}
}

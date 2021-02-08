package test.datatype;

public class StringTest {

	public static void main(String[] args) {
		String s1 = new String("java");
		String s2 = new String("java");
		System.out.println(s1);
		System.out.println(s2);
		
		String s3="java";
		String s4="java";
		System.out.println(s3);
		System.out.println(s4);
		
		if (s1==s2) {
			System.out.println("1과 2는 같음(같은 메모리 주소)");
		}
		if(s1==s3) {
			System.out.println("1과 3은 같음(같은 메모리 주소)");
		}
		if(s1==s4) {
			System.out.println("1과 4는 같음(같은 메모리 주소)");
		}
		if(s2==s3) {
			System.out.println("2와 3은 같음(같은 메모리 주소)");
		}
		if(s2==s4) {
			System.out.println("2와 4는 같음(같은 메모리 주소)");
		}
		if(s3==s4) {
			System.out.println("3과 4는 같음(같은 메모리 주소)");
		}
		
		
		if (s1.equals(s2)) {
			System.out.println("1과 2는 같은 값(문자열)");
		}
		if (s1.equals(s3)) {
			System.out.println("1과 3은 같은 값(문자열)");
		}
		if (s1.equals(s4)) {
			System.out.println("1과 4는 같은 값(문자열)");
		}
		if (s2.equals(s3)) {
			System.out.println("2와 3은 같은 값(문자열)");
		}
		if (s2.equals(s4)) {
			System.out.println("2와 4는 같은 값(문자열)");
		}
		if (s3.equals(s4)) {
			System.out.println("3과 4는 같은 값(문자열)");
		}
	}

}

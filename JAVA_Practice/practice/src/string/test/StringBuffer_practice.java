
public class StringBuffer_practice {

	public static void main(String[] args) throws ClassNotFoundException {

		Integer number1 = Integer.valueOf("100");
		Integer number2 = Integer.valueOf(100);
		
		System.out.println(number1);
		System.out.println(System.identityHashCode(number1));
		
		
		System.out.println(number2);
		System.out.println(System.identityHashCode(number2));

		Class c = Class.forName("java.lang.String");
		System.out.println(c);
	}
}

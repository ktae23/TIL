package test.exception;

public class Test {

	public static void main(String[] args) {
		
		Calculator c=new Calculator();
		int result = 0;
		try {
			result = c.divide(100, 0);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		System.out.println(result);
		System.out.println("아주 중요한일 시작..");
	}
	
}

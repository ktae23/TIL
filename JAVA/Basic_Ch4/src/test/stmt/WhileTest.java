package test.stmt;

public class WhileTest {

	public static void main(String[] args) {
		/*data
		while(조건) {
			실행문;
			증감치;
		}*/
		int i =0;
		while(i<100) {
			System.out.println(i);
			i++;
		}
		System.out.println("i=" + i);
	
	}
}

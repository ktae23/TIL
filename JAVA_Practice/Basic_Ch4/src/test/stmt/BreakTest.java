package test.stmt;

public class BreakTest {

	public static void main(String[] args) {
		int i=0;
		for(;i<10;i++) {
			if(i==5) {
				break;
			}
			System.out.println(i);
		}
		System.out.println("i="+ i);
	}
}

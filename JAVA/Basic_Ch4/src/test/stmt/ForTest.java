package test.stmt;

public class ForTest {

	public static void main(String[] args) {
		/*
		for(data;조건식;증감치) {
			실행문;
		}
		data
		for(data초기화;조건식;증감치) {
			실행문;
		}
		*/
/*		int i;
		for(i=0;i<100;i++) {
			System.out.println(i);
		}
		System.out.println("i=" + i);
*/
		int i=0;
		for(;i<100;i++) { //for문 밖에서 초기화해서 생략.
			System.out.println(i);
		}
		System.out.println("i=" + i);
	
	}
}

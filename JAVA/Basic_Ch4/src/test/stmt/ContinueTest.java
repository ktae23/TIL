package test.stmt;

public class ContinueTest {

	public static void main(String[] args) {
		int i=0;
		for(;i<10;i++) {
			if(i==5) {
				continue; // i가 5일 경우 남은 코드를 수행하지 않고 즉시 반복문 코드를 진행함.
			}
			System.out.println(i);
		}
		System.out.println("i="+ i);
	}
}

package test.stmt;

public class IfTest {

	public static void main(String[] args) {
		int month=10;
		if(month==1 || month==3 || month==5 || month==7 || month==8 || month==10 || month==12) {
			System.out.println(month + "월은 31일까지 있습니다.");
		}else if(month==4 || month==6 || month==8 || month==11) {
			System.out.println(month + "월은 30일까지 있습니다.");
		}else if(month==2) {
			System.out.println(month + "월은 28일까지 있습니다.");
		}else {
			System.out.println("잘못된 입력입니다.");
		}
	}
}

package multiple_choice;

import javax.swing.plaf.synth.SynthSeparatorUI;

public class Constructor_Test {

	public static void main(String[] args) {
//		TestA a = new TestA();// 생성자 매개변수가 없고 기본 생성자가 구현되지 않아서 컴파일 오류
//		a.i = 200;
//		a.go();
	}
	
class TestA{
	
	public int i = 100;
	public TestA(int i) {
		this.i = i;
	}
	public void go() {
		System.out.println("i= " + i);
		}
	}
}

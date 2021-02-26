package interface_practice;

public class Calculator implements Calc {

	@Override
	public int add(int num1, int num2) {
		return num1 + num2;
	}

	@Override
	public int substract(int num1, int num2) {
		return num1 - num2;
	}

	@Override
	public int times(int num1, int num2) {
		return num1 * num2;
	}

	@Override
	public int divide(int num1, int num2) {
		if(num2 !=0)
		return num1 / num2;
		else
			return Calc.ERROR; // 0으로 나눌 경우 에러 출력
	}
	
	public void showInfo() {
		System.out.println("인터페이스를 구현하였습니다.");
	}

}
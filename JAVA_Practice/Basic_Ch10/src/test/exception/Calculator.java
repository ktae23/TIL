package test.exception;

public class Calculator {

	public int divide(int x, int y) throws MyException {
		int z=0;
		if(y==0) {
			throw new MyException("y값을 0으로 입력하지 마세요");
		}else {
			z=x/y;
		}
		return z;
	}
}

package test;

public class Calculator {

	int add(int i, int j ) {
		return i+j;
	}
	
	int substract(int i, int j) {
		return i - j;
	}
	
	int mulutiply(int i, int j) {
		return i * j;
	}
	int divide(int i, int j) {
		return i / j;
	}
	
	public static void main(String[] args) {
		Calculator cal = new Calculator();
		System.out.println(cal.add(3,4));
		System.out.println(cal.substract(5, 4));
		System.out.println(cal.mulutiply(2, 6));
		System.out.println(cal.divide(8, 4));
	}
}

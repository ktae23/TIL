package test2.operator;

public class Calculator {
	
	int sum;
	
	public void add(int a, int b) {
		System.out.println(a+b);
	}
	
	public void mod(int a, int b) {
		System.out.println(a%b);
	}
		
	public void sum(int a) {
		sum += a;
		System.out.println(sum);
	}
	
	public void multiple(int a, int b) {
		System.out.println(a*b);
					
	}
	
}

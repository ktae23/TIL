package com.spring.ex01;

public class Calculator {
	public void add(int x, int y) {
		int result=x+y;
		System.out.println("���:"+ result);
	}

	public void subtract(int x, int y) {
		int result=x - y;
		System.out.println("���:"+ result);
	}

	public void multiply(int x, int y) {
		int result=x * y;
		System.out.println("���:"+ result);
	}

	public void divide(int x, int y) {
		int result=x / y;
		System.out.println("���:"+ result);
	}
}

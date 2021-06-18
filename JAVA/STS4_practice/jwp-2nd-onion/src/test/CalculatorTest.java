package test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CalculatorTest {
	
	private Calculator cal;
	
	// 각 테스트 실행 전에 단 한번 실행
	@BeforeEach
	public void setup() {
		cal = new Calculator();
		System.out.println("before");
	}
	@Test
	public void add() {
		assertEquals(9, cal.add(6, 3));
		System.out.println("더하기 : " +  cal.add(6, 3));
	}
	@Test
	public void subtract() {
		assertEquals(3, cal.subtract(6, 3));
		System.out.println("빼기 : " +  cal.subtract(6, 3));
	}
	@Test
	public void multiply() {
		assertEquals(18, cal.multiply(6, 3));
		System.out.println("곱하기 : " +  cal.multiply(6, 3));
	}
	@Test
	public void divide() {
		assertEquals(2, cal.divide(6, 3));
		System.out.println("나누기 : " +  cal.divide(6, 3));
	}
    // 각 테스트 실행 후에 단 한번 실행
	@AfterEach
	public void teardown() {
		System.out.println("teardown");
	}
}
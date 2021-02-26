package template;

public class Test {

	public static void main(String[] args) {
		System.out.println("=== 자율 주행하는 자동차 ===");
		Car myCar = new AICar();
		myCar.run();
		
		System.out.println("=== 사람이 운전하는 자동차 ===");
		Car yourCar = new ManualCar();
		yourCar.run();
		
	}
}

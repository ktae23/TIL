package my.fly;

public class Car extends Vehicle{

	public void drive(String from, String to) {
		int distance = transfer(10,70);
		System.out.println(distance + "만큼 달립니다...");
	}

}

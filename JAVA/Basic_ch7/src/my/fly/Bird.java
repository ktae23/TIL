package my.fly;

public class Bird extends Animal{

	@Override
	public void eat() {
		System.out.println("벌레를 잡아 먹고 산다...");
	}

	public void fly() {
		System.out.println("날개를 펄럭이며 난다...");
	}
}

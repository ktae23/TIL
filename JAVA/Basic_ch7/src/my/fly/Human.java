package my.fly;

public class Human extends Animal{
	
	@Override
	public void eat() {
		System.out.println("밥을 먹고 산다...");
	}
	
	public void walk() {
		System.out.println("직립 보행한다...");
	}

}

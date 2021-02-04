package my.fly;

public class Superman extends Human {

	@Override
	public void eat() {
		super.eat();
		System.out.println("빵도 먹는다...");
	}
	public void flying() {
		System.out.println("망토를 휘날리며 난다...");
	}


}

package design.adapter;

public class HairDryer implements Electronic110V{
	
	@Override
	public void powerOn() {
		System.out.println("헤어 드라이기 110V on");
	}
}

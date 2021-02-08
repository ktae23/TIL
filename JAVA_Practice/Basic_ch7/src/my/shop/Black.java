package my.shop;

public class Black extends Customer{
	
	@Override
	public void calcPoint() {
		System.out.println("100포인트 차감되었습니다.");
	}
}

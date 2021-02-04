package my.shop;

public class VIP extends Customer{

	@Override
	public void calcPoint() {
		System.out.println("100포인트 추가되었습니다.");
	}

}

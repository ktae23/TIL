package my.shop;

public class Monitor {

	public void monitorPoint(Customer c) {
		c.calcPoint();
	}
	
	public void monitorPoint(VIP c) {
		c.getAddress();
	}
	
	public void monitorPoint(Black c) {
		c.getCustNo();
	}
	
	
	
}

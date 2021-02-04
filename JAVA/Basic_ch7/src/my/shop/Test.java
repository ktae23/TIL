package my.shop;

public class Test {

	public static void main(String[] args) {

		VIP cust1=new VIP();
		Guest cust2=new Guest();
		Black cust3=new Black();
		
		Monitor m=new Monitor();
		m.monitorPoint(cust1);
		m.monitorPoint(cust2);
		m.monitorPoint(cust3);
	}

}

package example;

public class Account {
	public Account() {}

	public static final int MIN_BALANCE=0;
	public static final int MAX_BALANCE=1000000;
	private int balance = 0;

	public int getBalance() {
		return balance;
	}

	public void setBalance(int money) {
		if (MIN_BALANCE <= balance + money 
				&& MAX_BALANCE >= balance + money) {
			this.balance += money;
		}
		else if (balance + money >= 1000000) {
			System.out.println("잔액이 100만원을 넘을 수 없습니다.");
		}
		else {
			System.out.println("잔고가 부족합니다.");
			return;
		}
	}
	
	
}

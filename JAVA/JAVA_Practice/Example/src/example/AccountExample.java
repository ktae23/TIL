package example;

public class AccountExample {

	public static void main(String[] args) {
		Account account = new Account();
		
		account.setBalance(10000);
		System.out.println("현재 잔고: " + account.getBalance());
		
		account.setBalance(-5000);
		System.out.println("현재 잔고: " + account.getBalance());
		
		account.setBalance(-6000);
		System.out.println("현재 잔고: " + account.getBalance());
		
		
		account.setBalance(3000000);
		System.out.println("현재 잔고: " + account.getBalance());
		
		
	}
}

package test.thread;

public class Test {

	public static void main(String[] args) {
		A t1=new A();
		A t2=new A();
		
		t1.start();
		t2.start();
	}
}	
	class A extends Thread{
		public void run( ) {
			for(int i=0; i<100; i++) {
				System.out.println(i+"+1="+(i+1));
			}
		}
	}
		


package design;

import design.singlton.Aclass;
import design.singlton.Bclass;
import design.singlton.SocketClient;

public class Main {

	public static void main(String[] args) {
		
		Aclass aclass = new Aclass();
		Bclass bclass = new Bclass();
		
		SocketClient aClient = aclass.getSocketClient();
		SocketClient bClient = bclass.getSocketClient();
		
		System.out.println("두 객체가 동일한가?");
		System.out.println(aClient.equals(bClient));
	}

}

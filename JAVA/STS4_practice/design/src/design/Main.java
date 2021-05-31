package design;

import design.adapter.AirConditioner;
import design.adapter.Cleanner;
import design.adapter.Electronic110V;
import design.adapter.HairDryer;
import design.adapter.SocketAdapter;
import design.singlton.Aclass;
import design.singlton.Bclass;
import design.singlton.SocketClient;

public class Main {

	public static void main(String[] args) {
		/*
		//싱글톤
		Aclass aclass = new Aclass();
		Bclass bclass = new Bclass();
		
		SocketClient aClient = aclass.getSocketClient();
		SocketClient bClient = bclass.getSocketClient();
		
		System.out.println("두 객체가 동일한가?");
		System.out.println(aClient.equals(bClient));
		*/
		
		HairDryer hairDryer = new HairDryer();
		connect(hairDryer);
		
		Cleanner cleanner = new Cleanner();
//		connect(cleanner);
		
		Electronic110V adapter = new SocketAdapter(cleanner);
		connect(adapter);
		
		AirConditioner airConditioner = new AirConditioner();
		Electronic110V airAdapter = new SocketAdapter(airConditioner);
		connect(airAdapter);

	}
	
	//어댑터
	public static void connect(Electronic110V electronic110v) {
		electronic110v.powerOn();
	}

}

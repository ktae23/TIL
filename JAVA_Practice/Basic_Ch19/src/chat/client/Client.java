package chat.client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {

	public static void main(String[] args) {
		try {
			Socket s=new Socket("localhost",1234);//호스트 이름, 번호
			System.out.println("server connect ok");
			
			// 데이터 보내는 스트림
			DataOutputStream out=new DataOutputStream(s.getOutputStream());
			out.writeUTF("hello?");//UTF 문자열 작성
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
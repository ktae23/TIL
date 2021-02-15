package server;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.*;

public class Server {

	public static void main(String[] args) {
		try {
			ServerSocket ss=new ServerSocket(1234); //1024번 이전 번호는 대부분 사용 중. 이후로 사용
			Socket s=ss.accept(); //소켓 대기
			System.out.println(s.getInetAddress()+"connect ok!!!!");  //소켓 접속한 클라이언트 주소 얻는 메서드
			
			DataInputStream in=new DataInputStream(s.getInputStream()); //데이터 입력 받는 데이터 스트림
			String msg=in.readUTF();
			System.out.println(msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
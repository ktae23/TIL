package chat.server;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
	
	public void chatProcess() {
		try {
			ServerSocket ss=new ServerSocket(9999);//0~65565 1024번까지는 시스템 포트 21:FTP 23:telnet 25:SMTP 80:HTTP
			System.out.println("server ready...");
			Socket s=ss.accept();
			System.out.println(s.getInetAddress()+"님 접속");
			DataInputStream in=new DataInputStream(s.getInputStream());
			
			ServerThread t=new ServerThread(in);
			t.start(); // Thread 호출
			String msg=in.readUTF();
			System.out.println(msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	class ServerThread extends Thread{
		DataInputStream in;
		public ServerThread(DataInputStream in) {
			this.in=in;
		}
		@Override
		public void run() {
			String msg = "";
			try {
				msg = in.readUTF();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println(msg);
		}
	}
	public static void main(String[] args) {

	}
}
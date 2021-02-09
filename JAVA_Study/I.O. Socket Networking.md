## 소켓 네트워킹

클라이언트

```java
package client;

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
```



서버

```java
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
```



> main 순서대로 실행하다 보면  accept가 지나기 때문에 한명 이상의 접속이 안 된다.
>
> 때문에 Multi Thread 작업을 통해 Multi Accept가 되도록 해야 한다.


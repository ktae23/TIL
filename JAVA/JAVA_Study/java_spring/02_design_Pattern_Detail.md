## 디자인 패턴_Detail

### Singleton Pattern

- 어떠한 클래스(객체)가 유일하게 1개만 존재 할 때 사용한다.
- 실제 사물로는 여러 PC가 공유해서 사용 하는 프린터를 예로 들 수 있음
- 이를 주로 사용하는 곳은 TCP Socket 통신에서 서버와 연결 된 connect 객체에 주로 사용

- Aplication Context를 이용 해 Spring의 Bean을 싱글톤으로 관리 함

<br/>

##### 싱글톤 생성

```java
package design.singlton;

public class SocketClient {
	
	// static으로 인스턴스를 만들어 어디에서든 접근 할 수 있음
	// 하지만 private이기 때문에 직접 사용은 불가
	private static SocketClient socketClient = null;
	
	// 생성자는 private이 기본 설정
	private SocketClient() {
		
	}
	
	// static으로 getInstance 메서드를 만들어 어디에서든 호출 할 수 있음
	public static SocketClient getInstance() {
		// socketClient 인스턴스가 null이라면 생성하고 아니면 return
		if(socketClient == null) {
			socketClient = new SocketClient();
		}
		return socketClient;
		
	}
}

```

<br/>

##### AClass

```java
package design.singlton;

public class Aclass {
	private SocketClient socketClient;
	
	public Aclass() {
		this.socketClient = SocketClient.getInstance();
	}
	
	public SocketClient getSocketClient() {
		return this.socketClient;
	}
}
```

<br/>

##### BClass

```java
package design.singlton;

public class Bclass {
	private SocketClient socketClient;
	
	public Bclass() {
		this.socketClient = SocketClient.getInstance();
	}
	
	public SocketClient getSocketClient() {
		return this.socketClient;
	}
}
```

<br/>

##### Main

```java
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
```

<br/>


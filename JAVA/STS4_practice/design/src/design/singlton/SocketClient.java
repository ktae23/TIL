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

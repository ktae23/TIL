package example6;

public class Chatting {

	void startChat(String chatId) { //내부 중첩 메서드
		String nickName = chatId;	// nickName 변수 초기화 (로컬 클래스 변수라서 final 특성)
		Chat chat = new Chat() {	//내부 중첩 클래스 Chat 타입 chat 인스턴스 생성
									//start()메서드, sendMessage()메서드 호출 가능 
		 public void start() { //Chat 클래스의 start 재정의
			 while(true) {
				 String inputData = "안녕하세요";
				 String message = "[" + nickName + "]" + inputData;
				 					//[chatID] 안녕하세요.
				 sendMessage(message);
			 }
		 }
		};
		chat.start();
	}
	
	class Chat {   //내부 인스턴스 클래스
		void start() {}	//추상 메서드
		void sendMessage(String message) {}	// 추상 메서드
	}
}

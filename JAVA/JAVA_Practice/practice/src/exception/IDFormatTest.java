package exception;

public class IDFormatTest {
	public static void main(String[] args) {
		IDFormat test = new IDFormat();
		
		// 아이디가 null 값일 경우
		String userID = null;
		try {
			test.setUserID(userID);
		}catch(IDFormatException e) {
			System.out.println(e.getMessage());
		}
		
		// 아이디가 8글자 이하일 경우
		userID = "1234567";
		try {
			test.setUserID(userID);
		}catch(IDFormatException e) {
			System.out.println(e.getMessage());
		}
		
	}
}

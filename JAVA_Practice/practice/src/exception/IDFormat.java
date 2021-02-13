package exception;

public class IDFormat {
	
	private String userID;
	
	public String getUserID() {
		return userID;
	}
	
	public void setUserID(String userID) throws IDFormatException {
		if(userID == null ) {
			throw new IDFormatException("아이디칸을 비워둘 수 없습니다.");
		}else if(userID.length() < 8 || userID.length() > 20) {
			throw new IDFormatException("아이디는 8글자 이상 20글자 이하로 사용해주세요");
		}
		this.userID=userID;
	}
	
}

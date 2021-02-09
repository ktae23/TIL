package imsi;

public class Test {	// 아웃 클래스 시작
	public static void main(String[] args) { //메서드 블럭 시작
		int out=10; // 로컬클래스에서 사용하는 값은 final이어야 하지만 자바 8부터는 명시하지 않아도 자동 적용 됨

	class Inner{	//이너 클래스 시작
		public int count() {
			int cnt=0;
			for(int i=0;i<out;i++) {
				cnt +=i;
			}
			return cnt;
		}
	}	// 이너 클래스 끝
		Inner a=new Inner();
		System.out.println(a.count());
		
	} // 메서드 블럭 끝

}// 아웃 클래스 끝

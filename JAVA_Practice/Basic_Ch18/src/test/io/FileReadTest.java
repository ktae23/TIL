package test.io;
import java.io.*;


public class FileReadTest {

	public static void main(String[] args) {
		try {
			FileInputStream finput = new FileInputStream("C:\\Users\\zz238\\Desktop\\a.txt");
			int i = finput.read();
			System.out.println(i);
		
		
		} catch (FileNotFoundException e) {
			e.printStackTrace(); // 실행시점에선 삭제 해야하지만 개발 시점에선 에러 추적을 위해 사용
		} catch (IOException e) {
			e.printStackTrace(); // 실행시점에선 삭제 해야하지만 개발 시점에선 에러 추적을 위해 사용
		}
	}
}

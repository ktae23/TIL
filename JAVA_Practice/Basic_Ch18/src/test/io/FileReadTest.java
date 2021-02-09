package test.io;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


public class FileReadTest {

	public static void main(String[] args) {
		
		FileReader fr = null;
		BufferedReader br = null;
		
		try {
			fr = new FileReader("C:\\Users\\zz238\\Desktop\\a.txt");
			br=new  BufferedReader(fr);
			String oneLine="";
			
			while((oneLine = br.readLine()) != null) {
				System.out.println(oneLine);
			}
		
		
		} catch (FileNotFoundException e) {
			e.printStackTrace(); // 실행시점에선 삭제 해야하지만 개발 시점에선 에러 추적을 위해 사용
		} catch (IOException e) {
			e.printStackTrace(); // 실행시점에선 삭제 해야하지만 개발 시점에선 에러 추적을 위해 사용
		}
		finally {
			try {
					// 실행문 하나일때 중괄호 생략 가능
			if(br != null) br.close(); //null이 아닐 경우 생성의 역순으로 종료
			if(fr != null) fr.close(); //null이 아닐 경우 생성의 역순으로 종료
			
			}catch(IOException e){
				
			}
		}
	}
}

package stream.inputstream;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileInputStreamTest2 {

	public static void main(String[] args) {
		try(FileInputStream fis = new FileInputStream("input.txt")){  //try-with-resources 문 사용
			int i;
			while((i = fis.read()) != -1 ) {	// 읽을 값이 없으면 정수 -1을 리턴함
				System.out.println((char)i);
			}
			System.out.println("end");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

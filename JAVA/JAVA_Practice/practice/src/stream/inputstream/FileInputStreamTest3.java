package stream.inputstream;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileInputStreamTest3 {

	public static void main(String[] args) {
		try(FileInputStream fis = new FileInputStream("input.txt")){  //try-with-resources 문 사용
			byte[] bs = new byte[10];  //한번에 10바이트씩 배열단위로 읽어 오겠다.
			int i;
			// 바이트 배열 bs를 매개변수로 받음 
			while((i = fis.read(bs)) != -1 ) {	//읽을 값이 없으면 정수 -1을 리턴함
				for(int k = 0; k<i; k++) {
					System.out.print((char)bs[k]);
				}
				System.out.println(": " + i + "바이트 읽음");
			}
			} catch (IOException e) {
				e.printStackTrace();
			}
		System.out.println("end");
	}
}

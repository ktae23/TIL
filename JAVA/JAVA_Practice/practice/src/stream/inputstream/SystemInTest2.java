package stream.inputstream;

import java.io.IOException;

public class SystemInTest2 {

	public static void main(String[] args) throws IOException{
		
		System.out.println("알파벳을 여러개 를 쓰고 [Enter]를 누르세요");
		
		int i;
		try {
			while((i = System.in.read()) != -1) {
			System.out.print((char)i);
			}
			}catch(IOException e) {
			e.printStackTrace();
			}
	}
}

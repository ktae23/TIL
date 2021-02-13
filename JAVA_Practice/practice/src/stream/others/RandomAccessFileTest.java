package stream.others;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class RandomAccessFileTest {

	public static void main(String[] args) throws IOException {
		RandomAccessFile rf = new RandomAccessFile("random.txt", "rw");
		rf.writeInt(100);
		System.out.println("파일 포인터 위치:" + rf.getFilePointer()); //파일 포인터 위치 반환

		rf.writeDouble(3.14);
		System.out.println("파일 포인터 위치:" + rf.getFilePointer()); //파일 포인터 위치 반환
		
		rf.writeUTF("안녕하세요");
		System.out.println("파일 포인터 위치:" + rf.getFilePointer()); //파일 포인터 위치 반환
		
		rf.seek(0);	//파일 포인터 위치 매개변수 위치로 이동
		
		int i = rf.readInt();
		double d = rf.readDouble();
		String str = rf.readUTF();
		
		System.out.println("파일 포인터 위치:" + rf.getFilePointer());
		System.out.println(i);
		System.out.println(d);
		System.out.println(str);
	}
}

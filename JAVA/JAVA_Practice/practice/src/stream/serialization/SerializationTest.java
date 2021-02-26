package stream.serialization;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SerializationTest {

	public static void main(String[] args) throws ClassNotFoundException {
		Person personAhn = new Person("안재용", "대표이사");
		Person personKim = new Person("김철수", "상무이사");
		
		try(FileOutputStream fos = new FileOutputStream("serial.tst");	// 파일 쓰기 - 기반 스트림 생성
				ObjectOutputStream oos = new ObjectOutputStream(fos)){	// 자바9부터 지원하는 기반 스트림의 참조변수를 매개변수로 받아 직렬화 보조스트림 생성
			oos.writeObject(personAhn);	//personAhn을 입력 받아 파일에 값을 입력 함 (직렬화)
			oos.writeObject(personKim);	//personKim을 입력 받아 파일에 값을 입력 함 (직렬화)
		}catch(IOException e) {
			e.printStackTrace();
		}
		try(FileInputStream fis = new FileInputStream("serial.out"); // 파일 읽기 - 기반 스트림 생성
				ObjectInputStream ois = new ObjectInputStream(fis)) { //  자바9부터 지원하는 기반 스트림의 참조변수를 매개변수로 받아 직렬화 보조스트림 생성
			 Person p1 = (Person)ois.readObject();	//personAhn의 값을 파일에서 읽어  p1에 저장(역질렬화)
			 Person p2 = (Person)ois.readObject();	//personKim의 값을 파일에서 읽어  p2에 저장(역질렬화)
			 
			 System.out.println(p1);
			 System.out.println(p2);
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
}

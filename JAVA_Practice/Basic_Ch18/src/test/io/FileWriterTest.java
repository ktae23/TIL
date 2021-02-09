package test.io;

import java.io.FileWriter;
import java.io.IOException;

public class FileWriterTest {

	public static void main(String[] args) {
		FileWriter fw=null;
		try {
			fw=new FileWriter("C:\\Users\\zz238\\Desktop\\a.txt");
			fw.write("lalala");
			fw.write("hehehe");
			
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			 try {
				 if(fw !=null ) fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		

	}

}

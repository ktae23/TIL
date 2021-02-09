## I.O. Strem

### 외부 자원 사용 방법

1. 객체화 (Has a)

2. 상속 (Is a)

3. 외부 파일 인풋 받기

   

   ```
    내 프로그램
   
   	|
   
   BufferdReader -readLineI()
   
   	| chain
   
   FileREader - read()
   
   	|
   
   	|
   
      파일
   ```



## 파일 읽기

```java
public class FileReadTest {

	public static void main(String[] args) {
		try {
			FileInputStream finput = new FileInputStream("C:\\Users\\zz238\\Desktop\\교육\\멀티캠퍼스\\_temp_a.txt");
			int i = finput.read();
			System.out.println(i);
		
		
		} catch (FileNotFoundException e) {
			e.printStackTrace(); 
		} catch (IOException e) {
			e.printStackTrace(); 
		}
	}
}
```

>  `e.printStackTrace();` : 실행시점에선 삭제 해야하지만 개발 시점에선 에러 추적을 위해 사용

> ```java
> catch (FileNotFoundException | IOException e) {
> 			e.printStackTrace();
> ```
>
> 위 처럼 멀티 캐치하는 방식은 좋지 않음, 나눠서 catch 권장



```java
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

```

- 파일 읽기, 버퍼드 리더 사용, 트라이 캐치로 예외처리

### 

## 파일 쓰기

```java
package test.io;

import java.io.FileWriter;
import java.io.IOException;

public class FileWriterTest {

	public static void main(String[] args) {
		FileWriter fw=null;
		try {
			fw=new FileWriter("[파일 위치]b.txt");
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

```





---

#### [1바이트 단위 처리]

InputStream

-> FileInputStream



outputStream

-> FileoutputStream



#### [2바이트 단위 처리]

Reader

|======================> BufferdReader

-> InputStreamReader

​		->FileReader**(BufferdReader체인 걸어 사용)**



Writer



---


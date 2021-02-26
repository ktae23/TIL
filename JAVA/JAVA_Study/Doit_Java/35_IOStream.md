# Stream

- 자바의 모든 입출력은 스트림을 통해 이루어짐
- 네트워크에서 유래 된 요어
- 자료를 읽어 들이려는 소스(source)와 자료를 쓰려는 대상(target)에 따라 각각 다른 스트림 클래스 제공

- 파일 디스크, 키보드, 모니터, 메모리 입출력, 네트워크 등에서 일어나는 모든 입출력 기능을 스트림 클래스로 제공

<br/>

#### 입력, 출력 스트림

- 스트림은 단반향으로 자료가 이동하므로 입,출력을 동시에 할 수 없음

- 입력 스트림 : 어떤 대상으로부터 자료를 읽어 들일 때 사용
  - 동영상을 재생하기 위해 동영상 파일에서 자료 읽기
  - FileInputStream, FileReader, BufferdInputStream, BufferedReader 등
- 출력 스트림 : 어떤 대상으로 자료를 내보낼 때 사용
  - 편집 화면에서 사용자가 쓴 글을 파일에 저장
  - FileOutputStream, FileWriter, BufferedOutputStream, BufferedWriter 등

<br/>

#### 바이트 단위, 문자 단위 스트림

- 자바의 스트림은 기본이 바이트 단위의 입출력
- 바이트 스트림 : 그림, 동영상, 음악 파일 등 대부분 파일은 바이트 단위로 읽고 씀
  - FileInputStream, FileOutputStream, BufferdInputStream, BufferedOutputStream 등
  - 스트림 클래스 이름이 Stream으로 끝나는 경우

<br/>

- 문자 스트림 : 하나의 문자를 나타내는 char형은 2바이트기 때문에 문자를 위한 스트림 별도 제공
  - FileReader, FileWriter,BufferedReader, BufferedWriter 등
  - 스트림 클래스 이름이 Reader나 Writer로 끝나는 경우

<br/>

#### 기반, 보조 스트림

- 기반 스트림 : 읽거나 쓰는 기능을 직접 제공
  - 읽어 들일 곳(소스)나 써야 할 곳(대상)에서 직접 읽고 쓸 수 있음
  - 입출력 대상에 직접 연결 되어 생성 됨
  - FileInputStream, FileOutputStream, FileReader, FileWriter 등

- 보조 스트림 : 읽거나 쓰는 기능은 없지만 다른 스트림에 부가 기능을 제공
  - 반드시 다른 스트림을 포함하여 생성
  - InputStreamReader, OutputStreamWriter, BufferdInputStream, BufferedOutputStream 등

<br/>

## 표준 입출력

- 화면에 출력하고 입력 받는 표준 입출력 클래스가 자바에 미리 정의되어 있음
- 프로그램이 시작 될 때 생성되므로 따로 생성 할 필요 없음
- 대표적인 예 : System
  - 표준 입출력을 위한 System 클래스의 변수

| 자료형              | 변수 이름 | 설명                  |
| ------------------- | --------- | --------------------- |
| static PrintStream  | out       | 표준 출력 스트림      |
| static InputStream  | in        | 표준 입력 스트림      |
| static OutputStream | err       | 표준 오류 출력 스트림 |

<br/>

### System.in으로 화면에서 문자 입력 받기

```java
package stream.inputstream;

import java.io.IOException;

public class SystemInTest {

	public static void main(String[] args) throws IOException{
		
		System.out.println("알파벳 하나를 쓰고 [Enter]를 누르세요");
		
		int i;
		try {
			i = System.in.read();
			System.out.println(i);
			System.out.println((char)i);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
```

**알파벳 하나를 쓰고 [Enter]를 누르세요**

**g**

**103**

**g**

<br/>

- 정수형 변수 i는 4바이트지만 System.in은 1바이트만 읽기 때문에 i를 출력하면 아스키 값을 출력 한다
  - 문자로 변환하여 출력하면 입력했던 알파벳이 출력 된다 

<br/>

```java
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
```

**hello**  // 입력

**hello**  // 출력

<br/>

### 그 외 입력 클래스

#### Scanner 클래스

- java.util 패키지에 있는 입력 클래스
- 문자뿐 아니라 정수, 실수 등 다른 자료형도 읽을 수 있음
- 콘솔 화면 뿐 아니라 파일이나 문자열을 생성자의 매개변수로 받아 자료를 읽을 수 있음
- System.in보다 다양한 메서드를 활용 할 수 있음
  - System.in은 한바이트씩만 입력 받으므로 한글같은 경우는 보조 스트림을 추가로 사용해야 함
  - Scanner 클래스는 그냥 입력 가능 

| 생성자                      | 설명                                           |
| --------------------------- | ---------------------------------------------- |
| Scanner(File source)        | 파일을 매개변수로 받아 Scanner를 생성          |
| Scanner(InputStream source) | 바이트 스트림을 매개변수로 받아 SCanner를 생성 |
| Scanner(String source)      | String을 매개변수로 받아 Scanner를 생성        |

<br>

- Scanner 클래스에서 제공하는 메서드

| 메서드                | 설명                |
| --------------------- | ------------------- |
| boolean nextBoolean() | boolean 자료 읽기   |
| byte nextByte()       | 한 바이트 자료 읽기 |
| short nextShort()     | short 자료형 읽기   |
| int nextInt()         | int 자료형 읽기     |
| long nextLong()       | long 자료형 읽기    |
| float nextFloat()     | float 자료형 읽기   |
| double nextDouble()   | double 자료형 읽기  |
| String nextLine()     | 문자열 String 읽기  |

<br/>

#### Console 클래스

- System.in을 사용하지 않고 간단히 콘솔 내용을 읽을 수 있음
- 직접 콘솔 창에서 자료를 입력 받을 때 사용
  - 이클립스와는 연동 안 됨
  - 콘솔 클래스는 명령 프롬프트에서 파일 경로에 접속하여 실행해야 사용 가능
- 사용하는 메서드

| 메서드                | 설명                                    |
| --------------------- | --------------------------------------- |
| String readLine()     | 문자열 읽기                             |
| char[] readPassword() | 사용자에게 문자열을 보여 주지 않고 읽음 |
| Reader reader()       | Reader클래스 반환                       |
| PrintWriter writer()  | PrintWriter 클래스 반환                 |

<br/>

## 바이트 단위 스트림

### InputStream

- 바이트 단위로 읽는 스트림 중 최상위 스트림
- InputStream은 추상 메서드를 포함한 추상 클래스
  - 하위 클래스가 상속 받아 각 클래스에 맞게 기능을 구현하도록 제공
- 주로 사용하는 하위 클래스

| 스트림 클래스        | 설명                                           |
| -------------------- | ---------------------------------------------- |
| FileInputStream      | 파일에서 바이트 단위로 자료를 읽음             |
| ByteArrayInputStream | Byte 배열 메모리에서 바이트 단위로 자료를 읽음 |
| FilterInputStream    | 보조 스트림의 상위 클래스                      |

- 바이트 자료를 읽기 위해 제공하는 메서드

| 메서드                               | 설명                                                         |
| ------------------------------------ | ------------------------------------------------------------ |
| int read()                           | 입력 스트림으로부터 한 바이트의 자료를 읽음<br />읽은 자료의 바이트 수를 반환 |
| int read(byte b[])                   | 입력 스트림으로부터 b[] 크기의 자료를 b[]에 읽음<br />읽은 자료의 바이트 수를 반환 |
| int read(byte b[], int off, int len) | 입력 스트림으로부터 b[] 크기의 자료를 b[]의 off 변수 위치부터 저장하며 len만큼 읽음<br />읽은 자료의 바이트 수를 반환 |
| void close()                         | 입력 스트림과 연결 된 대상 리소스 닫음                       |

- read() 메서드의 반환형이 int인 이유는 더 이상 읽을 자료가 없을 경우 정수 -1이 반환 되기 때문

<br />

### FileInputStream

- 파일에서 바이트 단위로 자료를 읽을 때 사용
- 스트림 사용 위해서 스트림 클래스 생성 필요
- FileInputStream의 생성자

| 생성자                       | 설명                                                         |
| ---------------------------- | ------------------------------------------------------------ |
| FileInputStream(String name) | 파일 이름 name(경로 포함)을 매개변수로 받아 입력 스트림을 생성 |
| FileInputStream(File f)      | File 클래스 정보를 매개변수로 받아 입력 스트림 생성          |

<br />

#### 파일에서 자료 읽기

- input.txt 에는 "File"을 작성해둠

```java
package stream.inputstream;

import java.io.FileInputStream;
import java.io.IOException;

public class FileInputStreamTest {

	public static void main(String[] args) {
		FileInputStream fis = null;
		
		try {
			fis = new FileInputStream("input.txt");
			System.out.println((char)fis.read()); // 한바이트 읽기 / 읽어온 아스키 값을 문자로 변경
			System.out.println((char)fis.read());
			System.out.println((char)fis.read());
		} catch (IOException e) {
			System.out.println(e);
		} finally {
			try {
				fis.close();
			}catch (IOException e) {
				System.out.println(e);
			}
		}
		System.out.println("end");
	}
}
```

**F**

**i**

**l**

**end**

<br/>

#### 파일 끝까지 읽기

- input.txt 에는 "File"을 작성해둠

```java
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
```

**F**

**i**

**l**

**e**

**end**

<br/>

#### int read(byte[] b) 메서드로 읽기

- 한 바이트씩 읽는 것 보다 배열을 사용해 한꺼번에 많이 읽으면 처리 속도가 훨씬 빠름
- 선언한 바이트 배열의 크기만큼 한꺼번에 자료 읽고 자료의 바이트 수 반환

```java
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
				for(byte b : bs) {
					System.out.print((char)b);
				}
				System.out.println(": " + i + "바이트 읽음");
			}
			} catch (IOException e) {
				e.printStackTrace();
			}
		System.out.println("end");
	}
}

```

**abcdefghij: 10바이트 읽음**

**klmnopqrst: 10바이트 읽음**

**uvwxyzqrst: 6바이트 읽음**  // 새로 읽지 않은 뒤 4자리의 직전 값이 잔류함

**end**

<br/>

- 아래처럼 코드를 변경하면 값이 있는만큼만 출력 됨

```java
	while((i = fis.read(bs)) != -1 ) {	//읽을 값이 없으면 정수 -1을 리턴함
				for(int k = 0; k<i; k++) {
					System.out.print((char)bs[k]);
				}
```

**abcdefghij: 10바이트 읽음**

**klmnopqrst: 10바이트 읽음**

**uvwxyz: 6바이트 읽음**

**end**

<br/>

### OutputStream

- 바이트 단위로 쓰는 스트림 중 최상위 스트림
- 자료의 출력 대상에 따라 다른 스트림 제공
- 주로 사용하는 하위 클래스

| 스트림 클래스         | 설명                                |
| --------------------- | ----------------------------------- |
| FileOutputStream      | 바이트 단위로 파일에 자료 쓰기      |
| ByteArrayOutputStream | Byte 배열에 바이트 단위로 자료 쓰기 |
| FilterOutputStream    | 보조 스트림의 상위 클래스           |

- 바이트 자료를 쓰기 위해 제공하는 메서드

| 메서드                                 | 설명                                                         |
| -------------------------------------- | ------------------------------------------------------------ |
| void write(int b)                      | 한 바이트 출력                                               |
| void write(byte[] b)                   | b[] 배열에 있는 자료 출력                                    |
| void write(byte b[], int off, int len) | b[] 배열에 있는 자료의  off 위치부터 len 개수만큼 자료 출력  |
| void flush()                           | 출력 버퍼(출력을 위해 자료가 잠시 머무르는 장소)를 강제로 비워 자료 출력 |
| void close()                           | 출력 스트림과 연결 된 대상 리소스를 닫는다<br />출력 버퍼 비워짐 |

<br/>

### FileOutputStream

- OutputStream을 상속 받은 클래스 중 가장 많이 사용 되는 스트림
- 파일에 바이트 단위 자료를 출력하기 위해 사용하는 스트림
- 매개변수로 전달한 경로에 파일이 없으면 해당 경로에 파일을 새로 생성함

| 생성자                                        | 설명                                                         |
| --------------------------------------------- | ------------------------------------------------------------ |
| FileOutputStream(String name)                 | 파일 이름(경로 포함)을 매개변수로 받아 출력 스트림 생성      |
| FileOutputStream(String name, boolean append) | 파일 이름(경로 포함)을 매개변수로 받아 출력 스트림 생성<br />append 값이 true면 파일 스트림을 닫혔다 다시 생성했을 때 파일의 끝부터 이어서 작성<br />디폴트 값은 false로 덮어 쓰는(Over write) 설정 |
| FileOutputStream(File f)                      | File 클래스 정보를 매개변수로 받아 출력 스트림 생성          |
| FileOutputStream(File f, boolean append)      | File 클래스 정보를 매개변수로 받아 출력 스트림 생성<br />append 값이 true면 파일 스트림을 닫혔다 다시 생성했을 때 파일의 끝부터 이어서 작성<br />디폴트 값은 false로 덮어 쓰는(Over write) 설정 |

<br/>

#### write() 메서드 사용

- FileOutputStream을 생성한 뒤 write() 메서드를 이용해 파일에 정수 값 입력
  - 정수 값을 입력 시 아스키 값에 해당하는 문자로 변경 되어 저장 됨
  - 정수 값 그대로 입력 원할 시 DataOutputStream을 사용 

<br/>

#### write(byte[] b) 메서드 사용

- 한 바이트씩 쓰는 것 보다 배열을 사용해 한꺼번에 많이 쓰면 처리 속도가 훨씬 빠름
- 선언한 바이트 배열의 크기만큼 한꺼번에 자료 쓰고 자료의 바이트 수 반환

<br/>

#### write(byte[] b, int off, int len) 메서드 사용 

- 배열의 전체 자료를 출력하지 않고 배열의 off 위치부터 len 길이만큼 출력
- write(bs,2,10)은 bs[2]부터 10개 바이트 자료만 출력 

<br/>

#### flush() 메서드와 close() 메서드

- write() 메서드로 값을 쓰더라도 출력 버퍼에 어느 정도 자료가 모여야 피일 또는 네트워크로 전송 됨
  - 출력할 만큼 충분히 모이지 않으면 write() 메서드로 호출했어도 파일에 쓰이지 않거나 전송 안될 수 있음
  - 이 경우 close()메서드 안에서  flush() 메서드를 호출하여 남은 자료 비우면서 모두 출력

<br/>



## 문자 단위 스트림

### Reader

- 문자 단위로 읽는 스트림 중 최상위 스트림
- 주로 사용하는 하위 클래스

| 스트림 클래스     | 설명                                                         |
| ----------------- | ------------------------------------------------------------ |
| FileReader        | 파일에서 문자 단위로 읽는 스트림 클래스                      |
| InputSteramReader | 바이트 단위로 읽은 자료를 문자로 변환해주는 보조 스트림 클래스 |
| BufferedReader    | 문자로 읽을 때 배열을 제공하여 한번에 읽을 수 있는 기능을 제공 하는 보조 스트림 클래스 |

- 자료를 읽기 위해 제공하는 메서드

| 메서드                                 | 설명                                                         |
| -------------------------------------- | ------------------------------------------------------------ |
| int read()                             | 파일로부터 한 문자를 읽고 읽은 값 반환                       |
| int read(char[] buf)                   | 파일로부터 buf 배열에 문자를 읽음                            |
| int read(char[] buf, int off, int len) | 파일로부터 buf 배열의 off 위치에서부터 len 개수만큼 문자를 읽음 |
| void close()                           | 스트림과 연결된 파일 리소스 닫음                             |

<br/>

### FileReader

- Reader 스트림 중 가장 많이 사용 됨
- 읽으려는 파일이 없으면 FileNotFoundException 발생
- FileReader 생성자
- 문자 단위 스트림이기 때문에 한글도 정상적으로 읽힘

| 생성자                  | 설명                                                    |
| ----------------------- | ------------------------------------------------------- |
| FileReader(String name) | 파일 이름(경로 포함)을 매개변수로 받아 입력 스트림 생성 |
| FileReader(Fiile f)     | File 클래스 정보를 매개변수로 받아 입력 스트림 생성     |

<br/>

### Writer

- 문자 단위로 출력하는 스트림 중 최상위
- 자주 사용하는 하위 클래스

| 스트림 클래스      | 설명                                                         |
| ------------------ | ------------------------------------------------------------ |
| FileWriter         | 파일에 문자 단위로 출력하는 스트림 클래스                    |
| OutputStreamWriter | 파일에 바이트 단위로 출력한 자료를 문자로 변환해주는 보조 스트림 |
| BufferedWriter     | 문자로 쓸 때 배열을 제공하여 한번에 쓸 수 있는 기능을 제공 하는 보조 스트림 클래스 |

- 자료를 쓰기 위해 제공하는 메서드

| 메서드                                   | 설명                                                       |
| ---------------------------------------- | ---------------------------------------------------------- |
| void write(int c)                        | 한 문자를 파일에 출력                                      |
| void write(char[] buf)                   | 문자 배열 buf의 내용을 파일에 출력                         |
| void write(char[] buf, int off, int len) | 문자 배열 buf의 off 위치부터 len 개수의 문자를 파일에 출력 |
| void write(String str)                   | 문자열 str을 파일에 출력                                   |
| void write(String str, int off, int len) | 문자열 str의 off 위치부터 len 개수만큼 파일에 출력         |
| void flush()                             | 출력 버퍼를 비워 출력                                      |
| void close()                             | 파일과 연결된 스트림 닫음<br />출력 버퍼도 비워짐          |

<br/>

### FileWriter

- Writer 스트림 중 가장 많이 사용 됨
- 파일에 문자 단위 자료를 출력하기 위해 사용하는 스트림
- 매개변수로 전달한 경로에 파일이 없으면 해당 경로에 파일을 새로 생성함

| 생성자                                  | 설명                                                         |
| --------------------------------------- | ------------------------------------------------------------ |
| FileWriter(String name)                 | 파일 이름(경로 포함)을 매개변수로 받아 출력 스트림 생성      |
| FileWriter(String name, boolean append) | 파일 이름(경로 포함)을 매개변수로 받아 출력 스트림 생성<br />append 값이 true면 파일 스트림을 닫혔다 다시 생성했을 때 파일의 끝부터 이어서 작성<br />디폴트 값은 false로 덮어 쓰는(Over write) 설정 |
| FileWriter(File f)                      | File 클래스 정보를 매개변수로 받아 출력 스트림 생성          |
| FileWriter(File f, boolean append)      | File 클래스 정보를 매개변수로 받아 출력 스트림 생성<br />append 값이 true면 파일 스트림을 닫혔다 다시 생성했을 때 파일의 끝부터 이어서 작성<br />디폴트 값은 false로 덮어 쓰는(Over write) 설정 |

<br/>

## 보조 스트림

- 입출력 대상이 되는 파일이나 네트워크에 직접 쓰거나 읽는 기능은 없음
- 보조 기능을 추가하는 스트림
  - 다양한 기능을 제공하는 클래스를 디자인 패턴에서 '데코레이터'라고 함
- Wrapper Stream이라고도 함

<br/>

### FilterInputStream과 FilterOutputStream

- 보조 스트림의 상위 클래스
  - 모든 보조스트림은 위 두 클래스를 상속 받음
- 보조 스트림은 자료 입출력을 직접 할 수 없기 때문에 다른 기반 스트림을 포함 함

| 생성자                                      | 설명                                    |
| ------------------------------------------- | --------------------------------------- |
| protected FilterInputStream(InputStream in) | 생성자의 매개변수로 inputStream을 받음  |
| public FilterOutputStream(OutputStream out) | 생성자의 매개변수로 OutputStream을 받음 |

- 위 생성자 외에 다른 생성자(디폴트 생성자)는 제공 되지 않음
  - 때문에 상속 받아 사용하는 보조 클래스들도 다른 스트림을 매개 변수로 받아 상위 클래스를 호출 해야 함
- 두 클래스를 직접 생성하는 경우는 거의 없고 대부분 상속 받은 하위 클래스를 사용함
- 다른 보조 스트림 클래스를 매개변수로 받는 경우도 있으나 매개변수가 되는 보조 스트림이 기반 스트림을 포함 해야 한다

<br/>

### InputStreamReader와 OutputStreamWriter

- 바이트 단위로 자료를 읽으면 한글과 같은 문자를 제대로 읽을 수 없다
  - 그래서 문자는 Reader나 Writer에서 상속 받은 스트림을 사용해야 한다
- 표준 입출력 System.in 스트림, 네트워크에서 소켓이나 인터넷이 연결 되었을 때 사용하는 InputStream과 OutputStream도 바이트 단위로 읽고 쓰는 스트림이다
  - 이렇게 생성 된 바이트 스트림을 문자로 변환해 주는 보조 스트림이 InputStreamReader와 OutputStreamWriter이다.

| 생성자                                                | 설명                                                         |
| ----------------------------------------------------- | ------------------------------------------------------------ |
| InputStreamReader(InputStream in)                     | InputStream 클래스를 생성자의 매개변수로 받아 Reader를 생성  |
| InputStreamReader(InputStream in, Charset cs)         | InputStream과 Charset 클래스를 생성자의 매개변수로 받아 Reader를 생성 |
| InputStreamReader(InputStream in, CharsetDecoder dec) | InputStream과 CharsetDecode를 생성자의 매개변수로 받아 Reader를 생성 |
| InputStreamReader(InputStream in, String charsetName) | InputStream과 String으로 문자 세트 이름을 매개변수로 받아 Reader를 생성 |

- 여기서 문자 세트의 대표적 예는 UTF-16로 각 문자가 가지는 고유 값을 어던 값으로 이뤄졌느냐로 구분 된다.
  - 문자 인코딩 방식

```java
InputStreamReader isr = new InputStreamReader(new FileInputStream("reader.txt"))
						//보조스트림(기반스트림(파일 이름))
```

<br/>

### BufferedStream

- BufferdStream은 내부적으로 8,912바이트 크기의 배열을 가지고 있음
- 이미 생성 된 스트림에 배열 기능을 추가해 더 빠르게 입출력을 할 수 있는 버퍼링 기능 제공
  - 한 바이트, 한 문자 단위보다 처리 속도가 훨씬 빠름

- 버퍼링 기능을 제공하는 스트림 클래스

| 스트림 클래스        | 설명                                             |
| -------------------- | ------------------------------------------------ |
| BufferedInputStream  | 바이트 단위로 읽는 스트림에 버퍼링 기능 제공     |
| BufferedOutputStream | 바이트 단위로 출력하는 스트림에 버퍼링 기능 제공 |
| BufferedReader       | 문자 단위로 읽는 스트림에 버퍼링 기능 제공       |
| BufferedWriter       | 문자 단위로 출력하는 스트림에 버퍼링 기능 제공   |

- 버퍼링 기능을 제공하는 스트림 역시 보조스트림이기에 다른 스틀미을 포함하여 수행 됨
- BufferdInputStream의 생성자

| 생성자                                         | 설명                                                         |
| ---------------------------------------------- | ------------------------------------------------------------ |
| BufferedInputStream(InputStream in)            | InputStream 클래스를 생성자의 매개변수로 받아  BufferedInputStream을 생성 |
| BufferedIntputStream(InputStream in, int size) | InputStream 클래스와 버퍼 크기를 생성자의 매개변수로 받아  BufferedInputStream을 생성 |

- BufferedOutputStream(OutputSteram 클래스)
- BufferedReader(Reader 클래스)
- BufferedWiter(Writer클래스)

<br/>

### DataInputStream과 DataOutputStream

- 메모리에 저장 된 0과 1 상태를 그대로 읽고 쓰는 방식
  - 자료형의 크기가 그대로 보존 됨

| 생성자                             | 설명                                                         |
| ---------------------------------- | ------------------------------------------------------------ |
| DataInputStream(InputStream in)    | InputStream을 생성자의 매개변수로 받아 DataInputStream을 생성 |
| DataOutputStream(OutputStream out) | OutputStream을 생성자의 매개변수로 받아 DataOutputStream을 생성 |

- DataInputStream의 자료형별 제공 되는 메서드

| 메서드                | 설명                                            |
| --------------------- | ----------------------------------------------- |
| byte readByte()       | 1바이트를 읽어 반환                             |
| boolean readBoolean() | 읽은 자료가 0이면 flase, 아니면 true 반환       |
| char readChar()       | 한 문자를 읽어 반환                             |
| short readShort()     | 2바이트를 읽어 정수 값을 반환                   |
| int readInt()         | 4바이트를 읽어 정수 값을 반환                   |
| long readLong()       | 8바이트를 읽어 정수 값을 반환                   |
| float readFloat()     | 4바이트를 읽어 실수 값을 반환                   |
| double readDouble()   | 8바이트를 읽어 실수 값을 반환                   |
| String readUTF()      | 수정된 UTF-8 인코딩 기반으로 문자열을 읽어 반환 |

<br/>

- DataOutputStream은 각 자료형별로 read()에 대응 되는 write() 메서드를 제공

| 메서드                       | 설명                                       |
| ---------------------------- | ------------------------------------------ |
| void writeByte(int v)        | 1바이트의 자료를 출력                      |
| void writeBoolean(boolean v) | 1바이트 값을 출력                          |
| void writeChar(int v)        | 2바이트를 출력                             |
| void writeShort(int v)       | 2바이트를 출력                             |
| void writeInt(int v)         | 4바이트를 출력                             |
| void writeLong(long v)       | 8바이트를 출력                             |
| void writeFloat(float v)     | 4바이트를 출력                             |
| void writeDouble(double v)   | 8바이트를 출력                             |
| void writeUTF(String str)    | 수정된 UTF-8 인코딩 기반으로 문자열을 출력 |

<br/>

- writeByte(100)은 1바이트로 쓰인 100이지만 writeInt(100)은 4바이트로 쓰인 100이다.
  - 때문에 쓸때 사용한 자료형과 같은 자료형의 메서드로 읽어야 함



<br/>

## 직렬화

### 직렬화와 역직렬화

- 클래스의 인스턴스가 생성된 후에도 인스턴스 변수 값은 계속 변경 됨

- 직렬화 : 인스턴스의 어느 순간을 그대로 저장하거나 네트워크로 전달하는 일을 말함
  - 인스턴스 내용을 연속 스트림으로 만들기 위해 변수 값을 스트림으로 만드는 것
- 역직렬화 : 저장된 내용이나 전송 받은 내용을 다시 복원하는 일을 말함

| 생성자                               | 설명                                                         |
| ------------------------------------ | ------------------------------------------------------------ |
| ObjectInputStream(InputStream in)    | InputStream을 생성자의 매개변수로 받아 ObjectInputStream을 생성 |
| ObjectOutputStream(OutputStream out) | OutputStream을 생성자의 매개변수로 받아 ObjectOutputStream을 생성 |

- 저장 할 파일이나 전송 할 네트워크 등의 기반 스트림을 매개 변수로 받아 인스턴스 변수 값을 저장하거나 전송 함

<br />

#### Serializable Interface

- 직렬화는 인스턴스 내용이 외부로 유출 되는 것이기 때문에 직렬화 의도를 명시해야 함
  - 직렬화 대상이 되는 클래스에 implements Serializable을 클래스 이름 뒤에 명시해야 함

<br />

#### transient 예약어

- Socket 클래스와 같이 직렬화 될 수 없는 클래스가 인스턴스 변수로 있거나 직렬화를 원치 않는 변수가 있을 경우 사용
- 해당 변수 앞에 transient 예약어를 작성

```java
String name;
transient String job;
```

<br />

#### serialVersionUID를 사용한 버전 관리

- 객체를 역직렬화 할때 직렬화 할때와 클래스 상태가 다르면 오류 발생
  - 그 사이에 클래스가 수정 또는 변경 되었을 경우 역직렬화 불가
- 직렬화 할때 자동으로 serialVersionUID를 생성하여 정보를 저장 함
  - 역직렬화 할때 serialVersionUID를 비교하여 맞지 않으면 오류 발생
  - 작은 변경에도 클래스 버전이 계속 바뀌면 네트워크로 서로 객체를 공유하는 경우 매번 클래스를 새로 배포애햐 하는 번거로움이 있음
- 클래스의 버전 관리를 개발자가 직접 할 수 있음
  - 자바 설치 경로의 bin\serialver.exe를 사용하면 serialVerisonUID가 생성 된다
  - 이 정보를 클래스 파일에 적어주면 됨
  - 이클리스에서는 자동으로 제공 (Add generated serial version ID)

```java
public class Person implements Serializable {

	private static final long serialVersionUID = -1503252402544036183L;
```

- 직렬화 대상의 클래스 정보가 바뀌어서 이를 공유해야 할 때 버전 정보를 변경하여 사용

<br />



#### 직렬화 예시

##### 직렬화 대상 클래스

```java
package stream.serialization;

import java.io.Serializable;

public class Person implements Serializable {

	private static final long serialVersionUID = -1503252402544036183L; //버전 관리를 위한 정보
	String name;
	String job;
	
	public Person() {}

	public Person(String name, String job) {
		this.name = name;
		this.job = job;
	}
	
	public String toString() {
		return name + "," + job;
	}
}
```

##### 테스트 클래스

```java
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
		
		try(FileOutputStream fos = new FileOutputStream("serial.out");	// 파일 쓰기 - 기반 스트림 생성
				ObjectOutputStream oos = new ObjectOutputStream(fos)){	// 자바9부터 지원하는 기반 스트림의 참조변수를 매개변수로 받아 직렬화 보조스트림 생성
			oos.writeObject(personAhn);	//personAhn을 입력 받아 파일에 값을 입력 함 (직렬화)
			oos.writeObject(personKim);	//personKim을 입력 받아 파일에 값을 입력 함 (직렬화)
		}catch(IOException e) {
			e.printStackTrace();
		}
		try(FileInputStream fis = new FileInputStream("serial.txt"); // 파일 읽기 - 기반 스트림 생성
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
```

**안재용,대표이사**

**김철수,상무이사**

<br />

#### Externalizable Interface

- 직렬화하는 데 사용하는 또 다른 인터페이스
- 자료를 읽고 쓰는데 필요한 부분 중 프로그래머가 구현해야 할 메서드가 있음
  - 객체의 직렬화와 역직렬화를 프로그래머가 세밀하게 직접 제어하고자 할때 메서드를 구현하여 사용
- Externalizable Interface를 구현하면 writeExternal()메서드와 readExternal()메서드를 구현해야 함
  - 복원 할 때 디폴트 생성자가 호출 되므로 디폴트 생성자를 추가 해야 함
  - 읽고 쓰는 내용을 직접 구현해야 함

<br />

## 그 외 입출력 클래스

### File Class

- 파일이란 개념을 추상화 한 클래스
- 별도의 입출력 기능은 없음
  - 파일 자체의 경로나 정보를 가져오거나 파일을 생성하는 데 사용
- 실제로 파일이 생성 되는 것이 아니기 때문에 createNewFile() 메서드를 호출해야 실제로 생성 됨

| 생성자                            | 설명                                                    |
| --------------------------------- | ------------------------------------------------------- |
| File(File parent, String Child)   | parent 객체 폴더의 child라는 파일에 대한 File 객체 생성 |
| File(String pathname)             | pathname에 해당하는 File객체 생성                       |
| File(String parent, String Child) | parent 폴더 경로에 child라는 파일에 대한 File 객체 생성 |
| File(URL url)                     | File Url에 대한 파일의 File 객체를 생성                 |

<br/>

```java
package stream.others;

import java.io.File;
import java.io.IOException;

public class FileTest {
	public static void main(String[] args) throws IOException {
	File file = new File("C:\\파일 경로"); // 해당 경로에 File 클래스 생성, 아직 실제로 생성 된것은 아님
	file.createNewFile(); //실제 파일 생성
	
	System.out.println(file.isFile());	// 해당 경로가 일반 파일인지 여부 반환
	System.out.println(file.isDirectory());	// 해당 경로가 폴더인지 여부 반환
	System.out.println(file.getName());	.// 파일 이름 반환
	System.out.println(file.getAbsolutePath()); // 파일 절대 경로를 반환
	System.out.println(file.getPath());	// 파일 경로를 반환
	System.out.println(file.canRead());	// 파일을 읽을 수 있는지 여부 반환
	System.out.println(file.canWrite());	//파일에 쓸 수 있는지 여부 반환	
	
	file.delete();	// 파일 삭제(폴더일 경우 빈 폴더가 아니면 불가)
	}
}
```

<br />

### RandomAccessFile Class

- 입출력 클래스 중 유일하게 입력과 출력을 동시에 할 수 있는 클래스
- 처음부터 읽는 것이 아닌 임의의 위치로 이동하여 자료를 읽을 수 있음
- 파일 포인터가 있어서 어느 위치에서 읽고 쓰는지 가리키는 속성을 지님
  - 파일을 읽거나 쓰는 위치를 가리키는 역할
- DataInput, DataOutput 인터페이스를 포함하고 있음
  - 해당 인터페이를 구현하면 DataInputStream, DataOutputStream 같은 다양한 자료형을 다루는 메서드를 사용할 수 있다

| 생성자                                     | 설명                                                         |
| ------------------------------------------ | ------------------------------------------------------------ |
| RandomAccessFile(File file, String mode)   | 입출력을 할 File과 입출력 mode를 매개변수로 받음<br />mode에는 읽기 전용 "r"과 읽고 쓰기 기능인 "rw"를 사용 가능 |
| RandomAccessFile(String file, String mode) | 입출력을 할 파일 이름을 문자열로 받고 입출력 mode를 매개변수로 받음<br />mode에는 읽기 전용 "r"과 읽고 쓰기 기능인 "rw"를 사용 가능 |

<br />

```java
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
```

**파일 포인터 위치:4**

**파일 포인터 위치:12**

**파일 포인터 위치:29**

**파일 포인터 위치:29**

**100**

**3.14**

**안녕하세요**


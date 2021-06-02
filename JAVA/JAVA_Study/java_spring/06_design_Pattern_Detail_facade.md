## 디자인 패턴_Detail_4

### Facade Pattern

- Facade는 건물의 정면이라는 뜻으로 여러개의 객체와 실제 사용하는 서브 객체 사이에 복잡한 의존 관계가 있을 경우 사용
  - 중간에 facade라는 객체를 두고 여기서 제공하는 interface만을 활용하여 기능을 사용
  - facade는 자신이 가지고 있는 각 클래스의 기능을 명확히 알아야 한다.

- 여러개의 객체를 합쳐서 하나의 기능으로 제공 할 때 주로 사용

<br/>

#### Facade 없이 짜여진 코드

##### FTP

```java
package design.facade;

public class FTP {
	
	private String host;
	private int port;
	private String path;

	
	public FTP(String host, int port, String path) {
		this.host = host;
		this.port = port;
		this.path = path;
	}

	
	public void connect() {
		System.out.println("FTP Host : " + host + "Posrt" + port + "로 연결합니다.");
	}
	
	public void movevDirectory() {
		System.out.println("path : " + path + "로 이동합니다.");
	}
	
	public void disConnect() {
		System.out.println("FTP 연결을 종료합니다.");
		
	}
}
```

<br/>

##### Reader

```java
package design.facade;

public class Reader {

	private String fileName;
	
	public Reader(String fileName) {
		this.fileName = fileName;
	}
	
	public void fileConnect() {
		String msg = String.format("Reader %s로 연결 합니다.", fileName);
		System.out.println(msg);
	}
	
	public void fileRead() {
		String msg = String.format("Reader %s의 내용을 읽어 합니다.", fileName);
		System.out.println(msg);
	}
	
	public void fileDisconnect() {
		String msg = String.format("Reader %s로 연결을 종료합니다.", fileName);
		System.out.println(msg);
	}
}

```

<br/>

##### Writer

```java
package design.facade;

public class Writer {

	private String fileName;
	
	public Writer(String fileName) {
		this.fileName = fileName;
	}
	
	public void fileConnect() {
		String msg = String.format("Writer %s로 연결 합니다.", fileName);
		System.out.println(msg);
	}
	
	public void fileWrite() {
		String msg = String.format("Writer %s의 내용을 읽어 합니다.", fileName);
		System.out.println(msg);
	}
	
	public void fileDisconnect() {
		String msg = String.format("Writer %s로 연결을 종료합니다.", fileName);
		System.out.println(msg);
	}
}

```

<br/>

##### Main

```java
package design;

import javax.swing.text.WrappedPlainView;

import design.facade.FTP;
import design.facade.Reader;
import design.facade.Writer;

public class Main {

	public static void main(String[] args) {
		
		FTP ftpClient = new FTP("www.foo.co.kr", 22, "/home/etc");
		ftpClient.connect();
		ftpClient.movevDirectory();

		Reader reader = new Reader("text.temp");
		reader.fileConnect();
		reader.fileRead();

		
		Writer writer = new Writer("text.tmp");
		writer.fileConnect();
		writer.fileWrite();
		
		writer.fileDisconnect();		
		reader.fileDisconnect();
		ftpClient.disConnect();
	
	}
}
	

```

<br/>

##### 출력 결과

```shell
FTP Host : www.foo.co.krPosrt22로 연결합니다.
path : /home/etc로 이동합니다.
Reader text.temp로 연결 합니다.
Reader text.temp의 내용을 읽어 합니다.
Writer text.tmp로 연결 합니다.
Writer text.tmp의 내용을 읽어 합니다.
Writer text.tmp로 연결을 종료합니다.
Reader text.temp로 연결을 종료합니다.
FTP 연결을 종료합니다.
```



___

<br/>

#### Facade를 이용해 짜여진 코드

##### SFtpClient

```java
package design.facade;

public class SFtpClient {

	private FTP ftp;
	private Reader reader;
	private Writer writer;
	
	public SFtpClient(FTP ftp, Reader reader, Writer writer) {
		this.ftp = ftp;
		this.reader = reader;
		this.writer = writer;
	}
	
	public SFtpClient(String host, int port, String path, String fileName) {
		this.ftp = new FTP(host, port, path);
		this.reader = new Reader(fileName);
		this.writer = new Writer(fileName);
	}
	
	public void connect() {
		ftp.connect();
		ftp.movevDirectory();
		reader.fileConnect();
		writer.fileConnect();

	}
	
	public void disConnect() {
		writer.fileDisconnect();
		reader.fileDisconnect();
		ftp.disConnect();

	}
	
	public void read() {
		reader.fileRead();
	}
	
	public void write() {
		writer.fileWrite();
	}
	
}
```

- 위의 한 클래스에서 공통적인 작업을 관리

<br/>

##### Main

```java
package design;

import design.facade.FTP;
import design.facade.Reader;
import design.facade.SFtpClient;
import design.facade.Writer;

public class Main {

	public static void main(String[] args) {
		
		SFtpClient sftpCiClient = new SFtpClient("www.foo.co.kr", 22, "/home/etc/", "text.tmp");
		sftpCiClient.connect();
		sftpCiClient.read();
		sftpCiClient.write();
		sftpCiClient.disConnect();
		
	}
}
```

<br/>

- 사용하는 정면에서 봤을 때 훨씬 간결하고 사용하기 편리하다.


# Exception

## Exception Class

### 오류

- 컴파일 오류 : 프로그램 코드 작성 중 실수로 발생
  - 컴파일 단계에서 원인 확인이 가능하고 모두 수정해야 프로그램이 정상 실행 됨

- 실행 오류 : 실행 중인 프로그램이 의도하지 않은 동작을 하거나 중지 되는 오류
  - 버그(bug) : 의도한 바와 다르게 실행되어 생기는 오류
  - 실행 도중 발생하는 만큼 예측하기 어렵고 비정상 종료가 발생하기도 함

<br/>

### 오류와 에러

- 시스템 오류 : 자바 가상 머신에서 발생
  - 사용 가능한 동적 메모리가 없거나 스택 메모리의 오버 플로우 발생
  - 프로그램에서 제어 불가
- 예외 : 파일을 읽어 오려는데 파일이 없거나, 데이터 전송을 하려는데 네트워크 유실이 되는 등
  - 프로그램에서 제어 가능

<br/>

- 오류 클래스는 모두 Throwable 클래스에서 상속 받음
  - Error 클래스의 하위 클래스는 시스템에서 발생하는 오류를 다루며 프로그램에서 제어하지 않음
  - Exception 클래스와 하위 클래스는 프로그램에서 제어 함

<br/>

#### 예외 클래스의 종류

- 프로그램에서 처리하는 예외 클래스의 최상위 클래스는 Excepton 클래스이다.

![Exeption Hierarchy](https://www.codersdesks.com/wp-content/uploads/2020/01/exception_hierarchy.jpg)

[이미지출처](https://www.codersdesks.com/java-exception-in-detail/)

<br/>

## 예외 처리하기

### try-catch문

```java
try{
	예외가 발생할 위험이 있는 코드 부분
} catch(처리할 예외 타입 e) {
	//try 블록 안에서 예외가 발생 했을 때 처리하는 부분
}
```

<br/>

**예제 클래스**

```java
package exception;

public class ArrayExceptionHandling {
	public static void main(String[] args) {
	int[] arr = new int[5];
	
	try {
		for(int i=0; i<=5;i++) {
			arr[i] = i;
			System.out.println(arr[i]);
		}
	}catch(ArrayIndexOutOfBoundsException e) {
		System.out.println(e);
		System.out.println("예외 처리 부분");
	}
	System.out.println("프로그램 종료");
	}
}

```

**0**

**1**

**2**

**3**

**4**

**java.lang.ArrayIndexOutOfBoundsException: 5**

**예외 처리 부분**

**프로그램 종료**

<br/>

### try-catch-finally문

- 프로그램에서 사용한 리소스는 프로그램이 종료 되면 자동으로 해제 된다.
  - 리소스 : 시스템에서 사용하는 자원으로 파일, 네트워크, DB 연결 등
- 끝자지 않고 계속 수행되거나 해제되지 않으면 문제가 발생함
  - 사용한 자원은 close() 메서드를 비롯한 네트워크 연결 해제, 파일 스트림 닫기 등으로종료처리 해야 함

```java
package exception;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ExceptionHandling2 {
	public static void main(String[] args) {
		FileInputStream fis = null;
		
		try {
			fis = new FileInputStream("a.txt");
		}catch(FileNotFoundException e) {
			System.out.println(e);
			return;
		} finally {
			if(fis != null) {
				try{
					fis.close();
				} catch (IOException e){
					e.printStackTrace();
				} 
			}
			System.out.println("항상 수행");
		}
	}
}
```

**java.io.FileNotFoundException: a.txt (지정된 파일을 찾을 수 없습니다)**

**항상 수행**

<br/>

### try-with-resources문

- close() 메서드를 명시적으로 호출하지 않아도 try 블럭 내에서 열린 리소스를 자동으로 닫도록 제공하는 기능
- 자바 7부터 제공
- try-with-resources 문을 사용하려면 해당 리소스가 AutoCloseable 인터페이스를 구현해야 함
  - AutoCloseable 인터페이스에는 close() 메서드가 있고 이를 구현한 클래스에는 명시적 호출을 하지 않아도 자동으로 close() 메서드 부분이 호출 됨

- 자바 7부터는 FileInputStream 클래스에서 Closeable과 AutoCloseable 인터페이스를 구현하고 있음
- try-with-resources 문법을 사용하면 FileInputStream 을 사용 할 때 명시적으로 close() 메서드를 호출하지 않아도 정삭인 경우와 예외가 발생한 경우 모두 close() 메서드가 호출 됨

<br/>

#### AutoCloseable Interface

- try문의 괄호 안에 리소스를 선언하여 사용
  - 여러 리소스를 생성 할 경우 세미콜론으로 구분

```java
try (A a = new A(); B b = new B()) {
	...
} catch (Exception e) {
	...
}
```

<br/>

##### 구현클래스

```java
package exception;

public class AutoCloseObj implements AutoCloseable{

	@Override
	public void close() throws Exception {
		System.out.println("리소스가 닫혔습니다.");
	}
}
```

##### 테스트 클래스

```java
package exception;

public class AutoCleseTest {

	public static void main(String[] args) {
		try (AutoCloseObj obj = new AutoCloseObj()) { // 사용할 리소스 선언
			throw new Exception(); // 강제로 예외 발생
		}catch(Exception e) {
			System.out.println("예외 부분 입니다.");
		}
	}
}
```

**리소스가 닫혔습니다.**

**예외 부분 입니다.**

<br/>

#### 향상된 try-with-resources문

- 자바 9에서 추가 됨
- 자바 7에서는 try문 괄호 안에 AutoCloseable 인터페이스를 구현한 리소스의 변수 선언을 해야 했음 
  - 리소스가 외부에 선언되고 생성된 경우에도 다른 참조 변수를 이용해 괄호 안에 다시 선언해야 했음

```java
AutoCloseObj obj = new AutoCloseObj();	// 이미 생성 된 경우
try(AutoCloseObj obj2 = obj) {			// 다른 참조 변수로 재 선언 해야 함 
	throw new Exception();
} catch (Exception e) {
	System.out.println("예외 부분입니다.")
}
```

<br/>

- 자바9부터는 try문 괄호 안에 외부에서 선언한 변수를 바로 사용 할 수 있어 짐

```java
AutoCloseObj obj = new AutoCloseObj();	// 이미 생성 된 경우
try(obj) {		// 외부에서 선언한 변수 그대로 사용 
	throw new Exception();
} catch (Exception e) {
	System.out.println("예외 부분입니다.")
}
```

<br/>

## 예외처리 미루기

- 예외가 발생 했을때 처리 방법은 크게 두 가지가 있다.
  - Add throws declaration
  - surround with try/catch

<br/>

### throws 사용하여 예외 미루기

- 예외가 발생하는 메서드에서 바로 예외처리하는 try/catch문 사용과 달리 메서드를 호출하여 사용하는 부분으로 예외 처리를 미루는 방법

```java
package exception;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ThrowsException {
	public Class loadClass(String fileName, String className) 
			throws FileNotFoundException, ClassNotFoundException{
		FileInputStream fis = new FileInputStream(fileName);
		Class c = Class.forName(className);
		return c;
	}
	
	public static void main(String[] args) {
		ThrowsException test = new ThrowsException();
		test.loadClass("a.txt", "java.lang.String"); // 예외처리를 하라는 오류 메시지 생성 됨
	}
}
```

- 위의 예제를 보면 loadClass() 메서드를 실행 할 경우 특정 파일을 FileInputStream으로 열고 Class 객체를 동적 로딩하여 반환 한다.
  - FileInputStream으로 열때 FileNotFoundException이 발생 할 수 있고, Class 객체를 동적 로딩 할때는 ClassNotFoundException가 발생할 수 있다.
  - 하지만 메서드에서 try/catch문을 사용하여 처리하지 않고 선언부에 throws 예약어를 이용해 선언을 했을 뿐이다.

<br/>

#### throws를 활용하여 예외 처리 미루기

- try/catch를 미리 해두지 않고 throws 예약어를 이용하여 예외처리를 미룰 경우 메서드를 호출하는 사용 부분에서 처리를 해야 한다.
- 이때 한번 더 미룰 경우 main 함수로 미뤄져 main함수를 호출하는 JVM으로 보내져 비정상 종료 되므로 지양

<br/>

##### Surround with try/multi-catch

- 여러 예외처리를 한번에 하는 방식
- 각 상황마다 처리 방식을 달리해야하거나 로그를 각각 남겨야 할 경우 지양

```java
	public static void main(String[] args) {
		ThrowsException test = new ThrowsException();
		try {
			test.loadClass("a.txt", "java.lang.String");
		} catch (FileNotFoundException | ClassNotFoundException e) {
			e.printStackTrace();
		}
```

<br/>

##### Surround with try/catch

- 여러 예외처리를 각각 하는 방식

- 각 상황마다 처리 방식을 달리해야하거나 로그를 각각 남겨야 할 경우 사용
- 위에서 아래로 순서대로 예외처리를 진행
  - 작업의 순서, 예외 처리의 범위 등을 고려하여 작성 순서를 정함
  - 마지막 catch에는 최상위인 Exception 클래스로 그 외 예상하지 못한 예외처리를 처리 한다.

```java
	public static void main(String[] args) {
		ThrowsException test = new ThrowsException();
		try {
			test.loadClass("a.txt", "java.lang.String");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
```

<br/>

## 사용자 정의 예외

- 자바에서 제공하는 예외 외에 프로그램에 따라 다양한 예외 상황이 발생할 수 있음
  - 때문에 프로그램의 성격과 목적에 맞는 예외를 직접 정의하고 발생시킬 수 있다
- 예외의 중요성과 종류에 따라 로그를 만들어 남기는 것이 중요
  - 하지만 입문서이기 때문에 해당 방법을 넣지는 않음

<br/>

### 사용자 정의 예외 클래스 구현하기

- 아이디가 null이거나 지정 범위를 벗어날 경우 예외처리하는 예제

<br/>

##### 클래스

```java
package exception;

public class IDFormat {
	
	private String userID;
	
	public String getUserID() {
		return userID;
	}
	
	public void setUserID(String userID) throws IDFormatException {
		if(userID == null ) {
			throw new IDFormatException("아이디칸을 비워둘 수 없습니다.");
		}else if(userID.length() < 8 || userID.length() > 20) {
			throw new IDFormatException("아이디는 8글자 이상 20글자 이하로 사용해주세요");
		}
		this.userID=userID;
	}
}
```

##### 예외 클래스 정의

```java
package exception;

public class IDFormatException extends Exception{
	public IDFormatException(String message) { // 생성자 매개변수로 예외 상황 메세지 입력
		super(message);
	}
}
```

##### 테스트 클래스

```java
package exception;

public class IDFormatTest {
	public static void main(String[] args) {
		IDFormat test = new IDFormat();
		
		// 아이디가 null 값일 경우
		String userID = null;
		try {	// 메서드 선언 시 throws로 예외 처리를 미뤘기에 호출하는 곳에서 예외 처리 진행 
			test.setUserID(userID);
		}catch(IDFormatException e) {
			System.out.println(e.getMessage());
		}
		
		// 아이디가 8글자 이하일 경우
		userID = "1234567";
		try {	// 메서드 선언 시 throws로 예외 처리를 미뤘기에 호출하는 곳에서 예외 처리 진행 
			test.setUserID(userID);
		}catch(IDFormatException e) {
			System.out.println(e.getMessage());
		}
		
	}
}
```


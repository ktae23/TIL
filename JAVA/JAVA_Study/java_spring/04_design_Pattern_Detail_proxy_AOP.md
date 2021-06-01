## 디자인 패턴_Detail_2

### Proxy Pattern

- 대리인이란 뜻으로 무언갈 대신 처리하는 것
- Proxy Class를 통해 대신 전달하는 형태로 설계되며 실제 Client는 Proxy로부터 결과를 받는다.
- Cache의 기능으로도 활용이 가능하다
- SOLID 중에서 개방폐쇄 원칙(OCP)과 의존 역전 원칙 (DIP)를 따른다.

<br/>

##### 브라우저 인터페이스

```java
package design.proxy;

public interface IBroweser {

		Html show();
}
```

<br/>

##### 브라우저 클래스

```java
package design.proxy;

public class Browser implements IBroweser{

	private String url;
	
	public Browser(String url) {
		this.url = url;
	}

	@Override
	public Html show() {
		System.out.println("Browser loading html from : "  + url);
		return new Html(url);
	}
}
```

<br/>

##### HTML 클래스

```java
package design.proxy;

public class Html {
	private String url;
	
	public Html(String url) {
		this.url = url;
	}
}
```

<br/>

##### Main

```java
package design;

import design.proxy.Browser;


public class Main {

	public static void main(String[] args) {
		
	Browser browser = new Browser("www.naver.com");
	browser.show();
	browser.show();
	browser.show();
	browser.show();
	
	}
}
```

<br/>

- 출력 : 

```shell
BrowserProxy loading html from : www.naver.com
BrowserProxy loading html from : www.naver.com
BrowserProxy loading html from : www.naver.com
BrowserProxy loading html from : www.naver.com
```

<br/>

##### 프록시 패턴 사용

```java
package design.proxy;

public class BrowserProxy implements IBroweser{

	private String url;
	private Html html;
	
	public BrowserProxy(String url) {
		this.url = url;
	}
	
	@Override
	public Html show() {
		
		if(html == null) {
			this.html = new Html(url);
			System.out.println("BrowserProxy loading html from : " + url);
		}
		System.out.println("BrowserProxy use cache html : " + url);
		return html;
	}
	
}
```

<br/>

##### Main

```java
package design;

import design.proxy.Browser;
import design.proxy.BrowserProxy;
import design.proxy.IBroweser;


public class Main {

	public static void main(String[] args) {
		
//	Browser browser = new Browser("www.naver.com");
//	browser.show();
//	browser.show();
//	browser.show();
//	browser.show();

	IBroweser browser = new BrowserProxy("www.naver.com");
	browser.show();
	browser.show();
	browser.show();
	browser.show();
	
	}
}
```

<br/>

- 출력 결과

```shell
BrowserProxy loading html from : www.naver.com
BrowserProxy use cache html : www.naver.com
BrowserProxy use cache html : www.naver.com
BrowserProxy use cache html : www.naver.com
BrowserProxy use cache html : www.naver.com
```

<br/>

___

<br/>

### AOP(Aspect-Oriented-Programming)

- OOP를 보완하는 수단으로 흩어진 Aspect를 모듈화 하는 기법

- 특정 메서드의 실행 시간, 특정 패키지에 있는 메서드 들의 실행 시간
- 전후로 작업하고 싶은 부분들, 일괄적으로 특정한 요청에 대해 리퀘스트, 리스폰스 정보를 남긴다던지 코드에 개별 작업하는 것이 아니라 특정 패키지의 모든 기능 등 전후에 이러한 작업을 넣을 수 있는 패턴
- 특정 메서드의 수행 속도 측정 등에 사용

<br/>

##### Aop브라우저

```java
package design.aop;

import design.proxy.Html;
import design.proxy.IBroweser;

public class AopBrowser implements IBroweser{
	
	private String url;
	private Html html;
	private Runnable before;
	private Runnable after;
	
	public AopBrowser(String url, Runnable before, Runnable after) {
		this.url = url;
		this.before = before;
		this.after = after;
	
	}
	
	
	@Override
	public Html show() {
		before.run();
		
		if(html == null) {
			this.html = new Html(url);
			System.out.println("AopBrowser html loading from : " + url);
			
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		after.run();
		System.out.println("AopBrowser html use cache : "  + url);
		
		
		return html;
	}

}
```

<br/>

##### Main

```java
package design;

import java.util.concurrent.atomic.AtomicLong;

import design.aop.AopBrowser;
import design.proxy.IBroweser;


public class Main {

	public static void main(String[] args) {
		
	// 스레드에서의 동시성 문제를 해결하기 위해 AtomicLong 클래스 생성
	AtomicLong start = new AtomicLong();
	AtomicLong end = new AtomicLong(); 
		
	IBroweser aopBrowser = new AopBrowser("www.naver.com", 
			() -> {
				System.out.println("before");
				start.set(System.currentTimeMillis());
			},
			() -> {
				long now = System.currentTimeMillis();
				end.set(now - start.get());
			}
		);
	aopBrowser.show();
	System.out.println("loading time : " + end.get());
	
	aopBrowser.show();
	System.out.println("loading time : " + end.get());
	
	}
}
```

- AtomicLong은 Wrapping 클래스로 Long 자료형 
- 쓰레드 안정적으로 구현되었기 때문에 멀티쓰레드 상황에서 동기화 없이 동시성 문제 해결

[참고하면 좋은 블로그](https://codechacha.com/ko/java-atomic-long/)

<Br/>

##### 출력결과

```java
before
AopBrowser html loading from : www.naver.com
AopBrowser html use cache : www.naver.com
loading time : 1502
before
AopBrowser html use cache : www.naver.com
loading time : 0
```

<br/>


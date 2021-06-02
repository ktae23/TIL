## 디자인 패턴_Detail_5

### Strategy Pattern

- 전략 패턴으로 불리며 객체지향의 꽃이라고 불린다.
- 유사한 행위들을 캡슐화
  - 객체의 행위를 바꾸고 싶은 경우 직접 변경하는 것이 아닌 전략만 변경
  - 유연하게 확장하는 패턴으로 SOLID 중 개방폐쇄 원칙(OCP)와 의존 역전 원칙(DIP)를 따름

<br/>

##### EncodingStrategy Interface

```java
package design.strategy;

public interface EncodingStrategy {
	String encode(String text);
	
}
```

<br/>

##### NormalStrategy

```java
package design.strategy;

public class NormalStrategy implements EncodingStrategy {

	@Override
	public String encode(String text) {
		return text;
	}

}
```

<br/>

##### Base64Strategy

```java
package design.strategy;

import java.util.Base64;

public class Base64Strategy implements EncodingStrategy {

	@Override
	public String encode(String text) {
		return Base64.getEncoder().encodeToString(text.getBytes());
	}

}
```

<br/>

##### AppendStrategy 

```java
package design.strategy;

public class AppendStrategy implements EncodingStrategy{

	@Override
	public String encode(String text) {
		return "ABCD"+text;
	}

}
```

<br/>

##### Encoder 

```java
package design.strategy;

public class Encoder {
	
	private EncodingStrategy encodingStrategy;
	
	public String getMessage (String messge){
		return this.encodingStrategy.encode(messge);
		
	}
	
	public void setEncodingStrategy(EncodingStrategy enCodingStrategy) {
		this.encodingStrategy = enCodingStrategy;
	}

}
```

- 기본 인코더
  - setEncodingStrategy() 메서드를 이용해 전략을 주입한다.

<br/>

##### Main

```java
package design;

import design.strategy.AppendStrategy;
import design.strategy.Base64Strategy;
import design.strategy.Encoder;
import design.strategy.EncodingStrategy;
import design.strategy.NormalStrategy;

public class Main {

	public static void main(String[] args) {
		
        // 기본 인코더
		Encoder encoder = new Encoder();
		
		// base64
		EncodingStrategy base64 = new Base64Strategy();
		
		// noramal
		EncodingStrategy normal = new NormalStrategy();
		
		String message = "hello java";
		
        //기본 인코더에 전략을 주입해서 다르게 사용
		encoder.setEncodingStrategy(base64);
		String base64Result = encoder.getMessage(message);
		System.out.println(base64Result);
		
        //기본 인코더에 전략을 주입해서 다르게 사용
		encoder.setEncodingStrategy(normal);
		String normalResult = encoder.getMessage(message);
		System.out.println(normalResult);
		
        //기본 인코더에 전략을 주입해서 다르게 사용
		encoder.setEncodingStrategy(new AppendStrategy());
		String appendResult = encoder.getMessage(message);
		System.out.println(appendResult);	
		
	}
}
```

<br/>

##### 출력 결과

```shell
aGVsbG8gamF2YQ==
hello java
ABCDhello java
```

<br/>
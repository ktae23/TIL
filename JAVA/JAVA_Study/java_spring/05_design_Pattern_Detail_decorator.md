## 디자인 패턴_Detail_3

### Decorator Pattern

- 기존 뼈대(클래스)는 유지하되, 이후 필요한 형태로 꾸밀 때 사용
  - 예시 : 에스프레소에 물, 우유, 초콜릿 등을 넣듯이 원본을 유지하면서 첨가하여 확장
- 확장이 필요한 경우 상속의 대안으로도 활용
- SOLID 중 개방 폐쇄 원칙(OCP)과 의존 역전 원칙(DIP)를 따른다

<br/>

##### ICar

```java
package design.decorator;

public interface ICar {
	
	int getPrice();
	void showPrice();
}
```

- 기본이 되는 인턴페이스 작성

<br/>

##### Audi

```java
package design.decorator;

public class Audi implements ICar {

	private int price;
	
	public Audi(int price) {
		this.price = price;
	}
	
	@Override
	public int getPrice() {
		return price;
	}

	@Override
	public void showPrice() {
		System.out.println("아우디의 가격은 " + this.price + "원입니다.");
	}

}
```

- 뼈대가 되는 기본 구현 클래스 작성

<br/>

##### AudiDecrator

```java
package design.decorator;

public class AudiDecorator implements ICar{
	
	protected ICar audi;
	protected String modelName;
	protected int modelPrice;
	

	public AudiDecorator(ICar audi, String modelName, int modelPrice) {
		this.audi = audi;
		this.modelName = modelName;
		this.modelPrice = modelPrice;
	}

	@Override
	public int getPrice() {
		return audi.getPrice() + modelPrice;
	}

	@Override
	public void showPrice() {
		System.out.println(modelName + "의 가격은" + getPrice() + "원 입니다.");			
	}

}
```

- 기본 클래스를 가져와 추가 작업을 진행하는 데코레이터 클래스 작성

<br/>

##### A3 ~ A5

```java
package design.decorator;

public class A3 extends AudiDecorator{

	public A3(ICar audi, String modelName) {
		super(audi, modelName, 1000);

	}

}
===================================================
package design.decorator;

public class A4 extends AudiDecorator{

	public A4(ICar audi, String modelName) {
		super(audi, modelName, 2000);

	}

}
===================================================
package design.decorator;

public class A5 extends AudiDecorator{

	public A5(ICar audi, String modelName) {
		super(audi, modelName, 3000);

	}

}
```

- 데코레이터를 상속 받아 기본 클래스를 유지한 채 추가 작업을 수행하는 클래스를 작성

<br/>

##### Main

```java
package design;

import design.decorator.A3;
import design.decorator.A4;
import design.decorator.A5;
import design.decorator.Audi;
import design.decorator.ICar;

public class Main {

	public static void main(String[] args) {
		
    // 기본 클래스
	ICar audi = new Audi(1000);
	audi.showPrice();
	
    // 데코레이터 클래스
	ICar audi3 = new A3(audi,"A3");
	audi3.showPrice();
	
	ICar audi4 = new A4(audi,"A4");
	audi4.showPrice();
	
	ICar audi5 = new A5(audi,"A5");
	audi5.showPrice();
	
	}
}
```

<br/>

##### 출력 결과

```shell
아우디의 가격은 1000원입니다.
A3의 가격은2000원 입니다.
A4의 가격은3000원 입니다.
A5의 가격은4000원 입니다.
```

<br/>

___

<br/>

### Observer Pattern

- 관찰자 패턴이라고도 말함
- 변화가 일어났을 때 미리 등록 된 다른 클래스에 통보해주는 패턴을 구현
- 많이 사용되는 곳은 Event Listener

<br/>

##### IButtonListener

```java
package design.observer;

public interface IButtonListener {
	void clickEvent(String event);
}
```

- 클릭 이벤트 추상메서드를 가진 인터페이스 작성

<br/>

##### Button

```java
package design.observer;

public class Button {

	private String name;
	private IButtonListener buttonListener;
	
	public Button(String name) {
		this.name = name;
	}

	
	public void click(String message) {
		buttonListener.clickEvent(message);
	}
    
	// 클릭이벤트를 달아주는 메서드
	public void addListener(IButtonListener iButtonListener) {
		this.buttonListener = iButtonListener;
	}
	
}
```

<br/>

##### Main

```java
package design;


import design.observer.Button;
import design.observer.IButtonListener;



public class Main {

	public static void main(String[] args) {
		

		Button button = new Button("버튼");
		
        // 이너클래스로 구현한 버튼리스너를 추가해 줌
        button.addListener(new IButtonListener() {
			@Override
			public void clickEvent(String event) {
				System.out.println(event);
			}
		});
		
		button.click("메시지 전달 : click 1");
		button.click("메시지 전달 : click 2");
		button.click("메시지 전달 : click 3");
		button.click("메시지 전달 : click 4");
			
	}
}
```

<br/>

##### 출력 결과

```java
메시지 전달 : click 1
메시지 전달 : click 2
메시지 전달 : click 3
메시지 전달 : click 4
```


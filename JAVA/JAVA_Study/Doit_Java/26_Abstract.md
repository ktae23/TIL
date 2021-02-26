# 추상 클래스

- `추상`이란 말에서 알 수 있듯이 `추상 클래스`는 `구체적이지 않은 클래스`를 말한다.
  - 추상 클래스가 아닌 클래스는 `Concrete Class`라고 부른다.

- 추상 클래스는 항상 `추상 메서드`를 포함한다.

  - 추상 메서드는 구현부가 없다.

  - 모든 메서드가 추상 메서드인 것이 아닌, 추상 메서드를 하나 이상 포함하면 추상 클래스가 된다.

  - 모든 메서드를 구현 했어도 abstract 예약어를 사용하면 추상 클래스가 된다.

  - 추상 클래스는 모든 메서드가 구현되지 않았기 때문에 인스턴스로 생성 할 수 없다.

    - ```java
      package abstract_practice;
      
      public abstract class Computer {
      
      	public void turnOn() {
      		System.out.println("전원을 켭니다.");
      	}
      	public void turnOff() {
      		System.out.println("전원을 끕니다.");
      	}
      }
      ```

    - 위 코드는 모든 메서드가 구현 됐지만, 상속만을 위해 만들어진 클래스이기 때문에 new 예약어로 인스턴스 생성을 할 수 없다.

<br/>

- 일반적인 메서드

  - ```java
    int add(int x, int y){
    	return x+y;	// 구현부
    }
    ```

- 추상 메서드

  - ```java
    abstract int add(int x, int y);
    ```

  - abstract 예약어를 사용하고 중괄호 없이 바로 세미콜론으로 마무리 한다.

    - ```java
      int add(int x, int y) {}
      ```

    - 위와 같은 메서드는 추상 메서드가 아니다.

    - 중괄호 안에 코드가 없을 뿐이지 중괄호가 있다는것 만으로도 메서드를 구현한 셈이다.

<br/>

> 메서드의 선언부만 봐도 어떤 일을 하는 메서드인지 알 수 있도록 작성하는 것이 중요.

<br/>

---

<br/>

### 추상 클래스 구현하기

- 추상 클래스를 구현하기 위해 해당 클래스를 상속 받은 하위 클래스에서 오버라이딩 한다.

<br/>

#### 추상클래스

```java
package abstract_practice;

public abstract class Computer {

	public abstract void display(); // 
	public abstract void typing();	// 
	
	public void turnOn() {
		System.out.println("전원을 켭니다.");
	}
	public void turnOff() {
		System.out.println("전원을 끕니다.");
	}
}
```

#### 구현 클래스

```java
package abstract_practice;

public class DeskTop extends Computer{

	@Override
	public void display() {
		System.out.println("DeskTop displya()");
	}

	@Override
	public void typing() {
		System.out.println("DeskTop typing()");		
	}
}
```

```java
package abstract_practice;

public class NoteBook extends Computer{

	@Override
	public void display() {
		System.out.println("NoteBook display()");		
	}

	@Override
	public void typing() {
		System.out.println("NoteBook typing()");	
	}
}
```

#### 테스트 클래스

```java
package abstract_practice;

public class Test {

	public static void main(String[] args) {
		DeskTop dt = new DeskTop();
		NoteBook nb = new NoteBook();
		
		dt.display();
		dt.typing();
		nb.display();
		nb.typing();
		
		System.out.println("=======================");
		
		dt.turnOn();
		dt.turnOff();
		
		nb.turnOn();
		nb.turnOff();
	}
}
```

**DeskTop displya()**

**DeskTop typing()**

**NoteBook display()**

**NoteBook typing()**

**=======================**

**전원을 켭니다.**

**전원을 끕니다.**

**전원을 켭니다.**

**전원을 끕니다.**

<br/>

---

<br/>

### 템플릿 메서드

- 디자인 패턴의 한 방법으로 모든 객체 지향 프로그램에서 사용하는 구현 방법

```java
package template;

public abstract class Car {
	public abstract void drive();
	public abstract void stop();
	
	public void startCar() {
		System.out.println("시동을 켭니다.");
	}
	
	public void turnOff() {
		System.out.println("시동을 끕니다.");
	}
    
	// 템플릿 메서드
	final public void run() {
		startCar();
		drive();
		stop();
		turnOff();
	}
}
```

- 자동차라면 필수적인 `시동 켜기`, `시동 끄기`는 미리 구현해놓고, 차종에 따라 달라 질 수 있는 `주행`과 `멈춤` 기능은 추상 메서드로 남겨 둔다.
  - 또한 `시동 켜기`,  `주행`, `멈춤`,`시동 끄기` 네 가지 기능이 없이는 자동차라 할 수 없고,  반드시 해당 순서대로 진행 된다.
  - 구현 클래스에서 반드시 해당 순서대로 모든 메서드들을 구현하도록 run() 메서드를 `final`로 선언하여 변경 할 수 없도록 한다.

<br/>

#### 탬플릿(추상) 클래스

```java
package template;

public abstract class Car {
	public abstract void drive();
	public abstract void stop();
	
	public void startCar() {
		System.out.println("시동을 켭니다.");
	}
	
	public void turnOff() {
		System.out.println("시동을 끕니다.");
	}
	
	final public void run() {
		startCar();
		drive();
		stop();
		turnOff();
	}
}
```

#### 구현 클래스

```java
package template;

public class AICar extends Car{

	@Override
	public void drive() {
		System.out.println("자율 주행합니다.");
		System.out.println("자동차가 알아서 방향을 전환합니다.");
	}
	
	@Override
	public void stop() {
		System.out.println("스스로 멈춥니다.");
	}
}
```

```java
package template;

public class ManualCar extends Car{

	@Override
	public void drive() {
		System.out.println("사람이 운전합니다.");
		System.out.println("사람이 핸들을 조작합니다.");
	}

	@Override
	public void stop() {
		System.out.println("사람이 브레이크로 정지합니다.");
	}
}
```

#### 테스트 클래스

```java
package template;

public class Test {

	public static void main(String[] args) {
		System.out.println("=== 자율 주행하는 자동차 ===");
		Car myCar = new AICar();
		myCar.run();
		
		System.out.println("=== 사람이 운전하는 자동차 ===");
		Car yourCar = new ManualCar();
		yourCar.run();
	}
}
```

**=== 자율 주행하는 자동차 ===**

**시동을 켭니다.**

**자율 주행합니다.**

**자동차가 알아서 방향을 전환합니다.**

**스스로 멈춥니다.**

**시동을 끕니다.**

**=== 사람이 운전하는 자동차 ===**

**시동을 켭니다.**

**사람이 운전합니다.**

**사람이 핸들을 조작합니다.**

**사람이 브레이크로 정지합니다.**

**시동을 끕니다.**

<br/>

* 상위 클래스를 상속 받은 하위 클래스에서는 템플릿 메서드를 재정의 할 수 없고 반드시 정의한 시나리오 대로 메서드를 구현해야 한다.

<br/>

---

<br/>

### Final 예약어

- `상수` : final 변수
  - 초기화 된 값을 변경 할 수 없는 변수

<br/>

##### 여러 자바 파일에서 공유하는 상수 값 정의하기

- ```java
  public static final int MIN = 1;
  public static final int MAX = 9999;
  public static final int ENG = 1001;
  public static final int MATH = 2001;
  public static final double PI = 3.14;
  public static final Stirng GOOD_MORNIG = "Good Morning!";
  ```

  - static으로 선언 했기 때문에 인스턴스 선언 없이 다른 클래스에서 클래스이름을 참조하여 사용 할 수 있다.

<br/>

##### 상속 할 수 없는 final 클래스

- 클래스를 final로 선언하면 상속 할 수 없다.
  - 상속을 할 경우 변수나 메서드를 재정의 할 수 있기 때문에 보안상 또는 기반 클래스가 변하면 안되는 경우 final로 선언한다.

<br/>

---

<br/>

### TDD(Test Drive Development)

- 테스트 주도 개발
- 테스트 프로그램을 먼저 작성 한 뒤 오류들이 해결 되도록 역순으로 코드 작성하는 개발 기법
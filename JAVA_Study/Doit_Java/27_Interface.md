# 인터페이스

- 인터페이스는 클래스 혹은 프로그램이 제공하는 기능을 명시적으로 선언하는 역할

  - 추상 메서드와 상수로만 이루어짐
  - 구현 코드가 없으므로 인스턴스 생성 불가
  - class 대신 interface를 적어 사용
  - 한 클래스에서 여러 인터페이스 함께 사용하여 구현 가능

  ```java
  package interface_practice;
  
  public interface Calc {
  	double PI = 3.14;
  	int ERROR = -9999999;
  	
  	int add(int num1, int num2);
  	int substract(int num1, int num2);
  	int times(int num1, int num2);
  	int divide(int num1, int num2);
  }
  ```

  - 인터페이스에서는 public abstract 예약어를 명시하지 않아도 컴파일 과정에서 자동으로 추상 메서드로 변환
  - 인터페이스에 선언한 변수는 public static final 예약어를 명시하지 않아도 컴파일 과정에서 자동으로 모두 상수로 변환

<br/>

---

<br/>

### 클래스에서 인터페이스 구현하기

- implements 예약어를 사용
  - 인터페이스에 선언 된 추상메서드를 모두 구현해야만 한다.

<br/>

#### 인터페이스

```java
package interface_practice;

public interface Calc {
	double PI = 3.14;
	int ERROR = -9999999;
	
	int add(int num1, int num2);
	int substract(int num1, int num2);
	int times(int num1, int num2);
	int divide(int num1, int num2);
}
```

#### 구현 클래스

```java
package interface_practice;

public class Calculator implements Calc {

	@Override
	public int add(int num1, int num2) {
		return num1 + num2;
	}

	@Override
	public int substract(int num1, int num2) {
		return num1 - num2;
	}

	@Override
	public int times(int num1, int num2) {
		return num1 * num2;
	}

	@Override
	public int divide(int num1, int num2) {
		if(num2 !=0)
		return num1 / num2;
		else
			return Calc.ERROR; // 0으로 나눌 경우 에러 출력
	}
	
	public void showInfo() {  // 인터페이스에 없지만 메서드 추가로 구현
		System.out.println("인터페이스를 구현하였습니다.");
	}
}
```

#### 테스트 클래스

```java
package interface_practice;

public class Test {

	public static void main(String[] args) {
		int num1 = 10;
		int num2 = 5;
		
		Calculator calc = new Calculator();
		System.out.println(calc.add(num1,num2));
		System.out.println(calc.substract(num1,num2));
		System.out.println(calc.times(num1,num2));
		System.out.println(calc.divide(num1,num2));
		calc.showInfo();
	}
}
```

<br/>

---

<br/>

### 인터페이스와 다형성

- 자바 8부터 추과된 `디폴트 메서드`와 `정적 메서드`를 제외하면 인터페이스는 선언부만 있는 껍데기에 불과
  - 클라이언트 프로그램에 어떤 메서드를 제공하는지 미리 알려주는 `명세서` 또는 `약속`의 역할
  - 인터페이스에는 어떤 매개변수를 사용해서 어떤 자료형 값이 반환 되는지 모두 적혀 있기 때문에 선언문만 읽어도 해당 구현 클래스를 어떻게 사용할지 알 수 있음
- 클라이언트 프로그램을 많이 수정하지 않고 기능을 추가하거나 다른 기능을 사용 할 수 있는 등 다형성을 구현하여 확장성 있는 프로그램을 만들 수 있음

<br/>

---

<br/>

### 디폴트 메서드와 정적 메서드

- 자바 8부터 추가 된 기능, 해당 기능이 있다고 해서 인터페이스가 인스턴스를 생성 할 수 있는 것은 아님
- 인터페이스를 구현한 여러 클래스마다 같은 기능을 넣어주고 싶을 때 매 클래스에서 반복 구현해야하는 수고를 줄이기 위한 `디폴트 메서드`
- 클래스를 생성하지 않아도 사용 할 수 있는 메서드가 필요할 때 인터페이스 만으로는 메서드를 호출 할 수 없었던 불편함을 줄이기 위한 `정적 메서드`

<br/>

#### 디폴트 메서드

- 기본 제공 메서드로 인터페이스에 구현하지만 인터페이스를 구현한 클래스가 생성되면 해당 클래스에 기본 제공 되는 메서드
- `default` 예약어를 사용하여 선언

```java
package interface_practice;

public interface Calc {
	
	default void descripthon() {
		System.out.println("정수 계산기를 구현합니다.");
	}
```

- 디폴트 메서드 호출하기

  - ```java
    public static void main(String[] args) {
    
    		Calculator calc = new Calculator();		
    		calc.description();
    	}
    ```

- 인터페이스에 구현 된 디폴트 메서드를 하위 클래스에서 오버라이딩 할 수 있다.

  - 재정의 원하는 구현 클래스에서 [Source - Override/Implement Methods...]를 눌러서 생성

<br/>

- 두 인터페이스를 함께 사용하는 클래스에서 디폴트 메서드가 중복 되는 경우
  - 인스턴스를 생성해야 호출 할 수 있는 메서드이기 때문에 두 인터페이스를 모두 구현했을 경우 디폴트 메서드가 중복 되었다는 오류가 발생 함
  - 구현하는 클래스에서 디폴트 메서드를 재정의하여 사용해야 함

<br/>

#### 정적 메서드

- static 예약어를 사용하여 선언

- 클래스 생성과 무관하게 사용 가능한 메서드

- 인터페이스 이름으로 직접 참조하여 사용

  - ```java
    package interface_practice;
    
    public interface Calc {
    	
    	static int total(int[] arr) {
    		int total = 0;
    		
    		for(int i : arr) {
    			total += i;
    		}
    		return total;
    	}
    ```

  

- 정적 메서드 호출하기

  - ```java
    package interface_practice;
    
    public class Test {
    
    	public static void main(String[] args) {
    			Calculator calc = new Calculator();
    			
    			int[] arr = {1,2,3,4,5};
    		System.out.println(Calc.total(arr)); // 인터페이스 Calc에 바로 참조
    		
    	}
    }
    ```

    - 15가 출력 된다.

- 두 인터페이스를 함께 사용하는 클래스에서 정적 메서드가 중복 되는 경우

  - 인스턴스 생성과 상관 없이 사용 할 수 있으므로 인터페이스를 참조하여 특정 메서드를 호출 할 수 있음

<br/>

#### Private 메서드

- 자바 9부터 인터페이스에  private 메서드 구현이 가능해짐
- 인터페이스를 구현한 클래스에서 사용하거나 재정의 불가
  - 기존에 구현된 코드를 변경하지 않고 인터페이스를 구현한 클래스에서 공통으로 사용하는 경우 사용
  - 클라이언트 프로그램에 제공할 기본 기능을 private method로 구현하기도 함
  - 코드 재사용성을 높일 수 있음
  - 추상 메서드에 private 예약어 사용 불가
  - static 예약어와 함께 사용 가능

<br/>

#### 인터페이스 상속하기

- 인터페이스끼리도 상속이 가능

- 구현 코드를 통한 기능 상속이 아니므로 `형 상속(type inheritance)`라고 함

  - 여러 인터페이스 동시 상속 가능
  - 상위 인터페이스에 선언한 추상 메서드를 모두 갖게 됨

- 상속 할 때 extends 예약어 사용

  - ```java
    public interface MyInterface extends YourInterface, HisInterface{
    	// YourInterface, HisInterface 인터페이스들의 모든 추상 메서드 구현
    }
    ```

  - 상속 받은 상위 인터페이스들의 추상 메서드를 모두 구현해야 함

<br/>

#### 인터페이스 구현과 클래스 상속 함께 사용 가능

- ```java
  public class ClassName extends Parents implements InterfaceName{
  	// 인터페이스 추상 메서드 구현
      // 상속 받은 상위 클래스의 멤벼 변수와 메서드 사용 가능
  }
  ```

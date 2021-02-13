# 내부 클래스

- 클래스 내부에 선언한 클래스
  - 외부 클래스와 밀접한 관계가 있거나 그 외 클래스들과 협력할 일이 없을 경우

```java
class Out {
	class In{
		...
	}
}
```

<br/ >

#### 내부 클래스와 변수

```java
class ABC{									//외부 클래스
	int instance_var;						//인스턴스 변수
	static int static_var;					//정적 변수
	
	public void method(){
		int local_var;						//지역 변수
    	}
    //===============================================
    class InnerClass{						//인스턴스 내부 클래스
        static class StaticInnerClass {		//정적 내부 클래스
            ...
        }
	 }
    public void method() {					//메서드
        class LocalInnerClass{				//지역 내부 클래스
            ...
        }
    }
    //===============================================
    public void outMethod() {				//익명 내부 클래스 작성
        public String innerMethod(){
            System.out.println("익명 내부 클래스");
        }
    };										// 익명 클래스 끝에 ; 붙임
}
```

<br/ >

| 종류                 | 구현 위치                                         | 사용할 수 있는 외부 클래스 변수        | 생성 방법                                                    |
| -------------------- | ------------------------------------------------- | -------------------------------------- | ------------------------------------------------------------ |
| 인스턴스 내부 클래스 | 외부 클래스 멤버 변수와 동일                      | 외부 인스턴스 변수<br />외부 전역 변수 | 외부 클래스를 먼저 만든 후 내부 클래스 생성                  |
| 정적 내부 클래스     | 외부 클래스 멤버 변수와 동일                      | 외부 전역 변수                         | 외부 클래스와 무관하게 생성                                  |
| 지역 내부 클래스     | 메서드 내부에 구현                                | 외부 인스턴스 변수<br />외부 전역 변수 | 메서드를 호출 할 때 생성                                     |
| 익명 내부 클래스     | 메서드 내부에 구현<br />변수에 대입하여 직접 구현 | 외부 인스턴스 변수<br />외부 전역 변수 | 메서드를 호출할 때 생성<br />인터페이스 타입 변수에 대입할 때 new 예약어를 사용하여 생성 |

<br/ >

### 인스턴스 내부 클래스 (Instance Inner Class)

- 인스턴스 변수 선언 할때와 같은 위치에 선언
  - 해당 클래스 내부에서만 사용하는 객체를 선언 할 때 사용
- 외부 클래스 생성 후 내부 클래스 생성 되어 사용 할 수 있음
  - 때문에 내부 클래스에서 static 사용 불가

<br/ >

##### 클래스

```java
package innerclass;

class OutClass {

	private int num = 10;
	private static int sNum = 20;
	
	private InClass inClass;		// 내부 클래스 자료형 변수를 먼저 선언
	
	public OutClass() {				// 외부 클래스 디폴트 생성자
		inClass = new InClass();
	}
	
	//===============================================
	class InClass {
		int inNum = 100;
		
		void inTest() {			// 내부 클래스의 메서드
			System.out.println("OutClass num =" + num + "(외부 클래스의 인스턴스 변수)");
			System.out.println("OutClass sNum =" + sNum + "(외부 클래스의 정적 변수)");
		}
	}
	public void usingClass() {
		inClass.inTest();
        System.out.println(inClass.inNum);
	}
}
```

<br/>

1. OutClass 인스턴스를 생성하면 InnerClass 인스턴스도 생성 되어 private 변수 inClass에 할당 됨
2. OutClass 인스턴스인 outClass를 참조하여 내부 클래스의 usingClass()메서드를 호출 
3. usingClass()메서드 내부의 inClass를 참조한 inTest() 메서드 실행
4. 내부 클래스 메서드에서 외부 클래스의 인스턴스 변수를 가져와서 출력문 실행
5. inTest()메서드 종료 후 inClass를 참조한 inNum 변수 값 출력

<br/>

##### 테스트 클래스

```java
public class InnerTest{
	public static void main(String[] args) {
		OutClass outClass = new OutClass();
		System.out.println("외부 클래스 이용하여 내부 클래스 기능 호출");
		outClass.usingClass();				//내부 클래스의 메서드
	}
}
```

**외부 클래스 이용하여 내부 클래스 기능 호출**

**OutClass num =10(외부 클래스의 인스턴스 변수**)

**OutClass sNum =20(외부 클래스의 정적 변수)**

**100**

<br/>

### 정적 내부 클래스 (Static Inner Class)

- 외부 클래스의 생성과 무관하게 사용 할 수 있어야하거나 정적 변수가 필요할 때 사용
- 외부클래스의 멤버 변수와 같은 위치에 정의하여 static 예약어 사용
  - 외부클래스 생성과 무관하게 사용이 가능해야 하므로 클래스 내부에 외부클래스의 인스턴스 변수 사용 불가

<br/ >

##### 클래스

```java
package innerclass;

class OutClass {

	private int num = 10;
	private static int sNum = 20;
	
	static class InStaticClass {
		int inNum = 100;
		static int sInNum = 200;
		
		void inTest() {
			System.out.println("InStaticClass inNum = " + inNum + "(내부 클래스의 인스턴스 변수 사용)");
			System.out.println("InStaticClass sInNum =" + sInNum + "(내부 클래스의 장적 변수 사용)");
			System.out.println("OutClass sNum =" + sNum + "(외부 클래스의 정적 변수 사용)");
		}
	
	static void sTest() {
		System.out.println("OutClass sNum =" + sNum + "(외부 클래스의 정적 변수 사용)");
		System.out.println("InStaticClass sInNum =" + sInNum + "(내부 클래스의 장적 변수 사용)");
		}
	}
}
```

<br/>

##### 테스트 클래스

````java
public class InnerTest{
	public static void main(String[] args) {

		OutClass.InStaticClass sInClass = new OutClass.InStaticClass(); // 외부 클래스 생성 없이 바로 정적 내부 클래스 생성
		System.out.println("정적 내부 클래스의 일반 메서드 호출");
		sInClass.inTest();
		System.out.println();System.out.println("정적 내부 클래스의 정적 메서드 호출");
		OutClass.InStaticClass.sTest();
	}
}
````



<br/ >

| 정적 내부 클래스 메서드 | 변수 유형                                    | 사용 가능 여부 |
| ----------------------- | -------------------------------------------- | -------------- |
| **일반 메서드**         | 외부 클래스의 인스턴스 변수 **(num)**        | x              |
|                         | 외부 클래스의 정적 변수 **(sNum)**           | o              |
| void in Test()          | 정적 내부 클래스의 인스턴스 변수 (inNum)     | o              |
|                         | 정적 내부 클래스의 정적 변수 **(inNum)**     | o              |
| **정적 메서드**         | 외부 클래스의 인스턴스 변수 **(num)**        | x              |
|                         | 외부 클래스의 정적 변수 **(sNum)**           | o              |
| static void sTest()     | 정적 내부 클래스의 인스턴스 변수 **(inNum)** | x              |
|                         | 정적 내부 클래스의 정적 변수 **(sinNum)**    | o              |

<br/ >

### 지역 내부 클래스 (Local Inner Class)

- 지역 변수처럼 메서드 내부에 클래스를 정의하여 사용
  - 해당 매서드 내부에서만 클래스 사용 가능
- 인터페이스를 구현하는 클래스로도 사용
- 지역 내부 클래스에서 사용하는 지역 변수는 상수로 처리 됨
  - 메서드가 종료 되면 스택 메모리에서 제거 되기 때문에 사용 할 수 없음
  - 때문에 정상적인 사용을 위해 지역 내부 클래스의 매개변수와 내부 클래스에서 사용하는 지역 변수에 대해 상수로 관리 함
  - 자바 8부터는 final을 명시하지 않아도 final로 관리 됨

<br/ >

##### 클래스

```java
package innerclass;

public class Outer {

	int outNum = 100;
	static int sNum = 200;
	
	Runnable getRunnable(int i) {
		int num = 100;							//지역 변수
		
		class MyRunnable implements Runnable {	//지역 내부 클래스
			int localNum = 10;					//지역 내부 클래스의 인스턴스 변수
			
			@Override
			public void run() {
				System.out.println("i = " + i);
				System.out.println("num = " + num);
				System.out.println("localNum = " + localNum);
				System.out.println("outNum = " + outNum + "(외부 클래스 인스턴스 변수)");
				System.out.println("Outer.sNum = " + Outer.sNum + "(외부 클래스 정적 변수)");
			}
		}
		return new MyRunnable();				// getRunnable(int i) 메서드의 반환 값으로 지역 내부 클래스 객체를 반환
	}
}
```

<br/>

##### 테스트 클래스

```java
package innerclass;

public class LocalInnerTest {

	public static void main(String[] args) {
		Outer out = new Outer();
		Runnable runner = out.getRunnable(10);		//메서드 호출
		runner.run();
	}
}
```

<br/>

**i = 10**

**num = 10**

**localNum = 10**

**outNum = 100(외부 클래스 인스턴스 변수)**

**Outer.sNum = 200(외부 클래스 정적 변수)**

<br/ >

### 익명 내부 클래스 (Anonymous Inner Class)

- 클래스를 생성하여 반환 할때 지역 내부 클래스 이름을 명시해서 생성 함
  - 반환이 아니라 바로 사용 할 경우는 이름 없이 바로 사용\

<br/>

##### 클래스

```java
package innerclass;

public class Outer2 {
	Runnable getRunnable(int i) {
		int num = 100;
		
		return new Runnable() {
			@Override
			public void run() {
				System.out.println(i);
				System.out.println(num);
			}
		};
	}
	Runnable runner = new Runnable() {
		@Override
		public void run() {
			System.out.println("Runnable이 구현된 익명 클래스 변수");
		}
	};
}
```

<br />

##### 테스트 클래스

```java
package innerclass;

public class AnonymousInnerTest {

	public static void main(String[] args) {
		Outer2 out = new Outer2();
		Runnable runnerble = out.getRunnable(10);
		runnerble.run();
		out.runner.run();
	}
}
```

**10**

**100**

**Runnable이 구현된 익명 클래스 변수**

<br />

- 단 하나의 인터페이스 또는 추상 클래스를 바로 생성 할 수 있음
- 인터페이스는 인스턴스로 생성 할 수 없다
  - 때문에 인터페이스를 생성하려면 인터페이스 구현체가 필요함
  - 인터페이스나 추상 클래스 자료형으로 변수을 선언한 후 익명 내부 클래스를 생성해 대입하여 사용
- 변수에 직접 대입하여 사용하거나 메서드 내부에서 인터페이스나 추상 클래스를 구현하여 사용함
  - 이때 사용하는 지역 변수는 상수화 되기 때문에 메서드가 종료 되어도 사용 가능


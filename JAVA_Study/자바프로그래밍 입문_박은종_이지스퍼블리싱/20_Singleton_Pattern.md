### *** static 응용 - 싱글톤 패턴**

**디자인 패턴**

- 객체 지향 프로그램을 어떻게 구현해야 좀 더 유연하고 재활용성이 높은 프로그램을 만들 수 있는지를 정리한 내용
- 이 중 인스턴스를 단 하나만 생성하는 패턴을 싱글톤 패턴(singleton pattern)이라고 함
- 싱글톤 패턴은 실무나 여러 프레임워크에서 많이 사용하는 패턴

**1. 생성자를 private으로 만들기**

- 생성자가 하나도 없는 클래스는 컴파일러가 자동으로 디폴트 생성자 코드를 넣어 주는데 이때는 항상 public임
- 생성자의 접근제어자가 public이면 외부 클래스에서 인스턴스를 여러개 생 성 할 수 있으므로 private으로 설정 필요

```java
package singlton;

public class Company {

	private Company () {}

	}
}
```



**2. 클래스 내부에 static으로 유일한 인스턴스 생성하기**

- 사용할 유일한 인스턴스를 Company 클래스 내부에 생성
- private으로 설정하여 외부에서 접근하지 못하도록 제한

```java
package singlton;

public class Company {

	private static Company instance = new Company(); // 유일한 인스턴스
	
	private Company () {}

}
```



**3. 외부에서 참조할 수 있는 public 메서드 만들기**

- 외부에서 유일한 인스턴스에 접근하여 사용이 가능하도록 public 메서드 생성
- public메서드는 유일하게 생성한 인스턴스를 반환해줌
- 이때 반환 메서드는 인스턴스 생성과 무관하게 호출할 수 있어야하므로 반드시 static 변수로 설정

```java
package singlton;

public class Company {

	private static Company instance = new Company(); // 유일한 인스턴스
	
	private Company () {}

	public static Company getInstance() {
		if(instance == null)	//인스턴스 값이 없을 경우
			instance = new Company();	//Company 클래스 생성
		return instance;	// 유일하게 생성한 인스턴스 반환
	}
}
```



**4. 실제로 사용하는 코드 만들기**

- 인스턴스가 private이기 때문에 외부 클래스에서는 Company를 바로 생성 할 수 없다.
- 때문에 static으로 제공되는 getInstance()메서드를 호출해야 한다.

```java
package singlton;

public class CompanyTest {

	public static void main(String[] args) {

		Company c1 = Company.getInstance();
		Company c2 = Company.getInstance();
		System.out.println(c1 == c2); // 두 참조 변수가 가진 인스턴스 값이 같은지 확인

		}

}
```

**true**

- c1, c2 두 변수의 주소 값이 같음. (같은 참조 값을 가짐)
- 몇번을 호출하여도 모두 같은 주소의 인스턴스가 반환 됨
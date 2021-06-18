## J-Unit Test

##### 테스트를 위한 계산기 클래스

```java
package test;

public class Calculator {

	int add(int i, int j ) {
		return i+j;
	}
	
	int subtract(int i, int j) {
		return i - j;
	}
	
	int multiply(int i, int j) {
		return i * j;
	}
	int divide(int i, int j) {
		return i / j;
	}
	
	public static void main(String[] args) {
		Calculator cal = new Calculator();
		System.out.println(cal.add(3,4));
		System.out.println(cal.subtract(5, 4));
		System.out.println(cal.multiply(2, 6));
		System.out.println(cal.divide(8, 4));
	}
}
```

<br />

##### Test Class

```java
package test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CalculatorTest {
	
	private Calculator cal;
	
	// 각 테스트 실행 전에 단 한번 실행
	@BeforeEach
	public void setup() {
		cal = new Calculator();
		System.out.println("before");
	}
	@Test
	public void add() {
		assertEquals(9, cal.add(6, 3));
		System.out.println("더하기 : " +  cal.add(6, 3));
	}
	@Test
	public void subtract() {
		assertEquals(3, cal.subtract(6, 3));
		System.out.println("빼기 : " +  cal.subtract(6, 3));
	}
	@Test
	public void multiply() {
		assertEquals(18, cal.multiply(6, 3));
		System.out.println("곱하기 : " +  cal.multiply(6, 3));
	}
	@Test
	public void divide() {
		assertEquals(2, cal.divide(6, 3));
		System.out.println("나누기 : " +  cal.divide(6, 3));
	}
    // 각 테스트 실행 후에 단 한번 실행
	@AfterEach
	public void teardown() {
		System.out.println("teardown");
	}
}
```

<br />

##### 출력 결과

```shell
빼기 : 3
나누기 : 2
더하기 : 9
곱하기 : 18
```

- 테스트 성공

<br />

- `@Before` 어노테이션 내부여야만  `@Runwith`, `@Rule`에서 초기화 된 객체에 접근할 수 있다는 제약 사항이 있기 때문에 `@Before` 애너테이션을 사용해 테스트 메소드에 대한 초기화 작업을 하는 것이 권장 된다.
- `@Before / @After` -> `@BeforeAll / @AfterAll`과 `@BeforeEach / @AfterEach `로 분리
  - `@BeforeAll / @AfterAll` : 클래스 내의 모든 테스트보다 먼저 실행, static 으로 선언
  - `@BeforeEach / @AfterEach` : 각 테스트 실행 전 1회 실행, 기존 `@Before / @After`과 동일

[JUnit5 기본 사용법 참고 블로그](https://gmlwjd9405.github.io/2019/11/26/junit5-guide-basic.html)

##### [J-Unit Assert Class](https://junit.org/junit5/docs/5.0.1/api/org/junit/jupiter/api/Assertions.html)

- `assertEquals(var, var)` 메서드
  - 첫 번째 인자는 기대하는 결과 값(expected)
  - 두 번째 인자는 실행 한 결과 값(actual)

- 최근에는 `AssertJ` 클래스를 사용

```java
assertEquals(expected, actual);

assertThat(actual).isEqualTo(expected);
```

- 두 코드는 같은 코드이나 `assertEquals`는 인자의 기대 값, 결과 값을 혼동 할 수 있다.
  - `assertThat(결과 값).isEqulsTo(기대 값)`은 조금 더 직관적으로 사용 할 수 있다.

```java
@Test void a_few_simple_assertions() { 
    assertThat("The Lord of the Rings").isNotNull() 
                                        .startsWith("The") 
                                        .contains("Lord") 
                                        .endsWith("Rings"); 
}
```

[AsserJ 참고 블로그](https://pjh3749.tistory.com/241)

<br />


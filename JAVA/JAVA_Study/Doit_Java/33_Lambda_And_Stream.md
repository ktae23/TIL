# Lambda

- 자바 8부터 지원하는 함수형 프로그래밍 방식
- 메서드 정의 후 호출하여 사용하는 것이 아닌 함수 이름이 없는 익명 함수를 생성하여 사용하는 방식

```java
int add(int x, int y){
    return x + y;
}

//(매개변수) -> {실행문;}
(int x, int y) -> {retrusn x + y;}
```

<br />

### 람다식 문법 살펴보기

##### 매개변수 자료형과 괄호 생략

- 매개 변수 자료형 생략 가능
  - 매개 변수가 하나일 경우 소괄호 생략 가능

```java
str -> {System.out.prinln(str);}
```

<br/>

##### 중괄호 생략

- 중괄호 안의 구현부가 한 문장일 경우 중괄호 생략 가능
  - 구현부가 한 문장이더라도 return문을 작성할 경우 생략 할 수 없음

```java
str -> System.out.prinln(str); // 중괄호 생략 가능
str -> {return str.length();} // 중괄호 생략 불가
```

<br/>

##### return 생략

- 중괄호 안의 구현 부분이 return문 하나일 경우 중괄호와 return을 모두 생략 가능

```java
(x, y) -> x+ y;	// 두 값을 더하여 반환
str -> str.length();	// 문자열의 길이를 반환
```

<br/>

##### If문 생략

- 생략 할 수 있는 부분은 생략해서 구현

```java
(x, y) -> {
	if (x >= y) return x;
	else return y;
}

// 생략 할 경우

(x, y) -> x>= y ? x : y;
```

<br />

#### 함수형 인터페이스

- 자바에서는 참조 변수 없이 메서드를 호출 할 수 없다
  - 람다식을 구현하기 위해서는 함수형 인터페이스를 만들고 인터페이스에 람다식으로 구현할 메서드를 선언하여 사용
- 하나의 메서드를 구현하여 인터페이스형 변수에 대입하여 사용
  - 인터페이스에 두 개 이상의 메서드를 가져서는 안됨

- @FunctionalInterface 애노테이션
  - 함수형 인터페이스임을 명시 해줌
  - 메서드를 두개 이상 선언하면 오류 발생 함

```java
public interface MyNumber {
	int getMax(int numq, int num2) //추상 메서드를 단 하나만 가지는 인터페이스 생성
}

// ========================================

public class TEstMyNumber {
    public static void main(String[] args) {
        MyNumber max = (x, y) -> (x >= y) ? x : y;	// 인터페이스형 참조 변수 max에 람다식 대입
        System.out.println(max.getMax(10,20));	// 인터페이스형 변수 참조하여 메서드 호출
    }
}
```

<br />

#### 람다식으로 인터페이스 구현

- 기존 인터페이스 구현 방식은 구현 클래스를 만들어 코드를 작성 한 뒤 해당 클래스 또는 메서드를 호출하여 사용
  - 람다식으로 인터페이스를 구현 할 때는 클래스를 따로 생성하지 않고 바로 메서드로 구현

```java
public interface StringConcat { // 람다식으로 표현 할 메서드 단 하나만 가진 인터페이스 생성
    public void makeString(String s1, String s2);
}

// ===============================================

public class TestStringConCat {
	public static void main(String[] args) {
		String s1 = "Hello";
		String s2 = "World";
		StringConcat concat2 = (s, v) -> System.out.println(s + "," + v); 
        				// 인터페이스형 참조 변수에 객체 생성 없이 바로 람다식 구현
		concat2.makeString(s1,s2);
	}
}
```

<br />

#### 익명 객체를 생성하는 람다식

- 람다식으로 메서드를 구현하여 호출하면 내부적으로 익명 클래스가 생성 되어 익명 객체를 통해 메서드가 호출 됨

```java
 // 위의 에제의 람다식은 내부적으로 아래 코드 블럭처럼 익명 클래스가 생성 되어 사용 됨

StringConcat concat2 = new StringConcat() {
	@Override
	public void makeString(String s1, String s2) {
		System.out.println(s1 + "," + s2);
	}
};
```

<br />

#### 함수를 변수처럼 사용하는 람다식

- 람다식을 이용하면 구현 된 함수를 변수처럼 사용 할 수 있다
  - 변수를 사용하는 경우는 크게 아래의 세 경우

| 변수를 사용하는 경우                       | 예시                   |
| ------------------------------------------ | ---------------------- |
| 특정 자료형으로 변수 선언 후 대입하여 사용 | int a = 10;            |
| 매개변수로 전달하기                        | int add(int x, int y); |
| 메서드의 반환 값으로 반환하기              | return num;            |

<br />

##### 인터페이스형 변수에 람다식 대입하기

```java
public interface PrintString {
    public void showString(String str);
}

// ==============================

PrintString lambdaStr = s -> System.out.println(s); // 인터페이스형 변수에 람다식 대입
lambdaStr.showString("hello lambda");	// 변수를 참조하여 람다식 구현부 호출
```

<br />

##### 매개변수로 전달하는 람다식

- 인터페이스형 자료형의 매개변수로 전달

```java
public interface PrintString {
    public void showString(String str);
}

// ==============================

public class Testlambda {
	public static void main(String[] args) {
        PrintString lambdaStr = s -> System.out.println(s);
        lambdaStr.showString("hello lambda");
        showMyString(lambdaStr); //메서드의 매개변수로 람다식을 대입한 인터페이스형 변수 전달
	}
    public static void showMyString(PrintString p){ // 매개변수를 인터페이스 형으로 
        p.showString("hello lambda2");
    }
}
```

**hello lambda**

**hello lambda2**

<br />

##### 반환 값으로 쓰이는 람다식

- 메서드의 반환형을 람다식의 인터페이스형 변수로 선언하면 반환 값으로 람다식을 반환 할 수 있음

```java
public static PrintString returnString() {
	PrintString str = s -> System.out.println(s + "world");
	return str;
}

// 더 간단하게 작성 가능

public static PrintString returnString() {
	return s -> System.out.println(s + "world");
}
```

<br />

# Stream

- 특정 기준에 따라 정렬(sorting), 특정 값을 제외하고 출력(filter) 등의 자료 처리 기능을 구현해 놓은 클래스
- 배열, 컬렉션 등의 자료를 일관성 있게 처리 하는 데 사용
- 처리하려는 자료가 무엇이든 같은 방식으로 메서드를 호출 할 수 있음
  - 자료를 추상화 했다고 표현
  - 입출력을 위한 I/O Stream과 별개의 개념

<br/>

##### 정수 5개를 값으로 가지고 있는 배열과 모든 요소를 출력하는 출력문

```java
int[] arr = {1,2,3,4,5};
for(int i=0; i<arr.length; i++){
	System.out.println(arr[i]);
}
```

<br/>

##### 스트림을 이용한 출력 문

```java
int[] arr = {1,2,3,4,5};
Arrays.stream(arr).forEach(n -> System.out.println(n));
//스트림 생성부(매개변수).요소를 하나씩 꺼내어 출력하는 출력부
```

<br/>

### 스트림 연산

#### 중간 연산

- 자료를 거르거나 변경하여 또 다른 자료를 내부적으로 생산

- filter()
  - 입력 된 조건이 참일 경우에만 추출
  - 길이가 5 이상인 문자열만 추출하여 출력하는 코드

```java
sList.Stream().filter(s -> s.length() >= 5).forEach(s -> System.out.println(s));
//스트림 생성   .중간 연산                     .최종 연산
```

<br/>

- map()
  - 클래스가 가진 자료 중 이름만 출력하는 경우

```java
customerList.Stream().map(c -> c.getName()).forEach(s -> System.out.println(s));\
//스트림 생성 .중간 연산                       .최종 연산
```

<br/>

<br/>

#### 최종 연산

- 생성된 내부 자료를 소모해 가면서 연산을 수행되므로 한번 호출되고 나면 해당 스트림은 사용 종료
  - 연산의 마지막에 한 번만 호출 됨

- forEach()
  - 요소를 하나씩 꺼내는 기능
- count()
  - 요소의 개수를 출력
- sum()
  - 요소의 합을 구함
- reduce()
  - 프로그래머가 지정한 기능 수행
  -  매개변수로 초기값과 람다식을 입력 받음

<br />

### 스트림 생성 및 사용

#### 정수 배열에 스트림 생성하여 사용

<br />

```java
package stream;

import java.util.Arrays;

public class IntArrayTest {

	public static void main(String[] args) {
		
	int[] arr = {1,2,3,4,5};
	
	int sumVal = Arrays.stream(arr).sum(); // sum()연산으로 arr배열에 저장된 값을 모두 더함
	int count = (int)Arrays.stream(arr).count(); // count()연산으로 arr 배열의 요소 개수를 반환
	
	System.out.println(sumVal);
	System.out.println(count);
	}
}

```

**15**

**5**

<br />

#### Collection에서 스트림 생성하고 사용

```java
List<String> sList = new ArrayList<String>();
sList.add("Tomas");
sList.add("Edward");
sList.add("Jack");
```

- Collection에서 stream()메서드를 사용하면 제네릭형을 사용해 자료형을 명시 할 수 있음

```java
// Collection 인터페이스의 메서드 - Stream<E> stream()
Stream<String> stream = sList.stream();
```

- 이렇게 생성된 스트림은 내부적으로 ArrayList의 모든 요소를 가지고 있음

<br/>

##### 테스트 클래스

```java
package stream;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ArrayListStreamTest {

	public static void main(String[] args) {
		List<String> sList = new ArrayList<String>();
		sList.add("Tomas");
		sList.add("Edward");
		sList.add("Jack");
		
		Stream<String> stream = sList.stream();	//스트림 생성
		stream.forEach(s -> System.out.println(s + " "));	//배열 요소를 하나씩 출력
		System.out.println();
		
		sList.stream().sorted().forEach(s->System.out.println(s));
        //스트림 새로 생성.정렬하고.배열 요소를 하나씩 출력
	}
}
```

**Tomas** 

**Edward** 

**Jack** 

<br/>

**Edward**

**Jack**

**Tomas**

<br />

### 스트림의 특징

- 자료의 대상과 관계없이 동일한 연산을 수행한다

- 한 번 생성하고 사용한 스트림은 재사용 불가

  - 요소를 순회하면서 출력에 사용하는데 이를 `소모`한다고 한다.

- 스트림의 션산은 기존 자료를 변경하지 않음

  - 스트림 연산을 위해 사용하는 별도의 메모리 공간이 존재 함

- 스트림의 연산은 중간 연산과 최종 연산이 있다.

  - 중간 연산은 여러개 사용 가능

  - 최종 연산은 단 한번만 적용

  - 최종 연산이 호출되어야만 중간 연산이 적용 됨

    - 이를 지연 연산(lazy evaluation)이라고 함

      

#### 프로그래머가 기능을 지정하는 reduce()연산이 있다.

```java
T reduce(T indentify, BinaryOperator<T> accumulator)
		//초기 값, 람다식을 구현하여 각 요소가 수행해야 할 기능이 됨
```

- 최종 연산

- BinaryOperator 인터페이스

  - 함수형 인터페이스로 apply()메서드를 반드시 구현해야 함

- apply() 메서드

  - 같은 자료형의 두 개의 매개변수와 한 개의 반환 값을 가짐
  - reduce() 메서드가 호출 될 때 BinaryOperator의 apply()메서드가 호출 됨

  <br/>

#### BinaryOperator구현 방식

- 구현한 람다식을 직접 입력 가능

```java
package stream;

import java.util.Arrays;

public class ReduceTest {
	public static void main(String[] args) {
		
		String[] greetings = {"안녕하세요~~~", "hello", "Good morning", "반갑습니다.^^"};
		System.out.println(Arrays.stream(greetings).reduce("", (s1, s2) -> {
			if(s1.getBytes().length >= s2.getBytes().length) return s1;
			else return s2;
			}));
	} 
}
```

<br/>

- 람다식이 길면 인터페이스를 구현한 클래스를 생성하여 대입 가능

##### 구현클래스

```java
package stream;

import java.util.function.BinaryOperator;

public class CompareString implements BinaryOperator<String>{

	@Override
	public String apply(String s1, String s2) {
		if(s1.getBytes().length >= s2.getBytes().length) return s1;
		else return s2;
	}
}
```

<br/>

##### 테스트클래스

```java
package stream;

import java.util.Arrays;

public class ReduceTest {
	public static void main(String[] args) {
		
		String[] greetings = {"안녕하세요~~~", "hello", "Good morning", "반갑습니다.^^"};
		
        											//binaryOperator에 구현 클래스 객체 입력
		String str = Arrays.stream(greetings).reduce(new CompareString()).get();
		System.out.println(str);		
	} 
}
```


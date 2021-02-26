# Objcet Class

- 모든 자바 클래스의 최상위 클래스
- 직접 만드는 클래스, 기존 KDK 클래스 등 모든 클래스는 Object 클래스로부터 상속을 받음

```java
Class Example {
	예시 클래스
}

// ->
class Example extends Object {
    // 오브젝트 클래스 상속문이 생략되어 자동으로 컴파일 됨.
}
```

<br />

## 주로 사용되는오브젝트 클래스의 메서드

| 메서드                     | 설명                                                         |
| -------------------------- | :----------------------------------------------------------- |
| String toSring()           | 객체를 문자열로 표현하여 반환합니다. <br />재정의하여 객체에 대한 설명이나 특정 멤버 변수 값을 반환합니다. |
| boolean equals(Object obj) | 두 인스턴스가 동일한지 여부를 반환 합니다.<br />재정의하여 논리적으로 동일한 인스턴스임을 정의할 수 있습니다. |
| int hashCode()             | 객체의 해시 코드 값을 반환합니다.                            |
| Object clone()             | 객체를 복제하여 동일한 멤버 변수 값을 가진 새로운 인스턴스를 생성합니다. |
| Class getClass()           | 객체의 Class 클래스를 반환합니다.                            |
| void fianlize()            | 인스턴스가 힙 메모리에서 제거될 때 가비지 컬렉터(GC)에 의해 호출되는 메서드입니다.<br />네트워크 연결 해제, 열려 있는 파일 스트림 해제 등을 구현합니다. |
| void wait()                | 멀티스레드 프로그램에서 사용하는 메서드입니다.<br />스레드를 '실행 대기 상태'(non runnable)로 만듭니다. |
| coid notify()              | wait() 메서드에 의해 기다리고 있는 스레드(non runnable 상태)를 실행 가능한 상태 (runnable)로 가져옵니다. |

<br />

---

#### 1. toString() 메서드

- 객체를 `문자열`로 표현하여 반환
- Object 클래스를 상속받은 모든 클래스에서 재정의하여 사용 할 수 있음

```java
// toString() 메서드의 원형 - 인스턴스 주소값 반환
getClass().getName() + '@' + Integer.toHexString(hashcode())
```

- String과 Integer 클래스는 인스턴스 주소값이 아닌 문자열과 정수가 출력되도록 toString() 메서드가 이미 재정의 되어 있음

<br />

<br />

---

#### 2. equals() 메서드

- 원래의 기능은 두 인스턴스의 주소 값을 비교하여 `boolean` 값 (true / false) 반환

- 서로 다른 주소 값을 가질 때도 같은 인스턴스라고 정의 할 수 있는 경우도 있음
  - 따라서 물리적 동일성 뿐 아니라 논리적 동일성을 구현할 때도 equals() 메서드를 재정의 하여 사용

<br />

##### 2.1 Object 클래스의 equals() 메서드

- 두 인스턴스의 주소 값이 같은 경우

##### Student 클래스

```java
package equals;

public class Student {
	int studentID;
	String studentName;
	
	public Student() {}
	
	public Student(int studentID, String stuentName) {
		this.studentID=studentID;
		this.studentName=studentName;
	}

	public String toStirng() {
		return studentID + "," + studentName;
	}
}
```

##### 테스트 클래스

````java
package equals;

public class Test {

	public static void main(String[] args) {
		Student student1 = new Student(100, "이상원");
		Student student2 = student1;
		Student student3 = new Student(100, "이상원");
		
		// 동일한 주소의 두 인스턴스 비교
		if(student1 == student2) { // 기호로 비교
			System.out.println("1과 2의 주소는 같습니다.");
		}else {
			System.out.println("1과 2의 주소는 다릅니다.");	
		}
		
		if(student1.equals(student2)) { // equals() 메서드로 비교
			System.out.println("1과 2는 동일합니다.");
		}else {
			System.out.println("1과 2는 동일하지 않습니다.");
		}
		
		//동일한 값을 넣었지만 인스턴스 주소가 다른 경우
		if(student1 == student3) { // 기호로 비교
			System.out.println("1과 3의 주소는 같습니다.");
		}else {
			System.out.println("1과 3의 주소는 다릅니다.");	
		}
		
		if(student1.equals(student3)) { // equals() 메서드로 비교
			System.out.println("1과 3은 동일합니다.");
		}else {
			System.out.println("1과 3은 동일하지 않습니다.");
		}

	}
}

````

**1과 2의 주소는 같습니다.**

**1과 2는 동일합니다.**

**1과 3의 주소는 다릅니다.**

**1과 3은 동일하지 않습니다.**

<br />

- 결과에서 볼 수 있듯이 주소가 같을 때 equals()메서드는  `true`값을 리턴합니다.
- 하지만 인스턴스 주소가 다르다 해도 학번과 이름이 같으면 사실 같은 학생으로 봐야 한다.
  - 때문에 물리적 같음이 아닌 논리적 같음을 확인하도록 재정의 하여 사용할 수 있음

<br />

##### 2.2 String과 Integer 클래스의 equals() 메서드

- 이미 재정의 되어 있음
- 주소값이 아닌 String은 문자열, Integer는 정수 값을 리턴
- 물리적 주소가 아닌 같은 문자열, 정수 값일 경우 `true`값을 리턴

```java
package equals;

public class Test {

	public static void main(String[] args) {

		String str1 = new String("abc");
		String str2 = new String("abc");
		
		System.out.println(str1 == str2);
		System.out.println(str1.equals(str2));
		
		Integer i1 = new Integer(100);
		Integer i2 = new Integer(100);
		
		System.out.println(i1 == i2);
		System.out.println(i1.equals(i2));
	}
}
```

**false**

**true**

**false**

**true**

<br />

- String 클래스에서 같은 `"abc"`지만 각각 새로운 인스턴스를 생성하므로 주소 값이 다르다
  - 하지만   equals() 메서드를 사용 했을 땐 `true`값이 나온다
- Integer 클래스에서도 마찬가지로 같은 `100`이지만 각각 새로운 인스턴스이므로 주소 값이 다름
  - 하지만 equals() 메서드 사용 시 `true`값 리턴

<br />

---

#### 3. hashCode() 메서드

- 해시(hash)는 정보를 저장하거나 검색할 때 사용하는 자료 구조
- 정보를 어디에 저장할 것인지 ,어디서 가져올 것인지 해시 함수를 사용하여 구현
  - 해시 함수는 객체의 특정 정보(키 값)를 매개변수 값으로 넣으면 그 객체가 저장되어야 할 위치나 저장된 해시 테이블 주소(위치)를 반환
  - 개발하는 프로그램 특성에 따라 다르게 구현

<br />

##### 3.1 Object 클래스의 hashCode() 메서드

```java
//저장위치 = 해시함수(객체 정보)
index = hash(key)
```

- 자바에서는 인스턴스를 힙 메모리에 생성하여 관리할 때 해시 알고리즘을 사용

```java
// 객체의 해시 코드 값(메모리 위치 값)이 반환 됨
hashCode = hash(key);
```

- 자바에서는 두 인스턴스가 같다면 hashCode()메서드에서 반환하는 해시 코드 값이 같아야 함
  - 논리적으로 같다면 hashCode() 메서드에서 반환하는 해시 코드 값이 같아야 함
    - equals() 메서드를 논리적으로 같은 객체도 같다고 재정의 했으면  hashCode() 메서드 역시 두 객체가 같은 해시 코드 값을 반환하도록재정의 해야 함

<br />

##### 3.2 String과 Integer 클래스의 hashCode() 메서드

- String과 Integer 클래스의 equals() 메서드가 재정의 되어 있듯이 hashCode() 메서드도 재정의 되어 있음

```java
package equals;

public class Test {

	public static void main(String[] args) {

		String str1 = new String("abc");
		String str2 = new String("abc");
		
		System.out.println(str1.hashCode());
		System.out.println(str2.hashCode());
		
		Integer i1 = new Integer(100);
		Integer i2 = new Integer(100);
		
		System.out.println(i1.hashCode());
		System.out.println(i2.hashCode());
	}
}
```

**96354**

**96354**

**100**

**100**

<br />

- 실제로는 다른 주소 값을 가진 인스턴스이지만 논리적으로 같은 값이다.

<br />

#### 4. Clone() 메서드

- 객체 원본을 유지해 놓고 복사본을 사용하거나 기본 틀의 복사본을 사용해 동일한 인스턴스를 만들어 복잡한 생성 과정을 간단히 하려는 경우 사용
- clone() 메서드를 사용하려면 객체를 복제해도 된다는 의미로 클래스에  Cloneable 인터페이스를 구현 해야 함
- 메서드만 재정의하고 Cloneable 인터페이스를 명시하지 않으면 호출 시 CloneNotSupportedException이 발생

<br />

##### Point 클래스

```java
package clone_practice;

public class Point {
	
		int x;
		int y;
		
		Point(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public String toString() {
			return "x=" + x + "," + "y= " + y;
		}
}
```

##### Circle 클래스

```java
package clone_practice;

public class Circle implements Cloneable{
	Point point;
	int radius;
	
	Circle(int x, int y, int radius){
		this.radius = radius;
		point = new Point(x,y);
	}
	
	public String toString() {
		return "원점은" + point + "이고," + "반지름은 " + radius + "입니다.";
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException{
		return super.clone();
	}
}
```

<br />

##### 테스트 클래스

```java
package clone_practice;

public class Test {

	public static void main(String[] args) throws CloneNotSupportedException {
		
		Circle circle = new Circle(10, 20, 30);
		Circle copyCircle = (Circle)circle.clone();
		
		System.out.println(circle);
		System.out.println(copyCircle);
		System.out.println(System.identityHashCode(circle));
		System.out.println(System.identityHashCode(copyCircle));
	}
}
```

**원점은x=10,y= 20이고,반지름은 30입니다.**

**원점은x=10,y= 20이고,반지름은 30입니다.**

**1284693**

**31168322**

<br />

- 실행 결과 clone()을 통해 멤버 변수 값은 같고 주소 값은 다른 copy가 생성 됨

<br />

# String Class

- 문자열을 사용 할 수 있도록 제공 되는 클래스
- 문자열을 생성자의 매개변수로 하여 생성하는 방식과 이미 생성된 문자열 상수를 가리키는 방식이 있음

```java
String str1 = new String("abc");  // 생성자의 매개변수로 문자열 생성
String str2 = "test"	// 문자열 상수를 가리키는 방식
```

- new 예약어를 사용하여 객체를 생성 할 경우 "abc" 문자열 인스턴스를 위한 힙 메모리가 생성 됨
- new 예약어 없이 바로 대입 할 경우 "test" 문자열을 `문자열 상수 풀`에 메모리가 할당 됨

<br />

#### String 클래스의 final char[] 변수

- String 클래스의 구현 내용

```java
public final class String
implements java.io.Serializable, Comparable<String>, CharSequence {

private final char value[];
...
```

- 다른 프로그래밍 언어는 문자열을 구현할 때 일반적으로 char[] 배열을 사용하지만 자바에서는 String 클래스를 통해 제공함

- final로 선언 된 value 변수에 문자열이 저장 되기 때문에 new 예약어로 생성 이후 변경 불가
  - 문자열은 불변(Immutable)
  - 때문에 자주 참조되고 많은 연산이 필요하지 않은 경우 이용

<br />

- concat() 메서드로 문자열 변경해 보기

```java

public class String_test {

	public static void main(String[] args) {
		String javaStr = new String("java");
		String androidStr = new String("android");
		
		System.out.println(javaStr);
		System.out.println("처음 문자열 주소 값 :" + System.identityHashCode(javaStr));
		
		javaStr = javaStr.concat(androidStr);
		
		System.out.println(javaStr);
		System.out.println("연결 된 문자열 주소 값 :" + System.identityHashCode(javaStr));
	}
}
```

**java**

**처음 문자열 주소 값 :1284693**

**javaandroid**

**연결 된 문자열 주소 값 :31168322**

<br />

- javaStr에 연결 된 문자열을 입려했지만 `값 변경`이 아닌 `주소 대체`가 되었다.

<br />

---

#### StringBuffer와 StringBuilder 클래스 활용하기

- String 클래스는 한번 생성 되면 내부의 문자열이 변경 되지 않음
  - 때문에 String 클래스를 이용한 문자열 변경과 연결하는 프로그램은 메모리 낭비가 큼
- StringBuffer와 StringBuilder는 내부에 변경 가능한(final이 아닌) char[]를 변수로 가지고 있음
  - 따라서 두 클래스를 사용하여 문자열을 연결하면 기존에 사용 하던 char[]  배열이 확장되므로 추가 메모리를 사용하지 않음
  - 내부의 임시 데이터 저장 메모리 Buffer를 이용하여 작업
- StringBuffer 클래스는 멀티 스레드 작업 시 동기화로 문자열의 안전한 변경을 보장
- StringBulider 클래스는 멀티 스레드 작업 시 비동기화로 문자열의 안전한 변경을 보장하지 않음
  - 싱글 스레드 작업 시, 또는 멀티 스레드여도 동기화가 필요 없다면 빠른 실행 속도의 장점을 가짐

<br />

##### StringBuffer 사용 예제

```java
public class StringBuffer_practice {

	public static void main(String[] args) {
		String s = "스트링";
		StringBuilder sb = new StringBuilder(s);
		System.out.println(System.identityHashCode(sb));
		
		sb.append(" 변경");
		sb.append(" 되는");
		sb.append(" 문자열에는");
		sb.append(" 버퍼를");
		sb.append(" 권장");

		System.out.println(sb);
		System.out.println(System.identityHashCode(sb));
	}
}  // StringBuilder와 사용법 동일
```

**1284693**

**스트링 변경 되는 문자열에는 버퍼를 권장**

**1284693**

<br />

# Wrapper Class

- 기본 자료형을 객체처럼 사용하기 위한 클래스
  - 매개변수가 객체거나 반환 값이 객체인 경우

```java
public void setValue(Interger i) {...} //객체를 매개변수로 받는 경우
public Integer returnValue() {...} // 반환 값이 객체형인 경우
```

<br />

| 기본형  | Wrapper클래스 |
| ------- | ------------- |
| boolean | Boolean       |
| byte    | Byte          |
| char    | Character     |
| short   | Short         |
| int     | Integer       |
| long    | Long          |
| float   | Float         |
| double  | Double        |

<br />

---

### Integer 클래스 사용법

- 다른 wrapper 클래스들도 사용법이 유사하므로 책에서는 대표로  Integer클래스만 설명한다.

- Integer 클래스는 특정 정수와 문자열을 매개변수로 받는다

```java
Integer(int value){...} // 특정 정수를 매개변수로 받는 경우
Integer{String s}{...} // 특정 문자열을 매개변수로 받는 경우
```

- Integer.java

```java
public final class Integer extends Number implements Comparable<Integer> {

    @Native public static final int   MIN_VALUE = 0x80000000;

    @Native public static final int   MAX_VALUE = 0x7fffffff;
    
...
```

- Integer 클래스는 int 자료형의 특성이 그대로 구현되어 있다
  - 사용 가능한 최댓값과 최솟괎이 static 변수로 정의 되어 있다.
- 멤버 변수로 기본 자료형 int를 가지고 있으면서 int 값을 객체로 활용할 수 있는 여러 메서드를 제공

```java
...

private final int value;

    public Integer(int value) {
        this.value = value;
    }

...
```

- int value는 final 변수이며 한번 생성되면 변경 할 수 없다.

<br />

#### Integer 클래스의 메서드

- 자주 사용하는 메서드 소개

##### intValue()

- Integer 클래스 내부의 int 자료형 값을 가져오기 위해 사용

```java
Integer iValue = new Integer(100);
int myValue = iValue.intValue(); // int 값 가져오기, myValue 값을 출력하면 100이 출력 됨
```

<br />

##### valueOf()

- valueOf() 정적 메서드를 사용하면 생성자를 사용하지 않고 정수나 문자열을 바로 Integer 클래스로 반환 받을 수 있음

```java
    public static Integer valueOf(int i) {
        if (i >= IntegerCache.low && i <= IntegerCache.high)
            return IntegerCache.cache[i + (-IntegerCache.low)];
        return new Integer(i);
```

<br />

```java
Integer number2 = Integer.valueOf(100);
Integer number1 = Integer.valueOf("100"); 
```

- 문자열을 입력할 경우 문자열을 정수로 반환해주는 parseInt()메서드가 내부에서 실행 된다.
  - Integer.valueOf(String s)의 리턴 값은 Integer.valueOf(parseInt(s, 10))

<br />

##### parseInt()

- parseInt(해석할 문자열, 진수)
- 문자열을 입력할 경우 문자열을 int 값으로 반환

```java
int num = Integer.parseInt("100");
```

<br />

---

### 오토박싱과 언박싱

- int는 기본 자료형 4바이트지만 Integer는 정수 값을 인수로 넣어 생성자 호출로 인스턴스를 생성해야하는 클래스이다.
- 때문에 기본 자료형과 Wrapper 클래스형을 함께 연산하기 위해서는 자료형 일치가 필요했음
  - 자바 5부터 자동으로 일치 지원

```java
Integer num1 = new Integer(100);
int num2 = 200;
int sum = num1 + num2; // 이때 num1은 num.intValue()메서드로 인해 정수형으로 변환 됨(언 박싱)
Integer num3 = num2; // 이때 num2는 Integer.valueOf(num2)메서드로 인해 Wrapper 클래스형으로 변환 됨(오토 박싱)
```

<br />

# Class Class

- 자바의 모든 클래스와 인터페이스는 컴파일 후 class 파일로 생성 된다.
  - 이 class 파일에는 클래스나 인터페이스에 대한 변수, 메서드, 생성자 등의; 정보가 들어 있다.
  - Class 클래스는 컴파일 된 class 파일에 저장 된 클래스나 인터페이스 정보를 가져오는데 사용 한다.
- 여러 클래스 중 상황에 따라 다른 클래스를 사용해야 하거나 반환 받는 클래스가 정확히 어떤 자료형인지 모를 때도 있다.
  - 이렇게 모르는 클래스의 정보를 사용할 경우에 해당 정보를 찾기 위해 활용한다.

<br />

#### 1. Object 클래스의 getClass()메서드 사용하기

```java
String s = new Stirng();
Class c = s.getClass(); // getClass() 메서드의 반환형은 Class이다.
System.out.println(c);
```

**class java.lang.String**

- Object에 선언한 getClass()메서드는 모든 클래스가 사용 할 수 있는 메서드
  - 이미 생성된 인스턴스가 있어야 사용 가능

<br />

#### 2. 클래스 파일 이름을 Class 변수에 직접 대입하기

```java
Class c = String.class;
System.out.println(c);
```

**class java.lang.String**

- 컴파일 된 클래스 파일이 있다면 클래스 이름만으로 Class 클래스를 반환 받을 수 있음

<br />

#### 3. Class.forName("클래스 이름)메서드 사용하기

```java
Class c = Class.forName("java.lang.String");
System.out.println(c);
```

- 컴파일 된 클래스 파일이 있다면 클래스 이름만으로 Class 클래스를 반환 받을 수 있음

- 클래스 이름(패키지 이름 포함)으로 가져오는 경우에 매개변수가 문자열임
  - ClassNotFoundException 예외처리 필요

<br />

---

###  Class 클래스를 활용하여 클래스 정보 알아보기

- Class 클래스를 가져올 수 있다면 원하는 클래스의 정보 (생성자, 메서드, 멤버 변수)를 찾을 수 있다.
- 정보를 가져오고 활영하여 인스턴스를 생성하거나 메서드를 호출하는 방식을 `리플렉션`이라고 함
  - 리플렉션 프로그래밍을 염두에 두고 프로그래밍 하는 경우가 많지는 않음
  - 리플렉션 프로그래밍은 컴파일 시점에 알 수 없는 클래스, 즉 프로그램 실행 중에 클래스를 메모리에 로딩하거나 (동적 로딩) 객체가 다른 곳에 위채해서 원격으로 로딩하고 생성 할 때 주로 사용한다.

#### getConstructors() 메서드

- 클래스참조변수.getConstructors();
- 참조한 클래스형 변수에서 해당 클래스의 모든 생성자 가져오기

```java
    @CallerSensitive
    public Constructor<?>[] getConstructors() throws SecurityException {
        checkMemberAccess(Member.PUBLIC, Reflection.getCallerClass(), true);
        return copyConstructors(privateGetDeclaredConstructors(true));
    }
```

#### getFields() 메서드

- 클래스참조변수.getFields();
- 참조한 클래스형 변수에서 해당 클래스의 모든 멤버변수(필드) 가져오기

```java
    @CallerSensitive
    public Field[] getFields() throws SecurityException {
        checkMemberAccess(Member.PUBLIC, Reflection.getCallerClass(), true);
        return copyFields(privateGetPublicFields(null));
    }
```



#### getMethods() 메서드

- 클래스참조변수.getMethods();
- 참조한 클래스형 변수에서 해당 클래스의 모든 메서드가져오기

```java
    @CallerSensitive
    public Method[] getMethods() throws SecurityException {
        checkMemberAccess(Member.PUBLIC, Reflection.getCallerClass(), true);
        return copyMethods(privateGetPublicMethods());
    }
```



<br />

##### 실행 예제

```java
package class_practice;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class StringClassTest {

	public static void main(String[] args) throws ClassNotFoundException,InstantiationException,
	IllegalAccessException{
		Class strClass = Class.forName("java.lang.String"); //목표 클래스 이름으로 해당 클래스 가져오기
		
		
		System.out.println("===================생성자===================");
		
		Constructor[] cons = strClass.getConstructors(); // 생성자 목록을 배열 형태로 리턴
		for(Constructor c : cons) {
			System.out.println(c);
			}	
		System.out.println("===================멤버변수===================");
			
		Field[] fields = strClass.getFields(); // 멤버 변수 (필드) 목록을 배열 형태로 리턴
		for(Field f : fields) {
			System.out.println(f);
		}
		System.out.println("===================메서드===================");
		
		Method[] methods = strClass.getMethods(); // 메서드 목록을 배열 형태로 리턴
		for(Method m : methods) {
			System.out.println(m);
		}
	}
}
```

**===================생성자===================**

**public java.lang.String(byte[],int,int)**

**public java.lang.String(byte[],java.nio.charset.Charset)**

**`---중략---`**

**public java.lang.String(byte[],int)**

**public java.lang.String(byte[],int,int,int)**



**===================멤버변수===================**

**public static final java.util.Comparator java.lang.String.CASE_INSENSITIVE_ORDER**

**===================메서드===================**

**public boolean java.lang.String.equals(java.lang.Object)**

**public java.lang.String java.lang.String.toString()**

**`---중략---`**

**public default java.util.stream.IntStream java.lang.CharSequence.chars()**

**public default java.util.stream.IntStream java.lang.CharSequence.codePoints()**

<br />

- 이렇듯 Class 클래스를 이용하면 어떤 클래스의 이름만 알아도 클래스의 정보를 알 수 있다.

<br />

#### newInstance() 메서드를 사용해 클래스 생성하기

- 항상 Object를 반환하므로 생성된 객체형으로 형변환이 필요
- 해당 클래스의 디폴트 생성자를 호출하여 인스턴스 생성

```java
package class_practice;

public class NewInstanceTest {

	public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Class sClass = Class.forName("class_practice.StringClassTest");
		StringClassTest sct = (StringClassTest)sClass.newInstance(); // 다운캐스팅
		System.out.println(sct);
	}
}
```

**class_practice.StringClassTest@139a55**

<br />

#### Class.forName()을 사용해 동적 로딩하기

- 대부분의 클래스 정보는 프로그램이 로딩 될 때 이미 메모리에 있다.
- 데이터베이스 연동은 시스템을 컴파일 할때 모든 DB 라이브러리(드라이버)를 같이 컴파일 할 필요는 없다.
  - 때문에 시스템을 구동할 때 어떤 DB로 연결할지만 결정되면 해당 라이브러리만 로딩하면 된다.
  - DB 정보는 환경 파일에서 읽거나 다른 변수 값으로 받을 수 있다.
  - 이처럼 프로그램 실행 이후 클래스의 로딩이 필요한 경우 `동적 로딩`(dynamic loading)방식을 사용한다.
  - 자바는 Class.forName() 메서드를 동적 로딩으로 제공

<br />

```java
    @CallerSensitive
    public static Class<?> forName(String className)
                throws ClassNotFoundException {
        Class<?> caller = Reflection.getCallerClass();
        return forName0(className, true, ClassLoader.getClassLoader(caller), caller);
    }
```

<br />

- 매개변수로 문자열을 입력 받는다.
  - 이때 입력 받는 문자열을 변수로 선언하여 변수 값만 바꾸면 다른 클래스를 로딩 할 수 있다.

```java
DataBases db = "데이터 베이스"
Class sClass = Class.forName(db);
```

<br />

##### forName() 메서드 사용 시 주의 사항

- 문자열로 입력 받기 때문에 오타, 또는 해당 이름의 클래스가 없을 경우 ClassNotFoundException이 발생한다.

  - 하지만 동적 로딩은 프로그램이 실행 된 이후 호출 될때 발생하므로 컴파일 시점에서는 오류를 알 수 없다.

  - 여러 클래스 중 하나를 선택하게하거나 시스템 연동 중 매개변수로 넘어온 값에 해당하는 클래스를 로딩하는 등 유연하게 사용 할 수 있다.

  - 동적로딩을 통해 Class를 가져온 뒤 리플렉션 프로그래밍으로 객체를 생성하고 활용 할 수 있다.

     
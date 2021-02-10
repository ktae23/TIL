### *** 생성자\**(constructor)\****

- **생성자 만들기**

```java
package constructor;

public class Person {		//Person이란 이름의 class 생성, 
							//Person이란 클래스를 생성 할 수 있는 생성자
	String name;			//멤버 변수
	float height;			//멤버 변수
	float weight;			//멤버 변수
}
```

------

- **생성자 테스트하기**

```java
package constructor;

public class PersonTset {
	public static void main(String[] args) {	//main()함수
		Person personLee = new Person();	//Person 클래스자료형의 personLee 참조 변수 생성
		//Person() 클래스 생성자 이용하여 새로운 Person 클래스를 만들어 참조 변수에 입력
	}
}
```

- 클래스의 멤버 변수는 메서드에 의해 값이 변경 될 수 있음
- 하지만 처음 클래스를 생성할 때 정해야하는 경우도 있음.
- 생성자가 하는 일은 클래스를 처음 만들 때 멤버 변수나 상수를 초기화 하는 것

------

- 디폴트 생성자

```java
package constructor;

public class Person {
	String name;
	float height;
	float weight;
    
    public Person() {}	// 자바 컴파일러가 자동으로 제공하는 디폴트 생성자
}
```

- 생성자 이름은 클래스 이름과 같아야 하고 동시에 java 파일명과 같아야 함.
- 생성자는 반환 값이 따로 없음
- 따로 매개변수가 있는 생성자를 만들지 않으면 자동으로 디폴트 생성자가 제공 됨
- 여러 조건의 생성자을 만들 경우 디폴트 생성자를 직접 구현해 줘야 함.

------

- 매개변수를 받는 생성자

```java
package constructor;

public class Person {

	String name;
	float height;
	float weight;
	
	public Person() {}	//생성자를 생성했을 경우 디폴트 생성자는 직접 입력해주어야 함

	public Person(String pname) {	//pname 값을 매개변수로 하는 생성자 Person()
		name = pname;
	}
}
```

- 위 경우처럼 생성자가 두 개 이상 제공되는 경우를 '생성자 오버로드(constructor overload)'라고 함.\
- 필요에 따라 매개변수가 다른 여러 개의 생성자를 만들 수 있음
- 이때 디폴트 생성자를 일부러 생성하지 않아 매개변수를 반드시 받도록 할 수 있음

```java
package student;

public class Student {			//Student 클래스 생성

	int studentID;			//studentID 값이 있어야 함.
    
    public Student(int studentID) {		
			//정수형자료 studentID 값을 매개변수로 하는 Student 클래스를 만드는 생성자
    	this.studentID = studentID;

	}
}
```
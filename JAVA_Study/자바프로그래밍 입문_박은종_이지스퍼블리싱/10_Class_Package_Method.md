### *** 객체와 객체 지향 프로그래밍**

![img](https://blog.kakaocdn.net/dn/bVs62h/btqWjqn5uJb/NseYKiCfK6ZFflmcskRHw1/img.jpg)(출처 - Do it! 자바 프로그래밍 입문, 박은종)

- 예시의 사건을 순서대로 프로그래밍하는 것을 '절차 지향 프로그래밍'이라고 한다.



![img](https://blog.kakaocdn.net/dn/wJu59/btqV8E8lCbq/Y9PvvMwBneYbUrkHm7EiRk/img.jpg)(출처 - Do it! 자바 프로그래밍 입문, 박은종)

- 예시의 사건을 '객체' 단위로 나누어 객체 사이에 일어나는 일들을 구현하는 것이 '객체 지향 프로그래밍'이다.
- 객체1 = 학생, 객체2 = 밥, 객체3 = 버스, 객체4 = 학교
  - 객체1이 일어난다, 객체1이 객체2를 먹는다, 객체1이 객체3을 타고 요금을 지불한다, 객체1이 객체4에 도착한다.





------

### *** 클래스**



![img](https://blog.kakaocdn.net/dn/cRxO25/btqV8GrygSj/zb11oXvFU2apoaTcTEwOqk/img.jpg)(출처 - Do it! 자바 프로그래밍 입문, 박은종)

- 객체 지향 프로그램은 클래스를 기반으로 프로그래밍 한다.
- 클래스는 객체의 속성과 기능을 코드로 구현한 것으로 '클래스를 정의한다'라고 표현
- 클래스 이름과 속성/특성이 필요. -> 속성을 '멤버 변수'라고 함.
- 모든 요소가 클래스 내부에 있어야함. 클래스 외부에는 package선언과 import 문장만 존재
- 클래스 이름은 대문자로 시작하는 것이 관습

```java
package student;

public class Student {  //접근제어자 클래스생성예약어 클래스이름

	int studentID;		//멤버변수(속성(property) 또는 특성(attribute)라고 표현하기도 함
	String studentName;	//멤버변수
	int grade;			//멤버변수
	String address;		//멤버변수

	}    
    
   }
```



- 클래스에서의 자료형

| 변수의 자료형                        | 기본 자료형 |
| ------------------------------------ | ----------- |
| int, long, float, double 등          |             |
| 참조 자료형 (클래스형, 객체 자료형)  |             |
| String, Date, Student(클래스이름) 등 |             |





- 메서드(method) : 클래스 내부에서 멤버 변수를 사용하여 클래스 기능을 구현하는 것 - 멤버 함수(member function)

```java
package student;

public class Student {

	int studentID;
	String studentName;
	int grade;
	String address;
	
	public void showStudentInfo() {		//메서드
		System.out.println(studentName + "," + address); // 이름, 주소 출력
	}
```



------

### *** 패키지**



- 클래스 파일의 묶음
- 패키지를 만들면 프로젝트 하위에 물리적으로 디렉터리가 생성되어 계층 구조를 구성하게 됨.
- 패키지의 계층 구조를 만드는 작업은 소스 코드를 어떻게 관리할지 구성하는 작업.
- 클래스의 패키지 선언은 코드의 맨 위에 작성.
- 클래스의 전체 이름은 패키지이름.클래스이름
  - 같은 이름의 클래스여도 다름 패키지에 있으면 전혀 무관한 다른 클래스가 됨.



------

### *** 메서드**

- **함수의 한 종류**
- **함수가 하는 일을 코드로 구현하는 것을 '함수를 정의한다'고 말함.**
- **매개변수를 입력 받아 실행코드를 수행한 뒤 return 예약어를 받는 변수를 반환 함.**

```java
int add (int num1, int num2){		//함수 반환형 함수이름 (매개변수)
	int result;						//실행코드
    result = num1 + num2;			//실행코드
    return result;					//return예약어
    }
```



- **반환 값이 없을 경우 함수반환형을 void로 작성.**

```java
void printGreeting( String name) {	//함수 반환형 함수이름 (매개변수)
	System.out.println(name + "님 안녕하세요");	//실행코드
    return;	//반환 값 없음(함수 수행 종료)
    }
void divide(int num1, int num2) {
	if(num2 == 0) {
    	System.out.println("나누는 수는 0이 될 수 없습니다.");
        return;	//함수 수행 종료
    }
    else {
    int result = num1 / num2;
    System.out.println(num1 + "/" + num2 + "=" result + "입니다.");
    }
 }
```
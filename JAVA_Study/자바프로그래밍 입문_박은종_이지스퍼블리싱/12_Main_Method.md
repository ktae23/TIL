### *** 클래스 사용과 main() 함수**

```java
public class Student {

	int studentID;
	String studentName;
	int grade;
	String address;
    
    //멤버 변수
    
    
	public String getStudentName() {		
		return studentName; //학생 이름 반환하는 메서드
	}
	
	public void setStudentName(String name) {
		studentName = name; //입력된 이름을 학생 이름 변수에 대입하는 메서드
	}
	// 메서드
```



- main() 함수 : JVM(Java Virtual Machine)이 프로그램을 시작하기 위해 호출하는 함수
- 클래스 내부에 작성하지만 해당 클래스의 메서드가 아님
- 클래스 내부에 작성하거나 외부에 테스트용 클래스를 만들어서 사용 함.\



------



**Student 클래스에 main() 함수 표함하기**



```java
package student;

public class Student {

	int studentID;
	String studentName;
	int grade;
	String address;
	
	public void showStudentInfo() {
		System.out.println(studentName + "," + address);
	}

	public String getStudentName() {
		return studentName;
	}
    // main()함수가 시작 클래스가 되어 프로그램을 실행하면 가장 먼저 수행 됨
	public static void main(String[] args) { 
		Student studentAhn = new Student();  
		//Student 클래스자료형의 studentAhn이라는 새로운 Student 클래스 생성
		studentAhn.studentName = "안연수";
		//studentAhn 클래스의 studentName 값을 "안연수"로 입력.
		
		System.out.println(studentAhn.studentName);
		System.out.println(studentAhn.getStudentName());
	}
/*	public void setStudentName(String name) {
		studentName = name;
	}
		//setStudentName 메서드가 있다면
		//studentAhn.setStudentName("안연수");로 입력해도
		//studentAhn.studentName = "안연수";와 같은 결과.
*/	
}
```

**안연수**

**안연수**



- 만일 테스트용 외부 클래스를 따로 만들어 main()함수를 실행할 경우 같은 패키지 내에 있어야함
- 다른 패키지에 있다면 import 문을 사용해 사용하고자 하는 클래스를 불러와야 함.



------

### *** 클래스 사용과 main() 함수**





- '의사나 행위가 미치는 대상'이 '객체'
- '객체'를 코드로 구현한 것이 '클래스'
- 클래스를 생성한다는 것은 실제 사용할 수 있도록 메모리 공간을 할당 받는다는 의미
- 이때 할당 되는 메모리가 '힙 메모리' (heap memory)
- 실제로 사용 할 수 있도록 메모리에 생성된 클래스가 '인스턴스' (instance)
- 생성된 클래스의 인스턴스를 '객체'라고도 함.

![img](https://blog.kakaocdn.net/dn/D1FWF/btqV37pqHAE/D4lMajm2hcKQhsxAc23jGk/img.png)(출처 - Do it! 자바 프로그래밍 입문, 박은종)

```java
	Student studentAhn = new Student();
 // 클래스명 변수 이름 = new 생성자();
// Student 클래스 자료형으로 studentAhn 변수를 선언하고,
// new 예약어와 Student(); 생성자로 새로운 Student 클래스를 생성하여 studentAhn 변수에 대입.
// studentAhn을 참조변수, 새로 생성 된 인스턴스가 이 변수에 대입된다.
```
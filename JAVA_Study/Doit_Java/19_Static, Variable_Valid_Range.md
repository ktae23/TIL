### *** static 변수(클래스 변수), 클래스 메서드**

- 매 인스턴스마다 따로 생성되는 변수가 아닌 클래스 전반에서 공통적으로 사용 할 수 있는 기준 변수
- static예약어, 자료형, 변수이름 순으로 선언
- 프로그램이 실행되어 메모리에 올라갔을때 딱 한 번 메모리에 할당 되어 모든 인스턴스에서 공유 가능
- 가장 먼저 생성되기 때문에 인스턴스 생성과 무관하게 공유 가능
- 클래스 변수라고도 부름

```java
package staticEX;

public class Student {
	
	private static int serialNum = 10000;	
    //static 변수로 설정하여 모든 인스턴스에서 참조 가능한 고정 변수 설정.
    //private 설정을 통해 값을 변경할 수 없도록 제한.
    
	int studentID;	
	String studentName;
	int studentNumber;
	//Student 클래스가 생성되면 갖게 되는 속성값들
    
	public Student() {
		studentID = serialNum;	//Student 생성자가 실행되면 serialNum 값을 studentID에 저장
		studentNumber = studentID+100;	//studentID에 100 더한값을 studentNumber에 저장
		serialNum++;			//serialNum을 1 증가 - Student 클래스 생성마다 다른값 부여
	}

	public static int getSerialNum() {
		return serialNum;
    //setSerialNum()메서드는 삭제하고 get()메서드만 남겨서 정보 가져갈 수 있도록 함.
	}
}
```

- static 변수 serialNum에 대한 get(),set() 메서드를 클래스 메서드라고 한다.
- 클래스 메서드 내부에 지역변수를 선언하여 사용하는것은 문제가 되지 않는다.
  - 하지만 클래스의 멤버 변수를 사용 할 경우 오류가 발생함.
    - 클래스의 멤버 변수는 인스턴스가 생성 될 때 만들어지기 때문에 클래스 생성단계에서 사용 불가
- 클래스 메서드와 클래스 변수는 인스턴스가 생성되지 않아도 사용 할 수 있음.
- 사용 할때는 클래스명.클래스메서드()의 형식으로 클래스명에 직접 사용. 

```java
package staticEX;

public class StudentTest1 {

	public static void main(String[] args) {
		Student studentJ = new Student();
		System.out.println(studentJ.studentID);
		System.out.println(studentJ.studentNumber);

		
		Student studentT = new Student();
		System.out.println(studentT.studentID);
		System.out.println(studentT.studentNumber);
		
		/*
        System.out.println(Student.getSerialNum());
		System.out.println(Student.getSerialNum());
		*/
	}

}
```

**10000** // studentJ의 studentID값으로 최초 serialNum 값이 부여 됨**
10100** // studentJ의 studentNumber값으로 studentID값에 100을 더한 값이 부여 됨

**10001** // studentT의 studentID값으로 최초 serialNum 값에서 1이 늘어난 값이 부여 됨**
10101** // studentT의 studentNumber값으로 studentID값에 100을 더한 값이 부여 됨**
**

| 데이터 영역     | 스택 메모리       | 힙 메모리         |
| --------------- | ----------------- | ----------------- |
| serialNum 10001 | <- studentJ ->    | studentJ 인스턴스 |
| <- studentT ->  | studentT 인스턴스 |                   |



------



### *** 변수 유효 범위**

| 변수 유형                 | 선언 위치                                   | 사용 범위                                                    | 메모리      | 생성과 소멸                                                  |
| ------------------------- | ------------------------------------------- | ------------------------------------------------------------ | ----------- | ------------------------------------------------------------ |
| static 변수 (클래스 변수) | static 예약어를 사용하여 클래스 내부에 선언 | 클래스 내부에서 사용 private이 아니면 믈래스 이름으로 다른 클래스에서 사용 가능 | 데이터 영역 | 프로그램이 처음 시작할 때 상수와 함께 데이터 영역에 생성되고 프로그램이 끝나고 메모리를 해제할 때 소멸 됨 |
| 지역 변수 (로컬 변수)     | 함수 내부에 선언                            | 함수 내부에서만 사용                                         | 스택        | 함수가 호출될 때 생성되고 함수가 끝나면 소멸 함              |
| 멤버 변수 (인스턴스 변수) | 클래스 멤버 변수로 선언                     | 클래스 내부에서 사용 private이 아니면 참조 변수로 다른 클래스에서 사용 가능 | 힙          | 인스턴스가 생성될 때 힘에 생성되고 가비지 컬렉터가 메모리를 수거할 때 소멸 됨 |

static 변수(클래스 변수)의 유효 범위



- 프로그램을 실행하면 메모리에 프로그램이 상주하게 됨
- 이때 프로그램 영역 중 상수나 문자열, static 변수가 생성되는 데이터 영역이 있다
- static 변수는 클래스 생성과 상관 없이 처음부터 데이터 영역 메모리에 생성 됨
- 접근제어자가 private이 아니라면 클래스 외부에서도 객체 생성과 무관하게 사용 가능
- 프로그램 시작부터 종료까지 상주하기 때문에 너무 크기가 큰 변수는 적절치 않음



지역 변수(로컬 변수)의 유효 범위



- 함수 내부에 선언하기 때문에 해당 함수 밖에서는 사용 불가
- 스택 메모리에 지역 변수가 생성 됨
- 함수가 호출 될 때 생성 되었다가 함수가 반환되면 할당된 메모리 공간이 해제되면서 소멸



멤버 변수(인스턴스 변수)의 유효 범위



- 클래스가 생성 될 때 힙 메모리에 생성되는 변수
- 클래스의 어느 메서드에서나 사용 할 수 있음
- 클래스 내부의 여러 메서드에서 사용 할 변수는 멤버 변수로 선언하는 것이 좋음
- 가비지 컬렉터에 의해 수거되면서 메모리에서 사라짐
### *** 참조 자료형**

- **크기가 정해진 기본 자료형(int, char, float, double 등)으로 선언하는 변수가 있는 반면**
- **클래스형으로 선언하는 참조 자료형 변수가 있음.**

- 학생 클래스
  - '과목' 클래스 자료형(참조 자료형) korean 변수
  - '과목' 클래스 자료형(참조 자료형) math 변수
    - 클래스 생성과 동시에 korean, math 변수에 과목 이름 할당. 

```java
package reference;

public class Student {
	int studentID;		
	String studentName;
	
	Subject korean;	
	Subject math;	
	
	public Student() {
		korean = new Subject("국어");
		math = new Subject("수학");
	}
	public Student(int studentID, String studentName) {
		this.studentID = studentID;
		this.studentName = studentName;
		
		korean = new Subject("국어");
		math = new Subject("수학");
	}
	
	public void setKorean(int score) {
		korean.setScorePoint(score);
	}
	public void setmath(int score) {
		math.setScorePoint(score);
	}
	
	public void showStudentInfo() {
		int total = korean.getScorePoint() + math.getScorePoint();
		System.out.println(studentName + "학생의 총점은" + total + "점 입니다.");
		String totalSubject = korean.getSubjectName() + "와(과)"+ math.getSubjectName();
		System.out.println(studentName + "학생이 수강한 과목은 " + totalSubject + " 입니다.");
	}

}
```



- 과목 클래스
  - **과목입력을 매개변수로 입력받는 Subject 생성자**
  - **과목 이름, 과목 점수를 입력 받고 반환하는 get(), set() 메서드**

```java
package reference;

public class Subject {
	
	String subjectName;
	int scorePoint;
	
	public Subject (String name) {
		subjectName = name;
	}
	
	public void setSubjectName(String subjectName) {
		this.subjectName = subjectName;
	}

	public int getScorePoint() {
		return scorePoint;
	}

	public void setScorePoint(int scorePoint) {
		this.scorePoint = scorePoint;
	}

	public String getSubjectName() {
		return subjectName;
	}
}
```



- 학생 테스트 클래스
  - **James는 매개변수를 받지 않는 생성자 이용**
  - **Tomas는 매개변수를 받는 생성자 이용**

```java
package reference;

public class StudentTest {

	public static void main(String[] args) {
		
		Student studentJames = new Student();
		studentJames.studentID = 100;
		studentJames.studentName = "James";
		studentJames.setKorean(100);
		studentJames.setmath(100);
		
		Student studentTomas = new Student(101, "Tomas");
		studentTomas.setKorean(80);
		studentTomas.setmath(60);
		
		studentJames.showStudentInfo();
		studentTomas.showStudentInfo();
	}

}
```

**James학생의 총점은200점 입니다.**
**James학생이 수강한 과목은 국어와(과)수학 입니다.**
**Tomas학생의 총점은140점 입니다.**
**Tomas학생이 수강한 과목은 국어와(과)수학 입니다.**
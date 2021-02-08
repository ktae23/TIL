### *** 정보 은닉**

| 접근 제어자   | 설명                                                         |
| ------------- | ------------------------------------------------------------ |
| public        | 외부 클래스 어디에서나 접근 가능                             |
| protected     | 같은 패키지 내부와 상속 관계의 클래스에서만 접근 가능 그외 클래스에서 접근 불가 |
| 아무것도 없음 | default 값으로 같은 패키지 내부에서만 접근 가능              |
| private       | 같은 클래스 내부에서만 접근 가능                             |

- private 설정을 할 경우 다른 클래스에서 가져올 때는 get(), set() 메서드를 이용해야 함
- private 설정을하여 해당 정보에 접근하여 가져올 때 별도의 메서드를 통해 정보를 가려서 가져올 수 있음
- 이처럼 외부에서의 접근을 막는 것을 '정보은닉'(informantion hiding)이라고 함.

- 생일 클래스
  - day, month, year 정수형 변수를 private 접근 제어자를 사용함.
  - day의 정보를 가져갈 때 오류를 판단해 경우에 따라 다른 결과를 출력 함.

```java
package hiding;

public class Birthday {

	private int day;
	private int month;
	private int year;
	
	public int getMonth() {
		return month;
	}

	public void setMonth(int month) {
		this.month = month;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public int getDay() {
		return day;
	}

	public void setDay(int day) {
		if(month == 2) {
			if(day < 1 || day > 28) {
			System.out.println("입력 오류입니다.");
			} 
			else {
				this.day = day;
				System.out.println("생일은 " + year + "년 " + month + "월 " + day + "일 입니다.");
				System.out.println(year);
				System.out.println(month);
				System.out.println(day);
			}
		}
	}
}
```



- 테스트 클래스
  - year, month, day의 값을 참조변수.메서드(매개변수)의 형태로 입력 받아 Birthday생성자를 만드는 코드

```java
package hiding;

public class BirthdayTest {
	public static void main(String[] args) {
		Birthday date = new Birthday();
		date.setYear(2021);
		date.setMonth(2);
		date.setDay(27);

	}
}
```
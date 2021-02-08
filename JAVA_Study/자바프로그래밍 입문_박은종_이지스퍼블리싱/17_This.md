### *** this 예약어**

```java
package thisEx;

class Birthday{
	int day;
	int month;
	int year;
	
	public void setYear(int year) {
		this.year = year;
	}	// 태어난 연도를 지정하는 메서드. bDay.year = year;와 같음
	
	public void printThis() {
		System.out.println(this);
	}	// this 출력 메서드. System.out.println(bDay);와 같음
}

public class ThisExample {	
	
	public static void main(String[] args) {
	
		Birthday bDay = new Birthday();

		bDay.setYear(2000);			// 태어난 연도를 2000으로 지정
		System.out.println(bDay);	//참조 변수 출력
		bDay.printThis();				//this 출력 메서드 호출
	}
}
```

**thisEx.Birthday@5b6f7412
thisEx.Birthday@5b6f7412
**



this의 출력값은 참조 변수의 주소 값, 즉 참조 값이다.

이는 생성된 인스턴스의 경로가 저장된 참조변수 bDay의 값으로, 인스턴스의 힙메모리 주소 값을 보여준다.

| 스택 메모리              | 힙 메모리      |
| ------------------------ | -------------- |
| BirthDay 클래스 생성     |                |
| setYear() 함수의 this -> | year month day |
| main() 함수의 bDay ->    |                |
| main() 함수의 args ->    |                |



------



### *** this를 이용해 생성자에서 다른 생성자 호출, 자신의 주소 반환**

```java
package thisEx;

class Person {
	
	String name;
	float height;

	
	public Person() {
		this("이름 없음", 0); // this를 사용해 Person(String,int) 생성자 호출
		
	}
	
	public Person(String name, float height) { // 호출당하는 생성자
		this.name = name;
		this.height = height;
	}
    
    	Person returnItSelf() {
    		return this;		// this 반환
    }
	
}

public class CallAnotherConst {

	public static void main(String[] args) {
		
		Person p1 = new Person();
		System.out.println(p1.name);
		System.out.println(p1.height);
        
        Person p2 = p1.returnItSelf();	//this 값을 클래스 변수에 대입
        System.out.println(p2);			//p1.returnItSelf()의 반환 값(p1 값) 출력
        System.out.println(p1);			//참조 변수 출력
		
	}

}
```

**이름 없음**
**0.0**

**thisEx.Person@5b6f7412**

**thisEx.Person@5b6f7412**



- 입력되는 매개변수가 없을 경우 디폴트 생성자 속에 매개변수 생성자를 호출하여 기본값 입력
- 생성자 호출 코드 상단에 어떠한 속성이나 코드를 넣을 수 없음, 클래스가 생성되기 전 단계이기 때문에 오류 발생
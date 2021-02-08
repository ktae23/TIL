
![img](https://blog.kakaocdn.net/dn/YePVp/btqVZRH8U3e/IA1Q3YHMdmu38Cfk17VvAk/img.png)(출처 - Do it! 자바 프로그래밍 입문, 박은종)

- 위 상황에 택시를 더해 객체 지향 프로그래밍을 한다면 학생, 버스, 지하철, 택시, 탑승(동사)의 다섯 가지 클래스가 필요
- 버스, 지하철, 택시 클래스를 만들어 탑승 클래스와 협력시킨 후 학생 클래스를 만들어 탐승 클래스와 협력 한다.

**학생 클래스**

```java
package cooperation;

public class Student {
	
	public String studentName;	//학생 이름
	public int money;			//학생이 가지고 있는 돈
	
	public Student(String studentName, int money) {
		this.studentName = studentName;
		this.money = money;
		
	}
	
	public void takeBus(Bus bus) {
		bus.take(1000);
		this.money -= 1000;
	}
	
	public void takeSubway(Subway subway) {
		subway.take(1500);
		this.money -= 1500;
	}
	
	public void takeTaxi(Taxi taxi) {
		taxi.take(10000);
		this.money -= 10000;
	}
	
	public void showInfo() {
		System.out.println(studentName + "님의 남은 돈은" + money + "입니다.");
	}

}
```

-  학생이 지녀야하는 속성으로 "학생 이름", "가진 돈" 선언
- "학생 이름"과 "가진 돈"을 매개 변수로 받아 참조 변수(this가 가리키는 주소)에 해당 값을 저장하는 생성자 Student() 구현
- 버스를 탔을 경우 bus.take() 매개 변수로 입력 된 값을 요금 처리하고 가진돈에서 빼는 메서드 구현
- 지하철을 탔을 경우 subway.take() 매개 변수로 입력 된 값을 요금 처리하고 가진돈에서 빼는 메서드 구현
- 택시를 탔을 경우 taxi.take() 매개 변수로 입력 된 값을 요금 처리하고 가진돈에서 빼는 메서드 구현
- 학생의 이름과 가진 돈을 출력하는 메서드 구현

**버스 클래스**

```java
package cooperation;

public class Bus {
	int busNumber;
	int passengerCount;
	int money;
	
	public Bus(int busNumber) {
		this.busNumber = busNumber;
	}
	
	public void take(int money) {
		this.money += money;
		passengerCount++;
	}
	
	public void showInfo() {
		System.out.println("버스" + busNumber + "번의 승객은 " + passengerCount + "명이고, 수입은 " + money + "입니다.");
	}
	
}
```

- 버스가 지녀야하는 속성으로 "버스 번호", "승객 수", "받은 요금" 선언
- "버스 번호"를 매개 변수로 받아 참조 변수(this가 가리키는 주소)에 해당 값을 저장하는 메서드 구현
- "요금"을 매개 변수로 받아 입력 된 값을 요금 처리하고 "승객 수"를 1 증가시키는 메서드 구현
- 버스 번호, 승객 수와 수입을 출력하는 메서드 구현

**지하철 클래스**

```java
package cooperation;

public class Subway {
	String lineNumber;
	int passengerCount;
	int money;

	public Subway(String lineNumber) {
		this.lineNumber = lineNumber;
	}
	
	public void take(int money) {
		this.money += money;
		passengerCount++;
	}
	
	public void showInfo() {
		System.out.println(lineNumber + "의 승객은 " + passengerCount + "명이고, 수입은 " + money + "입니다.");
	}
}
```

- 지하철이 지녀야하는 속성으로 "호선 정보", "승객 수", "받은 요금" 선언
- "호선 정보"를 매개 변수로 받아 참조 변수(this가 가리키는 주소)에 해당 값을 저장하는 메서드 구현
- "요금"을 매개 변수로 받아 입력 된 값을 요금 처리하고 "승객 수"를 1 증가시키는 메서드 구현
- 호선 정보, 승객 수와 수입을 출력하는 메서드 구현



**택시 클래스**

```java
package cooperation;

public class Taxi {
	String taxi;
	int passengerCount;
	int money;
	
	public Taxi(String taxi) {
		this.taxi = taxi;
	}
	
	public void take(int money) {
		this.money += money;
		passengerCount++;
	}
	
	public void showInfo() {
		System.out.println(taxi + "의 승객은 " + passengerCount + "명이고, 수입은" + money + "입니다.");
	}
}
```

- 택시가 지녀야하는 속성으로 "택시", "승객 수", "받은 요금" 선언
- "택시"를 매개 변수로 받아 참조 변수(this가 가리키는 주소)에 해당 값을 저장하는 메서드 구현
- "요금"을 매개 변수로 받아 입력 된 값을 요금 처리하고 "승객 수"를 1 증가시키는 메서드 구현
- 택시, 승객 수와 수입을 출력하는 메서드 구현



**탑승 클래스**

```java
package cooperation;

public class TakeTrans {

	public static void main(String[] args) {
		Student studentJames = new Student("James", 5000);
		Student studentTomas = new Student("Tomas", 10000);
		Student studentEdward = new Student("Edward", 10000);
		
		Bus bus100 = new Bus(100);
		studentJames.takeBus(bus100);
		studentJames.showInfo();
		bus100.showInfo();
		
		Subway subwayGreen = new Subway("2호선");
		studentTomas.takeSubway(subwayGreen);
		studentTomas.showInfo();
		subwayGreen.showInfo();
		
		Taxi seoulTaxi = new Taxi("개인택시");
		studentEdward.takeTaxi(seoulTaxi);
		studentEdward.showInfo();
		seoulTaxi.showInfo();
	}
	

}
```

- 학생 이름이 "James", 가진 돈이 5000원인 새로운 Student 클래스 생성하여 studentJames 참조 변수에 저장
- 학생 이름이 "Tomas", 가진 돈이 10000원인 새로운 Student 클래스 생성하여 studentTomas 참조 변수에 저장
- 학생 이름이 "Edward", 가진 돈이 10000원인 새로운 Student 클래스 생성하여 studentEdward 참조 변수에 저장

- 버스 번호가 "100"인 새로운 Bus 클래스 생성하여 bus100 참조 변수에 저장
- studentJames.takeBus() 메서드를 이용하여 James가 가진 돈에서 1000원을 뺀 뒤 bus100의 승객을 1 증가하고 수입에 1000원을 더한다.
- James의 이름과 가진 돈을 출력한다.
- bus100의 버스 번호와 승객 수와 수입을 출력한다.

- 지하철 노선이 "2호선"인 새로운 Subway 클래스 생성하여 subwayGreen 참조 변수에 저장
- studentTomas.takeSubway() 메서드를 이용하여 Tomas가 가진 돈에서 1500원을 뺀 뒤 subwayGreen의 승객을 1 증가하고 수입에 1500원을 더한다.
- Tomas의 이름과 가진 돈을 출력한다.
- subwayGreen의 지하철 노선 정보와 승객 수와 수입을 출력한다.

- 택시가 "개인택시"인 새로운 Taxi 클래스 생성하여 seoulTaxi 참조 변수에 저장
- studentEdward.takeTaxi() 메서드를 이용하여 Edward가 가진 돈에서 10000원을 뺀 뒤 seoultaxi의 승객을 1 증가하고 수입에 10000원을 더한다.
- Edward의 이름과 가진 돈을 출력한다.
- taxi의 택시 정보와 승객 수와 수입을 출력한다.

**James님의 남은 돈은4000입니다.**
**버스100번의 승객은 1명이고, 수입은 1000입니다.**
**Tomas님의 남은 돈은8500입니다.**
**2호선의 승객은 1명이고, 수입은 1500입니다.**
**Edward님의 남은 돈은0입니다.**
**개인택시의 승객은 1명이고, 수입은10000입니다.**
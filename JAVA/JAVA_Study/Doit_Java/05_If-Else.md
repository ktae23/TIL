## **제어 흐름**

- **for문 : 반복 횟수가 정해진 경우**
- **do-while문 : 수행문을 반드시 한 번 이상 수행해야 할 경우**
- **while문 : 조건의 참/거짓에 따라 반복문을 수행하는 경우**

### *** if문과 if-else문**

**if 문**

```java
if (조건식){
	수행문; //조건식이 참일 경우에 이 문장을 수행
}
```

- **주어진 조건식이 '참'일 경우 중괄호 안에 있는 문장을 수행**
- **조건식에는 참, 거짓으로 판별되는 식이나 참, 거짓의 값을 가진 변수, 상수를 사용 할 수 있음.**

**if-else 문**

- **조건식을 만족하는 경우와 아닌 경우 모두를 나타 낼 때 사용.**
- **만약 ~ 라면 이렇게, 아니라면 저렇게**

```java
if (조건식) {
	수행문1: //조건식이 참일 경우에 이 문장을 수행
   
else {
	수행문2: //조건식이 거짓일 경우에 이 문장을 수행
}
public class IfExample1 {

	public static void main(String[] args) {
		
        char gender = 'F';
        if (gender == 'F'){
        	System.out.println("여성입니다.");
        }
        else{
        	Sysetm.out.println("남성입니다.");
		}

}
```

**if-else if-else 문**

- 하나의 상황에서 조건이 여러 개인 경우에 사용.
- if문만 반복해서 사용 할 경우 매 조건을 비교해보지만 if - else 문을 사용하면 첫 번째 조건을 비교해 보고 아니면 바로 다음 수행문을 실행 함.

```java
package chapter4;

public class IfExample {
	public static void main(String[] args) {
		int age = 9;
		int charge;
		
		if(age < 8) {
			charge = 1000;
			System.out.println("취학 전 아동입니다.");
		}
		else if(age < 14) {
			charge = 2000;
			System.out.println("초등학생입니다.");
		}
		else if(age < 20) {
			charge = 2500;
			System.out.println("중,고등학생입니다.");
		}
		else {
			charge = 3000;
			System.out.println("일반인입니다.");
			
		}
		System.out.println("입장료는" + charge + "원입니다.");
	}
}
```

**초등학생입니다.**
**입장료는2000원입니다****.**



------

### *** 조건문과 조건 연산자**

- **if-else 문은 조건 연산자로도 구현 할 수 있음**

```java
if(a>b){
	max = a;
	}
else {
	max = b;
    }
```

위 if - else 문과 아래 조건식은 같은 결과 값을 얻음

```java
max = (a > b) ? a : b;
```
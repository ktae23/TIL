### *** for 문**

**for문의 기본 구조**

- 초기화식은 for문이 시작할 때 딱 한번 수행하며 사용할 변수를 초기화 함
- 조건식에서 언제까지 반복 수행할 것인지 구현
- 증감식에서 반복 횟수나 for문에서 사용하는 변수값을 1만큼 늘리거나 줄임

```java
package loopexample;

public class ForExample1 {

	public static void main(String[] args) {

		int i;
		int sum;
		for(i = 1, sum = 0; i <= 10; i++) {
			sum += i;
		}
		System.out.println("1부터 10까지의 합은 " + sum + "입니다.");
	}

}
```

**1부터 10까지의 합은 55입니다.**



**for문을 자주 사용하는 이유**

- while문은 변수 최기화, 조건 비교, 증감식을 따로 구현해야 하지만 for문은 한 줄에 쓸 수 있음
- for문은 배열과 함께 자주 사용함

**for문 요소 생략하기 -** 코드가 중복되거나 논리 흐름상 필요 없을 경우 생략 가능

- 초기화 식 생략
  - 이미 이전에 다른 곳에서 변수가 초기화 되어서 중복으로 초기화 할 필요가 없을 때

```java
int = 0;
for(; i < 5; i++ {
	...
}
```

- 조건식 생략
  - 어떤 결과 값이 나왔을 때 멈추기 위해 for문 안에 if문을 사용 할 수 있음. 이럴 땐 조건식 생략 가능

```java
for(i = 0; ; i++ {
	sum += i;
    if(sum > 200) break;
}
```

- 증감식 생략
  - 증감식의 연산이 복잡하거나 다른 변수의 연산 결과 값에 자우된다면 생략하고 for문 안에 직접 입력

```java
for(i = 0; i < 5; ) {
	...
    i = (++i) % 10;
}
```

- 요소 모두 생략
  - 모든 요소를 생략하고 무한 반복하는 경우 생략.

```java
for ( ;' ; ) {
	...
}
```



### *** 중첩된 반복문**

- **반복문 안에 또 다른 반복문을 중첩하여 사용**

```java
package loopexample;

public class NestedLoop {

	public static void main(String[] args) {
		int dan;
		int times;
		
		for(dan = 2; dan <=9; dan++) {
			for(times = 1; times <= 9; times++) {
				System.out.println(dan + "X" + times + "=" + dan * times);
			}
			System.out.println( ); // 한줄 띄어서 출력 하도록.
		}

	}

}
```

**2X1=2
2X2=4
2X3=6
2X4=8
2X5=10
2X6=12
2X7=14
2X8=16
2X9=18
**

...



**8X7=56**
**8X8=64**
**8X9=72**

**9X1=9**
**9X2=18**
**9X3=27**
**9X4=36**
**9X5=45**
**9X6=54**
**9X7=63**
**9X8=72**
**9X9=81**
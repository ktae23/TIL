### *** 반복문 - while문**

- **조건식이 참인 동안 수행문을 반복하여 수행**
- **조건문이 하나일 경우 {}를 사용하지 않음**
- **조건문 내부에서 사용되는 변수는 미리 초기화 해주어야 오류가 나지 않음**

```java
package loopexample;

public class WhileExample1 {

	public static void main(String[] args) {

		int num = 1;
		int sum = 0;
		
		while(num <= 10) { // num값이 10보다 작거나 같을 동안
			sum += num;    // 합계를 뜻하는 sum에 num을 더하고
			num++;		   // num에 1씩 더함
		}
		System.out.println("1부터 10까지의 합은 " + sum + "입니다.");
	}

}
```

**1부터 10까지의 합은 55입니다.**



**while문이 무한히 반복하는 경우**

- 웹 서비스의 서버를 통한 작업은 24시간 동작해야 하므로 무한 반복이 필요함.



### *** do - while문**

- **do-while문은 무조건 한번 do 수행문을 실행한 다음 조건식을 검사함.**

```java
package loopexample;

public class WhileExample1 {

	public static void main(String[] args) {

		int num = 1;
		int sum = 0;
		
		do {
			sum += num;    // 합계를 뜻하는 sum에 num을 더하고
			num++;		   // num에 1씩 더함
		} while(num <= 10); { // num값이 10보다 작거나 같을 동안
						
		}
		System.out.println("1부터 10까지의 합은 " + sum + "입니다.");
	}

}
```
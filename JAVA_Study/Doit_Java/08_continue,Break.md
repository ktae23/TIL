### *** continue문**

- **반복문과 함께 사용 됨**
- **반복문 안에서 continue문을 만나면 이후 문장은 수행하지 않고 for문의 처음으로 돌아가서 증감식을 수행.**

```java
package loopexample;

public class ContinueExample {

	public static void main(String[] args) {
		int total = 0;
		int num;
		
		for(num = 1; num <= 100; num++) { //num 변수 1로 초기화, num이 100이 될때까지 반복
			if(num % 2 ==0)				// num이 짝수일 경우
				continue;				// for문으로 돌아가 증감식 수행
			total += num;				// num이 홀수일 경우 수행
			}
			System.out.println("1부터 100까지의 홀수의 합은" + total + "입니다."); 

	}

}
```

**1부터 100까지의 홀수의 합은2500입니다.**

###  

### *** break문**

- **반복문에서 더 이상 수행문을 반복하지 않고 빠져나갈 때 사용.**

```java
package loopexample;

public class WhileExample1 {

	public static void main(String[] args) {
		int sum = 0;
		int num = 0;
		
		for(num = 0; ; num++) { 
			sum += num;
			if(sum >= 100)				
				break;				
			}
			System.out.println("num : " + num); 
			System.out.println("sum : " + sum); 
	}

}
```



**break문의 위치**



- 모든 반복문을 빠져나오는 것이 아니라 break 문을 감싸고 있는 반복문만 빠져나옴.
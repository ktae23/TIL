### ***** **함수 호출과 스택 메모리**

```java
package student;

public class FunctionEX {

	public static void main(String[] args) {
		int num1 = 10; //num1 변수 정수형 선언 및 초기화
		int num2 = 30; //num2 변수 정수형 선언 및 초기화
		
		int sum = addNum(num1, num2);	// sum 변수 정수형 선언 및 addNum함수 결과 값 대입
        					// addNum 함수의 매개변수 n1에 num1을, n2에 num2를 대입
                            
		System.out.println(num1 + "+" num2 + "=" + sum + "입니다.");
        }
        
	public static int addNum(int n1, int n2) {	//정수 자료형을 반환하는 addNum함수
    							//매개변수로 정수형 n1, n2를 받는다
		int result = n1 + n2;			//n1 + n2의 값을 정수형 result 변수에 대입
		return result;				//result 변수 값을 반환. (정수형으로 반환)
	}
```

위의 n1,n2와 num1,nu2는 서로 무관한 변수

```java
int num1 = 10;
int num2 = 20;

int n1 = num1;
int n2 = num2;
```

- 풀어서 보자면 위와 같은 순서로 매개변수 n1,n2가 입력된 값을 받아주는 역할을 할 뿐.
- 위와 같이 함수 내부에서만 사용하는 변수를 '지역 변수'라고 함.

![img](https://blog.kakaocdn.net/dn/r8TBy/btqV2atqjCE/97RheXfLBEpRIErkjEpSw1/img.png)







- 함수가 호출되면 그 함수만을 위한 메모리 공간이 할당 되는데 이 공간을 '스택'이라고 부름









![img](https://blog.kakaocdn.net/dn/btxRYI/btqV0E9DQaK/8NcqcqvciMFA5sQ32AC411/img.png)





- 함수가 호출되는 순서대로 스택 메모리가 생성되고, 수행이 종료 된 함수는 순서대로 소멸 됨.
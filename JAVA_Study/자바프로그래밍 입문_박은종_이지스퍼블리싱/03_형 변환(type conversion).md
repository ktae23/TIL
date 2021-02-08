### *** 형 변환(type conversion)**

- **정수와 실수는 컴퓨터 내부에서 표현 되는 방식이 바르기 때문에 둘의 연산이 필요 할 경우 형 변환이 필요함.**

```java
int n = 10;          //int형 변수 n에 정수 값 10을 대입
double dnum = n;    //int형 변수 n의 값을 double형 변수 dnum에 대입
```

**묵시적 형 변환(자동 형 변환)**



1. **바이트 크기가 작은 자료형에서 큰 자료형으로의 형 변환은 자동으로 이뤄진다.**
2. **덜 정밀한 자료형에서 더 정밀한 자료형으로 형 변환은 자동으로 이뤄진다.**

![img](https://blog.kakaocdn.net/dn/ymlmf/btqSV8FgOZg/ENZUKgP1gGNZdb6Nn3SOEk/img.png)(출처 - Do it! 자바 프로그래밍 입문, 박은종)

위와 같은 화살표 방향으로는 자동으로 변환이 이뤄 진다.

표현 범위가 더 넓고 정밀한 뱡향으로는 자동 변환이 이뤄지지만 반대로 할 경우 표현 범위 한계로 자료 손실이 일어날 수 있다.



- 바이트 크기가 작은 자료형에서 큰 자료형으로 대입하는 경우

```java
byte bNum = 10;
int iNum = bNum; //byte형 변수 bNum 값을 int형 변수 iNum에 대입함.
```

위와 같은 경우 bNum의 크기는 1바이트고 iNum의 크기는 4바이트이므로 자료 손실 없이 변환이 되며 남은 공간은 0으로 채워 진다.



- 덜 정밀한 자료형에서 더 정밀한 자료형으로 대입하는 경우

```java
int iNum2 = 20;
float fNum = iNum2; // 정수형 값 iNum2을 실수형 값 fNum 변수에 대입
```

두 변수의 크기가 4바이트로 같더라도 정수형 int보다 실수형 float 자료형이 더 정밀하게 데이터를 표현 할 수 있으므로 변환 됨.



- 연산 중에 자동 변환 되는 경우

```java
int iNum = 20;
float fNum = iNum;    // 4바이트 정수형 자료 iNum을 4바이트 실수형 자료 fNum에 대입하여 형 변환.

double dNum;          // 8바이트 실수형 자료 dNum 선언.
dNum = fNum + iNum;   // 4바이트 정수형 자료와 4바이트 실수형 자료를 더하면 4바이트 실수형으로 형 변환 됨.
// 4바이트 실수형 자료를 8바이트 실수형 자료형에 대입하면 8바이트 실수형으로 형 변환 됨.
```

바이트 크기가 작은 수에서 큰 수로, 덜 정밀한 수에서 더 정밀한 수로 자료형이 자동으로 변환 됨.



```java
package chapter2;

public class ImplicitiConversion {

	public static void main(String[] args) {
		byte bNum = 10;
		int iNum = bNum;	//byte형 값이 int형 변수로 대입 됨
		
		System.out.println(bNum);
		System.out.println(iNum);
		
		int iNum2 = 20;
		float fNum = iNum2; //int형 값이 float형 변수로 대입됨 
		
		System.out.println(iNum);
		System.out.println(fNum);
		
		double dNum;
		dNum = fNum + iNum;
		System.out.println(dNum);
		
	}

}
```

**10**
**10**
**10**
**20.0**
**30.0**

------

**명시적 형 변환(강제 형 변환)**

\* 묵시적 형 변환과 반대의 경우로 생각 할 수 있음.



- 바이트 크기가 큰 자료형에서 작은 자료형으로 대입하는 경우

```java
int iNum = 10;
byte bNum = (byte)iNum; //강제로 형을 바꾸려면 바꾸려는 자료 형을 괄호를 이용해 명시 해야 함
```

4바이트인 int형 자료를 1바이트인 byte형 자료로 바꿀 경우 3바이트 만큼의 자료 손실이 일어날 수 있음.

때문에 변환하고자 하는 자료형을 명시해야 하며 강제로 형 변환을 진행 하게 된다.

위 예시의 경우 10은 1바이트 내로 표현이 가능하기에 자료 손실이 일어나지는 않는다.



```java
int iNum = 1000;
byte bNum = (byte)iNum;
```

하지만 이 경우 값 1000이 byte형의 표현 범위(-128~127)을 넘기 때문에 자료 손실이 발생하여 대입 된 값이 -24로 표현 된다.





- 더 정밀한 자료형에서 덜 정밀한 자료형으로 대입 되는 경우

실수 자료형에서 정수 자료형으로 값이 대입되는 경우에도 역시 형 변환을 명시적으로 해야 함.



```java
double dNum = 3.14;
int iNum2 = (int)dNum; // 실수 자료형 double 을 정수 자료형 int로 형 변환
```

**3.14**

**3**



위와 같은 경우 실수 자료형에서 정수 자료형으로 변환 되면서 소수점 아래 수가 손실 됨.



- 연산 중 형 변환

```java
package chapter2;

public class ExplicitConversion {
	public static void main(String[] args) {
		double dNum1 = 1.2;
		float fNum2 = 0.9F;
		
		int iNum3 = (int)dNum1 + (int)fNum2; // 두 실수가 각각 형 변환되어 더해짐
		int iNum4 = (int)(dNum1 + fNum2);    // 두 실수의 합이 먼저 계산되고 형 변환 됨
		System.out.println(iNum3);
		System.out.println(iNum4);
		
	}
}
```

**1**

**2**

iNum3 == (dNum1이 정수형으로 변하면서 1) + (fNum2가 정수형으로 변하면서 0) == 1

iNum4 == (dNum1 + fNum2 => 각 실수가 더 큰 자료형 값인 double형으로 자동 변환 됨

​        double 자료형으로 더해져서 2.1, 이후 정수형으로 변하면서 2) ==2


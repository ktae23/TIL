## *** switch - case 문**

```java
if(rank ==1) {
	medalColor = 'G';
}
else if(rank == 2) {
	medalColor = 'S'
}
else if(rank == 3) {
	medalColor = 'B'
}
else {
	medalColor = 'A'
}
```

> 위 if - else 문과 아래 switch - case 문은 같은 결과 값을 얻음



```java
switch(rank) {
	case 1 : medalColor = 'G';
		break;
    case 2 : medalColor = 'S'
		break;
    case 3 : medalColor = 'B'
		break;
    default : medalColor = 'A'
		break;
}
```

>  조건식의 결과가 정수 또는 문자열 값이고 그 값에 따라 수행되는 경우가 각각 다른 경우 효율적.
>
> case : ~ break; 까지가 조건식



### case문 동시에 사용하기

```java
switch(month) { 
	case 1 : case 3 : case 5 : case 7 : case 8 : case 10 : case 12 : day = 21;
		break;
    case 4 : case 6 : case 9 : case 11 : day = 30;
		break;
    case 2 : day = 28;
    	break;
}
```

---



### switch-case문에서 break문의 역할



break문을 사용하지 않을 경우 예시

```java
package chapter4;

public class IfExample {
	public static void main(String[] args) {
		int ranking = 1;
		char medalColor;
		switch(ranking) {
		case 1 : medalColor = 'G';
		case 2 : medalColor = 'S';
		case 3 : medalColor = 'B';
		default : medalColor = 'A';
	}
	System.out.println(ranking + "등 메달의 색깔은" + medalColor + "입니다.");
	}
}
```

**1등 메달의 색깔은A입니다.**

조건을 만족해서 메달 색이 정해져도 switch-case문을 빠져나오지 않기 때문에 마지막 조건까지 내려가서 기본 값으로 정해짐.

---

### case문에 문자열 사용하기



자바 7부터 case 값에 문자열도 사용 가능해 졌음.

이전에는 아래 처럼 직접 비교 메서드를 사용 해줘야 했음.

```java
if(medal.equals("Gold"){

  ...

}
```



```java
package chapter4;

public class IfExample {
	public static void main(String[] args) {
		String medal = "Gold";
			switch(medal) {
		case "Gold" :
			System.out.println("금메달입니다");		
			break;
		
		case "Silver" :
			System.out.println("은메달입니다");		
			break;			
		
		case "Bronze" :
			System.out.println("동메달입니다");		
			break;
		
		default :
			System.out.println("메달이 없습니다.");		
			break;		}
	}
}
```


**금메달입니다**
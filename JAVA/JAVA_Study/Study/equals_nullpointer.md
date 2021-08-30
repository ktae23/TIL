 

Null pointer Exception은 참조하는 객체가 null일 경우 발생한다.

쉽게 말해 . 점을 찍는 다는 것은 해당 객체를 참조한다는 뜻이다.

 

euals 메서드는 점을 찍어 앞쪽 객체를 참조하고 파라미터와 비교를 한다.

때문에 문자열을 비교하는 경우 보통은 많은 경우 객체를 참조하고 파라미터로 비교하는 문자열을 넘긴다.

```java
public class equalsTest {
	public static void main(String[] args) {
		 String allowed = null;
	        if(allowed.equals("allowed")){
	            System.out.println("일치");   
	        }else {
	            System.out.println("불일치");
	        }
	}
}
```



보통은 요청으로 전달 된 파라미터값을 비교하는 경우에 사용할거다. 

하지만 이 경우 참조한 객체가 null일 경우 

**Exception in thread "main" java.lang.NullPointerException: Cannot invoke "String.equals(Object)" because "allowed" is null at snippet.equalsTest.main(equalsTest.java:6)**



와 같은 예외를 발생시킨다. 때문에 항상 null이 아닌지 여부를 검사하는 로직을 추가해 줘야 한다.



하지만 처음부터 아래와 같이 참조하는 쪽을 명시된 문자열로 정해줌으로써 참조하는 객체가 null일 수 없도록 해주게 되면 전달 된 값이 null이더라도 null pointer exception이 발생하지 않는다.



```java
public class equalsTest {
	public static void main(String[] args) {
		 String allowed = null;
	        if("allowed".equals(allowed)){
	            System.out.println("일치");   
	        }else {
	            System.out.println("불일치");
	        }
	}
}
```

- 불일치



로직을 열어보지 않고 의식적으로 하지 않으면 그냥 하던대로만 사용하게 되는것 같다.

인턴 업무를 하는 중 연구소장님께서 null 검증 로직과 관한 팁을 주시면서 알려주신 방법을 기록한다.


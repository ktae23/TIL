## 멀티 쓰레드

#### 상속 사용

```java
class A extends Thread {
	 public void run(){
	 	// 실행문
	 }
}
```

- 쓰레드.start();로 실행

---

### 인터페이스 사용

```java
class A implements Runnable {
	@overrride
	public void run() {
		//실행문
	}
}
```

- ```java
  A t1 = new A();
  Thread th1 = new Thread(t1);
  th1.start();
  ```

  Start메서드가 있는 쓰레드와 연결해서 사용



```java
package test.thread;

public class Test {

	public static void main(String[] args) {
		A t1=new A();
		A t2=new A();
		
		t1.start();
		t2.start();
	}
}	
	class A extends Thread{
		public void run( ) {
			for(int i=0; i<100; i++) {
				System.out.println(i+"+1="+(i+1));
			}
		}
	}
```

>  Thread를 상속 받은 클래스에서 오버라이딩 된 run()객체 속 구현문을  쓰레드.start()메서드를 통해 실행 된다.



### 동시성

- 멀티 작업을 위해 하나의 코어에서 멀티 스레드가 번갈아가며 실행하는 성질

### 병렬성

- 멀티 작업을 위해 멀티 코어에서 개별 스레드를 동시에 실행하는 성질
###  Frame UI 내에서 Inner Class 사용하기

- 특정 클래스 내에서만 사용하는 용도라면 클래스 내부의 멤버 요소로 클래스를 선언.

  ```java
  public class ClientUi {
  	TextArea ta;
  	TextField tf;
  	
  	public void onCreate() {
  		Frame f = new Frame("나의 채팅");
  		Panel p=new Panel();	
  		Button b1 = new Button("전송");
  
  		
  		tf=new TextField(20);
  		ta=new TextArea();
  
  		WindowListener fHandler=new MyFrameHandler();
  		f.addWindowListener(fHandler);		
  		
  		MyButtonHandler b1Handler=new MyButtonHandler();			
  		b1.addActionListener(b1Handler);
  		
  		MyButtonHandler tfHandler=new MyButtonHandler();			
  		tf.addActionListener(tfHandler);
  
  
      // Main 메서드
  	public static void main(String[] args) {
          //ClientUi 클래스 생성 및 메서드 호출
  		ClientUi ui=new ClientUi();
  		ui.onCreate();
  		
  
  	}
  	//ClientUi 클래스 내부에 선언한 클래스, ClientUi 내부에서 사용할 목적.
  	public class MyButtonHandler implements ActionListener{
  	
  			
  		@Override
  		public void actionPerformed(ActionEvent e) {
  			//하고자 하는 일
  			String msg=tf.getText();
  			ta.append(msg+"\n");
  			tf.setText("");
  		}
  	}
      	public class MyFrameHandler extends WindowAdapter{	
  
  		@Override
  		public void windowClosing(WindowEvent e) {
  			System.out.println("windowClosing");
  			System.exit(0);
  		}
  	}
  }
  ```



* bin 폴더에서 확인하면 OuterClassName$InnerClassName.class로 이름 지어짐

### Frame UI 내에서 Anonymous Class 사용하기

- 메서드 안에 지역 클래스를 익명으로 사용하기
- 해당 블럭을 메서드 내부에서만 이용할 경우 사용

```java
f.addWindowListener(fHandler);
--------------------------------------------
	public class MyFrameHandler extends WindowAdapter{	

		@Override
		public void windowClosing(WindowEvent e) {
			System.out.println("windowClosing");
			System.exit(0);
		}
	}
```

> 위 코드를 아래 코드로 변경

```java
		f.addWindowListener(new WindowAdapter(){	

			@Override
			public void windowClosing(WindowEvent e) {
				System.out.println("windowClosing");
				System.exit(0);
			}
		});	
```

* 상속해주는 (Super클래스) 클래스 이름으로 사용
* bin 폴더에서 확인하면 OuterClassName$1.class, OuterClassName$2.class 순서대로 이름 지어 짐



### Local Class에서의 사용 제한

```java
package imsi;

public class Test {	// 아웃 클래스 시작
	public static void main(String[] args) { //메서드 블럭 시작
		int out=10; // 로컬클래스에서 사용하는 값은 final이어야 하지만 자바 8부터는 명시하지 않아도 자동 적용 됨

	class Inner{	//이너 클래스 시작
		public int count() {
			int cnt=0;
			for(int i=0;i<out;i++) {
				cnt +=i;
			}
			return cnt;
		}
	}	// 이너 클래스 끝
		Inner a=new Inner();
		System.out.println(a.count());
		
	} // 메서드 블럭 끝

}// 아웃 클래스 끝

```



### Lambda식 표현

- 딱 하나의 메서드만 지닌 interface == @FunctionalInterface는 lambda식으로 표현 가능

```java
	/*
	    b1.addActionListener(new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			//하고자 하는 일
			chatMsg();
		}
	});
	*/
    //위 메서드의 Lambda식 표현
    b1.addActionListener((ActionEvent e) ->{
    // 하고 싶은 일
    chatMsg();
    }
```

- ` -> ` : Lambda식 (화살표 함수)

```java
b1.addActionListener(() ->{ // 매개변수 없는 경우
			chatMsg();
			}
		);
```

```java
b1.addActionListener(e ->{ // 매개변수 하나인 경우 소괄호 생략 가능
			chatMsg();
			}
		);
```

```java
b1.addActionListener(e ->
			chatMsg() // 실행 문 하나인 경우 중괄호, 세미 콜론 생략 가능
			);
```

> 한줄 표현 할 경우

```java
b1.addActionListener(e -> chatMsg());
```


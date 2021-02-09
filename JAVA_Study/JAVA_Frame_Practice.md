### Frame 한글 인코딩

- Run as - Run Configurations - Arguments

```
-Dfile.encoding=MS949
```



### Frame 생성, 설정

```java
import java.awt.Frame;
import java.awt.BorderLayout;

Frame f = new Frame();

f.add(b, BorderLayout.SOUTH);
f.setSize(500,300);
f.setVisible(true);

f.addWindowListener(fHandler); //fHandler는 WindowListener 객체
```



### Button 생성, 설정

```java
import java.awt.Button;

Button b = new Button("버튼");
b.addActionListener(b1Handler); //b1Handler는 ActionListener 객체
```



### Panel 생성, 설정

```java
import java.awt.Panel;
import java.awt.Color;

Panel p=new Panel();
p.setBackground(Color.gray);

```



### TextArea 생성, 설정

```java
import java.awt.TextArea;

TextArea ta=new TextArea();
f.add(ta,BorderLayout.CENTER); //f는 프레임 객체
```



### TextField 생성, 설정

```java
import java.awt.TextField;

TextField tf=new TextField(20);
tf.addActionListener(b1Handler); //b1Handler는 ActionListener 객체
```



### Listener & Adapter

- WindowListener는 interface로 implements 하여 7개 메소드 전부 오버라이딩 해야 함

```java
class Classname implements WindowListener
    // 속해있는 7개 메서드 전부 오버라이딩 필수
```



- WindowAdapter는 abstract class로 extends 하여 원하는 메소드만 구현하여 사용 가능

```java
class ClassName extends WindowAdapter
    // 원하는 메서드만 구현 가능
```

- MouseListener, MouseAdapter 등 비슷한 클래스 제공

### 
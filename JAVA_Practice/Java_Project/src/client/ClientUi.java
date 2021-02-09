package client;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

	
public class ClientUi {
	TextArea ta;
	TextField tf;
	
	public void chatMsg() {
		String msg=tf.getText();
		ta.append(msg+"\n");
		tf.setText("");		
	}
	
	public void onCreate() {
		Frame f = new Frame("나의 채팅");
		Panel p=new Panel();	
		Panel p1=new Panel();
		Button b1 = new Button("전송");
		Button b2 = new Button("저장");
		Button b3 = new Button("백업");
		Checkbox c1 = new Checkbox("사진");
		Checkbox c2 = new Checkbox("동영상");
		Checkbox c3 = new Checkbox("문자");
		
		tf=new TextField(20);
		ta=new TextArea();
		
//-------------------------------------------------------------------------			
/*		
		ta.addTextListener(new TextListener() {
			@Override
			public void textValueChanged(TextEvent e) {
				System.out.println("텍스트값 변경");
			}
		});
*/
		// 위 블럭 람다식으로 표현
/*		
		ta.addTextListener((e) -> {
			System.out.println("텍스트값 변경");
		}
	);
*/
		// 위 식을 더 간단하게
		// 매개변수 하나면 소괄호 생략, 실행문 하나면 중괄호 생략
		ta.addTextListener(e -> System.out.println("텍스트값 변경")	);
//-------------------------------------------------------------------------		
		f.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);				
			}
		});
		
		// 익명 클래스 사용
		f.addWindowListener(new WindowAdapter(){	// 부모 클래스를 매개변수로 넣음

			@Override
			public void windowClosing(WindowEvent e) {
				System.out.println("windowClosing");
				System.exit(0);
			}
		});		
		
		

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
			chatMsg();
			}
		);
		
		
		tf.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				//하고자 하는 일
				chatMsg();
			}
		});

		
		f.add(ta, BorderLayout.CENTER);
		f.add(p, BorderLayout.SOUTH);
		f.add(p1, BorderLayout.EAST);
		p.add(tf);
		p.add(b1);
		
		p1.add(b2);
		p1.add(b3);

		p1.add(c1);
		p1.add(c2);
		p1.add(c3);
		
		
		
		p.setBackground(Color.gray);
		p1.setBackground(Color.lightGray);
		
		
//		Color bgColor = new Color(123,24,56);
		f.setLocation(800, 200);
		f.setSize(400,500);
		f.setVisible(true);
	}

    // Main 메서드
	public static void main(String[] args) {
        //ClientUi 클래스 생성 및 메서드 호출
		ClientUi ui=new ClientUi();
		ui.onCreate();
		

	}	
	
}

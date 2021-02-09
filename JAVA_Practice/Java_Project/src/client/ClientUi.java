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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

	
public class ClientUi {
	TextArea ta;
	TextField tf;
	
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

		WindowListener fHandler=new MyFrameHandler();
		f.addWindowListener(fHandler);		
		
		MyButtonHandler b1Handler=new MyButtonHandler();			
		b1.addActionListener(b1Handler);
		
		tf.addActionListener(b1Handler);

		
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

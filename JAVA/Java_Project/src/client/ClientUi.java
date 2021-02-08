package client;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.WindowListener;

public class ClientUi {

	public static void main(String[] args) {
		Frame f = new Frame("나의 채팅");
		Panel p=new Panel();	
		Button b1 = new Button("전송");
		TextField tf=new TextField(20);
		TextArea ta=new TextArea();

		WindowListener fHandler = new MyFrameHandler();
		f.addWindowListener(fHandler);
		
		
		MyButtonHandler b1Handler=new MyButtonHandler();
		b1Handler.setTextArea(ta);
		b1Handler.setTextField(tf);
		b1.addActionListener(b1Handler);
		
		tf.addActionListener(b1Handler);
		
		
/*		Button b2 = new Button("전송2");
		Button b3 = new Button("전송3");
		Button b4 = new Button("전송4");
		Button b5 = new Button("전송5");
*/
/*		GridLayout mgr=new GridLayout(2,3);
		FlowLayout mgr=new FlowLayout();
		f.setLayout(mgr);
*/		
/*		f.add(b1, BorderLayout.CENTER);
		f.add(b2, BorderLayout.NORTH);
		f.add(p, BorderLayout.SOUTH);
		f.add(b4, BorderLayout.EAST);
		f.add(b5, BorderLayout.WEST);*/
		
/*		f.add(p, BorderLayout.SOUTH)
		p.add(b1);
		p.add(b2);
		p.add(b3);
		p.add(b4);
		p.add(b5);*/
		
		
		f.add(ta, BorderLayout.CENTER);
		f.add(p, BorderLayout.SOUTH);
		p.add(tf);
		p.add(b1);
		
		
		p.setBackground(Color.gray);
		
		
//		Color bgColor = new Color(123,24,56);
		f.setBackground(Color.GREEN);
		f.setLocation(800, 200);
		f.setSize(400,500);
		f.setVisible(true);

	}
}

package imsi;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class Test {

	public static void main(String[] args) {
	
		Frame f=new Frame();
		TextArea ta=new TextArea();
		Button b=new Button("Button");
		Panel p=new Panel();
		
//		A handler=new A();
//		ta.addMouseListener(handler);
		
		ta.setBackground(Color.LIGHT_GRAY);
		p.setSize(500,100);
		p.setBackground(Color.green);
		f.add(ta,BorderLayout.CENTER);
		f.add(b, BorderLayout.SOUTH);
		f.setSize(500,300);
		f.setVisible(true);
	
	
	
	}
class A implements MouseListener{

		@Override
		public void mouseClicked(MouseEvent e) {
			System.out.println("마우스가 클릭되었습니다.");
		}

		@Override
		public void mousePressed(MouseEvent e) {
			System.out.println("마우스가 눌렸습니다.");
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			System.out.println("마우스가 릴리즈 되었습니다.");
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			 System.out.println("마우스가 입력되었습니다.");
		}

		@Override
		public void mouseExited(MouseEvent e) {
			System.out.println("마우스가 나가졌습니다.");
		}
		
		
	}
}

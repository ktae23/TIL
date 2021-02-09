package client;

import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MyButtonHandler implements ActionListener{
	TextArea ta;
	TextField tf;
	
/*	private final static MyButtonHandler me = new MyButtonHandler();	
*/  // 싱글톤 패턴을 위한 단일 인스턴스 생성
	
/*	 
	private MyButtonHandler() {
		super();
	}
*/  // 싱글톤 패턴을 위한 private 생성자
	
	
/*	
   public static MyButtonHandler getInstance() {
	
		return me;
}
*/ // 단일 인스턴스 사용을 위한 getInstance() 메서드
	
	
	public MyButtonHandler(TextArea ta, TextField tf) {
		this.ta=ta;
		this.tf=tf;
}
	@Override
	public void actionPerformed(ActionEvent e) {
		//하고자 하는 일
		String msg=tf.getText();
		ta.append(msg+"\n");
		tf.setText("");
	}

}

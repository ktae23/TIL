package client;

import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MyButtonHandler implements ActionListener{
	TextArea ta;
	TextField tf;
	
	private final static MyButtonHandler me = new MyButtonHandler();
	
	private MyButtonHandler() {
		super();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		//하고자 하는 일
		String msg=tf.getText();
		ta.append(msg+"\n");
		tf.setText("");
	}
	
	public void setResouce(TextArea ta, TextField tf) {
		this.ta=ta;
		this.tf=tf;
	}

	public static MyButtonHandler getInstance() {
		
		return me;
	}


}

package client;

import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MyButtonHandler implements ActionListener{
	TextArea ta;
	TextField tf;

	@Override
	public void actionPerformed(ActionEvent e) {
		//하고자 하는 일
		String msg=tf.getText();
		ta.append(msg+"\n");
		tf.setText("");
	}
	
	public void setTextArea(TextArea ta) {
		this.ta=ta;
	}
	
	public void setTextField(TextField tf) {
		this.tf=tf;
	}

}

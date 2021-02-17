package client.ui;

import java.awt.Button;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Date;

import javax.swing.JOptionPane;

import common.entity.Member;


public class CafeUi extends Frame{
	
	TextField tf_memId, tf_memName,tf_phone;
	Button btn_memInsert,btn_memUpdate,btn_memSelect,btn_memDelete;
	
			
	@Override
	public void setVisible(boolean b) {
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		setLayout(new GridLayout(1,3));
		Panel memberPanel=new Panel();
		Panel memberPanel_sub1=new Panel();
		Panel memberPanel_sub2=new Panel();
		tf_memId=new TextField(20);
		tf_memName=new TextField(20);
		tf_phone=new TextField(20);
		btn_memInsert=new Button("가입");
		btn_memUpdate=new Button("수정");
		btn_memSelect=new Button("조회");
		btn_memDelete=new Button("삭제");
		add(memberPanel);
		memberPanel.setLayout(new GridLayout(2,1));
		memberPanel.add(memberPanel_sub1);
		memberPanel.add(memberPanel_sub2);
		memberPanel_sub1.setLayout(new GridLayout(3,2,10,10));		
		memberPanel_sub1.add(new Label("고객 ID"));		
		memberPanel_sub1.add(tf_memId);
		memberPanel_sub1.add(new Label("고객이름"));
		memberPanel_sub1.add(tf_memName);
		memberPanel_sub1.add(new Label("전화번호"));
		memberPanel_sub1.add(tf_phone);
		memberPanel_sub2.add(btn_memInsert);
		memberPanel_sub2.add(btn_memUpdate);
		memberPanel_sub2.add(btn_memSelect);
		memberPanel_sub2.add(btn_memDelete);
		//setSize(800,500);
		pack();
		eventProcess();
		super.setVisible(b);
	}


	private void eventProcess() {
		btn_memInsert.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// 가입
				String memId= tf_memId.getText();
				String memName=tf_memName.getText();
				String phone=tf_phone.getText();
				Date now=new Date();
				Member m=new Member(memId,memName,now,phone);
				System.out.println(m);
				JOptionPane.showMessageDialog(CafeUi.this, "가입 되셨습니다.");
			}
		});
		
		btn_memSelect.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						// 조회
						String memId= tf_memId.getText();
						JOptionPane.showMessageDialog(CafeUi.this, memId+"님 조회 되셨습니다.");
					}
				});

	}
}
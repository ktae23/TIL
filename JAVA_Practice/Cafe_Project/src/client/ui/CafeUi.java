package client.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.Date;

import javax.swing.JOptionPane;

import common.entity.Member;
import common.util.CafeException;
import server.service.MemberService;


public class CafeUi extends Frame{
	
	TextField tf_memId, tf_memName,tf_phone,tf_prodCode,tf_prodName,tf_prodPrice,tf_orderMem,tf_orderProd,tf_orderQuan;
	Button btn_memInsert,btn_memUpdate,btn_memSelect,btn_memDelete,btn_prodInsert,btn_prodUpdate,btn_prodSelect,btn_prodDelete;
	Button btn_order;
	TextArea ta_mem,ta_prod,ta_order;
			
	@Override
	public void setVisible(boolean b) {
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		setLayout(new GridLayout(1,3,20,0));
		memberPanel();//����
		productPanel();	//��ǰ��
		ordersPanel();//�ֹ���
		
		
		setLocation(200,300);
		pack();
		eventProcess();
		super.setVisible(b);
	}

	private void ordersPanel() {
		Panel orderPanel=new Panel();
		Panel orderPanel_sub1=new Panel();
		Panel orderPanel_sub2=new Panel();
		Panel orderPanel_sub3=new Panel();
		tf_orderMem=new TextField(20);
		tf_orderProd=new TextField(20);
		tf_orderQuan=new TextField(20);
		
		btn_order=new Button("�ֹ��ϱ�");

		ta_order=new TextArea();
	
		add(orderPanel);
		orderPanel.setLayout(new GridLayout(3,1,0,20));
		orderPanel_sub3.add(ta_order);
		orderPanel.add(orderPanel_sub3);
		orderPanel.add(orderPanel_sub1);
		orderPanel.add(orderPanel_sub2);
		orderPanel_sub1.setLayout(new GridLayout(3,3,0,10));	

		orderPanel_sub1.add(new Label("        ����"));		
		orderPanel_sub1.add(tf_orderMem);
		orderPanel_sub1.add(new Label());
		orderPanel_sub1.add(new Label("        ��ǰ��"));
		orderPanel_sub1.add(tf_orderProd);
		orderPanel_sub1.add(new Label());
		orderPanel_sub1.add(new Label("        ����"));
		orderPanel_sub1.add(tf_orderQuan);
		orderPanel_sub1.add(new Label());
		orderPanel_sub2.add(btn_order);
		
		orderPanel.setBackground(new Color(120,150,051));
		
		btn_order.setPreferredSize(new Dimension(80,80));
		
	}
	
	private void productPanel() {
		Panel productPanel=new Panel();
		Panel productPanel_sub1=new Panel();
		Panel productPanel_sub2=new Panel();
		Panel productPanel_sub3=new Panel();
		tf_prodCode=new TextField(20);
		tf_prodName=new TextField(20);
		tf_prodPrice=new TextField(20);
		btn_prodInsert=new Button("���");
		btn_prodUpdate=new Button("����");
		btn_prodSelect=new Button("��ȸ");
		btn_prodDelete=new Button("����");
		ta_prod=new TextArea();
	
		add(productPanel);
		productPanel.setLayout(new GridLayout(3,1,0,20));
		productPanel_sub3.add(ta_prod);
		productPanel.add(productPanel_sub3);
		productPanel.add(productPanel_sub1);
		productPanel.add(productPanel_sub2);
		productPanel_sub1.setLayout(new GridLayout(3,3,0,10));	

		productPanel_sub1.add(new Label("        ��ǰ �ڵ�"));		
		productPanel_sub1.add(tf_prodCode);
		productPanel_sub1.add(new Label());
		productPanel_sub1.add(new Label("        ��ǰ��"));
		productPanel_sub1.add(tf_prodName);
		productPanel_sub1.add(new Label());
		productPanel_sub1.add(new Label("        ����"));
		productPanel_sub1.add(tf_prodPrice);
		productPanel_sub1.add(new Label());
		productPanel_sub2.add(btn_prodInsert);
		productPanel_sub2.add(btn_prodUpdate);
		productPanel_sub2.add(btn_prodSelect);
		productPanel_sub2.add(btn_prodDelete);
		productPanel.setBackground(new Color(204,204,051));
		
		btn_prodInsert.setPreferredSize(new Dimension(80,80));
		btn_prodUpdate.setPreferredSize(new Dimension(80,80));
		btn_prodSelect.setPreferredSize(new Dimension(80,80));
		btn_prodDelete.setPreferredSize(new Dimension(80,80));
	}
	
	private void memberPanel() {
		Panel memberPanel=new Panel();
		Panel memberPanel_sub1=new Panel();
		Panel memberPanel_sub2=new Panel();
		Panel memberPanel_sub3=new Panel();
		tf_memId=new TextField(20);
		tf_memName=new TextField(20);
		tf_phone=new TextField(20);
		btn_memInsert=new Button("����");
		btn_memUpdate=new Button("����");
		btn_memSelect=new Button("��ȸ");
		btn_memDelete=new Button("����");
		ta_mem=new TextArea();
		
		add(memberPanel);
		memberPanel.setLayout(new GridLayout(3,1,0,20));
		memberPanel_sub3.add(ta_mem);
		memberPanel.add(memberPanel_sub3);
		memberPanel.add(memberPanel_sub1);
		memberPanel.add(memberPanel_sub2);
		memberPanel_sub1.setLayout(new GridLayout(3,3,0,10));	

		memberPanel_sub1.add(new Label("        �� ID"));		
		memberPanel_sub1.add(tf_memId);
		memberPanel_sub1.add(new Label());
		memberPanel_sub1.add(new Label("        ����"));
		memberPanel_sub1.add(tf_memName);
		memberPanel_sub1.add(new Label());
		memberPanel_sub1.add(new Label("        ��ȭ��ȣ"));
		memberPanel_sub1.add(tf_phone);
		memberPanel_sub1.add(new Label());
		memberPanel_sub2.add(btn_memInsert);
		memberPanel_sub2.add(btn_memUpdate);
		memberPanel_sub2.add(btn_memSelect);
		memberPanel_sub2.add(btn_memDelete);
		memberPanel.setBackground(new Color(204,153,102));
		
		btn_memInsert.setPreferredSize(new Dimension(80,80));
		btn_memUpdate.setPreferredSize(new Dimension(80,80));
		btn_memSelect.setPreferredSize(new Dimension(80,80));
		btn_memDelete.setPreferredSize(new Dimension(80,80));
	}



	private void eventProcess() {
		
		btn_memSelect.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// ��ȸ
				String memId= tf_memId.getText();
				JOptionPane.showMessageDialog(CafeUi.this, memId+"�� ��ȸ �Ǽ̽��ϴ�.");
			}
		});
		
		btn_memInsert.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// ����
				String memId= tf_memId.getText();
				String memName=tf_memName.getText();
				String phone=tf_phone.getText();
				Date now=new Date();
				Member m=new Member(memId,memName,now,phone);
				System.out.println(m);
				try {
					MemberService mservice=new MemberService();
					mservice.insertMember(m);
					JOptionPane.showMessageDialog(CafeUi.this, "���� �Ǽ̽��ϴ�.");
				} catch (CafeException e1) {
					JOptionPane.showMessageDialog(CafeUi.this, e1.getMessage());
				}
				
			}
		});
		
	}

}
package client.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.JOptionPane;

import common.entity.MemberDTO;
import common.entity.ProductDTO;
import common.util.CafeException;
import server.service.MemberService;
import server.service.ProductService;


public class CafeUi extends Frame{
	
	MemberService mservice;
	ProductService pService;
	
	TextField tf_memId, tf_memName,tf_phone,tf_prodCode,tf_prodName,tf_prodPrice,tf_orderMem,tf_orderProd,tf_orderQuan;
	Button btn_memInsert,btn_memUpdate,btn_memSelect,btn_memDelete,btn_prodInsert,btn_prodUpdate,btn_prodSelect,btn_prodDelete;
	Button btn_order;
	TextArea ta_mem,ta_prod,ta_order;
	
	
			
	@Override
	public void setVisible(boolean b) {
		try {
			mservice=new MemberService();
			pService = new ProductService();
		} catch (CafeException e1) {
			System.out.println(e1.getMessage()+"시스템 종료");
			System.exit(0);
		}
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		setLayout(new GridLayout(1,3,20,0));
		memberPanel();//고객판
		productPanel();	//상품판
		ordersPanel();//주문판
		
		
		setLocation(200,200);
		pack();
		eventProcess();
		setMemberList();
		setProductList();
		super.setVisible(b);
	}
	
	private void setProductList() {
		try {
			ArrayList<ProductDTO> list=pService.selectProduct();
			ta_prod.setText("");
			for(ProductDTO p:list) {
				ta_prod.append(p.getProdCode()+"\t"+p.getProdName()+"\t"+p.getPrice()+"\n");
			}
		} catch (CafeException e) {
			JOptionPane.showMessageDialog(this, e.getMessage());
		}
		
	}

	private void setMemberList() {
		// 화면 뜨자마자 모든 고객 리스트가 보이게
		try {
			
			ArrayList<MemberDTO> list=mservice.selectMember();
			ta_mem.setText("");
			for(MemberDTO m:list) {
				ta_mem.append(m.getMemId()+"\t"+m.getName()+"\t"+m.getmDate()+"\t"+m.getPhone()+"\t"+m.getPoint()+"\n");
			}
		} catch (CafeException e) {
			JOptionPane.showMessageDialog(this, e.getMessage());
		}
		
	}

	private void ordersPanel() {
		Panel orderPanel=new Panel();
		Panel orderPanel_sub1=new Panel();
		Panel orderPanel_sub2=new Panel();
		Panel orderPanel_sub3=new Panel();
		tf_orderMem=new TextField(20);
		tf_orderProd=new TextField(20);
		tf_orderQuan=new TextField(20);
		
		btn_order=new Button("주문하기");

		ta_order=new TextArea();
	
		add(orderPanel);
		orderPanel.setLayout(new GridLayout(3,1,0,20));
		orderPanel_sub3.add(ta_order);
		orderPanel.add(orderPanel_sub3);
		orderPanel.add(orderPanel_sub1);
		orderPanel.add(orderPanel_sub2);
		orderPanel_sub1.setLayout(new GridLayout(3,3,0,10));	

		orderPanel_sub1.add(new Label("        고객명"));		
		orderPanel_sub1.add(tf_orderMem);
		orderPanel_sub1.add(new Label());
		orderPanel_sub1.add(new Label("        상품명"));
		orderPanel_sub1.add(tf_orderProd);
		orderPanel_sub1.add(new Label());
		orderPanel_sub1.add(new Label("        수량"));
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
		btn_prodInsert=new Button("등록");
		btn_prodUpdate=new Button("수정");
		btn_prodSelect=new Button("조회");
		btn_prodDelete=new Button("삭제");
		ta_prod=new TextArea();
	
		add(productPanel);
		productPanel.setLayout(new GridLayout(3,1,0,20));
		productPanel_sub3.add(ta_prod);
		productPanel.add(productPanel_sub3);
		productPanel.add(productPanel_sub1);
		productPanel.add(productPanel_sub2);
		productPanel_sub1.setLayout(new GridLayout(3,3,0,10));	

		productPanel_sub1.add(new Label("        상품 코드"));		
		productPanel_sub1.add(tf_prodCode);
		productPanel_sub1.add(new Label());
		productPanel_sub1.add(new Label("        상품명"));
		productPanel_sub1.add(tf_prodName);
		productPanel_sub1.add(new Label());
		productPanel_sub1.add(new Label("        가격"));
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
		btn_memInsert=new Button("가입");
		btn_memUpdate=new Button("수정");
		btn_memSelect=new Button("조회");
		btn_memDelete=new Button("삭제");
		ta_mem=new TextArea();
		
		add(memberPanel);
		memberPanel.setLayout(new GridLayout(3,1,0,20));
		memberPanel_sub3.add(ta_mem);
		memberPanel.add(memberPanel_sub3);
		memberPanel.add(memberPanel_sub1);
		memberPanel.add(memberPanel_sub2);
		memberPanel_sub1.setLayout(new GridLayout(3,3,0,10));	

		memberPanel_sub1.add(new Label("        고객 ID"));		
		memberPanel_sub1.add(tf_memId);
		memberPanel_sub1.add(new Label());
		memberPanel_sub1.add(new Label("        고객명"));
		memberPanel_sub1.add(tf_memName);
		memberPanel_sub1.add(new Label());
		memberPanel_sub1.add(new Label("        전화번호"));
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
		
		//주문
		btn_order.addActionListener(e->{
			String memId=tf_memId.getText();
			String prodCode=tf_prodCode.getText();
			int quantity=Integer.parseInt(tf_orderQuan.getText());
			
			
		});
		
		//상품조회
		btn_prodSelect.addActionListener(e-> { //람다식
				String prodCode=tf_prodCode.getText();
				try {
					String prodName=pService.selectProduct(prodCode);
					if(prodName==null) {
						JOptionPane.showMessageDialog(CafeUi.this, "상품 코드를 확인해 주세요");
					}else {
						tf_orderProd.setText(prodName);
					}
				} catch (CafeException e1) {
					JOptionPane.showMessageDialog(CafeUi.this, e1.getMessage());
				}
				
			}
		);
		
		
		//상품 등록
		btn_prodInsert.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				String prodCode=tf_prodCode.getText();
				String prodName=tf_prodName.getText();
				int price=Integer.parseInt(tf_prodPrice.getText());
				ProductDTO p=new ProductDTO(prodCode, prodName, price);
				
				try {
					
					pService.insertProduct(p);
					setProductList();
					JOptionPane.showMessageDialog(CafeUi.this, "상품 등록 완료");
				} catch (CafeException e1) {
					JOptionPane.showMessageDialog(CafeUi.this, e1.getMessage());
				}
				
				
			}
		});
		
		// 고객 조회		
		btn_memSelect.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				String memId= tf_memId.getText();
				try {
					String memName=mservice.selectMember(memId);
					if(memName==null) {
						JOptionPane.showMessageDialog(CafeUi.this, "고객 ID를 확인해 주세요");
					}else {
						tf_orderMem.setText(memName);
					}
				} catch (CafeException e1) {
					JOptionPane.showMessageDialog(CafeUi.this, e1.getMessage());
				}
				
			}
		});
		
		btn_memInsert.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// 가입
				String memId= tf_memId.getText();
				String memName=tf_memName.getText();
				String phone=tf_phone.getText();
				Date now=new Date();
				MemberDTO m=new MemberDTO(memId,memName,now,phone);
				System.out.println(m);
				try {
										mservice.insertMember(m);
					setMemberList();
					tf_memId.setText("");
					tf_memName.setText("");
					tf_phone.setText("");
					JOptionPane.showMessageDialog(CafeUi.this, "가입 되셨습니다.");
				} catch (CafeException e1) {
					JOptionPane.showMessageDialog(CafeUi.this, e1.getMessage());
				}
				
			}
		});
		
	}

}
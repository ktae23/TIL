package client;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

	
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
		MenuBar mb=new MenuBar();
		Menu file_menu=new Menu("파일");
		Menu edit_menu=new Menu("편집");
		MenuItem open_item=new MenuItem("열기");
		MenuItem save_item=new MenuItem("저장");
		
		file_menu.add(open_item);
		file_menu.add(save_item);
		
		mb.add(file_menu);
		mb.add(edit_menu);
		f.setMenuBar(mb);
		
		open_item.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent a) {
				System.out.println("open file");
				FileDialog open=new FileDialog(f, "열기 창", FileDialog.LOAD); // 열기 창 설정
				open.setVisible(true); //컨테이너 계열들은 보여주는 설정이 필요.

				FileReader fr = null;
				BufferedReader br = null;
				
				try {
					fr = new FileReader(open.getDirectory()+open.getFile()); //선택하는 파일의 경로 획득
					br=new  BufferedReader(fr);
					String oneLine="";
					ta.setText("");// 빈 문자열로 셋팅해서 지워지는 효과 
					while((oneLine = br.readLine()) != null) {
						ta.append(oneLine+"\n");	// append는 매 입력이 추가되는 방식
					}
				
				
				} catch (FileNotFoundException e) {
					e.printStackTrace(); // 실행시점에선 삭제 해야하지만 개발 시점에선 에러 추적을 위해 사용
				} catch (IOException e) {
					e.printStackTrace(); // 실행시점에선 삭제 해야하지만 개발 시점에선 에러 추적을 위해 사용
				}
				finally {
					try {
							// 실행문 하나일때 중괄호 생략 가능
					if(br != null) br.close(); //null이 아닐 경우 생성의 역순으로 종료
					if(fr != null) fr.close(); //null이 아닐 경우 생성의 역순으로 종료
					
					}catch(IOException e){
						
					}
				}
			}
		});
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
		b1.addActionListener(e-> {
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

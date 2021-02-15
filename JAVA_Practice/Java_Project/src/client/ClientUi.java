package client;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

	
public class ClientUi {
	TextArea ta;
	TextField tf;
	DataOutputStream out;
	DataInputStream in;
	String chatId;
	
	class ClientTread extends Thread{
		@Override
		public void run() {
			while(true) {
				try {
					ta.append(in.readUTF()+"\n");
				}catch (IOException e) {
					e.printStackTrace();
				}
			}
		
		}
	}
	
	public void chatMsg() {
		String msg=tf.getText();
		try {
			out.writeUTF(chatId + msg);
		} catch (IOException e) {
			e.printStackTrace();
		} //서버로 메시지 전송
		tf.setText("");
		}
	
	public void onCreate() {
		Frame f = new Frame("나의 채팅");
		Panel p=new Panel();	
		Button b1 = new Button("전송");
		
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
		
	save_item.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent ae) {
				FileDialog save=new FileDialog(f, "저장 창", FileDialog.SAVE);
				save.setVisible(true);
				
				FileWriter fw=null;
				try {
					fw=new FileWriter(save.getDirectory()+save.getFile());
					fw.write(ta.getText());
					
					
				} catch (IOException e) {
					e.printStackTrace();
				}finally {
					 try {
						 if(fw !=null ) fw.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}		
				
			}
		});
		
		
		open_item.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent a) {
				System.out.println("open file");
				FileDialog open=new FileDialog(f, "열기 창", FileDialog.LOAD); // 열기 창 설정
				open.setVisible(true); //컨테이너 계열들은 보여주는 설정이 필요.

				FileReader fr = null;
				BufferedReader br = null;
				String path_open = open.getDirectory()+open.getFile();
				
				try {
					fr = new FileReader(path_open); //선택하는 파일의 경로 획득
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
		
		// 익명 클래스 사용
		f.addWindowListener(new WindowAdapter(){	// 부모 클래스를 매개변수로 넣음

			@Override
			public void windowClosing(WindowEvent e) {
				System.out.println("windowClosing");
				System.exit(0);
			}
		});		
		
		b1.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				//채팅서버 연결
				chatId="[" + tf.getText() + "]";
				ta.setText(chatId + "님 채팅을 시작합니다.");
				try {
					Socket s=new Socket("localhost", 9999);
					out=new DataOutputStream(s.getOutputStream());
					in=new DataInputStream(s.getInputStream());
					ClientTread t = new ClientTread();
					t.start();
					
					ta.append("연결 ok\n");
					tf.setText("");

				} catch (UnknownHostException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		
		
		tf.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				//하고자 하는 일
				chatMsg();
			}
		});
		
		f.add(ta, BorderLayout.CENTER);
		f.add(p, BorderLayout.SOUTH);
		p.add(tf);
		p.add(b1);
		p.setBackground(Color.gray);
		
//		Color bgColor = new Color(123,24,56);
		f.setLocation(800, 200);
		f.setSize(400,500);
		f.setVisible(true);
	}
}

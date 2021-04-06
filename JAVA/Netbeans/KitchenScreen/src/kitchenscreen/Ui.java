/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package kitchenscreen;

import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.DefaultTableModel;
import com.mulcam.ai.web.vo.OrderVO;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;

/**
 *
 * @author javanism
 */
public class Ui extends javax.swing.JFrame {
    ObjectOutputStream out;
    String columnNames[];
    JComboBox comboBox;
    JFrame qrFrame;
    JLabel qr;
    Object order_group_no;

    /**
     * Creates new form Ui
     */
    public Ui() {
        initComponents();
        serverConnect();
        columnNames=new String[]{"order_group_no","prodname","quantity","ordermethod","orderdate","상황"};
  
        qrFrame=new JFrame();
        qr=new JLabel();
        qrFrame.getContentPane().add(qr);
        qrFrame.setLocation(300,400);
        
        tableEvent();
       
    }

    private void tableEvent(){

        jTable1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionModel data = jTable1.getSelectionModel();
        data.addListSelectionListener((e)-> {
            if(e.getValueIsAdjusting()){
                try{
                    order_group_no=jTable1.getModel().getValueAt(jTable1.getSelectedRow(),0);
                    System.out.println(order_group_no);
                }catch(Exception ex){
            }
        }
        });
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jButton1 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 386, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(18, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 233, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(24, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("주문/출고 현황", jPanel1);

        jButton1.setText("새로고침");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(43, 43, 43)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 421, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton1)
                .addContainerGap(19, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(34, 34, 34)
                        .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 296, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(82, 82, 82)
                        .addComponent(jButton1)))
                .addContainerGap(51, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        try {
            // 새로고침 버튼 처리
            out.writeObject("ordersSelect");
            System.out.println("새로고침요청");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }//GEN-LAST:event_jButton1ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables

        private void serverConnect() {
        try {
            Properties prop=new Properties();
            prop.load(new FileReader("config.properties"));
            String ip=(String) prop.get("server.ip");
            String port=(String) prop.get("server.port");
            System.out.println(ip+":"+port);
            Socket s=new Socket(ip,Integer.parseInt(port));
            out=new ObjectOutputStream(s.getOutputStream());
            ObjectInputStream in=new ObjectInputStream(s.getInputStream());
            new Thread(()->{
                while(true){
                    try {
                        List<OrderVO> list=(List<OrderVO>) in.readObject();
                        System.out.println(list);
                        Object [][]data=new Object[list.size()][6];
                        int i=0;
                        for(OrderVO vo:list){
                            System.out.println(i);
                            data[i][0]=vo.getOrder_group_no();
                            System.out.println(data[i][0]);
                            data[i][1]=vo.getProduct_name();
                            data[i][2]=vo.getQuantity();
                            data[i][3]=vo.getOrdermethod();
                            data[i][4]=vo.getOrderdate();
                            data[i++][5]="준비";                           
                        }
                       // "order_group_no","prodname","quantity","ordermethod","orderdate","memberid"
                        jTable1.setModel(new DefaultTableModel(data, columnNames));
                        comboBox = new JComboBox(); 
                        comboBox.addItem("준비"); 
                        comboBox.addItem("완료"); 
                        TableColumn col=jTable1.getColumn("상황");
                        col.setCellEditor(new DefaultCellEditor(comboBox));
                        jTable1.repaint();
                        comboBoxEvent();

                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } catch (ClassNotFoundException ex) {
                        ex.printStackTrace();
                    }catch(Exception e){
                        e.printStackTrace();
                    }

                }
            }).start();
            System.out.println("서버 연결 완료");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void comboBoxEvent() {
        System.out.println("comboBoxEvent() 호출됨");
        comboBox.addActionListener((e)->{
            String status = comboBox.getSelectedItem().toString();
            if(status.equals("완료")){
                try {
                    System.out.println("QR생성");
                    URL url = new URL("http://api.qrserver.com/v1/create-qr-code/?size=150x150&data=http://192.168.0.30:8090/output.chr?order_group_no="+order_group_no);
                     HttpURLConnection con = (HttpURLConnection) url.openConnection();
                     con.setRequestMethod("GET");
                     
                        // QR받기/응답 내용(BODY) 구하기        
                    try (InputStream in = con.getInputStream();
                            ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                            byte[] buf = new byte[1024 * 8];
                            int length = 0;
                            while ((length = in.read(buf)) != -1) {
                              
                            }                            
                            
                            qr.setIcon(new ImageIcon(buf));
                            qrFrame.pack();
                            qrFrame.setVisible(true);
                                      
                    }
        
                    // 접속 해제
                    con.disconnect();

                     
                     
                     
                } catch (Exception ex) {
                    Logger.getLogger(Ui.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        
    }
}



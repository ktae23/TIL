package my.jes.ai;

import java.awt.*;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

public class MainFrame extends JFrame {
    JPanel contentPane;
    JLabel imageLabel = new JLabel();
    JLabel headerLabel = new JLabel();

    public MainFrame() {
        try {
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            contentPane = (JPanel) getContentPane();
            contentPane.setLayout(new BorderLayout());
            setSize(new Dimension(400, 300));
            setTitle("런닝맨");
           
            headerLabel.setFont(new java.awt.Font("고딕", Font.BOLD, 16));
            headerLabel.setText("열심히 달리는 런닝맨");
            contentPane.add(headerLabel, java.awt.BorderLayout.NORTH);
            
            ImageIcon ii = new ImageIcon("img\\sound2.gif");
            //Image img=ii.getImage();
            //Image changeImg=img.getScaledInstance(53,60,Image.SCALE_SMOOTH);
            imageLabel.setIcon(ii);
            //imageLabel.setIcon(ii);
            contentPane.add(imageLabel, java.awt.BorderLayout.CENTER);
            
            imageLabel.addMouseListener(new MouseAdapter(){
                @Override
                public void mouseClicked(MouseEvent e) {
                   int selectNo=JOptionPane.showConfirmDialog(MainFrame.this, "hiu");
                    System.out.println(selectNo);
                }
                
            });
            this.setLocationRelativeTo(null);
            this.setVisible(true);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new MainFrame();
    }

}
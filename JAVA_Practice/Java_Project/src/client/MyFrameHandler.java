package client;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class MyFrameHandler extends WindowAdapter{

	public void windowOpened(WindowEvent e) {
		System.out.println("windowOpened");
	}

	public void windowClosing(WindowEvent e) {
		System.out.println("windowClosing");
		System.exit(0);
	}

	public void windowClosed(WindowEvent e) {
		System.out.println("windowClosed");
	}

	public void windowIconified(WindowEvent e) {
		System.out.println("windowIconified");
	}

	public void windowDeiconified(WindowEvent e) {
		System.out.println("windowDeiconified");
	}

	public void windowActivated(WindowEvent e) {
		System.out.println("windowActivated");
	}

	public void windowDeactivated(WindowEvent e) {
		System.out.println("windowDeactivated");		
	}

}
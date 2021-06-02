package design.observer;

public class Button {

	private String name;
	private IButtonListener buttonListener;
	
	public Button(String name) {
		this.name = name;
	}

	
	public void click(String message) {
		buttonListener.clickEvent(message);
	}
	
	public void addListener(IButtonListener iButtonListener) {
		this.buttonListener = iButtonListener;
	}
	
}

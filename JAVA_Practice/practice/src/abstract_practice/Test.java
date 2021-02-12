package abstract_practice;

public class Test {

	public static void main(String[] args) {
		DeskTop dt = new DeskTop();
		NoteBook nb = new NoteBook();
		
		dt.display();
		dt.typing();
		nb.display();
		nb.typing();
		
		System.out.println("=======================");
		
		dt.turnOn();
		dt.turnOff();
		
		nb.turnOn();
		nb.turnOff();
	}
}

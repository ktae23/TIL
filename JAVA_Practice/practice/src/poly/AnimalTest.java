package poly;

public class AnimalTest {

	public static void main(String[] args) {
		AnimalTest aTest = new AnimalTest();
		
		aTest.moveAnimal(new Human());
		aTest.moveAnimal(new Tiger());
		aTest.moveAnimal(new Eagle());
		
	}
	
	public void moveAnimal(Animal animal) {
		animal.move();
	}
}

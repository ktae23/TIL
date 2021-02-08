package abstract_method;

public class AnimalExample {
	public static void main(String[] args) {
		Dog dog = new Dog();
		Cat cat = new Cat();
		dog.sound();
		cat.sound();
		dog.breathe();
		cat.breathe();
		System.out.println("-----");
		
		//변수의 자동 타입 변환
		Animal animal = null;
		animal = new Dog();
		animal.sound();
		animal.breathe();
		animal = new Cat();
		animal.sound();
		animal.breathe();
		System.out.println("-----");
		
		//매개변수의 자동 타입 변환
		animalSound(new Dog());
		animalSound(new Cat());
		animalBreathe(new Dog());
		animalBreathe(new Cat());
	}
	
	public static void animalSound(Animal animal) {
		animal.sound();
	}
	public static void animalBreathe(Animal animal) {
		animal.breathe();
	}
}
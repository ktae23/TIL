package example5;

public class NestedClassExample {
	public static void main(String[] args) {
		Car myCar = new Car();
		
		Car.Tire tire = myCar.new Tire();
		// Tire가 내부 인스턴스 클래스기 때문에 인스턴스를 참조해서 생성
				
		Car.Engin engine = new Car.Engin();
		// Engine은 내부 정적 클래스기 때문에 바로 생성
		
		
	}

}

package test.Shape;

public class Circle extends Shape { 

	int radius = 3;
	public void area() {
		System.out.println("원의 넓이=" + (3.14*radius*radius));
	}
}


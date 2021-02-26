package clone_practice;

public class Circle implements Cloneable{
	Point point;
	int radius;
	
	Circle(int x, int y, int radius){
		this.radius = radius;
		point = new Point(x,y);
	}
	
	public String toString() {
		return "원점은" + point + "이고," + "반지름은 " + radius + "입니다.";
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException{
		return super.clone();
	}

}

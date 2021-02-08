package my.fly;

public class Test {

	public static void main(String[] args) {
		Bird b=new Bird();
		Superman s=new Superman();
		Airplane a=new Airplane();
		
		Show show=new Show();
		show.airShow(b);
		show.airShow(s);
		show.airShow(a);
		show.airShow("java"); // 처음부터 이런 값이 들어가지 않도록 작성하는 것이 좋은 프로그래밍
				
	}
}

package multiple_choice;

public class Doubleplus_Operator_Test {

	public static void mai(String[] args) {

		int total=0,x=0,y;
		while(x++<5) { // 여기서 4일때
			y=x*x;	   // 여기서는 5가 되므로 y는 25
			System.out.println(y);
			total +=y;  // y = {1,4,9,16,25}
		}
		System.out.println("총합은" + total);
		
	}

}

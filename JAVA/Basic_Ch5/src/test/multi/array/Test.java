package test.multi.array;

public class Test {

	public static void main(String[] args) {
		int[][] all = new int[3][2];
		System.out.println(all); //[[I@139a55
		System.out.println(all[0]); //[I@1db9742
		System.out.println(all[1]); //[I@106d69c
		System.out.println(all[2]); //[I@52e922
		
		all[0][0]=100;
		System.out.println(all[0][0]); //100
		System.out.println(all[2][1]); //0
		
		all[2][1]=1000;
		System.out.println(all[2][1]); //1000		
		System.out.println(all.length); //3
		System.out.println(all[0].length); //2
		
		
		char[][] all2 = {{'a','b'},{'c','d','e'}}; //new char[2][]
		
	}
}






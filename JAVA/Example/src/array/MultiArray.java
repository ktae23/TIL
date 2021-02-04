package array;

public class MultiArray {

	public static void main(String[] args) {
		int[][] multiArray = new int[2][];
		multiArray[0] = new int[2];
		multiArray[1] = new int[4];
		
		multiArray[0][0] = 1;
		multiArray[0][1] = 2;
		
		multiArray[1][0] = 3;		
		multiArray[1][1] = 4;
		multiArray[1][2] = 5;
		multiArray[1][3] = 6;

		for(int i=0; i<multiArray.length;i++) {
			for(int j=0; j<multiArray[i].length;j++) {
					System.out.println(multiArray[i][j]);
			}
			System.out.println("");
			
		}
	}
}

package test.multi.array;

public class ArrayCopyExample {

	public static void main(String[] args) {
		String[] oldStrsArray = {"java", "array", "copy"};
		String[] newStrArray = new String[5];
		
		System.arraycopy(oldStrsArray, 0, newStrArray, 0, oldStrsArray.length);
		
		for(int i=0; i<newStrArray.length; i++) {
			System.out.println(newStrArray[i] + ",");
		}
	}
	
}

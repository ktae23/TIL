package array;

public class ArrayCopy {
	
	public static void main(String[] args) {
		int[] arrayOrigin = {10,20,30,40,50};
		int[] arrayCopy = {1,2,3,4,5};
		
//		System.arraycopy(src, srcPos, dest, destPos, length);
		System.arraycopy(arrayOrigin, 0, arrayCopy, 1, 4);
						// arrayOrigin[0]자리부터 복사해서 arrayCopy[1]자리부터 붙여넣는데 4개만 옮긴다.
		
		for(int i=0; i<arrayCopy.length;i++) {
			System.out.println(arrayCopy[i]);
		}
	}

}

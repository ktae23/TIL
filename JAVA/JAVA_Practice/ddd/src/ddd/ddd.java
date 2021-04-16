package ddd;

import java.util.Scanner;

public class ddd {
	public static void main(String[] args) {

			
				
			  int[] no = {9,12,21,25,33,42};
		      
			  Scanner sc = new Scanner(System.in);
			  int[] arr = new int[6];
			  int k=0;
			  
			  for(int i =0; i<6;i++) {
			     System.out.println("번호는?");
			     int r = sc.nextInt();
			     arr[i]=r;
			  }
		      
		      for(int i =0; i<6;i++) {
		    	  for(int j=0; j<6; j++) {
			         if(arr[j]==no[i]) {
			            k++;
			         }
		    	  }
			  }
			      System.out.println(k);
	}
	

}

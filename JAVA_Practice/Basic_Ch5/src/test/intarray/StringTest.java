package test.intarray;

public class StringTest {

	public static void main(String[] args) {
		String[] all=new String[5];
		System.out.println(all);
		System.out.println(all[0]);
		String s1=new String("java");
		all[0]=s1;
		all[1]=new String("java");
		all[2]="java";
		all[3]="java";
		for(int i=0;i<all.length;i++) {
			System.out.println(all[i]);
		}
		
	}
}

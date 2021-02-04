package test.intarray;

public class MemberTest {

	public static void main(String[] args){
//		System.out.println(args);
//		System.out.println(args.length);
	
		MemberDB db = new MemberDB();
		
		for(int i=0; i<args.length; i++) {
			System.out.println(args[i]);		
		}
		db.printNames();

	
	}
}

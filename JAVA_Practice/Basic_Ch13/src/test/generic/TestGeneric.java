package test.generic;

import java.util.ArrayList;
import java.util.List;

public class TestGeneric {

	public static void main(String[] args) {
		List<String> list = new ArrayList<>();
		list.add("Hi");
		
		String[] array = new String[10];
		array[0]="Hi"; 
		
		int[] all = new int[10];
		all[0]=1;
		
		List list2=new ArrayList();
		Integer o1 = new Integer(1);
		Integer o2 = new Integer(2);
		list2.add(o1);
		list2.add(o2);
		
		list2.add(1); //Java5부터 Auto-Boxing으로 가능해짐. - 사용 권장하지 않음
//		int i = list2.get(2); //Java5부터 가능 Un-Boxint. - 사용 권장하지 않음
		
	}
}

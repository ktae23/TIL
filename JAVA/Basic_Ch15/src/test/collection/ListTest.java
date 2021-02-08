package test.collection;

import java.util.ArrayList;

public class ListTest {

	public static void main(String[] args) {
		ArrayList list=new ArrayList();
		list.add("1");
		list.add("2");
		list.add("3");
		list.add("1");
//		System.out.println(list);
//		System.out.println(list.size());
		for(int i=0;i<list.size();i++) {
			Object o=list.get(i);
			if(o instanceof String) {
				String s=(String)o;
				System.out.println(s);
			}
		}

	}

}

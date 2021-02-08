package test.collection;

import java.util.HashSet;
import java.util.Iterator;

public class SetTest {

	public static void main(String[] args) {
		HashSet<String> set=new HashSet<String>();
		set.add("1");
		set.add("2");
		set.add("3");
//		set.add(new SetTest());
//		set.add(new SetTest());
		set.add("1");
		System.out.println(set);
		System.out.println(set.size());
		System.out.println("___________________");
		Iterator<String> itr = set.iterator();
		System.out.println(itr);
		System.out.println("___________________");
		while(itr.hasNext()) {
			Object o=itr.next();
			System.out.println(o);
		}
	}
}

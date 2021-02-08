package test.collection;

import java.util.ArrayList;

public class ListTest {

	public static void main(String[] args) {
		ArrayList<String> list=new ArrayList<String>();
		list.add("1");
		list.add("2");
		list.add("3");
		list.add("1");
		
		for(String s:list) {			
			System.out.println(s);			
		}
	}

}

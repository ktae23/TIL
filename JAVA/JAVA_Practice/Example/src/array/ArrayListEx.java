package array;

import java.util.ArrayList;

public class ArrayListEx {
	public static void main(String[] args) {
		ArrayList<Book> library = new ArrayList<Book>();
		
		library.add(new Book("태백산맥", "조정래"));
		library.add(new Book("데미안", "헤르만 헤세"));
		library.add(new Book("어떻게 살 것인가", "유시민"));
		library.add(new Book("토지", "박경리"));
		library.add(new Book("어린왕자", "생텍쥐페리"));

		for(int i=0; i<library.size();i++) {
			Book book = library.get(i);
			book.showBookInfo();
			}
		
		System.out.println( );
		
		library.get(2).showBookInfo();		
		
		library.remove(2).showBookInfo();
		
		System.out.println("===요소 삭제 후===");
		
		for(int i=0; i<library.size();i++) {
			Book book = library.get(i);
			book.showBookInfo();
			}
		System.out.println(library.isEmpty());

	
	}
}


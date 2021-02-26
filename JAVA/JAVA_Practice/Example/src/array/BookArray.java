package array;


public class BookArray {

	public static void main(String[] args) {
		
		Book[] library = new Book[3];
		Book[] library2 = new Book[3];
		
		library[0] = new Book("태백산맥", "조정래");
		library[1] = new Book("데미안", "헤르만 헤세");
		library[2] = new Book("어떻게 살 것인가", "유시민");

		library2[0] = new Book();
		library2[1] = new Book();
		library2[2] = new Book();	//디폴트 생성자로 배열 인스턴스 생성
		
		
		
		for(int i=0; i<library.length;i++) {	//get,set 메소드를 이용하여 인스턴스 복사
			library2[i].setBookName(library[i].getBookName());
			library2[i].setAuthor(library[i].getAuthor());
			
		}
		for(int i=0; i<library.length;i++) {
			library2[i].showBookInfo();
		}
		
		library2[0].setBookName("나목");
		library2[0].setAuthor("박완서");
		
		System.out.println("=== library ===");
		for(int i=0; i<library.length;i++) {
			library[i].showBookInfo();
			System.out.println(library[i]);
		}
		System.out.println("=== library2 ===");
		for(int i=0; i<library2.length;i++) {
			library2[i].showBookInfo();
			System.out.println(library2[i]);
		}	
	}
}


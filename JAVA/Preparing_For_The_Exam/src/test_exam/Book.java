package test_exam;

public class Book {
	String isbn;
	String title;
	String author;
	String publisher;
	String desc;

	int price;
	
	public Book () {}
	

	public Book (String isbn, String title, String author, String publisher, int price) {
		this.isbn=isbn;
		this.title=title;
		this.author=author;
		this.publisher=publisher;
		this.desc="";
		this.price=price;
	}

	
	
	
	
	public Book (String isbn, String title, String author, String publisher, int price, String desc) {
		this.isbn=isbn;
		this.title=title;
		this.author=author;
		this.publisher=publisher;
		this.desc=desc;
		this.price=price;
	}


	@Override
	public String toString() {
		
		return isbn + "   |   " + title + "   | " + author + "               | " + publisher + "   |   "
				+ price +" " + desc;
	}
	
	

}

package Test_exam_2;

public class Magazine {
	String isbn;
	String title;
	String author;
	String publisher;
	String desc;

	int price;
	int year;
	int month;
	
	public Magazine() {}
	
	public Magazine(String isbn, String title, String author, String publisher, 
					int price, int year, int month) {
		this.isbn=isbn;
		this.title=title;
		this.author=author;
		this.publisher=publisher;
		this.desc="";
		this.price=price;
		this.year=year;
		this.month=month;
	}

	
	public Magazine(String isbn, String title, String author, String publisher, 
					String desc, int price, int year, int month) {
		this.isbn=isbn;
		this.title=title;
		this.author=author;
		this.publisher=publisher;
		this.desc=desc;
		this.price=price;
		this.year=year;
		this.month=month;
	}


	@Override
	public String toString() {
		if (isbn == null) {
			isbn = " ";
		}else if(title == null) {
			title = " ";
		}else if(author == null) {
			author = " ";
		}else if(publisher == null) {
			publisher = " ";
		}else if(desc == null) {
			desc = " ";
		}
		return isbn + "   |   " + title + "   " + author + "  |  " + publisher + "  |  "
				+ price +"      " + desc + "  |  " + year +"."+month;
	}
	
	
}
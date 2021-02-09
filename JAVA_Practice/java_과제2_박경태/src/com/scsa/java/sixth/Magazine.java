package com.scsa.java.sixth;

public class Magazine {
	private String isbn;
	private String title;
	private String author;
	private String publisher;
	private String desc;

	private int price;
	private int year;
	private int month;
	
	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		if (isbn == null) {
			isbn ="";
		}else {
			this.isbn = isbn;	
		}
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		if (title == null) {
			title ="";
		}else {
			this.title = title;	
		}
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		if (author == null) {
			author ="";
		}else {
			this.author = author;	
		}
	}

	public String getPublisher() {
		return publisher;
	}

	public void setPublisher(String publisher) {
		if (publisher == null) {
			publisher ="";
		}else {
			this.publisher = publisher;	
		}
	}
	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		if (desc == null) {
			desc ="";
		}else {
			this.desc = desc;	
		}
	}

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		if(price<0) {
			price = 0;
		}else {
			this.price = price;
		}
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		if(year<0) {
			year = 0;
		}else {
			this.year = year;
		}
	}

	public int getMonth() {
		return month;
	}

	public void setMonth(int month) {
		if(month<0) {
			month = 0;
		}else {
			this.month = month;
		}
	}

	public Magazine() {}
	
	
	public Magazine(String isbn, String title, String author, String publisher, 
			 int price, String desc, int year, int month) {
		this.isbn=isbn;
		this.title=title;
		this.author=author;
		this.publisher=publisher;
		this.desc=desc;
		this.price=price;
		this.year=year;
		this.month=month;
	}
	
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
	
	@Override
	public String toString() {
		return isbn + "\t| " + title + "\t| " + author + "\t| " + publisher + "\t| " + price + "\t| " + desc + "\t| " + year +"."+ month;
	}
}

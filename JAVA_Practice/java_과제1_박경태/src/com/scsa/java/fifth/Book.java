package com.scsa.java.fifth;

public class Book {
	String isbn;
	String title;
	String author;
	String publisher;
	String desc;
	int price;
	

	@Override
	public String toString() {
		if (desc == null) {
			desc = "";
		}
		return isbn + "\t| " + title + "\t| " + author + "\t| " + publisher + "\t| " + price + "\t| " + desc;
	}
}

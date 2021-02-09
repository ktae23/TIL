package com.scsa.java.fifth;

public class Magazine {
	String isbn;
	String title;
	String author;
	String publisher;
	String desc;
	int price;
	int year;
	int month;

	@Override
	public String toString() {
		if(desc == null) {
			desc = "";
		}
			return isbn + "\t| " + title + "\t| " + author + "\t| " + publisher + "\t| " + price + "\t| " + desc + "\t| " + year +"."+ month;
		
	}
}

package com.scsa.java.sixth;

public class BookTest {

	public static void main(String[] args) {
		Book b1=new Book("21424", "Java Pro", "김하나", "Jaen.kr", 15000, "기본문법");
		Book b2=new Book("33455", "JDBC Pro", "김철수", "Jaen.kr", 23000);
		Book b3=new Book("55355", "Servlet/JSP", "박자바", "Jaen.kr", 41000, "Model12 기반");
		Book b4=new Book("35332", "Android App", "홍길동", "Jaen.kr", 25000, "Lightweight Framework");
		Book b5=new Book("35355", "OOAD 분석,설계", "소나무", "Jaen.kr", 30000);
		
		Magazine m1=new Magazine("35535", "Java World", "편집부", "Jaen.kr", 7000, 2013, 2);
		Magazine m2=new Magazine("33434", "Mobile World", "편집부", "Jaen.kr", 8000, 2013, 2);
		Magazine m3=new Magazine("75342", "Next Web", "편집부", "Jaen.kr", 10000, "AJAX 소개", 2013, 2);
		Magazine m4=new Magazine("76543", "Architecture", "편집부", "Jaen.kr", 5000, "java 시스템", 2013, 2);
		Magazine m5=new Magazine("76534", "Data Modeling", "편집부", "Jaen.krm", 14000, 2013, 2);

		System.out.println("**************************도서 목록**************************");
		System.out.println(b1);
		System.out.println(b2);
		System.out.println(b3);
		System.out.println(b4);
		System.out.println(b5);
		

		System.out.println();
		System.out.println("**************************잡지 목록**************************");
		System.out.println(m1);
		System.out.println(m2);	
		System.out.println(m3);	
		System.out.println(m4);	
		System.out.println(m5);	
		
	
	}
}

package test_exam;

public class BookTest {

	public static void main(String[] args) {
		Book b1=new Book("21424", "Java Pro", "김하나", "Jaen.kr", 15000, "기본문법");
		Book b2=new Book("35355", "OOAD 분석,설계            ", "소나무", "Jaen.kr", 30000);
		Magazine m1=new Magazine("35535", "Java World", "편집부", "androidjava.com", 7000, 2013, 2);
		Magazine m2=new Magazine();
		m2.isbn = "22334";
		m2.title = "이것도 자바다";
		m2.author = "박경태";
		m2.publisher = "우리집";
		m2.price = 20000;
		m2.year = 2021;
		m2.month = 2;
		m2.desc = "부연설명";
		
		
		System.out.println("********************도서목록********************");
		System.out.println(b1.toString());
		System.out.println(b2.toString());
		System.out.println(m1.toString());
		System.out.println(m2.toString());
	}
}

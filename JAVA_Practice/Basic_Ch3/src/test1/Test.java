package test1;

public class Test {

	public static void main(String[] args) {
		MyProfile m1 = new MyProfile();
		m1.setProfile("김유신", 15, 177.4, '남', true); //호출
		System.out.println(m1);
		m1.printProfile();
	
		
		MyProfile m2 = new MyProfile();
		m2.setProfile("홍길동", 28, 175.3, '남', true); //호출
		System.out.println(m2);
		System.out.println(m2.name);
		System.out.println(m2.age);
		System.out.println(m2.tall);
		System.out.println(m2.gender);
		System.out.println(m2.isPretty);
	}
}

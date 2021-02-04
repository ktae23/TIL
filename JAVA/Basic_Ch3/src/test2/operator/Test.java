package test2.operator;

public class Test {

	public static void main(String[] args) {
	/*  int a=1;
		int b=2;
		System.out.println(a+b); //출력만하고 재사용 안한다면 c=a+b;처럼 메모리 할당 x
	*/
	
	    Calculator c = new Calculator();
		Calculator c2 = new Calculator();
		c.add(1,2);
		c.mod(1,2);
		c.sum(1);
		c.sum(1);
		c.sum(1);
		
		int i1=100;
		int i2=100;
		System.out.println(++i1);
		System.out.println(i1--);
		System.out.println(i2++);
		System.out.println(--i2);
		
		boolean b1=true;
		boolean b2=true;
		System.out.println(!((i1==i2) || (b1==b2)));
		System.out.println(!((i1==i2) || (c==c2)));
		
	/*	
		byte b=1;
		System.out.println(b);
		
		String s=Integer.toBinaryString(b);
		System.out.println(s);
		
		byte b1=-1;
		System.out.println(b1);
		
		String s1=Integer.toBinaryString(b1);
		System.out.println(s1); //Integer클래스 때문에 4byte의 32자리 수가 나옴
		
		String s2=Integer.toBinaryString(~b1); // b1의 보수
		System.out.println(s2); // 자바에서 -1의 보수는 0	
	*/
		/*int b=1;
		
		System.out.println(Integer.toBinaryString(b)); // 10진수 1
		b=b<<1; // 왼쪽으로 1칸 비트 이동
		System.out.println(b); // 10진수 2
		System.out.println(Integer.toBinaryString(b)); //2진수 10

		b=b>>1; // 오른쪽으로 1칸 비트 이동
		System.out.println(b); // 10진수 1
		System.out.println(Integer.toBinaryString(b)); // 2진수 1
		
		b=-1;
		b=b>>1; // 오른쪽으로 1칸 비트 이동 (부호 유지)
		System.out.println(b); // 10진수 -1
		System.out.println(Integer.toBinaryString(b)); // 2진수 111111111111111111111111111111111
	
		
		b=-1;
		b=b>>>1; // 오른쪽으로 1칸 비트 이동(부호 포함 이동)(빈자리 무조건 0으로 채워짐)
		System.out.println(b); // 10진수 2147483647
		System.out.println(Integer.toBinaryString(b)); // 2진수 1 1111111111111111111111111111111 (맨 앞 0 생략)
*/		
		int score = 95;
		char grade;
		if(score>90) {
			grade='A';
		}
		else {
			grade='B';
		}
		System.out.println(grade);
		// 위의 식을 삼항 연산자로 표현하면 아래 식과 같다.

		grade = (score>90)? 'A' : 'B';
		System.out.println(grade);
		
	}
}

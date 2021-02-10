***배열의 복사**



- 배열의 크기는 초기화시 고정되기 때문에 중간에 수정 할 수 없다.

**복사를 하는 이유**

1. 기존 배열과 자료형 및 배열 크기가 똑같은 배열을 새로 만들 때
2. 배열의 모든 요소에 자료가 꽉 차서 더 큰 배열을 만들어 기존 배열에 저장 된 자료를 가져오려 할 때

**복사 하는 방법**

1. 기존 배열과 배열 길이가 같거나 더 긴 배열을 만들고 for문을 사용하여 각 요소를 반복하여 복사하는 방법
2. System.arraycopy() 메서드를 사용하는 방법

- **1. 반복 복사**

```java
package array;


public class BookArray {

	public static void main(String[] args) {
		
		Book[] library = new Book[5];
		library[0] = new Book("태백산맥", "조정래");
		library[1] = new Book("데미안", "헤르만 헤세");
		library[2] = new Book("어떻게 살 것인가", "유시민");
		library[3] = new Book("토지", "박경리");
		library[4] = new Book("어린왕자", "생텍쥐페리");
		
		
		Book[] copyArray = new Book[7]; //더 큰 배열 생성
		
		for(int i=0;i<library.length;i++ ) {	// for문 이용하여 일일이 대입
			copyArray[i] = library[i];
		}
		copyArray[5] = new Book("이것이 자바다", "신용권");
		copyArray[6] = new Book("Do it 자바 프로그래밍 입문", "박은종");
		
		for(int i=0; i<copyArray.length;i++) {
					copyArray[i].showBookInfo();
		}
		for(int i=0;i<copyArray.length;i++ ) {
			System.out.println(copyArray[i]);
		}
		
	}
}
```

**태백산맥,조정래**
**데미안,헤르만 헤세**
**어떻게 살 것인가,유시민**
**토지,박경리**
**어린왕자,생텍쥐페리**
**이것이 자바다,신용권**
**Do it 자바 프로그래밍 입문,박은종**

// 위 일곱개는 Book 인스턴스 멤버들



**array.Book@139a55**

**array.Book@1db9742**

**array.Book@106d69c**

**array.Book@52e922**

**array.Book@25154f**

**array.Book@10dea4e**

**array.Book@647e05**

// 위 일곱개는 Book 인스턴스를 저장한 메모리 공간 주소



이때 윗쪽의 예제를 보면 기존 다섯개의 인스턴스 메모리 주소와 복사해온 값의 주소가 동일한 것을 알 수 있다.

이는 아래에 나올 '얕은 복사', '깊은 복사'에서 설명 함.



- **2. 메서드 복사**

```java
package array;

public class ArrayCopy {
	
	public static void main(String[] args) {
		int[] arrayOrigin = {10,20,30,40,50};
		int[] arrayCopy = {1,2,3,4,5};
		
//		System.arraycopy(src, srcPos, dest, destPos, length);
		System.arraycopy(arrayOrigin, 0, arrayCopy, 1, 4);
	// arrayOrigin[0]자리부터 복사해서 arrayCopy[1]자리부터 붙여넣는데 4개만 옮긴다.
		
		for(int i=0; i<arrayCopy.length;i++) {
			System.out.println(arrayCopy[i]);
		}
	}

}
```

**1**

**10**

**20**

**30**

**40**



**arrayCopy 배열의 인덱서[1]자리부터 4개의 값이 10, 20, 30, 40의 복사 값으로 변경 되었다.**

| 매개변수 | 설명                                                 |
| -------- | ---------------------------------------------------- |
| src      | 복사할 배열 이름                                     |
| scrPos   | 복사할 배열의 첫 번째 위치                           |
| dest     | 복사해서 붙여 넣을 대상 배열 이름                    |
| destPos  | 복사해서 대상 배열에 붙여 넣기를 시작할 첫 번째 위치 |
| length   | src에서 dest로 자료를 복사할 요소 개수               |



------

**얕은 복사**



```java
package array;


public class BookArray {

	public static void main(String[] args) {
		
		Book[] library = new Book[3];
		Book[] library2 = new Book[3];
		
		library[0] = new Book("태백산맥", "조정래");
		library[1] = new Book("데미안", "헤르만 헤세");
		library[2] = new Book("어떻게 살 것인가", "유시민");

		System.arraycopy(library, 0, library2, 0, 3); // 메서드 복사
		
		for(int i=0; i<library.length;i++) {
			library[i].showBookInfo();
		}
		
		library2[0].setBookName("나목");	// library2의 0번째 인덱스 책 제목 값 변경
		library2[0].setAuthor("박완서");	// library2의 0번째 인덱스 저자 값 변경
		
		System.out.println("=== library ===");
		for(int i=0; i<library.length;i++) {
			library[i].showBookInfo();
		}
		System.out.println("=== library2 ===");
		for(int i=0; i<library2.length;i++) {
			library2[i].showBookInfo();
		}	
	}
}
```

**태백산맥,조정래**
**데미안,헤르만 헤세**
**어떻게 살 것인가,유시민**


**=== library ===**
**나목,박완서**
**데미안,헤르만 헤세**
**어떻게 살 것인가,유시민**


**=== library2 ===**
**나목,박완서**
**데미안,헤르만 헤세**
**어떻게 살 것인가,유시민**



- 위를 보면 library2의 0번째 인덱스 값만 변경 했는데 library의 0번째 인덱스도 같이 변경 된 것을 확인

```java
		System.out.println("=== library ===");
		for(int i=0; i<library.length;i++) {
			library[i].showBookInfo();
			System.out.println(library[i]);
		}
		System.out.println("=== library2 ===");
		for(int i=0; i<library2.length;i++) {
			library2[i].showBookInfo();
			System.out.println(library2[i]);
```

위처럼 인덱스 내의 주소 값도 같이 출력하면



**=== library ===**
**나목,박완서**

**array.Book@139a55**

**데미안,헤르만 헤세**

**array.Book@1db9742**

**어떻게 살 것인가,유시민**

**array.Book@106d69c**



**=== library2 ===**
**나목,박완서**

**array.Book@139a55**

**데미안,헤르만 헤세**

**array.Book@1db9742**

**어떻게 살 것인가,유시민**

**array.Book@106d69c**



- 복사한 두 값의 주소가 모두 같은걸 알 수 있다.
- 때문에 library2의 값을 바꾸면 같은 주소를 갖는 library의 값도 변경 된다.

**깊은 복사**

```java
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
			library[i].setBookName(library[i].getBookName());
			library[i].setAuthor(library[i].getAuthor());
			
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
```

**null,null**
**null,null**
**null,null**



**=== library ===**
**나목,박완서**

**array.Book@139a55**

**데미안,헤르만 헤세**

**array.Book@1db9742**

**어떻게 살 것인가,유시민**

**array.Book@106d69c**



**=== library2 ===**
**나목,박완서**

**array.Book@52e922**

**데미안,헤르만 헤세**

**array.Book@25154f**

**어떻게 살 것인가,유시민**

**array.Book@10dea4e**



- 이처럼 새로운 인스턴스를 생성하면 새로운 주소값을 할당 받는다.
- 때문에 library2의 값을 바꿔도 원본 배열에 영향을 주지 않는다.



------



**향상된 for문과 배열**



```java
package array;

public class EnthancedForLoop {

	public static void main(String[] args) {
		String[] strArray = {"Java", "Android", "C", "JavaScript", "Python"};
		
		for(String lang : strArray) { // String 타입 lang 변수에 strArray 요소를 각 대입
			System.out.println(lang);
		}
	}
}
```

**Java**
**Android**
**C**
**JavaScript**
**Python**
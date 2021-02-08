### *** 배열**

- 같은 타입의 자료가 연속으로 나열 된 자료 구조
- 배열이 메모리에서 실제 저장되는 물리적 위치와 인덱스 순서의 논리적 순서는 같다
- 초기화 할때 정한 크기는 변경 불가.
- 정해진 크기보다 많은 값을 입력해야하면 새로운 배열 생성 후 복사 및 기존 배열 삭제

------

- int형 요소가 10개인 배열 선언

```java
int[] intArray = new int[10];
```



**배열 선언과 초기화**



- 자료형[] 배열이름 = new 자료형[개수];
  - => int[] intArray = new int[10];
- 자료형 배열이름[] = new 자료형[개수];
  -  => int intArray[] = new int[10];

|             |          |          |          | 배열     | 길이      | (length) |          |          |         |
| ----------- | -------- | -------- | -------- | -------- | --------- | -------- | -------- | -------- | ------- |
|             |          |          |          |          | 10개      |          |          |          |         |
| 4byte       | 4byte    | 4byte    | 4byte    | 4byte    | 4byte     | 4byte    | 4byte    | 4byte    | 4byte   |
| <---------- | -------- | -------- | -------- | -------- | 총 40byte | -------- | -------- | -------- | ------> |

```java
int[] studentIDs = new int[] {101, 102, 103}; 
// 값을 직접 입력하기 때문에 개수 생략. 자동으로 개수만큼의 크기 할당

int[] studentIDs = new int[3] {101, 102, 103}; 
// 값을 직접 입력하는데 개수도 입력하면 컴파엘 에러 발생

int[] studentIDs = {101, 102, 103}; 
// 선언과 동시에 초기화 할때는 new int[] 부분 생략 가능

int[] studentIDs;
int[] studentIDs = new int[] {101, 102, 103}; 
// 자료형과 배열을 먼저 선언한 뒤에 초기화 할때는 new int[] 생략 불가
```



- 배열 설명 중 가장 직관적이고 귀여웠던 이미지.

![img](https://blog.kakaocdn.net/dn/be2Rxc/btqV35E9vwu/b6p2oXRthBmyybbxNQs5C0/img.png)(출처 - Do it! 자바 프로그래밍 입문, 박은종)

- int[] num = int[10];과 같이 크기가 10인 배열을 생성하면 물리적(메모리 실제 저장 위치) 위치와 논리적 위치가 실제로 같다.



**배열의 유효한 요소 값 출력하기**

```java
package array;

public class ArrayTest3{
	public static void main(String args) {
    double[] data = new double[5];
    int size = 0;
    
    data[0] = 10.0; size++;
    data[1] = 20.0; size++;
    data[2] = 30.0; size++;
    
    for(int i=0; i<size; i++){
    	System.out.println(data[i];
        }
    }
}
```

**10.0**

**20.0**

**30.0**



**또는 인덱스 값이 배열의 기본 값인지를 검사해 조건문을 걸 수도 있다.**



------



***\*객체 배열사용\****

```java
package array;

public class Book {
	private String bookName;
	private String author;
	
	
	public Book() {} // 디폴트 생성자
	public Book(String bookName, String author) { //책 이름과 저자를 매개변수로 받아서
		this.bookName = bookName;		//해당하는 참조 변수에 값으로 넣어 줌
		this.author = author;		//해당하는 참조 변수에 값으로 넣어 줌
		
	}
	public String getBookName() {
		return bookName;
	}
	public void setBookName(String bookName) {
		this.bookName = bookName;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	
	public void showBookInfo() {
		System.out.println(bookName + "," + author); //책 이름과 저자 출력 메서드
	}
	

}
package array;


public class BookArray {

	public static void main(String[] args) {
		
		Book[] library = new Book[5];
		library[0] = new Book("태백산맥", "조정래");
		library[1] = new Book("데미안", "헤르만 헤세");
		library[2] = new Book("어떻게 살 것인가", "유시민");
		library[3] = new Book("토지", "박경리");
		library[4] = new Book("어린왕자", "생텍쥐페리");
		
		for(int i=0; i<library.length;i++) {
			library[i].showBookInfo(); //책 이름과 저자 출력 메서드 반복
		}
		for(int i=0;i<library.length;i++ ) {
			System.out.println(library[i]);	//각 인덱스 위치에 저장하는 값 출력
		}
		
		
	}
}
```

**태백산맥,조정래**
**데미안,헤르만 헤세**
**어떻게 살 것인가,유시민**
**토지,박경리**
**어린왕자,생텍쥐페리**

// 위 다섯개는 Book 인스턴스 멤버들

**array.Book@139a55**

**array.Book@1db9742**

**array.Book@106d69c**

**array.Book@52e922**

**array.Book@25154f**

// 위 다섯개는 Book 인스턴스를 저장한 메모리 공간 주소





| 인덱스 위치          | library[0]            | library[1]             | library[2]                  | library[3]            | library[4]              |
| -------------------- | --------------------- | ---------------------- | --------------------------- | --------------------- | ----------------------- |
| **메모리 공간 주소** | **array.Book@139a55** | **array.Book@1db9742** | **array.Book@106d69c**      | **array.Book@52e922** | **array.Book@25154f**   |
| **저장 값**          | **태백산맥,조정래**   | **데미안,헤르만 헤세** | **어떻게 살 것인가,유시민** | **토지,박경리**       | **어린왕자,생텍쥐페리** |
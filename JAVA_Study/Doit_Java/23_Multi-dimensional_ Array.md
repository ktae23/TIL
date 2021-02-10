*** 다차원 배열**



- 이차원 이상으로 구현한 배열
- 평면이나 공간 개념 구현하는데 사용

**2차원 배열**



- 배열 선언 : 자료형[][] 배열 이름 = new 자료형[행 개수] [열 개수];

```
int[][] arr = new int[2][3];
```

| arr[0][0] | arr[0][1] | arr[0][1] |
| --------- | --------- | --------- |
| 값        | 값        | 값        |
| 값        | 값        | 값        |
| arr[1][0] | arr[1][1] | arr[1][2] |





- 배열 선언과 동사에 초기화 : 자료형[][] 배열 이름 = {{값1,값2,값3}, {값4,값5,값6}};

```
int[][] arr = {{1,2,3}, {4,5,6}};
```



| arr[0][0] | arr[0][1] | arr[0][1] |
| --------- | --------- | --------- |
| 1         | 2         | 3         |
| 4         | 5         | 6         |
| arr[1][0] | arr[1][1] | arr[1][2] |

```
arr.length = 2 // 다차원 배열의 길이를 구하면 행의 길이가 나옴.
arr[0].length = 3	// 다차원 배열의 인덱스값(행)의 길이를 구하면 열의 길이가 나옴.
arr[1].length = 3
```



- 계단식 다차원 구조 ([이것이 자바다] - 신용권 참조)

```java
package array;

public class MultiArray {

	public static void main(String[] args) {
		int[][] multiArray = new int[2][]; // 2행만 먼저 생성
		multiArray[0] = new int[2]; // 1행에 2칸짜리 배열 생성
		multiArray[1] = new int[4]; // 2행에 4칸짜리 배열 생성
		
		multiArray[0][0] = 1;
		multiArray[0][1] = 2;
		
		multiArray[1][0] = 3;		
		multiArray[1][1] = 4;
		multiArray[1][2] = 5;
		multiArray[1][3] = 6;

		for(int i=0; i<multiArray.length;i++) {
			for(int j=0; j<multiArray[i].length;j++) {
					System.out.println(multiArray[i][j]);
			}
			System.out.println("");
			
		}
	}
}
```

**1**
**2**

**3**
**4**
**5**
**6**







*** ArrayList 클래스 사용하기**

- 12장 컬렉션 프레임워크에서 더 자세히 다루기 때문에 간단히 언급

**주요 메서드**

| 메서드              | 설명                                                      |
| ------------------- | --------------------------------------------------------- |
| boolean add(E e)    | 요소 하나를 배열에 추가 E는 요소의 자료형을 의미          |
| int size()          | 배열에 추가된 요소 전체 개수를 반환                       |
| E get(int index)    | 배열의 index 위치에 있는 요소 값을 반환                   |
| E remove(int indes) | 배열의 index 위치에 있는 ;요소 값을 제거하고 그 값을 반환 |
| boolean isEmpty()   | 배열이 비어 있는지 확인                                   |



```java
package array;

import java.util.ArrayList; //ArrayList 클래스 import

public class ArrayListEx {
	public static void main(String[] args) {
		ArrayList<Book> library = new ArrayList<Book>();  //ArrayList 선언
		
		library.add(new Book("태백산맥", "조정래")); // 요소 추가 (.add()메서드)
		library.add(new Book("데미안", "헤르만 헤세"));
		library.add(new Book("어떻게 살 것인가", "유시민"));
		library.add(new Book("토지", "박경리"));
		library.add(new Book("어린왕자", "생텍쥐페리"));

		for(int i=0; i<library.size();i++) { //추가된 요소 개수 만큼 출력(.size()메서드)
			Book book = library.get(i);
			book.showBookInfo();}
		
		System.out.println( );
		
		library.get(2).showBookInfo();		(.get()메서드)
		
		library.remove(2).showBookInfo();	(.remove()메서드)
        
		System.out.println("===요소 삭제 후===");
        
		for(int i=0; i<library.size();i++) {
			Book book = library.get(i);
			book.showBookInfo();
			}
		
		System.out.println(library.isEmpty());  (.isempty()메서드)

	
	}
}
```

**태백산맥,조정래
데미안,헤르만 헤세
어떻게 살 것인가,유시민
토지,박경리
어린왕자,생텍쥐페리

어떻게 살 것인가,유시민
어떻게 살 것인가,유시민
===요소 삭제 후===
태백산맥,조정래
데미안,헤르만 헤세
토지,박경리
어린왕자,생텍쥐페리
false**
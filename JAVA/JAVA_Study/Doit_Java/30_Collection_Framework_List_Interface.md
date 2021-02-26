# 컬렉션 프레임워크

## 자료구조

- 사용하는 자료를 관리하여 프로그램의 기능을 효과적으로 구현하기 위해 사용
- 프로그램 실행 중 메모리에 자료를 유지, 관리하기 위해 사용
- 자바에서는 필요한 자료 구조를 미리 구현하여 `java.util` 패키지에서 제공하고 있음
  - 이를 `컬렉션 프레임워크`라고 함
- 개발자가 직접 만들어서 사용 할 수 있지만 이미 잘 만들어진 자료구조 클래스를 활용 할 수 있음

<br/>

## 컬렉션 프레임워크

- 여러 인터페이스가 정의되어 있고 해당 인터페이스를 구현한 클래스도 있다

- Collection 인터페이스와 Map 인터페이스 기반으로 구성 됨
  - `Collection Interface` : 하나의 자료를 모아서 관리하는 데 필요한 기능 제공
  - `Map Interface` : 쌍(pair)으로 된 자료들을 관리하는 데 유용한 기능 제공
- 책에서는 자주 사용되고 알아 두어야하는 클래스 위주로 설명 함

<br/>

![img](https://techbum.io/content/images/2020/05/java-collections-framework--1-.png)

[컬렉션 프레임 워크 이미지 출처](https://techbum.io/content/images/2020/05/java-collections-framework--1-.png)

<br/>

## Collection Interface

- 하위에 List Interface와 Set 인터페이스가 있음
  - `List` : 순차적인 자료를 관리하는 데 구현하여 사용
    - ArrayList, Vector, LinkedList, Stack, Queue 등
  - `Set`: 집합, 순서가 없지만 중복을 허용하지 않는 객체를 다루는데 구현하여 사용
    - HashSet, TreeSet 등이 있음
- 선언 된 메서드 중 자주 사용되는 메서드

| 메서드                   | 설명                                                      |
| ------------------------ | --------------------------------------------------------- |
| boolean add(E e)         | Collention에 객체를 추가                                  |
| void clear()             | Collention의 모든 객체를 제거                             |
| Iterator<E> iterator     | Collention을 순환할 반복자(iterator)를 반환               |
| boolean remove(Object o) | Collention에 매개변수에 해당하는 인스턴스가 존재하면 제거 |
| int size()               | Collention에 있는 요소의 개수를 반환                      |

<br/>

- add(), remove() 메서드가 boolean 형으로 값을 반환하는 이유는 객체가 잘 추가 또는 제거되었는지 여부를 확인하기 위함

<br/>

### List Interface -> ArrayList Class, Vector Class

- `ArrayList` : 싱글 스레드 환경에서 Vector보다 빠른 속도로 작업이 가능하다
  - 동기화가 필요할 경우 동기화를 지원하는 방식으로 ArrayList 선언하여 사용

```java
Collections. synchronizedList(new ArrayList<String>());
```

<br/>

- `Vector` : 모든 메서드가 호출될 때 마다 데이터의 잠금과 해제를 하는 동기화를 지원한다.
  - 따라서 ArrayList 보다 실행 속도가 느리다.

<br/>

### List Interface -> LinkedList Class

- 생성 시 정적 크기로 선언하고 크기를 변경 할 수 없는 배열의 단점을 개선한 자료 구조
- 각 요소는 `데이터`와 `다음 요소의 주소값`을 가짐
  - 때문에 물리적인 메모리와 무관하게 논리적인 순서가 존재
  - 중간에 자료 수정 및 제거하는 시간이 ArrayList에 비해 적게 걸리는 장점
  - 크기를 동적으로 증가시킬 수 있음
- 배열은 물리적, 논리적 메모리 위치가 동일하므로 빠른 접근이 가능하고 구현하기 쉬움
  - 사용하는 자료의 변동이 많은 경우 링크드 리스트, 거의 없는 경우 배열 사용이 효율적

| 자료 | 다음요소의 주소   | ->   | 자료 | 다음요소의 주소   | ->   | 자료 | 다음요소의 주소                   |
| ---- | ----------------- | ---- | ---- | ----------------- | ---- | ---- | --------------------------------- |
| 100  | ex.ex@12ex54ample | ->   | 101  | ex.ex@58ex12ample | ->   | 102  | Null 또는 0<br />(다음 요소 없음) |

<br/>

- LinkedList Class에서만 제공하는 메서드
  - addFrisrt(), addLast(), removerFirst(), removerLast()등 ArrayList보다 다양한 메서드 제공
  - 스택이나 큐에서 다양하게 활용 가능

<br/>

- 요소 추가 - 이미지 출처 Do it 자바 프로그래밍 입문

![요소 추가](https://github.com/ktae23/TIL/blob/master/JAVA_Study/Doit_Java/img/LinkedList_add.jpg)

- 요소 제거 - 이미지 출처 Do it 자바 프로그래밍 입문

![요소 제거](https://github.com/ktae23/TIL/blob/master/JAVA_Study/Doit_Java/img/LinkedList_remove.jpg)

<br/>

### ArrayList로 스택과 큐 구현하기

#### Stack

- 상자를 쌓듯한 모습
- 추가(push), 삭제(pop), 가장 마지막에 입력 된 데이터(top)
  - 맨 나중에 추가 된 데이터를 먼저 꺼내는 방식
  - LIFO(Last In First Out)

<br/>

##### 스택 클래스

```java
package stack;

import java.util.ArrayList;

public class Stack {

	private ArrayList<String> arrayStack = new ArrayList<String>();
	
	public void push(String data) { //스택의 맨 뒤에 요소를 추가
		arrayStack.add(data);
	}
	
	public String pop() {
		int len = arrayStack.size(); // ArrayList에 저장된 유효한 자료의 개수
		if(len ==0) {
			System.out.println("스택이 비었습니다.");
		}
		
		return(arrayStack.remove(len-1)); // 맨 나중에 들어온 자료 반환하고 배열에서 제거
	}
}
```

##### 테스트 클래스

```java
package stack;

public class StackTest {

	public static void main(String[] args) {
		Stack stack = new Stack();
		stack.push("첫 번째로 입력 된 자료");
		stack.push("두 번째로 입력 된 자료");
		stack.push("세 번째로 입력 된 자료");
		
		System.out.println(stack.pop());
		System.out.println(stack.pop());
		System.out.println(stack.pop());
	}
}
```

**세 번째로 입력 된 자료**

**두 번째로 입력 된 자료**

**첫 번째로 입력 된 자료**

<br/>

#### Queue

- 선착순, 대기줄의 모습
- 추가(enqueue), 삭제(dequeue), 가장 먼저 입력 된 데이터(front), 가장 마지막에 입력 된 데이터(rear)
  - 가장 먼저 추가 된 데이터를 먼저 꺼내는 방식
  - FIFO(First In First Out)

<br/>

##### 큐 클래스

```java
package queue;

import java.util.ArrayList;

public class Queue {
	private ArrayList<String> arrayQueue = new ArrayList<String>();
	
	public void enQueue(String data) { // 큐의 맨 뒤에 자료 추가
		arrayQueue.add(data);
	}
	
	public String deQueue() {
		int len = arrayQueue.size(); // 큐의 유효한 자료 개수
		if(len == 0) {
			System.out.println("큐가 비었습니다.");
			return null;
		}
		
		return(arrayQueue.remove(0)); // 맨 앞의 자료 반환하고 배열에서 제거
	}
}
```

##### 테스트 클래스

```java
package queue;

public class Test {

	public static void main(String[] args) {
		Queue queue = new Queue();
		queue.enQueue("첫 번째로 입력 된 자료");
		queue.enQueue("두 번째로 입력 된 자료");
		queue.enQueue("세 번째로 입력 된 자료");
		
		System.out.println(queue.deQueue());
		System.out.println(queue.deQueue());
		System.out.println(queue.deQueue());
	}
}
```

**첫 번째로 입력 된 자료**

**두 번째로 입력 된 자료**

**세 번째로 입력 된 자료**

<br/>

### Collection 요소를 순회하는 Iterator

- 논리적 순서가 없는 Set 인터페이스를 구현한 경우에는 get()메서드를 사용 할 수 없음 이때 Iterator를 사용
  - Iterator는 Collection Interface를 구현한 객체에서 미리 정의 되어 있는 Iterator()를 호출하면 반환되는 Iterator 클래스를 참조하여 사용

```java
Iterator ir = ArrayList.iterator();
```

<br/>

- 요소를 순회할 때 사용하는 메서드

| 메서드            | 설명                              |
| ----------------- | --------------------------------- |
| bolean hashNext() | 다음 요소가 더 있으면 true를 반환 |
| E next()          | 다음 요소를 반환                  |

```java
public boolean removeMember(int memberId) {
	Iterator<Member> ir = arrayList.iterator();		// Iterator 반환받아 ir에 대입
	while(ir.hashNext()) {							// 요소가 존재하는 동안
		Member member =  = ir.next();				// 다음 회원 값을 반환 받음
		int tempId = meamber.getMemberId();
		if(tempId == memberId) {					// 메서드의 매개변수와 일치하면
			arrayList.remove(member);				// 해당 회원 정보 삭제
			return true;							// true 반환
	}
}
// 끝까지 삭제하려는 값을 찾지 못한 경우
System.out.println(memberId + "가 존재하지 않습니다.");
return false;
}
```


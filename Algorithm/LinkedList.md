## Linked List - 엔지니어대한민국 강의 정리

- 길이가 정해져 있는 Array와 달리 뒤의 노드의 주소를 갖는 특성 상 자료의 길이를 바꾸거나 삽입, 삭제가 가능

- 중간에 연결이 끊기는 노드는 가비지 메모리로 남게 됨
  - 자바에서는 가비지 컬렉터가 지워 줌

<br/>

### [단방향](https://www.youtube.com/watch?v=DzGnME1jIwY&t=0s)

- 중간에 있는 노드의 값을 찾더라도 맨 앞에서부터 찾아나가야함
- 이처럼 한 방향으로만 읽을 수 있는 링크드 리스트를 단방향 링크드 리스트라고 함

### [양방향](https://www.youtube.com/watch?v=Vi0hauJemxA)

- 다음 주소뿐 아니라 앞의 주소 역시 가지고 있어서 양 방향으로 검색 가능

- 하지만 그만큼 공간을 차지하기 때문에 필요한 목적에 맞춰 사용

<br/>

### [실습](https://www.youtube.com/watch?v=C1SDkdPvQPA)

```java
package linkedlist;

public class Node {
	int data;
	Node next = null;
	
	Node(int d){
		this.data=d;
	}
	void append(int d) {
		Node end = new Node(d);
		Node n = this;
		while (n.next != null) {
			n = n.next;
		}
		n.next=end;
	}
	
	void delete(int d) {
		Node n = this;
		while (n.next != null) {
			if (n.next.data == d) {
				n.next = n.next.next;
			}else {
				n = n.next;
			}
		}
	}
	
	void retrieve() {
		Node n = this;
		while (n.next != null) {
			System.out.print(n.data + " -> ");
			n = n.next;
		}
		System.out.print(n.data);
	}

	public static void main(String[] args) {
		Node head = new Node(1);
		head.append(2);
		head.append(3);
		head.append(4);
		head.retrieve();
		head.delete(2);
		System.out.println("");
		head.retrieve();
		
	}

}
```

- 첫번째 노드, 헤더의 값은 링크드 리스트를 대표하는 값이기 때문에 지울 수 없도록 제한

#### 출력 결과

```java
1 -> 2 -> 3 -> 4
1 -> 3 -> 4
```

<br/>

### [실습2](https://www.youtube.com/watch?v=Vb24scNDAVg)

- 단방향 LinkedList의 끝에서 K번째 노드를 찾는 알고리즘 구현
  1. 전체 노드 개수를 세고 뒤에서부터 K번째인 위치를 찾는다.

```java
private static Node KthToLast(Node first, int k){
    Node n = first;
    int total = 1;
    // 첫 번째 노드는 검색하지 않기 때문에 total이 1부터 시작
    while(n.next != null){
        total ++;
        n=n.next;
    }
    n=first;
    for(int i=1; i<total-k+1; i++){
        n=n.next;
    }
    return n;
}
public static void main(String[] args) {
		Node head = new Node(1);
		head.append(2);
		head.append(3);
		head.append(4);
		head.retrieve();
		System.out.println("");
		
		int k=3;
		Node kth=KthToLast(head, k);
		System.out.println("Lst k(" + k + ")th data is " + kth.data);
		
	}
```

##### 출력 결과

```java
1 -> 2 -> 3 -> 4
Lst k(3)th data is 2
```

<br/>

2.  재귀 호출
   - 원하는 답이 나올 때까지 자신을 호출하는 방법
   - 처음부터 끝까지 호출한 후 뒤에서부터 세면서 나옴

```java
private static int KthToLast(Node n, int k){
		if(n == null) {
			return 0;
		}
		
		int count = KthToLast(n.next, k) + 1;
		if (count == k) {
			System.out.println(k+"th to last node is " + n.data);
		}
		return count;
	}
public static void main(String[] args) {
		Node head = new Node(1);
		head.append(2);
		head.append(3);
		head.append(4);
		head.retrieve();
		System.out.println("");	
    
		int k=3;
		KthToLast(head, k);
		
	}
```

##### 출력 결과

```java
1 -> 2 -> 3 -> 4
3th to last node is 2
```

<br/>

- 하지만 자바에서 반환값은 하나이기 때문에 문제에서 요구한 노드를 반환하지 못하고 count 변수 값만 반환한다.
  - C++의 Path by Reference를 참고하여 객체에 count변수를 객체에 넣고 참조 주소 값을 반환하는 방식으로 수정

```java
class Reference{
	public int count = 0;
}

public class Node{
    .....

        private static Node KthToLast(Node n, int k, Reference r){
            if(n == null) {
                return null;
            }

            Node found = KthToLast(n.next, k, r);
            r.count++;
            if (r.count == k) {
                return n;
            }
            return found;
        }
        public static void main(String[] args) {
            Node head = new Node(1);
            head.append(2);
            head.append(3);
            head.append(4);
            head.retrieve();
            System.out.println("");
 
            int k=3;
            Reference r = new Reference();
            Node found = KthToLast(head, k, r);
            System.out.println(found.data);
        }
}
```

##### 출력 결과

```java
1 -> 2 -> 3 -> 4
2
```

<br/>

#### 정리

- 링크드 리스트의 길이를 n으로 볼때 처음부터 끝까지 검색하기 때문에 `O(N)`의 공간을 사용하는 알고리즘
  - 최악의 경우 끝에서 다시 앞까지 돌아와야 하므로 `O(2N)`, 빅O  표기법 상 상수는 제거하므로 `O(N)`의 시간 복잡도를 가진 알고리즘

<br/>

	3. 포인터

- 공간을 사용하지 않고 구현하는 방법
- pointer1을 K만큼 먼저 보내고 pointer2를 헤더에서 출발시켜서 동시에 한칸씩 앞으로 이동
  - pointer1이 null(마지막 노드를 지남)을 만나면 pointer2의 위치가 뒤에서 k번째

```java
	private static Node KthToLast(Node first, int k){
		Node p1 = first;
		Node p2 = first;
		
		for (int i=0; i< k; i++) {
			if(p1 == null) return null;
			p1 = p1.next;
		}
		
		while (p1 != null) {
			p1 = p1.next;
			p2 = p2.next;
		}
		return p2;
	}
	
	public static void main(String[] args) {
		Node head = new Node(1);
		head.append(2);
		head.append(3);
		head.append(4);
		head.retrieve();
		System.out.println("");
		
		int k=1;
		Node found = KthToLast(head, k);
		System.out.println(found.data);
	}
```

##### 출력 결과

```java
1 -> 2 -> 3 -> 4
4
```

<br/>

- 포인터를 이용한 알고리즘은  `O(N)`의 시간이 걸리고 별도의 버퍼를 사용하지 않기 때문에 공간은 `O(1)`


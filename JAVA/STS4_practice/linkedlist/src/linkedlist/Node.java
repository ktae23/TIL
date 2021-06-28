package linkedlist;

class Reference{
	public int count = 0;
}


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

//	private static Node KthToLast(Node first, int k){
//	    Node n = first;
//	    int total = 1;
//	    // 첫 번째 노드는 검색하지 않기 때문에 total이 1부터 시작
//	    while(n.next != null){
//	        total ++;
//	        n=n.next;
//	    }
//	    n=first;
//	    for(int i=1; i<total-k+1; i++){
//	        n=n.next;
//	    }
//	    return n;
//	}

//	private static Node KthToLast(Node n, int k, Reference r){
//		if(n == null) {
//			return null;
//		}
//		
//		Node found = KthToLast(n.next, k, r);
//		r.count++;
//		if (r.count == k) {
//			return n;
//		}
//		return found;
//	}

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
//		head.delete(2);
		System.out.println("");
//		head.retrieve();
		
//		int k=3;
//		Node kth=KthToLast(head, k);
//		System.out.println("Lst k(" + k + ")th data is " + kth.data);
		
		int k=1;
//		Reference r = new Reference();
		Node found = KthToLast(head, k);
		System.out.println(found.data);
	}

}

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

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
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

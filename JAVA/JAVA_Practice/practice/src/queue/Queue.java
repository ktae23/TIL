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
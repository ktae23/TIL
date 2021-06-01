package design;

import java.util.concurrent.atomic.AtomicLong;

import design.aop.AopBrowser;
import design.proxy.IBroweser;


public class Main {

	public static void main(String[] args) {
		
		
	AtomicLong start = new AtomicLong();
	AtomicLong end = new AtomicLong(); 
		
	IBroweser aopBrowser = new AopBrowser("www.naver.com", 
			() -> {
				System.out.println("before");
				start.set(System.currentTimeMillis());
			},
			() -> {
				long now = System.currentTimeMillis();
				end.set(now - start.get());
			}
		);
	aopBrowser.show();
	System.out.println("loading time : " + end.get());
	
	aopBrowser.show();
	System.out.println("loading time : " + end.get());
	
	
	
	}
}
	

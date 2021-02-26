package class_practice;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class StringClassTest {

	public static void main(String[] args) throws ClassNotFoundException,InstantiationException,
	IllegalAccessException{
		Class strClass = Class.forName("java.lang.String"); //목표 클래스 이름으로 해당 클래스 가져오기
		
		
		System.out.println("===================생성자===================");
		
		Constructor[] cons = strClass.getConstructors(); // 생성자 목록을 배열 형태로 리턴
		for(Constructor c : cons) {
			System.out.println(c);
			}	
		System.out.println("===================멤버변수===================");
			
		Field[] fields = strClass.getFields(); // 멤버 변수 (필드) 목록을 배열 형태로 리턴
		for(Field f : fields) {
			System.out.println(f);
		}
		System.out.println("===================메서드===================");
		
		Method[] methods = strClass.getMethods(); // 메서드 목록을 배열 형태로 리턴
		for(Method m : methods) {
			System.out.println(m);
		}
	}
}

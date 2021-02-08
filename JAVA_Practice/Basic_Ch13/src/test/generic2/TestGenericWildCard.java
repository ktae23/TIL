package test.generic2;

import java.util.ArrayList;
public class TestGenericWildCard {
	public static void main(String[] args) {
		Cat cat=new Cat();
		AnimalList zoo=new AnimalList();
		zoo.add(cat);

	}

}

class AnimalList{
	ArrayList al=new ArrayList();//빈상자
	public void cryingAnimalList() {//모든 동물을 울게
		
	}
	public void add(Object o) {//동물 추가
		al.add(o);
	}
	public void get(){//동물 리턴
		
	}
	public void remove() {//동물 삭제
		
	}
}

class LandAnimal{
	public void crying() {
		System.out.println("육지동물");
	}
}
class Cat extends LandAnimal{
	@Override
	public void crying() {
		System.out.println("냐옹냐옹");
	}
}
class Dog extends LandAnimal{
	@Override
	public void crying() {
		System.out.println("멍멍");
	}
}
class Sparrow extends LandAnimal{
	@Override
	public void crying() {
		System.out.println("짹짹");
	}
}

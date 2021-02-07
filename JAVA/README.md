# Override, Tight & loose Coupling



## Abstact_Method& Override



```java
package abstract_method;

public abstract class Animal {
	public String kind;
	
	public void breathe() {
		System.out.println("숨을 쉽니다.");
	}

	public abstract void sound();
}
```

> 추상 클래스 Animal은 변수로 kind를, 메서드로 일반메서드 breathe()와 추상메서드 sound()를 가짐

```java
package abstract_method;

public class Cat extends Animal {
	public Cat() {
		this.kind = "포유류";
	}

	@Override
	public void sound() {
		System.out.println("야옹");
	}
}
```

> Cat 클래스는 추상 클래스 Animal을 상속 받아 kind 변수에 값을 입력한다.
>
> 추상 메서드인 sound() 재정의 필수.

```java
package abstract_method;

public class Dog extends Animal {
	public Dog() {
		this.kind = "포유류";
	}

	@Override
	public void sound() {
		System.out.println("멍멍");
	}
}
```

>  Dog 클래스는 추상 클래스 Animal을 상속 받아 kind 변수에 값을 입력한다.
>
> 추상 메서드인 sound() 재정의 필수.

```java
package abstract_method;

public class AnimalExample {
	public static void main(String[] args) {
		Dog dog = new Dog();
		Cat cat = new Cat();
		dog.sound();
		cat.sound();
		dog.breathe();
		cat.breathe();
		System.out.println("-----");
		
		//변수의 자동 타입 변환
		Animal animal = null;
		animal = new Dog();
		animal.sound();
		animal.breathe();
		animal = new Cat();
		animal.sound();
		animal.breathe();
		System.out.println("-----");
		
		//매개변수의 자동 타입 변환
		animalSound(new Dog());
		animalSound(new Cat());
		animalBreathe(new Dog());
		animalBreathe(new Cat());
	}
	
	public static void animalSound(Animal animal) {
		animal.sound();
	}
	public static void animalBreathe(Animal animal) {
		animal.breathe();
	}
}
```

멍멍
야옹
숨을 쉽니다.
숨을 쉽니다.

` ----- `

멍멍
숨을 쉽니다.
야옹
숨을 쉽니다.

`-----`

멍멍
야옹
숨을 쉽니다.
숨을 쉽니다.



## Tight & loose Coupling



[단단한 결합, 느슨한 결합 참고 사이트](https://www.tutorialspoint.com/Coupling-in-Java)

### 단단한 결합

```java
package coupling.test;

public class Tester {
	   public static void main(String args[]) {
	      A a = new A();

	      //a.display()는 'A'와 'B'를 출력한다.
	      //이 실행은 동적 변경이 안된다.
	      //단단히 결합 되어 있다.
	      a.display();
	   }
	}

	class A {
	   B b;
	   public A() {
	      //b는 A에 단단히 결합되어 있다.
	      b = new B();
	   }

	   public void display() {
	      System.out.println("A");
	      b.display();
	   }
	}

	class B {    
	   public B(){}
	   public void display() {
	      System.out.println("B");
	   }
	}
```

**A**

**B**

---

### 느슨한 결합

```java
package coupling.test;

import java.io.IOException;

public class Tester {
   public static void main(String args[]) throws IOException {
      Show b = new B();
      Show c = new C();

      A a = new A(b);          
      //a.display()는  'A'와 'B'를 출력 한다.    
      a.display();

      A a1 = new A(c);
      //a1.display()는  'A'와 'C'를 출력 한다.      
      a1.display();
   }
}

interface Show {
   public void display();
}

class A {
   Show s;
   public A(Show s) {
      //s는 A에 느슨한 결합이 되어 있다.
      this.s = s;
   }

   public void display() {
      System.out.println("A");
      s.display();
   }
}

class B implements Show {    
   public B(){}
   public void display() {
      System.out.println("B");
   }
}

class C implements Show {    
   public C(){}
   public void display() {
      System.out.println("C");
   }
}
```

**A**
**B**
**A**
**C**
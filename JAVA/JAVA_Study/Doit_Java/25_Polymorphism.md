# 다형성

- 같은 코드에서 여러 자료형의 실행 결과가 구현되는 것

<br/>

### 상위 클래스

```java
package poly;

public class Animal {

	public void move() {
		System.out.println("동물이 움직입니다.");
	}
}
```

### 하위 클래스

```java
package poly;

public class Eagle extends Animal{
	
	public void move() {
	System.out.println("독수리가 하늘을 납니다.");
	}
}
```

<br/>

```java
package poly;

public class Human extends Animal{
	
	public void move() {
	System.out.println("사람이 두 발로 걷습니다.");
	}
}
```

<br/>

```java
package poly;

public class Tiger extends Animal{
	
	public void move() {
	System.out.println("호랑이가 네 발로 뜁니다.");
	}
}
```

<br/>

### 테스트 클래스

```java
package poly;

public class AnimalTest {

	public static void main(String[] args) {
		AnimalTest aTest = new AnimalTest();
		
		aTest.moveAnimal(new Human());
		aTest.moveAnimal(new Tiger());
		aTest.moveAnimal(new Eagle());
		
	}
	
	public void moveAnimal(Animal animal) {
		animal.move();
	}
}
```

**사람이 두 발로 걷습니다.**

**호랑이가 네 발로 뜁니다.**

**독수리가 하늘을 납니다.**

<br/>

- 매개변수로 넘어온 실제 인스턴스의 메서드가 출력 된다.
- `animal.mover();`코드는 변함이 없지만 어떤 매개 변수가 넘어오느냐에 따라 출력문이 달라진다. 이것이 다형성이다.
  - 새로운 동물이 추가 되어도 동일하게 Animal 클래스를 상속 받음으로써 관리가 가능하다. 이것이 확장성이다.
  - 다형성을 잘 활용하면 유연하면서도 구조화 된 코드를 구현하여 확장성 있고 유지보수 하기 좋은 프로그램을 개발 할 수 있다.

<br/>

---

<br/>

### 상속의 사용(IS -A 관계)

- 상속은 ''`사람`은 `동물`이다.'', "`독수리`는 `동물`이다."처럼 "`A`는 `B`이다."의 관계가 성립 할때 묶어서 사용한다.
- 아무런 관련이 없는 개념의 클래스들을 단지 코드 재사용을 목적으로 상속하는 것은 좋지 않음.

<br/>

---

<br/>

### 다운 캐스팅

- 우리는 상속을 배울 때 상위 클래스로 묵시적 형 변환이 가능하다는 것을 보았다.
- 하지만 이럴 땐 오버라이딩 한 메서드가 아니고서는 상위 클래스의 범위 내로 활동이 제한되었기에 다시 하위 클래스로 내려와야 할 경우가 생긴다.
  - 이처럼 하위 클래스로 형 변환을 하는 것을 `다운 캐스팅`이라 부른다.

<br/>

---

<br/>

### Instanceof

- 모든 인간은 동물이지만 모든 동물이 인간은 아니다.
  - 때문에 다운 캐스팅을 할때 해당 동물이 `인간`인지를 확인해서 다운 캐스팅을 해줘야 한다.
  - 이를 확인하는 예약어가 Instanceof이다.

```java
Animal animal_H = new Human();	// 상위 클래스로 객체 생성하여 묵시적 형 변환

if(Animal_H instanceof Human){	// 만약  animal_H 이 참조하는 인스턴스가 Human 클래스일 경우
	Human human_1 = (Human)Animal_H;	// Human 클래스의 human_1에 해당 값을 Human 클래스로 변환하여 할당
}
```

- instanceof로 확인하지 않고 다운캐스팅 할 경우 컴파일은 되나, 실행 시 오류가 발생한다.


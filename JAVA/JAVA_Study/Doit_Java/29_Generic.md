# Generic Programming

- 어떤 값이 하나의 참조 자료형이 아닌 여러 참조 자료형을 사용 할 수 있도록 프로그래밍 하는 것을 `제네릭 프로그래밍`이라고 한다
  - 참조 자료형이 변환 될 때 이에 대한 검증을 `컴파일러`가 하므로 안정적이다
  - `컬렉션 프레임워크`의 많은 부분이 `제네릭`으로 구현 되어 있다.

<br />

- 기존에는 다형성을 구현하기 위해 상위 클래스로 매개변수를 이용한 뒤 반드시 instanceof 예약어를 이용한 다운캐스팅을 해야 했다.
  - 제네릭을 사용하면 클래스나 메서드 정의 후 사용 시점에 자료형을 지정하므로 이러한 문제를 해결 할 수 있다.

<br />

<br />

### 제네릭 정의하기

- 여러 참조 자료형을 사용해야 하는 부분에 Object가 아닌 하나의 문자로 표현(어떠한 문자로도 사용 가능)
  - T는 자료형 매개변수 (Type Parameter)
  - E는 요소형 매개변수 (Element)
  - K는 키 매개변수(Key)
  - V는 값 매개변수(Value)
  - 컬렉션 클래스처럼 배열 기반 구조는 E를 사용하는 편
- 제네릭 클래스가 아니어도 제네릭 메서드 구현 가능

<br />

```java
public class 클래스 이름<T> { ... }
public interface 인터페이스 이름<T> { ... }
```

<br />

```java
public class GenericPrinter<T> { //제네릭 클래스 선언
	private T material;
	
	public void setMaterial(T material) {
	this.material = material;
	}
	
	public T getMaterial(){
	return material;
	}
}
```

<br />

##### 다이아몬드 연산자 <>

- 자바 7부터는 제네릭 자료형의 클래스를 생성할 때 생성자에 사용하는 자료형을자료형을 명시하지 않더라도 생성 부분을 컴파일러가 유추 할 수 있음

<br />

```java
ArrayList<String> list = new ArrayList<>(); 
// 앞에 String이 있기 때문에 뒤의 생성 부분은 생략
```

<br />

##### 자료형 매개변수 T와 static

- T의 자료형이 결정되는 시점은 제네렉 클래스의 인스턴스가 생성되는 순간
  - static 변수의 자료형이나 메서드 내부 변수의 자료형이 훨씬 먼저 생성 되기 때문에 static 내부에 T 사용 불가

<br />

##### 제네릭에서 자료형 추론하기

- 자바 10부터는 `지역 변수에 한해`서 `var`를 이용해 자료형 추론 가능

```java
var list = new ArrayList<String>();
```

<br />

### T 자료형 제한하기 <T extends 클래스>

- 특정 추상 클래스, 인터페이스를 상속한 클래스로 T 자료형 값을 제한 할때 사용
- 상위 클래스의 메서드를 사용 할 수 있다.
- 상속하는 상위 클래스의 하위 클래스로만 T로 사용 가능

<br />

---

<br /><br />

## '이것이 자바다'의 제너릭 설명 추가

<br />

### 멀티 타입 파라미터(class<k,V, ...>, interface<K,V, ...>)

- 제네릭 타입은 두 개 이상의 멀티 타입 파라미터를 사용 할 수 있고, 각 타입 파라미터를 콤마로 구분 한다

<br />

```java
package generic_practice;

public class ProductTest {

	public static void main(String[] args) {
		Product<String, Integer> product1 = new Product<String, Integer>();
		product1.setKind("TV");
		product1.setModel(151105);
		
		System.out.println(product1);
		
		String kind1 = product1.getKind();
		int model1 = product1.getModel();
				
		System.out.println(kind1 + "," + model1);
	}
}
```

**generic_practice.Product@139a55**

**TV,151105**

<br />

### 제네릭 메서드(<T,R>R method(T t))

- 제네릭 메서드는 매개 타입과 리턴 타입으로 `T(타입 파라미터)`를 갖는 메소드를 말한다.
  - 마찬가지로 여러 타입을 사용 할 수 있다.

```java
public <타입 파라미터, ...> 리턴타입 메서드 이름(매개변수, ...) { ... }
public <T, ...> Product<T> processing(T t, ...) { ... }
```

- 두 가지 방식으로 제네릭 메서드 호출 가능
  - 코드에서 타입 파라미터의 구체적인 타입을 명시적으로 지정
  - 컴파일러가 매개값의 타입을 보고 구체적인 타입 추정

```java
Product<Integer> pdt = <Imteger>processing(100);
Product<Integer> pdt = processing(100);
```



<br />

### 와일드카드 타입(<?>, <? extends ...>. <? super ...>)

- 코드에서 `?`를 일반적으로 와일드카드라고 부른다
- 제네릭 타입을 매개값이나 리턴 타입으로 사용할 때 구체적인 타입 대신 와일트카드를 세 가지 형태로 사용 가능

<br />

- **`제네릭 타입<?>`** : Unbouned Wildcards(제한 없음)
  - 타입 파라미터를 대치하는 구체적인 타입으로 모든 클래스나 인터페이스 타입이 올 수 있다.
- **`제네릭 타입<? extends 상위 타입>`** : Upper Bounded Wildcard(상위 클래스 제한)
  - 타입 파라미터를 대치하는 구체적인 타입으로 명시한 타입 이하의 타입만 올 수 있다.
- **`제네릭 타입<? super 하위 타입>`** : Lower Bounded Wildcards(하위 클래스 제한)
  - 타입 파라미터를 대치하는 구체적인 타입으로 명시한 타입이나 이상의 타입만 올 수 있다.

<br />

<br />

### 제네릭 타입의 상속과 구현

- 제네릭 타입도 다른 타입과 마찬가지로 상속의 상위 클래스가 될 수 있음

```java
public class ChildProduct<T,M> extends ParentProduct<T,M> {...}
```

- 하위 클래스는 추가적으로 타입 파라미터를 가질 수 있다.

```java
public class ChildProduct<T,M,C> extends ParentProduct<T,M> {...}
```

<br />

#### ChildProduct 클래스

```java
package wildcard;

public class ChildProduct <T, M, C> extends ParentProduct<T, M>{

	private C company;

	public C getCompany() {
		return company;
	}

	public void setCompany(C company) {
		this.company = company;
	}
}
```

<br />

#### ParentProduct 클래스

```java
package wildcard;

public class ParentProduct <T, M>{

	private T kind;
	private M model;
	
	public T getKind() {
		return kind;
	}
	public M getModel() {
		return model;	
	}
	
	public void setKind(T kind) {
		this.kind = kind;
	}
	public void setModel(M model) {
		this.model = model;
	}
}
	class Tv{
		String tv = "tv";
		
		public String getTv() {
			return tv;
		}
	}
```

<br />

#### Storage 인터페이스

```java
package wildcard;

public interface Storage<T> {

	public void add(T item, int index);
	public T get(int index);
}
```

<br />

#### StorageImpl 구현클래스

```java
package wildcard;

public class StorageImpl<T> implements Storage<T>{
	
	private T[] array;

	@SuppressWarnings("unchecked")
	public StorageImpl(int capacity) {
		this.array = (T[]) (new Object[capacity]);
        // new T[n]형태로는 배열을 생성 불가
        // 타입 파라미터로 배열을 생성gkfuaus (T[])(new Object[n])으로 생성해야 한다.
	}
	
	@Override
	public void add(T item, int index) {
		array[index] = item;
	}

	@Override
	public T get(int index) {
		return array[index];
	}
}
```

<br />

#### 테스트 클래스

```java
package wildcard;

public class Test {

	public static void main(String[] args) {
		
		ChildProduct<Tv, String, String> product = new ChildProduct<>();
		product.setKind(new Tv());
		product.setModel("Smart TV");
		product.setCompany("Samsung");
		
		System.out.println(product);
		System.out.println("제조사 : " + product.getCompany());
		System.out.println("품종과 제품 모델 : " + product.getKind().getTv() +", " + product.getModel());
		
		Storage<Tv> storage = new StorageImpl<>(100);
		storage.add(new Tv(), 0);
		Tv tv = storage.get(0);
		
		System.out.println(tv);
	}
}
```

**wildcard.ChildProduct@139a55**

**제조사 : Samsung**

**품종과 제품 모델 : tv, Smart TV**

**wildcard.Tv@1db9742**
# 상속

## 클래스의 상속

- 상속을 표현할때는 상속 받는 하위 클래스 쪽에서 상속해주는 상위 클래스쪽으로 화살표를 표현한다.

<br/>

- 클래스 상속 문법

```java
class Children extends Parents
```

---

<br/>

#### 상속을 받으면 상위 클래스의 모든 변수와 메서드를 그대로 물려 받아 이용 할 수 있다.

```java
class Parents {
	int money = 15000;
	String familyname = "Park";
	
	public void Study (){
		System.out.println("Study Hard");
	}
}
```

- 하위 클래스에서 상속 시

```java
package practice;

public class Children extends Parents {
    
    String myname = "Kyungtae";

	public static void main(String[] args) {
		Children child = new Children();
		
		System.out.println("물려받은 재산은 " + child.money);
		System.out.println("물려받은 이름은 " + child.familyname);
		child.Study();
	}
}
```

**물려받은 재산은 15000**

**물려받은 이름은Park**

**내 이름은 Kyungtae**

**Study Hard**

<br/>

<br/>

- 위에서 봤듯이 별다른 제한이 없을 경우 상속을 받으면 하위 클래스 객체를 통해 상위 클래스의 멤버 변수와 메서드를 모두 이용 할 수 있다.

---

<br/>

#### 상속 받은 객체 생성 시 호출 순서

```java
package practice;

public class Parents {
	
	public Parents() {
		System.out.println("부모님 소환");
	}

	int money = 15000;
	String familyname = "Park";
	
	public void Study() {
		System.out.println("Study Hard");
	}
}

```

<br/>

```java
package practice;

public class Children extends Parents {
		
    String myname = "Kyungtae";
	
	public Children () {
		System.out.println("자식 소환");
	}

	public static void main(String[] args) {
		Children child = new Children();
		
		System.out.println("물려받은 재산은 " + child.money);
		System.out.println("물려받은 이름은 " + child.familyname);
   		System.out.println("내 이름은 " + child.myname);
		child.Study();
	}
}
```

**부모님 소환**
**자식 소환**

**물려받은 재산은 15000**

**물려받은 이름은Park**

**내 이름은 Kyungtae**

**Study Hard**

<br/>

<br/>

- 이를 통해 하위 클래스를 생성시 상위 클래스를 먼저 호출한 뒤 메인 메서드를 실행 하는 것을 알 수 있다.

  - 지금은 기본적으로 컴파일러가 자동 실행을 해주어서 상위 클래스가 호출 되었지만 명시적으로 호출 할 수도 있다.

  - ```java
    package practice;
    
    public class Children extends Parents {
    	    
        String myname = "Kyungtae";
    	
    	public Children () {
    		super();	//컴파일러가 자동으로 추가해주는 코드, 직접 super 예약어를 사용할 수 있다.
    		System.out.println("자식 소환");
    	}
    
    	public static void main(String[] args) {
    		Children child = new Children();
    		
    		System.out.println("물려받은 재산은 " + child.money);
    		System.out.println("물려받은 이름은 " + child.familyname);
            System.out.println("내 이름은 " + child.myname);
    		child.Study();
    	}
    }
    ```

  <br/>

  ---

  #### 상위 클래스를 명시적으로 호출함에 따라 하위 클래스 생성시 매개 변수를 받아 상위 클래스 멤버 변수 값을 초기화 할 수 있다.

  

  ```java
  package practice;
  
  public class Parents { // 상위 클래스
  	
  	public Parents() {
  		System.out.println("부모님 소환");
  	}
  	
  	public Parents(int money, String familyname) {
  		this.money=money;
  		this.familyname=familyname;
  		System.out.println("부모님 소환");
  	}
  
  	int money = 15000;
  	String familyname = "Park";
  	
  	public void Study() {
  		System.out.println("Study Hard");
  	}
  }
  ```

  <br/>

  ````java
  package practice;
  
  public class Children extends Parents { // 하위 클래스
  	
      
      String myname = "Kyungtae";
  	
  	public Children () {
  		super();
  		System.out.println("자식 소환");
  	}
  	
  	public Children (int money, String familyname, String myname) {
  		super(money, familyname);
  		this.myname=myname;
  		System.out.println("자식 소환");
  	}
  
  	public static void main(String[] args) {
  		Children child = new Children(27000,"김","수한무거북이와두루미");
  		
  		System.out.println("물려받은 재산은 " + child.money);
  		System.out.println("물려받은 이름은 " + child.familyname);
  		System.out.println("내 이름은 " + child.myname);
  		child.Study();
  	}
  }
  ````

  **부모님 소환**

  **자식 소환**

  **물려받은 재산은 27000**

  **물려받은 이름은 김**

  **내 이름은 수한무거북이와두루미**

  **Study Hard**

  <br/>

  ---

- 이를 응용해보면 상위 클래스의 멤버 변수나 메서드를 super 예약어를 이용해 참조 할 수 있다.

```java
package practice;

public class Parents {
	
	public Parents() {
		System.out.println("부모님 소환");
	}
	
	public Parents(int money, String familyname) {
		this.money=money;
		this.familyname=familyname;
		System.out.println("부모님 소환");
	}

	int money = 15000;
	String familyname = "Park";
	
	public void Study() {
		System.out.println("Study Hard");
	}
	
	
	public String showParentsInfo() {
		return familyname + "" + "\n가진 돈은 :" + money;
	}
	
}

```

<br/>

```java
package practice;

public class Children extends Parents {
	
    
    String myname = "Kyungtae";
	
	public Children () {
		super();
		System.out.println("자식 소환");
	}
	
	public Children (int money, String familyname, String myname) {
		super(money, familyname);
		this.myname=myname;
		System.out.println("자식 소환");
	}
	
	public String showChildrenInfo() {
		return "부모님의 정보는 : " + super.showParentsInfo() + "\n저의 이름는 :" + familyname + "" + myname + "\n가진 돈은 :" + money;
	}

	public static void main(String[] args) {
		Children child = new Children(27000,"김","수한무거북이와두루미");
		
		System.out.println("물려받은 재산은 " + child.money);
		System.out.println("물려받은 이름은 " + child.familyname);
		System.out.println("내 이름은 " + child.myname);
		child.Study();
		
		System.out.println("==========================");

		System.out.println(child.showChildrenInfo());
	}
}

```

**부모님 소환**

**자식 소환**

**물려받은 재산은 27000**

**물려받은 이름은 김**

**내 이름은 수한무거북이와두루미**

**Study Hard**

**==========================**

**부모님의 정보는 : 김**

**가진 돈은 :27000**

**저의 이름는 :김수한무거북이와두루미**

**가진 돈은 :27000**

---

<br/>

#### 상위 클래스로 묵시적 클래스 형 변환

```java
	public static void main(String[] args) {
//		Parents child = new Children(27000,"김","수한무거북이와두루미");
		Parents child = new Children(27000,"김");
			
		
		System.out.println("물려받은 재산은 " + child.money);
		System.out.println("물려받은 이름은 " + child.familyname);
//		System.out.println("내 이름은 " + child.myname);
		child.Study();
		
		System.out.println("==========================");

//		System.out.println(child.showChildrenInfo());
```

- 상위 클래스로 선언하고 생성해도 별도의 조치 없이 자동으로 형 변환이 일어난다.
  - 다만 이렇게 형 변환을 할 경우 상위 클래스의 객체로 생성 되기 때문에 하위 클래스에 있던 myname 변수를 사용할 수 없다.
  - 이처럼 상위 클래스로 변환 시 상위 클래스의 변수와 메서드만 사용 가능하고 하위 클래스에만 있던 정보는 이용 할 수 없다.

---

<br/>

#### 메서드 오버라이딩

- 메서드 오버라이딩은 상위 클래스의 메서드를 `재정의`하여 하위 클래스만의 메서드를 만드는 일이다.
  - 위의 예제에서 부모 클래스의 Study() 메서드는 "Study Hard"를 출력한다. 하지만 아이는 공부를 열심히 하기가 싫다.
  - 이럴때  Study()메서드를 오버라이딩하여 "Nope! Play Hard"로 변경하는거다.

```java
	@Override
	public void Study() {
		System.out.println("Nope! Play Hard");
	}

	public static void main(String[] args) {
     	Children child = new Children(27000,"김","수한무거북이와두루미");

		System.out.println("물려받은 재산은 " + child.money);
		System.out.println("물려받은 이름은 " + child.familyname);
		System.out.println("내 이름은 " + child.myname);
		child.Study();
```

**부모님 소환**

**자식 소환**

**물려받은 재산은 27000**

**물려받은 이름은 김**

**내 이름은 수한무거북이와두루미**

**Nope! Play Hard**

<br/>

- study()의 출력문이 변경 된 것을 볼 수 있다. 
- 위의 상위 클래스로의 형 변환과 오버라이딩을 함께 사용하면 Study()메서드 출력문은 어떤게 나올까?
  - 위에서 상위 클래스로 형 변환시 상위 클래스의 멤버 변수와 메서드만 사용 가능하다고 말했었다.
  - 그럼 "Study Hard"가 출력되려나? 같이 확인해보자.

```java
	public static void main(String[] args) {
     	Parents child2= new Children();
     	
     	child2.Study();
```

**Nope! Play Hard**

<br/>

- 놉!~! 정답은 오버라이딩 된 메서드가 사용되었다. 
  - 자식 이기는 부모 없다고, 자식이 하겠다는건 부모가 못 말린다.
  - 상위 클래스와 하위 클래스에 같은 이름의 메서드가 있을 경우 메스드가 참조하는 클래스의 메서드를 출력한다.
  - child2.Study() 실행 시 @Override 어노테이션을 떼어도 하위 클래스의 메서드가 작동한다.
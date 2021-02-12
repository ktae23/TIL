# 컬렉션 프레임워크

## 컬렉션 프레임워크

- 여러 인터페이스가 정의되어 있고 해당 인터페이스를 구현한 클래스도 있다

- Collection 인터페이스와 Map 인터페이스 기반으로 구성 됨
  - `Collection Interface` : 하나의 자료를 모아서 관리하는 데 필요한 기능 제공
  - `Map Interface` : 쌍(pair])으로 된 자료들을 관리하는 데 유용한 기능 제공
- 책에서는 자주 사용되고 알아 두어야하는 클래스 위주로 설명 함

<br/>

![img](https://techbum.io/content/images/2020/05/java-collections-framework--1-.png)

[컬렉션 프레임 워크 이미지 출처](https://techbum.io/content/images/2020/05/java-collections-framework--1-.png)



<br/>

## Set Interface

- 순서와 상관 없이 중복을 허용하지 않는 경우 사용 (집합)
  - Set을 구현한 클래스 중 대표 적으로 `HashSet`과 `TreeSet`이 있다

<br/>

### Collection 요소를 순회하는 Iterator

- 논리적 순서가 없는 Set 인터페이스를 구현한 경우에는 get()메서드를 사용 할 수 없음 이때 Iterator를 사용
  - Iterator는 Collection Interface를 구현한 객체에서 미리 정의 되어 있는 Iterator()를 호출하면 반환되는 Iterator 클래스를 참조하여 사용

```java
Iterator ir = ArrayList.iterator();
```

<br/>

- 요소를 순회할 때 사용하는 메서드

| 메서드            | 설명                              |
| ----------------- | --------------------------------- |
| bolean hashNext() | 다음 요소가 더 있으면 true를 반환 |
| E next()          | 다음 요소를 반환                  |

```java
public boolean removeMember(int memberId) {
	Iterator<Member> ir = arrayList.iterator();		// Iterator 반환받아 ir에 대입
	while(ir.hashNext()) {							// 요소가 존재하는 동안
		Member member =  = ir.next();				// 다음 회원 값을 반환 받음
		int tempId = meamber.getMemberId();
		if(tempId == memberId) {					// 메서드의 매개변수와 일치하면
			arrayList.remove(member);				// 해당 회원 정보 삭제
			return true;							// true 반환
	}
}
// 끝까지 삭제하려는 값을 찾지 못한 경우
System.out.println(memberId + "가 존재하지 않습니다.");
return false;
}
```

<br/>

-  hashCode()메서드와 equals() 메서드를 재정의하여 중복값 유효성 검사 구현 필요

<br/>

### TreeSet Class

- Tree로 시작하는 클래스는 데이터 추가 후 결과를 출력하면 결과 값이 정렬 된다
- 자료의 중복을 허용하지 않으면서 출력 값을 정렬하는 클래스

```java
package collection.treeset;

import java.util.TreeSet;

public class TreeSetTest {
	public static void main(String[] args) {
		TreeSet<String> treeSet = new TreeSet<String>();
		treeSet.add("홍길동");
		treeSet.add("강감찬");
		treeSet.add("이순신");
		
		for(String str : treeSet) {
			System.out.pringln(str);
		}
	}
}
```

**강감찬**

**이순신**

**홍길동**

<br/>

- 결과에서 알 수 있듯이 입력 순서와 무관하게 정렬이 되어 출력이 됨
- 자바는 정렬을 위해 `이진 트리`를 사용함

![img](https://t1.daumcdn.net/cfile/tistory/23760A3356406D1F2E)

[이미지 출처](https://t1.daumcdn.net/cfile/tistory/23760A3356406D1F2E)

<br/>

- TreeSet을 사용할 때는 어떤 기준으로 비교하여 정렬할 것인지 Comparable 또는 Comparator 인터페이스를 구현해야 함

<br/>

#### Comparable 인터페이스

- 자기 자신과 전달받은 매개변수를 비교하는 compareTo() 추상 메서드를 구현해야 함



```java
public class Member implements Comparable<Member>{
    private int memberId;
    private Stirng memberName;
    
    public Member(int memberId, String memberName) {
        this.memberId = memberId;
        this.memberName = mamberName;
    }

    ...
    
@Overrride
public int compareTo(Member member) {
	return (this.memberId - member.memberId); 
        //추가한 Id 값과 매개변수로 받은 Id 값을 비교
        // 내림차순으로 정렬하려면 return (this.memberId - member.memberId)*(-1);
	}
}
```



- this 값이 더 크면 양수를 반환하여 오름차순으로 정렬
  - 반대는 음수를 반환하여 내림 차순으로 정렬

<br/>

#### Comparator 인터페이스

- 전달 받은 두 매개변수를 비교하는 compare() 추상 메서드를 구현해야 함
- 주의사항 : TreeSet 생성자에 Comparator를 구현한 객체를 매개변수로 전달해야 함

```java
TreeSet<Member> treeSet = new TreeSet<Member>(new Member());
```

<br/>

```java
package collection;

import java.util.Comparator;

public class Member2 implements Comparator<Member2> {
    private int memberId;
    private String memberName;
    
    public Member2(int memberId, String memberName) {
        this.memberId = memberId;
        this.memberName = memberName;
    }
    
    ...
        
    @Override
    public int compare(Member2 mem1, Member2 mem2) {
        return mem1.getMemberId() - mem2.getMemberId();
    }
}
```



- 첫 번째 매개변수 값이 더 크면 양수를 반환하여 오름차순으로 정렬
  - 반대는 음수를 반환하여 내림 차순으로 정렬

<br/>

- 일반적으로 Comparable 인터페이스를 더 많이 사용함
  - 이미 Comparable 인터페이스가 구현 되었을 경우 클래스의 정렬 방식을 정의할 때 Comparator 인터페이스 사용
  - String 클래스의 경우 이미 compareTo() 메서드가 오름차순으로 구현되어 있고 final로 선언 되어 있어서 정렬 방식을 재정의 할 수 없다. 이럴때 Comparator 인터페이스 사용
    - TreeSet생성자의 매개변수에 Comparator를 구현한 클래스 객체를 넣어줘야만 적용

<br/>

## Map Interface

- `쌍(pair)`으로 되어 있는 자료를 관리하는 메서드들이 선언 되어 있음
  - `key-value 쌍`이라고 표현, key값은 중복 될 수 없음

- Hashtable, HashMap, TreeMap, Properties 등
- 기본적으로 검색용 자료 구조
  - key 값을 알고 있을 때 value 값을 찾기 위한 구조
- 선언 된 메서드 중 자주 사용되는 메서드

| 메서드                                  | 설명                                                         |
| --------------------------------------- | ------------------------------------------------------------ |
| V put(K key, V value)                   | key에 해당하는 value 값을 map에 넣음                         |
| V get(K Key)                            | key에 해당하는 valu;e 값을 반환                              |
| boolean isEmpty()                       | Map이 비어 있는지 여부 반환                                  |
| boolean containsKey(Object key)         | Map에 해당 key가 있는지 여부 반환                            |
| boolean containsValue(Object value)     | Map에 해당 value가 있는지 여부 반환                          |
| Set keyset()                            | key  집합을 Set으로 반환<br />(Set과 key는 모두 중복 안되는 특징이 있으므로) |
| Collection values()                     | value를 Collection으로 반환 (중복 무관)                      |
| V remove(key)                           | 해당 key가 있는 경우 삭제                                    |
| booean remove(Object key, Object value) | 해당 key가 존재하고 key에 해당하는 value가 매배변수와 일치할 경우 삭제 |

<br/>

### HashMap Class

- Map 인터페이스를 구현한 클래스 중 가장 많이 사용 됨
- 해시를 이용해 자료를 관리하는 방식
  - Hashtable :해시 방식의 자료를 저장하는 공간
- key 값이 정해지면 그에 대응하는 해시 테이블의 저장위치가 정해짐
  - 해시 함수 : 해시테이블의 저장 위치를 계산하는 함수

```java
index = hash(key)
// index는 저장 위치
```

- 해시 함수를 어떻게 만드느냐는 key 값으 특성이나 개발 프로그램의 성격에 따라 다를 수 있음
- 자료 추가 속도나 검색 속도가 상당히 빠르다는 장점이 있음
- 서로 다른 key 값에 같은 index가 반환되는 충돌(collision)이 발생할 수 있음
  - 이런 확률을 낮추기 위해 테이블이 적정 수준 채워지면 테이블을 확장한다.
  - 자바는 해시 테이블의 75%까지만 사용하고 컴파일러가 자동으로 메모리를 확장한다
- Map 인터페이스에서 사용하는 key 값은 중복 될 수 없으므로 equals(), hashcode() 메서드를 재정의하여 사용하는 것을 권장

<br/>

##### HashMap 구현 클래스 예제

```java
public class MemberHashMap{
	pivate HashMap<Integer, Member> hashMap;
	
	public MemberHashMap() {
		hashMap = new HashMap<Integer, Member>();
	}

public void addMember(Member member) {	//HashMap에 회원을 추가하는 메서드
	hashMap.put(member.getMemberId(), member); //key-value 쌍으로 추가
	}
	
	public boolean removeMember(int memberId) { //HashMap에서 회원을 삭제하는 메서드
		if(hashMap.containsKey(memberId)) {	
            // HashMap에 매개변수로 받은 회원 Id를 key로 갖는 값이 있다면 
			hashMap.remove(memberId); // 해당 회원 삭제
			return;
		}
		System.out.println(memberId + "가 존재하지 않습니다.");
		return false;
	}
	
	public void showAllMember() {	// Iterator를 이용해 전체 회원을 출력하는 메서드
		Iterator<Integer> ir = hashMap.keySet().iterator(); 
        	//HashMap의 키값을 Set구조로 가져와 iterator 생성
		while (ir.hasNest()) { // 값이 존재하는 동안
			int key = ir.next();	// key값을 가져와서
			Member member = hashMap.get(key); 	// HashMap에서 key값과 일치하는 value 가져오기
			System.out.println(member);
		}
		System.out.println();
	}
}
```

- showAllMember() 메서드 진행 순서
  1. hashMap.keySet()메서드 호출 : 모든 key 값이 Set 객체로 반환
  2. iterator()메서드 호출 : key를 순회 할 수 있는 Iterator 객체 반환 
  3. 모든 key 값을 하나씩 순회하면서 get()메서드를 사용하여 해당하는 value 값을 가져옴
- values()메서드를 사용하면 key 값 없이 모든 value값을 Colelction 자료형으로 반환
  - value는 중복 가능하기 때문

<br/>

### TreeMap Class

- Map Interface를 구현한 클래스 중 key 값으로 자료를 정렬할 때 사용
  - TreeSet과 마찬가지로 이진 검색 트리로 구현 됨
  - key 값에 해당하는 클래스에 Comparable이나 Comparator 인터페이스를 구현하여 사용
- Interger 클래스에 구현되어 있는 Comparable 인터페이스 예시

```java
public final class Integer extends Number implements Comparable<integer> {
...
	public int compareTo(Integer anotherInteger) {
		return compare(this.value, anotherInteger.value);
	}
...
```


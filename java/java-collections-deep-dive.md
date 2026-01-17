# Java Collections 심화

HashMap 내부 구조와 ConcurrentHashMap의 동작 원리를 정리합니다.

## 목차

1. [HashMap 내부 구조](#1-hashmap-내부-구조)
2. [해시 충돌 처리](#2-해시-충돌-처리)
3. [ConcurrentHashMap](#3-concurrenthashmap)
4. [List 구현체 비교](#4-list-구현체-비교)
5. [Set 구현체 비교](#5-set-구현체-비교)
6. [선택 가이드](#6-선택-가이드)

---

## 1. HashMap 내부 구조

### 기본 구조

```
HashMap = 배열(버킷) + 연결 리스트/트리

┌─────────────────────────────────────────────────────────────┐
│  table[] (Node<K,V>[])                                      │
│                                                             │
│  Index 0: null                                              │
│  Index 1: [Key1, Val1] → [Key5, Val5] → null               │
│  Index 2: null                                              │
│  Index 3: [Key2, Val2] → null                              │
│  Index 4: [Key3, Val3] → (TreeNode로 변환)                  │
│  ...                                                        │
│  Index 15: [Key4, Val4] → null                             │
│                                                             │
│  기본 capacity: 16                                          │
│  load factor: 0.75                                          │
│  threshold: 16 * 0.75 = 12 (리사이징 임계점)                │
└─────────────────────────────────────────────────────────────┘
```

### 인덱스 계산

```java
// 버킷 인덱스 계산
int hash = key.hashCode();
int index = hash & (table.length - 1);  // 비트 연산으로 모듈러

// 왜 & 연산?
// table.length는 항상 2의 거듭제곱
// 2^n - 1 = 모든 비트가 1 (예: 15 = 1111)
// hash & 1111 = 하위 4비트만 사용 = 0~15 범위
```

### Node 구조

```java
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;
    final K key;
    V value;
    Node<K,V> next;  // 연결 리스트

    Node(int hash, K key, V value, Node<K,V> next) {
        this.hash = hash;
        this.key = key;
        this.value = value;
        this.next = next;
    }
}
```

---

## 2. 해시 충돌 처리

### Separate Chaining

```
동일 버킷에 여러 엔트리 → 연결 리스트로 연결

put(key1, val1)  → index 5
put(key5, val5)  → index 5 (충돌!)

Index 5: [key1, val1] → [key5, val5] → null

검색 시:
1. index 계산
2. 연결 리스트 순회하며 key.equals() 비교
```

### Treeification (Java 8+)

```
충돌이 많으면 연결 리스트 → Red-Black Tree 변환

TREEIFY_THRESHOLD = 8   // 8개 이상이면 트리로
UNTREEIFY_THRESHOLD = 6 // 6개 이하면 리스트로
MIN_TREEIFY_CAPACITY = 64 // 테이블 크기 64 이상일 때만

성능:
- 연결 리스트: O(n)
- Red-Black Tree: O(log n)
```

### 리사이징

```java
// threshold 초과 시 2배 확장
void resize() {
    // 새 테이블 생성 (2배 크기)
    Node<K,V>[] newTab = new Node[oldCap * 2];

    // 모든 엔트리 재해싱
    for (Node<K,V> e : oldTab) {
        // 새 인덱스 계산 후 이동
    }
}

// 재해싱 비용이 크므로 초기 용량 설정 권장
Map<String, Integer> map = new HashMap<>(1000);
// 1000개 예상 시: 1000 / 0.75 = 약 1334
// 2의 거듭제곱: 2048
```

---

## 3. ConcurrentHashMap

### Java 7 vs Java 8

```
Java 7: Segment 기반
┌─────────────────────────────────────────────────────────────┐
│  Segment[] (각 Segment가 Lock 보유)                         │
│                                                             │
│  Segment 0 [Lock] → HashEntry[] → 연결 리스트              │
│  Segment 1 [Lock] → HashEntry[] → 연결 리스트              │
│  ...                                                        │
│  Segment 15 [Lock] → HashEntry[]                           │
│                                                             │
│  동시성 수준: 기본 16개 Segment                             │
└─────────────────────────────────────────────────────────────┘

Java 8+: Node 배열 + CAS + synchronized
┌─────────────────────────────────────────────────────────────┐
│  Node<K,V>[] table                                          │
│                                                             │
│  각 버킷별로 독립적 잠금                                    │
│  - 빈 버킷: CAS로 삽입                                      │
│  - 충돌 버킷: synchronized(head) 사용                       │
│                                                             │
│  더 세밀한 잠금 → 높은 동시성                               │
└─────────────────────────────────────────────────────────────┘
```

### 주요 연산

```java
// put 연산 (Java 8+)
public V put(K key, V value) {
    // 1. 해시 계산
    int hash = spread(key.hashCode());

    // 2. 버킷이 비어있으면 CAS로 삽입
    if (tabAt(tab, i) == null) {
        if (casTabAt(tab, i, null, new Node<>(hash, key, value)))
            break;  // 성공
    }

    // 3. 버킷에 노드가 있으면 synchronized
    else {
        synchronized (f) {  // 해당 버킷의 head 노드
            // 연결 리스트 또는 트리에 삽입
        }
    }
}

// get 연산 - Lock 없음!
public V get(Object key) {
    // volatile 읽기로 최신 값 보장
    Node<K,V> e = tabAt(tab, i);
    // 리스트/트리 순회
    return e.value;
}
```

### HashMap vs ConcurrentHashMap

| 특성 | HashMap | ConcurrentHashMap |
|------|---------|-------------------|
| Thread-Safe | X | O |
| null 키/값 | O | X |
| 순회 중 수정 | ConcurrentModificationException | 안전 |
| 성능 (단일 스레드) | 빠름 | 약간 느림 |
| 성능 (멀티 스레드) | 동기화 필요 | 우수 |

---

## 4. List 구현체 비교

### ArrayList

```
내부: 동적 배열

┌─────────────────────────────────────────────────────────────┐
│  Object[] elementData                                       │
│  [E0][E1][E2][E3][E4][null][null][null]                    │
│   0   1   2   3   4    5     6     7                       │
│                                                             │
│  size = 5                                                   │
│  capacity = 8                                               │
└─────────────────────────────────────────────────────────────┘

성능:
- get(index): O(1) - 랜덤 액세스
- add(끝): O(1) 평균, O(n) 리사이징 시
- add(중간): O(n) - 시프트 필요
- remove(중간): O(n) - 시프트 필요
```

### LinkedList

```
내부: 이중 연결 리스트

┌──────┐   ┌──────┐   ┌──────┐   ┌──────┐
│ Node │←→│ Node │←→│ Node │←→│ Node │
│  E0  │   │  E1  │   │  E2  │   │  E3  │
└──────┘   └──────┘   └──────┘   └──────┘
  ↑ first                          last ↑

성능:
- get(index): O(n) - 순회 필요
- add(끝): O(1)
- add(중간): O(1) - 노드 찾은 후
- remove(중간): O(1) - 노드 찾은 후
- 노드 찾기: O(n)
```

### 비교

| 연산 | ArrayList | LinkedList |
|------|-----------|------------|
| get(i) | O(1) | O(n) |
| add(끝) | O(1) 평균 | O(1) |
| add(처음) | O(n) | O(1) |
| add(중간) | O(n) | O(n) |
| remove(중간) | O(n) | O(n) |
| 메모리 | 낮음 | 높음 (노드 오버헤드) |

---

## 5. Set 구현체 비교

### HashSet

```java
// 내부적으로 HashMap 사용
private transient HashMap<E, Object> map;
private static final Object PRESENT = new Object();

public boolean add(E e) {
    return map.put(e, PRESENT) == null;
}

// 특징: 순서 없음, O(1) 성능
```

### LinkedHashSet

```java
// 내부적으로 LinkedHashMap 사용
// 삽입 순서 유지

Set<String> set = new LinkedHashSet<>();
set.add("B");
set.add("A");
set.add("C");
// 순회: B, A, C (삽입 순서)
```

### TreeSet

```java
// 내부적으로 TreeMap 사용 (Red-Black Tree)
// 정렬된 순서 유지

Set<String> set = new TreeSet<>();
set.add("B");
set.add("A");
set.add("C");
// 순회: A, B, C (자연 순서)

// 시간 복잡도: O(log n)
```

---

## 6. 선택 가이드

### Map 선택

```
단일 스레드:
- 대부분의 경우 → HashMap
- 순서 필요 → LinkedHashMap
- 정렬 필요 → TreeMap

멀티 스레드:
- 높은 동시성 → ConcurrentHashMap
- 전체 동기화 → Collections.synchronizedMap()
```

### List 선택

```
랜덤 액세스 빈번 → ArrayList
삽입/삭제 빈번 (처음/끝) → LinkedList
Thread-Safe 필요 → CopyOnWriteArrayList
```

### Set 선택

```
빠른 조회 → HashSet
삽입 순서 유지 → LinkedHashSet
정렬 필요 → TreeSet
Thread-Safe → ConcurrentSkipListSet, CopyOnWriteArraySet
```

### 초기 용량 설정

```java
// 예상 크기를 알 때 초기 용량 설정
int expectedSize = 1000;

// HashMap: size / loadFactor
Map<K, V> map = new HashMap<>(expectedSize * 4 / 3 + 1);

// ArrayList
List<E> list = new ArrayList<>(expectedSize);
```

---

## 핵심 정리

| 자료구조 | 내부 구조 | 특징 |
|----------|----------|------|
| HashMap | 배열 + 연결리스트/트리 | O(1), 순서 없음 |
| ConcurrentHashMap | 버킷별 Lock/CAS | Thread-Safe |
| ArrayList | 동적 배열 | O(1) 랜덤 액세스 |
| LinkedList | 이중 연결 리스트 | O(1) 삽입/삭제 |
| HashSet | HashMap 래핑 | 중복 없음 |
| TreeSet | Red-Black Tree | 정렬됨 |

---

*마지막 업데이트: 2025년 01월*

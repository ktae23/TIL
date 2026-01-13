# 재귀함수 (Recursion)

> 관련 코드: `section2_basic/recursion/RecursionBasic.java`

---

## 1. 함수란?

함수는 특정 작업을 수행하는 코드 블록입니다.
- 입력(매개변수)을 받아서
- 정해진 작업을 수행하고
- 결과(반환값)를 돌려줌

---

## 2. 재귀함수란?

**재귀함수는 자기 자신을 호출하는 함수**입니다.

### 재귀함수의 구조
```
function(input) {
    if (종료 조건) {           ← Base Case (기저 조건)
        return 값;
    }
    return function(변형된 input);  ← Recursive Case (재귀 호출)
}
```

---

## 3. Base Case vs Recursive Case

### Base Case (기저 조건)
- 재귀 호출을 **멈추는 조건**
- 이것이 없으면 무한 루프에 빠짐 (StackOverflowError)
- 가장 작은 문제의 답을 직접 반환

### Recursive Case (재귀 호출)
- 문제를 **더 작은 문제로 분할**
- 자기 자신을 호출하여 작은 문제 해결
- 입력값이 Base Case를 향해 변해야 함

---

## 4. 재귀의 동작 원리

예: `sum(3)` 호출 시

```
sum(3)
  └─ 3 + sum(2)
         └─ 2 + sum(1)
                └─ 1 + sum(0)
                       └─ return 0  (Base Case)
                └─ return 1 + 0 = 1
         └─ return 2 + 1 = 3
  └─ return 3 + 3 = 6

결과: 6
```

---

## 5. 재귀 vs 반복문

### 재귀의 장점
- 코드가 간결하고 직관적
- 복잡한 문제를 단순하게 표현 가능
- 트리 구조, 분할 정복에 적합

### 재귀의 단점
- 함수 호출 오버헤드 존재
- 스택 메모리 사용 (깊은 재귀 시 StackOverflow)
- 디버깅이 어려울 수 있음

> 모든 재귀는 반복문으로 변환 가능하며, 그 역도 성립합니다.

---

## 6. 대표 예제

### 1부터 n까지의 합
```java
// 점화식: sum(n) = n + sum(n-1)
// Base Case: sum(0) = 0
public static int sum(int n) {
    if (n == 0) return 0;           // Base Case
    return n + sum(n - 1);          // Recursive Case
}
```

### 팩토리얼 (n!)
```java
// 점화식: n! = n × (n-1)!
// Base Case: 0! = 1
public static long factorial(int n) {
    if (n == 0) return 1;
    return n * factorial(n - 1);
}
```

### 거듭제곱 (x^n)
```java
// 단순 버전: O(n)
public static long power(int x, int n) {
    if (n == 0) return 1;
    return x * power(x, n - 1);
}

// 최적화 버전: O(log n) - 분할 정복
public static long powerFast(int x, int n) {
    if (n == 0) return 1;
    long half = powerFast(x, n / 2);
    if (n % 2 == 0) return half * half;
    else return x * half * half;
}
```

### 문자열 뒤집기
```java
public static String reverse(String s) {
    if (s.isEmpty()) return "";
    return reverse(s.substring(1)) + s.charAt(0);
}
```

### 하노이 탑
```java
public static void hanoi(int n, char from, char to, char aux) {
    if (n == 1) {
        System.out.println(from + " -> " + to);
        return;
    }
    hanoi(n - 1, from, aux, to);    // n-1개를 보조 기둥으로
    System.out.println(from + " -> " + to);  // 가장 큰 원판 이동
    hanoi(n - 1, aux, to, from);    // n-1개를 목적지로
}
```

---

## 핵심 정리

1. **Base Case를 먼저 정의**하라 (종료 조건)
2. **Recursive Case에서 문제를 작게** 만들어라
3. 입력이 **반드시 Base Case를 향해** 변해야 함
4. 깊은 재귀는 **StackOverflow** 주의

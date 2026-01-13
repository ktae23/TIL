# 브루트포스 / 완전탐색 (Brute Force)

> 관련 코드: `section3_essential1/bruteforce/BruteForceBasic.java`

---

## 1. 브루트포스란?

**"무차별 대입"**이라는 뜻으로, 가능한 **모든 경우의 수를 탐색**하여 정답을 찾는 방법입니다.

### 특징
- **항상 정답을 찾을 수 있음** (완전탐색)
- 구현이 단순하고 직관적
- 경우의 수가 많으면 **시간 초과**

---

## 2. 브루트포스 적용 조건

### 시간 제한 1초 기준

| 복잡도 | n의 범위 |
|--------|----------|
| O(n) | n ≤ 1억 |
| O(n²) | n ≤ 1만 |
| O(n³) | n ≤ 500 |
| O(2ⁿ) | n ≤ 20 |
| O(n!) | n ≤ 10 |

> 문제에서 **n의 범위를 확인**하고 시간 복잡도를 계산해야 합니다.

---

## 3. 브루트포스 유형

### 1) 단순 반복문
- 1중, 2중, 3중 반복문
- 예: 두 수의 합이 K인 경우 찾기

### 2) 순열/조합
- 순서가 중요하면 순열, 아니면 조합
- 예: N개 중 R개를 선택하는 모든 경우

### 3) 비트마스크
- 2^n개의 부분집합 탐색
- 예: 부분집합의 합 문제

### 4) 재귀/백트래킹
- 가지치기로 탐색 공간 줄이기
- 예: N-Queen, 스도쿠

---

## 4. 가지치기 (Pruning)

**불필요한 탐색을 미리 중단**하는 기법
- 조건에 맞지 않으면 더 이상 탐색하지 않음
- 탐색 공간을 크게 줄일 수 있음

---

## 5. 구현 예제

### 두 수의 합 - O(n²)
```java
public static void findTwoSum(int[] arr, int target) {
    int n = arr.length;
    for (int i = 0; i < n; i++) {
        for (int j = i + 1; j < n; j++) {
            if (arr[i] + arr[j] == target) {
                System.out.println("(" + arr[i] + ", " + arr[j] + ")");
            }
        }
    }
}
```

### 세 수의 합 - O(n³)
```java
public static void findThreeSum(int[] arr) {
    int n = arr.length;
    for (int i = 0; i < n; i++) {
        for (int j = i + 1; j < n; j++) {
            for (int k = j + 1; k < n; k++) {
                if (arr[i] + arr[j] + arr[k] == 0) {
                    System.out.println("(" + arr[i] + ", " + arr[j] + ", " + arr[k] + ")");
                }
            }
        }
    }
}
```

### 부분집합의 합 - 비트마스크 O(2^n)
```java
public static void subsetSum(int[] arr, int target) {
    int n = arr.length;

    // 0부터 2^n - 1까지 모든 비트마스크
    for (int mask = 0; mask < (1 << n); mask++) {
        int sum = 0;
        for (int i = 0; i < n; i++) {
            if ((mask & (1 << i)) != 0) {  // i번째 비트가 켜져 있으면
                sum += arr[i];
            }
        }
        if (sum == target) {
            // 해당 부분집합 출력
        }
    }
}
```

### 순열 생성 - O(n!)
```java
public static void generatePermutations(int[] arr, int depth) {
    if (depth == arr.length) {
        // 순열 완성
        return;
    }

    for (int i = 0; i < arr.length; i++) {
        if (!used[i]) {
            used[i] = true;
            result[depth] = arr[i];
            generatePermutations(arr, depth + 1);
            used[i] = false;  // 백트래킹
        }
    }
}
```

### 백트래킹 (가지치기)
```java
public static void findCombinationsWithSum(int n, int k, int s, int start, int depth, int currentSum) {
    // 가지치기: 합이 이미 S를 초과
    if (currentSum > s) return;

    // Base Case: K개를 선택
    if (depth == k) {
        if (currentSum == s) countWays++;
        return;
    }

    // 가지치기: 남은 수로 K개를 못 채움
    if (n - start + 1 < k - depth) return;

    // Recursive Case
    for (int i = start; i <= n; i++) {
        findCombinationsWithSum(n, k, s, i + 1, depth + 1, currentSum + i);
    }
}
```

---

## 핵심 정리

1. **n의 범위**를 먼저 확인하여 완전탐색 가능 여부 판단
2. 복잡도가 클 때는 **가지치기** 적용
3. 유형에 따라 **반복문/순열/조합/비트마스크** 선택
4. 브루트포스가 가능하면 가장 확실한 방법

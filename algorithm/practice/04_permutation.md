# 순열 (Permutation)

> 관련 코드: `section2_basic/permutation/PermutationBasic.java`

---

## 1. 순열이란?

**n개의 원소 중에서 r개를 순서 있게 나열**하는 경우의 수입니다.

### 기호
- nPr 또는 P(n, r)

### 공식
```
nPr = n! / (n-r)!
```

### 예시
{1, 2, 3}에서 2개를 순서대로 나열:
- (1,2), (1,3), (2,1), (2,3), (3,1), (3,2) → 총 6가지
- 3P2 = 3! / 1! = 6

---

## 2. 순열 vs 조합

| 구분 | 순열 (Permutation) | 조합 (Combination) |
|------|-------------------|-------------------|
| 순서 | 순서가 **의미 있음** | 순서가 **의미 없음** |
| 예시 | (1, 2)와 (2, 1)은 **다름** | {1, 2}와 {2, 1}은 **같음** |
| 공식 | nPr = n! / (n-r)! | nCr = n! / (r! × (n-r)!) |

### 관계
```
nPr = nCr × r!
```

---

## 3. 순열의 종류

### 1) 일반 순열
n개 중 r개를 선택하여 나열
```
nPr = n! / (n-r)!
```

### 2) 전체 순열
n개 모두를 나열
```
nPn = n!
```

### 3) 중복 순열
같은 원소를 여러 번 선택 가능
```
n^r
```

---

## 4. 구현 방법

### 방법 1: visited 배열
```java
public static void permutation(int[] arr, int[] result, boolean[] visited, int depth, int r) {
    // Base Case: r개를 모두 선택
    if (depth == r) {
        printArray(result, r);
        return;
    }

    // 모든 원소에 대해 시도
    for (int i = 0; i < arr.length; i++) {
        if (!visited[i]) {           // 아직 사용하지 않은 원소만
            visited[i] = true;        // 사용 표시
            result[depth] = arr[i];   // 현재 위치에 배치
            permutation(arr, result, visited, depth + 1, r);
            visited[i] = false;       // 사용 표시 해제 (백트래킹)
        }
    }
}
```

**핵심 아이디어:**
- 각 위치(depth)에 아직 사용하지 않은 원소를 배치
- **visited 배열**로 사용 여부 추적

### 방법 2: 스왑(Swap) 방식
```java
public static void permutationSwap(int[] arr, int depth, int r) {
    // Base Case
    if (depth == r) {
        printArray(arr, r);
        return;
    }

    // depth 위치에 i번째 원소를 놓아봄
    for (int i = depth; i < arr.length; i++) {
        swap(arr, depth, i);                    // 교환
        permutationSwap(arr, depth + 1, r);     // 다음 위치
        swap(arr, depth, i);                    // 복원
    }
}
```

**핵심 아이디어:**
- 배열의 원소들을 서로 교환하며 순열 생성
- 추가 배열 없이 **in-place**로 동작

### 방법 3: 중복 순열
```java
public static void permutationWithRepetition(int[] arr, int[] result, int depth, int r) {
    if (depth == r) {
        printArray(result, r);
        return;
    }

    // 모든 원소 선택 가능 (중복 허용)
    for (int i = 0; i < arr.length; i++) {
        result[depth] = arr[i];
        permutationWithRepetition(arr, result, depth + 1, r);
    }
}
```

---

## 5. 순열의 개수 계산

```java
public static long nPr(int n, int r) {
    long result = 1;
    for (int i = n; i > n - r; i--) {
        result *= i;
    }
    return result;
}
```

---

## 6. 예제 실행 결과

{1, 2, 3}에서 2개 나열:
```
[1, 2]
[1, 3]
[2, 1]
[2, 3]
[3, 1]
[3, 2]
```

중복 순열 {1, 2, 3}에서 2개:
```
[1, 1]
[1, 2]
[1, 3]
[2, 1]
[2, 2]
[2, 3]
[3, 1]
[3, 2]
[3, 3]
```

---

## 핵심 정리

1. 순열은 **순서가 있는 나열**
2. **visited 배열** 또는 **swap**으로 구현
3. 시간 복잡도: O(nPr) = O(n! / (n-r)!)
4. 중복 순열은 visited 검사 제거

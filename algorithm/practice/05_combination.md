# 5. 조합 (Combination)

> 관련 코드: `section2_basic/combination/CombinationBasic.java`

---

## 1. 조합이란?

**n개의 원소 중에서 r개를 순서 없이 선택**하는 경우의 수입니다.

### 기호
- nCr 또는 C(n, r) 또는 (n r)

### 공식
```
nCr = n! / (r! × (n-r)!)
```

### 예시
{1, 2, 3}에서 2개를 선택:
- {1, 2}, {1, 3}, {2, 3} → 총 3가지
- 3C2 = 3! / (2! × 1!) = 6 / 2 = 3

---

## 2. 조합 vs 순열

| 구분 | 조합 (Combination) | 순열 (Permutation) |
|------|-------------------|-------------------|
| 순서 | 순서가 **의미 없음** | 순서가 **의미 있음** |
| 예시 | {1, 2}와 {2, 1}은 **같음** | (1, 2)와 (2, 1)은 **다름** |
| 공식 | nCr = n! / (r! × (n-r)!) | nPr = n! / (n-r)! |

### 관계
```
nPr = nCr × r!
```

---

## 3. 조합의 성질

### 1) nC0 = nCn = 1
- 0개 선택하는 방법 = 1가지 (아무것도 안 고름)
- n개 모두 선택하는 방법 = 1가지

### 2) nCr = nC(n-r)
- r개를 고르는 것 = (n-r)개를 안 고르는 것

### 3) 파스칼 삼각형
```
nCr = (n-1)C(r-1) + (n-1)Cr
```
- 특정 원소를 포함하는 경우 + 포함하지 않는 경우

---

## 4. 구현 방법

### 방법 1: 재귀 (Backtracking)
```java
public static void combination(int[] arr, List<Integer> selected, int start, int r) {
    // Base Case: r개를 모두 선택
    if (r == 0) {
        System.out.println(selected);
        return;
    }

    // start부터 끝까지 각 원소를 선택
    for (int i = start; i < arr.length; i++) {
        selected.add(arr[i]);                    // 선택
        combination(arr, selected, i + 1, r - 1); // 다음 원소들 중에서 r-1개 선택
        selected.remove(selected.size() - 1);    // 선택 취소 (백트래킹)
    }
}
```

**핵심 아이디어:**
- 각 원소에 대해 "선택한다" 또는 "선택하지 않는다"
- **start 인덱스**부터 탐색하여 중복을 방지

### 방법 2: visited 배열
```java
public static void combinationWithVisited(int start, int depth, int r) {
    if (depth == r) {
        // visited[i] == true인 원소들 출력
        return;
    }

    for (int i = start; i < arr.length; i++) {
        visited[i] = true;
        combinationWithVisited(i + 1, depth + 1, r);
        visited[i] = false;
    }
}
```

### 방법 3: 비트마스크
```java
public static void combinationBitmask(int[] arr, int r) {
    int n = arr.length;

    // 0부터 2^n - 1까지 모든 비트마스크 확인
    for (int mask = 0; mask < (1 << n); mask++) {
        // 비트가 r개 켜진 것만 선택
        if (Integer.bitCount(mask) == r) {
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    System.out.print(arr[i] + " ");
                }
            }
            System.out.println();
        }
    }
}
```

---

## 5. 조합의 개수 계산

### 재귀 (파스칼 삼각형)
```java
public static long nCr(int n, int r) {
    if (r == 0 || r == n) return 1;
    return nCr(n - 1, r - 1) + nCr(n - 1, r);
}
```

### DP (효율적)
```java
public static long nCrDP(int n, int r) {
    long[][] dp = new long[n + 1][r + 1];

    for (int i = 0; i <= n; i++) {
        dp[i][0] = 1;
        for (int j = 1; j <= Math.min(i, r); j++) {
            if (i == j) dp[i][j] = 1;
            else dp[i][j] = dp[i - 1][j - 1] + dp[i - 1][j];
        }
    }

    return dp[n][r];
}
```

---

## 핵심 정리

1. 조합은 **순서가 없는 선택**
2. **start 인덱스**로 중복 방지
3. 시간 복잡도: O(nCr)
4. 파스칼 삼각형: `nCr = (n-1)C(r-1) + (n-1)Cr`

# 파라메트릭 서치 (Parametric Search)

> 관련 코드: `section5_essential2/parametric/ParametricSearchBasic.java`

---

## 1. 파라메트릭 서치란?

**최적화 문제를 결정 문제로 바꾸어** 이분 탐색으로 해결하는 기법입니다.

### 문제 변환
- **최적화 문제**: "최댓값/최솟값을 구하라"
- **결정 문제**: "주어진 값이 조건을 만족하는가?" → Yes/No

### 예시
- **최적화**: "가장 큰 X를 구하라"
- **결정**: "X가 가능한가?" → Yes/No

---

## 2. 적용 조건

결정 함수 f(x)가 **단조성**을 가져야 함:
- f(x)가 True이면, x보다 작은(또는 큰) 모든 값도 True
- **경계값**을 이분 탐색으로 찾을 수 있음

### 예시
"최소 X 이상 가능한가?"
```
x=5 불가능, x=6 불가능, x=7 가능, x=8 가능 ...
→ 경계값 7을 이분 탐색으로 찾기
```

---

## 3. 구현 패턴

```java
while (left < right) {
    mid = (left + right) / 2;  // 또는 (left + right + 1) / 2
    if (check(mid)) {
        // 조건에 따라 범위 조정
    } else {
        // 반대쪽으로 범위 조정
    }
}
```

### 최댓값을 찾을 때
```java
while (left < right) {
    long mid = (left + right + 1) / 2;  // 올림
    if (canMake(mid)) {
        left = mid;       // 가능하면 더 큰 값 시도
    } else {
        right = mid - 1;  // 불가능하면 더 작은 값
    }
}
```

### 최솟값을 찾을 때
```java
while (left < right) {
    long mid = (left + right) / 2;  // 내림
    if (canMake(mid)) {
        right = mid;      // 가능하면 더 작은 값 시도
    } else {
        left = mid + 1;   // 불가능하면 더 큰 값
    }
}
```

---

## 4. 대표 문제 유형

### 1) "최댓값의 최솟값, 최솟값의 최댓값" 유형
- 예: "최대 거리의 최솟값을 구하라"

### 2) "K개 이상 만들 수 있는 최대 길이"
- 예: 나무 자르기, 랜선 자르기

### 3) "조건을 만족하는 최소/최대 값"
- 예: 공유기 설치, 입국심사

---

## 5. 구현 예제

### 랜선 자르기
```java
// K개의 랜선으로 N개 이상의 같은 길이 랜선을 만들 때 최대 길이
public static long maxLanLength(int[] lans, int need) {
    long left = 1;
    long right = 0;
    for (int lan : lans) right = Math.max(right, lan);

    while (left < right) {
        long mid = (left + right + 1) / 2;  // 올림 (최댓값 찾기)

        if (canMake(lans, mid, need)) {
            left = mid;       // 가능하면 더 큰 값
        } else {
            right = mid - 1;  // 불가능하면 더 작은 값
        }
    }
    return left;
}

private static boolean canMake(int[] lans, long length, int need) {
    long count = 0;
    for (int lan : lans) count += lan / length;
    return count >= need;
}
```

### 나무 자르기
```java
// 높이 H로 자를 때 M 미터 이상 얻을 수 있는 H의 최댓값
public static long maxCutHeight(int[] trees, long need) {
    long left = 0;
    long right = 0;
    for (int tree : trees) right = Math.max(right, tree);

    while (left < right) {
        long mid = (left + right + 1) / 2;

        if (getWood(trees, mid) >= need) {
            left = mid;
        } else {
            right = mid - 1;
        }
    }
    return left;
}
```

### 공유기 설치 (최소 거리 최대화)
```java
public static int maxMinDistance(int[] positions, int k) {
    Arrays.sort(positions);

    int left = 1;
    int right = positions[positions.length - 1] - positions[0];

    while (left < right) {
        int mid = (left + right + 1) / 2;

        if (canPlace(positions, mid, k)) {
            left = mid;
        } else {
            right = mid - 1;
        }
    }
    return left;
}

private static boolean canPlace(int[] positions, int minDist, int k) {
    int count = 1;
    int lastPos = positions[0];

    for (int i = 1; i < positions.length; i++) {
        if (positions[i] - lastPos >= minDist) {
            count++;
            lastPos = positions[i];
        }
    }
    return count >= k;
}
```

---

## 핵심 정리

1. **최적화 문제 → 결정 문제**로 변환
2. 결정 함수가 **단조성**을 가지는지 확인
3. 최댓값 찾기: `(left + right + 1) / 2` (올림)
4. 최솟값 찾기: `(left + right) / 2` (내림)

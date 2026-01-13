# 이분 탐색 (Binary Search)

> 관련 코드: `section5_essential2/binarysearch/BinarySearchBasic.java`

---

## 1. 이분 탐색이란?

**정렬된 배열**에서 특정 값을 찾는 **O(log n)** 알고리즘입니다.

### 핵심 아이디어
1. 중간 값과 목표 값을 비교
2. 목표가 중간보다 작으면 **왼쪽 절반** 탐색
3. 목표가 중간보다 크면 **오른쪽 절반** 탐색
4. 매 단계마다 탐색 범위가 **절반**으로 줄어듦

---

## 2. 이분 탐색 조건

> **필수 조건: 배열이 정렬되어 있어야 함**

---

## 3. 시간 복잡도

| 연산 | 복잡도 |
|------|--------|
| 탐색 | O(log n) |
| 정렬 후 탐색 | O(n log n) + O(log n) = O(n log n) |

---

## 4. Lower Bound vs Upper Bound

### Lower Bound
- target **이상**인 첫 번째 위치
- target이 없으면 target보다 큰 첫 번째 위치

### Upper Bound
- target **초과**인 첫 번째 위치
- target이 여러 개면 마지막 target **다음** 위치

### 예시
```
arr = [1, 2, 2, 2, 3, 4], target = 2
- Lower Bound: index 1 (첫 번째 2)
- Upper Bound: index 4 (3의 위치)
- 2의 개수 = Upper - Lower = 4 - 1 = 3
```

---

## 5. 구현

### 기본 이분 탐색
```java
public static int binarySearch(int[] arr, int target) {
    int left = 0;
    int right = arr.length - 1;

    while (left <= right) {
        int mid = left + (right - left) / 2;  // 오버플로우 방지

        if (arr[mid] == target) {
            return mid;  // 찾음
        } else if (arr[mid] < target) {
            left = mid + 1;  // 오른쪽 절반
        } else {
            right = mid - 1;  // 왼쪽 절반
        }
    }

    return -1;  // 못 찾음
}
```

### Lower Bound
```java
public static int lowerBound(int[] arr, int target) {
    int left = 0;
    int right = arr.length;

    while (left < right) {
        int mid = left + (right - left) / 2;

        if (arr[mid] < target) {
            left = mid + 1;
        } else {
            right = mid;  // arr[mid] >= target이면 후보
        }
    }

    return left;
}
```

### Upper Bound
```java
public static int upperBound(int[] arr, int target) {
    int left = 0;
    int right = arr.length;

    while (left < right) {
        int mid = left + (right - left) / 2;

        if (arr[mid] <= target) {
            left = mid + 1;
        } else {
            right = mid;  // arr[mid] > target이면 후보
        }
    }

    return left;
}
```

### target 개수 세기
```java
public static int countTarget(int[] arr, int target) {
    return upperBound(arr, target) - lowerBound(arr, target);
}
```

---

## 6. Java 내장 함수

```java
int[] arr = {1, 2, 3, 4, 5, 6, 7, 8, 9};

// Arrays.binarySearch: 있으면 인덱스, 없으면 -(삽입위치+1)
int result1 = Arrays.binarySearch(arr, 5);  // 4
int result2 = Arrays.binarySearch(arr, 10); // -10

// 삽입 위치 구하기 (없는 경우)
if (result2 < 0) {
    int insertionPoint = -(result2 + 1);  // 9
}
```

---

## 핵심 정리

1. **정렬된 배열**에서만 사용 가능
2. `left + (right - left) / 2`로 **오버플로우 방지**
3. Lower/Upper Bound는 **경계 조건** 주의
4. 시간 복잡도: **O(log n)**

# 0. 시간 복잡도와 공간 복잡도

> 관련 코드: `section2_basic/complexity/TimeSpaceComplexity.java`

---

## 1. 시간 복잡도 (Time Complexity)

### 정의
- 알고리즘이 실행되는 데 걸리는 시간을 입력 크기(n)에 대한 함수로 표현
- 실제 실행 시간이 아닌 **연산 횟수**를 기준으로 측정
- **Big-O 표기법**을 사용하여 최악의 경우를 표현

### 주요 시간 복잡도 (빠른 순서)

| 복잡도 | 명칭 | 설명 | 예시 |
|--------|------|------|------|
| O(1) | 상수 시간 | 입력 크기와 무관 | 배열 인덱스 접근 |
| O(log n) | 로그 시간 | 매번 절반씩 줄어듦 | 이분 탐색 |
| O(n) | 선형 시간 | 입력에 비례 | 단순 반복문 |
| O(n log n) | 선형 로그 시간 | 효율적인 정렬 | 병합 정렬, 퀵 정렬 |
| O(n²) | 이차 시간 | 이중 반복문 | 버블 정렬 |
| O(n³) | 삼차 시간 | 삼중 반복문 | 플로이드-워셜 |
| O(2ⁿ) | 지수 시간 | 부분집합 | 비효율적 재귀 |
| O(n!) | 팩토리얼 시간 | 모든 순열 | 순열 완전탐색 |

---

## 2. 공간 복잡도 (Space Complexity)

### 정의
- 알고리즘이 실행되는 데 필요한 **메모리 공간**을 입력 크기(n)에 대한 함수로 표현
- 변수, 배열, 재귀 호출 스택 등을 고려

### 예시
- **O(1)**: 변수 몇 개만 사용
- **O(n)**: 입력 크기만큼 배열 생성
- **O(n)**: 재귀 호출 스택 (depth n)

---

## 3. 코딩 테스트에서의 활용

### 시간 제한 1초 기준

| n의 범위 | 가능한 복잡도 |
|----------|---------------|
| n ≤ 10 | O(n!) 가능 |
| n ≤ 20 | O(2ⁿ) 가능 |
| n ≤ 500 | O(n³) 가능 |
| n ≤ 5,000 | O(n²) 가능 |
| n ≤ 100,000 | O(n log n) 필요 |
| n ≤ 10,000,000 | O(n) 필요 |
| 그 이상 | O(log n) 또는 O(1) 필요 |

### 메모리 제한 (보통 256MB 기준)

| 자료형 | 최대 개수 |
|--------|-----------|
| int 배열 | 약 6천만 개 |
| long 배열 | 약 3천만 개 |
| int[][] 2차원 배열 | 약 7,000 × 7,000 |

---

## 4. 코드 예시

### O(1) - 상수 시간
```java
public static int constantTime(int[] arr) {
    return arr[0];  // 배열 크기와 무관
}
```

### O(log n) - 로그 시간
```java
public static int logarithmicTime(int[] arr, int target) {
    int left = 0, right = arr.length - 1;
    while (left <= right) {
        int mid = (left + right) / 2;
        if (arr[mid] == target) return mid;
        else if (arr[mid] < target) left = mid + 1;
        else right = mid - 1;
    }
    return -1;
}
```

### O(n) - 선형 시간
```java
public static int linearTime(int[] arr) {
    int sum = 0;
    for (int num : arr) {  // n번 반복
        sum += num;
    }
    return sum;
}
```

### O(n²) - 이차 시간
```java
public static void quadraticTime(int[] arr) {
    int n = arr.length;
    for (int i = 0; i < n - 1; i++) {          // n번
        for (int j = 0; j < n - 1 - i; j++) {  // n번
            // 버블 정렬 교환
        }
    }
}
```

---

## 핵심 정리

1. **문제의 n 범위를 확인**하고 적절한 알고리즘 선택
2. 시간 복잡도는 **최악의 경우** 기준
3. 공간 복잡도도 함께 고려 (특히 재귀 사용 시)

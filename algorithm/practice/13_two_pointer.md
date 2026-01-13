# 13. 투 포인터 (Two Pointer)

> 관련 코드: `section5_essential2/twopointer/TwoPointerBasic.java`

---

## 1. 투 포인터란?

**두 개의 포인터**를 사용하여 배열이나 리스트를 탐색하는 기법입니다.

### 특징
- **O(n²)을 O(n)으로** 줄일 수 있음
- 정렬된 배열에서 특히 유용
- 연속된 구간을 다룰 때 효과적

---

## 2. 투 포인터 유형

### 1) 같은 방향 (슬라이딩 윈도우)
- left, right가 **같은 방향**으로 이동
- 연속된 부분 배열 문제

### 2) 반대 방향
- left는 **왼쪽**에서, right는 **오른쪽**에서
- 두 수의 합 문제

---

## 3. 시간 복잡도

- 각 포인터가 배열을 **한 번씩만** 순회: **O(n)**
- 브루트포스 O(n²)보다 효율적

---

## 4. 구현 예제

### 두 수의 합 (정렬된 배열)
```java
public static int[] twoSum(int[] arr, int target) {
    int left = 0;
    int right = arr.length - 1;

    while (left < right) {
        int sum = arr[left] + arr[right];

        if (sum == target) {
            return new int[]{left, right};
        } else if (sum < target) {
            left++;   // 합이 작으면 왼쪽 포인터 증가
        } else {
            right--;  // 합이 크면 오른쪽 포인터 감소
        }
    }

    return new int[]{-1, -1};  // 찾지 못함
}
```

### 부분합 (합이 S 이상인 최소 길이)
```java
public static int minSubArrayLen(int[] arr, int target) {
    int n = arr.length;
    int left = 0;
    int sum = 0;
    int minLen = Integer.MAX_VALUE;

    for (int right = 0; right < n; right++) {
        sum += arr[right];

        // 합이 target 이상이면 왼쪽 포인터 이동
        while (sum >= target) {
            minLen = Math.min(minLen, right - left + 1);
            sum -= arr[left];
            left++;
        }
    }

    return minLen == Integer.MAX_VALUE ? 0 : minLen;
}
```

### 세 수의 합 (3Sum)
```java
// 정렬 후 첫 번째 수를 고정하고 나머지에 투 포인터
public static void threeSum(int[] arr, int target) {
    Arrays.sort(arr);
    int n = arr.length;

    for (int i = 0; i < n - 2; i++) {
        if (i > 0 && arr[i] == arr[i - 1]) continue;  // 중복 건너뛰기

        int left = i + 1;
        int right = n - 1;

        while (left < right) {
            int sum = arr[i] + arr[left] + arr[right];

            if (sum == target) {
                System.out.println("(" + arr[i] + ", " + arr[left] + ", " + arr[right] + ")");

                while (left < right && arr[left] == arr[left + 1]) left++;
                while (left < right && arr[right] == arr[right - 1]) right--;

                left++;
                right--;
            } else if (sum < target) {
                left++;
            } else {
                right--;
            }
        }
    }
}
```

### 물 담기 (Container With Most Water)
```java
public static int maxWater(int[] heights) {
    int left = 0;
    int right = heights.length - 1;
    int maxArea = 0;

    while (left < right) {
        int width = right - left;
        int height = Math.min(heights[left], heights[right]);
        int area = width * height;
        maxArea = Math.max(maxArea, area);

        // 높이가 낮은 쪽을 이동
        if (heights[left] < heights[right]) {
            left++;
        } else {
            right--;
        }
    }

    return maxArea;
}
```

---

## 5. 슬라이딩 윈도우 패턴

```java
int left = 0;
int sum = 0;

for (int right = 0; right < n; right++) {
    sum += arr[right];  // 오른쪽 확장

    while (조건) {
        // 왼쪽 축소
        sum -= arr[left];
        left++;
    }
}
```

---

## 핵심 정리

1. **같은 방향**: 연속 구간 문제 (슬라이딩 윈도우)
2. **반대 방향**: 정렬된 배열의 두 수 문제
3. 시간 복잡도: O(n²) → **O(n)**
4. 정렬이 필요한 경우가 많음

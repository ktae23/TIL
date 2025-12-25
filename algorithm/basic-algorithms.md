# 기초 알고리즘

알고리즘의 기초를 다지는 핵심 개념들을 정리합니다. 각 알고리즘의 원리, 구현 방법, 시간/공간 복잡도를 이해하는 것이 중요합니다.

## 목차
1. [정렬 알고리즘](#정렬-알고리즘)
2. [탐색 알고리즘](#탐색-알고리즘)
3. [브루트포스](#브루트포스)
4. [순열과 조합](#순열과-조합)

---

## 정렬 알고리즘

### 1. 버블 정렬 (Bubble Sort)

**개념**: 인접한 두 원소를 비교하여 큰 값을 뒤로 보내는 과정을 반복합니다. 마치 거품이 수면으로 올라오는 것처럼 가장 큰 값이 배열의 끝으로 이동합니다.

**시간 복잡도**: O(n²)
**공간 복잡도**: O(1) - 제자리 정렬

```java
public class BubbleSort {
    public static void bubbleSort(int[] arr) {
        int n = arr.length;

        // 외부 루프: n-1번 반복
        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;

            // 내부 루프: 인접한 원소 비교
            for (int j = 0; j < n - 1 - i; j++) {
                if (arr[j] > arr[j + 1]) {
                    // 교환
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                    swapped = true;
                }
            }

            // 최적화: 한 번도 교환이 일어나지 않으면 이미 정렬됨
            if (!swapped) break;
        }
    }

    public static void main(String[] args) {
        int[] arr = {64, 34, 25, 12, 22, 11, 90};
        bubbleSort(arr);
        System.out.println(Arrays.toString(arr));
        // 출력: [11, 12, 22, 25, 34, 64, 90]
    }
}
```

### 2. 선택 정렬 (Selection Sort)

**개념**: 매 단계마다 남은 원소 중 최솟값을 찾아 맨 앞의 원소와 교환합니다. 정렬된 부분과 정렬되지 않은 부분을 나누어 진행합니다.

**시간 복잡도**: O(n²)
**공간 복잡도**: O(1) - 제자리 정렬

```java
public class SelectionSort {
    public static void selectionSort(int[] arr) {
        int n = arr.length;

        for (int i = 0; i < n - 1; i++) {
            // 최솟값의 인덱스 찾기
            int minIdx = i;
            for (int j = i + 1; j < n; j++) {
                if (arr[j] < arr[minIdx]) {
                    minIdx = j;
                }
            }

            // 최솟값을 현재 위치와 교환
            int temp = arr[minIdx];
            arr[minIdx] = arr[i];
            arr[i] = temp;
        }
    }

    public static void main(String[] args) {
        int[] arr = {64, 25, 12, 22, 11};
        selectionSort(arr);
        System.out.println(Arrays.toString(arr));
        // 출력: [11, 12, 22, 25, 64]
    }
}
```

### 3. 삽입 정렬 (Insertion Sort)

**개념**: 카드 게임에서 카드를 정렬하는 방식과 유사합니다. 각 원소를 이미 정렬된 부분의 적절한 위치에 삽입합니다.

**시간 복잡도**:
- 최선: O(n) - 이미 정렬된 경우
- 평균/최악: O(n²)

**공간 복잡도**: O(1) - 제자리 정렬

```java
public class InsertionSort {
    public static void insertionSort(int[] arr) {
        int n = arr.length;

        for (int i = 1; i < n; i++) {
            int key = arr[i];
            int j = i - 1;

            // key보다 큰 원소들을 한 칸씩 뒤로 이동
            while (j >= 0 && arr[j] > key) {
                arr[j + 1] = arr[j];
                j--;
            }

            // key를 적절한 위치에 삽입
            arr[j + 1] = key;
        }
    }

    public static void main(String[] args) {
        int[] arr = {12, 11, 13, 5, 6};
        insertionSort(arr);
        System.out.println(Arrays.toString(arr));
        // 출력: [5, 6, 11, 12, 13]
    }
}
```

---

## 탐색 알고리즘

### 1. 선형 탐색 (Linear Search)

**개념**: 배열의 처음부터 끝까지 순차적으로 탐색합니다. 가장 단순하지만 느린 탐색 방법입니다.

**시간 복잡도**: O(n)
**공간 복잡도**: O(1)

```java
public class LinearSearch {
    public static int linearSearch(int[] arr, int target) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == target) {
                return i; // 찾은 경우 인덱스 반환
            }
        }
        return -1; // 못 찾은 경우 -1 반환
    }

    public static void main(String[] args) {
        int[] arr = {10, 23, 45, 70, 11, 15};
        int target = 70;
        int result = linearSearch(arr, target);

        if (result != -1) {
            System.out.println("원소를 인덱스 " + result + "에서 찾았습니다.");
        } else {
            System.out.println("원소를 찾지 못했습니다.");
        }
    }
}
```

### 2. 이진 탐색 (Binary Search)

**개념**: **정렬된 배열**에서 중간값과 비교하며 탐색 범위를 절반씩 줄여나갑니다. 매우 효율적인 탐색 알고리즘입니다.

**전제 조건**: 배열이 정렬되어 있어야 함
**시간 복잡도**: O(log n)
**공간 복잡도**: O(1) - 반복문 / O(log n) - 재귀

```java
public class BinarySearch {
    // 반복문 방식
    public static int binarySearchIterative(int[] arr, int target) {
        int left = 0;
        int right = arr.length - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2; // 오버플로우 방지

            if (arr[mid] == target) {
                return mid; // 찾음
            } else if (arr[mid] < target) {
                left = mid + 1; // 오른쪽 절반 탐색
            } else {
                right = mid - 1; // 왼쪽 절반 탐색
            }
        }

        return -1; // 못 찾음
    }

    // 재귀 방식
    public static int binarySearchRecursive(int[] arr, int target, int left, int right) {
        if (left > right) {
            return -1; // 못 찾음
        }

        int mid = left + (right - left) / 2;

        if (arr[mid] == target) {
            return mid;
        } else if (arr[mid] < target) {
            return binarySearchRecursive(arr, target, mid + 1, right);
        } else {
            return binarySearchRecursive(arr, target, left, mid - 1);
        }
    }

    public static void main(String[] args) {
        int[] arr = {2, 3, 4, 10, 40, 50, 60};
        int target = 10;

        int result = binarySearchIterative(arr, target);
        System.out.println("반복문 방식: " + result); // 출력: 3

        result = binarySearchRecursive(arr, target, 0, arr.length - 1);
        System.out.println("재귀 방식: " + result); // 출력: 3
    }
}
```

---

## 브루트포스

**개념**: 모든 가능한 경우를 전부 시도해보는 방법입니다. "무식하게 푼다"는 의미로, 완전 탐색이라고도 합니다. 효율적이지 않지만 확실한 답을 찾을 수 있습니다.

**시간 복잡도**: 문제에 따라 다름 (보통 O(n!), O(2ⁿ) 등 지수 시간)
**공간 복잡도**: 문제에 따라 다름

### 예제: 배열에서 합이 특정 값인 두 수 찾기

```java
public class BruteForce {
    // 브루트포스로 두 수의 합 찾기
    public static int[] twoSumBruteForce(int[] arr, int target) {
        int n = arr.length;

        // 모든 쌍을 확인
        for (int i = 0; i < n - 1; i++) {
            for (int j = i + 1; j < n; j++) {
                if (arr[i] + arr[j] == target) {
                    return new int[]{i, j};
                }
            }
        }

        return new int[]{-1, -1}; // 못 찾음
    }

    public static void main(String[] args) {
        int[] arr = {2, 7, 11, 15};
        int target = 9;
        int[] result = twoSumBruteForce(arr, target);

        System.out.println("인덱스: [" + result[0] + ", " + result[1] + "]");
        // 출력: 인덱스: [0, 1] (2 + 7 = 9)
    }
}
```

### 예제: 최댓값과 최솟값 찾기

```java
public class MinMaxBruteForce {
    public static void findMinMax(int[] arr) {
        if (arr.length == 0) return;

        int min = arr[0];
        int max = arr[0];

        // 모든 원소를 확인
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] < min) {
                min = arr[i];
            }
            if (arr[i] > max) {
                max = arr[i];
            }
        }

        System.out.println("최솟값: " + min);
        System.out.println("최댓값: " + max);
    }

    public static void main(String[] args) {
        int[] arr = {3, 5, 1, 8, 2, 9, 4};
        findMinMax(arr);
        // 출력: 최솟값: 1, 최댓값: 9
    }
}
```

---

## 순열과 조합

### 순열 (Permutation)

**개념**: n개 중에서 r개를 선택하여 순서를 고려하여 나열하는 경우의 수입니다.
**공식**: P(n, r) = n! / (n-r)!

**시간 복잡도**: O(n!)
**공간 복잡도**: O(n) - 재귀 깊이

```java
import java.util.*;

public class Permutation {
    // 순열 생성 (재귀)
    public static void permutation(int[] arr, int depth, int n, int r, boolean[] visited, int[] output) {
        if (depth == r) {
            // r개를 선택했으면 출력
            System.out.println(Arrays.toString(output));
            return;
        }

        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                visited[i] = true;
                output[depth] = arr[i];
                permutation(arr, depth + 1, n, r, visited, output);
                visited[i] = false; // 백트래킹
            }
        }
    }

    public static void main(String[] args) {
        int[] arr = {1, 2, 3};
        int n = arr.length;
        int r = 2;

        boolean[] visited = new boolean[n];
        int[] output = new int[r];

        System.out.println("3개 중 2개를 선택하는 순열:");
        permutation(arr, 0, n, r, visited, output);
        // 출력: [1,2], [1,3], [2,1], [2,3], [3,1], [3,2]
    }
}
```

### 조합 (Combination)

**개념**: n개 중에서 r개를 선택하는데 순서를 고려하지 않는 경우의 수입니다.
**공식**: C(n, r) = n! / (r! × (n-r)!)

**시간 복잡도**: O(2ⁿ)
**공간 복잡도**: O(n) - 재귀 깊이

```java
import java.util.*;

public class Combination {
    // 조합 생성 (재귀)
    public static void combination(int[] arr, int start, int n, int r, int depth, int[] output) {
        if (depth == r) {
            // r개를 선택했으면 출력
            System.out.println(Arrays.toString(output));
            return;
        }

        for (int i = start; i < n; i++) {
            output[depth] = arr[i];
            combination(arr, i + 1, n, r, depth + 1, output);
        }
    }

    public static void main(String[] args) {
        int[] arr = {1, 2, 3, 4};
        int n = arr.length;
        int r = 2;

        int[] output = new int[r];

        System.out.println("4개 중 2개를 선택하는 조합:");
        combination(arr, 0, n, r, 0, output);
        // 출력: [1,2], [1,3], [1,4], [2,3], [2,4], [3,4]
    }
}
```

### 중복 순열

**개념**: 중복을 허용하여 n개 중에서 r개를 선택하여 순서를 고려하여 나열합니다.

```java
public class PermutationWithRepetition {
    public static void permutationWithRep(int[] arr, int depth, int r, int[] output) {
        if (depth == r) {
            System.out.println(Arrays.toString(output));
            return;
        }

        for (int i = 0; i < arr.length; i++) {
            output[depth] = arr[i];
            permutationWithRep(arr, depth + 1, r, output);
        }
    }

    public static void main(String[] args) {
        int[] arr = {1, 2, 3};
        int r = 2;
        int[] output = new int[r];

        System.out.println("중복 순열:");
        permutationWithRep(arr, 0, r, output);
        // 출력: [1,1], [1,2], [1,3], [2,1], [2,2], [2,3], [3,1], [3,2], [3,3]
    }
}
```

---

## 정리

### 정렬 알고리즘 비교

| 알고리즘 | 시간 복잡도 (평균) | 공간 복잡도 | 안정성 | 특징 |
|---------|------------------|-----------|-------|-----|
| 버블 정렬 | O(n²) | O(1) | 안정 | 구현 간단, 비효율적 |
| 선택 정렬 | O(n²) | O(1) | 불안정 | 교환 횟수 적음 |
| 삽입 정렬 | O(n²) | O(1) | 안정 | 거의 정렬된 경우 빠름 |

### 탐색 알고리즘 비교

| 알고리즘 | 시간 복잡도 | 전제 조건 | 특징 |
|---------|-----------|---------|-----|
| 선형 탐색 | O(n) | 없음 | 단순, 느림 |
| 이진 탐색 | O(log n) | 정렬된 배열 | 매우 빠름 |

### 학습 팁
1. **기초 정렬은 손으로 직접 그려보며 이해**하세요
2. **시간/공간 복잡도**는 알고리즘 선택의 핵심입니다
3. **이진 탐색**은 정렬된 배열에서만 사용 가능합니다
4. **브루트포스**는 문제 해결의 첫 단계로 유용합니다
5. **순열/조합**은 백트래킹의 기초입니다

---

*마지막 업데이트: 2025년 12월*

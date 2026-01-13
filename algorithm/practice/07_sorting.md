# 7. 정렬 (Sorting)

> 관련 코드: `section3_essential1/sorting/SortingBasic.java`

---

## 1. 정렬이란?

데이터를 특정 기준에 따라 순서대로 나열하는 것입니다.
- **오름차순**: 작은 것 → 큰 것
- **내림차순**: 큰 것 → 작은 것

---

## 2. 주요 정렬 알고리즘

### O(n²) 정렬 - 간단하지만 느림
| 알고리즘 | 안정 정렬 | 특징 |
|----------|----------|------|
| 버블 정렬 | O | 인접 원소 비교/교환 |
| 선택 정렬 | X | 최솟값을 맨 앞으로 |
| 삽입 정렬 | O | 정렬된 부분에 삽입, 거의 정렬된 경우 빠름 |

### O(n log n) 정렬 - 효율적
| 알고리즘 | 안정 정렬 | 특징 |
|----------|----------|------|
| 병합 정렬 | O | 분할 정복, 추가 메모리 O(n) |
| 퀵 정렬 | X | 평균적으로 가장 빠름, 최악 O(n²) |
| 힙 정렬 | X | 힙 자료구조 활용 |

### O(n) 정렬 - 특수 조건
| 알고리즘 | 조건 |
|----------|------|
| 계수 정렬 | 값의 범위가 작을 때 |
| 기수 정렬 | 자릿수 기반 정렬 |

---

## 3. 안정 정렬 vs 불안정 정렬

### 안정 정렬 (Stable Sort)
- 같은 값의 원소들의 **상대적 순서가 유지**됨
- 예: 병합 정렬, 삽입 정렬, 버블 정렬

### 불안정 정렬 (Unstable Sort)
- 같은 값의 원소들의 **상대적 순서가 바뀔 수 있음**
- 예: 퀵 정렬, 힙 정렬, 선택 정렬

---

## 4. 각 정렬 알고리즘 구현

### 버블 정렬
```java
public static void bubbleSort(int[] arr) {
    int n = arr.length;
    for (int i = 0; i < n - 1; i++) {
        boolean swapped = false;
        for (int j = 0; j < n - 1 - i; j++) {
            if (arr[j] > arr[j + 1]) {
                int temp = arr[j];
                arr[j] = arr[j + 1];
                arr[j + 1] = temp;
                swapped = true;
            }
        }
        if (!swapped) break;  // 교환 없으면 정렬 완료
    }
}
```

### 선택 정렬
```java
public static void selectionSort(int[] arr) {
    int n = arr.length;
    for (int i = 0; i < n - 1; i++) {
        int minIdx = i;
        for (int j = i + 1; j < n; j++) {
            if (arr[j] < arr[minIdx]) {
                minIdx = j;
            }
        }
        // 최솟값을 i번째 위치로
        int temp = arr[i];
        arr[i] = arr[minIdx];
        arr[minIdx] = temp;
    }
}
```

### 삽입 정렬
```java
public static void insertionSort(int[] arr) {
    int n = arr.length;
    for (int i = 1; i < n; i++) {
        int key = arr[i];
        int j = i - 1;
        while (j >= 0 && arr[j] > key) {
            arr[j + 1] = arr[j];
            j--;
        }
        arr[j + 1] = key;
    }
}
```

### 병합 정렬
```java
public static void mergeSort(int[] arr, int left, int right) {
    if (left < right) {
        int mid = (left + right) / 2;
        mergeSort(arr, left, mid);
        mergeSort(arr, mid + 1, right);
        merge(arr, left, mid, right);
    }
}

private static void merge(int[] arr, int left, int mid, int right) {
    int[] temp = new int[right - left + 1];
    int i = left, j = mid + 1, k = 0;

    while (i <= mid && j <= right) {
        if (arr[i] <= arr[j]) temp[k++] = arr[i++];
        else temp[k++] = arr[j++];
    }
    while (i <= mid) temp[k++] = arr[i++];
    while (j <= right) temp[k++] = arr[j++];

    for (int t = 0; t < temp.length; t++) {
        arr[left + t] = temp[t];
    }
}
```

### 퀵 정렬
```java
public static void quickSort(int[] arr, int left, int right) {
    if (left < right) {
        int pivot = partition(arr, left, right);
        quickSort(arr, left, pivot - 1);
        quickSort(arr, pivot + 1, right);
    }
}

private static int partition(int[] arr, int left, int right) {
    int pivot = arr[right];
    int i = left - 1;
    for (int j = left; j < right; j++) {
        if (arr[j] < pivot) {
            i++;
            int temp = arr[i];
            arr[i] = arr[j];
            arr[j] = temp;
        }
    }
    int temp = arr[i + 1];
    arr[i + 1] = arr[right];
    arr[right] = temp;
    return i + 1;
}
```

---

## 5. Java 내장 정렬

### Arrays.sort()
```java
int[] arr = {64, 34, 25, 12, 22, 11, 90};
Arrays.sort(arr);  // 오름차순

// 내림차순 (Integer 배열 필요)
Integer[] arr2 = {64, 34, 25, 12, 22, 11, 90};
Arrays.sort(arr2, Collections.reverseOrder());
```

- **기본 타입**: Dual-Pivot Quicksort (불안정)
- **객체 타입**: TimSort (안정)

### Collections.sort()
```java
List<Integer> list = Arrays.asList(64, 34, 25, 12);
Collections.sort(list);  // TimSort (안정)
```

---

## 핵심 정리

1. 코딩 테스트에서는 대부분 **Arrays.sort()** 사용
2. 정렬 알고리즘은 **원리 이해**가 중요
3. **안정 정렬**이 필요한지 확인
4. 시간 복잡도: O(n²) vs O(n log n)

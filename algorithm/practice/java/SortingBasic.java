package section3_essential1.sorting;

import java.util.Arrays;
import java.util.Collections;

/**
 * 정렬 (Sorting) 기본 개념
 *
 * ========================================
 * 1. 정렬이란?
 * ========================================
 *
 * 정렬은 데이터를 특정 기준에 따라 순서대로 나열하는 것입니다.
 * - 오름차순: 작은 것 → 큰 것
 * - 내림차순: 큰 것 → 작은 것
 *
 * ========================================
 * 2. 주요 정렬 알고리즘
 * ========================================
 *
 * 1) O(n²) 정렬 - 간단하지만 느림
 *    - 버블 정렬 (Bubble Sort)
 *    - 선택 정렬 (Selection Sort)
 *    - 삽입 정렬 (Insertion Sort)
 *
 * 2) O(n log n) 정렬 - 효율적
 *    - 병합 정렬 (Merge Sort) - 안정 정렬
 *    - 퀵 정렬 (Quick Sort) - 평균적으로 가장 빠름
 *    - 힙 정렬 (Heap Sort)
 *
 * 3) O(n) 정렬 - 특수한 조건에서 사용
 *    - 계수 정렬 (Counting Sort)
 *    - 기수 정렬 (Radix Sort)
 *
 * ========================================
 * 3. 안정 정렬 vs 불안정 정렬
 * ========================================
 *
 * 안정 정렬 (Stable Sort):
 * - 같은 값의 원소들의 상대적 순서가 유지됨
 * - 예: 병합 정렬, 삽입 정렬, 버블 정렬
 *
 * 불안정 정렬 (Unstable Sort):
 * - 같은 값의 원소들의 상대적 순서가 바뀔 수 있음
 * - 예: 퀵 정렬, 힙 정렬, 선택 정렬
 *
 * ========================================
 * 4. Java에서의 정렬
 * ========================================
 *
 * Arrays.sort():
 * - 기본 타입: Dual-Pivot Quicksort (불안정)
 * - 객체 타입: TimSort (안정)
 *
 * Collections.sort():
 * - TimSort (안정)
 */
public class SortingBasic {

    /**
     * 버블 정렬 (Bubble Sort)
     *
     * 인접한 두 원소를 비교하여 교환
     * 한 번의 순회로 가장 큰 원소가 맨 뒤로 이동
     *
     * 시간 복잡도: O(n²)
     * 공간 복잡도: O(1)
     * 안정 정렬: O
     */
    public static void bubbleSort(int[] arr) {
        int n = arr.length;

        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;

            for (int j = 0; j < n - 1 - i; j++) {
                if (arr[j] > arr[j + 1]) {
                    // 교환
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                    swapped = true;
                }
            }

            // 교환이 없었다면 이미 정렬됨
            if (!swapped) break;
        }
    }

    /**
     * 선택 정렬 (Selection Sort)
     *
     * 가장 작은 원소를 찾아 맨 앞으로 이동
     *
     * 시간 복잡도: O(n²)
     * 공간 복잡도: O(1)
     * 안정 정렬: X
     */
    public static void selectionSort(int[] arr) {
        int n = arr.length;

        for (int i = 0; i < n - 1; i++) {
            int minIdx = i;

            // i 이후에서 최솟값 찾기
            for (int j = i + 1; j < n; j++) {
                if (arr[j] < arr[minIdx]) {
                    minIdx = j;
                }
            }

            // 최솟값을 i번째 위치로 이동
            int temp = arr[i];
            arr[i] = arr[minIdx];
            arr[minIdx] = temp;
        }
    }

    /**
     * 삽입 정렬 (Insertion Sort)
     *
     * 각 원소를 이미 정렬된 부분의 적절한 위치에 삽입
     * 거의 정렬된 배열에서 매우 효율적
     *
     * 시간 복잡도: O(n²), 거의 정렬된 경우 O(n)
     * 공간 복잡도: O(1)
     * 안정 정렬: O
     */
    public static void insertionSort(int[] arr) {
        int n = arr.length;

        for (int i = 1; i < n; i++) {
            int key = arr[i];
            int j = i - 1;

            // key보다 큰 원소들을 오른쪽으로 이동
            while (j >= 0 && arr[j] > key) {
                arr[j + 1] = arr[j];
                j--;
            }

            // key를 적절한 위치에 삽입
            arr[j + 1] = key;
        }
    }

    /**
     * 병합 정렬 (Merge Sort)
     *
     * 분할 정복: 반으로 나누고, 정렬하고, 합침
     *
     * 시간 복잡도: O(n log n)
     * 공간 복잡도: O(n)
     * 안정 정렬: O
     */
    public static void mergeSort(int[] arr, int left, int right) {
        if (left < right) {
            int mid = (left + right) / 2;

            // 분할
            mergeSort(arr, left, mid);
            mergeSort(arr, mid + 1, right);

            // 병합
            merge(arr, left, mid, right);
        }
    }

    private static void merge(int[] arr, int left, int mid, int right) {
        int[] temp = new int[right - left + 1];
        int i = left, j = mid + 1, k = 0;

        // 두 부분을 비교하며 병합
        while (i <= mid && j <= right) {
            if (arr[i] <= arr[j]) {
                temp[k++] = arr[i++];
            } else {
                temp[k++] = arr[j++];
            }
        }

        // 남은 원소 복사
        while (i <= mid) {
            temp[k++] = arr[i++];
        }
        while (j <= right) {
            temp[k++] = arr[j++];
        }

        // 원본 배열에 복사
        for (int t = 0; t < temp.length; t++) {
            arr[left + t] = temp[t];
        }
    }

    /**
     * 퀵 정렬 (Quick Sort)
     *
     * 피벗을 기준으로 작은 것은 왼쪽, 큰 것은 오른쪽으로 분할
     *
     * 시간 복잡도: 평균 O(n log n), 최악 O(n²)
     * 공간 복잡도: O(log n) - 재귀 스택
     * 안정 정렬: X
     */
    public static void quickSort(int[] arr, int left, int right) {
        if (left < right) {
            int pivot = partition(arr, left, right);
            quickSort(arr, left, pivot - 1);
            quickSort(arr, pivot + 1, right);
        }
    }

    private static int partition(int[] arr, int left, int right) {
        int pivot = arr[right];  // 맨 오른쪽을 피벗으로
        int i = left - 1;

        for (int j = left; j < right; j++) {
            if (arr[j] < pivot) {
                i++;
                int temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }

        // 피벗을 올바른 위치로
        int temp = arr[i + 1];
        arr[i + 1] = arr[right];
        arr[right] = temp;

        return i + 1;
    }

    /**
     * 계수 정렬 (Counting Sort)
     *
     * 원소의 개수를 세어 정렬
     * 원소 값의 범위가 작을 때 효율적
     *
     * 시간 복잡도: O(n + k), k는 값의 범위
     * 공간 복잡도: O(k)
     * 안정 정렬: O
     */
    public static void countingSort(int[] arr, int maxVal) {
        int[] count = new int[maxVal + 1];

        // 개수 세기
        for (int num : arr) {
            count[num]++;
        }

        // 정렬된 결과 만들기
        int idx = 0;
        for (int i = 0; i <= maxVal; i++) {
            while (count[i] > 0) {
                arr[idx++] = i;
                count[i]--;
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("=== 정렬 알고리즘 예제 ===\n");

        int[] original = {64, 34, 25, 12, 22, 11, 90};

        // 버블 정렬
        int[] arr1 = original.clone();
        bubbleSort(arr1);
        System.out.println("버블 정렬: " + Arrays.toString(arr1));

        // 선택 정렬
        int[] arr2 = original.clone();
        selectionSort(arr2);
        System.out.println("선택 정렬: " + Arrays.toString(arr2));

        // 삽입 정렬
        int[] arr3 = original.clone();
        insertionSort(arr3);
        System.out.println("삽입 정렬: " + Arrays.toString(arr3));

        // 병합 정렬
        int[] arr4 = original.clone();
        mergeSort(arr4, 0, arr4.length - 1);
        System.out.println("병합 정렬: " + Arrays.toString(arr4));

        // 퀵 정렬
        int[] arr5 = original.clone();
        quickSort(arr5, 0, arr5.length - 1);
        System.out.println("퀵 정렬: " + Arrays.toString(arr5));

        // 계수 정렬
        int[] arr6 = original.clone();
        countingSort(arr6, 90);
        System.out.println("계수 정렬: " + Arrays.toString(arr6));

        // Java 내장 정렬
        System.out.println("\n=== Java 내장 정렬 ===");
        int[] arr7 = original.clone();
        Arrays.sort(arr7);
        System.out.println("Arrays.sort(): " + Arrays.toString(arr7));

        // 내림차순 정렬 (Integer 배열 필요)
        Integer[] arr8 = {64, 34, 25, 12, 22, 11, 90};
        Arrays.sort(arr8, Collections.reverseOrder());
        System.out.println("내림차순: " + Arrays.toString(arr8));
    }
}

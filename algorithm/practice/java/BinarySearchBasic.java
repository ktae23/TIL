package section5_essential2.binarysearch;

import java.util.Arrays;

/**
 * 이분 탐색 (Binary Search) 기본 개념
 *
 * ========================================
 * 1. 이분 탐색이란?
 * ========================================
 *
 * 정렬된 배열에서 특정 값을 찾는 O(log n) 알고리즘입니다.
 *
 * 핵심 아이디어:
 * - 중간 값과 목표 값을 비교
 * - 목표가 중간보다 작으면 왼쪽 절반 탐색
 * - 목표가 중간보다 크면 오른쪽 절반 탐색
 * - 매 단계마다 탐색 범위가 절반으로 줄어듦
 *
 * ========================================
 * 2. 이분 탐색 조건
 * ========================================
 *
 * 필수 조건: 배열이 정렬되어 있어야 함
 *
 * ========================================
 * 3. Lower Bound vs Upper Bound
 * ========================================
 *
 * Lower Bound:
 * - target 이상인 첫 번째 위치
 * - target이 없으면 target보다 큰 첫 번째 위치
 *
 * Upper Bound:
 * - target 초과인 첫 번째 위치
 * - target이 여러 개면 마지막 target 다음 위치
 *
 * 예: arr = [1, 2, 2, 2, 3, 4], target = 2
 * - Lower Bound: index 1 (첫 번째 2)
 * - Upper Bound: index 4 (3의 위치)
 * - 2의 개수 = Upper - Lower = 4 - 1 = 3
 *
 * ========================================
 * 4. 시간 복잡도
 * ========================================
 *
 * - 탐색: O(log n)
 * - 정렬 후 탐색: O(n log n) + O(log n) = O(n log n)
 */
public class BinarySearchBasic {

    /**
     * 기본 이분 탐색: target이 있으면 인덱스, 없으면 -1
     */
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

    /**
     * Lower Bound: target 이상인 첫 번째 위치
     *
     * 반환값이 배열 길이면 모든 원소가 target보다 작음
     */
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

    /**
     * Upper Bound: target 초과인 첫 번째 위치
     *
     * 반환값이 배열 길이면 모든 원소가 target 이하
     */
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

    /**
     * target의 개수 세기
     */
    public static int countTarget(int[] arr, int target) {
        return upperBound(arr, target) - lowerBound(arr, target);
    }

    /**
     * 재귀적 이분 탐색
     */
    public static int binarySearchRecursive(int[] arr, int target, int left, int right) {
        if (left > right) return -1;

        int mid = left + (right - left) / 2;

        if (arr[mid] == target) return mid;
        if (arr[mid] < target) return binarySearchRecursive(arr, target, mid + 1, right);
        return binarySearchRecursive(arr, target, left, mid - 1);
    }

    /**
     * Java 내장 이분 탐색 사용법
     */
    public static void javaBuiltIn() {
        int[] arr = {1, 2, 3, 4, 5, 6, 7, 8, 9};

        // Arrays.binarySearch: 있으면 인덱스, 없으면 -(삽입위치+1)
        int result1 = Arrays.binarySearch(arr, 5);  // 4
        int result2 = Arrays.binarySearch(arr, 6);  // 5
        int result3 = Arrays.binarySearch(arr, 10); // -10 (삽입위치 9+1)

        System.out.println("5의 위치: " + result1);
        System.out.println("6의 위치: " + result2);
        System.out.println("10의 결과: " + result3);

        // 삽입 위치 구하기 (없는 경우)
        if (result3 < 0) {
            int insertionPoint = -(result3 + 1);
            System.out.println("10의 삽입 위치: " + insertionPoint);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== 이분 탐색 기본 예제 ===\n");

        int[] arr = {1, 2, 2, 2, 3, 4, 5, 6};

        // 기본 이분 탐색
        System.out.println("1) 기본 이분 탐색");
        System.out.println("배열: " + Arrays.toString(arr));
        System.out.println("3의 위치: " + binarySearch(arr, 3));
        System.out.println("7의 위치: " + binarySearch(arr, 7) + " (없음)");

        // Lower/Upper Bound
        System.out.println("\n2) Lower/Upper Bound");
        System.out.println("Lower Bound(2): " + lowerBound(arr, 2));  // 1
        System.out.println("Upper Bound(2): " + upperBound(arr, 2));  // 4
        System.out.println("2의 개수: " + countTarget(arr, 2));       // 3

        // 재귀적 이분 탐색
        System.out.println("\n3) 재귀적 이분 탐색");
        System.out.println("4의 위치: " + binarySearchRecursive(arr, 4, 0, arr.length - 1));

        // Java 내장 함수
        System.out.println("\n4) Java 내장 함수");
        javaBuiltIn();
    }
}

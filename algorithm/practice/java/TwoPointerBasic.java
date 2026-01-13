package section5_essential2.twopointer;

import java.util.Arrays;

/**
 * 투 포인터 (Two Pointer) 기본 개념
 *
 * ========================================
 * 1. 투 포인터란?
 * ========================================
 *
 * 두 개의 포인터를 사용하여 배열이나 리스트를 탐색하는 기법입니다.
 *
 * 특징:
 * - O(n²)을 O(n)으로 줄일 수 있음
 * - 정렬된 배열에서 특히 유용
 * - 연속된 구간을 다룰 때 효과적
 *
 * ========================================
 * 2. 투 포인터 유형
 * ========================================
 *
 * 1) 같은 방향: 슬라이딩 윈도우
 *    - left, right가 같은 방향으로 이동
 *    - 연속된 부분 배열 문제
 *
 * 2) 반대 방향: 양 끝에서 시작
 *    - left는 왼쪽에서, right는 오른쪽에서
 *    - 두 수의 합 문제
 *
 * ========================================
 * 3. 시간 복잡도
 * ========================================
 *
 * 각 포인터가 배열을 한 번씩만 순회: O(n)
 * 브루트포스 O(n²)보다 효율적
 */
public class TwoPointerBasic {

    /**
     * 예제 1: 두 수의 합 (정렬된 배열)
     *
     * 정렬된 배열에서 두 수의 합이 target인 쌍 찾기
     */
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

    /**
     * 예제 2: 부분합 (연속된 구간의 합이 S 이상인 최소 길이)
     *
     * 슬라이딩 윈도우 방식
     */
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

    /**
     * 예제 3: 주어진 합을 가지는 부분 수열의 개수
     */
    public static int countSubArraySum(int[] arr, int target) {
        int n = arr.length;
        int count = 0;
        int left = 0;
        int sum = 0;

        for (int right = 0; right < n; right++) {
            sum += arr[right];

            while (sum > target && left <= right) {
                sum -= arr[left];
                left++;
            }

            if (sum == target) {
                count++;
            }
        }

        return count;
    }

    /**
     * 예제 4: 세 수의 합 (3Sum)
     *
     * 정렬 후 첫 번째 수를 고정하고 나머지에 투 포인터
     */
    public static void threeSum(int[] arr, int target) {
        Arrays.sort(arr);
        int n = arr.length;

        for (int i = 0; i < n - 2; i++) {
            // 중복 건너뛰기
            if (i > 0 && arr[i] == arr[i - 1]) continue;

            int left = i + 1;
            int right = n - 1;

            while (left < right) {
                int sum = arr[i] + arr[left] + arr[right];

                if (sum == target) {
                    System.out.println("(" + arr[i] + ", " + arr[left] + ", " + arr[right] + ")");

                    // 중복 건너뛰기
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

    /**
     * 예제 5: 물 담기 (Container With Most Water)
     *
     * 양쪽 끝에서 시작하여 높이가 낮은 쪽을 이동
     */
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

    public static void main(String[] args) {
        System.out.println("=== 투 포인터 기본 예제 ===\n");

        // 예제 1: 두 수의 합
        System.out.println("1) 두 수의 합");
        int[] arr1 = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        int target1 = 10;
        int[] result1 = twoSum(arr1, target1);
        System.out.println("합이 " + target1 + "인 인덱스: " + Arrays.toString(result1));
        System.out.println("값: " + arr1[result1[0]] + " + " + arr1[result1[1]]);

        // 예제 2: 부분합
        System.out.println("\n2) 부분합 (합이 15 이상인 최소 길이)");
        int[] arr2 = {2, 3, 1, 2, 4, 3};
        System.out.println("최소 길이: " + minSubArrayLen(arr2, 7));

        // 예제 3: 세 수의 합
        System.out.println("\n3) 세 수의 합 = 0");
        int[] arr3 = {-1, 0, 1, 2, -1, -4};
        threeSum(arr3, 0);

        // 예제 4: 물 담기
        System.out.println("\n4) 물 담기");
        int[] heights = {1, 8, 6, 2, 5, 4, 8, 3, 7};
        System.out.println("최대 물: " + maxWater(heights));
    }
}

package section3_essential1.bruteforce;

/**
 * 브루트포스 (Brute Force) / 완전탐색 기본 개념
 *
 * ========================================
 * 1. 브루트포스란?
 * ========================================
 *
 * 브루트포스(Brute Force)는 "무차별 대입"이라는 뜻으로,
 * 가능한 모든 경우의 수를 탐색하여 정답을 찾는 방법입니다.
 *
 * 특징:
 * - 항상 정답을 찾을 수 있음 (완전탐색)
 * - 구현이 단순하고 직관적
 * - 경우의 수가 많으면 시간 초과
 *
 * ========================================
 * 2. 브루트포스 적용 조건
 * ========================================
 *
 * 시간 제한 1초 기준:
 * - O(n): n ≤ 1억
 * - O(n²): n ≤ 1만
 * - O(n³): n ≤ 500
 * - O(2ⁿ): n ≤ 20
 * - O(n!): n ≤ 10
 *
 * 문제에서 n의 범위를 확인하고 시간 복잡도를 계산해야 합니다.
 *
 * ========================================
 * 3. 브루트포스 유형
 * ========================================
 *
 * 1) 단순 반복문
 *    - 1중, 2중, 3중 반복문
 *    - 예: 두 수의 합이 K인 경우 찾기
 *
 * 2) 순열/조합
 *    - 순서가 중요하면 순열, 아니면 조합
 *    - 예: N개 중 R개를 선택하는 모든 경우
 *
 * 3) 비트마스크
 *    - 2^n개의 부분집합 탐색
 *    - 예: 부분집합의 합 문제
 *
 * 4) 재귀/백트래킹
 *    - 가지치기로 탐색 공간 줄이기
 *    - 예: N-Queen, 스도쿠
 *
 * ========================================
 * 4. 가지치기 (Pruning)
 * ========================================
 *
 * 불필요한 탐색을 미리 중단하는 기법
 * - 조건에 맞지 않으면 더 이상 탐색하지 않음
 * - 탐색 공간을 크게 줄일 수 있음
 */
public class BruteForceBasic {

    /**
     * 예제 1: 두 수의 합이 target인 쌍 찾기
     *
     * O(n²) 브루트포스
     */
    public static void findTwoSum(int[] arr, int target) {
        int n = arr.length;

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (arr[i] + arr[j] == target) {
                    System.out.println("(" + arr[i] + ", " + arr[j] + ")");
                }
            }
        }
    }

    /**
     * 예제 2: 세 수의 합이 0인 쌍 찾기 (3Sum)
     *
     * O(n³) 브루트포스
     */
    public static void findThreeSum(int[] arr) {
        int n = arr.length;

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                for (int k = j + 1; k < n; k++) {
                    if (arr[i] + arr[j] + arr[k] == 0) {
                        System.out.println("(" + arr[i] + ", " + arr[j] + ", " + arr[k] + ")");
                    }
                }
            }
        }
    }

    /**
     * 예제 3: 부분집합의 합 (비트마스크)
     *
     * 모든 부분집합 중 합이 target인 것 찾기
     * O(2^n)
     */
    public static void subsetSum(int[] arr, int target) {
        int n = arr.length;

        // 0부터 2^n - 1까지 모든 비트마스크
        for (int mask = 0; mask < (1 << n); mask++) {
            int sum = 0;
            StringBuilder subset = new StringBuilder("{");
            boolean first = true;

            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {  // i번째 비트가 켜져 있으면
                    sum += arr[i];
                    if (!first) subset.append(", ");
                    subset.append(arr[i]);
                    first = false;
                }
            }
            subset.append("}");

            if (sum == target && mask != 0) {  // 빈 집합 제외
                System.out.println(subset + " = " + sum);
            }
        }
    }

    /**
     * 예제 4: 순열 생성 (재귀)
     *
     * N개의 수로 만들 수 있는 모든 순열
     * O(n!)
     */
    static int[] permResult;
    static boolean[] used;

    public static void generatePermutations(int[] arr, int depth) {
        if (depth == arr.length) {
            printArray(permResult);
            return;
        }

        for (int i = 0; i < arr.length; i++) {
            if (!used[i]) {
                used[i] = true;
                permResult[depth] = arr[i];
                generatePermutations(arr, depth + 1);
                used[i] = false;  // 백트래킹
            }
        }
    }

    /**
     * 예제 5: 백트래킹 (가지치기)
     *
     * 1부터 N까지 수 중 K개를 선택하여 합이 S인 경우
     * 가지치기: 현재까지의 합이 S를 초과하면 중단
     */
    public static int countWays;

    public static void findCombinationsWithSum(int n, int k, int s, int start, int depth, int currentSum) {
        // 가지치기: 합이 이미 S를 초과
        if (currentSum > s) {
            return;
        }

        // Base Case: K개를 선택
        if (depth == k) {
            if (currentSum == s) {
                countWays++;
            }
            return;
        }

        // 가지치기: 남은 수로 K개를 못 채움
        if (n - start + 1 < k - depth) {
            return;
        }

        // Recursive Case
        for (int i = start; i <= n; i++) {
            findCombinationsWithSum(n, k, s, i + 1, depth + 1, currentSum + i);
        }
    }

    private static void printArray(int[] arr) {
        System.out.print("[");
        for (int i = 0; i < arr.length; i++) {
            System.out.print(arr[i]);
            if (i < arr.length - 1) System.out.print(", ");
        }
        System.out.println("]");
    }

    public static void main(String[] args) {
        System.out.println("=== 브루트포스 기본 예제 ===\n");

        // 예제 1: 두 수의 합
        System.out.println("1) 두 수의 합이 10인 쌍:");
        int[] arr1 = {1, 4, 6, 7, 3, 9};
        findTwoSum(arr1, 10);

        // 예제 2: 세 수의 합
        System.out.println("\n2) 세 수의 합이 0인 쌍:");
        int[] arr2 = {-1, 0, 1, 2, -2, -1};
        findThreeSum(arr2);

        // 예제 3: 부분집합의 합
        System.out.println("\n3) 합이 5인 부분집합:");
        int[] arr3 = {1, 2, 3, 4};
        subsetSum(arr3, 5);

        // 예제 4: 순열
        System.out.println("\n4) {1, 2, 3}의 모든 순열:");
        int[] arr4 = {1, 2, 3};
        permResult = new int[arr4.length];
        used = new boolean[arr4.length];
        generatePermutations(arr4, 0);

        // 예제 5: 백트래킹
        System.out.println("\n5) 1~10 중 3개를 선택하여 합이 15인 경우의 수:");
        countWays = 0;
        findCombinationsWithSum(10, 3, 15, 1, 0, 0);
        System.out.println("경우의 수: " + countWays);
    }
}

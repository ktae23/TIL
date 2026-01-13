package section3_essential1.dp;

/**
 * 동적 프로그래밍 (Dynamic Programming) 기본 개념
 *
 * ========================================
 * 1. DP란?
 * ========================================
 *
 * 동적 프로그래밍은 복잡한 문제를 작은 부분 문제로 나누어 해결하고,
 * 그 결과를 저장하여 재사용하는 알고리즘 설계 기법입니다.
 *
 * 핵심 아이디어:
 * - 이미 계산한 값을 다시 계산하지 않음 (메모이제이션)
 * - 작은 문제의 해를 이용해 큰 문제의 해를 구함
 *
 * ========================================
 * 2. DP 적용 조건
 * ========================================
 *
 * 1) 최적 부분 구조 (Optimal Substructure)
 *    - 큰 문제의 최적해가 작은 문제의 최적해로 구성됨
 *
 * 2) 중복되는 부분 문제 (Overlapping Subproblems)
 *    - 같은 부분 문제가 여러 번 반복됨
 *    - 분할 정복과의 차이점
 *
 * ========================================
 * 3. DP 구현 방식
 * ========================================
 *
 * 1) Top-Down (메모이제이션)
 *    - 재귀 + 캐싱
 *    - 필요한 부분 문제만 계산
 *    - 직관적이지만 스택 오버플로우 주의
 *
 * 2) Bottom-Up (타뷸레이션)
 *    - 반복문으로 작은 문제부터 해결
 *    - 모든 부분 문제를 계산
 *    - 메모리 최적화 가능
 *
 * ========================================
 * 4. DP 문제 접근법
 * ========================================
 *
 * 1) 상태 정의: dp[i]가 무엇을 의미하는지 정의
 * 2) 점화식 수립: dp[i]를 이전 상태들로 표현
 * 3) 초기값 설정: 기저 조건 (Base Case)
 * 4) 계산 순서 결정: 의존 관계에 따라 계산
 *
 * ========================================
 * 5. 대표적인 DP 문제
 * ========================================
 *
 * - 피보나치 수열
 * - 최장 증가 부분 수열 (LIS)
 * - 최장 공통 부분 수열 (LCS)
 * - 배낭 문제 (Knapsack)
 * - 동전 교환 문제
 * - 최단 경로 문제
 */
public class DPBasic {

    /**
     * 예제 1: 피보나치 수열
     *
     * 점화식: F(n) = F(n-1) + F(n-2)
     * 초기값: F(0) = 0, F(1) = 1
     */

    // Top-Down (메모이제이션)
    static long[] memo = new long[100];

    public static long fibTopDown(int n) {
        if (n <= 1) return n;
        if (memo[n] != 0) return memo[n];
        return memo[n] = fibTopDown(n - 1) + fibTopDown(n - 2);
    }

    // Bottom-Up (타뷸레이션)
    public static long fibBottomUp(int n) {
        if (n <= 1) return n;

        long[] dp = new long[n + 1];
        dp[0] = 0;
        dp[1] = 1;

        for (int i = 2; i <= n; i++) {
            dp[i] = dp[i - 1] + dp[i - 2];
        }

        return dp[n];
    }

    // 공간 최적화 (변수 2개만 사용)
    public static long fibOptimized(int n) {
        if (n <= 1) return n;

        long prev2 = 0, prev1 = 1;
        for (int i = 2; i <= n; i++) {
            long current = prev1 + prev2;
            prev2 = prev1;
            prev1 = current;
        }

        return prev1;
    }

    /**
     * 예제 2: 계단 오르기
     *
     * 한 번에 1계단 또는 2계단을 오를 수 있을 때,
     * n번째 계단에 도달하는 방법의 수
     *
     * 점화식: dp[i] = dp[i-1] + dp[i-2]
     */
    public static int climbStairs(int n) {
        if (n <= 2) return n;

        int[] dp = new int[n + 1];
        dp[1] = 1;
        dp[2] = 2;

        for (int i = 3; i <= n; i++) {
            dp[i] = dp[i - 1] + dp[i - 2];
        }

        return dp[n];
    }

    /**
     * 예제 3: 동전 교환
     *
     * 주어진 동전들로 금액 amount를 만드는 최소 동전 개수
     *
     * 점화식: dp[i] = min(dp[i], dp[i - coin] + 1)
     */
    public static int coinChange(int[] coins, int amount) {
        int[] dp = new int[amount + 1];
        int INF = amount + 1;  // 불가능한 경우

        // 초기화
        for (int i = 1; i <= amount; i++) {
            dp[i] = INF;
        }
        dp[0] = 0;

        for (int i = 1; i <= amount; i++) {
            for (int coin : coins) {
                if (i >= coin && dp[i - coin] != INF) {
                    dp[i] = Math.min(dp[i], dp[i - coin] + 1);
                }
            }
        }

        return dp[amount] == INF ? -1 : dp[amount];
    }

    /**
     * 예제 4: 격자 경로 수
     *
     * (0,0)에서 (m-1, n-1)까지 오른쪽 또는 아래로만 이동
     * 가능한 경로의 수
     *
     * 점화식: dp[i][j] = dp[i-1][j] + dp[i][j-1]
     */
    public static int uniquePaths(int m, int n) {
        int[][] dp = new int[m][n];

        // 첫 행과 첫 열은 1 (한 가지 방법만 존재)
        for (int i = 0; i < m; i++) dp[i][0] = 1;
        for (int j = 0; j < n; j++) dp[0][j] = 1;

        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                dp[i][j] = dp[i - 1][j] + dp[i][j - 1];
            }
        }

        return dp[m - 1][n - 1];
    }

    /**
     * 예제 5: 최대 부분 배열 합 (Kadane's Algorithm)
     *
     * 연속된 부분 배열의 최대 합
     *
     * 점화식: dp[i] = max(dp[i-1] + arr[i], arr[i])
     */
    public static int maxSubArray(int[] arr) {
        int maxSum = arr[0];
        int currentSum = arr[0];

        for (int i = 1; i < arr.length; i++) {
            // 이전까지의 합 + 현재 vs 현재부터 새로 시작
            currentSum = Math.max(currentSum + arr[i], arr[i]);
            maxSum = Math.max(maxSum, currentSum);
        }

        return maxSum;
    }

    public static void main(String[] args) {
        System.out.println("=== 동적 프로그래밍 기본 예제 ===\n");

        // 예제 1: 피보나치
        System.out.println("1) 피보나치 수열");
        System.out.println("F(10) Top-Down: " + fibTopDown(10));
        System.out.println("F(10) Bottom-Up: " + fibBottomUp(10));
        System.out.println("F(10) Optimized: " + fibOptimized(10));

        // 예제 2: 계단 오르기
        System.out.println("\n2) 계단 오르기");
        System.out.println("5계단 오르는 방법: " + climbStairs(5) + "가지");

        // 예제 3: 동전 교환
        System.out.println("\n3) 동전 교환");
        int[] coins = {1, 2, 5};
        System.out.println("11원을 만드는 최소 동전: " + coinChange(coins, 11) + "개");

        // 예제 4: 격자 경로
        System.out.println("\n4) 격자 경로");
        System.out.println("3x7 격자 경로 수: " + uniquePaths(3, 7));

        // 예제 5: 최대 부분 배열
        System.out.println("\n5) 최대 부분 배열 합");
        int[] arr = {-2, 1, -3, 4, -1, 2, 1, -5, 4};
        System.out.println("최대 부분 배열 합: " + maxSubArray(arr));
    }
}

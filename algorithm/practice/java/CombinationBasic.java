package section2_basic.combination;

import java.util.ArrayList;
import java.util.List;

/**
 * 조합 (Combination) 기본 개념
 *
 * ========================================
 * 1. 조합이란?
 * ========================================
 *
 * 조합은 n개의 원소 중에서 r개를 순서 없이 선택하는 경우의 수입니다.
 *
 * 기호: nCr 또는 C(n, r) 또는 (n r)
 *
 * 공식: nCr = n! / (r! × (n-r)!)
 *
 * 예시: {1, 2, 3}에서 2개를 선택
 * - {1, 2}, {1, 3}, {2, 3} → 총 3가지
 * - 3C2 = 3! / (2! × 1!) = 6 / 2 = 3
 *
 * ========================================
 * 2. 조합 vs 순열
 * ========================================
 *
 * 조합: 순서가 의미 없음
 * - {1, 2}와 {2, 1}은 같은 것
 *
 * 순열: 순서가 의미 있음
 * - (1, 2)와 (2, 1)은 다른 것
 *
 * 관계: nPr = nCr × r!
 *
 * ========================================
 * 3. 조합의 성질
 * ========================================
 *
 * 1) nC0 = nCn = 1
 *    - 0개 선택하는 방법 = 1가지 (아무것도 안 고름)
 *    - n개 모두 선택하는 방법 = 1가지
 *
 * 2) nCr = nC(n-r)
 *    - r개를 고르는 것 = (n-r)개를 안 고르는 것
 *
 * 3) nCr = (n-1)C(r-1) + (n-1)Cr  (파스칼 삼각형)
 *    - 특정 원소를 포함하는 경우 + 포함하지 않는 경우
 *
 * ========================================
 * 4. 구현 방법
 * ========================================
 *
 * 1) 재귀 (Backtracking)
 *    - 각 원소를 선택하거나 선택하지 않거나
 *    - 시간 복잡도: O(nCr)
 *
 * 2) 비트마스크
 *    - n개 원소의 부분집합을 비트로 표현
 *    - 2^n개의 모든 부분집합 중 크기가 r인 것만 선택
 */
public class CombinationBasic {

    /**
     * 방법 1: 재귀를 이용한 조합 생성
     *
     * 핵심 아이디어:
     * - 각 원소에 대해 "선택한다" 또는 "선택하지 않는다"
     * - start 인덱스부터 탐색하여 중복을 방지
     *
     * @param arr 원본 배열
     * @param selected 현재까지 선택된 원소들
     * @param start 탐색 시작 인덱스
     * @param r 선택해야 하는 남은 개수
     */
    public static void combination(int[] arr, List<Integer> selected, int start, int r) {
        // Base Case: r개를 모두 선택했으면 출력
        if (r == 0) {
            System.out.println(selected);
            return;
        }

        // start부터 끝까지 각 원소를 선택
        for (int i = start; i < arr.length; i++) {
            selected.add(arr[i]);                    // 선택
            combination(arr, selected, i + 1, r - 1); // 다음 원소들 중에서 r-1개 선택
            selected.remove(selected.size() - 1);    // 선택 취소 (백트래킹)
        }
    }

    /**
     * 방법 2: boolean 배열을 이용한 조합
     *
     * visited[i] = true면 i번째 원소를 선택
     */
    static boolean[] visited;
    static int[] arr;

    public static void combinationWithVisited(int start, int depth, int r) {
        if (depth == r) {
            // 선택된 원소들 출력
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (int i = 0; i < arr.length; i++) {
                if (visited[i]) {
                    if (!first) sb.append(", ");
                    sb.append(arr[i]);
                    first = false;
                }
            }
            sb.append("]");
            System.out.println(sb);
            return;
        }

        for (int i = start; i < arr.length; i++) {
            visited[i] = true;
            combinationWithVisited(i + 1, depth + 1, r);
            visited[i] = false;
        }
    }

    /**
     * 방법 3: 비트마스크를 이용한 조합
     *
     * n개의 원소 → 2^n개의 부분집합
     * 이 중 비트가 r개 켜진 것만 선택
     */
    public static void combinationBitmask(int[] arr, int r) {
        int n = arr.length;

        // 0부터 2^n - 1까지 모든 비트마스크 확인
        for (int mask = 0; mask < (1 << n); mask++) {
            // 비트가 r개 켜져 있는지 확인
            if (Integer.bitCount(mask) == r) {
                System.out.print("[");
                boolean first = true;
                for (int i = 0; i < n; i++) {
                    if ((mask & (1 << i)) != 0) {  // i번째 비트가 켜져 있으면
                        if (!first) System.out.print(", ");
                        System.out.print(arr[i]);
                        first = false;
                    }
                }
                System.out.println("]");
            }
        }
    }

    /**
     * 조합의 개수 계산 (nCr)
     *
     * 파스칼 삼각형의 성질 이용:
     * nCr = (n-1)C(r-1) + (n-1)Cr
     */
    public static long nCr(int n, int r) {
        // Base Case
        if (r == 0 || r == n) {
            return 1;
        }

        // Recursive Case (파스칼 삼각형)
        return nCr(n - 1, r - 1) + nCr(n - 1, r);
    }

    /**
     * DP를 이용한 조합 개수 계산 (효율적)
     */
    public static long nCrDP(int n, int r) {
        long[][] dp = new long[n + 1][r + 1];

        for (int i = 0; i <= n; i++) {
            dp[i][0] = 1;  // iC0 = 1
            for (int j = 1; j <= Math.min(i, r); j++) {
                if (i == j) {
                    dp[i][j] = 1;  // iCi = 1
                } else {
                    dp[i][j] = dp[i - 1][j - 1] + dp[i - 1][j];
                }
            }
        }

        return dp[n][r];
    }

    public static void main(String[] args) {
        System.out.println("=== 조합 기본 예제 ===\n");

        int[] numbers = {1, 2, 3, 4};
        int r = 2;

        System.out.println("배열 {1, 2, 3, 4}에서 2개 선택:\n");

        // 방법 1: 재귀
        System.out.println("방법 1 - 재귀:");
        combination(numbers, new ArrayList<>(), 0, r);

        // 방법 2: visited 배열
        System.out.println("\n방법 2 - visited 배열:");
        arr = numbers;
        visited = new boolean[numbers.length];
        combinationWithVisited(0, 0, r);

        // 방법 3: 비트마스크
        System.out.println("\n방법 3 - 비트마스크:");
        combinationBitmask(numbers, r);

        // 조합 개수 계산
        System.out.println("\n조합의 개수:");
        System.out.println("4C2 = " + nCr(4, 2));
        System.out.println("10C3 = " + nCrDP(10, 3));
        System.out.println("20C10 = " + nCrDP(20, 10));
    }
}

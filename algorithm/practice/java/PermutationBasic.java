package section2_basic.permutation;

import java.util.ArrayList;
import java.util.List;

/**
 * 순열 (Permutation) 기본 개념
 *
 * ========================================
 * 1. 순열이란?
 * ========================================
 *
 * 순열은 n개의 원소 중에서 r개를 순서 있게 나열하는 경우의 수입니다.
 *
 * 기호: nPr 또는 P(n, r)
 *
 * 공식: nPr = n! / (n-r)!
 *
 * 예시: {1, 2, 3}에서 2개를 순서대로 나열
 * - (1,2), (1,3), (2,1), (2,3), (3,1), (3,2) → 총 6가지
 * - 3P2 = 3! / 1! = 6
 *
 * ========================================
 * 2. 순열 vs 조합
 * ========================================
 *
 * 순열 (Permutation): 순서가 의미 있음
 * - (1, 2)와 (2, 1)은 다른 것
 * - nPr = n! / (n-r)!
 *
 * 조합 (Combination): 순서가 의미 없음
 * - {1, 2}와 {2, 1}은 같은 것
 * - nCr = n! / (r! × (n-r)!)
 *
 * 관계: nPr = nCr × r!
 *
 * ========================================
 * 3. 순열의 종류
 * ========================================
 *
 * 1) 일반 순열: n개 중 r개를 선택하여 나열
 *    - nPr = n! / (n-r)!
 *
 * 2) 전체 순열: n개 모두를 나열
 *    - nPn = n!
 *
 * 3) 중복 순열: 같은 원소를 여러 번 선택 가능
 *    - n^r
 *
 * ========================================
 * 4. 구현 방법
 * ========================================
 *
 * 1) 재귀 + visited 배열
 *    - 각 위치에 아직 사용하지 않은 원소를 배치
 *    - visited[i] = true면 i번째 원소는 이미 사용됨
 *
 * 2) 스왑(Swap) 방식
 *    - 배열의 원소들을 서로 교환하며 순열 생성
 *    - 추가 배열 없이 in-place로 동작
 */
public class PermutationBasic {

    /**
     * 방법 1: visited 배열을 이용한 순열
     *
     * 핵심 아이디어:
     * - 각 위치(depth)에 아직 사용하지 않은 원소를 배치
     * - visited 배열로 사용 여부 추적
     *
     * @param arr 원본 배열
     * @param result 현재까지 만들어진 순열
     * @param visited 사용 여부 배열
     * @param depth 현재 선택한 원소 개수
     * @param r 선택해야 하는 총 개수
     */
    public static void permutation(int[] arr, int[] result, boolean[] visited, int depth, int r) {
        // Base Case: r개를 모두 선택했으면 출력
        if (depth == r) {
            printArray(result, r);
            return;
        }

        // 모든 원소에 대해 시도
        for (int i = 0; i < arr.length; i++) {
            if (!visited[i]) {           // 아직 사용하지 않은 원소만
                visited[i] = true;        // 사용 표시
                result[depth] = arr[i];   // 현재 위치에 배치
                permutation(arr, result, visited, depth + 1, r);  // 다음 위치
                visited[i] = false;       // 사용 표시 해제 (백트래킹)
            }
        }
    }

    /**
     * 방법 2: 스왑(Swap)을 이용한 순열
     *
     * 핵심 아이디어:
     * - depth 위치에 올 수 있는 모든 원소와 교환
     * - 재귀 호출 후 원래대로 복원 (백트래킹)
     *
     * @param arr 배열 (직접 수정됨)
     * @param depth 현재 고정할 위치
     * @param r 선택해야 하는 총 개수
     */
    public static void permutationSwap(int[] arr, int depth, int r) {
        // Base Case
        if (depth == r) {
            printArray(arr, r);
            return;
        }

        // depth 위치에 i번째 원소를 놓아봄
        for (int i = depth; i < arr.length; i++) {
            swap(arr, depth, i);                    // 교환
            permutationSwap(arr, depth + 1, r);     // 다음 위치
            swap(arr, depth, i);                    // 복원
        }
    }

    /**
     * 방법 3: 중복 순열
     *
     * 같은 원소를 여러 번 선택 가능
     * visited 검사 없이 모든 원소 선택 가능
     */
    public static void permutationWithRepetition(int[] arr, int[] result, int depth, int r) {
        // Base Case
        if (depth == r) {
            printArray(result, r);
            return;
        }

        // 모든 원소 선택 가능 (중복 허용)
        for (int i = 0; i < arr.length; i++) {
            result[depth] = arr[i];
            permutationWithRepetition(arr, result, depth + 1, r);
        }
    }

    /**
     * List를 이용한 순열 (더 직관적인 버전)
     */
    public static void permutationList(int[] arr, List<Integer> result, boolean[] visited, int r) {
        if (result.size() == r) {
            System.out.println(result);
            return;
        }

        for (int i = 0; i < arr.length; i++) {
            if (!visited[i]) {
                visited[i] = true;
                result.add(arr[i]);
                permutationList(arr, result, visited, r);
                result.remove(result.size() - 1);  // 백트래킹
                visited[i] = false;
            }
        }
    }

    // 배열 교환
    private static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    // 배열 출력
    private static void printArray(int[] arr, int length) {
        System.out.print("[");
        for (int i = 0; i < length; i++) {
            System.out.print(arr[i]);
            if (i < length - 1) System.out.print(", ");
        }
        System.out.println("]");
    }

    /**
     * 순열의 개수 계산 (nPr)
     */
    public static long nPr(int n, int r) {
        long result = 1;
        for (int i = n; i > n - r; i--) {
            result *= i;
        }
        return result;
    }

    public static void main(String[] args) {
        System.out.println("=== 순열 기본 예제 ===\n");

        int[] numbers = {1, 2, 3};
        int r = 2;

        System.out.println("배열 {1, 2, 3}에서 2개 순서대로 나열:\n");

        // 방법 1: visited 배열
        System.out.println("방법 1 - visited 배열:");
        permutation(numbers, new int[r], new boolean[numbers.length], 0, r);

        // 방법 2: 스왑
        System.out.println("\n방법 2 - 스왑:");
        permutationSwap(numbers.clone(), 0, r);

        // 방법 3: 중복 순열
        System.out.println("\n방법 3 - 중복 순열 (2개):");
        permutationWithRepetition(numbers, new int[r], 0, r);

        // 전체 순열 (3P3)
        System.out.println("\n전체 순열 {1, 2, 3}:");
        permutationList(numbers, new ArrayList<>(), new boolean[numbers.length], 3);

        // 순열 개수
        System.out.println("\n순열의 개수:");
        System.out.println("3P2 = " + nPr(3, 2));
        System.out.println("5P3 = " + nPr(5, 3));
        System.out.println("4P4 = " + nPr(4, 4) + " (4!)");
    }
}

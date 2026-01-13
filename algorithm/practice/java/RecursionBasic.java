package section2_basic.recursion;

/**
 * 재귀함수 (Recursion) 기본 개념
 *
 * ========================================
 * 1. 함수란?
 * ========================================
 *
 * 함수는 특정 작업을 수행하는 코드 블록입니다.
 * - 입력(매개변수)을 받아서
 * - 정해진 작업을 수행하고
 * - 결과(반환값)를 돌려줍니다.
 *
 * ========================================
 * 2. 재귀함수란?
 * ========================================
 *
 * 재귀함수는 자기 자신을 호출하는 함수입니다.
 *
 * 재귀함수의 구조:
 * ┌─────────────────────────────────────┐
 * │  function(input) {                  │
 * │      if (종료 조건) {               │  ← Base Case (기저 조건)
 * │          return 값;                 │
 * │      }                              │
 * │      return function(변형된 input); │  ← Recursive Case (재귀 호출)
 * │  }                                  │
 * └─────────────────────────────────────┘
 *
 * ========================================
 * 3. Base Case vs Recursive Case
 * ========================================
 *
 * Base Case (기저 조건):
 * - 재귀 호출을 멈추는 조건
 * - 이것이 없으면 무한 루프에 빠짐 (StackOverflowError)
 * - 가장 작은 문제의 답을 직접 반환
 *
 * Recursive Case (재귀 호출):
 * - 문제를 더 작은 문제로 분할
 * - 자기 자신을 호출하여 작은 문제 해결
 * - 입력값이 Base Case를 향해 변해야 함
 *
 * ========================================
 * 4. 재귀의 동작 원리
 * ========================================
 *
 * 예: sum(3) 호출 시
 *
 * sum(3)
 *   └─ 3 + sum(2)
 *          └─ 2 + sum(1)
 *                 └─ 1 + sum(0)
 *                        └─ return 0  (Base Case)
 *                 └─ return 1 + 0 = 1
 *          └─ return 2 + 1 = 3
 *   └─ return 3 + 3 = 6
 *
 * 결과: 6
 *
 * ========================================
 * 5. 재귀 vs 반복문
 * ========================================
 *
 * 재귀의 장점:
 * - 코드가 간결하고 직관적
 * - 복잡한 문제를 단순하게 표현 가능
 * - 트리 구조, 분할 정복에 적합
 *
 * 재귀의 단점:
 * - 함수 호출 오버헤드 존재
 * - 스택 메모리 사용 (깊은 재귀 시 StackOverflow)
 * - 디버깅이 어려울 수 있음
 *
 * 모든 재귀는 반복문으로 변환 가능하며, 그 역도 성립합니다.
 */
public class RecursionBasic {

    /**
     * 예제 1: 1부터 n까지의 합
     *
     * 수학적 정의:
     * - sum(0) = 0                    (Base Case)
     * - sum(n) = n + sum(n-1)         (Recursive Case)
     */
    public static int sum(int n) {
        // Base Case: n이 0이면 0 반환
        if (n == 0) {
            return 0;
        }

        // Recursive Case: n + (1부터 n-1까지의 합)
        return n + sum(n - 1);
    }

    /**
     * 예제 2: 팩토리얼 (n!)
     *
     * 수학적 정의:
     * - 0! = 1                        (Base Case)
     * - n! = n × (n-1)!               (Recursive Case)
     */
    public static long factorial(int n) {
        // Base Case
        if (n == 0) {
            return 1;
        }

        // Recursive Case
        return n * factorial(n - 1);
    }

    /**
     * 예제 3: 거듭제곱 (x^n)
     *
     * 단순 재귀 버전
     * - x^0 = 1                       (Base Case)
     * - x^n = x × x^(n-1)             (Recursive Case)
     */
    public static long power(int x, int n) {
        // Base Case
        if (n == 0) {
            return 1;
        }

        // Recursive Case
        return x * power(x, n - 1);
    }

    /**
     * 예제 3-2: 거듭제곱 최적화 버전 O(log n)
     *
     * 분할 정복을 이용한 빠른 거듭제곱
     * - x^0 = 1
     * - x^n = (x^(n/2))^2             (n이 짝수)
     * - x^n = x × (x^(n/2))^2         (n이 홀수)
     */
    public static long powerFast(int x, int n) {
        if (n == 0) {
            return 1;
        }

        // 절반 계산
        long half = powerFast(x, n / 2);

        // 짝수면 half * half
        if (n % 2 == 0) {
            return half * half;
        }
        // 홀수면 x * half * half
        else {
            return x * half * half;
        }
    }

    /**
     * 예제 4: 문자열 뒤집기
     *
     * - reverse("") = ""              (Base Case)
     * - reverse(s) = reverse(s의 나머지) + s의 첫 글자
     */
    public static String reverse(String s) {
        // Base Case: 빈 문자열이면 그대로 반환
        if (s.isEmpty()) {
            return "";
        }

        // Recursive Case: 첫 글자를 맨 뒤로
        return reverse(s.substring(1)) + s.charAt(0);
    }

    /**
     * 예제 5: 배열의 합 (분할 정복 방식)
     *
     * 배열을 반으로 나누어 각각의 합을 구한 후 더함
     */
    public static int arraySum(int[] arr, int left, int right) {
        // Base Case: 원소가 하나면 그 값 반환
        if (left == right) {
            return arr[left];
        }

        // Recursive Case: 반으로 나누어 합산
        int mid = (left + right) / 2;
        int leftSum = arraySum(arr, left, mid);
        int rightSum = arraySum(arr, mid + 1, right);

        return leftSum + rightSum;
    }

    /**
     * 예제 6: 하노이 탑
     *
     * n개의 원판을 from에서 to로 이동
     * - 1개면 바로 이동                (Base Case)
     * - n개면:                         (Recursive Case)
     *   1. n-1개를 보조 기둥으로 이동
     *   2. 가장 큰 원판을 목적지로 이동
     *   3. n-1개를 목적지로 이동
     */
    public static void hanoi(int n, char from, char to, char aux) {
        // Base Case
        if (n == 1) {
            System.out.println(from + " -> " + to);
            return;
        }

        // Recursive Case
        hanoi(n - 1, from, aux, to);    // n-1개를 보조 기둥으로
        System.out.println(from + " -> " + to);  // 가장 큰 원판 이동
        hanoi(n - 1, aux, to, from);    // n-1개를 목적지로
    }

    public static void main(String[] args) {
        System.out.println("=== 재귀함수 기본 예제 ===\n");

        // 1. 1부터 n까지의 합
        System.out.println("1) 1부터 10까지의 합: " + sum(10));

        // 2. 팩토리얼
        System.out.println("2) 5! = " + factorial(5));

        // 3. 거듭제곱
        System.out.println("3) 2^10 = " + power(2, 10));
        System.out.println("   2^10 (최적화) = " + powerFast(2, 10));

        // 4. 문자열 뒤집기
        System.out.println("4) reverse(\"hello\") = " + reverse("hello"));

        // 5. 배열의 합
        int[] arr = {1, 2, 3, 4, 5};
        System.out.println("5) 배열 {1,2,3,4,5}의 합: " + arraySum(arr, 0, arr.length - 1));

        // 6. 하노이 탑
        System.out.println("\n6) 하노이 탑 (n=3):");
        hanoi(3, 'A', 'C', 'B');
    }
}

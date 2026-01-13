package section2_basic.complexity;

/**
 * 시간 복잡도와 공간 복잡도 (Time & Space Complexity)
 *
 * ========================================
 * 1. 시간 복잡도 (Time Complexity)
 * ========================================
 *
 * 시간 복잡도란?
 * - 알고리즘이 실행되는 데 걸리는 시간을 입력 크기(n)에 대한 함수로 표현
 * - 실제 실행 시간이 아닌 "연산 횟수"를 기준으로 측정
 * - Big-O 표기법을 사용하여 최악의 경우를 표현
 *
 * 주요 시간 복잡도 (빠른 순서):
 * - O(1)       : 상수 시간 - 입력 크기와 무관하게 일정
 * - O(log n)   : 로그 시간 - 이분 탐색
 * - O(n)       : 선형 시간 - 단순 반복문
 * - O(n log n) : 선형 로그 시간 - 효율적인 정렬 (병합, 퀵)
 * - O(n²)      : 이차 시간 - 이중 반복문
 * - O(n³)      : 삼차 시간 - 삼중 반복문
 * - O(2ⁿ)      : 지수 시간 - 부분집합
 * - O(n!)      : 팩토리얼 시간 - 순열
 *
 * ========================================
 * 2. 공간 복잡도 (Space Complexity)
 * ========================================
 *
 * 공간 복잡도란?
 * - 알고리즘이 실행되는 데 필요한 메모리 공간을 입력 크기(n)에 대한 함수로 표현
 * - 변수, 배열, 재귀 호출 스택 등을 고려
 *
 * ========================================
 * 3. 코딩 테스트에서의 활용
 * ========================================
 *
 * 일반적인 기준 (1초 시간 제한 기준):
 * - n ≤ 10      : O(n!) 가능
 * - n ≤ 20      : O(2ⁿ) 가능
 * - n ≤ 500     : O(n³) 가능
 * - n ≤ 5,000   : O(n²) 가능
 * - n ≤ 100,000 : O(n log n) 필요
 * - n ≤ 10,000,000 : O(n) 필요
 * - 그 이상     : O(log n) 또는 O(1) 필요
 *
 * 메모리 제한 (보통 256MB 기준):
 * - int 배열: 약 6천만 개
 * - long 배열: 약 3천만 개
 * - int[][] 2차원 배열: 약 7,000 x 7,000
 */
public class TimeSpaceComplexity {

    /**
     * O(1) - 상수 시간
     * 입력 크기와 관계없이 항상 같은 시간이 걸림
     */
    public static int constantTime(int[] arr) {
        // 배열 크기와 무관하게 첫 번째 요소만 접근
        return arr[0];
    }

    /**
     * O(log n) - 로그 시간
     * 매 단계마다 탐색 범위가 절반으로 줄어듦
     */
    public static int logarithmicTime(int[] arr, int target) {
        int left = 0;
        int right = arr.length - 1;

        while (left <= right) {
            int mid = (left + right) / 2;

            if (arr[mid] == target) {
                return mid;
            } else if (arr[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return -1;
    }

    /**
     * O(n) - 선형 시간
     * 입력 크기에 비례하여 시간이 증가
     */
    public static int linearTime(int[] arr) {
        int sum = 0;

        // n번 반복
        for (int num : arr) {
            sum += num;
        }

        return sum;
    }

    /**
     * O(n²) - 이차 시간
     * 이중 반복문으로 인해 n × n 번 연산
     */
    public static void quadraticTime(int[] arr) {
        int n = arr.length;

        // 버블 정렬 예시
        for (int i = 0; i < n - 1; i++) {          // n번
            for (int j = 0; j < n - 1 - i; j++) {  // n번
                if (arr[j] > arr[j + 1]) {
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                }
            }
        }
    }

    /**
     * O(2ⁿ) - 지수 시간
     * 재귀적으로 2개씩 분기하는 경우
     */
    public static int exponentialTime(int n) {
        // 비효율적인 피보나치 (예시용)
        if (n <= 1) {
            return n;
        }
        return exponentialTime(n - 1) + exponentialTime(n - 2);
    }

    /**
     * 공간 복잡도 예시
     */

    // O(1) 공간 - 추가 공간 사용 없음
    public static int spaceConstant(int[] arr) {
        int sum = 0;  // 변수 1개만 사용
        for (int num : arr) {
            sum += num;
        }
        return sum;
    }

    // O(n) 공간 - 입력 크기만큼 배열 생성
    public static int[] spaceLinear(int n) {
        int[] result = new int[n];  // n 크기의 배열 생성
        for (int i = 0; i < n; i++) {
            result[i] = i * 2;
        }
        return result;
    }

    // O(n) 공간 - 재귀 호출 스택
    public static int spaceRecursive(int n) {
        if (n <= 0) {
            return 0;
        }
        // 재귀 호출마다 스택 프레임이 쌓임 (n개)
        return n + spaceRecursive(n - 1);
    }

    public static void main(String[] args) {
        System.out.println("=== 시간/공간 복잡도 예제 ===\n");

        int[] arr = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

        // O(1) 예시
        System.out.println("O(1) 상수 시간: " + constantTime(arr));

        // O(log n) 예시
        System.out.println("O(log n) 로그 시간 - 5 찾기: 인덱스 " + logarithmicTime(arr, 5));

        // O(n) 예시
        System.out.println("O(n) 선형 시간 - 합계: " + linearTime(arr));

        // O(2^n) 예시 (작은 n으로 테스트)
        System.out.println("O(2^n) 지수 시간 - fib(10): " + exponentialTime(10));

        System.out.println("\n각 복잡도별 n에 따른 연산 횟수:");
        System.out.println("n=10    | O(n)=10, O(n²)=100, O(2^n)=1,024");
        System.out.println("n=100   | O(n)=100, O(n²)=10,000, O(2^n)=매우 큼");
        System.out.println("n=1000  | O(n)=1,000, O(n²)=1,000,000");
    }
}

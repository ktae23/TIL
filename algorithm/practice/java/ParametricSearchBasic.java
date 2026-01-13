package section5_essential2.parametric;

/**
 * 파라메트릭 서치 (Parametric Search) 기본 개념
 *
 * ========================================
 * 1. 파라메트릭 서치란?
 * ========================================
 *
 * 최적화 문제를 결정 문제로 바꾸어 이분 탐색으로 해결하는 기법입니다.
 *
 * 최적화 문제: "최댓값/최솟값을 구하라"
 * 결정 문제: "주어진 값이 조건을 만족하는가?"
 *
 * 예:
 * - 최적화: "가장 큰 X를 구하라"
 * - 결정: "X가 가능한가?" → Yes/No
 *
 * ========================================
 * 2. 적용 조건
 * ========================================
 *
 * 결정 함수 f(x)가 단조성을 가져야 함:
 * - f(x)가 True이면, x보다 작은(또는 큰) 모든 값도 True
 * - 경계값을 이분 탐색으로 찾을 수 있음
 *
 * 예: "최소 X 이상 가능한가?"
 * - x=5 불가능, x=6 불가능, x=7 가능, x=8 가능 ...
 * - 경계값 7을 이분 탐색으로 찾기
 *
 * ========================================
 * 3. 구현 패턴
 * ========================================
 *
 * while (left < right) {
 *     mid = (left + right) / 2;  // 또는 (left + right + 1) / 2
 *     if (check(mid)) {
 *         // 조건에 따라 범위 조정
 *     } else {
 *         // 반대쪽으로 범위 조정
 *     }
 * }
 *
 * ========================================
 * 4. 대표 문제 유형
 * ========================================
 *
 * 1) 최댓값의 최솟값, 최솟값의 최댓값 유형
 *    - 예: "최대 거리의 최솟값을 구하라"
 *
 * 2) "K개 이상 만들 수 있는 최대 길이"
 *    - 예: 나무 자르기, 랜선 자르기
 *
 * 3) "조건을 만족하는 최소/최대 값"
 *    - 예: 공유기 설치, 입국심사
 */
public class ParametricSearchBasic {

    /**
     * 예제 1: 랜선 자르기
     *
     * K개의 랜선을 잘라 N개 이상의 같은 길이 랜선을 만들 때,
     * 만들 수 있는 랜선의 최대 길이
     */
    public static long maxLanLength(int[] lans, int need) {
        long left = 1;
        long right = 0;

        // right는 가장 긴 랜선 길이
        for (int lan : lans) {
            right = Math.max(right, lan);
        }

        while (left < right) {
            // 최댓값을 찾으므로 올림
            long mid = (left + right + 1) / 2;

            if (canMake(lans, mid, need)) {
                left = mid;       // mid로 가능하면 더 큰 값 시도
            } else {
                right = mid - 1;  // mid로 불가능하면 더 작은 값
            }
        }

        return left;
    }

    private static boolean canMake(int[] lans, long length, int need) {
        long count = 0;
        for (int lan : lans) {
            count += lan / length;
        }
        return count >= need;
    }

    /**
     * 예제 2: 나무 자르기
     *
     * 높이 H로 나무를 자를 때,
     * M 미터 이상의 나무를 얻을 수 있는 H의 최댓값
     */
    public static long maxCutHeight(int[] trees, long need) {
        long left = 0;
        long right = 0;

        for (int tree : trees) {
            right = Math.max(right, tree);
        }

        while (left < right) {
            long mid = (left + right + 1) / 2;

            if (getWood(trees, mid) >= need) {
                left = mid;
            } else {
                right = mid - 1;
            }
        }

        return left;
    }

    private static long getWood(int[] trees, long height) {
        long total = 0;
        for (int tree : trees) {
            if (tree > height) {
                total += tree - height;
            }
        }
        return total;
    }

    /**
     * 예제 3: 최소 거리의 최댓값
     *
     * N개의 위치 중 K개를 선택할 때,
     * 인접한 것 사이의 최소 거리의 최댓값
     * (공유기 설치 문제와 유사)
     */
    public static int maxMinDistance(int[] positions, int k) {
        // 정렬 필수
        java.util.Arrays.sort(positions);

        int left = 1;  // 최소 거리 1
        int right = positions[positions.length - 1] - positions[0];  // 최대 가능 거리

        while (left < right) {
            int mid = (left + right + 1) / 2;

            if (canPlace(positions, mid, k)) {
                left = mid;
            } else {
                right = mid - 1;
            }
        }

        return left;
    }

    private static boolean canPlace(int[] positions, int minDist, int k) {
        int count = 1;  // 첫 번째 위치에 배치
        int lastPos = positions[0];

        for (int i = 1; i < positions.length; i++) {
            if (positions[i] - lastPos >= minDist) {
                count++;
                lastPos = positions[i];
            }
        }

        return count >= k;
    }

    public static void main(String[] args) {
        System.out.println("=== 파라메트릭 서치 예제 ===\n");

        // 예제 1: 랜선 자르기
        System.out.println("1) 랜선 자르기");
        int[] lans = {802, 743, 457, 539};
        int need = 11;
        System.out.println("최대 랜선 길이: " + maxLanLength(lans, need));

        // 예제 2: 나무 자르기
        System.out.println("\n2) 나무 자르기");
        int[] trees = {20, 15, 10, 17};
        long needWood = 7;
        System.out.println("최대 절단 높이: " + maxCutHeight(trees, needWood));

        // 예제 3: 공유기 설치
        System.out.println("\n3) 공유기 설치 (최소 거리 최대화)");
        int[] houses = {1, 2, 4, 8, 9};
        int routers = 3;
        System.out.println("최소 거리의 최댓값: " + maxMinDistance(houses, routers));
    }
}

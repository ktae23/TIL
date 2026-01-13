package section3_essential1.greedy;

import java.util.Arrays;

/**
 * 그리디 (Greedy) 알고리즘 기본 개념
 *
 * ========================================
 * 1. 그리디 알고리즘이란?
 * ========================================
 *
 * 그리디(탐욕) 알고리즘은 매 순간 가장 좋아 보이는 선택을 하는 알고리즘입니다.
 *
 * 특징:
 * - 현재 상황에서 지역적으로 최적인 선택
 * - 이전 선택을 번복하지 않음
 * - 빠르고 구현이 간단함
 *
 * 주의:
 * - 항상 최적해를 보장하지는 않음
 * - 그리디가 최적해를 보장하는지 증명 필요
 *
 * ========================================
 * 2. 그리디 적용 조건
 * ========================================
 *
 * 1) 탐욕적 선택 속성 (Greedy Choice Property)
 *    - 현재의 최적 선택이 이후에도 최적
 *
 * 2) 최적 부분 구조 (Optimal Substructure)
 *    - 전체 문제의 최적해가 부분 문제의 최적해로 구성됨
 *
 * ========================================
 * 3. 대표적인 그리디 문제
 * ========================================
 *
 * 1) 거스름돈 문제
 *    - 동전 단위가 배수 관계일 때만 그리디 가능
 *
 * 2) 활동 선택 문제 (Activity Selection)
 *    - 끝나는 시간 기준 정렬 후 선택
 *
 * 3) 분할 가능 배낭 (Fractional Knapsack)
 *    - 무게당 가치 기준으로 선택
 *
 * 4) 최소 신장 트리 (MST)
 *    - Kruskal, Prim 알고리즘
 *
 * 5) 다익스트라 알고리즘
 *    - 최단 경로 찾기
 *
 * ========================================
 * 4. 그리디 vs DP
 * ========================================
 *
 * 그리디:
 * - 지역적 최적 선택
 * - 빠름 (보통 O(n log n))
 * - 최적해 보장 안 될 수 있음
 *
 * DP:
 * - 모든 경우를 고려
 * - 느림 (보통 O(n²) 이상)
 * - 항상 최적해 보장
 */
public class GreedyBasic {

    /**
     * 예제 1: 거스름돈 (동전 단위가 배수 관계)
     *
     * 큰 단위부터 최대한 많이 사용
     */
    public static int countCoins(int amount, int[] coins) {
        // 동전을 내림차순 정렬
        Integer[] sortedCoins = Arrays.stream(coins).boxed().toArray(Integer[]::new);
        Arrays.sort(sortedCoins, (a, b) -> b - a);

        int count = 0;
        for (int coin : sortedCoins) {
            count += amount / coin;
            amount %= coin;
        }

        return count;
    }

    /**
     * 예제 2: 활동 선택 문제
     *
     * 각 활동의 시작/종료 시간이 주어질 때,
     * 최대한 많은 활동을 선택하는 문제
     *
     * 전략: 종료 시간이 빠른 순서로 정렬하고 선택
     */
    public static int activitySelection(int[][] activities) {
        // 종료 시간 기준 정렬
        Arrays.sort(activities, (a, b) -> a[1] - b[1]);

        int count = 1;
        int lastEnd = activities[0][1];

        for (int i = 1; i < activities.length; i++) {
            // 이전 활동 종료 후 시작할 수 있으면 선택
            if (activities[i][0] >= lastEnd) {
                count++;
                lastEnd = activities[i][1];
            }
        }

        return count;
    }

    /**
     * 예제 3: 분할 가능 배낭 (Fractional Knapsack)
     *
     * 물건을 쪼갤 수 있을 때 배낭에 담을 수 있는 최대 가치
     *
     * 전략: 무게당 가치가 높은 순서로 담기
     */
    public static double fractionalKnapsack(int capacity, int[][] items) {
        // items[i] = {가치, 무게}
        // 무게당 가치 = 가치 / 무게

        // 무게당 가치 기준 내림차순 정렬
        Arrays.sort(items, (a, b) -> {
            double ratioA = (double) a[0] / a[1];
            double ratioB = (double) b[0] / b[1];
            return Double.compare(ratioB, ratioA);
        });

        double totalValue = 0;
        int remainingCapacity = capacity;

        for (int[] item : items) {
            int value = item[0];
            int weight = item[1];

            if (weight <= remainingCapacity) {
                // 전체를 담을 수 있으면 전체 담기
                totalValue += value;
                remainingCapacity -= weight;
            } else {
                // 일부만 담기 (분할)
                totalValue += (double) value * remainingCapacity / weight;
                break;
            }
        }

        return totalValue;
    }

    /**
     * 예제 4: 회의실 배정 (Meeting Room)
     *
     * N개의 회의 중 최대한 많은 회의를 진행
     * = 활동 선택 문제와 동일
     */
    public static int maxMeetings(int[][] meetings) {
        return activitySelection(meetings);
    }

    /**
     * 예제 5: 주유소 문제
     *
     * 각 도시에서의 기름 가격이 다를 때,
     * 최소 비용으로 목적지까지 이동
     *
     * 전략: 더 싼 주유소가 나올 때까지 현재 주유소에서 주유
     */
    public static long minFuelCost(int[] distances, int[] prices) {
        int n = prices.length;
        long totalCost = 0;
        int minPrice = prices[0];

        for (int i = 0; i < n - 1; i++) {
            // 현재까지의 최소 가격 갱신
            minPrice = Math.min(minPrice, prices[i]);
            totalCost += (long) minPrice * distances[i];
        }

        return totalCost;
    }

    public static void main(String[] args) {
        System.out.println("=== 그리디 알고리즘 예제 ===\n");

        // 예제 1: 거스름돈
        System.out.println("1) 거스름돈 (1260원)");
        int[] coins = {500, 100, 50, 10};
        System.out.println("필요한 동전 개수: " + countCoins(1260, coins));

        // 예제 2: 활동 선택
        System.out.println("\n2) 활동 선택");
        int[][] activities = {{1, 4}, {3, 5}, {0, 6}, {5, 7}, {3, 9}, {5, 9}, {6, 10}, {8, 11}, {8, 12}, {2, 14}, {12, 16}};
        System.out.println("최대 활동 수: " + activitySelection(activities));

        // 예제 3: 분할 가능 배낭
        System.out.println("\n3) 분할 가능 배낭 (용량: 50)");
        int[][] items = {{60, 10}, {100, 20}, {120, 30}};  // {가치, 무게}
        System.out.println("최대 가치: " + fractionalKnapsack(50, items));

        // 예제 4: 회의실 배정
        System.out.println("\n4) 회의실 배정");
        int[][] meetings = {{1, 4}, {3, 5}, {5, 7}, {6, 10}, {8, 12}};
        System.out.println("최대 회의 수: " + maxMeetings(meetings));

        // 예제 5: 주유소
        System.out.println("\n5) 주유소 (최소 비용)");
        int[] distances = {2, 3, 1};
        int[] prices = {5, 2, 4, 1};
        System.out.println("최소 비용: " + minFuelCost(distances, prices));
    }
}

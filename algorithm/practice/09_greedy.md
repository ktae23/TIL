# 9. 그리디 알고리즘 (Greedy)

> 관련 코드: `section3_essential1/greedy/GreedyBasic.java`

---

## 1. 그리디 알고리즘이란?

**매 순간 가장 좋아 보이는 선택**을 하는 알고리즘입니다.

### 특징
- 현재 상황에서 **지역적으로 최적**인 선택
- 이전 선택을 번복하지 않음
- 빠르고 구현이 간단함

### 주의
- **항상 최적해를 보장하지는 않음**
- 그리디가 최적해를 보장하는지 **증명 필요**

---

## 2. 그리디 적용 조건

### 1) 탐욕적 선택 속성 (Greedy Choice Property)
현재의 최적 선택이 이후에도 최적

### 2) 최적 부분 구조 (Optimal Substructure)
전체 문제의 최적해가 부분 문제의 최적해로 구성됨

---

## 3. 대표적인 그리디 문제

| 문제 | 전략 |
|------|------|
| 거스름돈 | 큰 단위부터 (배수 관계일 때만) |
| 활동 선택 | 종료 시간 기준 정렬 후 선택 |
| 분할 가능 배낭 | 무게당 가치 기준 선택 |
| 최소 신장 트리 | Kruskal, Prim |
| 최단 경로 | 다익스트라 |

---

## 4. 그리디 vs DP

| 구분 | 그리디 | DP |
|------|--------|-----|
| 선택 | 지역적 최적 | 모든 경우 고려 |
| 속도 | 빠름 (보통 O(n log n)) | 느림 (보통 O(n²) 이상) |
| 최적해 | 보장 안 될 수 있음 | 항상 보장 |

---

## 5. 구현 예제

### 거스름돈
```java
// 동전 단위가 배수 관계일 때만 그리디 가능
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
```

### 활동 선택 (회의실 배정)
```java
// 종료 시간 기준 정렬 후 선택
public static int activitySelection(int[][] activities) {
    Arrays.sort(activities, (a, b) -> a[1] - b[1]);  // 종료 시간 기준 정렬

    int count = 1;
    int lastEnd = activities[0][1];

    for (int i = 1; i < activities.length; i++) {
        if (activities[i][0] >= lastEnd) {  // 이전 활동 종료 후 시작 가능
            count++;
            lastEnd = activities[i][1];
        }
    }
    return count;
}
```

### 분할 가능 배낭 (Fractional Knapsack)
```java
// 무게당 가치가 높은 순서로 담기
public static double fractionalKnapsack(int capacity, int[][] items) {
    // items[i] = {가치, 무게}
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
```

### 주유소 문제
```java
// 더 싼 주유소가 나올 때까지 현재 주유소에서 주유
public static long minFuelCost(int[] distances, int[] prices) {
    int n = prices.length;
    long totalCost = 0;
    int minPrice = prices[0];

    for (int i = 0; i < n - 1; i++) {
        minPrice = Math.min(minPrice, prices[i]);
        totalCost += (long) minPrice * distances[i];
    }
    return totalCost;
}
```

---

## 핵심 정리

1. 그리디는 **"현재 최선의 선택"**
2. **최적해 보장 여부**를 반드시 확인
3. 정렬 후 탐색하는 패턴이 많음
4. 틀리면 DP로 전환 고려

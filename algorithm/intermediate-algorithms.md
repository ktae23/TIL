# 중급 알고리즘

기초를 넘어서는 효율적인 알고리즘들을 학습합니다. 분할 정복, 그래프 탐색, 그리디, 동적 프로그래밍의 기본 개념을 다룹니다.

## 목차
1. [고급 정렬 알고리즘](#고급-정렬-알고리즘)
2. [그래프 탐색](#그래프-탐색)
3. [그리디 알고리즘](#그리디-알고리즘)
4. [동적 프로그래밍 기초](#동적-프로그래밍-기초)

---

## 고급 정렬 알고리즘

### 1. 퀵 정렬 (Quick Sort)

**개념**: 분할 정복(Divide and Conquer) 방식을 사용합니다. 피벗(pivot)을 선택하여 피벗보다 작은 값은 왼쪽, 큰 값은 오른쪽으로 분할한 후 재귀적으로 정렬합니다.

**시간 복잡도**:
- 평균: O(n log n)
- 최악: O(n²) - 이미 정렬된 경우
**공간 복잡도**: O(log n) - 재귀 호출 스택

```java
public class QuickSort {
    public static void quickSort(int[] arr, int low, int high) {
        if (low < high) {
            // 파티션 인덱스 찾기
            int pi = partition(arr, low, high);

            // 피벗 기준 왼쪽 부분 정렬
            quickSort(arr, low, pi - 1);
            // 피벗 기준 오른쪽 부분 정렬
            quickSort(arr, pi + 1, high);
        }
    }

    private static int partition(int[] arr, int low, int high) {
        // 마지막 원소를 피벗으로 선택
        int pivot = arr[high];
        int i = low - 1; // 작은 원소들의 인덱스

        for (int j = low; j < high; j++) {
            // 현재 원소가 피벗보다 작으면
            if (arr[j] < pivot) {
                i++;
                // arr[i]와 arr[j] 교환
                int temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }

        // 피벗을 올바른 위치에 배치
        int temp = arr[i + 1];
        arr[i + 1] = arr[high];
        arr[high] = temp;

        return i + 1;
    }

    public static void main(String[] args) {
        int[] arr = {10, 7, 8, 9, 1, 5};
        quickSort(arr, 0, arr.length - 1);
        System.out.println(Arrays.toString(arr));
        // 출력: [1, 5, 7, 8, 9, 10]
    }
}
```

### 2. 합병 정렬 (Merge Sort)

**개념**: 분할 정복 방식으로 배열을 절반으로 나눈 후, 각각을 정렬하고 합치는 과정을 반복합니다. 항상 O(n log n)을 보장하는 안정적인 알고리즘입니다.

**시간 복잡도**: O(n log n) - 모든 경우
**공간 복잡도**: O(n) - 추가 배열 필요

```java
public class MergeSort {
    public static void mergeSort(int[] arr, int left, int right) {
        if (left < right) {
            // 중간 지점 찾기
            int mid = left + (right - left) / 2;

            // 왼쪽 절반 정렬
            mergeSort(arr, left, mid);
            // 오른쪽 절반 정렬
            mergeSort(arr, mid + 1, right);

            // 합치기
            merge(arr, left, mid, right);
        }
    }

    private static void merge(int[] arr, int left, int mid, int right) {
        // 서브 배열의 크기 계산
        int n1 = mid - left + 1;
        int n2 = right - mid;

        // 임시 배열 생성
        int[] L = new int[n1];
        int[] R = new int[n2];

        // 데이터 복사
        for (int i = 0; i < n1; i++) {
            L[i] = arr[left + i];
        }
        for (int j = 0; j < n2; j++) {
            R[j] = arr[mid + 1 + j];
        }

        // 합치기
        int i = 0, j = 0;
        int k = left;

        while (i < n1 && j < n2) {
            if (L[i] <= R[j]) {
                arr[k] = L[i];
                i++;
            } else {
                arr[k] = R[j];
                j++;
            }
            k++;
        }

        // 남은 원소들 복사
        while (i < n1) {
            arr[k] = L[i];
            i++;
            k++;
        }

        while (j < n2) {
            arr[k] = R[j];
            j++;
            k++;
        }
    }

    public static void main(String[] args) {
        int[] arr = {12, 11, 13, 5, 6, 7};
        mergeSort(arr, 0, arr.length - 1);
        System.out.println(Arrays.toString(arr));
        // 출력: [5, 6, 7, 11, 12, 13]
    }
}
```

---

## 그래프 탐색

그래프는 **정점(Vertex)**과 **간선(Edge)**으로 구성된 자료구조입니다. 그래프 탐색은 모든 정점을 방문하는 방법입니다.

### 1. 깊이 우선 탐색 (DFS - Depth First Search)

**개념**: 한 방향으로 끝까지 탐색한 후 다시 돌아와서 다른 방향을 탐색합니다. **스택** 또는 **재귀**를 사용합니다.

**시간 복잡도**: O(V + E) - V는 정점 수, E는 간선 수
**공간 복잡도**: O(V) - 재귀 스택 또는 명시적 스택

```java
import java.util.*;

public class DFS {
    private int V; // 정점의 개수
    private LinkedList<Integer>[] adj; // 인접 리스트

    public DFS(int v) {
        V = v;
        adj = new LinkedList[v];
        for (int i = 0; i < v; i++) {
            adj[i] = new LinkedList<>();
        }
    }

    // 간선 추가
    public void addEdge(int v, int w) {
        adj[v].add(w);
    }

    // DFS 재귀 함수
    private void dfsUtil(int v, boolean[] visited) {
        // 현재 노드를 방문 처리
        visited[v] = true;
        System.out.print(v + " ");

        // 인접한 모든 정점을 재귀적으로 방문
        for (int n : adj[v]) {
            if (!visited[n]) {
                dfsUtil(n, visited);
            }
        }
    }

    // DFS 시작
    public void dfs(int start) {
        boolean[] visited = new boolean[V];
        dfsUtil(start, visited);
    }

    // 스택을 사용한 반복적 DFS
    public void dfsIterative(int start) {
        boolean[] visited = new boolean[V];
        Stack<Integer> stack = new Stack<>();

        stack.push(start);

        while (!stack.isEmpty()) {
            int v = stack.pop();

            if (!visited[v]) {
                visited[v] = true;
                System.out.print(v + " ");

                // 인접 정점들을 스택에 추가
                for (int n : adj[v]) {
                    if (!visited[n]) {
                        stack.push(n);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        DFS graph = new DFS(4);

        graph.addEdge(0, 1);
        graph.addEdge(0, 2);
        graph.addEdge(1, 2);
        graph.addEdge(2, 0);
        graph.addEdge(2, 3);
        graph.addEdge(3, 3);

        System.out.print("DFS (재귀): ");
        graph.dfs(2);
        // 출력: 2 0 1 3

        System.out.print("\nDFS (반복): ");
        graph.dfsIterative(2);
    }
}
```

### 2. 너비 우선 탐색 (BFS - Breadth First Search)

**개념**: 시작 정점에서 가까운 정점부터 순서대로 탐색합니다. **큐(Queue)**를 사용합니다. 최단 경로를 찾을 때 유용합니다.

**시간 복잡도**: O(V + E)
**공간 복잡도**: O(V) - 큐

```java
import java.util.*;

public class BFS {
    private int V;
    private LinkedList<Integer>[] adj;

    public BFS(int v) {
        V = v;
        adj = new LinkedList[v];
        for (int i = 0; i < v; i++) {
            adj[i] = new LinkedList<>();
        }
    }

    public void addEdge(int v, int w) {
        adj[v].add(w);
    }

    public void bfs(int start) {
        boolean[] visited = new boolean[V];
        Queue<Integer> queue = new LinkedList<>();

        // 시작 노드를 방문 처리하고 큐에 추가
        visited[start] = true;
        queue.add(start);

        while (!queue.isEmpty()) {
            // 큐에서 하나 꺼내기
            int v = queue.poll();
            System.out.print(v + " ");

            // 인접한 모든 정점 확인
            for (int n : adj[v]) {
                if (!visited[n]) {
                    visited[n] = true;
                    queue.add(n);
                }
            }
        }
    }

    // 최단 거리 구하기
    public int[] shortestPath(int start) {
        boolean[] visited = new boolean[V];
        int[] distance = new int[V];
        Arrays.fill(distance, -1);

        Queue<Integer> queue = new LinkedList<>();
        visited[start] = true;
        distance[start] = 0;
        queue.add(start);

        while (!queue.isEmpty()) {
            int v = queue.poll();

            for (int n : adj[v]) {
                if (!visited[n]) {
                    visited[n] = true;
                    distance[n] = distance[v] + 1;
                    queue.add(n);
                }
            }
        }

        return distance;
    }

    public static void main(String[] args) {
        BFS graph = new BFS(4);

        graph.addEdge(0, 1);
        graph.addEdge(0, 2);
        graph.addEdge(1, 2);
        graph.addEdge(2, 0);
        graph.addEdge(2, 3);
        graph.addEdge(3, 3);

        System.out.print("BFS: ");
        graph.bfs(2);
        // 출력: 2 0 3 1

        System.out.println("\n최단 거리:");
        int[] distances = graph.shortestPath(2);
        for (int i = 0; i < distances.length; i++) {
            System.out.println("2 -> " + i + ": " + distances[i]);
        }
    }
}
```

---

## 그리디 알고리즘

**개념**: 매 순간 최선의 선택을 하는 알고리즘입니다. 항상 최적해를 보장하지는 않지만, 특정 문제에서는 최적해를 찾을 수 있습니다.

**특징**:
- 빠른 실행 시간
- 직관적인 해결 방법
- 모든 문제에 적용 불가능

### 예제 1: 거스름돈 문제

**시간 복잡도**: O(n) - n은 동전 종류의 수
**공간 복잡도**: O(1)

```java
public class CoinChange {
    public static int getMinCoins(int amount) {
        int[] coins = {500, 100, 50, 10}; // 동전 종류 (큰 것부터)
        int count = 0;

        for (int coin : coins) {
            count += amount / coin; // 해당 동전 개수 추가
            amount %= coin; // 남은 금액 갱신
        }

        return count;
    }

    public static void main(String[] args) {
        int amount = 1260;
        int result = getMinCoins(amount);
        System.out.println("최소 동전 개수: " + result);
        // 출력: 최소 동전 개수: 6 (500*2 + 100*2 + 50*1 + 10*1)
    }
}
```

### 예제 2: 활동 선택 문제

**개념**: 시작 시간과 종료 시간이 있는 여러 활동 중, 겹치지 않게 최대한 많은 활동을 선택합니다.

**시간 복잡도**: O(n log n) - 정렬 때문
**공간 복잡도**: O(1)

```java
import java.util.*;

class Activity {
    int start, end;

    Activity(int start, int end) {
        this.start = start;
        this.end = end;
    }
}

public class ActivitySelection {
    public static List<Activity> selectActivities(Activity[] activities) {
        // 종료 시간 기준으로 정렬
        Arrays.sort(activities, Comparator.comparingInt(a -> a.end));

        List<Activity> selected = new ArrayList<>();
        selected.add(activities[0]);
        int lastEnd = activities[0].end;

        for (int i = 1; i < activities.length; i++) {
            // 시작 시간이 이전 활동의 종료 시간 이후면 선택
            if (activities[i].start >= lastEnd) {
                selected.add(activities[i]);
                lastEnd = activities[i].end;
            }
        }

        return selected;
    }

    public static void main(String[] args) {
        Activity[] activities = {
            new Activity(1, 4),
            new Activity(3, 5),
            new Activity(0, 6),
            new Activity(5, 7),
            new Activity(8, 9),
            new Activity(5, 9)
        };

        List<Activity> result = selectActivities(activities);
        System.out.println("선택된 활동 수: " + result.size());
        // 출력: 선택된 활동 수: 4
    }
}
```

---

## 동적 프로그래밍 기초

**개념**: 큰 문제를 작은 부분 문제로 나누고, 한 번 계산한 결과를 **메모이제이션(Memoization)** 또는 **테이블화(Tabulation)**로 저장하여 재사용합니다.

**핵심 조건**:
1. **최적 부분 구조**: 큰 문제의 최적해가 작은 문제의 최적해로 구성됨
2. **중복되는 부분 문제**: 같은 계산이 반복됨

### 예제 1: 피보나치 수열

**시간 복잡도**: O(n)
**공간 복잡도**: O(n)

```java
public class Fibonacci {
    // 일반 재귀 (비효율적 - O(2^n))
    public static int fibRecursive(int n) {
        if (n <= 1) return n;
        return fibRecursive(n - 1) + fibRecursive(n - 2);
    }

    // 메모이제이션 (Top-Down DP)
    public static int fibMemo(int n, int[] memo) {
        if (n <= 1) return n;
        if (memo[n] != 0) return memo[n]; // 이미 계산됨

        memo[n] = fibMemo(n - 1, memo) + fibMemo(n - 2, memo);
        return memo[n];
    }

    // 테이블화 (Bottom-Up DP)
    public static int fibTabulation(int n) {
        if (n <= 1) return n;

        int[] dp = new int[n + 1];
        dp[0] = 0;
        dp[1] = 1;

        for (int i = 2; i <= n; i++) {
            dp[i] = dp[i - 1] + dp[i - 2];
        }

        return dp[n];
    }

    // 공간 최적화 버전
    public static int fibOptimized(int n) {
        if (n <= 1) return n;

        int prev2 = 0, prev1 = 1;
        int current = 0;

        for (int i = 2; i <= n; i++) {
            current = prev1 + prev2;
            prev2 = prev1;
            prev1 = current;
        }

        return current;
    }

    public static void main(String[] args) {
        int n = 10;

        System.out.println("재귀: " + fibRecursive(n));
        System.out.println("메모이제이션: " + fibMemo(n, new int[n + 1]));
        System.out.println("테이블화: " + fibTabulation(n));
        System.out.println("최적화: " + fibOptimized(n));
        // 모두 출력: 55
    }
}
```

### 예제 2: 계단 오르기

**문제**: n개의 계단을 오르는데, 한 번에 1계단 또는 2계단씩 오를 수 있을 때 방법의 수를 구합니다.

**시간 복잡도**: O(n)
**공간 복잡도**: O(n) 또는 O(1)

```java
public class ClimbingStairs {
    // DP 테이블화
    public static int climbStairs(int n) {
        if (n <= 2) return n;

        int[] dp = new int[n + 1];
        dp[1] = 1; // 1계단: 1가지
        dp[2] = 2; // 2계단: 2가지

        for (int i = 3; i <= n; i++) {
            dp[i] = dp[i - 1] + dp[i - 2];
        }

        return dp[n];
    }

    // 공간 최적화
    public static int climbStairsOptimized(int n) {
        if (n <= 2) return n;

        int prev2 = 1, prev1 = 2;
        int current = 0;

        for (int i = 3; i <= n; i++) {
            current = prev1 + prev2;
            prev2 = prev1;
            prev1 = current;
        }

        return current;
    }

    public static void main(String[] args) {
        int n = 5;
        System.out.println(n + "계단 오르는 방법: " + climbStairs(n));
        // 출력: 8가지
    }
}
```

### 예제 3: 최소 비용으로 계단 오르기

**문제**: 각 계단마다 비용이 있고, 한 번에 1~2계단씩 오를 수 있을 때 최소 비용을 구합니다.

**시간 복잡도**: O(n)
**공간 복잡도**: O(n)

```java
public class MinCostClimbingStairs {
    public static int minCostClimbingStairs(int[] cost) {
        int n = cost.length;
        int[] dp = new int[n + 1];

        // 0번째와 1번째는 비용 없음 (시작점)
        dp[0] = 0;
        dp[1] = 0;

        for (int i = 2; i <= n; i++) {
            // i번째 계단에 도달하는 최소 비용
            dp[i] = Math.min(
                dp[i - 1] + cost[i - 1],  // 1계단 전에서 오는 경우
                dp[i - 2] + cost[i - 2]   // 2계단 전에서 오는 경우
            );
        }

        return dp[n];
    }

    public static void main(String[] args) {
        int[] cost = {10, 15, 20};
        System.out.println("최소 비용: " + minCostClimbingStairs(cost));
        // 출력: 15 (1번 계단에서 바로 꼭대기로)
    }
}
```

---

## 정리

### 정렬 알고리즘 비교

| 알고리즘 | 시간 복잡도 (평균) | 공간 복잡도 | 안정성 | 특징 |
|---------|------------------|-----------|-------|-----|
| 퀵 정렬 | O(n log n) | O(log n) | 불안정 | 빠름, 피벗 선택 중요 |
| 합병 정렬 | O(n log n) | O(n) | 안정 | 항상 O(n log n) 보장 |

### DFS vs BFS

| 특징 | DFS | BFS |
|-----|-----|-----|
| 자료구조 | 스택/재귀 | 큐 |
| 시간 복잡도 | O(V + E) | O(V + E) |
| 공간 복잡도 | O(V) | O(V) |
| 최단 경로 | X | O (가중치 없는 그래프) |
| 사용 사례 | 경로 존재 확인, 사이클 검출 | 최단 거리, 레벨별 탐색 |

### 학습 팁
1. **분할 정복**: 문제를 작게 나눠 해결 (퀵/합병 정렬)
2. **그래프**: 인접 리스트 vs 인접 행렬 구현 차이 이해
3. **그리디**: 항상 최적해를 보장하지 않음을 주의
4. **DP**: 재귀 → 메모이제이션 → 테이블화 순으로 학습
5. **시간 복잡도**: O(n log n)이 중급 알고리즘의 핵심

---

*마지막 업데이트: 2025년 12월*

# 고급 알고리즘

효율성과 최적화가 중요한 고급 알고리즘들을 다룹니다. 힙 정렬, 최단 경로 알고리즘, 고급 동적 프로그래밍을 학습합니다.

## 목차
1. [고급 정렬 - 힙 정렬](#고급-정렬---힙-정렬)
2. [최단 경로 알고리즘](#최단-경로-알고리즘)
3. [고급 동적 프로그래밍](#고급-동적-프로그래밍)
4. [고급 조합 최적화](#고급-조합-최적화)

---

## 고급 정렬 - 힙 정렬

### 힙 정렬 (Heap Sort)

**개념**: 힙(Heap) 자료구조를 이용한 정렬입니다. 최대 힙을 구성한 후, 루트 노드(최댓값)를 배열 끝으로 보내는 과정을 반복합니다.

**힙이란?**: 완전 이진 트리의 일종으로, 부모 노드가 자식 노드보다 크거나(최대 힙) 작습니다(최소 힙).

**시간 복잡도**: O(n log n) - 모든 경우
**공간 복잡도**: O(1) - 제자리 정렬

```java
public class HeapSort {
    public static void heapSort(int[] arr) {
        int n = arr.length;

        // 1단계: 최대 힙 구성 (Heapify)
        // 마지막 부모 노드부터 루트까지 heapify
        for (int i = n / 2 - 1; i >= 0; i--) {
            heapify(arr, n, i);
        }

        // 2단계: 하나씩 원소를 힙에서 꺼내어 정렬
        for (int i = n - 1; i > 0; i--) {
            // 루트(최댓값)를 끝으로 이동
            int temp = arr[0];
            arr[0] = arr[i];
            arr[i] = temp;

            // 축소된 힙에 대해 heapify
            heapify(arr, i, 0);
        }
    }

    // 서브트리를 힙으로 만드는 함수
    private static void heapify(int[] arr, int n, int i) {
        int largest = i;        // 루트를 최댓값으로 가정
        int left = 2 * i + 1;   // 왼쪽 자식
        int right = 2 * i + 2;  // 오른쪽 자식

        // 왼쪽 자식이 루트보다 크면
        if (left < n && arr[left] > arr[largest]) {
            largest = left;
        }

        // 오른쪽 자식이 현재 최댓값보다 크면
        if (right < n && arr[right] > arr[largest]) {
            largest = right;
        }

        // 최댓값이 루트가 아니면
        if (largest != i) {
            int swap = arr[i];
            arr[i] = arr[largest];
            arr[largest] = swap;

            // 재귀적으로 영향받은 서브트리를 heapify
            heapify(arr, n, largest);
        }
    }

    public static void main(String[] args) {
        int[] arr = {12, 11, 13, 5, 6, 7};
        heapSort(arr);
        System.out.println(Arrays.toString(arr));
        // 출력: [5, 6, 7, 11, 12, 13]
    }
}
```

---

## 최단 경로 알고리즘

그래프에서 두 정점 간의 최단 경로를 찾는 알고리즘들입니다.

### 1. 다익스트라 알고리즘 (Dijkstra's Algorithm)

**개념**: 하나의 시작 정점에서 다른 모든 정점까지의 최단 경로를 찾습니다. **양의 가중치**만 허용합니다.

**시간 복잡도**:
- 인접 행렬: O(V²)
- 우선순위 큐 사용: O((V + E) log V)
**공간 복잡도**: O(V)

```java
import java.util.*;

class Edge {
    int target, weight;

    Edge(int target, int weight) {
        this.target = target;
        this.weight = weight;
    }
}

class Node implements Comparable<Node> {
    int vertex, distance;

    Node(int vertex, int distance) {
        this.vertex = vertex;
        this.distance = distance;
    }

    @Override
    public int compareTo(Node other) {
        return Integer.compare(this.distance, other.distance);
    }
}

public class Dijkstra {
    private int V;
    private List<List<Edge>> adj;

    public Dijkstra(int v) {
        V = v;
        adj = new ArrayList<>();
        for (int i = 0; i < v; i++) {
            adj.add(new ArrayList<>());
        }
    }

    public void addEdge(int source, int target, int weight) {
        adj.get(source).add(new Edge(target, weight));
    }

    public int[] dijkstra(int start) {
        int[] dist = new int[V];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[start] = 0;

        PriorityQueue<Node> pq = new PriorityQueue<>();
        pq.offer(new Node(start, 0));

        boolean[] visited = new boolean[V];

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            int u = current.vertex;

            if (visited[u]) continue;
            visited[u] = true;

            // 인접한 모든 정점 확인
            for (Edge edge : adj.get(u)) {
                int v = edge.target;
                int weight = edge.weight;

                // 더 짧은 경로를 발견하면 업데이트
                if (dist[u] + weight < dist[v]) {
                    dist[v] = dist[u] + weight;
                    pq.offer(new Node(v, dist[v]));
                }
            }
        }

        return dist;
    }

    public static void main(String[] args) {
        Dijkstra graph = new Dijkstra(5);

        graph.addEdge(0, 1, 10);
        graph.addEdge(0, 4, 5);
        graph.addEdge(1, 2, 1);
        graph.addEdge(1, 4, 2);
        graph.addEdge(2, 3, 4);
        graph.addEdge(3, 2, 6);
        graph.addEdge(3, 0, 7);
        graph.addEdge(4, 1, 3);
        graph.addEdge(4, 2, 9);
        graph.addEdge(4, 3, 2);

        int[] distances = graph.dijkstra(0);

        System.out.println("정점 0에서 각 정점까지의 최단 거리:");
        for (int i = 0; i < distances.length; i++) {
            System.out.println("0 -> " + i + ": " + distances[i]);
        }
    }
}
```

### 2. 벨만-포드 알고리즘 (Bellman-Ford Algorithm)

**개념**: 다익스트라와 달리 **음의 가중치**도 허용하며, **음의 사이클** 검출이 가능합니다.

**시간 복잡도**: O(VE)
**공간 복잡도**: O(V)

```java
import java.util.*;

class WeightedEdge {
    int source, target, weight;

    WeightedEdge(int source, int target, int weight) {
        this.source = source;
        this.target = target;
        this.weight = weight;
    }
}

public class BellmanFord {
    private int V, E;
    private List<WeightedEdge> edges;

    public BellmanFord(int v, int e) {
        V = v;
        E = e;
        edges = new ArrayList<>();
    }

    public void addEdge(int source, int target, int weight) {
        edges.add(new WeightedEdge(source, target, weight));
    }

    public int[] bellmanFord(int start) {
        int[] dist = new int[V];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[start] = 0;

        // V-1번 반복하여 최단 거리 갱신
        for (int i = 0; i < V - 1; i++) {
            for (WeightedEdge edge : edges) {
                int u = edge.source;
                int v = edge.target;
                int weight = edge.weight;

                if (dist[u] != Integer.MAX_VALUE && dist[u] + weight < dist[v]) {
                    dist[v] = dist[u] + weight;
                }
            }
        }

        // 음의 사이클 검출
        for (WeightedEdge edge : edges) {
            int u = edge.source;
            int v = edge.target;
            int weight = edge.weight;

            if (dist[u] != Integer.MAX_VALUE && dist[u] + weight < dist[v]) {
                System.out.println("음의 사이클이 존재합니다!");
                return null;
            }
        }

        return dist;
    }

    public static void main(String[] args) {
        BellmanFord graph = new BellmanFord(5, 8);

        graph.addEdge(0, 1, -1);
        graph.addEdge(0, 2, 4);
        graph.addEdge(1, 2, 3);
        graph.addEdge(1, 3, 2);
        graph.addEdge(1, 4, 2);
        graph.addEdge(3, 2, 5);
        graph.addEdge(3, 1, 1);
        graph.addEdge(4, 3, -3);

        int[] distances = graph.bellmanFord(0);

        if (distances != null) {
            System.out.println("정점 0에서 각 정점까지의 최단 거리:");
            for (int i = 0; i < distances.length; i++) {
                System.out.println("0 -> " + i + ": " +
                    (distances[i] == Integer.MAX_VALUE ? "∞" : distances[i]));
            }
        }
    }
}
```

### 3. 플로이드-워셜 알고리즘 (Floyd-Warshall Algorithm)

**개념**: **모든 정점 쌍** 간의 최단 거리를 구합니다. 동적 프로그래밍을 사용합니다.

**시간 복잡도**: O(V³)
**공간 복잡도**: O(V²)

```java
public class FloydWarshall {
    final static int INF = 99999;

    public static void floydWarshall(int[][] graph, int V) {
        int[][] dist = new int[V][V];

        // 거리 배열 초기화
        for (int i = 0; i < V; i++) {
            for (int j = 0; j < V; j++) {
                dist[i][j] = graph[i][j];
            }
        }

        // k: 경유 정점
        for (int k = 0; k < V; k++) {
            // i: 시작 정점
            for (int i = 0; i < V; i++) {
                // j: 도착 정점
                for (int j = 0; j < V; j++) {
                    // i -> j 직접 vs i -> k -> j 비교
                    if (dist[i][k] + dist[k][j] < dist[i][j]) {
                        dist[i][j] = dist[i][k] + dist[k][j];
                    }
                }
            }
        }

        // 결과 출력
        printSolution(dist, V);
    }

    private static void printSolution(int[][] dist, int V) {
        System.out.println("모든 정점 쌍의 최단 거리:");
        for (int i = 0; i < V; i++) {
            for (int j = 0; j < V; j++) {
                if (dist[i][j] == INF) {
                    System.out.print("INF ");
                } else {
                    System.out.printf("%3d ", dist[i][j]);
                }
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        int V = 4;
        int[][] graph = {
            {0, 5, INF, 10},
            {INF, 0, 3, INF},
            {INF, INF, 0, 1},
            {INF, INF, INF, 0}
        };

        floydWarshall(graph, V);
    }
}
```

---

## 고급 동적 프로그래밍

### 1. 최장 공통 부분 수열 (LCS - Longest Common Subsequence)

**개념**: 두 문자열에서 공통으로 나타나는 가장 긴 부분 수열을 찾습니다. (연속적일 필요 없음)

**시간 복잡도**: O(m × n)
**공간 복잡도**: O(m × n)

```java
public class LCS {
    public static int lcs(String text1, String text2) {
        int m = text1.length();
        int n = text2.length();

        int[][] dp = new int[m + 1][n + 1];

        // DP 테이블 채우기
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    // 문자가 같으면 이전 대각선 값 + 1
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    // 문자가 다르면 위 또는 왼쪽 중 큰 값
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        return dp[m][n];
    }

    // LCS 문자열 복원
    public static String getLCSString(String text1, String text2) {
        int m = text1.length();
        int n = text2.length();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        // 역추적으로 LCS 문자열 구성
        StringBuilder lcs = new StringBuilder();
        int i = m, j = n;

        while (i > 0 && j > 0) {
            if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                lcs.append(text1.charAt(i - 1));
                i--;
                j--;
            } else if (dp[i - 1][j] > dp[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }

        return lcs.reverse().toString();
    }

    public static void main(String[] args) {
        String text1 = "ABCDGH";
        String text2 = "AEDFHR";

        System.out.println("LCS 길이: " + lcs(text1, text2)); // 3
        System.out.println("LCS: " + getLCSString(text1, text2)); // ADH
    }
}
```

### 2. 배낭 문제 (Knapsack Problem)

**개념**: 무게 제한이 있는 배낭에 최대 가치를 갖도록 물건을 넣는 문제입니다.

**0/1 배낭 문제**: 각 물건을 넣거나 넣지 않거나 (분할 불가)

**시간 복잡도**: O(n × W) - n은 물건 수, W는 배낭 용량
**공간 복잡도**: O(n × W)

```java
public class Knapsack {
    public static int knapsack(int[] weights, int[] values, int capacity) {
        int n = weights.length;
        int[][] dp = new int[n + 1][capacity + 1];

        // dp[i][w]: i번째 물건까지 고려하고 용량이 w일 때 최대 가치
        for (int i = 1; i <= n; i++) {
            for (int w = 1; w <= capacity; w++) {
                if (weights[i - 1] <= w) {
                    // 물건을 넣을 수 있는 경우
                    // max(넣지 않음, 넣음)
                    dp[i][w] = Math.max(
                        dp[i - 1][w],  // 넣지 않음
                        dp[i - 1][w - weights[i - 1]] + values[i - 1]  // 넣음
                    );
                } else {
                    // 물건을 넣을 수 없는 경우
                    dp[i][w] = dp[i - 1][w];
                }
            }
        }

        return dp[n][capacity];
    }

    // 공간 최적화 버전 (1차원 배열)
    public static int knapsackOptimized(int[] weights, int[] values, int capacity) {
        int n = weights.length;
        int[] dp = new int[capacity + 1];

        for (int i = 0; i < n; i++) {
            // 뒤에서부터 업데이트 (중복 사용 방지)
            for (int w = capacity; w >= weights[i]; w--) {
                dp[w] = Math.max(dp[w], dp[w - weights[i]] + values[i]);
            }
        }

        return dp[capacity];
    }

    public static void main(String[] args) {
        int[] weights = {1, 3, 4, 5};
        int[] values = {1, 4, 5, 7};
        int capacity = 7;

        System.out.println("최대 가치: " + knapsack(weights, values, capacity));
        // 출력: 9 (물건 1, 2 선택)

        System.out.println("최대 가치 (최적화): " + knapsackOptimized(weights, values, capacity));
    }
}
```

### 3. 편집 거리 (Edit Distance)

**개념**: 두 문자열을 같게 만들기 위한 최소 편집 횟수를 구합니다. (삽입, 삭제, 교체)

**시간 복잡도**: O(m × n)
**공간 복잡도**: O(m × n)

```java
public class EditDistance {
    public static int minDistance(String word1, String word2) {
        int m = word1.length();
        int n = word2.length();

        int[][] dp = new int[m + 1][n + 1];

        // 초기화: 빈 문자열로 변환
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i; // word1[0..i]를 빈 문자열로
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j; // 빈 문자열을 word2[0..j]로
        }

        // DP 테이블 채우기
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    // 문자가 같으면 편집 불필요
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    // 문자가 다르면 3가지 연산 중 최소 선택
                    dp[i][j] = 1 + Math.min(
                        dp[i - 1][j],      // 삭제
                        Math.min(
                            dp[i][j - 1],  // 삽입
                            dp[i - 1][j - 1] // 교체
                        )
                    );
                }
            }
        }

        return dp[m][n];
    }

    public static void main(String[] args) {
        String word1 = "horse";
        String word2 = "ros";

        System.out.println("편집 거리: " + minDistance(word1, word2));
        // 출력: 3 (horse -> rorse -> rose -> ros)
    }
}
```

---

## 고급 조합 최적화

### 비트마스킹을 이용한 부분집합 생성

**개념**: 비트 연산을 사용하여 모든 부분집합을 효율적으로 생성합니다.

**시간 복잡도**: O(2ⁿ × n)
**공간 복잡도**: O(1)

```java
import java.util.*;

public class SubsetBitmask {
    public static List<List<Integer>> generateSubsets(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        int n = nums.length;

        // 2^n개의 부분집합
        for (int mask = 0; mask < (1 << n); mask++) {
            List<Integer> subset = new ArrayList<>();

            // 각 비트 확인
            for (int i = 0; i < n; i++) {
                // i번째 비트가 1이면 nums[i] 포함
                if ((mask & (1 << i)) != 0) {
                    subset.add(nums[i]);
                }
            }

            result.add(subset);
        }

        return result;
    }

    public static void main(String[] args) {
        int[] nums = {1, 2, 3};
        List<List<Integer>> subsets = generateSubsets(nums);

        System.out.println("모든 부분집합:");
        for (List<Integer> subset : subsets) {
            System.out.println(subset);
        }
        // 출력: [], [1], [2], [1,2], [3], [1,3], [2,3], [1,2,3]
    }
}
```

### 백트래킹 최적화 - N-Queens 문제

**개념**: N×N 체스판에 N개의 퀸을 서로 공격할 수 없게 배치하는 문제입니다.

**시간 복잡도**: O(N!)
**공간 복잡도**: O(N)

```java
import java.util.*;

public class NQueens {
    private List<List<String>> solutions;

    public List<List<String>> solveNQueens(int n) {
        solutions = new ArrayList<>();
        char[][] board = new char[n][n];

        // 체스판 초기화
        for (int i = 0; i < n; i++) {
            Arrays.fill(board[i], '.');
        }

        backtrack(board, 0, n);
        return solutions;
    }

    private void backtrack(char[][] board, int row, int n) {
        if (row == n) {
            // 모든 퀸을 배치했으면 해답 추가
            solutions.add(construct(board));
            return;
        }

        // 현재 행의 각 열에 퀸 배치 시도
        for (int col = 0; col < n; col++) {
            if (isValid(board, row, col, n)) {
                board[row][col] = 'Q';
                backtrack(board, row + 1, n);
                board[row][col] = '.'; // 백트래킹
            }
        }
    }

    private boolean isValid(char[][] board, int row, int col, int n) {
        // 같은 열 확인
        for (int i = 0; i < row; i++) {
            if (board[i][col] == 'Q') return false;
        }

        // 왼쪽 대각선 확인
        for (int i = row - 1, j = col - 1; i >= 0 && j >= 0; i--, j--) {
            if (board[i][j] == 'Q') return false;
        }

        // 오른쪽 대각선 확인
        for (int i = row - 1, j = col + 1; i >= 0 && j < n; i--, j++) {
            if (board[i][j] == 'Q') return false;
        }

        return true;
    }

    private List<String> construct(char[][] board) {
        List<String> result = new ArrayList<>();
        for (char[] row : board) {
            result.add(new String(row));
        }
        return result;
    }

    public static void main(String[] args) {
        NQueens nq = new NQueens();
        List<List<String>> solutions = nq.solveNQueens(4);

        System.out.println("4-Queens 해답 개수: " + solutions.size());
        for (List<String> solution : solutions) {
            for (String row : solution) {
                System.out.println(row);
            }
            System.out.println();
        }
    }
}
```

---

## 정리

### 최단 경로 알고리즘 비교

| 알고리즘 | 시간 복잡도 | 음의 가중치 | 특징 |
|---------|-----------|-----------|-----|
| 다익스트라 | O((V+E) log V) | X | 단일 시작점, 빠름 |
| 벨만-포드 | O(VE) | O | 음의 사이클 검출 |
| 플로이드-워셜 | O(V³) | O | 모든 쌍 최단 경로 |

### 고급 DP 문제 유형

| 문제 | 핵심 아이디어 | 시간 복잡도 |
|-----|------------|-----------|
| LCS | 문자 일치/불일치 | O(m × n) |
| 배낭 문제 | 넣음/안 넣음 | O(n × W) |
| 편집 거리 | 삽입/삭제/교체 | O(m × n) |

### 학습 팁
1. **힙 정렬**: 힙 자료구조의 원리를 먼저 이해
2. **최단 경로**: 그래프 종류에 따라 알고리즘 선택
3. **고급 DP**: 2차원 테이블 점화식 도출 연습
4. **백트래킹**: 가지치기(Pruning)로 성능 향상
5. **비트마스킹**: 집합 연산을 비트로 표현하여 최적화

### 면접 대비 핵심
- **정렬**: O(n log n) 보장 → 합병/힙 정렬
- **최단 경로**: 양의 가중치 → 다익스트라, 음의 가중치 → 벨만-포드
- **DP**: 부분 문제의 중복 → 메모이제이션
- **조합**: 백트래킹 + 가지치기

---

*마지막 업데이트: 2025년 12월*

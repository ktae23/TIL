# 다익스트라 알고리즘 (Dijkstra)

> 관련 코드: `section6_graph/shortest/DijkstraBasic.java`, `DijkstraProblems.java`

---

## 1. 다익스트라란?

**하나의 시작점**에서 다른 모든 정점까지의 **최단 경로**를 구하는 알고리즘입니다.

### 특징
- **가중치가 있는 그래프**에서 최단 경로
- **음수 가중치가 없어야** 함
- 그리디 알고리즘의 일종

---

## 2. 동작 원리

1. 시작점의 거리를 0으로, 나머지는 **무한대**로 초기화
2. 방문하지 않은 정점 중 **거리가 가장 짧은 정점** 선택
3. 선택한 정점을 통해 이웃 정점까지의 거리 **갱신**
4. 모든 정점을 방문할 때까지 반복

---

## 3. 시간 복잡도

| 구현 방식 | 시간 복잡도 |
|-----------|-------------|
| 배열 사용 | O(V²) |
| **우선순위 큐** | O((V + E) log V) |

---

## 4. 비교

| 알고리즘 | 용도 |
|----------|------|
| BFS | 가중치 **없는** 그래프의 최단 경로 |
| **다익스트라** | **양수 가중치** 그래프의 최단 경로 |
| 벨만-포드 | **음수 가중치** 허용, 음수 사이클 감지 |
| 플로이드-워셜 | **모든 쌍** 최단 경로 |

---

## 5. 구현

### 우선순위 큐 사용 (권장)
```java
public static int[] dijkstra(int start, List<List<int[]>> adj) {
    int n = adj.size();
    int[] dist = new int[n];
    Arrays.fill(dist, Integer.MAX_VALUE);

    // 우선순위 큐: {거리, 정점}
    PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);

    dist[start] = 0;
    pq.offer(new int[]{0, start});

    while (!pq.isEmpty()) {
        int[] curr = pq.poll();
        int d = curr[0];
        int node = curr[1];

        // 이미 처리된 정점이면 스킵
        if (d > dist[node]) continue;

        // 인접 정점들의 거리 갱신
        for (int[] edge : adj.get(node)) {
            int next = edge[0];
            int weight = edge[1];

            if (dist[node] + weight < dist[next]) {
                dist[next] = dist[node] + weight;
                pq.offer(new int[]{dist[next], next});
            }
        }
    }

    return dist;
}
```

### 최단 경로 역추적
```java
public static List<Integer> dijkstraPath(int start, int end, List<List<int[]>> adj) {
    int n = adj.size();
    int[] dist = new int[n];
    int[] parent = new int[n];
    Arrays.fill(dist, Integer.MAX_VALUE);
    Arrays.fill(parent, -1);

    PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);
    dist[start] = 0;
    pq.offer(new int[]{0, start});

    while (!pq.isEmpty()) {
        int[] curr = pq.poll();
        int d = curr[0];
        int node = curr[1];

        if (d > dist[node]) continue;

        for (int[] edge : adj.get(node)) {
            int next = edge[0];
            int weight = edge[1];

            if (dist[node] + weight < dist[next]) {
                dist[next] = dist[node] + weight;
                parent[next] = node;
                pq.offer(new int[]{dist[next], next});
            }
        }
    }

    // 경로 역추적
    List<Integer> path = new ArrayList<>();
    if (dist[end] == Integer.MAX_VALUE) return path;

    for (int node = end; node != -1; node = parent[node]) {
        path.add(node);
    }
    Collections.reverse(path);
    return path;
}
```

---

## 6. 응용 문제

### 경유지를 거치는 최단 경로
```java
// 1 → v1 → v2 → N 또는 1 → v2 → v1 → N
public static long pathWithTwoPoints(int n, int v1, int v2, List<List<int[]>> adj) {
    int[] distFrom1 = dijkstra(1, adj);
    int[] distFromV1 = dijkstra(v1, adj);
    int[] distFromV2 = dijkstra(v2, adj);

    long path1 = (long) distFrom1[v1] + distFromV1[v2] + distFromV2[n];
    long path2 = (long) distFrom1[v2] + distFromV2[v1] + distFromV1[n];

    long result = Math.min(path1, path2);
    return result >= Integer.MAX_VALUE ? -1 : result;
}
```

### 그리드에서 다익스트라
```java
public static int minCostGrid(int[][] grid) {
    int n = grid.length, m = grid[0].length;
    int[][] dist = new int[n][m];
    for (int[] row : dist) Arrays.fill(row, Integer.MAX_VALUE);

    int[] dr = {-1, 1, 0, 0};
    int[] dc = {0, 0, -1, 1};

    PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);
    dist[0][0] = grid[0][0];
    pq.offer(new int[]{grid[0][0], 0, 0});

    while (!pq.isEmpty()) {
        int[] curr = pq.poll();
        int d = curr[0], r = curr[1], c = curr[2];

        if (d > dist[r][c]) continue;

        for (int i = 0; i < 4; i++) {
            int nr = r + dr[i], nc = c + dc[i];

            if (nr >= 0 && nr < n && nc >= 0 && nc < m) {
                int newDist = dist[r][c] + grid[nr][nc];
                if (newDist < dist[nr][nc]) {
                    dist[nr][nc] = newDist;
                    pq.offer(new int[]{newDist, nr, nc});
                }
            }
        }
    }
    return dist[n - 1][m - 1];
}
```

---

## 핵심 정리

1. **양수 가중치**에서만 사용 가능
2. **우선순위 큐**로 O((V+E) log V)
3. `if (d > dist[node]) continue;`로 **중복 처리 방지**
4. 음수 가중치 → **벨만-포드** 사용

# 16. 너비 우선 탐색 (BFS)

> 관련 코드: `section6_graph/traversal/BFSBasic.java`

---

## 1. BFS란?

그래프를 탐색하는 방법 중 하나로, 시작점에서 **가까운 정점부터** 차례대로 방문하는 방식입니다.

### 특징
- **큐(Queue)**로 구현
- **최단 경로** 탐색에 유용 (가중치 없는 그래프)
- 레벨 순회

---

## 2. 시간/공간 복잡도

| 표현 방식 | 시간 복잡도 |
|-----------|-------------|
| 인접 리스트 | O(V + E) |
| 인접 행렬 | O(V²) |

공간: O(V) - visited 배열 + 큐

---

## 3. BFS 활용

- **최단 경로** (가중치 없는 그래프)
- 레벨 순회 (트리)
- 연결 요소 찾기
- 이분 그래프 판별
- **미로 탐색** (최단 거리)

---

## 4. DFS vs BFS

| 구분 | DFS | BFS |
|------|-----|-----|
| 탐색 방식 | 깊이 우선 | 너비 우선 |
| 구현 | 스택/재귀 | 큐 |
| 용도 | 경로 탐색, 백트래킹 | **최단 경로**, 레벨 순회 |
| 메모리 | 효율적 (희소 그래프) | 더 많이 사용 |

---

## 5. 구현

### 기본 BFS
```java
public static void bfs(int start, List<List<Integer>> adj) {
    boolean[] visited = new boolean[adj.size()];
    Queue<Integer> queue = new LinkedList<>();

    visited[start] = true;
    queue.offer(start);

    while (!queue.isEmpty()) {
        int node = queue.poll();
        System.out.print(node + " ");

        for (int next : adj.get(node)) {
            if (!visited[next]) {
                visited[next] = true;
                queue.offer(next);
            }
        }
    }
}
```

### 최단 거리 계산
```java
public static int[] shortestDistance(int start, List<List<Integer>> adj) {
    int n = adj.size();
    int[] dist = new int[n];
    Arrays.fill(dist, -1);  // -1: 미방문

    Queue<Integer> queue = new LinkedList<>();
    dist[start] = 0;
    queue.offer(start);

    while (!queue.isEmpty()) {
        int node = queue.poll();

        for (int next : adj.get(node)) {
            if (dist[next] == -1) {
                dist[next] = dist[node] + 1;
                queue.offer(next);
            }
        }
    }
    return dist;
}
```

### 최단 경로 역추적
```java
public static List<Integer> shortestPath(int start, int end, List<List<Integer>> adj) {
    int n = adj.size();
    int[] dist = new int[n];
    int[] parent = new int[n];
    Arrays.fill(dist, -1);
    Arrays.fill(parent, -1);

    Queue<Integer> queue = new LinkedList<>();
    dist[start] = 0;
    queue.offer(start);

    while (!queue.isEmpty()) {
        int node = queue.poll();
        if (node == end) break;

        for (int next : adj.get(node)) {
            if (dist[next] == -1) {
                dist[next] = dist[node] + 1;
                parent[next] = node;
                queue.offer(next);
            }
        }
    }

    // 경로 역추적
    List<Integer> path = new ArrayList<>();
    if (dist[end] == -1) return path;

    for (int node = end; node != -1; node = parent[node]) {
        path.add(node);
    }
    Collections.reverse(path);
    return path;
}
```

### 레벨 순회
```java
public static void bfsLevel(int start, List<List<Integer>> adj) {
    boolean[] visited = new boolean[adj.size()];
    Queue<Integer> queue = new LinkedList<>();

    visited[start] = true;
    queue.offer(start);
    int level = 0;

    while (!queue.isEmpty()) {
        int size = queue.size();  // 현재 레벨의 노드 수
        System.out.print("Level " + level + ": ");

        for (int i = 0; i < size; i++) {
            int node = queue.poll();
            System.out.print(node + " ");

            for (int next : adj.get(node)) {
                if (!visited[next]) {
                    visited[next] = true;
                    queue.offer(next);
                }
            }
        }
        System.out.println();
        level++;
    }
}
```

### 2D 그리드 BFS (미로 탐색)
```java
public static int shortestPathGrid(int[][] grid, int[] start, int[] end) {
    int rows = grid.length, cols = grid[0].length;
    int[] dr = {-1, 1, 0, 0};
    int[] dc = {0, 0, -1, 1};

    boolean[][] visited = new boolean[rows][cols];
    Queue<int[]> queue = new LinkedList<>();

    visited[start[0]][start[1]] = true;
    queue.offer(new int[]{start[0], start[1], 0});  // {row, col, distance}

    while (!queue.isEmpty()) {
        int[] curr = queue.poll();
        int r = curr[0], c = curr[1], dist = curr[2];

        if (r == end[0] && c == end[1]) return dist;

        for (int d = 0; d < 4; d++) {
            int nr = r + dr[d], nc = c + dc[d];

            if (nr >= 0 && nr < rows && nc >= 0 && nc < cols
                    && !visited[nr][nc] && grid[nr][nc] == 0) {
                visited[nr][nc] = true;
                queue.offer(new int[]{nr, nc, dist + 1});
            }
        }
    }
    return -1;
}
```

---

## 핵심 정리

1. **최단 경로** 문제 → BFS
2. **큐**로 구현, 방문 표시는 **큐에 넣을 때**
3. 레벨 구분: `queue.size()`로 현재 레벨 노드 수 확인
4. 그리드: 4방향 `{-1,0}, {1,0}, {0,-1}, {0,1}`

package section6_graph.traversal;

import java.util.*;

/**
 * 너비 우선 탐색 (BFS - Breadth First Search)
 *
 * ========================================
 * 1. BFS란?
 * ========================================
 *
 * 그래프를 탐색하는 방법 중 하나로,
 * 시작점에서 가까운 정점부터 차례대로 방문하는 방식입니다.
 *
 * 특징:
 * - 큐(Queue)로 구현
 * - 최단 경로 탐색에 유용 (가중치 없는 그래프)
 * - 레벨 순회
 *
 * ========================================
 * 2. 시간/공간 복잡도
 * ========================================
 *
 * 인접 리스트: O(V + E)
 * 인접 행렬: O(V²)
 * 공간: O(V) - visited 배열 + 큐
 *
 * ========================================
 * 3. BFS 활용
 * ========================================
 *
 * - 최단 경로 (가중치 없는 그래프)
 * - 레벨 순회 (트리)
 * - 연결 요소 찾기
 * - 이분 그래프 판별
 * - 미로 탐색 (최단 거리)
 *
 * ========================================
 * 4. DFS vs BFS
 * ========================================
 *
 * DFS:
 * - 깊이 우선, 스택/재귀
 * - 경로 탐색, 백트래킹
 * - 메모리 효율적 (희소 그래프)
 *
 * BFS:
 * - 너비 우선, 큐
 * - 최단 경로, 레벨 순회
 * - 더 많은 메모리 사용
 */
public class BFSBasic {

    /**
     * 기본 BFS
     */
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

    /**
     * 최단 거리 계산 (가중치 없는 그래프)
     */
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

    /**
     * 최단 경로 역추적
     */
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
        if (dist[end] == -1) return path;  // 경로 없음

        for (int node = end; node != -1; node = parent[node]) {
            path.add(node);
        }
        Collections.reverse(path);
        return path;
    }

    /**
     * 레벨 순회 (각 레벨의 노드 구분)
     */
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

    /**
     * 이분 그래프 판별
     *
     * 인접한 노드끼리 다른 색으로 칠할 수 있는지
     */
    public static boolean isBipartite(List<List<Integer>> adj) {
        int n = adj.size();
        int[] color = new int[n];
        Arrays.fill(color, -1);  // -1: 미방문

        for (int i = 0; i < n; i++) {
            if (color[i] == -1) {
                if (!bfsBipartite(i, adj, color)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean bfsBipartite(int start, List<List<Integer>> adj, int[] color) {
        Queue<Integer> queue = new LinkedList<>();
        color[start] = 0;
        queue.offer(start);

        while (!queue.isEmpty()) {
            int node = queue.poll();

            for (int next : adj.get(node)) {
                if (color[next] == -1) {
                    color[next] = 1 - color[node];  // 다른 색
                    queue.offer(next);
                } else if (color[next] == color[node]) {
                    return false;  // 같은 색이면 이분 그래프 아님
                }
            }
        }

        return true;
    }

    /**
     * 2D 그리드에서 BFS (미로 탐색)
     */
    public static int shortestPathGrid(int[][] grid, int[] start, int[] end) {
        int rows = grid.length;
        int cols = grid[0].length;

        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};

        boolean[][] visited = new boolean[rows][cols];
        Queue<int[]> queue = new LinkedList<>();

        visited[start[0]][start[1]] = true;
        queue.offer(new int[]{start[0], start[1], 0});  // {row, col, distance}

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int r = curr[0], c = curr[1], dist = curr[2];

            if (r == end[0] && c == end[1]) {
                return dist;
            }

            for (int d = 0; d < 4; d++) {
                int nr = r + dr[d];
                int nc = c + dc[d];

                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols
                        && !visited[nr][nc] && grid[nr][nc] == 0) {
                    visited[nr][nc] = true;
                    queue.offer(new int[]{nr, nc, dist + 1});
                }
            }
        }

        return -1;  // 경로 없음
    }

    public static void main(String[] args) {
        System.out.println("=== BFS 기본 예제 ===\n");

        /*
         * 그래프:
         *       0
         *      /|\
         *     1 2 3
         *    /|   |
         *   4 5   6
         */

        int V = 7;
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < V; i++) {
            adj.add(new ArrayList<>());
        }

        addEdge(adj, 0, 1);
        addEdge(adj, 0, 2);
        addEdge(adj, 0, 3);
        addEdge(adj, 1, 4);
        addEdge(adj, 1, 5);
        addEdge(adj, 3, 6);

        // 1. 기본 BFS
        System.out.println("1) 기본 BFS (시작: 0):");
        bfs(0, adj);
        System.out.println("\n");

        // 2. 레벨 순회
        System.out.println("2) 레벨 순회:");
        bfsLevel(0, adj);

        // 3. 최단 거리
        System.out.println("\n3) 각 노드까지의 최단 거리:");
        int[] dist = shortestDistance(0, adj);
        for (int i = 0; i < V; i++) {
            System.out.println("0 → " + i + ": " + dist[i]);
        }

        // 4. 최단 경로
        System.out.println("\n4) 0 → 6 최단 경로:");
        System.out.println(shortestPath(0, 6, adj));

        // 5. 이분 그래프
        System.out.println("\n5) 이분 그래프 여부: " + isBipartite(adj));

        // 6. 2D 그리드 BFS
        System.out.println("\n6) 미로 최단 경로:");
        int[][] grid = {
                {0, 0, 0, 0},
                {1, 1, 0, 1},
                {0, 0, 0, 0},
                {0, 1, 1, 0}
        };
        System.out.println("최단 거리: " + shortestPathGrid(grid, new int[]{0, 0}, new int[]{3, 3}));
    }

    private static void addEdge(List<List<Integer>> adj, int u, int v) {
        adj.get(u).add(v);
        adj.get(v).add(u);
    }
}

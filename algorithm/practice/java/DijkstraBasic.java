package section6_graph.shortest;

import java.util.*;

/**
 * 다익스트라 알고리즘 (Dijkstra's Algorithm)
 *
 * ========================================
 * 1. 다익스트라란?
 * ========================================
 *
 * 하나의 시작점에서 다른 모든 정점까지의 최단 경로를 구하는 알고리즘입니다.
 *
 * 특징:
 * - 가중치가 있는 그래프에서 최단 경로
 * - 음수 가중치가 없어야 함
 * - 그리디 알고리즘의 일종
 *
 * ========================================
 * 2. 동작 원리
 * ========================================
 *
 * 1) 시작점의 거리를 0으로, 나머지는 무한대로 초기화
 * 2) 방문하지 않은 정점 중 거리가 가장 짧은 정점 선택
 * 3) 선택한 정점을 통해 이웃 정점까지의 거리 갱신
 * 4) 모든 정점을 방문할 때까지 반복
 *
 * ========================================
 * 3. 시간 복잡도
 * ========================================
 *
 * - 배열 사용: O(V²)
 * - 우선순위 큐 사용: O((V + E) log V)
 *
 * ========================================
 * 4. 비교
 * ========================================
 *
 * BFS: 가중치 없는 그래프의 최단 경로
 * 다익스트라: 양수 가중치 그래프의 최단 경로
 * 벨만-포드: 음수 가중치 허용, 음수 사이클 감지
 * 플로이드-워셜: 모든 쌍 최단 경로
 */
public class DijkstraBasic {

    static final int INF = Integer.MAX_VALUE;

    /**
     * 다익스트라 - 우선순위 큐 사용 (권장)
     *
     * O((V + E) log V)
     */
    public static int[] dijkstra(int start, List<List<int[]>> adj) {
        int n = adj.size();
        int[] dist = new int[n];
        Arrays.fill(dist, INF);

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

    /**
     * 다익스트라 - 배열 사용 (단순 구현)
     *
     * O(V²)
     */
    public static int[] dijkstraArray(int start, int[][] graph) {
        int n = graph.length;
        int[] dist = new int[n];
        boolean[] visited = new boolean[n];

        Arrays.fill(dist, INF);
        dist[start] = 0;

        for (int i = 0; i < n; i++) {
            // 방문하지 않은 정점 중 거리가 가장 짧은 정점 선택
            int u = -1;
            int minDist = INF;

            for (int v = 0; v < n; v++) {
                if (!visited[v] && dist[v] < minDist) {
                    minDist = dist[v];
                    u = v;
                }
            }

            if (u == -1) break;

            visited[u] = true;

            // 인접 정점들의 거리 갱신
            for (int v = 0; v < n; v++) {
                if (!visited[v] && graph[u][v] != 0) {
                    if (dist[u] + graph[u][v] < dist[v]) {
                        dist[v] = dist[u] + graph[u][v];
                    }
                }
            }
        }

        return dist;
    }

    /**
     * 최단 경로 역추적
     */
    public static List<Integer> dijkstraPath(int start, int end, List<List<int[]>> adj) {
        int n = adj.size();
        int[] dist = new int[n];
        int[] parent = new int[n];
        Arrays.fill(dist, INF);
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
        if (dist[end] == INF) return path;  // 경로 없음

        for (int node = end; node != -1; node = parent[node]) {
            path.add(node);
        }
        Collections.reverse(path);
        return path;
    }

    /**
     * 특정 목적지까지의 최단 거리만 필요한 경우
     */
    public static int dijkstraSingle(int start, int end, List<List<int[]>> adj) {
        int n = adj.size();
        int[] dist = new int[n];
        Arrays.fill(dist, INF);

        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);

        dist[start] = 0;
        pq.offer(new int[]{0, start});

        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int d = curr[0];
            int node = curr[1];

            // 목적지에 도달하면 바로 반환
            if (node == end) return d;

            if (d > dist[node]) continue;

            for (int[] edge : adj.get(node)) {
                int next = edge[0];
                int weight = edge[1];

                if (dist[node] + weight < dist[next]) {
                    dist[next] = dist[node] + weight;
                    pq.offer(new int[]{dist[next], next});
                }
            }
        }

        return dist[end];
    }

    public static void main(String[] args) {
        System.out.println("=== 다익스트라 알고리즘 ===\n");

        /*
         * 가중치 그래프:
         *     0 --1-- 1
         *     |      /|
         *     4    3  2
         *     |  /    |
         *     2 --1-- 3
         */

        int V = 4;
        List<List<int[]>> adj = new ArrayList<>();
        for (int i = 0; i < V; i++) {
            adj.add(new ArrayList<>());
        }

        // 무방향 가중치 간선
        addEdge(adj, 0, 1, 1);
        addEdge(adj, 0, 2, 4);
        addEdge(adj, 1, 2, 3);
        addEdge(adj, 1, 3, 2);
        addEdge(adj, 2, 3, 1);

        // 1. 다익스트라 (우선순위 큐)
        System.out.println("1) 정점 0에서 각 정점까지의 최단 거리:");
        int[] dist = dijkstra(0, adj);
        for (int i = 0; i < V; i++) {
            System.out.println("0 → " + i + ": " + (dist[i] == INF ? "INF" : dist[i]));
        }

        // 2. 최단 경로 역추적
        System.out.println("\n2) 0 → 3 최단 경로:");
        List<Integer> path = dijkstraPath(0, 3, adj);
        System.out.println("경로: " + path);
        System.out.println("거리: " + dist[3]);

        // 3. 단일 목적지
        System.out.println("\n3) 0 → 2 최단 거리: " + dijkstraSingle(0, 2, adj));
    }

    private static void addEdge(List<List<int[]>> adj, int u, int v, int weight) {
        adj.get(u).add(new int[]{v, weight});
        adj.get(v).add(new int[]{u, weight});
    }
}

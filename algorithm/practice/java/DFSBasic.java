package section6_graph.traversal;

import java.util.*;

/**
 * 깊이 우선 탐색 (DFS - Depth First Search)
 *
 * ========================================
 * 1. DFS란?
 * ========================================
 *
 * 그래프를 탐색하는 방법 중 하나로,
 * 가능한 깊이 들어갔다가 더 이상 갈 곳이 없으면 돌아오는 방식입니다.
 *
 * 특징:
 * - 스택 또는 재귀로 구현
 * - 경로 탐색, 사이클 검출 등에 유용
 * - 모든 정점을 방문하려면 visited 배열 필요
 *
 * ========================================
 * 2. 시간/공간 복잡도
 * ========================================
 *
 * 인접 리스트: O(V + E)
 * 인접 행렬: O(V²)
 * 공간: O(V) - visited 배열 + 재귀 스택
 *
 * ========================================
 * 3. DFS 활용
 * ========================================
 *
 * - 연결 요소(Connected Component) 찾기
 * - 사이클 검출
 * - 위상 정렬
 * - 경로 탐색
 * - 미로 탐색
 * - 백트래킹
 */
public class DFSBasic {

    static List<List<Integer>> adj;
    static boolean[] visited;

    /**
     * DFS - 재귀 구현
     */
    public static void dfsRecursive(int node) {
        visited[node] = true;
        System.out.print(node + " ");

        for (int next : adj.get(node)) {
            if (!visited[next]) {
                dfsRecursive(next);
            }
        }
    }

    /**
     * DFS - 스택 구현
     */
    public static void dfsStack(int start) {
        Stack<Integer> stack = new Stack<>();
        boolean[] visited = new boolean[adj.size()];

        stack.push(start);

        while (!stack.isEmpty()) {
            int node = stack.pop();

            if (visited[node]) continue;

            visited[node] = true;
            System.out.print(node + " ");

            // 역순으로 넣어야 작은 번호부터 방문 (선택 사항)
            List<Integer> neighbors = adj.get(node);
            for (int i = neighbors.size() - 1; i >= 0; i--) {
                int next = neighbors.get(i);
                if (!visited[next]) {
                    stack.push(next);
                }
            }
        }
    }

    /**
     * 연결 요소의 개수 세기
     */
    public static int countComponents(int n, List<List<Integer>> graph) {
        boolean[] visited = new boolean[n];
        int count = 0;

        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                dfsComponent(i, graph, visited);
                count++;
            }
        }

        return count;
    }

    private static void dfsComponent(int node, List<List<Integer>> graph, boolean[] visited) {
        visited[node] = true;
        for (int next : graph.get(node)) {
            if (!visited[next]) {
                dfsComponent(next, graph, visited);
            }
        }
    }

    /**
     * 사이클 검출 (무방향 그래프)
     */
    public static boolean hasCycleUndirected(int n, List<List<Integer>> graph) {
        boolean[] visited = new boolean[n];

        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                if (dfsCycle(i, -1, graph, visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean dfsCycle(int node, int parent, List<List<Integer>> graph, boolean[] visited) {
        visited[node] = true;

        for (int next : graph.get(node)) {
            if (!visited[next]) {
                if (dfsCycle(next, node, graph, visited)) {
                    return true;
                }
            } else if (next != parent) {
                // 방문했는데 부모가 아니면 사이클
                return true;
            }
        }

        return false;
    }

    /**
     * 경로 탐색: 시작점에서 도착점까지 경로가 있는지
     */
    public static boolean hasPath(int start, int end, List<List<Integer>> graph) {
        boolean[] visited = new boolean[graph.size()];
        return dfsPath(start, end, graph, visited);
    }

    private static boolean dfsPath(int node, int end, List<List<Integer>> graph, boolean[] visited) {
        if (node == end) return true;

        visited[node] = true;

        for (int next : graph.get(node)) {
            if (!visited[next]) {
                if (dfsPath(next, end, graph, visited)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 모든 경로 출력 (백트래킹)
     */
    public static void printAllPaths(int start, int end, List<List<Integer>> graph) {
        boolean[] visited = new boolean[graph.size()];
        List<Integer> path = new ArrayList<>();
        dfsAllPaths(start, end, graph, visited, path);
    }

    private static void dfsAllPaths(int node, int end, List<List<Integer>> graph,
                                    boolean[] visited, List<Integer> path) {
        visited[node] = true;
        path.add(node);

        if (node == end) {
            System.out.println(path);
        } else {
            for (int next : graph.get(node)) {
                if (!visited[next]) {
                    dfsAllPaths(next, end, graph, visited, path);
                }
            }
        }

        // 백트래킹
        visited[node] = false;
        path.remove(path.size() - 1);
    }

    public static void main(String[] args) {
        System.out.println("=== DFS 기본 예제 ===\n");

        /*
         * 그래프:
         *     0 --- 1
         *     |     |
         *     2 --- 3 --- 4
         */

        int V = 5;
        adj = new ArrayList<>();
        for (int i = 0; i < V; i++) {
            adj.add(new ArrayList<>());
        }

        // 무방향 간선 추가
        addEdge(0, 1);
        addEdge(0, 2);
        addEdge(1, 3);
        addEdge(2, 3);
        addEdge(3, 4);

        // 1. 재귀 DFS
        System.out.println("1) 재귀 DFS (시작: 0):");
        visited = new boolean[V];
        dfsRecursive(0);
        System.out.println();

        // 2. 스택 DFS
        System.out.println("\n2) 스택 DFS (시작: 0):");
        dfsStack(0);
        System.out.println();

        // 3. 연결 요소 개수
        System.out.println("\n3) 연결 요소 개수: " + countComponents(V, adj));

        // 4. 사이클 검출
        System.out.println("4) 사이클 존재: " + hasCycleUndirected(V, adj));

        // 5. 경로 존재 여부
        System.out.println("5) 0→4 경로 존재: " + hasPath(0, 4, adj));

        // 6. 모든 경로 출력
        System.out.println("\n6) 0→4 모든 경로:");
        printAllPaths(0, 4, adj);
    }

    private static void addEdge(int u, int v) {
        adj.get(u).add(v);
        adj.get(v).add(u);
    }
}

package section6_graph.basics;

import java.util.*;

/**
 * 그래프 표현 방법 (Graph Representation)
 *
 * ========================================
 * 1. 그래프란?
 * ========================================
 *
 * 그래프 G = (V, E)
 * - V: 정점(Vertex/Node)의 집합
 * - E: 간선(Edge)의 집합
 *
 * 그래프 종류:
 * - 방향 그래프 (Directed): 간선에 방향이 있음
 * - 무방향 그래프 (Undirected): 양방향 이동 가능
 * - 가중치 그래프 (Weighted): 간선에 비용이 있음
 *
 * ========================================
 * 2. 그래프 표현 방법
 * ========================================
 *
 * 1) 인접 행렬 (Adjacency Matrix)
 *    - 2D 배열로 연결 여부 저장
 *    - adj[i][j] = 1 if 간선 존재, 0 otherwise
 *    - 공간: O(V²)
 *    - 간선 확인: O(1)
 *    - 인접 정점 순회: O(V)
 *
 * 2) 인접 리스트 (Adjacency List)
 *    - 각 정점에 연결된 정점들의 리스트
 *    - 공간: O(V + E)
 *    - 간선 확인: O(degree)
 *    - 인접 정점 순회: O(degree)
 *
 * ========================================
 * 3. 선택 기준
 * ========================================
 *
 * 인접 행렬이 유리한 경우:
 * - 간선이 많은 밀집 그래프 (E ≈ V²)
 * - 특정 간선의 존재 여부를 자주 확인
 *
 * 인접 리스트가 유리한 경우:
 * - 간선이 적은 희소 그래프 (E << V²)
 * - 모든 인접 정점을 순회해야 하는 경우
 * - 대부분의 코딩 테스트 문제
 */
public class GraphRepresentation {

    /**
     * 1. 인접 행렬 (Adjacency Matrix)
     */
    public static class AdjacencyMatrix {
        int V;           // 정점 수
        int[][] matrix;  // 인접 행렬

        public AdjacencyMatrix(int V) {
            this.V = V;
            matrix = new int[V][V];
        }

        // 무방향 간선 추가
        public void addEdge(int u, int v) {
            matrix[u][v] = 1;
            matrix[v][u] = 1;  // 무방향
        }

        // 방향 간선 추가
        public void addDirectedEdge(int u, int v) {
            matrix[u][v] = 1;
        }

        // 가중치 간선 추가
        public void addWeightedEdge(int u, int v, int weight) {
            matrix[u][v] = weight;
            matrix[v][u] = weight;
        }

        // 간선 존재 여부
        public boolean hasEdge(int u, int v) {
            return matrix[u][v] != 0;
        }

        // 그래프 출력
        public void print() {
            System.out.println("인접 행렬:");
            for (int i = 0; i < V; i++) {
                System.out.println(Arrays.toString(matrix[i]));
            }
        }
    }

    /**
     * 2. 인접 리스트 (Adjacency List) - List<List<Integer>>
     */
    public static class AdjacencyList {
        int V;
        List<List<Integer>> adj;

        public AdjacencyList(int V) {
            this.V = V;
            adj = new ArrayList<>();
            for (int i = 0; i < V; i++) {
                adj.add(new ArrayList<>());
            }
        }

        // 무방향 간선 추가
        public void addEdge(int u, int v) {
            adj.get(u).add(v);
            adj.get(v).add(u);
        }

        // 방향 간선 추가
        public void addDirectedEdge(int u, int v) {
            adj.get(u).add(v);
        }

        // 그래프 출력
        public void print() {
            System.out.println("인접 리스트:");
            for (int i = 0; i < V; i++) {
                System.out.println(i + " → " + adj.get(i));
            }
        }
    }

    /**
     * 3. 인접 리스트 - 가중치 그래프
     */
    public static class WeightedAdjacencyList {
        int V;
        List<List<int[]>> adj;  // int[] = {연결 정점, 가중치}

        public WeightedAdjacencyList(int V) {
            this.V = V;
            adj = new ArrayList<>();
            for (int i = 0; i < V; i++) {
                adj.add(new ArrayList<>());
            }
        }

        // 무방향 가중치 간선 추가
        public void addEdge(int u, int v, int weight) {
            adj.get(u).add(new int[]{v, weight});
            adj.get(v).add(new int[]{u, weight});
        }

        // 방향 가중치 간선 추가
        public void addDirectedEdge(int u, int v, int weight) {
            adj.get(u).add(new int[]{v, weight});
        }

        public void print() {
            System.out.println("가중치 인접 리스트:");
            for (int i = 0; i < V; i++) {
                System.out.print(i + " → ");
                for (int[] edge : adj.get(i)) {
                    System.out.print("(" + edge[0] + ", w=" + edge[1] + ") ");
                }
                System.out.println();
            }
        }
    }

    /**
     * 4. 간선 리스트 (Edge List)
     *
     * 크루스칼 알고리즘 등에서 사용
     */
    public static class EdgeList {
        int V;
        List<int[]> edges;  // int[] = {시작, 끝, 가중치}

        public EdgeList(int V) {
            this.V = V;
            edges = new ArrayList<>();
        }

        public void addEdge(int u, int v, int weight) {
            edges.add(new int[]{u, v, weight});
        }

        // 가중치 기준 정렬
        public void sortByWeight() {
            edges.sort((a, b) -> a[2] - b[2]);
        }

        public void print() {
            System.out.println("간선 리스트:");
            for (int[] edge : edges) {
                System.out.println(edge[0] + " -- " + edge[2] + " --> " + edge[1]);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("=== 그래프 표현 방법 ===\n");

        /*
         * 예시 그래프:
         *     0 --- 1
         *     |   / |
         *     |  /  |
         *     | /   |
         *     2 --- 3
         */

        int V = 4;

        // 1. 인접 행렬
        System.out.println("1) 인접 행렬");
        AdjacencyMatrix am = new AdjacencyMatrix(V);
        am.addEdge(0, 1);
        am.addEdge(0, 2);
        am.addEdge(1, 2);
        am.addEdge(1, 3);
        am.addEdge(2, 3);
        am.print();

        // 2. 인접 리스트
        System.out.println("\n2) 인접 리스트");
        AdjacencyList al = new AdjacencyList(V);
        al.addEdge(0, 1);
        al.addEdge(0, 2);
        al.addEdge(1, 2);
        al.addEdge(1, 3);
        al.addEdge(2, 3);
        al.print();

        // 3. 가중치 인접 리스트
        System.out.println("\n3) 가중치 인접 리스트");
        WeightedAdjacencyList wal = new WeightedAdjacencyList(V);
        wal.addEdge(0, 1, 5);
        wal.addEdge(0, 2, 3);
        wal.addEdge(1, 2, 2);
        wal.addEdge(1, 3, 4);
        wal.addEdge(2, 3, 6);
        wal.print();

        // 4. 간선 리스트
        System.out.println("\n4) 간선 리스트");
        EdgeList el = new EdgeList(V);
        el.addEdge(0, 1, 5);
        el.addEdge(0, 2, 3);
        el.addEdge(1, 2, 2);
        el.addEdge(1, 3, 4);
        el.addEdge(2, 3, 6);
        el.sortByWeight();
        el.print();
    }
}

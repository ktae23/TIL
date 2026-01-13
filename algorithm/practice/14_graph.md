# 14. 그래프 표현 (Graph Representation)

> 관련 코드: `section6_graph/basics/GraphRepresentation.java`

---

## 1. 그래프란?

**G = (V, E)**
- **V**: 정점(Vertex/Node)의 집합
- **E**: 간선(Edge)의 집합

### 그래프 종류

| 종류 | 설명 |
|------|------|
| 방향 그래프 | 간선에 방향이 있음 |
| 무방향 그래프 | 양방향 이동 가능 |
| 가중치 그래프 | 간선에 비용이 있음 |

---

## 2. 그래프 표현 방법

### 1) 인접 행렬 (Adjacency Matrix)

2D 배열로 연결 여부 저장
- `adj[i][j] = 1` if 간선 존재
- `adj[i][j] = 0` otherwise

| 항목 | 복잡도 |
|------|--------|
| 공간 | O(V²) |
| 간선 확인 | O(1) |
| 인접 정점 순회 | O(V) |

```java
public class AdjacencyMatrix {
    int V;
    int[][] matrix;

    public AdjacencyMatrix(int V) {
        this.V = V;
        matrix = new int[V][V];
    }

    // 무방향 간선
    public void addEdge(int u, int v) {
        matrix[u][v] = 1;
        matrix[v][u] = 1;
    }

    // 방향 간선
    public void addDirectedEdge(int u, int v) {
        matrix[u][v] = 1;
    }

    // 간선 존재 여부
    public boolean hasEdge(int u, int v) {
        return matrix[u][v] != 0;
    }
}
```

### 2) 인접 리스트 (Adjacency List)

각 정점에 연결된 정점들의 리스트

| 항목 | 복잡도 |
|------|--------|
| 공간 | O(V + E) |
| 간선 확인 | O(degree) |
| 인접 정점 순회 | O(degree) |

```java
public class AdjacencyList {
    int V;
    List<List<Integer>> adj;

    public AdjacencyList(int V) {
        this.V = V;
        adj = new ArrayList<>();
        for (int i = 0; i < V; i++) {
            adj.add(new ArrayList<>());
        }
    }

    // 무방향 간선
    public void addEdge(int u, int v) {
        adj.get(u).add(v);
        adj.get(v).add(u);
    }

    // 방향 간선
    public void addDirectedEdge(int u, int v) {
        adj.get(u).add(v);
    }
}
```

### 3) 가중치 인접 리스트

```java
List<List<int[]>> adj;  // int[] = {연결 정점, 가중치}

public void addEdge(int u, int v, int weight) {
    adj.get(u).add(new int[]{v, weight});
    adj.get(v).add(new int[]{u, weight});
}
```

### 4) 간선 리스트 (Edge List)

크루스칼 알고리즘 등에서 사용

```java
List<int[]> edges;  // int[] = {시작, 끝, 가중치}

public void addEdge(int u, int v, int weight) {
    edges.add(new int[]{u, v, weight});
}

// 가중치 기준 정렬
public void sortByWeight() {
    edges.sort((a, b) -> a[2] - b[2]);
}
```

---

## 3. 선택 기준

### 인접 행렬이 유리한 경우
- 간선이 많은 **밀집 그래프** (E ≈ V²)
- 특정 간선의 존재 여부를 **자주 확인**

### 인접 리스트가 유리한 경우
- 간선이 적은 **희소 그래프** (E << V²)
- 모든 **인접 정점을 순회**해야 하는 경우
- **대부분의 코딩 테스트** 문제

---

## 4. 예시 그래프

```
    0 --- 1
    |   / |
    |  /  |
    | /   |
    2 --- 3
```

### 인접 행렬
```
  0 1 2 3
0 0 1 1 0
1 1 0 1 1
2 1 1 0 1
3 0 1 1 0
```

### 인접 리스트
```
0 → [1, 2]
1 → [0, 2, 3]
2 → [0, 1, 3]
3 → [1, 2]
```

---

## 핵심 정리

1. **희소 그래프** → 인접 리스트 (대부분의 경우)
2. **밀집 그래프** 또는 간선 조회가 많음 → 인접 행렬
3. 가중치 그래프는 `int[]` 또는 클래스로 저장
4. 간선 정렬이 필요하면 간선 리스트

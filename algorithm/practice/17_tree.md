# 트리 순회 (Tree Traversal)

> 관련 코드: `section6_graph/tree/TreeTraversal.java`

---

## 1. 트리란?

**사이클이 없는 연결 그래프**입니다.

### 특징
- N개의 노드, **N-1개의 간선**
- **루트 노드**가 있음 (rooted tree)
- 부모-자식 관계

---

## 2. 이진 트리 순회 방법

| 순회 | 순서 | 용도 |
|------|------|------|
| **전위** (Preorder) | 루트 → 왼쪽 → 오른쪽 | 트리 복사, 전위 표기법 |
| **중위** (Inorder) | 왼쪽 → 루트 → 오른쪽 | BST 정렬 순서 |
| **후위** (Postorder) | 왼쪽 → 오른쪽 → 루트 | 트리 삭제, 후위 표기법 |
| **레벨** (Level-order) | BFS | 최단 경로, 층별 처리 |

---

## 3. 순회 시각화

```
        1
       / \
      2   3
     / \   \
    4   5   6
```

| 순회 | 결과 |
|------|------|
| 전위 | 1 2 4 5 3 6 |
| 중위 | 4 2 5 1 3 6 |
| 후위 | 4 5 2 6 3 1 |
| 레벨 | 1 2 3 4 5 6 |

---

## 4. 구현

### 이진 트리 노드
```java
class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;

    TreeNode(int val) {
        this.val = val;
    }
}
```

### 전위 순회 (Preorder)
```java
public static void preorder(TreeNode node) {
    if (node == null) return;

    System.out.print(node.val + " ");  // 루트
    preorder(node.left);                // 왼쪽
    preorder(node.right);               // 오른쪽
}
```

### 중위 순회 (Inorder)
```java
public static void inorder(TreeNode node) {
    if (node == null) return;

    inorder(node.left);                 // 왼쪽
    System.out.print(node.val + " ");   // 루트
    inorder(node.right);                // 오른쪽
}
```

### 후위 순회 (Postorder)
```java
public static void postorder(TreeNode node) {
    if (node == null) return;

    postorder(node.left);               // 왼쪽
    postorder(node.right);              // 오른쪽
    System.out.print(node.val + " ");   // 루트
}
```

### 레벨 순회 (Level-order)
```java
public static void levelOrder(TreeNode root) {
    if (root == null) return;

    Queue<TreeNode> queue = new LinkedList<>();
    queue.offer(root);

    while (!queue.isEmpty()) {
        TreeNode node = queue.poll();
        System.out.print(node.val + " ");

        if (node.left != null) queue.offer(node.left);
        if (node.right != null) queue.offer(node.right);
    }
}
```

### 레벨별 구분
```java
public static List<List<Integer>> levelOrderList(TreeNode root) {
    List<List<Integer>> result = new ArrayList<>();
    if (root == null) return result;

    Queue<TreeNode> queue = new LinkedList<>();
    queue.offer(root);

    while (!queue.isEmpty()) {
        int size = queue.size();
        List<Integer> level = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            TreeNode node = queue.poll();
            level.add(node.val);

            if (node.left != null) queue.offer(node.left);
            if (node.right != null) queue.offer(node.right);
        }

        result.add(level);
    }
    return result;
}
```

---

## 5. 반복문 구현 (스택)

### 전위 순회
```java
public static void preorderIterative(TreeNode root) {
    if (root == null) return;

    Stack<TreeNode> stack = new Stack<>();
    stack.push(root);

    while (!stack.isEmpty()) {
        TreeNode node = stack.pop();
        System.out.print(node.val + " ");

        // 오른쪽을 먼저 넣어야 왼쪽이 먼저 나옴
        if (node.right != null) stack.push(node.right);
        if (node.left != null) stack.push(node.left);
    }
}
```

### 중위 순회
```java
public static void inorderIterative(TreeNode root) {
    Stack<TreeNode> stack = new Stack<>();
    TreeNode curr = root;

    while (curr != null || !stack.isEmpty()) {
        // 왼쪽 끝까지 이동
        while (curr != null) {
            stack.push(curr);
            curr = curr.left;
        }

        curr = stack.pop();
        System.out.print(curr.val + " ");

        curr = curr.right;
    }
}
```

---

## 6. 트리 정보

### 트리의 높이
```java
public static int height(TreeNode node) {
    if (node == null) return 0;
    return 1 + Math.max(height(node.left), height(node.right));
}
```

### 노드 개수
```java
public static int countNodes(TreeNode node) {
    if (node == null) return 0;
    return 1 + countNodes(node.left) + countNodes(node.right);
}
```

---

## 7. N-ary 트리 순회

코딩 테스트에서 자주 등장하는 형태

```java
public static void traverseNaryTree(int node, List<List<Integer>> adj, boolean[] visited) {
    visited[node] = true;
    System.out.print(node + " ");

    for (int child : adj.get(node)) {
        if (!visited[child]) {
            traverseNaryTree(child, adj, visited);
        }
    }
}
```

---

## 핵심 정리

1. **전위**: 루트 먼저 (복사, 직렬화)
2. **중위**: BST에서 정렬 순서
3. **후위**: 자식 먼저 (삭제, 계산)
4. **레벨**: BFS로 층별 처리

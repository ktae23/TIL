package section6_graph.tree;

import java.util.*;

/**
 * 트리 순회 (Tree Traversal)
 *
 * ========================================
 * 1. 트리란?
 * ========================================
 *
 * 트리는 사이클이 없는 연결 그래프입니다.
 *
 * 특징:
 * - N개의 노드, N-1개의 간선
 * - 루트 노드가 있음 (rooted tree)
 * - 부모-자식 관계
 *
 * ========================================
 * 2. 이진 트리 순회 방법
 * ========================================
 *
 * 1) 전위 순회 (Preorder): 루트 → 왼쪽 → 오른쪽
 * 2) 중위 순회 (Inorder): 왼쪽 → 루트 → 오른쪽
 * 3) 후위 순회 (Postorder): 왼쪽 → 오른쪽 → 루트
 * 4) 레벨 순회 (Level-order): BFS
 *
 * ========================================
 * 3. 활용
 * ========================================
 *
 * - 전위: 트리 복사, 표현식 트리의 전위 표기법
 * - 중위: 이진 탐색 트리의 정렬된 순서
 * - 후위: 트리 삭제, 표현식 트리의 후위 표기법
 * - 레벨: 최단 경로, 층별 처리
 */
public class TreeTraversal {

    /**
     * 이진 트리 노드
     */
    static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;

        TreeNode(int val) {
            this.val = val;
        }
    }

    /**
     * 전위 순회 (Preorder): 루트 → 왼쪽 → 오른쪽
     */
    public static void preorder(TreeNode node) {
        if (node == null) return;

        System.out.print(node.val + " ");  // 루트
        preorder(node.left);                // 왼쪽
        preorder(node.right);               // 오른쪽
    }

    /**
     * 중위 순회 (Inorder): 왼쪽 → 루트 → 오른쪽
     */
    public static void inorder(TreeNode node) {
        if (node == null) return;

        inorder(node.left);                 // 왼쪽
        System.out.print(node.val + " ");   // 루트
        inorder(node.right);                // 오른쪽
    }

    /**
     * 후위 순회 (Postorder): 왼쪽 → 오른쪽 → 루트
     */
    public static void postorder(TreeNode node) {
        if (node == null) return;

        postorder(node.left);               // 왼쪽
        postorder(node.right);              // 오른쪽
        System.out.print(node.val + " ");   // 루트
    }

    /**
     * 레벨 순회 (Level-order): BFS
     */
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

    /**
     * 레벨별로 구분하여 출력
     */
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

    /**
     * 전위 순회 - 반복문 (스택)
     */
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

    /**
     * 중위 순회 - 반복문 (스택)
     */
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

    /**
     * 트리의 높이 (깊이)
     */
    public static int height(TreeNode node) {
        if (node == null) return 0;
        return 1 + Math.max(height(node.left), height(node.right));
    }

    /**
     * 트리의 노드 개수
     */
    public static int countNodes(TreeNode node) {
        if (node == null) return 0;
        return 1 + countNodes(node.left) + countNodes(node.right);
    }

    /**
     * N-ary 트리 순회 (일반 트리)
     *
     * 코딩 테스트에서 자주 등장하는 형태
     */
    public static void traverseNaryTree(int node, List<List<Integer>> adj, boolean[] visited) {
        visited[node] = true;
        System.out.print(node + " ");

        for (int child : adj.get(node)) {
            if (!visited[child]) {
                traverseNaryTree(child, adj, visited);
            }
        }
    }

    /**
     * BOJ 1991 - 트리 순회
     *
     * 알파벳으로 주어진 이진 트리의 전위/중위/후위 순회
     */
    public static class BinaryTreeAlpha {
        static int[][] tree;  // tree[i] = {왼쪽 자식, 오른쪽 자식}
        static StringBuilder sb;

        public static void solve(int n, int[][] edges) {
            tree = new int[26][2];
            for (int[] row : tree) Arrays.fill(row, -1);

            for (int[] edge : edges) {
                int parent = edge[0];
                int left = edge[1];
                int right = edge[2];
                tree[parent][0] = left;
                tree[parent][1] = right;
            }

            sb = new StringBuilder();

            // 전위
            preorderAlpha(0);
            sb.append("\n");

            // 중위
            inorderAlpha(0);
            sb.append("\n");

            // 후위
            postorderAlpha(0);

            System.out.println(sb);
        }

        static void preorderAlpha(int node) {
            if (node == -1) return;
            sb.append((char) ('A' + node));
            preorderAlpha(tree[node][0]);
            preorderAlpha(tree[node][1]);
        }

        static void inorderAlpha(int node) {
            if (node == -1) return;
            inorderAlpha(tree[node][0]);
            sb.append((char) ('A' + node));
            inorderAlpha(tree[node][1]);
        }

        static void postorderAlpha(int node) {
            if (node == -1) return;
            postorderAlpha(tree[node][0]);
            postorderAlpha(tree[node][1]);
            sb.append((char) ('A' + node));
        }
    }

    public static void main(String[] args) {
        System.out.println("=== 트리 순회 예제 ===\n");

        /*
         * 이진 트리:
         *        1
         *       / \
         *      2   3
         *     / \   \
         *    4   5   6
         */

        TreeNode root = new TreeNode(1);
        root.left = new TreeNode(2);
        root.right = new TreeNode(3);
        root.left.left = new TreeNode(4);
        root.left.right = new TreeNode(5);
        root.right.right = new TreeNode(6);

        // 1. 전위 순회
        System.out.print("1) 전위 순회: ");
        preorder(root);
        System.out.println();

        // 2. 중위 순회
        System.out.print("2) 중위 순회: ");
        inorder(root);
        System.out.println();

        // 3. 후위 순회
        System.out.print("3) 후위 순회: ");
        postorder(root);
        System.out.println();

        // 4. 레벨 순회
        System.out.print("4) 레벨 순회: ");
        levelOrder(root);
        System.out.println();

        // 5. 레벨별 구분
        System.out.println("5) 레벨별 구분: " + levelOrderList(root));

        // 6. 반복문 순회
        System.out.print("6) 전위 (반복): ");
        preorderIterative(root);
        System.out.println();

        System.out.print("7) 중위 (반복): ");
        inorderIterative(root);
        System.out.println();

        // 8. 트리 정보
        System.out.println("\n8) 트리 높이: " + height(root));
        System.out.println("9) 노드 개수: " + countNodes(root));

        // 10. BOJ 1991 예제
        System.out.println("\n10) BOJ 1991 스타일:");
        // A(0)->B(1),C(2), B(1)->D(3),E(4), C(2)->.F(5)
        int[][] edges = {
                {0, 1, 2},   // A -> B, C
                {1, 3, 4},   // B -> D, E
                {2, -1, 5}   // C -> ., F
        };
        BinaryTreeAlpha.solve(7, edges);
    }
}

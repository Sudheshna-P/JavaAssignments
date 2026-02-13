/**
 * Binary Search tree implementation
 *
 * @param <T> the type of element that is stored
 *            Comparable must be implemented
 */
public class BST<T extends Comparable<T>> {

    private Node root;

    private class Node {
        T data;
        Node left, right;

        Node(T data) {
            this.data = data;
        }
    }

    /**
     * To insert the data in the bst.
     *
     * @param data is the value that is inserted
     */
    public void insert(T data) {
        Node nn = new Node(data);
        if (root == null) {
            root = nn;
            return;
        }
        Node curr = root;
        while (true) {
            if (data.compareTo(curr.data) == 0) return;
            if (data.compareTo(curr.data) < 0) {
                if (curr.left == null) {
                    curr.left = nn;
                    break;
                }
                curr = curr.left;

            } else {
                if (curr.right == null) {
                    curr.right = nn;
                    break;
                }
                curr = curr.right;
            }

        }
    }

    /**
     * To find whether the target is present in the bst
     *
     * @param target is the value to search for
     * @return {@code true} if the target is present else {@code false}
     */
    public boolean find(T target) {
        Node curr = root;
        while (curr != null) {

            int cmp = target.compareTo(curr.data);

            if (cmp == 0)
                return true;
            else if (cmp < 0)
                curr = curr.left;
            else
                curr = curr.right;
        }
        return false;
    }

    /**
     * to find the minimum value in the tree
     *
     * @return the minimum value and null if tree is empty
     */
    public T findMin() {
        if (root == null) return null;
        Node curr = root;
        while (curr.left != null)
            curr = curr.left;
        return curr.data;
    }

    /**
     * To find the maximum value
     *
     * @return the maximum value in the tree
     */
    public T findMax() {
        if (root == null) return null;
        Node curr = root;
        while (curr.right != null)
            curr = curr.right;
        return curr.data;
    }

    /**
     * to return the height of the tree
     *
     * @return the height of the tree. if null, -1 is returned
     */
    public int height() {
        return height(root);
    }

    private int height(Node root) {
        if (root == null) return -1;
        return Math.max(height(root.left), height(root.right)) + 1;
    }

    /**
     * to check if the tree is balanced
     * A tree is balanced if the height diff is -1 or 0 or 1
     *
     * @return {@code true} if it is balanced else {@code false}
     */
    public boolean isBalanced() {
        return isBalanced(root);
    }

    private boolean isBalanced(Node root) {
        if (root == null) return true;

        int l = height(root.left);
        int r = height(root.right);

        if (Math.abs(l - r) > 1) return false;

        return isBalanced(root.left) && isBalanced(root.right);
    }

    /**
     * to check whether the tree is bst
     *
     * @return {@code true} is the tree is a bst else return {@code false}
     */
    public boolean isBST() {
        return isBST(root, null, null);
    }


    private boolean isBST(Node root, T min, T max) {
        if (root == null) return true;

        if (min != null && root.data.compareTo(min) <= 0) return false;
        if (max != null && root.data.compareTo(max) >= 0) return false;

        return isBST(root.left, min, root.data) &&
                isBST(root.right, root.data, max);
    }

    /**
     * inorder traversal - left root right
     */
    public void inorder() {
        inorder(root);
        System.out.println();
    }

    private void inorder(Node root) {
        if (root == null) return;
        inorder(root.left);
        System.out.print(root.data + " ");
        inorder(root.right);
    }

    /**
     * preorder traversal - root left right
     */
    public void preorder() {
        preorder(root);
        System.out.println();
    }

    private void preorder(Node root) {
        if (root == null) return;
        System.out.print(root.data + " ");
        preorder(root.left);
        preorder(root.right);
    }

    /**
     * postorder traversal - left right root
     */
    public void postorder() {
        postorder(root);
        System.out.println();
    }

    private void postorder(Node root) {
        if (root == null) return;
        postorder(root.left);
        postorder(root.right);
        System.out.print(root.data + " ");
    }

    /**
     * Delete a value from the bst
     *
     * @param key is the value to be deleted.
     * returns null if the root is null and the deleted value is returned
     */
    public void delete(T key) {
        root = delete(root, key);
    }

    private Node delete(Node root, T key) {
        if (root == null) return null;

        int cmp = key.compareTo(root.data);
        if (cmp < 0) {
            root.left = delete(root.left, key);

        } else if (cmp > 0) {
            root.right = delete(root.right, key);
        } else {
            if (root.left == null && root.right == null) return null;
            if (root.left == null) return root.right;
            if (root.right == null) return root.left;
            Node curr = root.right;
            while (curr.left != null) {
                curr = curr.left;
            }
            root.data = curr.data;
            root.right = delete(root.right, curr.data);
        }
        return root;
    }

}

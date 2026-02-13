import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

public class BSTTest {

    private BST<Integer> bst;
    private BST<String> strbst;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    /**
     * Runs before each test case
     */
    @BeforeEach
    void setUp() {
        bst = new BST<>();
        strbst=new BST<>();
        System.setOut(new PrintStream(out));

        int[] values = {14, 2, 40, 9, 31, 25, 18, 5};
        for (int v : values) {
            bst.insert(v);
        }
    }

    @Test
    void testStringBst(){
        strbst.insert("a");
        strbst.insert("b");
        strbst.insert("c");
        assertTrue(strbst.find("a"));
        assertEquals("a", strbst.findMin());
        strbst.delete("b");
        assertFalse(strbst.find("b"));
    }

    @Test
    void testInsertAndFind() {
        assertTrue(bst.find(14));
        assertTrue(bst.find(5));
        assertFalse(bst.find(100));
    }

    @Test
    void testFindMin() {
        assertEquals(2, bst.findMin());
    }

    @Test
    void testFindMax() {
        assertEquals(40, bst.findMax());
    }

    @Test
    void testHeight() {
        assertEquals(4, bst.height());
    }

    @Test
    void testIsBST() {
        assertTrue(bst.isBST());
    }

    @Test
    void testDeleteLeafNode() {
        bst.delete(5);
        assertFalse(bst.find(5));
    }

    @Test
    void testDeleteNodeWithOneChild() {
        bst.delete(9);
        assertFalse(bst.find(9));
    }
    @Test
    void testDeleteFromEmptyTree() {
        BST<Integer> empty = new BST<>();
        empty.delete(10); // should not throw
        assertTrue(empty.isBST());
    }


    @Test
    void testDeleteNodeWithTwoChildren() {
        bst.delete(14);
        assertFalse(bst.find(14));
        assertTrue(bst.isBST());
    }

    @Test
    void testEmptyTree() {
        BST<Integer> empty = new BST<>();

        assertFalse(empty.find(10));
        assertNull(empty.findMin());
        assertNull(empty.findMax());
        assertEquals(-1, empty.height());
        assertTrue(empty.isBalanced());
        assertTrue(empty.isBST());
    }

    @Test
    void testInorder() {
        bst.inorder();
        assertEquals("2 5 9 14 18 25 31 40 \n", out.toString());
    }

    @Test
    void testPreorder() {
        bst.preorder();
        assertEquals("14 2 9 5 40 31 25 18 \n", out.toString());
    }


    @Test
    void testPostorder() {
        bst.postorder();
        assertEquals("5 9 2 18 25 31 40 14 \n", out.toString());
    }

    @Test
    void testTraversalsOnEmptyTree() {
        BST<Integer> empty = new BST<>();

        empty.inorder();
        assertEquals("\n", out.toString());

        out.reset();

        empty.preorder();
        assertEquals("\n", out.toString());

        out.reset();

        empty.postorder();
        assertEquals("\n", out.toString());
    }

    @Test
    void testStringOrdering() {
        strbst.insert("dog");
        strbst.insert("cat");
        strbst.insert("apple");

        assertEquals("apple", strbst.findMin());
        assertEquals("dog", strbst.findMax());
    }

    @Test
    void testBalancedTree() {
        BST<Integer> balanced = new BST<>();
        balanced.insert(10);
        balanced.insert(5);
        balanced.insert(15);

        assertTrue(balanced.isBalanced());
    }

    @Test
    void testIsBalanced() {
        assertFalse(bst.isBalanced());
    }

}

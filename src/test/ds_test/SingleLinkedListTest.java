import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.NoSuchElementException;
import static org.junit.jupiter.api.Assertions.*;

class SingleLinkedListTest {

    private SingleLinkedList<String> list;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() {
        list = new SingleLinkedList<>();
        System.setOut(new PrintStream(out));
    }

    @Test
    void testInitialization() {
        assertEquals(0, list.size());
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
        assertThrows(NoSuchElementException.class, () -> list.getLast());
    }

    @Test
    void testRemoveFirstOnEmpty() {
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    void testRemoveLastOnEmpty() {
        assertThrows(NoSuchElementException.class, () -> list.removeLast());
    }

    @Test
    void testAddAndGetFirst() {
        list.add("one");
        assertEquals("one", list.getFirst());
    }

    @Test
    void testMiddle() {
        list.add("one");
        list.add("two");
        list.add("three");
        list.add("four");
        list.add("five");
        assertEquals("three", list.middle());
    }

    @Test
    void testAddFirst() {
        list.addFirst("one");
        list.addFirst("two");
        assertEquals("two", list.getFirst());  // "two" should be the first element
    }

    @Test
    void testRemoveFirst() {
        list.add("one");
        list.add("two");
        assertEquals("one", list.removeFirst());
        assertEquals("two", list.getFirst());  // "two" should be the new first element
    }

    @Test
    void testRemoveLast() {
        list.add("one");
        list.add("two");
        list.add("three");
        assertEquals("three", list.removeLast());  // Should remove "three"
        assertEquals("two", list.getLast());       // "two" should be the last element
    }

    @Test
    void testSize() {
        list.add("one");
        list.add("two");
        assertEquals(2, list.size());  // Size should be 2
    }

    @Test
    void testReverse() {
        list.add("one");
        list.add("two");
        list.add("three");
        list.reverse();
        assertEquals("three", list.getFirst());  // After reversal, "three" should be first
        assertEquals("one", list.getLast());     // After reversal, "one" should be last
    }

    @Test
    void testClear(){
        list.add("one");
        list.add("two");
        list.clear();
        assertEquals(0, list.size());
        assertThrows(NoSuchElementException.class, () -> list.getFirst());

    }

    @Test
    void testContains(){
        list.add("one");
        list.add("two");
        assertTrue(list.contains("one"));
        assertFalse(list.contains("three"));
    }

    @Test
    void testHasCycle() {
        assertFalse(list.cycle());

        list.add("first");
        list.createSelfCycleForTest();

        assertTrue(list.cycle());
    }

    @Test
    void testMergeTwoLists() {

        list.add("one");
        list.add("two");
        SingleLinkedList<String> list2 = new SingleLinkedList<>();
        list2.add("three");
        list2.add("four");
        list.merge(list2);
        assertEquals(4, list.size());
        assertEquals("one", list.getFirst());
        assertEquals("four", list.getLast());
    }

    @Test
    void testPrintList() {
        list.add("one");
        list.add("two");
        list.printList();
        assertEquals("one -> two -> Null\n", out.toString());
    }


}

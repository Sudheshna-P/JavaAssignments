import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

public class HashTableTest {

    private HashTable<String, String> hashtable;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    @BeforeEach
    public void setUp() {

        hashtable = new HashTable<>();
        System.setOut(new PrintStream(out));
    }

    @Test
    public void testPutAndGet() {
        hashtable.put("1", "ONE");
        hashtable.put("2", "TWO");
        assertEquals("ONE",hashtable.put("1", "one"));
        assertEquals("one", hashtable.get("1"));
        assertEquals("TWO", hashtable.get("2"));
    }

    @Test
    public void testRemove() {
        hashtable.put("1", "ONE");
        hashtable.remove("1");
        assertNull(hashtable.get("1"));
        hashtable.put("2","Two");
        assertEquals("Two",hashtable.remove("2"));
        assertNull(hashtable.get("2"));
    }

    @Test
    public void testContainsKey() {
        hashtable.put("1", "ONE");
        assertTrue(hashtable.containsKey("1"));
        assertFalse(hashtable.containsKey("2"));
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(hashtable.isEmpty());
        hashtable.put("1", "ONE");
        hashtable.put("2", "TWO");
        assertEquals(2, hashtable.size());
        assertFalse(hashtable.isEmpty());
    }

    @Test
    public void testClear() {
        hashtable.put("1", "ONE");
        hashtable.put("2", "TWO");
        hashtable.clear();
        assertEquals(0, hashtable.size());
        assertTrue(hashtable.isEmpty());
    }

    @Test
    public void testTraverse(){
        hashtable.put("1", "ONE");
        hashtable.put("2", "TWO");
        hashtable.traverse();
        assertEquals("Key: 2 Value: TWO\nKey: 1 Value: ONE\n", out.toString());
    }

    @Test
    public void testResize() {
        HashTable<Integer, String> map = new HashTable<>(2);

        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");

        assertEquals("one", map.get(1));
        assertEquals("two", map.get(2));
        assertEquals("three", map.get(3));
    }


}

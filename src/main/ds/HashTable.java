/**
 * HashTable implementation
 * <K> the type of keys
 * <V> the type of value that is mapped to the key
 */
public class HashTable<K, V> {

    /**
     * Default initial capacity
     *
     */
    private static final int DEFAULT_CAPACITY = 10;
    private static final double LOAD_FACTOR = 0.75;

    private static class Entry<K, V> {
        int hash;
        K key;
        V value;
        Entry<K, V> next;

        /**
         * Constructs a new hashtable entry
         *
         * @param hash  hash code of the key
         * @param key   the key
         * @param value the value
         * @param next  reference to the next entry
         */
        Entry(int hash, K key, V value, Entry<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private Entry<K, V>[] table;
    private int count;

    /**
     * Constructs an empty hashtable with specified capacity
     *
     * @param capacity initial capacity of the table
     *                 throws IllegalArgumentException when the capacity is not greater than 0
     */
    @SuppressWarnings("unchecked")
    public HashTable(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }


        table = (Entry<K, V>[]) new Entry[capacity];
        count = 0;
    }

    public HashTable() {
        this(DEFAULT_CAPACITY);
    }

    private int getIndex(Object key) {
        return (Math.abs(key.hashCode()) % table.length);
    }

    /**
     * Insert/ Put the value into the hashTable
     *
     * @param key   is the Key
     * @param value is the value
     * @return the old value if replaced
     * throws NullPointerException when the key is null
     */
    public V put(K key, V value) {
        if (key == null) throw new NullPointerException("Key cannot be null");

        if ((double) (count + 1) / table.length > LOAD_FACTOR) {
            resize(table.length * 2);
        }
        int hash = key.hashCode();
        int index = getIndex(key);
        Entry<K, V> curr = table[index];
        while (curr != null) {
            if (curr.hash == hash && curr.key.equals(key)) {
                V oldVal = curr.value;
                curr.value = value;
                return oldVal;
            }
            curr = curr.next;
        }
        table[index] = new Entry<>(hash, key, value, table[index]);
        count++;
        return null;
    }

    /**
     * To get the value from the hashtable
     *
     * @param key is the key
     * @return the value of the key
     * throws NullPointerException when the key is null
     */
    public V get(K key) {
        if (key == null) throw new NullPointerException("Key cannot be null");

        int hash = key.hashCode();
        int index = getIndex(key);
        Entry<K, V> curr = table[index];
        while (curr != null) {
            if (curr.hash == hash && curr.key.equals(key)) {
                return curr.value;
            }
            curr = curr.next;
        }
        return null;
    }

    /**
     * to remove the key,value from the table
     *
     * @param key to be removed
     * @return the removed value
     * throws NullPointerException when the key is null
     */
    public V remove(K key) {
        if (key == null) throw new NullPointerException("Key cannot be null");

        int hash = key.hashCode();
        int index = getIndex(key);
        Entry<K, V> curr = table[index];

        Entry<K, V> prev = null;

        while (curr != null) {
            if (curr.hash == hash && curr.key.equals(key)) {
                if (prev == null) {
                    table[index] = curr.next;
                } else {
                    prev.next = curr.next;
                }
                count--;
                return curr.value;
            }
            prev = curr;
            curr = curr.next;
        }
        return null;
    }

    /**
     * To check whether the table contains the key
     *
     * @param key checks the value
     * @return true if present else false
     */
    public boolean containsKey(K key) {
        return get(key) != null;
    }

    /**
     * Gives the size of the hashtable
     *
     * @return the size of the hashtable
     */
    public int size() {
        return count;
    }

    /**
     * Checks if the hashtable is Empty
     *
     * @return true if the table is empty else false
     */
    public boolean isEmpty() {
        return count == 0;
    }

    /**
     * To remove every entry from the hashtable
     */
    public void clear() {
        table = (Entry<K, V>[]) new Entry[table.length];
        count = 0;
    }

    /**
     * To change the existing capacity to a new capacity
     *
     * @param newCapacity new capacity of the hashtable
     */

    public void resize(int newCapacity) {
        Entry<K, V>[] oldTable = table;
        table = (Entry<K, V>[]) new Entry[newCapacity];
        count = 0;
        for (Entry<K, V> head : oldTable) {
            while (head != null) {
                put(head.key, head.value);
                head = head.next;
            }
        }
    }

    /**
     * To traverse the hashtable
     */
    public void traverse() {
        for (Entry<K, V> kvEntry : table) {
            Entry<K, V> curr = kvEntry;
            while (curr != null) {
                System.out.println("Key: " + curr.key + " Value: " + curr.value);
                curr = curr.next;
            }

        }
    }



}

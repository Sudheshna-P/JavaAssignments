import java.util.NoSuchElementException;

/**
 * @param <T> The type of element stored-here String
 */
public class SingleLinkedList<T> {

    private int size;
    private Node first;
    private Node last;


    private class Node {
        T item;
        Node next;

        /**
         * Constructs node with item and next
         *
         * @param item the element that is stored
         * @param next pointer to next node
         */
        Node(T item, Node next) {
            this.item = item;
            this.next = next;
        }
    }

    /**
     * Constructs an empty linked list
     */
    public SingleLinkedList() {
        size = 0;
        first = null;
        last = null;
    }

    /**
     * Returns the first element of the list
     *
     * @return the first element ot null if it is empty
     */
    public T getFirst() {
        if (size == 0) throw new NoSuchElementException("List is empty");
        return first.item;
    }


    /**
     * Returns the last element
     *
     * @return the last element or {@code null} if the list is empty
     * throws NoSuchElementException when the list is empty
     */
    public T getLast() {
        if (size == 0) throw new NoSuchElementException("List is empty");
        return last.item;
    }

    /**
     * Inserts the specified element at the beginning of the list
     *
     * @param e the element to be added
     */
    public void addFirst(T e) {
        Node nn = new Node(e, first);
        first = nn;
        if (size == 0) {
            last = nn;
        }
        size++;
    }

    /**
     * Appends the specified element to the end of the list
     *
     * @param e the element to be added
     */
    public void addLast(T e) {
        Node nn = new Node(e, null);
        if (size == 0) {
            first = nn;
        } else {
            last.next = nn;
        }
        last = nn;
        size++;
    }

    /**
     * Appends the element to the last of the list
     * @param e the element to be added
     */
    public void add(T e) {
        addLast(e);
    }

    /**
     * Removes and returns the first element of the list
     *
     * @return the removed element or {@code null} is the list is empty
     * throws NoSuchElementException when the list is empty
     */
    public T removeFirst() {
        if (size == 0) throw new NoSuchElementException("List is empty");

        T item = first.item;
        first = first.next;
        size--;

        if (size == 0) {
            last = null;
        }
        return item;
    }


    /**
     * Returns the number of elements in the linked list
     *
     * @return the size off the list
     */
    public int size() {
        return size;
    }

    /**
     * Removes the last element from the list
     * throws NoSuchElementException when the list is empty
     */
    public T removeLast() {
        if (size == 0) throw new NoSuchElementException("List is empty");

        if (size == 1) {
            T item = first.item;
            first = last = null;
            size = 0;
            return item;
        }

        Node temp = first;
        while (temp.next != last) {
            temp = temp.next;
        }

        T item = last.item;
        temp.next = null;
        last = temp;
        size--;
        return item;
    }


    /**
     * Returns {@code true} if the list contains the specified element
     *
     * @param target element whose presence is being tested
     * @return {@code true} if present {@code false} if it is not present
     */
    public boolean contains(Object target) {
        Node temp = first;
        while (temp != null) {
            if (temp.item.equals(target)) {
                return true;
            }
            temp = temp.next;
        }
        return false;
    }

    /**
     * Detects whether the list contains a cycle
     *
     * @return {@code true} if cycle exist or {@code false}
     */
    public boolean cycle() {
        Node fast = first;
        Node slow = first;
        while (slow != null && fast != null && fast.next != null) {
            fast = fast.next.next;
            slow = slow.next;
            if (fast == slow) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the middle element of the list
     *
     * @return the middle element, or {@code null} if list is empty
     */
    public T middle() {
        Node fast = first;
        Node slow = first;
        while (slow != null && fast != null && fast.next != null) {
            fast = fast.next.next;
            slow = slow.next;
        }
        if (slow != null) {
            return slow.item;
        } else {
            return null;
        }
    }

    /**
     * Removes all the element from the list
     */
    public void clear() {
        first = null;
        last = null;
        size = 0;
    }

    /**
     * Prints the linked list
     */
    public void printList() {
        Node temp = first;
        while (temp != null) {
            System.out.print(temp.item + " -> ");
            temp = temp.next;
        }
        System.out.println("Null");
    }

    /**
     * Reverses the linked list
     */
    public void reverse() {
        Node prev = null;
        Node curr = first;
        Node next;
        last = first;
        while (curr != null) {
            next = curr.next;
            curr.next = prev;
            prev = curr;
            curr = next;
        }
        first = prev;
    }

    /**
     * Merges another list into this list
     *
     * @param l2 the list to be merged
     */
    public void merge(SingleLinkedList<T> l2) {
        if (this.size == 0) {
            this.first = l2.first;
            this.last = l2.last;
            this.size = l2.size;
            return;
        }
        this.last.next = l2.first;
        if (l2.last != null) {
            this.last = l2.last;
        }
        this.size = this.size + l2.size;
    }

    void createSelfCycleForTest() {
        if (first != null) {
            first.next = first;
        }
    }


}

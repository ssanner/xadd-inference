/**
 * MinIntHeap.java
 *
 *
 * Created: Mon Jul 8 13:07:49 2002
 *
 * @author Scott Sanner - intern
 * @version 1.0
 */
package util;

import java.util.Arrays;

/**
 * A heap implementation for priority queue type operations.
 * Assumes (-1) is an invalid value.
 */
class MinIntHeap {

    // Note, for arithmetic efficiency and clarity, a[1]
    // is the first.  Actual array size is always at least
    // 1 + sz.
    public int[] a;                      // array
    public int   sz = 0;                 // num elements
    
    public final static int DEF_SZ = 32; // default heap size
    
    /**
     * Build a priority queue from scratch
     */
    public MinIntHeap() {
	a  = new int[DEF_SZ];
	sz = 0;
    }

    /**
     * Build a priority queue from an integer array
     */ 
    public MinIntHeap(int[] val, int size) {
	a = new int[size + 1];

	// a[1] is the first element, a[0] is unused
	System.arraycopy(val, 0, a, 1, size);
	sz = size;
	buildHeap();
    }

    /**
     * Turn the array into a heap
     */
    public void buildHeap() {
	for (int i = sz >> 1; i >= 1; i--) {
	    heapify(i);
	}
    }

    /**
     * Heapify the subtree rooted at node i, and recurse the swapped
     * subtree
     */
    public void heapify(int i) {
	
	int l, r, largest;
	boolean cont = true;

	while (cont) {

	    l = 2 * i; // The left subnode
	    r = l + 1; // The right subnode
	    
	    if (l <= sz && a[l] < a[i]) {
		largest = l;
	    } else {
		largest = i;
	    }	    
	    
	    if (r <= sz && a[r] < a[largest]) {
		largest = r;
	    }
	    
	    if (largest != i) {
		int temp = a[i];
		a[i] = a[largest];
		a[largest] = temp;
                i = largest; // heapify(largest)
	    } else {
		cont = false; // No need to recurse
	    }
	}
    }

    /**
     * Insert an element into the queue
     */
    public void insert(int key) {

	// Expand the array?
	if (sz + 1 >= a.length - 1) {
	    int temp[] = new int[a.length * 2];
	    System.arraycopy(a, 0, temp, 0, a.length);
	    a = temp;
	}

	// Find a place to insert the key
	int i = ++sz;
	int parent;
	while (i > 1 && a[parent = (i >> 1)] > key) {
	    a[i] = a[parent];
	    i = parent;
	}
	a[i] = key;
    }

    /**
     * Remove the max element from the queue
     */
    public int removeMin() {
	if (sz == 0) {
	    return (-1);
	}

	int max = a[1];
	a[1] = a[sz];
	sz--;
	heapify(1);

	return max;
    }
    
    /**
     * Check the max element in the queue but do not remove
     */
    public int min() {
	if (sz > 0) {
	    return a[1];
	} else {
	    return (-1);
	}
    }
    
    /**
     * Is heap empty?
     */
    public boolean isEmpty() {
	return (sz == 0);
    }

    /**
     * Clear the heap
     */
    public void clear() {
	sz = 0;
    }
    
    /**
     * Print the current heap
     */
    public String printHeap() {
	StringBuffer sb1 = new StringBuffer();
	StringBuffer sb2 = new StringBuffer();
	for (int i = 1; i <= sz; i++) {
	    sb1.append(i + " ");
	    sb2.append(a[i] + " ");
	}
	return sb2.toString() + "\n" + sb1.toString();
    }
    
    /**
     * Testing method
     */
    
    /*
    public static void main(String args[]) {

	int a[] = {5,2,9,5,8,3,4,5,7,6,10};
	MinIntHeap h = new MinIntHeap(a, a.length);
	h.insert(1);
	h.insert(11);
	h.insert(0);
        h.insert(12);
	h.insert(13);

	while (!h.isEmpty()) {
	   println("Next: " + h.removeMin());
	}
    } */
}

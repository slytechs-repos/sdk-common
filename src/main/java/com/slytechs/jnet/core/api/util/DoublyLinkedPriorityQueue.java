package com.slytechs.jnet.core.api.util;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * A priority queue implementation based on a doubly-linked list. This queue
 * orders elements according to their natural order, or by a Comparator provided
 * at queue construction time, depending on which constructor is used.
 *
 * <p>
 * The head of this queue is the <em>least</em> element with respect to the
 * specified ordering. If multiple elements are tied for least value, the head
 * is one of those elements -- ties are broken arbitrarily.
 *
 * <p>
 * This implementation provides O(1) time for the enqueuing and dequeuing
 * operations ({@code offer}, {@code poll}, {@code peek}). However, iteration
 * over the queue requires O(n) time, as the elements are not stored
 * contiguously in memory.
 *
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong> Multiple
 * threads should not access a {@code DoublyLinkedPriorityQueue} instance
 * concurrently if any of the threads modifies the queue. Instead, use the
 * thread-safe {@code PriorityBlockingQueue} class.
 *
 * @param <E> the type of elements held in this queue
 *
 * @author Mark Bednarczyk
 */
public class DoublyLinkedPriorityQueue<E extends DoublyLinkedElement<E>>
		extends AbstractQueue<E>
		implements java.io.Serializable {

	private class Itr implements Iterator<E> {
		private E current = head;
		private E lastReturned = null;
		private int expectedModCount = modCount;

		final void checkForComodification() {
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
		}

		@Override
		public boolean hasNext() {
			return current != null;
		}

		@Override
		public E next() {
			checkForComodification();
			if (!hasNext())
				throw new NoSuchElementException();
			lastReturned = current;
			current = current.nextElement();
			return lastReturned;
		}

		@Override
		public void remove() {
			if (lastReturned == null)
				throw new IllegalStateException();
			checkForComodification();

			DoublyLinkedPriorityQueue.this.remove(lastReturned);
			lastReturned = null;
			expectedModCount = modCount;
		}
	}

	/**
	 * A customized Spliterator for DoublyLinkedPriorityQueue.
	 */
	private class PQSpliterator implements Spliterator<E> {
		private E current;
		private int index;
		private int fence;
		private int expectedModCount;

		/**
		 * Creates a new spliterator covering the given range of the queue.
		 *
		 * @param current          the current element to start from
		 * @param index            the current index
		 * @param fence            the end index (exclusive)
		 * @param expectedModCount the expected modification count of the queue
		 */
		PQSpliterator(E current, int index, int fence, int expectedModCount) {
			this.current = current;
			this.index = index;
			this.fence = fence;
			this.expectedModCount = expectedModCount;
		}

		/**
		 * Returns a set of characteristics of this Spliterator and its elements.
		 *
		 * @return the characteristics
		 */
		@Override
		public int characteristics() {
			return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
		}

		/**
		 * Returns an estimate of the number of elements that would be encountered by a
		 * forEachRemaining traversal.
		 *
		 * @return the estimated size
		 */
		@Override
		public long estimateSize() {
			return fence - index;
		}

		/**
		 * Processes all remaining elements using the given action.
		 *
		 * @param action the action to perform on each element
		 */
		@Override
		public void forEachRemaining(Consumer<? super E> action) {
			Objects.requireNonNull(action);
			int hi = fence;
			E e = current;
			int i = index;

			// Check for concurrent modification at the beginning
			if (modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}

			while (i < hi && e != null) {
				action.accept(e);
				e = e.nextElement();
				i++;
			}

			// Update state
			current = e;
			index = i;

			// Final check for concurrent modification
			if (modCount != expectedModCount) {
				throw new ConcurrentModificationException();
			}
		}

		/**
		 * Attempts to advance and process the next element.
		 *
		 * @param action the action to perform on the element
		 * @return true if an element was processed, false if no more elements
		 */
		@Override
		public boolean tryAdvance(Consumer<? super E> action) {
			Objects.requireNonNull(action);
			if (index < fence && current != null) {
				action.accept(current);
				current = current.nextElement();
				index++;
				return true;
			}

			return false;
		}

		/**
		 * Attempts to split this spliterator into two spliterators.
		 *
		 * @return a new spliterator covering the first half of the range, or null if
		 *         this spliterator can't be split
		 */
		@Override
		public Spliterator<E> trySplit() {
			int lo = index, mid = (lo + fence) >>> 1;
			if (lo >= mid) {
				return null; // Too small to split
			}

			return new PQSpliterator(current, lo, index = mid, expectedModCount);
		}
	}

	private static final long serialVersionUID = -7720805057305804111L;
	private E head;
	private E tail;
	private int size;

	private final Comparator<? super E> comparator;

	private transient int modCount;

	/**
	 * Creates a {@code DoublyLinkedPriorityQueue} with the default initial capacity
	 * and natural ordering.
	 */
	public DoublyLinkedPriorityQueue() {
		this((Comparator<? super E>) null);
	}

	/**
	 * Creates a {@code DoublyLinkedPriorityQueue} containing the elements in the
	 * specified collection.
	 *
	 * @param c the collection whose elements are to be placed into this priority
	 *          queue
	 * @throws ClassCastException   if elements of the specified collection cannot
	 *                              be compared to one another according to the
	 *                              priority queue's ordering
	 * @throws NullPointerException if the specified collection or any of its
	 *                              elements are null
	 */
	public DoublyLinkedPriorityQueue(Collection<? extends E> c) {
		this();
		addAll(c);
	}

	/**
	 * Creates a {@code DoublyLinkedPriorityQueue} with the specified comparator.
	 *
	 * @param comparator the comparator that will be used to order this priority
	 *                   queue. If {@code null}, the natural ordering of the
	 *                   elements will be used.
	 */
	public DoublyLinkedPriorityQueue(Comparator<? super E> comparator) {
		this.comparator = comparator;
	}

	/**
	 * Removes all of the elements from this priority queue. The queue will be empty
	 * after this call returns.
	 */
	@Override
	public void clear() {
		modCount++;
		for (E e = head; e != null;) {
			E next = e.nextElement();
			e.nextElement(null);
			e.prevElement(null);
			e = next;
		}

		head = tail = null;
		size = 0;
	}

	/**
	 * Returns the comparator used to order the elements in this queue, or
	 * {@code null} if this queue uses the natural ordering of its elements.
	 *
	 * @return the comparator used to order the elements in this queue, or
	 *         {@code null} if this queue uses the natural ordering of its elements
	 */
	public Comparator<? super E> comparator() {
		return comparator;
	}

	/**
	 * Compares two elements using the queue's comparator or natural ordering.
	 *
	 * @param e1 the first element to compare
	 * @param e2 the second element to compare
	 * @return a negative integer, zero, or a positive integer as the first argument
	 *         is less than, equal to, or greater than the second.
	 */
	@SuppressWarnings("unchecked")
	private int compare(E e1, E e2) {
		if (comparator != null) {
			return comparator.compare(e1, e2);
		} else {
			return ((Comparable<? super E>) e1).compareTo(e2);
		}
	}

	/**
	 * Returns {@code true} if this queue contains the specified element.
	 *
	 * @param o object to be checked for containment in this queue
	 * @return {@code true} if this queue contains the specified element
	 */
	@Override
	public boolean contains(Object o) {
		if (o == null)
			return false;

		for (E e = head; e != null; e = e.nextElement()) {
			if (o.equals(e))
				return true;
		}

		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object e : c) {
			if (!contains(e)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Returns an iterator over the elements in this queue. The iterator does not
	 * return the elements in any particular order.
	 *
	 * @return an iterator over the elements in this queue
	 */
	@Override
	public Iterator<E> iterator() {
		return new Itr();
	}

	/**
	 * Inserts the specified element into this priority queue.
	 *
	 * @param e the element to add
	 * @return {@code true} if the element was added to the queue, {@code false} if
	 *         it was already present
	 * @throws NullPointerException if the specified element is null
	 */
	@Override
	public boolean offer(E e) {
		if (e == null)
			throw new NullPointerException();

		modCount++;
		size++;

		if (head == null) {
			head = tail = e;
			e.nextElement(null);
			e.prevElement(null);
		} else if (compare(e, head) <= 0) {
			e.nextElement(head);
			e.prevElement(null);
			head.prevElement(e);
			head = e;
		} else {
			E current = head;
			while (current.nextElement() != null && compare(e, current.nextElement()) > 0) {
				current = current.nextElement();
			}
			e.nextElement(current.nextElement());
			e.prevElement(current);
			if (current.nextElement() != null) {
				current.nextElement().prevElement(e);
			} else {
				tail = e;
			}
			current.nextElement(e);
		}

		return true;
	}

	/**
	 * Retrieves, but does not remove, the head of this queue, or returns
	 * {@code null} if this queue is empty.
	 *
	 * @return the head of this queue, or {@code null} if this queue is empty
	 */
	@Override
	public E peek() {
		return head;
	}

	/**
	 * Retrieves and removes the head of this queue, or returns {@code null} if this
	 * queue is empty.
	 *
	 * @return the head of this queue, or {@code null} if this queue is empty
	 */
	@Override
	public E poll() {
		if (head == null)
			return null;

		modCount++;
		size--;
		E result = head;
		head = head.nextElement();
		if (head == null) {
			tail = null;
		} else {
			head.prevElement(null);
		}

		result.nextElement(null);
		result.prevElement(null);

		return result;
	}

	/**
	 * Removes a single instance of the specified element from this queue, if it is
	 * present.
	 *
	 * @param o element to be removed from this queue, if present
	 * @return {@code true} if this queue changed as a result of the call
	 */
	@Override
	public boolean remove(Object o) {
		for (E e = head; e != null; e = e.nextElement()) {
			if (o.equals(e)) {
				unlink(e);
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean modified = false;
		Iterator<E> it = iterator();

		while (it.hasNext()) {
			if (c.contains(it.next())) {
				it.remove();
				modified = true;
			}
		}

		return modified;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean modified = false;
		Iterator<E> it = iterator();

		while (it.hasNext()) {
			if (!c.contains(it.next())) {
				it.remove();
				modified = true;
			}
		}

		return modified;
	}

	/**
	 * Returns the number of elements in this queue.
	 *
	 * @return the number of elements in this queue
	 */
	@Override
	public int size() {
		return size;
	}

	/**
	 * Creates a {@code Spliterator} over the elements in this queue.
	 *
	 * @return a {@code Spliterator} over the elements in this queue
	 */
	@Override
	public Spliterator<E> spliterator() {
		return new PQSpliterator(head, 0, size, modCount);
	}

	/**
	 * Removes the specified element from the queue.
	 *
	 * @param e the element to be removed
	 */
	private void unlink(E e) {
		if (e.prevElement() == null) {
			head = e.nextElement();
		} else {
			e.prevElement().nextElement(e.nextElement());
		}

		if (e.nextElement() == null) {
			tail = e.prevElement();
		} else {
			e.nextElement().prevElement(e.prevElement());
		}

		e.prevElement(null);
		e.nextElement(null);

		size--;
		modCount++;
	}
}
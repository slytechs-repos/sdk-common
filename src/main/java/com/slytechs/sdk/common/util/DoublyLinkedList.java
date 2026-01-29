/*
 * Copyright 2005-2026 Sly Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slytechs.sdk.common.util;

import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * A doubly-linked list implementation of the {@link List} and {@link Deque}
 * interfaces. This implementation allows for constant-time insertions,
 * removals, and access to the first and last elements of the list.
 * 
 * <p>
 * This class provides a unique approach where list elements directly implement
 * the {@link DoublyLinkedElement} interface, eliminating the need for separate
 * container nodes. This design can lead to improved memory efficiency in
 * certain scenarios.
 * </p>
 *
 * <p>
 * Key features of this implementation include:
 * </p>
 * <ul>
 * <li>O(1) time complexity for operations on the ends of the list (e.g.,
 * {@link #addFirst}, {@link #addLast}, {@link #removeFirst},
 * {@link #removeLast})</li>
 * <li>O(n) time complexity for indexed operations in the middle of the list
 * (e.g., {@link #get}, {@link #set}, {@link #add(int, Object)},
 * {@link #remove(int)})</li>
 * <li>Implementation of both {@link List} and {@link Deque} interfaces,
 * providing a wide range of operations</li>
 * <li>SettingsSupport for null elements</li>
 * <li>Fail-fast iterators which throw {@link ConcurrentModificationException}
 * if the list is structurally modified during iteration</li>
 * </ul>
 *
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong> If
 * multiple threads access a linked list concurrently, and at least one of the
 * threads modifies the list structurally, it must be synchronized externally.
 * </p>
 *
 * <p>
 * The iterators returned by this class's {@link #iterator} and
 * {@link #listIterator} methods are <i>fail-fast</i>: if the list is
 * structurally modified at any time after the iterator is created, in any way
 * except through the iterator's own {@link Iterator#remove} or
 * {@link ListIterator#add} methods, the iterator will throw a
 * {@link ConcurrentModificationException}.
 * </p>
 *
 * <p>
 * This class is a member of the Java Collections Framework.
 * </p>
 *
 * @param <E> the type of elements held in this collection, must extend
 *            {@link DoublyLinkedElement}
 *
 * @author Mark Bednarczyk
 * @see List
 * @see Deque
 * @see ArrayList
 * @see DoublyLinkedElement
 */
public class DoublyLinkedList<E extends DoublyLinkedElement<E>> extends AbstractSequentialList<E>
		implements List<E>, Deque<E>, Cloneable, java.io.Serializable {

	private class DescendingIterator implements Iterator<E> {
		private final ListItr itr = new ListItr(size());

		@Override
		public boolean hasNext() {
			return itr.hasPrevious();
		}

		@Override
		public E next() {
			return itr.previous();
		}

		@Override
		public void remove() {
			itr.remove();
		}
	}

	private class ListItr implements ListIterator<E> {
		private E lastReturned;
		private E next;
		private int nextIndex;
		private int expectedModCount = modCount;

		ListItr(int index) {
			next = (index == size) ? null : node(index);
			nextIndex = index;
		}

		@Override
		public void add(E e) {
			checkForComodification();
			lastReturned = null;
			if (next == null)
				linkLast(e);
			else
				linkBefore(e, next);
			nextIndex++;
			expectedModCount++;
		}

		final void checkForComodification() {
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
		}

		@Override
		public boolean hasNext() {
			return nextIndex < size;
		}

		@Override
		public boolean hasPrevious() {
			return nextIndex > 0;
		}

		@Override
		public E next() {
			checkForComodification();
			if (!hasNext())
				throw new NoSuchElementException();

			lastReturned = next;
			next = next.nextElement();
			nextIndex++;
			return lastReturned;
		}

		@Override
		public int nextIndex() {
			return nextIndex;
		}

		@Override
		public E previous() {
			checkForComodification();
			if (!hasPrevious())
				throw new NoSuchElementException();

			lastReturned = next = (next == null) ? last : next.prevElement();
			nextIndex--;
			return lastReturned;
		}

		@Override
		public int previousIndex() {
			return nextIndex - 1;
		}

		@Override
		public void remove() {
			checkForComodification();
			if (lastReturned == null)
				throw new IllegalStateException();

			E lastNext = lastReturned.nextElement();
			unlink(lastReturned);
			if (next == lastReturned)
				next = lastNext;
			else
				nextIndex--;
			lastReturned = null;
			expectedModCount++;
		}

		@Override
		public void set(E e) {
			if (lastReturned == null)
				throw new IllegalStateException();
			checkForComodification();
			DoublyLinkedList.this.set(nextIndex - 1, e);
			lastReturned = e;
		}
	}

	/** A customized variant of Spliterators.IteratorSpliterator */
	static final class LLSpliterator<E extends DoublyLinkedElement<E>> implements Spliterator<E> {
		static final int BATCH_UNIT = 1 << 10; // batch array size increment
		static final int MAX_BATCH = 1 << 25; // max batch array size;
		final DoublyLinkedList<E> list;
		E current; // current node; null until initialized
		int est; // size estimate; -1 until first needed
		int expectedModCount; // initialized when est set
		int batch; // batch size for splits

		LLSpliterator(DoublyLinkedList<E> list, int est, int expectedModCount) {
			this.list = list;
			this.est = est;
			this.expectedModCount = expectedModCount;
		}

		@Override
		public int characteristics() {
			return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
		}

		@Override
		public long estimateSize() {
			return getEst();
		}

		@Override
		public void forEachRemaining(Consumer<? super E> action) {
			E p;
			int n;
			if (action == null)
				throw new NullPointerException();
			if ((n = getEst()) > 0 && (p = current) != null) {
				current = null;
				est = 0;
				do {
					E e = p;
					p = p.nextElement();
					action.accept(e);
				} while (p != null && --n > 0);
			}
			if (list.modCount != expectedModCount)
				throw new ConcurrentModificationException();
		}

		final int getEst() {
			int s; // force initialization
			final DoublyLinkedList<E> lst;
			if ((s = est) < 0) {
				if ((lst = list) == null)
					s = est = 0;
				else {
					expectedModCount = lst.modCount;
					current = lst.first;
					s = est = lst.size;
				}
			}
			return s;
		}

		@Override
		public boolean tryAdvance(Consumer<? super E> action) {
			E p;
			if (action == null)
				throw new NullPointerException();
			if (getEst() > 0 && (p = current) != null) {
				--est;
				E e = p;
				current = p.nextElement();
				action.accept(e);
				if (list.modCount != expectedModCount)
					throw new ConcurrentModificationException();
				return true;
			}
			return false;
		}

		@Override
		public Spliterator<E> trySplit() {
			E p;
			int s = getEst();
			if (s > 1 && (p = current) != null) {
				int n = batch + BATCH_UNIT;
				if (n > s)
					n = s;
				if (n > MAX_BATCH)
					n = MAX_BATCH;
				Object[] a = new Object[n];
				int j = 0;
				do {
					a[j++] = p;
				} while ((p = p.nextElement()) != null && j < n);
				current = p;
				batch = j;
				est = s - j;
				return Spliterators.spliterator(a, 0, j, Spliterator.ORDERED);
			}
			return null;
		}
	}

	// This inner class provides a reverse view of the DoublyLinkedList
	private static class ReverseOrderDoublyLinkedListView<E extends DoublyLinkedElement<E>> extends
			DoublyLinkedList<E> {
		private final DoublyLinkedList<E> originalList;

		ReverseOrderDoublyLinkedListView(DoublyLinkedList<E> list) {
			this.originalList = list;
		}

		@Override
		public boolean add(E e) {
			originalList.addFirst(e);
			return true;
		}

		@Override
		public void addFirst(E e) {
			originalList.addLast(e);
		}

		@Override
		public void addLast(E e) {
			originalList.addFirst(e);
		}

		@Override
		public Iterator<E> descendingIterator() {
			return originalList.iterator();
		}

		@Override
		public E get(int index) {
			return originalList.get(size() - 1 - index);
		}

		@Override
		public E getFirst() {
			return originalList.getLast();
		}

		@Override
		public E getLast() {
			return originalList.getFirst();
		}

		@Override
		public Iterator<E> iterator() {
			return originalList.descendingIterator();
		}

		@Override
		public E removeFirst() {
			return originalList.removeLast();
		}

		@Override
		public E removeLast() {
			return originalList.removeFirst();
		}

		@Override
		public DoublyLinkedList<E> reversed() {
			return originalList; // Reversing a reverse view gives the original list
		}

		@Override
		public int size() {
			return originalList.size();
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("[");
			Iterator<E> it = iterator();
			while (it.hasNext()) {
				sb.append(it.next());
				/**
				 * Returns an array containing all of the elements in this list in proper
				 * sequence (from first to last element); the runtime type of the returned array
				 * is that of the specified array.
				 *
				 * @param a the array into which the elements of the list are to be stored, if
				 *          it is big enough; otherwise, a new array of the same runtime type is
				 *          allocated for this purpose.
				 * @return an array containing the elements of the list
				 * @throws ArrayStoreException  if the runtime type of the specified array is
				 *                              not a supertype of the runtime type of every
				 *                              element in this list
				 * @throws NullPointerException if the specified array is null
				 */

				if (it.hasNext()) {
					sb.append(", ");
				}
			}
			sb.append("]");
			return sb.toString();
		}
	}

	private static final long serialVersionUID = 876323262645176354L;

	private transient int size = 0;

	private transient E first;

	private transient E last;

	/**
	 * Constructs an empty list.
	 */
	public DoublyLinkedList() {
	}

	/**
	 * Returns an array containing all of the elements in this list in proper
	 * sequence (from first to last element); the runtime type of the returned array
	 * is that of the specified array.
	 *
	 * @param a the array into which the elements of the list are to be stored, if
	 *          it is big enough; otherwise, a new array of the same runtime type is
	 *          allocated for this purpose.
	 * @return an array containing the elements of the list
	 * @throws ArrayStoreException  if the runtime type of the specified array is
	 *                              not a supertype of the runtime type of every
	 *                              element in this list
	 * @throws NullPointerException if the specified array is null
	 */

	/**
	 * Constructs a list containing the elements of the specified collection, in the
	 * order they are returned by the collection's iterator.
	 *
	 * @param c the collection whose elements are to be placed into this list
	 * @throws NullPointerException if the specified collection is null
	 */
	public DoublyLinkedList(Collection<? extends E> c) {
		this();
		addAll(c);
	}

	@Override
	/**
	 * Appends the specified element to the end of this list.
	 *
	 * @param e element to be appended to this list
	 * @return {@code true} (as specified by {@link Collection#add})
	 */
	public boolean add(E e) {
		linkLast(e);
		return true;
	}

	@Override
	/**
	 * Inserts the specified element at the specified position in this list. Shifts
	 * the element currently at that position (if any) and any subsequent elements
	 * to the right (adds one to their indices).
	 *
	 * @param index   index at which the specified element is to be inserted
	 * @param element element to be inserted
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *                                   ({@code index < 0 || index > size()})
	 */
	public void add(int index, E element) {
		checkPositionIndex(index);

		if (index == size)
			linkLast(element);
		else
			linkBefore(element, node(index));
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		return addAll(size, c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		checkPositionIndex(index);

		Object[] a = c.toArray();
		int numNew = a.length;
		if (numNew == 0)
			return false;

		E pred, succ;
		if (index == size) {
			succ = null;
			pred = last;
		} else {
			succ = node(index);
			pred = succ.prevElement();
		}

		for (Object o : a) {
			@SuppressWarnings("unchecked")
			E e = (E) o;
			if (pred == null)
				first = e;
			else {
				pred.nextElement(e);
				e.prevElement(pred);
			}
			pred = e;
		}

		if (succ == null) {
			last = pred;
		} else {
			pred.nextElement(succ);
			succ.prevElement(pred);
		}

		size += numNew;
		modCount++;
		return true;
	}

	@Override
	/**
	 * Inserts the specified element at the beginning of this list.
	 *
	 * @param e the element to add
	 */
	public void addFirst(E e) {
		linkFirst(e);
	}

	@Override
	/**
	 * Appends the specified element to the end of this list.
	 *
	 * @param e the element to add
	 */
	public void addLast(E e) {
		linkLast(e);
	}

	private void checkElementIndex(int index) {
		if (!isElementIndex(index))
			throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
	}

	private void checkPositionIndex(int index) {
		if (!isPositionIndex(index))
			throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
	}

	@Override
	/**
	 * Removes all of the elements from this list. The list will be empty after this
	 * call returns.
	 */
	public void clear() {
		for (E x = first; x != null;) {
			E next = x.nextElement();
			x.nextElement(null);
			x.prevElement(null);
			x = next;
		}
		first = last = null;
		size = 0;
		modCount++;
	}

	@Override
	/**
	 * Returns a shallow copy of this {@code DoublyLinkedList}. (The elements
	 * themselves are not cloned.)
	 *
	 * @return a shallow copy of this {@code DoublyLinkedList} instance
	 */
	public Object clone() {
		DoublyLinkedList<E> clone = superClone();

		clone.first = clone.last = null;
		clone.size = 0;
		clone.modCount = 0;

		for (E x = first; x != null; x = x.nextElement())
			clone.add(x); // Assuming E implements Cloneable

		return clone;
	}

	@Override
	/**
	 * Returns {@code true} if this list contains the specified element.
	 *
	 * @param o element whose presence in this list is to be tested
	 * @return {@code true} if this list contains the specified element
	 */
	public boolean contains(Object o) {
		return indexOf(o) >= 0;
	}

	@Override
	public Iterator<E> descendingIterator() {
		return new DescendingIterator();
	}

	@Override
	public E element() {
		return getFirst();
	}

	@Override
	/**
	 * Returns the element at the specified position in this list.
	 *
	 * @param index index of the element to return
	 * @return the element at the specified position in this list
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *                                   ({@code index < 0 || index >= size()})
	 */
	public E get(int index) {
		checkElementIndex(index);
		return node(index);
	}

	@Override
	/**
	 * Returns the first element in this list.
	 *
	 * @return the first element in this list
	 * @throws NoSuchElementException if this list is empty
	 */
	public E getFirst() {
		if (first == null)
			throw new NoSuchElementException();
		return first;
	}

	@Override
	/**
	 * Returns the last element in this list.
	 *
	 * @return the last element in this list
	 * @throws NoSuchElementException if this list is empty
	 */
	public E getLast() {
		if (last == null)
			throw new NoSuchElementException();
		return last;
	}

	@Override
	public int indexOf(Object o) {
		int index = 0;
		if (o instanceof DoublyLinkedElement) {
			for (E x = first; x != null; x = x.nextElement()) {
				if (o.equals(x))
					return index;
				index++;
			}
		}
		return -1;
	}

	private boolean isElementIndex(int index) {
		return index >= 0 && index < size;
	}

	private boolean isPositionIndex(int index) {
		return index >= 0 && index <= size;
	}

	@Override
	public int lastIndexOf(Object o) {
		int index = size;
		if (o instanceof DoublyLinkedElement) {
			for (E x = last; x != null; x = x.prevElement()) {
				index--;
				if (o.equals(x))
					return index;
			}
		}
		return -1;
	}

	void linkBefore(E e, E succ) {
		final E pred = succ.prevElement();
		e.prevElement(pred);
		e.nextElement(succ);
		succ.prevElement(e);
		if (pred == null)
			first = e;
		else
			pred.nextElement(e);
		size++;
		modCount++;
	}

	private void linkFirst(E e) {
		final E f = first;
		first = e;
		if (f == null)
			last = e;
		else {
			e.nextElement(f);
			f.prevElement(e);
		}
		size++;
		modCount++;
	}

	void linkLast(E e) {
		final E l = last;
		last = e;
		if (l == null)
			first = e;
		else {
			e.prevElement(l);
			l.nextElement(e);
		}
		size++;
		modCount++;
	}

	@Override
	/**
	 * Returns a list-iterator of the elements in this list (in proper sequence),
	 * starting at the specified position in the list.
	 *
	 * @param index index of the first element to be returned from the list-iterator
	 *              (by a call to {@code next})
	 * @return a ListIterator of the elements in this list (in proper sequence),
	 *         starting at the specified position in the list
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *                                   ({@code index < 0 || index > size()})
	 */
	public ListIterator<E> listIterator(int index) {
		checkPositionIndex(index);
		return new ListItr(index);
	}

	E node(int index) {
		if (index < (size >> 1)) {
			E x = first;
			for (int i = 0; i < index; i++)
				x = x.nextElement();
			return x;
		} else {
			E x = last;
			for (int i = size - 1; i > index; i--)
				x = x.prevElement();
			return x;
		}
	}

	@Override
	public boolean offer(E e) {
		return add(e);
	}

	@Override
	public boolean offerFirst(E e) {
		addFirst(e);
		return true;
	}

	@Override
	public boolean offerLast(E e) {
		addLast(e);
		return true;
	}

	private String outOfBoundsMsg(int index) {
		return "Index: " + index + ", Size: " + size;
	}

	@Override
	public E peek() {
		return first;
	}

	@Override
	public E peekFirst() {
		return first;
	}

	@Override
	public E peekLast() {
		return last;
	}

	@Override
	public E poll() {
		return (first == null) ? null : unlinkFirst(first);
	}

	@Override
	public E pollFirst() {
		return (first == null) ? null : unlinkFirst(first);
	}

	@Override
	public E pollLast() {
		return (last == null) ? null : unlinkLast(last);
	}

	@Override
	public E pop() {
		return removeFirst();
	}

	@Override
	public void push(E e) {
		addFirst(e);
	}

	@SuppressWarnings("unchecked")
	private void readObject(java.io.ObjectInputStream s)
			throws java.io.IOException, ClassNotFoundException {
		s.defaultReadObject();
		int size = s.readInt();
		for (int i = 0; i < size; i++)
			linkLast((E) s.readObject());
	}

	@Override
	public E remove() {
		return removeFirst();
	}

	@Override
	/**
	 * Removes the element at the specified position in this list. Shifts any
	 * subsequent elements to the left (subtracts one from their indices). Returns
	 * the element that was removed from the list.
	 *
	 * @param index the index of the element to be removed
	 * @return the element previously at the specified position
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *                                   ({@code index < 0 || index >= size()})
	 */
	public E remove(int index) {
		checkElementIndex(index);
		return unlink(node(index));
	}

	@Override
	/**
	 * Removes the first occurrence of the specified element from this list, if it
	 * is present.
	 *
	 * @param o element to be removed from this list, if present
	 * @return {@code true} if this list contained the specified element
	 */
	public boolean remove(Object o) {
		if (o instanceof DoublyLinkedElement) {
			for (E x = first; x != null; x = x.nextElement()) {
				if (o.equals(x)) {
					unlink(x);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	/**
	 * Removes and returns the first element from this list.
	 *
	 * @return the first element from this list
	 * @throws NoSuchElementException if this list is empty
	 */
	public E removeFirst() {
		if (first == null)
			throw new NoSuchElementException();
		return unlinkFirst(first);
	}

	@Override
	public boolean removeFirstOccurrence(Object o) {
		return remove(o);
	}

	@Override
	/**
	 * Removes and returns the last element from this list.
	 *
	 * @return the last element from this list
	 * @throws NoSuchElementException if this list is empty
	 */
	public E removeLast() {
		if (last == null)
			throw new NoSuchElementException();
		return unlinkLast(last);
	}

	@Override
	public boolean removeLastOccurrence(Object o) {
		if (o instanceof DoublyLinkedElement) {
			for (E x = last; x != null; x = x.prevElement()) {
				if (o.equals(x)) {
					unlink(x);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public DoublyLinkedList<E> reversed() {
		return new ReverseOrderDoublyLinkedListView<>(this);
	}

	@Override
	/**
	 * Replaces the element at the specified position in this list with the
	 * specified element.
	 *
	 * @param index   index of the element to replace
	 * @param element element to be stored at the specified position
	 * @return the element previously at the specified position
	 * @throws IndexOutOfBoundsException if the index is out of range
	 *                                   ({@code index < 0 || index >= size()})
	 */
	public E set(int index, E element) {
		checkElementIndex(index);
		E x = node(index);
		E oldVal = x;
		if (index == 0)
			first = element;
		if (index == size - 1)
			last = element;
		if (x.prevElement() != null)
			x.prevElement().nextElement(element);
		if (x.nextElement() != null)
			x.nextElement().prevElement(element);
		element.prevElement(x.prevElement());
		element.nextElement(x.nextElement());
		return oldVal;
	}

	@Override
	/**
	 * Returns the number of elements in this list.
	 *
	 * @return the number of elements in this list
	 */
	public int size() {
		return size;
	}

	@Override
	public Spliterator<E> spliterator() {
		return new LLSpliterator<>(this, -1, 0);
	}

	@SuppressWarnings("unchecked")
	private DoublyLinkedList<E> superClone() {
		try {
			return (DoublyLinkedList<E>) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e);
		}
	}

	@Override
	/**
	 * Returns an array containing all of the elements in this list in proper
	 * sequence (from first to last element).
	 *
	 * @return an array containing all of the elements in this list in proper
	 *         sequence
	 */
	public Object[] toArray() {
		Object[] result = new Object[size];
		int i = 0;
		for (E x = first; x != null; x = x.nextElement())
			result[i++] = x;
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	/**
	 * Returns an array containing all of the elements in this list in proper
	 * sequence (from first to last element); the runtime type of the returned array
	 * is that of the specified array.
	 *
	 * @param a the array into which the elements of the list are to be stored, if
	 *          it is big enough; otherwise, a new array of the same runtime type is
	 *          allocated for this purpose.
	 * @return an array containing the elements of the list
	 * @throws ArrayStoreException  if the runtime type of the specified array is
	 *                              not a supertype of the runtime type of every
	 *                              element in this list
	 * @throws NullPointerException if the specified array is null
	 */
	public <T> T[] toArray(T[] a) {
		if (a.length < size)
			a = (T[]) java.lang.reflect.Array.newInstance(
					a.getClass().getComponentType(), size);
		int i = 0;
		Object[] result = a;
		for (E x = first; x != null; x = x.nextElement())
			result[i++] = x;

		if (a.length > size)
			a[size] = null;

		return a;
	}

	E unlink(E x) {
		final E next = x.nextElement();
		final E prev = x.prevElement();

		if (prev == null) {
			first = next;
		} else {
			prev.nextElement(next);
			x.prevElement(null);
		}

		if (next == null) {
			last = prev;
		} else {
			next.prevElement(prev);
			x.nextElement(null);
		}

		size--;
		modCount++;
		return x;
	}

	private E unlinkFirst(E f) {
		final E next = f.nextElement();
		f.nextElement(null);
		first = next;
		if (next == null)
			last = null;
		else
			next.prevElement(null);
		size--;
		modCount++;
		return f;
	}

	private E unlinkLast(E l) {
		final E prev = l.prevElement();
		l.prevElement(null);
		last = prev;
		if (prev == null)
			first = null;
		else
			prev.nextElement(null);
		size--;
		modCount++;
		return l;
	}

	private void writeObject(java.io.ObjectOutputStream s)
			throws java.io.IOException {
		s.defaultWriteObject();
		s.writeInt(size);
		for (E x = first; x != null; x = x.nextElement())
			s.writeObject(x);
	}
}
/*
 * Sly Technologies Free License
 * 
 * Copyright 2025 Sly Technologies Inc.
 *
 * Licensed under the Sly Technologies Free License (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.slytechs.com/free-license-text
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.slytechs.sdk.common.util.collection;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * The manager of a singly linked list that supports operations for traversal,
 * insertion, deletion, searching, updating, reversal, and index-based access.
 * The list can use either the element's {@code nextLink} and
 * {@code setNextLink} methods or custom getter and setter functions for
 * managing next references.
 *
 * @param <T> the type of elements in the list
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public final class LinkableList<T> {

	private final UnaryOperator<T> getNext;
	private final BiConsumer<T, T> setNext;
	private T head;

	/**
	 * Constructs an empty {@code LinkableList} with custom getter and setter
	 * functions for managing next references.
	 *
	 * @param getNext the function to get the next element
	 * @param setNext the function to set the next element
	 */
	public LinkableList(UnaryOperator<T> getNext, BiConsumer<T, T> setNext) {
		this.getNext = getNext;
		this.setNext = setNext;
	}

	/**
	 * Constructs a {@code LinkableList} with the specified head element and custom
	 * getter and setter functions for managing next references.
	 *
	 * @param head    the head element of the list
	 * @param getNext the function to get the next element
	 * @param setNext the function to set the next element
	 */
	public LinkableList(T head, UnaryOperator<T> getNext, BiConsumer<T, T> setNext) {
		this(getNext, setNext);
		this.head = head;
	}

	/**
	 * Adds an element at the head of the list.
	 *
	 * @param element the element to add
	 * @return the added element
	 */
	public T addFirst(T element) {
		setNext.accept(element, head);
		head = element;
		return element;
	}

	/**
	 * Adds an element at the specified index.
	 *
	 * @param index   the index at which to add the element
	 * @param element the element to add
	 * @throws IndexOutOfBoundsException if the index is negative or beyond the list
	 *                                   size
	 */
	public void add(int index, T element) {
		if (index < 0) {
			throw new IndexOutOfBoundsException("Index: " + index);
		}
		if (index == 0) {
			addFirst(element);
			return;
		}
		T current = head;
		T prev = null;
		int i = 0;
		while (current != null && i < index) {
			prev = current;
			current = getNext.apply(current);
			i++;
		}
		if (i != index) {
			throw new IndexOutOfBoundsException("Index: " + index);
		}
		setNext.accept(element, current);
		if (prev == null) {
			head = element;
		} else {
			setNext.accept(prev, element);
		}
	}

	/**
	 * Retrieves the element at the specified index.
	 *
	 * @param index the index of the element to retrieve
	 * @return the element at the specified index
	 * @throws IndexOutOfBoundsException if the index is negative or beyond the list
	 *                                   size
	 */
	public T get(int index) {
		if (index < 0) {
			throw new IndexOutOfBoundsException("Index: " + index);
		}
		T current = head;
		int i = 0;
		while (current != null && i < index) {
			current = getNext.apply(current);
			i++;
		}
		if (current == null) {
			throw new IndexOutOfBoundsException("Index: " + index);
		}
		return current;
	}

	/**
	 * Replaces the element at the specified index with a new element.
	 *
	 * @param index   the index of the element to replace
	 * @param element the new element
	 * @return the element previously at the specified index
	 * @throws IndexOutOfBoundsException if the index is negative or beyond the list
	 *                                   size
	 */
	public T set(int index, T element) {
		if (index < 0) {
			throw new IndexOutOfBoundsException("Index: " + index);
		}
		T current = head;
		T prev = null;
		int i = 0;
		while (current != null && i < index) {
			prev = current;
			current = getNext.apply(current);
			i++;
		}
		if (current == null) {
			throw new IndexOutOfBoundsException("Index: " + index);
		}
		setNext.accept(element, getNext.apply(current));
		if (prev == null) {
			head = element;
		} else {
			setNext.accept(prev, element);
		}
		return current;
	}

	/**
	 * Removes and returns the element at the specified index.
	 *
	 * @param index the index of the element to remove
	 * @return the removed element
	 * @throws IndexOutOfBoundsException if the index is negative or beyond the list
	 *                                   size
	 */
	public T remove(int index) {
		if (index < 0) {
			throw new IndexOutOfBoundsException("Index: " + index);
		}
		T current = head;
		T prev = null;
		int i = 0;
		while (current != null && i < index) {
			prev = current;
			current = getNext.apply(current);
			i++;
		}
		if (current == null) {
			throw new IndexOutOfBoundsException("Index: " + index);
		}
		if (prev == null) {
			head = getNext.apply(current);
		} else {
			setNext.accept(prev, getNext.apply(current));
		}
		return current;
	}

	/**
	 * Returns the head element of the list.
	 *
	 * @return the head element, or null if the list is empty
	 */
	public T getHead() {
		return head;
	}

	/**
	 * Traverses the list, applying the specified action to each element.
	 *
	 * @param action the action to apply to each element
	 */
	public void traverse(Consumer<T> action) {
		T current = head;
		while (current != null) {
			action.accept(current);
			current = getNext.apply(current);
		}
	}

	/**
	 * Inserts an element after the first element matching the specified predicate.
	 *
	 * @param target  the element to insert
	 * @param element the element to match
	 * @param match   the predicate to identify the target element
	 * @return the inserted element, or null if no match is found
	 */
	public T insertAfter(T target, T element, Predicate<T> match) {
		T current = head;
		T prev = null;
		while (current != null && !match.test(current)) {
			prev = current;
			current = getNext.apply(current);
		}
		if (current == null) {
			return null; // Not found
		}
		setNext.accept(target, getNext.apply(current));
		if (prev == null) {
			head = target;
			setNext.accept(target, current);
		} else {
			setNext.accept(prev, target);
			setNext.accept(target, current);
		}
		return target;
	}

	/**
	 * Removes the element following the first element matching the specified
	 * predicate.
	 *
	 * @param target the element to match
	 * @param match  the predicate to identify the target element
	 * @return the removed element, or null if no match or no next element
	 */
	public T removeAfter(T target, Predicate<T> match) {
		T current = head;
		T prev = null;
		while (current != null && !match.test(current)) {
			prev = current;
			current = getNext.apply(current);
		}
		if (current == null || getNext.apply(current) == null) {
			return null; // Not found or no next node
		}
		T next = getNext.apply(current);
		if (prev == null) {
			head = getNext.apply(next);
		} else {
			setNext.accept(prev, getNext.apply(next));
		}
		return next;
	}

	/**
	 * Finds the first element matching the specified predicate.
	 *
	 * @param match the predicate to identify the element
	 * @return the matching element, or null if no match is found
	 */
	public T find(Predicate<T> match) {
		T current = head;
		while (current != null) {
			if (match.test(current)) {
				return current;
			}
			current = getNext.apply(current);
		}
		return null;
	}

	/**
	 * Updates elements matching the specified predicate with the result of the
	 * provided updater function.
	 *
	 * @param match   the predicate to identify elements to update
	 * @param updater the function to compute the new element
	 */
	public void update(Predicate<T> match, UnaryOperator<T> updater) {
		T current = head;
		T prev = null;
		while (current != null) {
			if (match.test(current)) {
				T updated = updater.apply(current);
				if (prev == null) {
					head = updated;
				} else {
					setNext.accept(prev, updated);
				}
				setNext.accept(updated, getNext.apply(current));
			}
			prev = current;
			current = getNext.apply(current);
		}
	}

	/**
	 * Reverses the order of the list.
	 */
	public void reverse() {
		T prev = null;
		T current = head;
		while (current != null) {
			T next = getNext.apply(current);
			setNext.accept(current, prev);
			prev = current;
			current = next;
		}
		head = prev;
	}
}
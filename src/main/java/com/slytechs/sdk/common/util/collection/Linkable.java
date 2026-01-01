/*
 * Sly Technologies Free License
 * 
 * Copyright 2024 Sly Technologies Inc.
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

import com.slytechs.sdk.common.memory.Memory;

/**
 * Interface for a singly linked list where elements directly manage their next
 * reference, either via {@code nextLink} and {@code setNextLink} methods or
 * through custom getter and setter functions. This design minimizes object
 * allocation by avoiding wrapper nodes, making it highly efficient for
 * head-based operations such as those used in memory pools or tag chains.
 *
 * @param <T> the type of elements in the list, which must implement this
 *            interface
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public interface Linkable<T extends Linkable<T>> {

	/**
	 * Creates a new {@code LinkableList} instance managing a singly linked list
	 * starting with the specified head element, using the element's
	 * {@code nextLink} and {@code setNextLink} methods for navigation.
	 *
	 * @param <T>  the type of elements in the list
	 * @param head the head element of the list
	 * @return a new {@code LinkableList} instance
	 */
	static <T extends Linkable<T>> LinkableList<T> of(T head) {
		return new LinkableList<>(head, Linkable::nextLink, Linkable::setNextLink);
	}

	/**
	 * Creates a new thread-safe {@code AtomicLinkableList} instance managing a
	 * singly linked list starting with the specified head element, using the
	 * element's {@code nextLink} and {@code setNextLink} methods for navigation.
	 *
	 * @param <T>  the type of elements in the list
	 * @param head the head element of the list
	 * @return a new {@code AtomicLinkableList} instance
	 */
	static <T extends Linkable<T>> AtomicLinkableList<T> ofAtomic(T head) {
		return new AtomicLinkableList<>(head, Linkable::nextLink, Linkable::setNextLink);
	}

	/**
	 * Example usage demonstrating how to create a {@code LinkableList} for a memory
	 * freeListPool.
	 */
	static void example() {
		LinkableList<Memory> freeList = new LinkableList<>(Memory::nextSegment, Memory::nextSegment);
	}

	/**
	 * Returns the next element in the list.
	 *
	 * @return the next element, or null if none
	 */
	T nextLink();

	/**
	 * Sets the next element in the list.
	 *
	 * @param node the element to set as the next
	 * @return the set element
	 */
	T setNextLink(T node);
}
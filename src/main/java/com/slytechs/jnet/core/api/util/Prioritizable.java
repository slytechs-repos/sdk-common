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
package com.slytechs.jnet.core.api.util;

import java.util.Comparator;

/**
 * Interface for objects that can be assigned and ordered by priority values.
 * Implementing classes can be prioritized in processing queues, scheduling
 * systems, or any scenario requiring relative importance ordering.
 * 
 * <p>
 * The priority system uses an integer-based scale where:
 * </p>
 * 
 * <ul>
 * <li>Lower numerical values indicate higher priority
 * <li>0 represents the highest possible priority ({@link #MIN_PRIORITY_VALUE})
 * <li>{@code Integer.MAX_VALUE - 1} is the lowest priority
 * ({@link #MAX_PRIORITY_VALUE})
 * <li>The default priority is 0 ({@link #DEFAULT_PRIORITY_VALUE})
 * </ul>
 * 
 * <p>
 * Example usage for sorting prioritizable objects:
 * </p>
 * 
 * <pre>
 * List&lt;Prioritizable&gt; items = ...;
 * // Sort from highest to lowest priority
 * items.sort((a, b) -> Prioritizable.compareHighToLow(a, b));
 * // Or sort from lowest to highest priority
 * items.sort((a, b) -> Prioritizable.compareLowToHigh(a, b));
 * </pre>
 * 
 * <p>
 * Important notes:
 * </p>
 * 
 * <ul>
 * <li>Objects with equal priority values have undefined relative ordering
 * <li>Priority values outside the valid range will cause
 * {@link IllegalArgumentException}
 * <li>Implementations should ensure thread-safety if used in concurrent
 * contexts
 * </ul>
 *
 * @author Mark Bednarczyk
 * @see #priority()
 * @see #isValidPriority(int)
 * @see #checkPriorityValue(int)
 */
public interface Prioritizable {

	/**
	 * The Interface LowToHigh.
	 */
	interface LowToHigh extends Prioritizable, Comparable<Prioritizable> {

		/**
		 * Compare to.
		 *
		 * @param o the o
		 * @return the int
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		default int compareTo(Prioritizable o) {
			return Prioritizable.compareLowToHigh(this, o);
		}
	}

	/**
	 * The Interface HighToLow.
	 */
	interface HighToLow extends Prioritizable, Comparable<Prioritizable> {

		/**
		 * Compare to.
		 *
		 * @param o the o
		 * @return the int
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		default int compareTo(Prioritizable o) {
			return Prioritizable.compareHighToLow(this, o);
		}
	}

	/**
	 * The default priority value (0). This is the highest priority level.
	 */
	int DEFAULT_PRIORITY_VALUE = 0;

	/**
	 * The minimum allowed priority value (0). Represents highest priority.
	 */
	int MIN_PRIORITY_VALUE = 0;

	/**
	 * The maximum allowed priority value (Integer.MAX_VALUE - 1). Represents lowest
	 * priority.
	 */
	int MAX_PRIORITY_VALUE = (Integer.MAX_VALUE - 1);

	/**
	 * Comparator for sorting Prioritizable objects from highest to lowest priority.
	 * Lower numerical values are considered higher priority.
	 */
	Comparator<Prioritizable> HIGH_LOW_COMPARATOR = Prioritizable::compareHighToLow;

	/**
	 * Comparator for sorting Prioritizable objects from lowest to highest priority.
	 * Higher numerical values are considered lower priority.
	 */
	Comparator<Prioritizable> LOW_HIGH_COMPARATOR = Prioritizable::compareLowToHigh;

	/** The default. */
	Comparator<Prioritizable> DEFAULT = LOW_HIGH_COMPARATOR;

	/**
	 * Validates that a priority value is within the allowed range.
	 *
	 * @param value the priority value to validate
	 * @return the int
	 * @throws IllegalArgumentException if value is less than
	 *                                  {@link #MIN_PRIORITY_VALUE} or greater than
	 *                                  {@link #MAX_PRIORITY_VALUE}
	 * @see #isValidPriority(int)
	 */
	static int checkPriorityValue(int value) throws IllegalArgumentException {
		if (value < MIN_PRIORITY_VALUE || value > MAX_PRIORITY_VALUE)
			throw new IllegalArgumentException(
					"Priority value out of range [%d], allowed values are [%d, %,d] inclusive"
							.formatted(value, MIN_PRIORITY_VALUE, MAX_PRIORITY_VALUE));

		return value;
	}

	/**
	 * Compares two Prioritizable objects in descending priority order (highest to
	 * lowest). A negative return value indicates {@code a} has higher priority than
	 * {@code b}.
	 *
	 * @param a the first Prioritizable object
	 * @param b the second Prioritizable object
	 * @return negative if a has higher priority, positive if lower, zero if equal
	 * @see #HIGH_LOW_COMPARATOR
	 */
	static int compareHighToLow(Prioritizable a, Prioritizable b) {
		if (a.priority() == b.priority())
			return 0;

		return a.priority() < b.priority() ? 1 : -1;
	}

	/**
	 * Compares two Prioritizable objects in ascending priority order (lowest to
	 * highest). A negative return value indicates {@code a} has lower priority than
	 * {@code b}.
	 *
	 * @param a the first Prioritizable object
	 * @param b the second Prioritizable object
	 * @return negative if a has lower priority, positive if higher, zero if equal
	 * @see #LOW_HIGH_COMPARATOR
	 */
	static int compareLowToHigh(Prioritizable a, Prioritizable b) {
		if (a.priority() == b.priority())
			return 0;

		return a.priority() < b.priority() ? -1 : 1;
	}

	/**
	 * Tests if a priority value is within the valid range.
	 *
	 * @param value the priority value to test
	 * @return true if the value is between {@link #MIN_PRIORITY_VALUE} and
	 *         {@link #MAX_PRIORITY_VALUE} inclusive, false otherwise
	 * @see #checkPriorityValue(int)
	 */
	static boolean isValidPriority(int value) {
		return value >= MIN_PRIORITY_VALUE && value <= MAX_PRIORITY_VALUE;
	}

	/**
	 * Gets the priority value for this object.
	 *
	 * @return an integer between {@link #MIN_PRIORITY_VALUE} and
	 *         {@link #MAX_PRIORITY_VALUE} inclusive, where lower numbers indicate
	 *         higher priority
	 */
	int priority();
}
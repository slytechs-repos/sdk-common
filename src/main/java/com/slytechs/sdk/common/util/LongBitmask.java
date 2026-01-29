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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Utility class for encoding and manipulating port numbers (0–63) as a bitmask
 * in a 64-bit long.
 */
public final class LongBitmask {

	/**
	 * Instantiates a new long bitmask.
	 */
	private LongBitmask() {
		// Static utility class; prevent instantiation
	}

	/**
	 * Enable a port (set bit at index).
	 *
	 * @param mask       the mask
	 * @param portNumber the port number
	 * @return the long
	 */
	public static long enable(long mask, int portNumber) {
		validatePort(portNumber);
		return mask | (1L << portNumber);
	}

	/**
	 * Disable a port (clear bit at index).
	 *
	 * @param mask       the mask
	 * @param portNumber the port number
	 * @return the long
	 */
	public static long disable(long mask, int portNumber) {
		validatePort(portNumber);
		return mask & ~(1L << portNumber);
	}

	/**
	 * Toggle a port (flip bit at index).
	 *
	 * @param mask       the mask
	 * @param portNumber the port number
	 * @return the long
	 */
	public static long toggle(long mask, int portNumber) {
		validatePort(portNumber);
		return mask ^ (1L << portNumber);
	}

	/**
	 * Check if a port is enabled (bit is set).
	 *
	 * @param mask       the mask
	 * @param portNumber the port number
	 * @return true, if is enabled
	 */
	public static boolean isEnabled(long mask, int portNumber) {
		validatePort(portNumber);
		return (mask & (1L << portNumber)) != 0;
	}

	/**
	 * Count how many ports are enabled (number of bits set).
	 *
	 * @param mask the mask
	 * @return the int
	 */
	public static int countEnabled(long mask) {
		return Long.bitCount(mask);
	}

	/**
	 * Check if all ports are disabled (no bits set).
	 *
	 * @param mask the mask
	 * @return true, if is empty
	 */
	public static boolean isEmpty(long mask) {
		return mask == 0L;
	}

	/**
	 * Iterate enabled ports.
	 *
	 * @param mask the mask
	 * @return the iterable
	 */
	public static Iterable<Integer> iterateEnabledPorts(long mask) {
		return () -> new Iterator<>() {
			private long remaining = mask;
			private int next = findNext();

			private int findNext() {
				if (remaining == 0)
					return -1;
				int index = Long.numberOfTrailingZeros(remaining);
				remaining &= ~(1L << index); // clear the bit
				return index;
			}

			@Override
			public boolean hasNext() {
				return next >= 0;
			}

			@Override
			public Integer next() {
				if (next < 0)
					throw new NoSuchElementException();
				int result = next;
				next = findNext();
				return result;
			}
		};
	}

	/**
	 * Get the index of the lowest enabled port (least significant bit set), or -1
	 * if none.
	 *
	 * @param mask the mask
	 * @return the int
	 */
	public static int firstEnabled(long mask) {
		return mask == 0 ? -1 : Long.numberOfTrailingZeros(mask);
	}

	/**
	 * Get the index of the highest enabled port (most significant bit set), or -1
	 * if none.
	 *
	 * @param mask the mask
	 * @return the int
	 */
	public static int lastEnabled(long mask) {
		return mask == 0 ? -1 : 63 - Long.numberOfLeadingZeros(mask);
	}

	/**
	 * Enable all ports (set all bits).
	 *
	 * @return the long
	 */
	public static long enableAll() {
		return -1L; // all 64 bits set
	}

	/**
	 * Disable all ports (clear all bits).
	 *
	 * @return the long
	 */
	public static long disableAll() {
		return 0L;
	}

	/**
	 * Validate port number is within 0–63.
	 *
	 * @param portNumber the port number
	 */
	private static void validatePort(int portNumber) {
		if (portNumber < 0 || portNumber >= Long.SIZE) {
			throw new IllegalArgumentException("Port number must be between 0 and 63: " + portNumber);
		}
	}
}

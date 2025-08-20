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
package com.slytechs.jnet.core.api.util.function;

/**
 * A specialized interface for tuple types that represent a fixed-size
 * collection of integer values. Each element in the tuple is a primitive int
 * value.
 * 
 * <p>
 * The record implementations provide standard {@code equals()},
 * {@code hashCode()} and {@code toString()} behavior where:
 * <ul>
 * <li>equals() performs value-based comparison of all components
 * <li>hashCode() combines component hash codes consistently with equals()
 * <li>toString() formats as "IntTupleN [value1, value2, ...]"
 * </ul>
 * </p>
 * 
 * <p>
 * This interface defines tuple types from 2 to 5 values. Each tuple type is
 * immutable and thread-safe. Tuples provide convenient static factory methods
 * through their respective {@code of} methods.
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>{@code
 * // Create tuple with two integers
 * IntTuple2 point = IntTuple2.of(10, 20);
 * 
 * // Access values
 * int x = point.value1(); // 10
 * int y = point.value2(); // 20
 * int[] all = point.values(); // [10, 20]
 * }</pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc
 * @see IntTuple2
 * @see IntTuple3
 * @see IntTuple4
 * @see IntTuple5
 */
public interface IntTuple {

	/**
	 * A tuple of 2 integer elements.
	 */
	interface IntTuple2 extends IntTuple {

		/**
		 * Creates a new tuple with two integer values.
		 *
		 * @param value1 the first value
		 * @param value2 the second value
		 * @return a new tuple containing the values
		 */
		static IntTuple2 of(int value1, int value2) {
			return new Tuple2i(value1, value2);
		}

		@Override
		default int size() {
			return 2;
		}
	}

	/**
	 * A tuple of 3 integer elements.
	 */
	interface IntTuple3 extends IntTuple {

		/**
		 * Creates a new tuple with three integer values.
		 *
		 * @param value1 the first value
		 * @param value2 the second value
		 * @param value3 the third value
		 * @return a new tuple containing the values
		 */
		static IntTuple3 of(int value1, int value2, int value3) {
			return new Tuple3i(value1, value2, value3);
		}

		@Override
		default int size() {
			return 3;
		}

		@Override
		int value3();
	}

	/**
	 * A tuple of 4 integer elements.
	 */
	interface IntTuple4 extends IntTuple {

		/**
		 * Creates a new tuple with four integer values.
		 *
		 * @param value1 the first value
		 * @param value2 the second value
		 * @param value3 the third value
		 * @param value4 the fourth value
		 * @return a new tuple containing the values
		 */
		static IntTuple4 of(int value1, int value2, int value3, int value4) {
			return new Tuple4i(value1, value2, value3, value4);
		}

		@Override
		default int size() {
			return 4;
		}

		@Override
		int value3();

		@Override
		int value4();
	}

	/**
	 * A tuple of 5 integer elements.
	 */
	interface IntTuple5 extends IntTuple {

		/**
		 * Creates a new tuple with five integer values.
		 *
		 * @param value1 the first value
		 * @param value2 the second value
		 * @param value3 the third value
		 * @param value4 the fourth value
		 * @param value5 the fifth value
		 * @return a new tuple containing the values
		 */
		static IntTuple5 of(int value1, int value2, int value3, int value4, int value5) {
			return new Tuple5i(value1, value2, value3, value4, value5);
		}

		@Override
		default int size() {
			return 5;
		}

		@Override
		int value3();

		@Override
		int value4();

		@Override
		int value5();
	}

	/**
	 * Record implementation of IntTuple2.
	 */
	record Tuple2i(int value1, int value2) implements IntTuple2 {
		@Override
		public int[] values() {
			return new int[] {
					value1,
					value2
			};
		}

		@Override
		public String toString() {
			return "IntTuple2 [" + value1 + ", " + value2 + "]";
		}

	}

	/**
	 * Record implementation of IntTuple3.
	 */
	record Tuple3i(int value1, int value2, int value3) implements IntTuple3 {
		@Override
		public int[] values() {
			return new int[] {
					value1,
					value2,
					value3
			};
		}

		@Override
		public String toString() {
			return "IntTuple3 [" + value1 + ", " + value2 + ", " + value3 + "]";
		}
	}

	/**
	 * Record implementation of IntTuple4.
	 */
	record Tuple4i(int value1, int value2, int value3, int value4) implements IntTuple4 {
		@Override
		public int[] values() {
			return new int[] {
					value1,
					value2,
					value3,
					value4
			};
		}

		@Override
		public String toString() {
			return "IntTuple4 [" + value1 + ", " + value2 + ", " + value3 + ", " + value4 + "]";
		}
	}

	/**
	 * Record implementation of IntTuple5.
	 */
	record Tuple5i(int value1, int value2, int value3, int value4, int value5)
			implements IntTuple5 {
		@Override
		public int[] values() {
			return new int[] {
					value1,
					value2,
					value3,
					value4,
					value5
			};
		}

		@Override
		public String toString() {
			return "IntTuple5 [" + value1 + ", " + value2 + ", " + value3 + ", " +
					value4 + ", " + value5 + "]";
		}
	}

	/**
	 * Creates a new tuple with two integer values.
	 *
	 * @param value1 the first value
	 * @param value2 the second value
	 * @return a new tuple containing the values
	 */
	static IntTuple2 of(int value1, int value2) {
		return new Tuple2i(value1, value2);
	}

	/**
	 * Creates a new tuple with three integer values.
	 *
	 * @param value1 the first value
	 * @param value2 the second value
	 * @param value3 the third value
	 * @return a new tuple containing the values
	 */
	static IntTuple3 of(int value1, int value2, int value3) {
		return new Tuple3i(value1, value2, value3);
	}

	/**
	 * Creates a new tuple with four integer values.
	 *
	 * @param value1 the first value
	 * @param value2 the second value
	 * @param value3 the third value
	 * @param value4 the fourth value
	 * @return a new tuple containing the values
	 */
	static IntTuple4 of(int value1, int value2, int value3, int value4) {
		return new Tuple4i(value1, value2, value3, value4);
	}

	/**
	 * Creates a new tuple with five integer values.
	 *
	 * @param value1 the first value
	 * @param value2 the second value
	 * @param value3 the third value
	 * @param value4 the fourth value
	 * @param value5 the fifth value
	 * @return a new tuple containing the values
	 */
	static IntTuple5 of(int value1, int value2, int value3, int value4, int value5) {
		return new Tuple5i(value1, value2, value3, value4, value5);
	}

	/**
	 * Returns the number of elements in this tuple.
	 *
	 * @return the number of elements in this tuple
	 */
	int size();

	/**
	 * Returns the first value.
	 *
	 * @return the first integer value
	 */
	int value1();

	/**
	 * Returns the second value.
	 *
	 * @return the second integer value
	 */
	int value2();

	/**
	 * Returns the third value.
	 *
	 * @return the third integer value
	 * @throws UnsupportedOperationException if this tuple type does not support a
	 *                                       third value
	 */
	default int value3() {
		throw new UnsupportedOperationException("this tuple type does not have a value3");
	}

	/**
	 * Returns the fourth value.
	 *
	 * @return the fourth integer value
	 * @throws UnsupportedOperationException if this tuple type does not support a
	 *                                       fourth value
	 */
	default int value4() {
		throw new UnsupportedOperationException("this tuple type does not have a value4");
	}

	/**
	 * Returns the fifth value.
	 *
	 * @return the fifth integer value
	 * @throws UnsupportedOperationException if this tuple type does not support a
	 *                                       fifth value
	 */
	default int value5() {
		throw new UnsupportedOperationException("this tuple type does not have a value5");
	}

	/**
	 * Returns all values of this tuple as an array of integers.
	 *
	 * @return an array containing all integer values in this tuple
	 */
	int[] values();
}
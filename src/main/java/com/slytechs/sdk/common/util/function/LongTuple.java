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
package com.slytechs.sdk.common.util.function;

/**
 * A specialized interface for tuple types that represent a fixed-size
 * collection of long values. Each element in the tuple is a primitive long
 * value.
 * 
 * <p>
 * The record implementations provide standard {@code equals()},
 * {@code hashCode()} and {@code toString()} behavior where:
 * <ul>
 * <li>equals() performs value-based comparison of all components
 * <li>hashCode() combines component hash codes consistently with equals()
 * <li>toString() formats as "LongTupleN [value1, value2, ...]"
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
 * // Create tuple with two longs
 * LongTuple2 range = LongTuple2.of(1000L, 2000L);
 * 
 * // Access values
 * long start = range.value1(); // 1000L
 * long end = range.value2(); // 2000L
 * long[] all = range.values(); // [1000L, 2000L]
 * }</pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc
 * @see LongTuple2
 * @see LongTuple3
 * @see LongTuple4
 * @see LongTuple5
 */
public interface LongTuple {

	/**
	 * A tuple of 2 long elements.
	 */
	interface LongTuple2 extends LongTuple {

		/**
		 * Creates a new tuple with two long values.
		 *
		 * @param value1 the first value
		 * @param value2 the second value
		 * @return a new tuple containing the values
		 */
		static LongTuple2 of(long value1, long value2) {
			return new Tuple2l(value1, value2);
		}

		/**
		 * @see com.slytechs.sdk.common.util.function.LongTuple#size()
		 */
		@Override
		default int size() {
			return 2;
		}
	}

	/**
	 * A tuple of 3 long elements.
	 */
	interface LongTuple3 extends LongTuple {

		/**
		 * Creates a new tuple with three long values.
		 *
		 * @param value1 the first value
		 * @param value2 the second value
		 * @param value3 the third value
		 * @return a new tuple containing the values
		 */
		static LongTuple3 of(long value1, long value2, long value3) {
			return new Tuple3l(value1, value2, value3);
		}

		/**
		 * @see com.slytechs.sdk.common.util.function.LongTuple#size()
		 */
		@Override
		default int size() {
			return 3;
		}

		/**
		 * @see com.slytechs.sdk.common.util.function.LongTuple#value3()
		 */
		@Override
		long value3();
	}

	/**
	 * A tuple of 4 long elements.
	 */
	interface LongTuple4 extends LongTuple {

		/**
		 * Creates a new tuple with four long values.
		 *
		 * @param value1 the first value
		 * @param value2 the second value
		 * @param value3 the third value
		 * @param value4 the fourth value
		 * @return a new tuple containing the values
		 */
		static LongTuple4 of(long value1, long value2, long value3, long value4) {
			return new Tuple4l(value1, value2, value3, value4);
		}

		/**
		 * @see com.slytechs.sdk.common.util.function.LongTuple#size()
		 */
		@Override
		default int size() {
			return 4;
		}

		/**
		 * @see com.slytechs.sdk.common.util.function.LongTuple#value3()
		 */
		@Override
		long value3();

		/**
		 * @see com.slytechs.sdk.common.util.function.LongTuple#value4()
		 */
		@Override
		long value4();
	}

	/**
	 * A tuple of 5 long elements.
	 */
	interface LongTuple5 extends LongTuple {

		/**
		 * Creates a new tuple with five long values.
		 *
		 * @param value1 the first value
		 * @param value2 the second value
		 * @param value3 the third value
		 * @param value4 the fourth value
		 * @param value5 the fifth value
		 * @return a new tuple containing the values
		 */
		static LongTuple5 of(long value1, long value2, long value3, long value4, long value5) {
			return new Tuple5l(value1, value2, value3, value4, value5);
		}

		/**
		 * @see com.slytechs.sdk.common.util.function.LongTuple#size()
		 */
		@Override
		default int size() {
			return 5;
		}

		/**
		 * @see com.slytechs.sdk.common.util.function.LongTuple#value3()
		 */
		@Override
		long value3();

		/**
		 * @see com.slytechs.sdk.common.util.function.LongTuple#value4()
		 */
		@Override
		long value4();

		/**
		 * @see com.slytechs.sdk.common.util.function.LongTuple#value5()
		 */
		@Override
		long value5();
	}

	/**
	 * Record implementation of LongTuple2.
	 *
	 * @param value1 the value 1
	 * @param value2 the value 2
	 */
	record Tuple2l(long value1, long value2) implements LongTuple2 {
		
		/**
		 * @see com.slytechs.sdk.common.util.function.LongTuple#values()
		 */
		@Override
		public long[] values() {
			return new long[] {
					value1,
					value2
			};
		}

		/**
		 * @see java.lang.Record#toString()
		 */
		@Override
		public String toString() {
			return "LongTuple2 [" + value1 + ", " + value2 + "]";
		}
	}

	/**
	 * Record implementation of LongTuple3.
	 *
	 * @param value1 the value 1
	 * @param value2 the value 2
	 * @param value3 the value 3
	 */
	record Tuple3l(long value1, long value2, long value3) implements LongTuple3 {
		
		/**
		 * @see com.slytechs.sdk.common.util.function.LongTuple#values()
		 */
		@Override
		public long[] values() {
			return new long[] {
					value1,
					value2,
					value3
			};
		}

		/**
		 * @see java.lang.Record#toString()
		 */
		@Override
		public String toString() {
			return "LongTuple3 [" + value1 + ", " + value2 + ", " + value3 + "]";
		}
	}

	/**
	 * Record implementation of LongTuple4.
	 *
	 * @param value1 the value 1
	 * @param value2 the value 2
	 * @param value3 the value 3
	 * @param value4 the value 4
	 */
	record Tuple4l(long value1, long value2, long value3, long value4) implements LongTuple4 {
		
		/**
		 * @see com.slytechs.sdk.common.util.function.LongTuple#values()
		 */
		@Override
		public long[] values() {
			return new long[] {
					value1,
					value2,
					value3,
					value4
			};
		}

		/**
		 * @see java.lang.Record#toString()
		 */
		@Override
		public String toString() {
			return "LongTuple4 [" + value1 + ", " + value2 + ", " + value3 + ", " + value4 + "]";
		}
	}

	/**
	 * Record implementation of LongTuple5.
	 *
	 * @param value1 the value 1
	 * @param value2 the value 2
	 * @param value3 the value 3
	 * @param value4 the value 4
	 * @param value5 the value 5
	 */
	record Tuple5l(long value1, long value2, long value3, long value4, long value5)
			implements LongTuple5 {
		
		/**
		 * @see com.slytechs.sdk.common.util.function.LongTuple#values()
		 */
		@Override
		public long[] values() {
			return new long[] {
					value1,
					value2,
					value3,
					value4,
					value5
			};
		}

		/**
		 * @see java.lang.Record#toString()
		 */
		@Override
		public String toString() {
			return "LongTuple5 [" + value1 + ", " + value2 + ", " + value3 + ", " +
					value4 + ", " + value5 + "]";
		}
	}

	/**
	 * Creates a new tuple with two long values.
	 *
	 * @param value1 the first value
	 * @param value2 the second value
	 * @return a new tuple containing the values
	 */
	static LongTuple2 of(long value1, long value2) {
		return new Tuple2l(value1, value2);
	}

	/**
	 * Creates a new tuple with three long values.
	 *
	 * @param value1 the first value
	 * @param value2 the second value
	 * @param value3 the third value
	 * @return a new tuple containing the values
	 */
	static LongTuple3 of(long value1, long value2, long value3) {
		return new Tuple3l(value1, value2, value3);
	}

	/**
	 * Creates a new tuple with four long values.
	 *
	 * @param value1 the first value
	 * @param value2 the second value
	 * @param value3 the third value
	 * @param value4 the fourth value
	 * @return a new tuple containing the values
	 */
	static LongTuple4 of(long value1, long value2, long value3, long value4) {
		return new Tuple4l(value1, value2, value3, value4);
	}

	/**
	 * Creates a new tuple with five long values.
	 *
	 * @param value1 the first value
	 * @param value2 the second value
	 * @param value3 the third value
	 * @param value4 the fourth value
	 * @param value5 the fifth value
	 * @return a new tuple containing the values
	 */
	static LongTuple5 of(long value1, long value2, long value3, long value4, long value5) {
		return new Tuple5l(value1, value2, value3, value4, value5);
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
	 * @return the first long value
	 */
	long value1();

	/**
	 * Returns the second value.
	 *
	 * @return the second long value
	 */
	long value2();

	/**
	 * Returns the third value.
	 *
	 * @return the third long value
	 * @throws UnsupportedOperationException if this tuple type does not support a
	 *                                       third value
	 */
	default long value3() {
		throw new UnsupportedOperationException("this tuple type does not have a value3");
	}

	/**
	 * Returns the fourth value.
	 *
	 * @return the fourth long value
	 * @throws UnsupportedOperationException if this tuple type does not support a
	 *                                       fourth value
	 */
	default long value4() {
		throw new UnsupportedOperationException("this tuple type does not have a value4");
	}

	/**
	 * Returns the fifth value.
	 *
	 * @return the fifth long value
	 * @throws UnsupportedOperationException if this tuple type does not support a
	 *                                       fifth value
	 */
	default long value5() {
		throw new UnsupportedOperationException("this tuple type does not have a value5");
	}

	/**
	 * Returns all values of this tuple as an array of longs.
	 *
	 * @return an array containing all long values in this tuple
	 */
	long[] values();
}
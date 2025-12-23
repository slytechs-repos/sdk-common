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
 * collection of float values. Each element in the tuple is a primitive float
 * value.
 * 
 * <p>
 * The record implementations provide standard {@code equals()},
 * {@code hashCode()} and {@code toString()} behavior where:
 * <ul>
 * <li>equals() performs value-based comparison of all components
 * <li>hashCode() combines component hash codes consistently with equals()
 * <li>toString() formats as "FloatTupleN [value1, value2, ...]"
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
 * // Create tuple with two floats
 * FloatTuple2 point = FloatTuple2.of(3.14f, 2.718f);
 * 
 * // Access values
 * float x = point.value1(); // 3.14f
 * float y = point.value2(); // 2.718f
 * float[] all = point.values(); // [3.14f, 2.718f]
 * }</pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc
 * @see FloatTuple2
 * @see FloatTuple3
 * @see FloatTuple4
 * @see FloatTuple5
 */
public interface FloatTuple {

	/**
	 * A tuple of 2 float elements.
	 */
	interface FloatTuple2 extends FloatTuple {

		/**
		 * Creates a new tuple with two float values.
		 *
		 * @param value1 the first value
		 * @param value2 the second value
		 * @return a new tuple containing the values
		 */
		static FloatTuple2 of(float value1, float value2) {
			return new Tuple2f(value1, value2);
		}

		/**
		 * @see com.slytechs.sdk.common.util.function.FloatTuple#size()
		 */
		@Override
		default int size() {
			return 2;
		}
	}

	/**
	 * A tuple of 3 float elements.
	 */
	interface FloatTuple3 extends FloatTuple {

		/**
		 * Creates a new tuple with three float values.
		 *
		 * @param value1 the first value
		 * @param value2 the second value
		 * @param value3 the third value
		 * @return a new tuple containing the values
		 */
		static FloatTuple3 of(float value1, float value2, float value3) {
			return new Tuple3f(value1, value2, value3);
		}

		/**
		 * @see com.slytechs.sdk.common.util.function.FloatTuple#size()
		 */
		@Override
		default int size() {
			return 3;
		}

		/**
		 * @see com.slytechs.sdk.common.util.function.FloatTuple#value3()
		 */
		@Override
		float value3();
	}

	/**
	 * A tuple of 4 float elements.
	 */
	interface FloatTuple4 extends FloatTuple {

		/**
		 * Creates a new tuple with four float values.
		 *
		 * @param value1 the first value
		 * @param value2 the second value
		 * @param value3 the third value
		 * @param value4 the fourth value
		 * @return a new tuple containing the values
		 */
		static FloatTuple4 of(float value1, float value2, float value3, float value4) {
			return new Tuple4f(value1, value2, value3, value4);
		}

		/**
		 * @see com.slytechs.sdk.common.util.function.FloatTuple#size()
		 */
		@Override
		default int size() {
			return 4;
		}

		/**
		 * @see com.slytechs.sdk.common.util.function.FloatTuple#value3()
		 */
		@Override
		float value3();

		/**
		 * @see com.slytechs.sdk.common.util.function.FloatTuple#value4()
		 */
		@Override
		float value4();
	}

	/**
	 * A tuple of 5 float elements.
	 */
	interface FloatTuple5 extends FloatTuple {

		/**
		 * Creates a new tuple with five float values.
		 *
		 * @param value1 the first value
		 * @param value2 the second value
		 * @param value3 the third value
		 * @param value4 the fourth value
		 * @param value5 the fifth value
		 * @return a new tuple containing the values
		 */
		static FloatTuple5 of(float value1, float value2, float value3, float value4, float value5) {
			return new Tuple5f(value1, value2, value3, value4, value5);
		}

		/**
		 * @see com.slytechs.sdk.common.util.function.FloatTuple#size()
		 */
		@Override
		default int size() {
			return 5;
		}

		/**
		 * @see com.slytechs.sdk.common.util.function.FloatTuple#value3()
		 */
		@Override
		float value3();

		/**
		 * @see com.slytechs.sdk.common.util.function.FloatTuple#value4()
		 */
		@Override
		float value4();

		/**
		 * @see com.slytechs.sdk.common.util.function.FloatTuple#value5()
		 */
		@Override
		float value5();
	}

	/**
	 * Record implementation of FloatTuple2.
	 *
	 * @param value1 the value 1
	 * @param value2 the value 2
	 */
	record Tuple2f(float value1, float value2) implements FloatTuple2 {
		
		/**
		 * @see com.slytechs.sdk.common.util.function.FloatTuple#values()
		 */
		@Override
		public float[] values() {
			return new float[] {
					value1,
					value2
			};
		}

		/**
		 * @see java.lang.Record#toString()
		 */
		@Override
		public String toString() {
			return "FloatTuple2 [" + value1 + ", " + value2 + "]";
		}
	}

	/**
	 * Record implementation of FloatTuple3.
	 *
	 * @param value1 the value 1
	 * @param value2 the value 2
	 * @param value3 the value 3
	 */
	record Tuple3f(float value1, float value2, float value3) implements FloatTuple3 {
		
		/**
		 * @see com.slytechs.sdk.common.util.function.FloatTuple#values()
		 */
		@Override
		public float[] values() {
			return new float[] {
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
			return "FloatTuple3 [" + value1 + ", " + value2 + ", " + value3 + "]";
		}
	}

	/**
	 * Record implementation of FloatTuple4.
	 *
	 * @param value1 the value 1
	 * @param value2 the value 2
	 * @param value3 the value 3
	 * @param value4 the value 4
	 */
	record Tuple4f(float value1, float value2, float value3, float value4)
			implements FloatTuple4 {
		
		/**
		 * @see com.slytechs.sdk.common.util.function.FloatTuple#values()
		 */
		@Override
		public float[] values() {
			return new float[] {
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
			return "FloatTuple4 [" + value1 + ", " + value2 + ", " + value3 + ", " + value4 + "]";
		}
	}

	/**
	 * Record implementation of FloatTuple5.
	 *
	 * @param value1 the value 1
	 * @param value2 the value 2
	 * @param value3 the value 3
	 * @param value4 the value 4
	 * @param value5 the value 5
	 */
	record Tuple5f(float value1, float value2, float value3, float value4, float value5)
			implements FloatTuple5 {
		
		/**
		 * @see com.slytechs.sdk.common.util.function.FloatTuple#values()
		 */
		@Override
		public float[] values() {
			return new float[] {
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
			return "FloatTuple5 [" + value1 + ", " + value2 + ", " + value3 + ", " +
					value4 + ", " + value5 + "]";
		}
	}

	/**
	 * Creates a new tuple with two float values.
	 *
	 * @param value1 the first value
	 * @param value2 the second value
	 * @return a new tuple containing the values
	 */
	static FloatTuple2 of(float value1, float value2) {
		return new Tuple2f(value1, value2);
	}

	/**
	 * Creates a new tuple with three float values.
	 *
	 * @param value1 the first value
	 * @param value2 the second value
	 * @param value3 the third value
	 * @return a new tuple containing the values
	 */
	static FloatTuple3 of(float value1, float value2, float value3) {
		return new Tuple3f(value1, value2, value3);
	}

	/**
	 * Creates a new tuple with four float values.
	 *
	 * @param value1 the first value
	 * @param value2 the second value
	 * @param value3 the third value
	 * @param value4 the fourth value
	 * @return a new tuple containing the values
	 */
	static FloatTuple4 of(float value1, float value2, float value3, float value4) {
		return new Tuple4f(value1, value2, value3, value4);
	}

	/**
	 * Creates a new tuple with five float values.
	 *
	 * @param value1 the first value
	 * @param value2 the second value
	 * @param value3 the third value
	 * @param value4 the fourth value
	 * @param value5 the fifth value
	 * @return a new tuple containing the values
	 */
	static FloatTuple5 of(float value1, float value2, float value3, float value4, float value5) {
		return new Tuple5f(value1, value2, value3, value4, value5);
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
	 * @return the first float value
	 */
	float value1();

	/**
	 * Returns the second value.
	 *
	 * @return the second float value
	 */
	float value2();

	/**
	 * Returns the third value.
	 *
	 * @return the third float value
	 * @throws UnsupportedOperationException if this tuple type does not support a
	 *                                       third value
	 */
	default float value3() {
		throw new UnsupportedOperationException("this tuple type does not have a value3");
	}

	/**
	 * Returns the fourth value.
	 *
	 * @return the fourth float value
	 * @throws UnsupportedOperationException if this tuple type does not support a
	 *                                       fourth value
	 */
	default float value4() {
		throw new UnsupportedOperationException("this tuple type does not have a value4");
	}

	/**
	 * Returns the fifth value.
	 *
	 * @return the fifth float value
	 * @throws UnsupportedOperationException if this tuple type does not support a
	 *                                       fifth value
	 */
	default float value5() {
		throw new UnsupportedOperationException("this tuple type does not have a value5");
	}

	/**
	 * Returns all values of this tuple as an array of floats.
	 *
	 * @return an array containing all float values in this tuple
	 */
	float[] values();
}
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
package com.slytechs.jnet.core.api.util.function;

import com.slytechs.jnet.core.api.util.function.Tuple.Tuple2.Tuple2Record;
import com.slytechs.jnet.core.api.util.function.Tuple.Tuple3.Tuple3Record;
import com.slytechs.jnet.core.api.util.function.Tuple.Tuple4.Tuple4Record;
import com.slytechs.jnet.core.api.util.function.Tuple.Tuple5.Tuple5Record;

/**
 * A generic interface for tuple types that represent a fixed-size collection of
 * heterogeneous values. Each element in a tuple can be of a different type and
 * may be null.
 * 
 * <p>
 * The record implementations provide standard {@code equals()},
 * {@code hashCode()} and {@code toString()} behavior where:
 * <ul>
 * <li>equals() performs value-based comparison of all components, correctly
 * handling nulls
 * <li>hashCode() combines component hash codes consistently with equals()
 * <li>toString() formats as "TupleN [value1, value2, ...]"
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
 * // Create tuple with a String and Integer
 * Tuple2<String, Integer> person = Tuple2.of("John", 25);
 * 
 * // Access values (may return null)
 * String name = person.value1(); // "John"
 * Integer age = person.value2(); // 25
 * Object[] all = person.values(); // ["John", 25]
 * }</pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see Tuple2
 * @see Tuple3
 * @see Tuple4
 * @see Tuple5
 */
public interface Tuple {

	/**
	 * A tuple of 2 elements.
	 *
	 * @param <T1> the type of the first value
	 * @param <T2> the type of the second value
	 */
	interface Tuple2<T1, T2> extends Tuple {

		/**
		 * Record implementation of Tuple2.
		 *
		 * @param <T1>   the generic type
		 * @param <T2>   the generic type
		 * @param size   the size
		 * @param value1 the value 1
		 * @param value2 the value 2
		 */
		record Tuple2Record<T1, T2>(int size, T1 value1, T2 value2) implements Tuple2<T1, T2> {
			
			/**
			 * @see com.slytechs.jnet.core.api.util.function.Tuple#values()
			 */
			@Override
			public Object[] values() {
				return new Object[] {
						value1,
						value2
				};
			}

			/**
			 * @see java.lang.Record#toString()
			 */
			@Override
			public String toString() {
				return "Tuple2 [" + String.valueOf(value1) + ", " + String.valueOf(value2) + "]";
			}
		}

		/**
		 * Creates a new tuple with two values.
		 *
		 * @param <T1>   the type of the first value
		 * @param <T2>   the type of the second value
		 * @param value1 the first value, may be null
		 * @param value2 the second value, may be null
		 * @return a new tuple containing the values
		 */
		static <T1, T2> Tuple2<T1, T2> of(T1 value1, T2 value2) {
			return new Tuple2Record<>(2, value1, value2);
		}

		/**
		 * Returns the first value.
		 *
		 * @return the first value, may be null
		 */
		@Override

		T1 value1();

		/**
		 * Returns the second value.
		 *
		 * @return the second value, may be null
		 */
		@Override

		T2 value2();
	}

	/**
	 * A tuple of 3 elements.
	 *
	 * @param <T1> the type of the first value
	 * @param <T2> the type of the second value
	 * @param <T3> the type of the third value
	 */
	interface Tuple3<T1, T2, T3> extends Tuple {

		/**
		 * Record implementation of Tuple3.
		 *
		 * @param <T1>   the generic type
		 * @param <T2>   the generic type
		 * @param <T3>   the generic type
		 * @param size   the size
		 * @param value1 the value 1
		 * @param value2 the value 2
		 * @param value3 the value 3
		 */
		record Tuple3Record<T1, T2, T3>(int size, T1 value1, T2 value2, T3 value3)
				implements Tuple3<T1, T2, T3> {
			
			/**
			 * @see com.slytechs.jnet.core.api.util.function.Tuple#values()
			 */
			@Override
			public Object[] values() {
				return new Object[] {
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
				return "Tuple3 [" + String.valueOf(value1) + ", " + String.valueOf(value2) + ", " +
						String.valueOf(value3) + "]";
			}
		}

		/**
		 * Creates a new tuple with three values.
		 *
		 * @param <T1>   the type of the first value
		 * @param <T2>   the type of the second value
		 * @param <T3>   the type of the third value
		 * @param value1 the first value, may be null
		 * @param value2 the second value, may be null
		 * @param value3 the third value, may be null
		 * @return a new tuple containing the values
		 */
		static <T1, T2, T3> Tuple3<T1, T2, T3> of(
				T1 value1,
				T2 value2,
				T3 value3) {
			return new Tuple3Record<>(3, value1, value2, value3);
		}

		/**
		 * Returns the first value.
		 *
		 * @return the first value, may be null
		 */
		@Override

		T1 value1();

		/**
		 * Returns the second value.
		 *
		 * @return the second value, may be null
		 */
		@Override

		T2 value2();

		/**
		 * Returns the third value.
		 *
		 * @return the third value, may be null
		 */
		@Override

		T3 value3();
	}

	/**
	 * A tuple of 4 elements.
	 *
	 * @param <T1> the type of the first value
	 * @param <T2> the type of the second value
	 * @param <T3> the type of the third value
	 * @param <T4> the type of the fourth value
	 */
	interface Tuple4<T1, T2, T3, T4> extends Tuple {

		/**
		 * Record implementation of Tuple4.
		 *
		 * @param <T1>   the generic type
		 * @param <T2>   the generic type
		 * @param <T3>   the generic type
		 * @param <T4>   the generic type
		 * @param size   the size
		 * @param value1 the value 1
		 * @param value2 the value 2
		 * @param value3 the value 3
		 * @param value4 the value 4
		 */
		record Tuple4Record<T1, T2, T3, T4>(
				int size,
				T1 value1,
				T2 value2,
				T3 value3,
				T4 value4) implements Tuple4<T1, T2, T3, T4> {

			/**
			 * @see com.slytechs.jnet.core.api.util.function.Tuple#values()
			 */
			@Override
			public Object[] values() {
				return new Object[] {
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
				return "Tuple4 [" +
						String.valueOf(value1) + ", " +
						String.valueOf(value2) + ", " +
						String.valueOf(value3) + ", " +
						String.valueOf(value4) + "]";
			}
		}

		/**
		 * Creates a new tuple with four values.
		 *
		 * @param <T1>   the type of the first value
		 * @param <T2>   the type of the second value
		 * @param <T3>   the type of the third value
		 * @param <T4>   the type of the fourth value
		 * @param value1 the first value, may be null
		 * @param value2 the second value, may be null
		 * @param value3 the third value, may be null
		 * @param value4 the fourth value, may be null
		 * @return a new tuple containing the values
		 */
		static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> of(
				T1 value1,
				T2 value2,
				T3 value3,
				T4 value4) {
			return new Tuple4Record<>(4, value1, value2, value3, value4);
		}

		/**
		 * Returns the first value.
		 *
		 * @return the first value, may be null
		 */
		@Override

		T1 value1();

		/**
		 * Returns the second value.
		 *
		 * @return the second value, may be null
		 */
		@Override

		T2 value2();

		/**
		 * Returns the third value.
		 *
		 * @return the third value, may be null
		 */
		@Override

		T3 value3();

		/**
		 * Returns the fourth value.
		 *
		 * @return the fourth value, may be null
		 */
		@Override

		T4 value4();
	}

	/**
	 * A tuple of 5 elements.
	 *
	 * @param <T1> the type of the first value
	 * @param <T2> the type of the second value
	 * @param <T3> the type of the third value
	 * @param <T4> the type of the fourth value
	 * @param <T5> the type of the fifth value
	 */
	interface Tuple5<T1, T2, T3, T4, T5> extends Tuple {

		/**
		 * Record implementation of Tuple5.
		 *
		 * @param <T1>   the generic type
		 * @param <T2>   the generic type
		 * @param <T3>   the generic type
		 * @param <T4>   the generic type
		 * @param <T5>   the generic type
		 * @param size   the size
		 * @param value1 the value 1
		 * @param value2 the value 2
		 * @param value3 the value 3
		 * @param value4 the value 4
		 * @param value5 the value 5
		 */
		record Tuple5Record<T1, T2, T3, T4, T5>(
				int size,
				T1 value1,
				T2 value2,
				T3 value3,
				T4 value4,
				T5 value5) implements Tuple5<T1, T2, T3, T4, T5> {

			/**
			 * @see com.slytechs.jnet.core.api.util.function.Tuple#values()
			 */
			@Override
			public Object[] values() {
				return new Object[] {
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
				return "Tuple5 [" +
						String.valueOf(value1) + ", " +
						String.valueOf(value2) + ", " +
						String.valueOf(value3) + ", " +
						String.valueOf(value4) + ", " +
						String.valueOf(value5) + "]";
			}
		}

		/**
		 * Creates a new tuple with five values.
		 *
		 * @param <T1>   the type of the first value
		 * @param <T2>   the type of the second value
		 * @param <T3>   the type of the third value
		 * @param <T4>   the type of the fourth value
		 * @param <T5>   the type of the fifth value
		 * @param value1 the first value, may be null
		 * @param value2 the second value, may be null
		 * @param value3 the third value, may be null
		 * @param value4 the fourth value, may be null
		 * @param value5 the fifth value, may be null
		 * @return a new tuple containing the values
		 */
		static <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> of(
				T1 value1,
				T2 value2,
				T3 value3,
				T4 value4,
				T5 value5) {
			return new Tuple5Record<>(5, value1, value2, value3, value4, value5);
		}

		/**
		 * Returns the first value.
		 *
		 * @return the first value, may be null
		 */
		@Override
		T1 value1();

		/**
		 * Returns the second value.
		 *
		 * @return the second value, may be null
		 */
		@Override
		T2 value2();

		/**
		 * Returns the third value.
		 *
		 * @return the third value, may be null
		 */
		@Override
		T3 value3();

		/**
		 * Returns the fourth value.
		 *
		 * @return the fourth value, may be null
		 */
		@Override
		T4 value4();

		/**
		 * Returns the fifth value.
		 *
		 * @return the fifth value, may be null
		 */
		@Override
		T5 value5();
	}

	/**
	 * Creates a new tuple with two values.
	 *
	 * @param <T1>   the type of the first value
	 * @param <T2>   the type of the second value
	 * @param value1 the first value, may be null
	 * @param value2 the second value, may be null
	 * @return a new tuple containing the values
	 */
	static <T1, T2> Tuple2<T1, T2> of(T1 value1, T2 value2) {
		return new Tuple2Record<>(2, value1, value2);
	}

	/**
	 * Creates a new tuple with three values.
	 *
	 * @param <T1>   the type of the first value
	 * @param <T2>   the type of the second value
	 * @param <T3>   the type of the third value
	 * @param value1 the first value, may be null
	 * @param value2 the second value, may be null
	 * @param value3 the third value, may be null
	 * @return a new tuple containing the values
	 */
	static <T1, T2, T3> Tuple3<T1, T2, T3> of(
			T1 value1,
			T2 value2,
			T3 value3) {
		return new Tuple3Record<>(3, value1, value2, value3);
	}

	/**
	 * Creates a new tuple with four values.
	 *
	 * @param <T1>   the type of the first value
	 * @param <T2>   the type of the second value
	 * @param <T3>   the type of the third value
	 * @param <T4>   the type of the fourth value
	 * @param value1 the first value, may be null
	 * @param value2 the second value, may be null
	 * @param value3 the third value, may be null
	 * @param value4 the fourth value, may be null
	 * @return a new tuple containing the values
	 */
	static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> of(
			T1 value1,
			T2 value2,
			T3 value3,
			T4 value4) {
		return new Tuple4Record<>(4, value1, value2, value3, value4);
	}

	/**
	 * Creates a new tuple with five values.
	 *
	 * @param <T1>   the type of the first value
	 * @param <T2>   the type of the second value
	 * @param <T3>   the type of the third value
	 * @param <T4>   the type of the fourth value
	 * @param <T5>   the type of the fifth value
	 * @param value1 the first value, may be null
	 * @param value2 the second value, may be null
	 * @param value3 the third value, may be null
	 * @param value4 the fourth value, may be null
	 * @param value5 the fifth value, may be null
	 * @return a new tuple containing the values
	 */
	static <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> of(
			T1 value1,
			T2 value2,
			T3 value3,
			T4 value4,
			T5 value5) {
		return new Tuple5Record<>(5, value1, value2, value3, value4, value5);
	}

	/**
	 * Returns the first value of this tuple.
	 * <p>
	 * All tuple types support at least two values, so this method will always
	 * return a value. The value may be null if the tuple was created with a null
	 * first value.
	 * </p>
	 *
	 * @return the first value in this tuple, may be null
	 */
	Object value1();

	/**
	 * Returns the second value of this tuple.
	 * <p>
	 * All tuple types support at least two values, so this method will always
	 * return a value. The value may be null if the tuple was created with a null
	 * second value.
	 * </p>
	 *
	 * @return the second value in this tuple, may be null
	 */
	Object value2();

	/**
	 * Returns the third value of this tuple.
	 * <p>
	 * This method is only supported by {@code Tuple3}, {@code Tuple4}, and
	 * {@code Tuple5} implementations. Calling this method on {@code Tuple2} will
	 * throw an {@code UnsupportedOperationException}.
	 * </p>
	 *
	 * @return the third value in this tuple, may be null
	 * @throws UnsupportedOperationException if this tuple type does not support a
	 *                                       third value
	 */
	default Object value3() {
		throw new UnsupportedOperationException("this tuple type does not have a value3");
	}

	/**
	 * Returns the fourth value of this tuple.
	 * <p>
	 * This method is only supported by {@code Tuple4} and {@code Tuple5}
	 * implementations. Calling this method on {@code Tuple2} or {@code Tuple3} will
	 * throw an {@code UnsupportedOperationException}.
	 * </p>
	 *
	 * @return the fourth value in this tuple, may be null
	 * @throws UnsupportedOperationException if this tuple type does not support a
	 *                                       fourth value
	 */
	default Object value4() {
		throw new UnsupportedOperationException("this tuple type does not have a value4");
	}

	/**
	 * Returns the fifth value of this tuple.
	 * <p>
	 * This method is only supported by {@code Tuple5} implementations. Calling this
	 * method on {@code Tuple2}, {@code Tuple3}, or {@code Tuple4} will throw an
	 * {@code UnsupportedOperationException}.
	 * </p>
	 *
	 * @return the fifth value in this tuple, may be null
	 * @throws UnsupportedOperationException if this tuple type does not support a
	 *                                       fifth value
	 */
	default Object value5() {
		throw new UnsupportedOperationException("this tuple type does not have a value5");
	}

	/**
	 * Returns the number of elements in this tuple.
	 * <p>
	 * The size is fixed based on the specific tuple implementation:
	 * <ul>
	 * <li>{@code Tuple2} returns 2
	 * <li>{@code Tuple3} returns 3
	 * <li>{@code Tuple4} returns 4
	 * <li>{@code Tuple5} returns 5
	 * </ul>
	 * </p>
	 *
	 * @return the number of elements in this tuple
	 */
	int size();

	/**
	 * Returns all values of this tuple as an array of Objects.
	 *
	 * @return an array containing all values in this tuple, elements may be null
	 */
	Object[] values();

}
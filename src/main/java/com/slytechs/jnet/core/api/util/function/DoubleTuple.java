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

/**
 * A specialized interface for tuple types that represent a fixed-size
 * collection of double values. Each element in the tuple is a primitive double
 * value.
 * 
 * <p>
 * The record implementations provide standard {@code equals()},
 * {@code hashCode()} and {@code toString()} behavior where:
 * <ul>
 * <li>equals() performs value-based comparison of all components
 * <li>hashCode() combines component hash codes consistently with equals()
 * <li>toString() formats as "DoubleTupleN [value1, value2, ...]"
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
 * // Create tuple with two doubles
 * DoubleTuple2 coord = DoubleTuple2.of(3.14159265359, 2.71828182846);
 * 
 * // Access values
 * double x = coord.value1(); // 3.14159265359
 * double y = coord.value2(); // 2.71828182846
 * double[] all = coord.values(); // [3.14159265359, 2.71828182846]
 * }</pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc
 * @see DoubleTuple2
 * @see DoubleTuple3
 * @see DoubleTuple4
 * @see DoubleTuple5
 */
public interface DoubleTuple {

	/**
	 * A tuple of 2 double elements.
	 */
	interface DoubleTuple2 extends DoubleTuple {

		/**
		 * Creates a new tuple with two double values.
		 *
		 * @param value1 the first value
		 * @param value2 the second value
		 * @return a new tuple containing the values
		 */
		static DoubleTuple2 of(double value1, double value2) {
			return new Tuple2d(value1, value2);
		}

		/**
		 * @see com.slytechs.jnet.core.api.util.function.DoubleTuple#size()
		 */
		@Override
		default int size() {
			return 2;
		}
	}

	/**
	 * A tuple of 3 double elements.
	 */
	interface DoubleTuple3 extends DoubleTuple {

		/**
		 * Creates a new tuple with three double values.
		 *
		 * @param value1 the first value
		 * @param value2 the second value
		 * @param value3 the third value
		 * @return a new tuple containing the values
		 */
		static DoubleTuple3 of(double value1, double value2, double value3) {
			return new Tuple3d(value1, value2, value3);
		}

		/**
		 * @see com.slytechs.jnet.core.api.util.function.DoubleTuple#size()
		 */
		@Override
		default int size() {
			return 3;
		}

		/**
		 * @see com.slytechs.jnet.core.api.util.function.DoubleTuple#value3()
		 */
		@Override
		double value3();
	}

	/**
	 * A tuple of 4 double elements.
	 */
	interface DoubleTuple4 extends DoubleTuple {

		/**
		 * Creates a new tuple with four double values.
		 *
		 * @param value1 the first value
		 * @param value2 the second value
		 * @param value3 the third value
		 * @param value4 the fourth value
		 * @return a new tuple containing the values
		 */
		static DoubleTuple4 of(double value1, double value2, double value3, double value4) {
			return new Tuple4d(value1, value2, value3, value4);
		}

		/**
		 * @see com.slytechs.jnet.core.api.util.function.DoubleTuple#size()
		 */
		@Override
		default int size() {
			return 4;
		}

		/**
		 * @see com.slytechs.jnet.core.api.util.function.DoubleTuple#value3()
		 */
		@Override
		double value3();

		/**
		 * @see com.slytechs.jnet.core.api.util.function.DoubleTuple#value4()
		 */
		@Override
		double value4();
	}

	/**
	 * A tuple of 5 double elements.
	 */
	interface DoubleTuple5 extends DoubleTuple {

		/**
		 * Creates a new tuple with five double values.
		 *
		 * @param value1 the first value
		 * @param value2 the second value
		 * @param value3 the third value
		 * @param value4 the fourth value
		 * @param value5 the fifth value
		 * @return a new tuple containing the values
		 */
		static DoubleTuple5 of(double value1, double value2, double value3, double value4, double value5) {
			return new Tuple5d(value1, value2, value3, value4, value5);
		}

		/**
		 * @see com.slytechs.jnet.core.api.util.function.DoubleTuple#size()
		 */
		@Override
		default int size() {
			return 5;
		}

		/**
		 * @see com.slytechs.jnet.core.api.util.function.DoubleTuple#value3()
		 */
		@Override
		double value3();

		/**
		 * @see com.slytechs.jnet.core.api.util.function.DoubleTuple#value4()
		 */
		@Override
		double value4();

		/**
		 * @see com.slytechs.jnet.core.api.util.function.DoubleTuple#value5()
		 */
		@Override
		double value5();
	}

	/**
	 * Record implementation of DoubleTuple2.
	 *
	 * @param value1 the value 1
	 * @param value2 the value 2
	 */
	record Tuple2d(double value1, double value2) implements DoubleTuple2 {
		
		/**
		 * @see com.slytechs.jnet.core.api.util.function.DoubleTuple#values()
		 */
		@Override
		public double[] values() {
			return new double[] {
					value1,
					value2
			};
		}

		/**
		 * @see java.lang.Record#toString()
		 */
		@Override
		public String toString() {
			return "DoubleTuple2 [" + value1 + ", " + value2 + "]";
		}
	}

	/**
	 * Record implementation of DoubleTuple3.
	 *
	 * @param value1 the value 1
	 * @param value2 the value 2
	 * @param value3 the value 3
	 */
	record Tuple3d(double value1, double value2, double value3) implements DoubleTuple3 {
		
		/**
		 * @see com.slytechs.jnet.core.api.util.function.DoubleTuple#values()
		 */
		@Override
		public double[] values() {
			return new double[] {
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
			return "DoubleTuple3 [" + value1 + ", " + value2 + ", " + value3 + "]";
		}
	}

	/**
	 * Record implementation of DoubleTuple4.
	 *
	 * @param value1 the value 1
	 * @param value2 the value 2
	 * @param value3 the value 3
	 * @param value4 the value 4
	 */
	record Tuple4d(double value1, double value2, double value3, double value4)
			implements DoubleTuple4 {
		
		/**
		 * @see com.slytechs.jnet.core.api.util.function.DoubleTuple#values()
		 */
		@Override
		public double[] values() {
			return new double[] {
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
			return "DoubleTuple4 [" + value1 + ", " + value2 + ", " + value3 + ", " + value4 + "]";
		}
	}

	/**
	 * Record implementation of DoubleTuple5.
	 *
	 * @param value1 the value 1
	 * @param value2 the value 2
	 * @param value3 the value 3
	 * @param value4 the value 4
	 * @param value5 the value 5
	 */
	record Tuple5d(double value1, double value2, double value3, double value4, double value5)
			implements DoubleTuple5 {
		
		/**
		 * @see com.slytechs.jnet.core.api.util.function.DoubleTuple#values()
		 */
		@Override
		public double[] values() {
			return new double[] {
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
			return "DoubleTuple5 [" + value1 + ", " + value2 + ", " + value3 + ", " +
					value4 + ", " + value5 + "]";
		}
	}

	/**
	 * Creates a new tuple with two double values.
	 *
	 * @param value1 the first value
	 * @param value2 the second value
	 * @return a new tuple containing the values
	 */
	static DoubleTuple2 of(double value1, double value2) {
		return new Tuple2d(value1, value2);
	}

	/**
	 * Creates a new tuple with three double values.
	 *
	 * @param value1 the first value
	 * @param value2 the second value
	 * @param value3 the third value
	 * @return a new tuple containing the values
	 */
	static DoubleTuple3 of(double value1, double value2, double value3) {
		return new Tuple3d(value1, value2, value3);
	}

	/**
	 * Creates a new tuple with four double values.
	 *
	 * @param value1 the first value
	 * @param value2 the second value
	 * @param value3 the third value
	 * @param value4 the fourth value
	 * @return a new tuple containing the values
	 */
	static DoubleTuple4 of(double value1, double value2, double value3, double value4) {
		return new Tuple4d(value1, value2, value3, value4);
	}

	/**
	 * Creates a new tuple with five double values.
	 *
	 * @param value1 the first value
	 * @param value2 the second value
	 * @param value3 the third value
	 * @param value4 the fourth value
	 * @param value5 the fifth value
	 * @return a new tuple containing the values
	 */
	static DoubleTuple5 of(double value1, double value2, double value3, double value4, double value5) {
		return new Tuple5d(value1, value2, value3, value4, value5);
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
	 * @return the first double value
	 */
	double value1();

	/**
	 * Returns the second value.
	 *
	 * @return the second double value
	 */
	double value2();

	/**
	 * Returns the third value.
	 *
	 * @return the third double value
	 * @throws UnsupportedOperationException if this tuple type does not support a
	 *                                       third value
	 */
	default double value3() {
		throw new UnsupportedOperationException("this tuple type does not have a value3");
	}

	/**
	 * Returns the fourth value.
	 *
	 * @return the fourth double value
	 * @throws UnsupportedOperationException if this tuple type does not support a
	 *                                       fourth value
	 */
	default double value4() {
		throw new UnsupportedOperationException("this tuple type does not have a value4");
	}

	/**
	 * Returns the fifth value.
	 *
	 * @return the fifth double value
	 * @throws UnsupportedOperationException if this tuple type does not support a
	 *                                       fifth value
	 */
	default double value5() {
		throw new UnsupportedOperationException("this tuple type does not have a value5");
	}

	/**
	 * Returns all values of this tuple as an array of doubles.
	 *
	 * @return an array containing all double values in this tuple
	 */
	double[] values();
}
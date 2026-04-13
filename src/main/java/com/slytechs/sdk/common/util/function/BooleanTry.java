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

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A specialized version of Try for primitive boolean values, optimized for
 * high-performance operations without boxing/unboxing overhead.
 *
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>{@code
 * // Parse and validate flag
 * BooleanTry result = BooleanTry.of(() -> parseFlag(data, offset));
 * 
 * // Transform and validate
 * BooleanTry processed = result
 * 		.map(b -> !b) // Invert flag
 * 		.filter(b -> isValid(b)); // Validate state
 * 
 * // Get value with default
 * boolean value = processed.orElse(false);
 * }</pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public interface BooleanTry {

	/**
	 * A supplier of boolean values that may throw an exception.
	 */
	@FunctionalInterface
	interface ThrowingBooleanSupplier {
		
		/**
		 * Gets the as boolean.
		 *
		 * @return the as boolean
		 * @throws Exception the exception
		 */
		boolean getAsBoolean() throws Exception;
	}

	/**
	 * A consumer of boolean values that may throw an exception.
	 */
	@FunctionalInterface
	interface ThrowingBooleanConsumer {
		
		/**
		 * Accept.
		 *
		 * @param value the value
		 * @throws Exception the exception
		 */
		void accept(boolean value) throws Exception;
	}

	/**
	 * Function that takes a boolean and returns a boolean, may throw exception.
	 */
	@FunctionalInterface
	interface ThrowingBooleanUnaryOperator {
		
		/**
		 * Apply as boolean.
		 *
		 * @param value the value
		 * @return true, if successful
		 * @throws Exception the exception
		 */
		boolean applyAsBoolean(boolean value) throws Exception;
	}

	/**
	 * Record implementation for BooleanTry that stores either a boolean value or
	 * failure.
	 *
	 * @param value   the value
	 * @param failure the failure
	 */
	record BooleanTryRecord(boolean value, @Nullable Exception failure) implements BooleanTry {

		/**
		 * Instantiates a new boolean try record.
		 *
		 * @param value   the value
		 * @param failure the failure
		 */
		public BooleanTryRecord {
			if (!isSuccess() && failure == null) {
				throw new IllegalArgumentException("Failure case requires non-null exception");
			}
		}

		/**
		 * @see com.slytechs.sdk.common.util.function.BooleanTry#isSuccess()
		 */
		@Override
		public boolean isSuccess() {
			return failure == null;
		}

		/**
		 * @see java.lang.Record#toString()
		 */
		@Override
		public String toString() {
			return isSuccess()
					? "BooleanSuccess[" + value + "]"
					: "BooleanFailure[" + failure + "]";
		}
	}

	/**
	 * Creates a new successful BooleanTry.
	 *
	 * @param value the success value
	 * @return a new successful BooleanTry
	 */
	static BooleanTry success(boolean value) {
		return new BooleanTryRecord(value, null);
	}

	/**
	 * Creates a new failed BooleanTry.
	 *
	 * @param exception the failure exception
	 * @return a new failed BooleanTry
	 */
	static BooleanTry failure(Exception exception) {
		return new BooleanTryRecord(false, exception);
	}

	/**
	 * Attempts to execute the given supplier and wrap its result.
	 *
	 * @param supplier the operation to attempt
	 * @return a BooleanTry containing either the result or any thrown exception
	 */
	static BooleanTry of(ThrowingBooleanSupplier supplier) {
		try {
			return success(supplier.getAsBoolean());
		} catch (Exception e) {
			return failure(e);
		}
	}

	/**
	 * Returns whether this is a successful BooleanTry.
	 *
	 * @return true if this is a success, false if it's a failure
	 */
	boolean isSuccess();

	/**
	 * Returns whether this is a failed BooleanTry.
	 *
	 * @return true if this is a failure, false if it's a success
	 */
	default boolean isFailure() {
		return !isSuccess();
	}

	/**
	 * Executes the given action if this is a success.
	 *
	 * @param action the action to execute with the boolean value
	 * @return this BooleanTry for method chaining
	 */
	default BooleanTry ifSuccess(ThrowingBooleanConsumer action) {
		if (isSuccess()) {
			try {
				action.accept(value());
			} catch (Exception e) {
				return failure(e);
			}
		}
		return this;
	}

	/**
	 * Executes the given action if this is a failure.
	 *
	 * @param action the action to execute with the failure exception
	 * @return this BooleanTry for method chaining
	 */
	default BooleanTry ifFailure(Consumer<? super Exception> action) {
		if (isFailure()) {
			action.accept(failure());
		}
		return this;
	}

	/**
	 * Maps the boolean value using the given operator.
	 *
	 * @param mapper the function to apply to the value
	 * @return a new BooleanTry with either the mapped value or the original failure
	 */
	default BooleanTry map(ThrowingBooleanUnaryOperator mapper) {
		if (isSuccess()) {
			try {
				return success(mapper.applyAsBoolean(value()));
			} catch (Exception e) {
				return failure(e);
			}
		}
		return this;
	}

	/**
	 * Returns the boolean value or throws the failure exception.
	 *
	 * @return the boolean value if present
	 * @throws Exception the failure exception if this is a failure
	 */
	default boolean getAsBoolean() throws Exception {
		if (isSuccess()) {
			return value();
		}
		throw failure();
	}

	/**
	 * Returns the boolean value or the given default value.
	 *
	 * @param other the value to return if this is a failure
	 * @return the boolean value or the default value
	 */
	default boolean orElse(boolean other) {
		return isSuccess() ? value() : other;
	}

	/**
	 * Returns the boolean value or gets a value from the supplier.
	 *
	 * @param supplier the supplier of the value to return if this is a failure
	 * @return the boolean value or the supplied value
	 */
	default boolean orElseGet(BooleanSupplier supplier) {
		return isSuccess() ? value() : supplier.getAsBoolean();
	}

	/**
	 * Returns the boolean value or throws the given exception.
	 *
	 * @param <X>               the type of exception to throw
	 * @param exceptionSupplier the supplier of the exception
	 * @return the boolean value if present
	 * @throws X if this is a failure
	 */
	default <X extends Throwable> boolean orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
		if (isSuccess()) {
			return value();
		}
		throw exceptionSupplier.get();
	}

	/**
	 * Returns the primitive boolean value.
	 *
	 * @return the boolean value
	 */
	boolean value();

	/**
	 * Returns the failure exception if present.
	 *
	 * @return the failure exception, null if this is a success
	 */
	@Nullable
	Exception failure();

	/**
	 * Lifts a throwing boolean consumer into a safe consumer.
	 *
	 * @param c the consumer that may throw
	 * @return a consumer that handles exceptions
	 */
	static Consumer<Boolean> liftConsumer(ThrowingBooleanConsumer c) {
		return value -> {
			try {
				c.accept(value);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
	}

	/**
	 * Lifts a throwing boolean consumer into a safe consumer with error handling.
	 *
	 * @param c            the consumer that may throw
	 * @param errorHandler handles any exceptions
	 * @return a consumer that safely handles exceptions
	 */
	static Consumer<Boolean> liftConsumer(ThrowingBooleanConsumer c, Consumer<Exception> errorHandler) {
		return value -> {
			try {
				c.accept(value);
			} catch (Exception e) {
				errorHandler.accept(e);
			}
		};
	}
}
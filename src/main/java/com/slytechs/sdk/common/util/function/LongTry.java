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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.LongUnaryOperator;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import java.util.stream.LongStream;

/**
 * A specialized version of Try for primitive long values, optimized for
 * high-performance operations without boxing/unboxing overhead.
 *
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>{@code
 * // Parse packet timestamp
 * LongTry timestamp = LongTry.of(() -> parseTimestamp(data, offset));
 * 
 * // Transform and validate
 * LongTry processed = timestamp
 * 		.filter(t -> t > 0) // Must be positive
 * 		.map(t -> t / 1000) // Convert to seconds
 * 		.fromNetworkOrder(); // Convert byte order
 * 
 * // Simple error handling
 * if (processed.isFailure()) {
 * 	log.error("Invalid timestamp: {}", processed.failure().getMessage());
 * 	return LongTry.failure(processed.failure());
 * }
 * 
 * // Get value or use current time
 * long time = processed.orElseGet(System::currentTimeMillis);
 * }</pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public interface LongTry {

	/**
	 * Creates a new successful LongTry with the given value.
	 *
	 * @param value the success value
	 * @return a new successful LongTry
	 */
	static LongTry success(long value) {
		return new LongTryRecord(value, null);
	}

	/**
	 * Creates a new failed LongTry with the given exception.
	 *
	 * @param exception the failure exception, must not be null
	 * @return a new failed LongTry
	 * @throws IllegalArgumentException if exception is null
	 */
	static LongTry failure(Exception exception) {
		return new LongTryRecord(0, exception);
	}

	/**
	 * Attempts to execute the given supplier and wrap its long result.
	 *
	 * @param supplier the operation to attempt
	 * @return an LongTry containing either the operation's result or any thrown
	 *         exception
	 */
	static LongTry liftSupplier(ThrowingLongSupplier supplier) {
		try {
			return success(supplier.getAsLong());
		} catch (Exception e) {
			return failure(e);
		}
	}

	/**
	 * Returns whether this is a successful LongTry.
	 *
	 * @return true if this is a success, false if it's a failure
	 */
	boolean isSuccess();

	/**
	 * Returns whether this is a failed LongTry.
	 *
	 * @return true if this is a failure, false if it's a success
	 */
	default boolean isFailure() {
		return !isSuccess();
	}

	/**
	 * Executes the given action if this is a success.
	 *
	 * @param action the action to execute with the long value
	 * @return this LongTry for method chaining
	 */
	default LongTry ifSuccess(LongConsumer action) {
		if (isSuccess()) {
			action.accept(value());
		}
		return this;
	}

	/**
	 * Executes the given action if this is a failure.
	 *
	 * @param action the action to execute with the failure exception
	 * @return this LongTry for method chaining
	 */
	default LongTry ifFailure(Consumer<? super Exception> action) {
		if (isFailure()) {
			action.accept(failure());
		}
		return this;
	}

	/**
	 * Maps the long value using the given operator if this is a success.
	 *
	 * @param mapper the function to apply to the value
	 * @return a new LongTry with either the mapped value or the original failure
	 */
	default LongTry map(LongUnaryOperator mapper) {
		return isSuccess()
				? LongTry.success(mapper.applyAsLong(value()))
				: this;
	}

	/**
	 * Maps the long value to a new LongTry if this is a success.
	 *
	 * @param mapper the function to apply to the value
	 * @return the mapped LongTry or the original failure
	 */
	default LongTry flatMap(LongFunction<? extends LongTry> mapper) {
		return isSuccess()
				? mapper.apply(value())
				: this;
	}

	/**
	 * Filters the long value using the given predicate.
	 *
	 * @param predicate the predicate to test the value
	 * @return this LongTry if it's a failure or the predicate returns true,
	 *         otherwise a failure with IllegalStateException
	 */
	default LongTry filter(LongPredicate predicate) {
		if (isSuccess() && !predicate.test(value())) {
			return LongTry.failure(new IllegalStateException("Predicate does not match for: " + value()));
		}
		return this;
	}

	/**
	 * Returns the long value or throws the failure exception.
	 *
	 * @return the long value if present
	 * @throws Exception the failure exception if this is a failure
	 */
	default long getAsLong() throws Exception {
		if (isSuccess()) {
			return value();
		}
		throw failure();
	}

	/**
	 * Returns the long value or the given default value.
	 *
	 * @param other the value to return if this is a failure
	 * @return the long value or the default value
	 */
	default long orElse(long other) {
		return isSuccess() ? value() : other;
	}

	/**
	 * Returns the long value or gets a value from the supplier.
	 *
	 * @param supplier the supplier of the value to return if this is a failure
	 * @return the long value or the supplied value
	 */
	default long orElseGet(LongSupplier supplier) {
		return isSuccess() ? value() : supplier.getAsLong();
	}

	/**
	 * Returns the long value or throws the given exception.
	 *
	 * @param <X>               the type of exception to throw
	 * @param exceptionSupplier the supplier of the exception
	 * @return the long value if present
	 * @throws X if this is a failure
	 */
	default <X extends Throwable> long orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
		if (isSuccess()) {
			return value();
		}
		throw exceptionSupplier.get();
	}

	/**
	 * Converts this LongTry to an LongStream.
	 *
	 * @return an LongStream containing the value, or an empty LongStream if this is
	 *         a failure
	 */
	default LongStream stream() {
		return isSuccess() ? LongStream.of(value()) : LongStream.empty();
	}

	/**
	 * Applies a bit mask to the long value if this is a success. Commonly used for
	 * extracting specific fields from network packet data.
	 *
	 * @param mask the bit mask to apply
	 * @return a new LongTry with either the masked value or the original failure
	 */
	default LongTry maskBits(long mask) {
		return map(v -> v & mask);
	}

	/**
	 * Validates that the long value falls within the specified range. Useful for
	 * validating packet field values against protocol specifications.
	 *
	 * @param min the minimum allowed value (inclusive)
	 * @param max the maximum allowed value (inclusive)
	 * @return this LongTry if the value is within range, or a failure if outside
	 *         range
	 */
	default LongTry validateRange(long min, long max) {
		return filter(v -> v >= min && v <= max)
				.mapFailure(e -> new IllegalValueException(
						String.format("Value %d outside valid range [%d, %d]", value(), min, max)));
	}

	/**
	 * Converts a network byte order (big-endian) value to host byte order.
	 * Essential for correctly interpreting multi-byte fields in network protocols.
	 *
	 * @return a new LongTry with the byte-swapped value or the original failure
	 */
	default LongTry fromNetworkOrder() {
		return map(Long::reverseBytes);
	}

	/**
	 * Converts a host byte order value to network byte order (big-endian). Used
	 * when preparing values for network transmission.
	 *
	 * @return a new LongTry with the byte-swapped value or the original failure
	 */
	default LongTry toNetworkOrder() {
		return map(Long::reverseBytes);
	}

	/**
	 * Extracts a specific byte from the long value. Useful for processing
	 * multi-byte protocol fields.
	 *
	 * @param byteIndex the index of the byte to extract (0-7, where 0 is least
	 *                  significant)
	 * @return a new LongTry with the extracted byte or a failure if index is
	 *         invalid
	 */
	default LongTry extractByte(int byteIndex) {
		if (byteIndex < 0 || byteIndex > 7) {
			return failure(new IllegalArgumentException("Byte index must be between 0 and 7"));
		}
		return map(v -> (v >> (byteIndex * 8)) & 0xFF);
	}

	/**
	 * Maps a failure to a different exception type. Useful for converting generic
	 * exceptions to protocol-specific ones.
	 *
	 * @param mapper the function to convert the exception
	 * @return this LongTry if success, or a new failure with the mapped exception
	 */
	default LongTry mapFailure(Function<Exception, Exception> mapper) {
		if (isSuccess()) {
			return this;
		}
		return failure(mapper.apply(failure()));
	}

	/**
	 * Returns the primitive long value. Only call when isSuccess() is true.
	 *
	 * @return the long value
	 */
	long value();

	/**
	 * Returns the failure exception if present.
	 *
	 * @return the failure exception, null if this is a success
	 */
	@Nullable
	Exception failure();

	/**
	 * Lifts a throwing long-to-long function into a safe function.
	 *
	 * @param f the function that may throw
	 * @return a function that returns a LongTry
	 */
	static ToLongFunction<Long> lift(ThrowingToLongFunction<Long> f) {
		return value -> {
			try {
				return f.applyAsLong(value);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
	}

	/**
	 * Lifts a throwing long consumer into a safe consumer.
	 *
	 * @param c the consumer that may throw
	 * @return a consumer that handles exceptions
	 */
	static LongConsumer liftConsumer(ThrowingLongConsumer c) {
		return value -> {
			try {
				c.accept(value);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
	}

	/**
	 * Lifts a throwing long consumer into a safe consumer with error handling.
	 *
	 * @param c            the consumer that may throw
	 * @param errorHandler handles any exceptions
	 * @return a consumer that safely handles exceptions
	 */
	static LongConsumer liftConsumer(ThrowingLongConsumer c, Consumer<Exception> errorHandler) {
		return value -> {
			try {
				c.accept(value);
			} catch (Exception e) {
				errorHandler.accept(e);
			}
		};
	}

	/**
	 * Maps this long using a throwing function.
	 *
	 * @param f the function that may throw
	 * @return a new LongTry with the mapped value
	 */
	default LongTry mapThrowing(ThrowingToLongFunction<Long> f) {
		if (isFailure()) {
			return LongTry.failure(failure());
		}
		try {
			return LongTry.success(f.applyAsLong(value()));
		} catch (Exception e) {
			return LongTry.failure(e);
		}
	}

	/**
	 * Executes a throwing consumer if this is a success.
	 *
	 * @param c the consumer that may throw
	 * @return this LongTry for chaining
	 */
	default LongTry ifSuccessThrowing(ThrowingLongConsumer c) {
		if (isSuccess()) {
			try {
				c.accept(value());
			} catch (Exception e) {
				return LongTry.failure(e);
			}
		}
		return this;
	}
}
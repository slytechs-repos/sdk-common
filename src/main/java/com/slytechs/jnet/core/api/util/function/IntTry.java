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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

/**
 * A specialized version of Try for primitive int values, optimized for
 * high-performance operations without boxing/unboxing overhead.
 *
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>{@code
 * // Parse packet header field
 * IntTry result = IntTry.of(() -> parsePacketField(data, offset));
 * 
 * // Basic error handling
 * if (result.isFailure()) {
 * 	log.error("Failed to parse field: {}", result.failure().getMessage());
 * 	return IntTry.failure(result.failure());
 * }
 * 
 * // Validate and transform
 * IntTry processed = result
 * 		.filter(v -> v >= 0) // Must be positive
 * 		.map(v -> v & 0xFF) // Mask to byte
 * 		.fromNetworkOrder(); // Convert byte order
 * 
 * // Get value with default
 * int value = processed.orElse(-1);
 * }</pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public interface IntTry {

	/**
	 * Creates a new successful IntTry with the given value.
	 *
	 * @param value the success value
	 * @return a new successful IntTry
	 */
	static IntTry success(int value) {
		return new IntTryRecord(value, null);
	}

	/**
	 * Creates a new failed IntTry with the given exception.
	 *
	 * @param exception the failure exception, must not be null
	 * @return a new failed IntTry
	 * @throws IllegalArgumentException if exception is null
	 */
	static IntTry failure(Exception exception) {
		return new IntTryRecord(0, exception);
	}

	/**
	 * Attempts to execute the given supplier and wrap its int result.
	 *
	 * @param supplier the operation to attempt
	 * @return an IntTry containing either the operation's result or any thrown
	 *         exception
	 */
	static IntTry liftSupplier(ThrowingIntSupplier supplier) {
		try {
			return success(supplier.getAsInt());
		} catch (Exception e) {
			return failure(e);
		}
	}

	/**
	 * Returns whether this is a successful IntTry.
	 *
	 * @return true if this is a success, false if it's a failure
	 */
	boolean isSuccess();

	/**
	 * Returns whether this is a failed IntTry.
	 *
	 * @return true if this is a failure, false if it's a success
	 */
	default boolean isFailure() {
		return !isSuccess();
	}

	/**
	 * Executes the given action if this is a success.
	 *
	 * @param action the action to execute with the int value
	 * @return this IntTry for method chaining
	 */
	default IntTry ifSuccess(IntConsumer action) {
		if (isSuccess()) {
			action.accept(value());
		}
		return this;
	}

	/**
	 * Executes the given action if this is a failure.
	 *
	 * @param action the action to execute with the failure exception
	 * @return this IntTry for method chaining
	 */
	default IntTry ifFailure(Consumer<? super Exception> action) {
		if (isFailure()) {
			action.accept(failure());
		}
		return this;
	}

	/**
	 * Maps the int value using the given operator if this is a success.
	 *
	 * @param mapper the function to apply to the value
	 * @return a new IntTry with either the mapped value or the original failure
	 */
	default IntTry map(IntUnaryOperator mapper) {
		return isSuccess()
				? IntTry.success(mapper.applyAsInt(value()))
				: this;
	}

	/**
	 * Maps the int value to a new IntTry if this is a success.
	 *
	 * @param mapper the function to apply to the value
	 * @return the mapped IntTry or the original failure
	 */
	default IntTry flatMap(IntFunction<? extends IntTry> mapper) {
		return isSuccess()
				? mapper.apply(value())
				: this;
	}

	/**
	 * Filters the int value using the given predicate.
	 *
	 * @param predicate the predicate to test the value
	 * @return this IntTry if it's a failure or the predicate returns true,
	 *         otherwise a failure with IllegalStateException
	 */
	default IntTry filter(IntPredicate predicate) {
		if (isSuccess() && !predicate.test(value())) {
			return IntTry.failure(new IllegalStateException("Predicate does not match for: " + value()));
		}
		return this;
	}

	/**
	 * Returns the int value or throws the failure exception.
	 *
	 * @return the int value if present
	 * @throws Exception the failure exception if this is a failure
	 */
	default int getAsInt() throws Exception {
		if (isSuccess()) {
			return value();
		}
		throw failure();
	}

	/**
	 * Returns the int value or the given default value.
	 *
	 * @param other the value to return if this is a failure
	 * @return the int value or the default value
	 */
	default int orElse(int other) {
		return isSuccess() ? value() : other;
	}

	/**
	 * Returns the int value or gets a value from the supplier.
	 *
	 * @param supplier the supplier of the value to return if this is a failure
	 * @return the int value or the supplied value
	 */
	default int orElseGet(IntSupplier supplier) {
		return isSuccess() ? value() : supplier.getAsInt();
	}

	/**
	 * Returns the int value or throws the given exception.
	 *
	 * @param <X>               the type of exception to throw
	 * @param exceptionSupplier the supplier of the exception
	 * @return the int value if present
	 * @throws X if this is a failure
	 */
	default <X extends Throwable> int orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
		if (isSuccess()) {
			return value();
		}
		throw exceptionSupplier.get();
	}

	/**
	 * Converts this IntTry to an IntStream.
	 *
	 * @return an IntStream containing the value, or an empty IntStream if this is a
	 *         failure
	 */
	default IntStream stream() {
		return isSuccess() ? IntStream.of(value()) : IntStream.empty();
	}

	/**
	 * Returns the primitive int value. Only call when isSuccess() is true.
	 *
	 * @return the int value
	 */
	int value();

	/**
	 * Returns the failure exception if present.
	 *
	 * @return the failure exception, null if this is a success
	 */
	@Nullable
	Exception failure();

	/**
	 * Applies a bit mask to the int value if this is a success. Commonly used for
	 * extracting specific fields from network packet data.
	 *
	 * @param mask the bit mask to apply
	 * @return a new IntTry with either the masked value or the original failure
	 */
	default IntTry maskBits(int mask) {
		return map(v -> v & mask);
	}

	/**
	 * Validates that the int value falls within the specified range. Useful for
	 * validating packet field values against protocol specifications.
	 *
	 * @param min the minimum allowed value (inclusive)
	 * @param max the maximum allowed value (inclusive)
	 * @return this IntTry if the value is within range, or a failure if outside
	 *         range
	 */
	default IntTry validateRange(int min, int max) {
		return filter(v -> v >= min && v <= max)
				.mapFailure(e -> new IllegalValueException(
						String.format("Value %d outside valid range [%d, %d]", value(), min, max)));
	}

	/**
	 * Converts a network byte order (big-endian) value to host byte order.
	 * Essential for correctly interpreting multi-byte fields in network protocols.
	 *
	 * @return a new IntTry with the byte-swapped value or the original failure
	 */
	default IntTry fromNetworkOrder() {
		return map(Integer::reverseBytes);
	}

	/**
	 * Converts a host byte order value to network byte order (big-endian). Used
	 * when preparing values for network transmission.
	 *
	 * @return a new IntTry with the byte-swapped value or the original failure
	 */
	default IntTry toNetworkOrder() {
		return map(Integer::reverseBytes);
	}

	/**
	 * Extracts a specific byte from the int value. Useful for processing multi-byte
	 * protocol fields.
	 *
	 * @param byteIndex the index of the byte to extract (0-3, where 0 is least
	 *                  significant)
	 * @return a new IntTry with the extracted byte or a failure if index is invalid
	 */
	default IntTry extractByte(int byteIndex) {
		if (byteIndex < 0 || byteIndex > 3) {
			return failure(new IllegalArgumentException("Byte index must be between 0 and 3"));
		}
		return map(v -> (v >> (byteIndex * 8)) & 0xFF);
	}

	/**
	 * Extracts a specific bit field from the int value. Useful for processing
	 * protocol flags and bit fields.
	 *
	 * @param offset the starting bit position (0-31)
	 * @param length the number of bits to extract (1-32)
	 * @return a new IntTry with the extracted bits or a failure if parameters are
	 *         invalid
	 */
	default IntTry extractBits(int offset, int length) {
		if (offset < 0 || offset > 31 || length < 1 || length > 32 || (offset + length) > 32) {
			return failure(new IllegalArgumentException(
					"Invalid bit extraction parameters: offset=" + offset + ", length=" + length));
		}
		return map(v -> (v >> offset) & ((1 << length) - 1));
	}

	/**
	 * Maps a failure to a different exception type. Useful for converting generic
	 * exceptions to protocol-specific ones.
	 *
	 * @param mapper the function to convert the exception
	 * @return this IntTry if success, or a new failure with the mapped exception
	 */
	default IntTry mapFailure(Function<Exception, Exception> mapper) {
		if (isSuccess()) {
			return this;
		}
		return failure(mapper.apply(failure()));
	}

	/**
	 * Validates this value represents a valid port number (0-65535).
	 * 
	 * @return this IntTry if the value is a valid port, or a failure if invalid
	 */
	default IntTry validatePortNumber() {
		return validateRange(0, 65535)
				.mapFailure(e -> new IllegalValueException("Invalid port number: " + value()));
	}

	/**
	 * Validates this value represents a valid IPv4 address octet (0-255).
	 * 
	 * @return this IntTry if the value is a valid octet, or a failure if invalid
	 */
	default IntTry validateIpv4Octet() {
		return validateRange(0, 255)
				.mapFailure(e -> new IllegalValueException("Invalid IPv4 octet: " + value()));
	}

	/**
	 * Lifts a throwing int-to-int function into a safe function.
	 *
	 * @param f the function that may throw
	 * @return a function that returns an IntTry
	 */
	static ToIntFunction<Integer> lift(ThrowingToIntFunction<Integer> f) {
		return value -> {
			try {
				return f.applyAsInt(value);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
	}

	/**
	 * Lifts a throwing int consumer into a safe consumer.
	 *
	 * @param c the consumer that may throw
	 * @return a consumer that handles exceptions
	 */
	static IntConsumer liftConsumer(ThrowingIntConsumer c) {
		return value -> {
			try {
				c.accept(value);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
	}

	/**
	 * Lifts a throwing int consumer into a safe consumer with error handling.
	 *
	 * @param c            the consumer that may throw
	 * @param errorHandler handles any exceptions
	 * @return a consumer that safely handles exceptions
	 */
	static IntConsumer liftConsumer(ThrowingIntConsumer c, Consumer<Exception> errorHandler) {
		return value -> {
			try {
				c.accept(value);
			} catch (Exception e) {
				errorHandler.accept(e);
			}
		};
	}

	/**
	 * Maps this int using a throwing function.
	 *
	 * @param f the function that may throw
	 * @return a new IntTry with the mapped value
	 */
	default IntTry mapThrowing(ThrowingToIntFunction<Integer> f) {
		if (isFailure()) {
			return IntTry.failure(failure());
		}
		try {
			return IntTry.success(f.applyAsInt(value()));
		} catch (Exception e) {
			return IntTry.failure(e);
		}
	}

	/**
	 * Executes a throwing consumer if this is a success.
	 *
	 * @param c the consumer that may throw
	 * @return this IntTry for chaining
	 */
	default IntTry ifSuccessThrowing(ThrowingIntConsumer c) {
		if (isSuccess()) {
			try {
				c.accept(value());
			} catch (Exception e) {
				return IntTry.failure(e);
			}
		}
		return this;
	}
}
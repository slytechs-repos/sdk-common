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

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A monadic container type that represents the result of an operation that may
 * either succeed with a value or fail with an exception. The Try type is
 * designed to handle computations that can potentially fail, providing a more
 * functional approach to error handling compared to traditional try-catch
 * blocks.
 *
 * <p>
 * The Try type has two possible states:
 * </p>
 * <ul>
 * <li>Success: Contains a value of type T</li>
 * <li>Failure: Contains an Exception that occurred during the computation</li>
 * </ul>
 *
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>Chainable operations using map, flatMap, and filter</li>
 * <li>Composition of multiple Try instances using combineWith</li>
 * <li>Conversion to primitive type variants (IntTry, LongTry)</li>
 * <li>Integration with Optional and Stream APIs</li>
 * <li>Support for recovery from failures</li>
 * </ul>
 *
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>{@code
 * // Parse message
 * Try<Message> result = Try.of(() -> parseMessage(data));
 * 
 * // Basic error handling
 * if (result.isFailure()) {
 * 	log.error("Parse failed: {}", result.failure().getMessage());
 * 	return Try.failure(result.failure());
 * }
 * 
 * // Transform message
 * Try<Response> response = result
 * 		.map(Message::process)
 * 		.map(Response::new);
 * 
 * // Combine host and port
 * Try<String> host = Try.of(() -> getHost());
 * Try<Integer> port = Try.of(() -> getPort());
 * 
 * Try<Connection> conn = host.combineWith(
 * 		port,
 * 		(h, p) -> connect(h, p));
 * 
 * // Convert to primitive
 * Try<Integer> size = Try.of(() -> message.getSize());
 * IntTry intSize = size.toIntTry();
 * 
 * // Get value or default
 * Connection connection = conn.orElse(defaultConnection);
 * }</pre>
 *
 * @param <T> the type of the success value
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see Optional
 * @see Stream
 */
public interface Try<T> {

	/**
	 * A specialized exception class that wraps unchecked exceptions that occur
	 * during Try operations. This exception serves as a bridge between checked and
	 * unchecked exceptions in the Try monad, allowing uniform handling of all
	 * exception types.
	 * 
	 * <p>
	 * Key characteristics:
	 * </p>
	 * <ul>
	 * <li>Preserves the original exception as the cause</li>
	 * <li>Maintains the exception chain for debugging</li>
	 * <li>Serializable for distributed systems</li>
	 * <li>Designed specifically for Try monad error handling</li>
	 * </ul>
	 * 
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * try {
	 * 	// Some operation that might throw a RuntimeException
	 * 	performRiskyOperation();
	 * } catch (RuntimeException e) {
	 * 	// Wrap the unchecked exception
	 * 	throw new Try.UncheckedException(e);
	 * }
	 * 
	 * // In error handling code:
	 * Try<Result> result = Try.ofSupplier(() -> {
	 * 	try {
	 * 		return computeResult();
	 * 	} catch (RuntimeException e) {
	 * 		throw new Try.UncheckedException(e);
	 * 	}
	 * });
	 * 
	 * // Handle specific unchecked exceptions
	 * result.ifFailure(UncheckedException.class, e -> {
	 * 	Throwable cause = e.getCause();
	 * 	if (cause instanceof IllegalArgumentException) {
	 * 		// Handle invalid argument
	 * 		handleInvalidArgument((IllegalArgumentException) cause);
	 * 	} else if (cause instanceof NullPointerException) {
	 * 		// Handle null pointer
	 * 		handleNullPointer((NullPointerException) cause);
	 * 	}
	 * });
	 * }</pre>
	 */
	public class UncheckedException extends Exception {

		private static final long serialVersionUID = 3007861571844188541L;

		/**
		 * @param cause
		 */
		public UncheckedException(Throwable cause) {
			super(cause);
		}

	}

	/**
	 * Creates a new failed Try instance with the given exception.
	 *
	 * <p>
	 * This static factory method constructs a Try instance representing a failed
	 * computation. The provided exception represents the reason for the failure and
	 * can be retrieved later using {@link #failure()}.
	 * </p>
	 *
	 * @param <T>       the type parameter of the Try
	 * @param exception the exception that caused the failure, must not be null
	 * @return a new Try instance representing a failure
	 * @throws NullPointerException if the exception is null
	 */
	static <T> Try<T> failure(Exception exception) {
		return new TryFailureRecord<>(exception);
	}

	/**
	 * Creates a Try from a Callable.
	 *
	 * <p>
	 * This method executes the given Callable immediately and returns its result
	 * wrapped in a Try. Any exception thrown by the Callable will be caught and
	 * wrapped in a failed Try.
	 * </p>
	 *
	 * @param <T>      the type of value returned by the Callable
	 * @param callable the Callable to execute
	 * @return a Try containing either the callable's result or any thrown exception
	 */
	static <T> Try<T> ofCallable(Callable<T> callable) {
		try {
			return success(callable.call());
		} catch (Exception e) {
			return failure(e);
		}
	}

	/**
	 * Creates a Try by applying a throwing function to an input value.
	 *
	 * @param <T>      the input type
	 * @param <R>      the result type
	 * @param input    the input value
	 * @param function the function to apply
	 * @return a Try containing the function result or any thrown exception
	 */
	static <T, R> Try<R> ofFunction(T input, ThrowingFunction<T, R> function) {
		try {
			return success(function.apply(input));
		} catch (Exception e) {
			return failure(e);
		}
	}

	/**
	 * Creates a Try from a ThrowingRunnable.
	 *
	 * <p>
	 * This method executes the given runnable and returns a Try&lt;Void&gt;. Any
	 * exception thrown by the runnable will be caught and wrapped in a failed Try.
	 * </p>
	 *
	 * @param runnable the runnable to execute
	 * @return a Try representing the execution result
	 */
	static Try<Void> ofRunnable(ThrowingRunnable runnable) {
		try {
			runnable.run();
			return success(null);
		} catch (Exception e) {
			return failure(e);
		}
	}

	/**
	 * Creates a Try from a ThrowingSupplier.
	 *
	 * @param <T>      the type of value supplied
	 * @param supplier the supplier that may throw
	 * @return a Try containing the supplied value or any thrown exception
	 */
	static <T> Try<T> ofSupplier(ThrowingSupplier<T> supplier) {
		try {
			return success(supplier.get());
		} catch (Exception e) {
			return failure(e);
		}
	}

	/**
	 * Creates a Try by applying a throwing unary operator to an input value.
	 *
	 * @param <T>      the type of the input and result
	 * @param input    the input value
	 * @param operator the operator to apply
	 * @return a Try containing the operator result or any thrown exception
	 */
	static <T> Try<T> ofUnaryOperator(T input, ThrowingUnaryOperator<T> operator) {
		try {
			return success(operator.apply(input));
		} catch (Exception e) {
			return failure(e);
		}
	}

	/**
	 * Creates a new successful Try with the given value.
	 *
	 * @param <T>   the type of the success value
	 * @param value the success value, must not be null
	 * @return a new successful Try
	 * @throws NullPointerException if value is null
	 */
	static <T> Try<T> success(T value) {
		return new TrySuccessRecord<>(value);
	}

	/**
	 * Wraps a Callable in a Callable that returns a Try.
	 *
	 * <p>
	 * This method provides integration with the {@link Callable} interface commonly
	 * used in concurrent programming. The returned Callable will execute the
	 * original task and wrap its result or exception in a Try instance.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * Callable<String> task = () -> {
	 * 	// Some potentially failing operation
	 * 	return processData();
	 * };
	 * 
	 * Callable<Try<String>> safeTask = Try.wrapCallable(task);
	 * Try<String> result = safeTask.call();
	 * }</pre>
	 *
	 * @param <T>  the type of value returned by the Callable
	 * @param task the Callable to wrap
	 * @return a Callable that returns Try instances
	 */
	static <T> Callable<Try<T>> wrapCallable(Callable<T> task) {
		return () -> Try.wrapSupplier(() -> task.call()).get();
	}

	/**
	 * Transforms a throwing function into a function that returns a Try.
	 *
	 * <p>
	 * This method is useful for lifting functions that may throw exceptions into
	 * the Try monad. The resulting function will never throw exceptions; instead,
	 * it wraps any thrown exception in a failed Try.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * ThrowingFunction<String, Integer> parser = Integer::parseInt;
	 * Function<String, Try<Integer>> safeParse = Try.wrapFunction(parser);
	 * 
	 * Try<Integer> result = safeParse.apply("123"); // Success(123)
	 * Try<Integer> error = safeParse.apply("abc"); // Failure(NumberFormatException)
	 * }</pre>
	 *
	 * @param <T> the input type of the function
	 * @param <R> the return type of the function
	 * @param f   the function to wrap that may throw exceptions
	 * @return a function that returns results wrapped in Try instances
	 */
	static <T, R> Function<T, Try<R>> wrapFunction(ThrowingFunction<T, R> f) {
		return t -> Try.wrapSupplier(() -> f.apply(t)).get();
	}

	/**
	 * Wraps a throwing supplier in a supplier that returns a Try.
	 *
	 * <p>
	 * This method transforms a supplier that may throw exceptions into one that
	 * safely returns a Try instance. If the original supplier throws an exception,
	 * it will be caught and wrapped in a failed Try.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * ThrowingSupplier<String> riskyRead = () -> Files.readString(Path.of("file.txt"));
	 * Supplier<Try<String>> safeRead = Try.wrapSupplier(riskyRead);
	 * 
	 * Try<String> result = safeRead.get();
	 * }</pre>
	 *
	 * @param <T>      the type of value supplied
	 * @param supplier the supplier that may throw exceptions
	 * @return a supplier that returns Try instances
	 */
	static <T> Supplier<Try<T>> wrapSupplier(ThrowingSupplier<T> supplier) {
		try {
			T value = supplier.get();
			return () -> success(value);
		} catch (Exception e) {
			return () -> failure(e);
		}
	}

	/**
	 * Combines this Try with another Try using a combining function.
	 *
	 * <p>
	 * This method allows combining two Try instances into a single Try containing
	 * the result of applying the combiner function to both success values. If
	 * either Try is a failure, the combined result will be a failure.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * Try<String> host = Try.success("localhost");
	 * Try<Integer> port = Try.success(8080);
	 * 
	 * Try<URI> uri = host.combineWith(
	 * 		port,
	 * 		(h, p) -> new URI("http", null, h, p, "/", null, null));
	 * }</pre>
	 *
	 * @param <U>      the type of the other Try
	 * @param <R>      the type of the result
	 * @param other    the other Try to combine with
	 * @param combiner the function to combine both success values
	 * @return a new Try with the combined result or the first failure encountered
	 */
	default <U, R> Try<R> combineWith(Try<U> other, BiFunction<T, U, R> combiner) {
		if (isFailure()) {
			return Try.failure(failure());
		}
		if (other.isFailure()) {
			return Try.failure(other.failure());
		}
		return Try.success(combiner.apply(success(), other.success()));
	}

	/**
	 * Returns the failure exception if this Try is a failure.
	 *
	 * @return the failure exception, or null if this is a success
	 */
	Exception failure();

	/**
	 * Filters the success value using a predicate.
	 *
	 * <p>
	 * If this is a success and the predicate returns false, converts this Try to a
	 * failure containing an IllegalStateException. Otherwise, returns this Try
	 * unchanged.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * Try<Integer> try = Try.success(42);
	 * Try<Integer> filtered = try.filter(n -> n > 0); // Success(42)
	 * Try<Integer> filtered2 = try.filter(n -> n < 0); // Failure
	 * }</pre>
	 *
	 * @param predicate the predicate to test the success value
	 * @return this Try if it's a failure or the predicate returns true, otherwise a
	 *         failure with IllegalStateException
	 */
	default Try<T> filter(Predicate<? super T> predicate) {
		if (isSuccess() && !predicate.test(success())) {
			return Try.failure(new IllegalStateException("Predicate does not match for: " + success()));
		}
		return this;
	}

	/**
	 * Maps the success value to a new Try using the given function.
	 *
	 * <p>
	 * This method is similar to {@link #map}, but the mapping function returns a
	 * Try instead of a plain value. This is useful when the mapping operation
	 * itself may fail.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * Try<String> try = Try.success("42");
	 * Try<Integer> result = try.flatMap(s -> Try.ofSupplier(() -> Integer.parseInt(s)));
	 * }</pre>
	 *
	 * @param <U>    the type of the mapped value
	 * @param mapper the function to apply to the success value
	 * @return the mapped Try or the original failure
	 */
	default <U> Try<U> flatMap(Function<? super T, Try<U>> mapper) {
		return isSuccess()
				? mapper.apply(success())
				: Try.failure(failure());
	}

	/**
	 * Gets the success value or throws the failure exception.
	 *
	 * <p>
	 * This method provides direct access to the contained value, but requires
	 * exception handling. For a safer alternative, consider using {@link #orElse}
	 * or {@link #orElseGet}.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * Try<String> tryValue = Try.success("Hello");
	 * try {
	 * 	String value = tryValue.get(); // Returns "Hello"
	 * 	System.out.println(value);
	 * } catch (Exception e) {
	 * 	// Handle failure case
	 * 	e.printStackTrace();
	 * }
	 * }</pre>
	 *
	 * @return the success value if present
	 * @throws Exception the failure exception if this is a failure
	 */
	T get() throws Exception;

	/**
	 * Executes the given action if this Try is a failure.
	 *
	 * <p>
	 * This method provides a way to handle failure cases without changing the Try
	 * instance. The action is only executed if this Try is a failure.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * Try<String> result = Try.failure(new IOException("File not found"));
	 * result.ifFailure(e -> logger.error("Operation failed", e))
	 * 		.ifSuccess(value -> System.out.println(value));
	 * }</pre>
	 *
	 * @param action the action to execute with the failure exception
	 * @return this Try for method chaining
	 */
	default Try<T> ifFailure(Consumer<? super Exception> action) {
		if (isFailure()) {
			action.accept(failure());
		}
		return this;
	}

	/**
	 * Executes the given action if this Try is a failure and the exception matches
	 * the specified type. This method allows for type-safe handling of specific
	 * exception types.
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * Try<String> result = someOperation();
	 * result
	 * 		.ifFailure(IOException.class, e -> logger.error("IO error occurred: {}", e.getMessage()))
	 * 		.ifFailure(IllegalArgumentException.class, e -> logger.warn("Invalid argument: {}", e.getMessage()));
	 * }</pre>
	 *
	 * @param <E>            the type of exception to handle
	 * @param exceptionClass the Class object representing the exception type to
	 *                       match
	 * @param action         the action to execute with the failure exception if it
	 *                       matches the specified type
	 * @return this Try for method chaining
	 */
	@SuppressWarnings("unchecked")
	default <E extends Throwable> Try<T> ifFailure(Class<E> exceptionClass, Consumer<E> action) {

		if (isFailure()) {
			if (failure().getClass().isAssignableFrom(exceptionClass)) {
				action.accept((E) failure());

			} else if (failure() instanceof UncheckedException unchecked
					&& unchecked.getClass().isAssignableFrom(exceptionClass)) {

				action.accept((E) failure());
			}
		}

		return this;
	}

	/**
	 * Executes the given action if this Try is a success.
	 *
	 * <p>
	 * This method provides a way to handle success cases without changing the Try
	 * instance. The action is only executed if this Try is a success.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * Try<String> result = Try.success("Hello");
	 * result.ifSuccess(value -> System.out.println("Got value: " + value))
	 * 		.ifFailure(e -> System.err.println("Failed: " + e.getMessage()));
	 * }</pre>
	 *
	 * @param action the action to execute with the success value
	 * @return this Try for method chaining
	 */
	default Try<T> ifSuccess(Consumer<? super T> action) {
		if (isSuccess()) {
			action.accept(success());
		}

		return this;
	}

	/**
	 * Executes a throwing consumer if this Try is a success.
	 *
	 * <p>
	 * Similar to {@link #ifSuccess}, but allows the consumer to throw exceptions.
	 * If the consumer throws an exception, this Try is converted to a failure.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * Try<Path> filePath = Try.success(Path.of("data.txt"));
	 * Try<Path> result = filePath.ifSuccessThrowing(path -> Files.delete(path));
	 * }</pre>
	 *
	 * @param consumer the consumer that may throw an exception
	 * @return this Try if success and consumer succeeds, or a new failure if the
	 *         consumer throws
	 */
	default Try<T> ifSuccessThrowing(ThrowingConsumer<? super T> consumer) {
		if (isSuccess()) {
			try {
				consumer.accept(success());
			} catch (Exception e) {
				return Try.failure(e);
			}
		}
		return this;
	}

	/**
	 * Checks if this Try is a failure.
	 *
	 * @return true if this is a failure, false if it's a success
	 */
	boolean isFailure();

	/**
	 * Checks if this Try is a success.
	 *
	 * @return true if this is a success, false if it's a failure
	 */
	boolean isSuccess();

	/**
	 * Maps the success value to a new value using the given function.
	 *
	 * <p>
	 * If this is a success, applies the mapping function to the success value and
	 * returns a new Try containing the result. If this is a failure, returns the
	 * failure unchanged.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * Try<String> stringTry = Try.success("42");
	 * Try<Integer> intTry = stringTry.map(Integer::parseInt);
	 * }</pre>
	 *
	 * @param <U>    the type of the mapped value
	 * @param mapper the function to apply to the success value
	 * @return a new Try with either the mapped value or the original failure
	 */
	default <U> Try<U> map(Function<? super T, ? extends U> mapper) {
		return isSuccess()
				? Try.success(mapper.apply(success()))
				: Try.failure(failure());
	}

	/**
	 * Maps a failure to a different exception type.
	 *
	 * <p>
	 * If this is a failure, applies the mapping function to the exception and
	 * returns a new failure with the mapped exception. If this is a success,
	 * returns this Try unchanged.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * Try<String> result = Try.failure(new IOException("Read failed"))
	 * 		.mapFailure(e -> new ApplicationException("Data access failed", e));
	 * }</pre>
	 *
	 * @param mapper the function to convert the exception
	 * @return this Try if success, or a new failure with the mapped exception
	 */
	default Try<T> mapFailure(Function<Exception, Exception> mapper) {
		if (isSuccess()) {
			return this;
		}
		return Try.failure(mapper.apply(failure()));
	}

	/**
	 * Maps this Try using a function that may throw exceptions.
	 *
	 * <p>
	 * Similar to {@link #map}, but the mapping function is allowed to throw
	 * exceptions. If the mapping function throws, the result will be a failure.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * Try<String> filePath = Try.success("data.txt");
	 * Try<String> content = filePath.mapThrowing(path -> Files.readString(Path.of(path)));
	 * }</pre>
	 *
	 * @param <R> the result type
	 * @param f   the function that may throw
	 * @return a new Try containing either the mapped value or any thrown exception
	 */
	default <R> Try<R> mapThrowing(ThrowingFunction<? super T, ? extends R> f) {
		if (isFailure()) {
			return Try.failure(failure());
		}
		try {
			return Try.success(f.apply(success()));
		} catch (Exception e) {
			return Try.failure(e);
		}
	}

	/**
	 * Returns the success value or a default value.
	 *
	 * <p>
	 * This method provides a safe way to get the success value with a fallback if
	 * this Try is a failure.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * Try<String> result = Try.failure(new Exception());
	 * String value = result.orElse("default"); // Returns "default"
	 * }</pre>
	 *
	 * @param defaultValue the value to return if this is a failure
	 * @return the success value or the default value
	 */
	default T orElse(T defaultValue) {
		return isSuccess() ? success() : defaultValue;
	}

	/**
	 * Returns the success value or gets a value from the supplier.
	 *
	 * <p>
	 * Similar to {@link #orElse}, but the default value is computed only when
	 * needed (i.e., only if this Try is a failure).
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * Try<Resource> resource = Try.failure(new Exception());
	 * Resource value = resource.orElseGet(() -> Resource.createDefault());
	 * }</pre>
	 *
	 * @param supplier the supplier to provide a value if this is a failure
	 * @return the success value or the supplied value
	 */
	default T orElseGet(Supplier<? extends T> supplier) {
		return isSuccess() ? success() : supplier.get();
	}

	/**
	 * Returns the success value or throws a supplied exception.
	 *
	 * <p>
	 * This method is similar to {@link #get}, but allows specifying the exception
	 * to throw in case of failure.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * Try<String> result = Try.failure(new IOException());
	 * String value = result.orElseThrow(() -> new IllegalStateException("Could not get value"));
	 * }</pre>
	 *
	 * @param <X>               the type of exception to throw
	 * @param exceptionSupplier the supplier of the exception
	 * @return the success value if present
	 * @throws X if this is a failure
	 */
	default <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
		if (isSuccess()) {
			return success();
		}
		throw exceptionSupplier.get();
	}

	@SuppressWarnings("unchecked")
	default <X extends Throwable> Try<T> ifFailureThrow(Class<X> exceptionClass) throws X {
		if (isSuccess()) {
			return this;
		}

		if (failure().getClass().isAssignableFrom(exceptionClass)) {
			throw (X) failure();

		} else if (failure() instanceof UncheckedException unchecked
				&& unchecked.getClass().isAssignableFrom(exceptionClass)) {

			throw (X) unchecked.getCause();
		}

		return this;
	}

	@SuppressWarnings("unchecked")
	default <X extends Throwable> T orElseThrow(Class<X> exceptionClass) throws X {
		if (isSuccess()) {
			return success();
		}

		if (failure().getClass().isAssignableFrom(exceptionClass)) {
			throw (X) failure();

		} else if (failure() instanceof UncheckedException unchecked
				&& unchecked.getClass().isAssignableFrom(exceptionClass)) {

			throw (X) unchecked.getCause();
		}

		throw (X) failure();
	}

	/**
	 * Recovers from a failure using the given recovery function.
	 *
	 * <p>
	 * If this is a failure, applies the recovery function to the exception to
	 * produce an alternative value.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * Try<String> result = Try.failure(new Exception("Not found"))
	 * 		.recover(e -> "default value");
	 * }</pre>
	 *
	 * @param recovery the function to provide an alternative value from the failure
	 * @return the success value or the recovered value
	 */
	default T recover(Function<? super Exception, ? extends T> recovery) {
		return isSuccess() ? success() : recovery.apply(failure());
	}

	/**
	 * Converts this Try to a Stream.
	 *
	 * <p>
	 * If this is a success, returns a Stream containing the success value. If this
	 * is a failure, returns an empty Stream.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * Try<String> try = Try.success("value");
	 * List<String> values = try.stream().collect(Collectors.toList());
	 * }</pre>
	 *
	 * @return a Stream containing the success value, or an empty Stream if this is
	 *         a failure
	 */
	default Stream<T> stream() {
		return isSuccess() ? Stream.of(success()) : Stream.empty();
	}

	/**
	 * Returns the success value.
	 *
	 * @return the success value, or null if this is a failure
	 */
	T success();

	/**
	 * Converts this Try to an IntTry if the contained value is an Integer.
	 *
	 * <p>
	 * This method provides a way to convert a Try&lt;Integer&gt; to a primitive
	 * IntTry for more efficient operations on primitive int values.
	 * </p>
	 *
	 * @return an IntTry containing the int value or the original failure
	 * @throws IllegalStateException if the success value is not an Integer
	 */
	default IntTry toIntTry() {
		if (isFailure()) {
			return IntTry.failure(failure());
		}

		T value = success();
		if (!(value instanceof Integer i)) {
			return IntTry.failure(new IllegalStateException("Value is not an Integer"));
		}

		return IntTry.success(i);
	}

	/**
	 * Converts this Try to a LongTry if the contained value is a Long.
	 *
	 * <p>
	 * This method provides a way to convert a Try&lt;Long&gt; to a primitive
	 * LongTry for more efficient operations on primitive long values.
	 * </p>
	 *
	 * @return a LongTry containing the long value or the original failure
	 * @throws IllegalStateException if the success value is not a Long
	 */
	default LongTry toLongTry() {
		if (isFailure()) {
			return LongTry.failure(failure());
		}

		T value = success();
		if (!(value instanceof Long l)) {
			return LongTry.failure(new IllegalStateException("Value is not a Long"));
		}

		return LongTry.success(l);
	}

	/**
	 * Converts this Try to an Optional.
	 *
	 * <p>
	 * If this is a success, returns an Optional containing the success value. If
	 * this is a failure, returns an empty Optional.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * Try<String> try = Try.success("value");
	 * Optional<String> opt = try.toOptional();  // Optional["value"]
	 * 
	 * Try<String> failure = Try.failure(new Exception());
	 * Optional<String> empty = failure.toOptional();  // Optional.empty()
	 * }</pre>
	 *
	 * @return an Optional containing the success value, or empty if this is a
	 *         failure
	 */
	default Optional<T> toOptional() {
		return Optional.ofNullable(success());
	}
}
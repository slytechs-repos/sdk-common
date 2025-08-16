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
package com.slytechs.jnet.core.api.foreign;

/**
 * Represents an exception that originates from a foreign system or library,
 * such as a native library or external process. This interface provides methods
 * to access the exception's code, message, and underlying cause.
 * 
 * <p>
 * Implementations of this interface are used to wrap exceptions that occur
 * outside of the Java runtime, providing standardized access to the error
 * information.
 * </p>
 * 
 * <p>
 * This interface can help bridge between native error codes and Java's
 * exception system, making it easier to handle and propagate exceptions across
 * boundaries.
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>{@code
 * try {
 * 	// Some foreign library call
 * } catch (ForeignException e) {
 * 	System.err.println("Error Code: " + e.getCode());
 * 	System.err.println("Error Message: " + e.getMessage());
 * }
 * }</pre>
 * 
 * @author Mark Bednarczyk
 */
public interface ForeignException {


	/**
	 * A factory interface for creating instances of exceptions that extend
	 * {@link Throwable}. This interface supports creating exceptions with an error
	 * code, message, or both.
	 * 
	 * <p>
	 * It provides a flexible way to create exceptions originating from foreign
	 * systems (such as native libraries) with both integer and long-based error
	 * codes.
	 * </p>
	 * 
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * ForeignExceptionFactory<MyException> factory = MyException::new;
	 * MyException ex = factory.newException(1001, "An error occurred.");
	 * }</pre>
	 * 
	 * @param <E> the type of exception that this factory produces, which must
	 *            extend {@link Throwable}
	 */
	public interface ForeignExceptionFactory<E extends Throwable & ForeignException> {

		/**
		 * Creates a new exception of type {@code E} with the specified code and
		 * message.
		 *
		 * @param code    the error code associated with the exception
		 * @param message the message describing the exception
		 * @return a new exception of type {@code E}
		 */
		E newException(int code, String message);

		/**
		 * Creates a new exception of type {@code E} with the specified long code and
		 * message. This method delegates to {@link #newException(int, String)} by
		 * converting the code to an integer.
		 *
		 * @param code    the error code associated with the exception, as a long
		 * @param message the message describing the exception
		 * @return a new exception of type {@code E}
		 */
		default E newException(long code, String message) {
			return newException((int) code, message);
		}

		/**
		 * Creates a new exception of type {@code E} with the specified message and a
		 * default code. The default code is {@code Integer.MIN_VALUE}.
		 *
		 * @param message the message describing the exception
		 * @return a new exception of type {@code E}
		 */
		default E newException(String message) {
			return newException(Integer.MIN_VALUE, message);
		}
	}

	/**
	 * A specialized factory interface for creating simple exceptions that extend
	 * {@link Throwable}.
	 * 
	 * <p>
	 * This interface simplifies the creation of exceptions by focusing primarily on
	 * creating exceptions with messages. It overrides methods from
	 * {@link ForeignExceptionFactory} to provide implementations that use a
	 * message-only constructor.
	 * </p>
	 * 
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * SimpleExceptionFactory<MyException> factory = MyException::new;
	 * MyException ex = factory.newException("A simple error message.");
	 * }</pre>
	 * 
	 * @param <E> the type of exception that this factory produces, which must
	 *            extend {@link Throwable}
	 */
	public interface SimpleExceptionFactory<E extends Throwable & ForeignException> extends ForeignExceptionFactory<E> {

		/**
		 * Creates a new exception of type {@code E} with the specified code and
		 * message. This method delegates to {@link #newException(String)} since this
		 * factory focuses on message-based exceptions.
		 *
		 * @param code    the error code associated with the exception
		 * @param message the message describing the exception
		 * @return a new exception of type {@code E}
		 */
		@Override
		default E newException(int code, String message) {
			return newException(message);
		}

		/**
		 * Creates a new exception of type {@code E} with the specified message.
		 *
		 * @param message the message describing the exception
		 * @return a new exception of type {@code E}
		 */
		@Override
		E newException(String message);
	}

	/**
	 * Returns the error code associated with this exception. The code is typically
	 * used to identify the specific error that occurred in the foreign system.
	 *
	 * @return the error code as an integer
	 */
	int getCode();

	/**
	 * Returns a detailed message explaining the cause of this exception. The
	 * message may provide additional context about the error or indicate potential
	 * solutions.
	 *
	 * @return the exception message as a {@code String}
	 */
	String getMessage();

	/**
	 * Returns the underlying cause of this exception, if any. This method provides
	 * access to the original exception or error that triggered this foreign
	 * exception.
	 *
	 * @return the cause of this exception as a {@code Throwable}, or {@code null}
	 *         if there is no cause
	 */
	Throwable getCause();
}

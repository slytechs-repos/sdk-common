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
 * An {@link IllegalStateException} that implements the {@link ForeignException}
 * interface. This exception is used to represent illegal states in foreign
 * systems, such as native libraries, while also capturing a specific error code
 * for diagnostic purposes.
 * 
 * <p>
 * This class allows exceptions originating from foreign sources to be enriched
 * with additional error codes, making it easier to diagnose and handle errors
 * that occur outside the Java runtime.
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>{@code
 * throw new ForeignIllegalStateException(1001, "An illegal state occurred in the foreign system.");
 * }</pre>
 * 
 * @author Mark Bednarczyk
 */
public class ForeignIllegalStateException extends IllegalStateException implements ForeignException {

	private static final long serialVersionUID = 73470741976646954L;

	/** The error code associated with this exception. */
	private final int code;

	/**
	 * Constructs a new {@code ForeignIllegalStateException} with the specified
	 * error code.
	 *
	 * @param code the error code representing the foreign system's error state
	 */
	public ForeignIllegalStateException(int code) {
		super();
		this.code = code;
	}

	/**
	 * Constructs a new {@code ForeignIllegalStateException} with the specified
	 * error code, message, and cause.
	 *
	 * @param code    the error code representing the foreign system's error state
	 * @param message the detail message explaining the exception
	 * @param cause   the cause of the exception (may be {@code null})
	 */
	public ForeignIllegalStateException(int code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	/**
	 * Constructs a new {@code ForeignIllegalStateException} with the specified
	 * error code and message.
	 *
	 * @param code    the error code representing the foreign system's error state
	 * @param message the detail message explaining the exception
	 */
	public ForeignIllegalStateException(int code, String message) {
		super(message);
		this.code = code;
	}

	/**
	 * Constructs a new {@code ForeignIllegalStateException} with the specified
	 * error code and cause.
	 *
	 * @param code  the error code representing the foreign system's error state
	 * @param cause the cause of the exception (may be {@code null})
	 */
	public ForeignIllegalStateException(int code, Throwable cause) {
		super(cause);
		this.code = code;
	}

	/**
	 * Returns the error code associated with this exception. The error code
	 * provides additional context about the foreign system's state when the
	 * exception was thrown.
	 *
	 * @return the error code as an integer
	 */
	@Override
	public int getCode() {
		return code;
	}
}

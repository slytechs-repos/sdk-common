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
package com.slytechs.sdk.common.memory;

/**
 * The Class BufferOperationException.
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class BufferOperationException extends RuntimeException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1991127839049446974L;

	/**
	 * Instantiates a new buffer operation exception.
	 */
	public BufferOperationException() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Instantiates a new buffer operation exception.
	 *
	 * @param message the message
	 */
	public BufferOperationException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Instantiates a new buffer operation exception.
	 *
	 * @param cause the cause
	 */
	public BufferOperationException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Instantiates a new buffer operation exception.
	 *
	 * @param message the message
	 * @param cause   the cause
	 */
	public BufferOperationException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Instantiates a new buffer operation exception.
	 *
	 * @param message            the message
	 * @param cause              the cause
	 * @param enableSuppression  the enable suppression
	 * @param writableStackTrace the writable stack trace
	 */
	public BufferOperationException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

}

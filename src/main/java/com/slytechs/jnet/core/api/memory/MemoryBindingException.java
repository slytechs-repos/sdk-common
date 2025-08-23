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
package com.slytechs.jnet.core.api.memory;

/**
 * Exception thrown when attempting to bind an already-bound MemoryProxy.
 * 
 * <p>
 * This exception indicates a programming error where code attempts to bind a
 * MemoryProxy that is already bound to memory without first unbinding it.
 * </p>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 */
public class MemoryBindingException extends RuntimeException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -595068674386606756L;

	/**
	 * Constructs a MemoryBindingException with the specified message.
	 * 
	 * @param message the exception message
	 */
	public MemoryBindingException(String message) {
		super(message);
	}

	/**
	 * Constructs a MemoryBindingException with message and cause.
	 * 
	 * @param message the exception message
	 * @param cause   the underlying cause
	 */
	public MemoryBindingException(String message, Throwable cause) {
		super(message, cause);
	}
}
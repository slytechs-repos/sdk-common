/*
 * Copyright 2005-2026 Sly Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slytechs.sdk.common.session;

/**
 * General runtime exception for lifecycleSession-related errors, such as
 * invalid state or operation failures.
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class SessionException extends RuntimeException {

	private static final long serialVersionUID = 3422129549674159036L;
	private final Session session;

	/**
	 * Constructs a new exception.
	 *
	 * @param session the associated lifecycleSession
	 * @param message the detail message
	 */
	public SessionException(Session session) {
		super();
		this.session = session;
	}

	/**
	 * Constructs a new exception.
	 *
	 * @param session the associated lifecycleSession
	 * @param message the detail message
	 */
	public SessionException(Session session, String message) {
		super(message);
		this.session = session;
	}

	/**
	 * Constructs a new exception with cause.
	 *
	 * @param session the associated lifecycleSession
	 * @param message the detail message
	 * @param cause   the cause
	 */
	public SessionException(Session session, String message, Throwable cause) {
		super(message, cause);
		this.session = session;
	}

	/**
	 * Returns the associated Session.
	 *
	 * @return the lifecycleSession
	 */
	public Session session() {
		return session;
	}
}
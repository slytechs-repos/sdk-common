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
 * Runtime exception for shutdown-specific errors, such as failures during task
 * interruption or resource release. Subclass of {@link SessionException}.
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class SessionShutdownException extends SessionException {

	private static final long serialVersionUID = 8575599897478005077L;

	/**
	 * Constructs a new exception.
	 *
	 * @param session the associated session
	 * @param message the detail message
	 */
	public SessionShutdownException(Session session) {
		super(session);
	}

	/**
	 * Constructs a new exception.
	 *
	 * @param session the associated session
	 * @param message the detail message
	 */
	public SessionShutdownException(Session session, String message) {
		super(session, message);
	}

	/**
	 * Constructs a new exception with cause.
	 *
	 * @param session the associated session
	 * @param message the detail message
	 * @param cause   the cause
	 */
	public SessionShutdownException(Session session, String message, Throwable cause) {
		super(session, message, cause);
	}
}
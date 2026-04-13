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
 * Sessions that are also closaeable for use with try-with-resource statements
 */
public interface CloseableSession extends AutoCloseable, Session {
	/**
	 * Closes the session, releasing all resources. This method ensures a graceful
	 * shutdown if running, followed by resource cleanup (e.g., closing sockets,
	 * draining queues).
	 * <p>
	 * If an error occurs during closure (e.g., resource release failure), a
	 * {@link SessionException} or {@link SessionShutdownException} is thrown as a
	 * runtime exception.
	 * </p>
	 * 
	 * @throws InterruptedException
	 */
	@Override
	void close() throws SessionException, InterruptedException;
}
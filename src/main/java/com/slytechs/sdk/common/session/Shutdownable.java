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
 * 
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public interface Shutdownable {

	/**
	 * Attempts an immediate, forceful shutdown by interrupting threads and blocking
	 * until termination or timeout.
	 *
	 * <p>
	 * This method is blocking and may throw if interrupted or if shutdown cannot
	 * complete (e.g., threads refuse interruption).
	 * </p>
	 *
	 * @throws InterruptedException     if the calling thread is interrupted while
	 *                                  waiting
	 * @throws SessionShutdownException if forceful shutdown fails (e.g., stuck
	 *                                  threads)
	 */
	void shutdownNow() throws InterruptedException, SessionShutdownException;

	/**
	 * Initiates a graceful shutdown if not already shutting down or terminated.
	 * This method is non-blocking and idempotent — safe to call multiple times.
	 *
	 * @return {@code true} if shutdown was newly initiated, {@code false} if
	 *         already shutting down or terminated (no-op)
	 */
	boolean shutdown();

	/**
	 * Returns whether shutdown has been initiated (graceful or forceful).
	 */
	boolean isShutdown();

	/**
	 * Returns whether the session has fully terminated.
	 */
	boolean isTerminated();
}

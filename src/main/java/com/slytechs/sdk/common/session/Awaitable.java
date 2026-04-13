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

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * 
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public interface Awaitable {

	/**
	 * Waits indefinitely until all internal tasks have completed following a
	 * shutdown or until the current thread is interrupted. This does not wait for
	 * user-forked tasks (e.g., in a {@link TaskScope}), which must be managed
	 * separately using {@link TaskScope#join()}.
	 *
	 * @throws InterruptedException if the current thread is interrupted while
	 *                              waiting (e.g., during forceful shutdown).
	 */
	void awaitCompletion() throws InterruptedException, SessionAwaitException;

	/**
	 * Waits until all internal tasks have completed following a shutdown, the
	 * specified timeout elapses, or the current thread is interrupted. This does
	 * not wait for user-forked tasks (e.g., in a {@link TaskScope}), which must be
	 * managed separately using {@link TaskScope#joinUntil(Instant)}.
	 *
	 * @param timeout the maximum time to wait.
	 * @param unit    the time unit of the timeout.
	 * @return true if all internal tasks completed, false if timeout elapsed.
	 * @throws InterruptedException     if interrupted while waiting (e.g., during
	 *                                  forceful shutdown).
	 * @throws IllegalArgumentException if timeout is negative.
	 */
	default boolean awaitCompletion(Duration timeout) throws InterruptedException, SessionAwaitException {
		throw new UnsupportedOperationException("timed await not supported");
	}

	/**
	 * Waits until all internal tasks have completed following a shutdown, the
	 * specified timeout elapses, or the current thread is interrupted. This does
	 * not wait for user-forked tasks (e.g., in a {@link TaskScope}), which must be
	 * managed separately using {@link TaskScope#joinUntil(Instant)}.
	 *
	 * @param timeout the maximum time to wait.
	 * @param unit    the time unit of the timeout.
	 * @return true if all internal tasks completed, false if timeout elapsed.
	 * @throws InterruptedException     if interrupted while waiting (e.g., during
	 *                                  forceful shutdown).
	 * @throws IllegalArgumentException if timeout is negative.
	 */
	default boolean awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException, SessionAwaitException {
		return awaitCompletion(Duration.ofNanos(unit.toNanos(timeout)));
	}
}

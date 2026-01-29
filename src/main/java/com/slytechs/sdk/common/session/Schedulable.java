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

/**
 * 
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
@FunctionalInterface
public interface Schedulable {

	/**
	 * Cancels any scheduled shutdown condition (e.g., time-based). The session
	 * continues running until another shutdown is initiated.
	 *
	 * @return this instance for method chaining.
	 * @throws SessionException if an error occurs during cancellation (e.g.,
	 *                          invalid state).
	 */
	default Schedulable cancelShutdown() {
		throw new UnsupportedOperationException("shutdownAt not supported");
	}

	/**
	 * Schedules a graceful shutdown after the specified duration. The session
	 * continues running until the duration elapses, then initiates shutdown.
	 *
	 * @param duration the duration to wait before shutting down.
	 * @return this instance for method chaining.
	 * @throws NullPointerException     if duration is null.
	 * @throws IllegalArgumentException if duration is negative.
	 * @throws SessionException         if an error occurs during scheduling (e.g.,
	 *                                  invalid state).
	 */
	Schedulable shutdownAfter(Duration duration) throws SessionSchedulingException, IllegalArgumentException;

	/**
	 * Schedules a graceful shutdown at the specified instant. The session continues
	 * running until the instant is reached, then initiates shutdown.
	 *
	 * @param when the instant at which to shutdown.
	 * @return this instance for method chaining.
	 * @throws NullPointerException if when is null.
	 * @throws SessionException     if an error occurs during scheduling (e.g.,
	 *                              invalid state).
	 */
	default Schedulable shutdownAt(Instant when) throws SessionSchedulingException, IllegalArgumentException {
		if (when == null) {
			throw new NullPointerException("when cannot be null");
		}

		Duration duration = Duration.between(Instant.now(), when);
		if (duration.isNegative() || duration.isZero()) {
			throw new IllegalArgumentException("shutdown time must be in the future");
		}

		return shutdownAfter(duration);
	}

	/**
	 * Checks if a shutdown has been scheduled but not yet initiated.
	 *
	 * @return true if a shutdown is scheduled via {@link #shutdownAfter(Duration)}
	 *         or {@link #shutdownAt(Instant)}, false otherwise.
	 */
	default boolean isShutdownScheduled() {
		return false;
	}

}

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
package com.slytechs.sdk.common.session.state;

import java.time.Duration;
import java.time.Instant;

import com.slytechs.sdk.common.session.state.recorder.StateRecorder;

/**
 * Immutable context containing error state for recovery decisions.
 * 
 * <p>
 * Provides all relevant information about an error occurrence to allow
 * {@link ErrorPolicy} handlers to make informed recovery decisions.
 * </p>
 * 
 * <h2>Usage in Error Handler</h2>
 * 
 * <pre>{@code
 * service.errorPolicy()
 * 		.onError(ctx -> {
 * 			// Check exception type
 * 			if (ctx.exception() instanceof IOException) {
 * 				return Recovery.Default.RESTART_DELAYED;
 * 			}
 * 
 * 			// Check error count
 * 			if (ctx.errorCount() > 3) {
 * 				return Recovery.Default.FAIL;
 * 			}
 * 
 * 			// Check time since first error
 * 			if (ctx.timeSinceFirstError().toMinutes() > 5) {
 * 				return Recovery.Default.ESCALATE;
 * 			}
 * 
 * 			return Recovery.Default.RESTART;
 * 		});
 * }</pre>
 *
 * @param exception      the exception that caused the error
 * @param errorCount     total number of errors since creation or last reset
 * @param restartCount   number of restarts attempted
 * @param firstErrorTime time of first error in current error sequence
 * @param lastErrorTime  time of most recent error
 * @param serviceName    name of the service/state machine
 * @param recorder       for recording recovery related state events
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see ErrorPolicy
 * @see Recovery
 */
public record ErrorContext(
		Throwable exception,
		int errorCount,
		int restartCount,
		Instant firstErrorTime,
		Instant lastErrorTime,
		String serviceName,
		StateRecorder recorder) {

	/**
	 * Returns the duration since the first error occurred.
	 * 
	 * @return duration since first error, or zero if no previous errors
	 */
	public Duration timeSinceFirstError() {
		if (firstErrorTime == null) {
			return Duration.ZERO;
		}
		return Duration.between(firstErrorTime, Instant.now());
	}

	/**
	 * Returns the duration since the last error occurred.
	 * 
	 * @return duration since last error, or zero if this is first error
	 */
	public Duration timeSinceLastError() {
		if (lastErrorTime == null) {
			return Duration.ZERO;
		}
		return Duration.between(lastErrorTime, Instant.now());
	}

	/**
	 * Checks if this is the first error.
	 * 
	 * @return true if errorCount is 1
	 */
	public boolean isFirstError() {
		return errorCount == 1;
	}

	/**
	 * Checks if the exception is of the specified type.
	 * 
	 * @param type the exception class to check
	 * @return true if exception is instance of type
	 */
	public boolean isExceptionType(Class<? extends Throwable> type) {
		return type.isInstance(exception);
	}

	/**
	 * Returns the root cause of the exception chain.
	 * 
	 * @return the deepest cause, or the exception itself if no cause
	 */
	public Throwable rootCause() {
		Throwable cause = exception;
		while (cause.getCause() != null && cause.getCause() != cause) {
			cause = cause.getCause();
		}
		return cause;
	}

	/**
	 * Checks if restart limit has been exceeded.
	 * 
	 * @param maxRestarts maximum allowed restarts
	 * @return true if restartCount >= maxRestarts
	 */
	public boolean exceedsRestartLimit(int maxRestarts) {
		return restartCount >= maxRestarts;
	}

	@Override
	public String toString() {
		return String.format("ErrorContext[service=%s, error=%s, count=%d, restarts=%d]",
				serviceName,
				exception != null ? exception.getClass().getSimpleName() : "null",
				errorCount,
				restartCount);
	}
}
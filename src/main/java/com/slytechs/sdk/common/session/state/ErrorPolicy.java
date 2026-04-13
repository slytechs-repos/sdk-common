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
import java.util.function.Function;

/**
 * Manages error handling policy and recovery decisions for state machines.
 * 
 * <p>
 * ErrorPolicy is a composition element that can be added to any state machine
 * to provide consistent error handling, restart tracking, and recovery
 * decisions. It maintains error state and delegates recovery decisions to
 * either default behavior or user-registered handlers.
 * </p>
 * 
 * <h2>Basic Usage</h2>
 * 
 * <pre>{@code
 * // In state machine
 * private final ErrorPolicy<Recovery.Default> errorPolicy = new ErrorPolicy<>("my-service", Recovery.Default.FAIL);
 * 
 * // When error occurs
 * Recovery.Default action = errorPolicy.evaluate(exception);
 * if (action.isTerminal()) {
 * 	transitionTo(ERROR);
 * } else if (action.shouldRestart()) {
 * 	if (action.isDelayed()) {
 * 		Thread.sleep(errorPolicy.restartDelay().toMillis());
 * 	}
 * 	restart();
 * }
 * }</pre>
 * 
 * <h2>Configuration</h2>
 * 
 * <pre>{@code
 * service.errorPolicy()
 * 		.maxRestarts(5)
 * 		.restartDelay(Duration.ofSeconds(2))
 * 		.onError(ctx -> {
 * 			if (ctx.exception() instanceof IOException)
 * 				return Recovery.Default.RESTART_DELAYED;
 * 			if (ctx.errorCount() > 3)
 * 				return Recovery.Default.FAIL;
 * 			return Recovery.Default.RESTART;
 * 		});
 * }</pre>
 * 
 * <h2>Custom Recovery Types</h2>
 * 
 * <pre>{@code
 * public enum TaskRecovery implements Recovery<TaskRecovery> {
 * 	FAIL, RESTART, RESTART_DELAYED, SHUTDOWN_GROUP;
 * 	// ...
 * }
 * 
 * ErrorPolicy<TaskRecovery> policy = new ErrorPolicy<>("task", TaskRecovery.FAIL);
 * }</pre>
 *
 * @param <R> the recovery action enum type
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see Recovery
 * @see ErrorContext
 */
public class ErrorPolicy<R extends Enum<R> & Recovery<R>> {

	private final StateMachine<?> state;
	private final R defaultAction;

	private volatile Throwable uncaughtException;
	private volatile int errorCount;
	private volatile int restartCount;
	private volatile Instant firstErrorTime;
	private volatile Instant lastErrorTime;

	private int maxRestarts = 3;
	private Duration restartDelay = Duration.ofSeconds(1);
	private Function<ErrorContext, R> handler;

	/**
	 * Creates an error policy with the specified default recovery action.
	 * 
	 * @param state         the state machine this error policy applies to.
	 * @param defaultAction the default recovery action when no handler is set
	 */
	public ErrorPolicy(StateMachine<?> state, R defaultAction) {
		this.state = state;
		this.defaultAction = defaultAction;
	}

	/**
	 * Evaluates an error and returns the recovery action to take.
	 * 
	 * <p>
	 * Updates error tracking state and delegates to the registered handler or
	 * returns the default action. If max restarts exceeded, returns the default
	 * (terminal) action.
	 * </p>
	 * 
	 * @param t the exception that occurred
	 * @return the recovery action to take
	 */
	public R evaluate(Throwable t) {
		this.lastErrorTime = Instant.now();
		this.uncaughtException = t;
		this.errorCount++;

		if (firstErrorTime == null) {
			firstErrorTime = lastErrorTime;
		}

		ErrorContext ctx = context();

		state.recorder().warn("Error #{} in {}: {}",
				errorCount, state.name(), t.getMessage());

		// Exceeded max restarts - fail
		if (ctx.exceedsRestartLimit(maxRestarts)) {
			state.recorder().error("Max restarts ({}) exceeded, failing", maxRestarts);
			return defaultAction;
		}

		// Delegate to handler if present
		R action;
		if (handler != null) {
			action = handler.apply(ctx);
			state.recorder().info("Custom handler returned: {}", action);
		} else {
			action = defaultAction;
		}

		if (action.shouldRestart()) {
			state.recorder().info("Recovery action: {} (restart #{})", action, restartCount + 1);
		} else if (action.isTerminal()) {
			state.recorder().error("Recovery action: {} (terminal)", action);
		} else if (action.shouldEscalate()) {
			state.recorder().warn("Recovery action: {} (escalating to parent)", action);
		}

		return defaultAction;
	}

	/**
	 * Records a successful restart.
	 * 
	 * <p>
	 * Call this after a successful restart to track restart count.
	 * </p>
	 */
	public void recordRestart() {
		restartCount++;
		state.recorder().info("Restart #{} completed", restartCount);
	}

	/**
	 * Resets error tracking state.
	 * 
	 * <p>
	 * Call this after successful recovery or to clear error history.
	 * </p>
	 */
	public void reset() {
		if (errorCount > 0) {
			state.recorder().info("Error policy reset (was: {} errors, {} restarts)",
					errorCount, restartCount);
		}

		uncaughtException = null;
		errorCount = 0;
		restartCount = 0;
		firstErrorTime = null;
		lastErrorTime = null;
	}

	/**
	 * Returns the current error context.
	 * 
	 * @return immutable error context
	 */
	public ErrorContext context() {
		return new ErrorContext(
				uncaughtException,
				errorCount,
				restartCount,
				firstErrorTime,
				lastErrorTime,
				state.name(),
				state.recorder());
	}

	/**
	 * Sets the maximum number of restarts before failing.
	 * 
	 * @param max maximum restarts allowed
	 * @return this policy for chaining
	 */
	public ErrorPolicy<R> maxRestarts(int max) {
		this.maxRestarts = max;
		return this;
	}

	/**
	 * Sets the delay between restarts.
	 * 
	 * @param delay duration to wait before restart
	 * @return this policy for chaining
	 */
	public ErrorPolicy<R> restartDelay(Duration delay) {
		this.restartDelay = delay;
		return this;
	}

	/**
	 * Registers an error handler for custom recovery decisions.
	 * 
	 * <p>
	 * The handler receives an {@link ErrorContext} and returns the recovery action
	 * to take.
	 * </p>
	 * 
	 * @param handler the error handler function
	 * @return this policy for chaining
	 */
	public ErrorPolicy<R> onError(Function<ErrorContext, R> handler) {
		this.handler = handler;
		return this;
	}

	/**
	 * Returns the configured restart delay.
	 * 
	 * @return restart delay duration
	 */
	public Duration restartDelay() {
		return restartDelay;
	}

	/**
	 * Returns the maximum restarts allowed.
	 * 
	 * @return max restarts
	 */
	public int maxRestarts() {
		return maxRestarts;
	}

	/**
	 * Returns the most recent uncaught exception.
	 * 
	 * @return the exception, or null if no error has occurred
	 */
	public Throwable uncaughtException() {
		return uncaughtException;
	}

	/**
	 * Returns the total error count.
	 * 
	 * @return number of errors since creation or last reset
	 */
	public int errorCount() {
		return errorCount;
	}

	/**
	 * Returns the restart count.
	 * 
	 * @return number of restarts since creation or last reset
	 */
	public int restartCount() {
		return restartCount;
	}

	/**
	 * Checks if any error has occurred.
	 * 
	 * @return true if errorCount > 0
	 */
	public boolean hasError() {
		return errorCount > 0;
	}

	/**
	 * Returns the default recovery action.
	 * 
	 * @return the default action
	 */
	public R defaultAction() {
		return defaultAction;
	}

	@Override
	public String toString() {
		return String.format("ErrorPolicy[%s, errors=%d, restarts=%d/%d, exception=%s]",
				state.name(),
				errorCount,
				restartCount,
				maxRestarts,
				uncaughtException != null ? uncaughtException.getClass().getSimpleName() : "none");
	}
}
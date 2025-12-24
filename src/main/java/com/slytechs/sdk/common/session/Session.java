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
package com.slytechs.sdk.common.session;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Interface for managing the lifecycle of network sessions (e.g., capture,
 * transmission, configuration). Sessions are started implicitly upon open.
 * Provides methods to shutdown gracefully or forcefully, schedule termination,
 * query state, and wait for completion.
 * <p>
 * Use {@link NetWorks#openSession(Class)} or similar factory methods to create
 * sessions. For user tasks (e.g., processing), use {@link TaskScope} and call
 * {@link TaskScope#join()} separately.
 * </p>
 * <p>
 * All methods that may encounter errors throw runtime exceptions such as
 * {@link SessionException} or {@link SessionShutdownException} instead of
 * checked exceptions. This simplifies error handling in user code while
 * ensuring that critical issues are reported immediately.
 * </p>
 * <p>
 * In hierarchical setups, the root {@link NetWorks} implements this interface
 * and propagates shutdowns to sub-sessions (e.g., {@link Capture}). Simple
 * sessions without ongoing processes (e.g., Config) may implement no-op
 * defaults or throw {@link UnsupportedOperationException} for timing methods.
 * </p>
 * <p>
 * Thread safety: State queries (e.g., {@link #isRunning()}) are atomic and safe
 * for concurrent access. Shutdown methods are idempotent and coordinate safely
 * in multi-threaded environments.
 * </p>
 * <p>
 * Shutdown pattern:
 * <ul>
 * <li><b>Gentle Shutdown ({@link #shutdown()}):</b> Sets {@link #isRunning()} =
 * false and {@link #isShutdown()} = true, allowing in-progress operations to
 * complete (e.g., draining resources) but preventing new ones. Producers stop;
 * consumers continue until drained. Does not interrupt threads.</li>
 * <li><b>Forceful Shutdown ({@link #shutdownNow()}):</b> Initiates gentle
 * shutdown, then interrupts blocked threads (e.g., producers/consumers) to
 * accelerate termination. Relies on
 * {@link Thread#currentThread()#isInterrupted()} checks or
 * {@link InterruptedException} in blocking ops (e.g., phaser awaits) to detect
 * forceful mode. Sets {@link #isTerminated()} = true after recovery (e.g.,
 * internal drain).</li>
 * <li><b>Termination:</b> When drained and all resources released,
 * {@link #isTerminated()} = true. Await methods block until this state.</li>
 * </ul>
 * Forceful shutdown uses Java's interruption mechanism—no extra flags needed.
 * Implementations handle interrupts to distinguish gentle vs. forceful (e.g.,
 * throw {@link SessionShutdownException} on interrupt in forceful mode).
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public interface Session {

	/**
	 * Sessions that are also closaeable for use with try-with-resource statements
	 */
	public interface CloseableSession extends Session, AutoCloseable {
		/**
		 * Closes the session, releasing all resources. This method ensures a graceful
		 * shutdown if running, followed by resource cleanup (e.g., closing sockets,
		 * draining queues).
		 * <p>
		 * If an error occurs during closure (e.g., resource release failure), a
		 * {@link SessionException} or {@link SessionShutdownException} is thrown as a
		 * runtime exception.
		 * </p>
		 */
		@Override
		void close() throws SessionException;
	}

	/**
	 * Initiates a graceful shutdown, allowing in-progress operations to complete
	 * but preventing new ones. Does not wait for completion; use
	 * {@link #awaitCompletion()} or {@link #awaitCompletion(long, TimeUnit)} to
	 * wait.
	 * <p>
	 * If an error occurs during shutdown (e.g., failure to stop internal tasks), a
	 * {@link SessionShutdownException} is thrown as a runtime exception.
	 * </p>
	 * <p>
	 * In hierarchies, propagates to sub-sessions.
	 * </p>
	 */
	void shutdown();

	/**
	 * Initiates an immediate but graceful shutdown, interrupting in-progress
	 * operations where necessary but striving to complete critical ones. Faster
	 * than {@link #shutdown()} but less gentle.
	 * <p>
	 * If an error occurs during shutdown (e.g., failure to interrupt tasks), a
	 * {@link SessionShutdownException} is thrown as a runtime exception.
	 * </p>
	 * <p>
	 * In hierarchies, propagates to sub-sessions. Interrupts blocked threads,
	 * relying on {@link Thread#currentThread()#isInterrupted()} or
	 * {@link InterruptedException} to signal forceful termination.
	 * </p>
	 */
	void shutdownNow();

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
	default Session shutdownAfter(Duration duration) {
		if (state() instanceof SessionStateImpl sessionStateImpl)
			sessionStateImpl.shutdownAfter(duration);
		else
			throw new IllegalStateException("invalid session state type");

		return this;
	}

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
	default Session shutdownAt(Instant when) {
		if (state() instanceof SessionStateImpl sessionStateImpl)
			sessionStateImpl.shutdownAt(when);
		else
			throw new IllegalStateException("invalid session state type");

		return this;
	}

	/**
	 * Cancels any scheduled shutdown condition (e.g., time-based). The session
	 * continues running until another shutdown is initiated.
	 *
	 * @return this instance for method chaining.
	 * @throws SessionException if an error occurs during cancellation (e.g.,
	 *                          invalid state).
	 */
	default Session cancelShutdown() {
		if (state() instanceof SessionStateImpl sessionStateImpl)
			sessionStateImpl.cancelScheduledShutdown();
		else
			throw new IllegalStateException("invalid session state type");

		return this;
	}

	/**
	 * Checks if the session is currently running or has a scheduled shutdown
	 * condition.
	 *
	 * @return true if active, false if shutdown is complete.
	 */
	default boolean isRunning() {
		return state().isRunning();
	}

	/**
	 * Checks if shutdown has been initiated for this session.
	 *
	 * @return true if {@link #shutdown()} or {@link #shutdownNow()} has been
	 *         called, false otherwise.
	 */
	default boolean isShutdown() {
		return state().isShutdown();
	}

	/**
	 * Checks if a shutdown has been scheduled but not yet initiated.
	 *
	 * @return true if a shutdown is scheduled via {@link #shutdownAfter(Duration)}
	 *         or {@link #shutdownAt(Instant)}, false otherwise.
	 */
	default boolean isShutdownScheduled() {
		return state().isShutdownScheduled();
	}

	/**
	 * Checks if all internal tasks have completed following a shutdown.
	 *
	 * @return true if all internal tasks have completed after shutdown, false
	 *         otherwise.
	 */
	default boolean isTerminated() {
		return state().isTerminated();
	}

	/**
	 * Waits indefinitely until all internal tasks have completed following a
	 * shutdown or until the current thread is interrupted. This does not wait for
	 * user-forked tasks (e.g., in a {@link TaskScope}), which must be managed
	 * separately using {@link TaskScope#join()}.
	 *
	 * @throws InterruptedException if the current thread is interrupted while
	 *                              waiting (e.g., during forceful shutdown).
	 * @throws SessionException     if an error occurs during waiting (e.g., session
	 *                              state inconsistency).
	 */
	default void awaitCompletion() throws InterruptedException {
		if (state() instanceof SessionStateImpl sessionStateImpl)
			sessionStateImpl.await();
		else
			throw new IllegalStateException("invalid session state type");
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
	 * @throws SessionException         if an error occurs during waiting (e.g.,
	 *                                  session state inconsistency).
	 */
	default boolean awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException {
		if (state() instanceof SessionStateImpl sessionStateImpl)
			return sessionStateImpl.await(timeout, unit);
		else
			throw new IllegalStateException("invalid session state type");
	}

	/**
	 * Returns the {@link SessionStateImpl} managing this session's lifecycle flags.
	 *
	 * @return the net state instance for this session.
	 */
	SessionState state();
}
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

import com.slytechs.sdk.common.session.managed.ManagedStateMachine;

/**
 * Interface for querying the state of a {@link Session} in the jNetworks
 * SDK. Provides read-only access to the lifecycle stages of a network session:
 * running, shutdown scheduled, shutdown initiated, and terminated. This
 * interface allows users to inspect session state without modifying it,
 * ensuring safe interaction with session lifecycle management.
 * 
 * <p>
 * The jNetworks SDK employs a hierarchical structure for managing network
 * sessions, where NetWorks acts as the root session containing multiple
 * sub-sessions (e.g., Capture for packet capture, Transmitter for
 * transmission, Config for configuration, FileCapture for file operations,
 * Statistics for metrics, and EventMonitor for events). Each sub-session
 * implements {@link Session} and provides a {@link SessionState} for
 * querying its state. The root NetWorks also implements {@link Session}, 
 * allowing unified lifecycle management.
 * </p>
 * 
 * <p>
 * <b>Lifecycle Stages:</b> The interface provides methods to query distinct,
 * non-overlapping stages in the session lifecycle, enabling precise state
 * checks. All queries are thread-safe for concurrent access.
 * </p>
 * <ul>
 * <li><b>Running ({@link #isRunning()}):</b> Indicates the session is active
 * and operational (e.g., capturing or transmitting packets). True after session
 * open and false when shutdown is initiated.</li>
 * <li><b>Shutdown Scheduled ({@link #isShutdownScheduled()}):</b> Indicates a
 * shutdown has been scheduled (e.g., via
 * {@link Session#shutdownAfter(Duration)} or
 * {@link Session#shutdownAt(Instant)}), but not yet initiated. The session
 * remains running until the deadline.</li>
 * <li><b>Shutdown Initiated ({@link #isShutdown()}):</b> Indicates shutdown has
 * started (e.g., via {@link Session#shutdown()},
 * {@link Session#shutdownNow()}, or scheduled deadline reached), but
 * internal tasks are still completing. No new operations are accepted.</li>
 * <li><b>Terminated ({@link #isTerminated()}):</b> Indicates all internal tasks
 * and registered sub-sessions/components have completed after shutdown, and the
 * session is fully stopped.</li>
 * </ul>
 * 
 * <p>
 * <b>Thread Safety:</b> All methods are designed for concurrent access from
 * multiple threads without external synchronization. Queries ensure atomicity
 * and visibility across threads.
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see Session
 * @see StateMachine
 * @see ManagedStateMachine
 */
public sealed interface SessionState permits StateMachine, ManagedStateMachine {

	/**
	 * Returns the name of the session.
	 *
	 * @return the session name
	 */
	String name();

	/**
	 * Checks if the session is currently running or has a scheduled shutdown
	 * condition.
	 *
	 * @return true if active, false if shutdown is complete
	 */
	boolean isRunning();

	/**
	 * Checks if shutdown has been initiated for this session.
	 *
	 * @return true if {@link Session#shutdown()} or
	 *         {@link Session#shutdownNow()} has been called, false otherwise
	 */
	boolean isShutdown();

	/**
	 * Checks if a shutdown has been scheduled but not yet initiated.
	 *
	 * @return true if a shutdown is scheduled via
	 *         {@link Session#shutdownAfter(Duration)} or
	 *         {@link Session#shutdownAt(Instant)}, false otherwise
	 */
	boolean isShutdownScheduled();

	/**
	 * Checks if all internal tasks have completed following a shutdown.
	 *
	 * @return true if all internal tasks have completed after shutdown, false
	 *         otherwise
	 */
	boolean isTerminated();

	/**
	 * Waits until all registered components complete.
	 * <p>
	 * Blocks indefinitely or until interrupted. Used in
	 * {@link Session#awaitCompletion()}. Throws {@link InterruptedException} on
	 * interrupt (e.g., during forceful shutdown).
	 * </p>
	 *
	 * @throws InterruptedException if interrupted while waiting
	 */
	void await() throws InterruptedException;

	/**
	 * Waits until all registered components complete or the timeout elapses.
	 * <p>
	 * Used in timed {@link Session#awaitCompletion(long, TimeUnit)}. Returns
	 * true if components completed, false on timeout. Throws
	 * {@link InterruptedException} on interrupt.
	 * </p>
	 *
	 * @param timeout the maximum time to wait
	 * @param unit    the time unit
	 * @return true if terminated (components completed), false on timeout
	 * @throws InterruptedException if interrupted while waiting
	 */
	boolean await(long timeout, TimeUnit unit) throws InterruptedException;
}
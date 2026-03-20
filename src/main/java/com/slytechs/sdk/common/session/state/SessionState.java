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

import com.slytechs.sdk.common.session.SystemSession;

/**
 * Interface for querying the state of a {@link SystemSession} in the jNetworks
 * SDK. Provides read-only access to the lifecycle stages of a network session:
 * running, shutdown scheduled, shutdown initiated, and terminated. This
 * interface allows users to inspect session state without modifying it,
 * ensuring safe interaction with session lifecycle management.
 * 
 * <p>
 * The jNetworks SDK employs a hierarchical structure for managing network
 * sessions, where NetWorks acts as the root session containing multiple
 * sub-sessions (e.g., Capture for packet capture, Transmitter for transmission,
 * Config for configuration, FileCapture for file operations, Statistics for
 * metrics, and EventMonitor for events). Each sub-session implements
 * {@link SystemSession} and provides a {@link SessionState} for querying its
 * state. The root NetWorks also implements {@link SystemSession}, allowing
 * unified lifecycle management.
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
 * {@link SystemSession#shutdownAfter(Duration)} or
 * {@link SystemSession#shutdownAt(Instant)}), but not yet initiated. The
 * session remains running until the deadline.</li>
 * <li><b>Shutdown Initiated ({@link #isShutdown()}):</b> Indicates shutdown has
 * started (e.g., via {@link SystemSession#shutdown()},
 * {@link SystemSession#shutdownNow()}, or scheduled deadline reached), but
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
 * @see SystemSession
 * @see StateMachine
 * @see ManagedStateMachine
 */
public interface SessionState {

	/** A closed session instance. */
	SessionState CLOSED = new SessionState() {

		@Override
		public String name() {
			return "CLOSED";
		}
	};

	/**
	 * Returns the name of the session.
	 *
	 * @return the session name
	 */
	String name();

}
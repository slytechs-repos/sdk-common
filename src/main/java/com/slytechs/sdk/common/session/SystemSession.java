package com.slytechs.sdk.common.session;

import java.time.Duration;

import com.slytechs.sdk.common.session.state.SystemStateMachine;

/**
 * Top-level session representing a major service or root component in the
 * jNetWorks SDK.
 * 
 * <p>
 * {@code SystemSession} instances are the primary entry points for managing
 * hierarchical lifecycles (e.g., {@code Net} instance). They support full
 * lifecycle operations: starting, stopping, graceful shutdown, forceful
 * shutdown, scheduling, and awaiting completion.
 * </p>
 * 
 * <p>
 * Child sessions (e.g., captures, channels, task scopes) are typically managed
 * as {@link ServiceSession} instances and propagate state changes upward.
 * </p>
 * 
 * <p>
 * Thread-safety: All methods are safe for concurrent access, with internal
 * synchronization where required for state transitions and shutdown
 * coordination.
 * </p>
 * 
 * @see ServiceSession for child/component sessions
 * @see ClientSession for read-only views
 * @see Schedulable for time-based shutdown
 * @see Awaitable for waiting on completion
 * @see Shutdownable for shutdown operations
 * @see CloseableSession for try-with-resources support
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public interface SystemSession
		extends CloseableSession, Shutdownable, Schedulable, Awaitable {

	Duration DEFAULT_TIMEOUT = Session.DEFAULT_TIMEOUT;

	@Override
	SystemStateMachine state();
}
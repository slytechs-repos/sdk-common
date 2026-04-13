package com.slytechs.sdk.common.session;

import java.time.Duration;

import com.slytechs.sdk.common.session.state.ComponentStateMachine;

/**
 * Represents a managed subsystem or component session within the jNetWorks SDK
 * hierarchy.
 * 
 * <p>
 * {@code ServiceSession} instances are children of a {@link SystemSession}
 * (e.g., capture, channel, task scope) and support partial lifecycle
 * operations: shutdown and deletion. They do not support scheduling or explicit
 * start (activation is managed by the parent).
 * </p>
 * 
 * <p>
 * Shutdown propagates upward: when all subsystems of a service are terminated,
 * the parent service may automatically transition to terminated state.
 * </p>
 * 
 * <p>
 * Thread-safety: All methods are safe for concurrent access.
 * </p>
 * 
 * @see SystemSession for parent/root sessions with full control
 * @see ClientSession for read-only views
 * @see Shutdownable for shutdown operations
 * @see Deletable for deletion
 * @see Awaitable for awaiting completion
 * @see CloseableSession for try-with-resources support
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public interface ComponentSession extends Session, Shutdownable, Deletable {
	Duration DEFAULT_TIMEOUT = Session.DEFAULT_TIMEOUT;

	@Override
	ComponentStateMachine state();
}
package com.slytechs.sdk.common.session;

import java.time.Duration;

import com.slytechs.sdk.common.session.state.SessionState;

/**
 * Base interface for all session types in the jNetWorks SDK.
 * 
 * <p>
 * A {@code Session} represents a managed entity with a lifecycle (created, active, shutting down, terminated)
 * and optional hierarchical relationships (parent/child). It provides read-only access to the current state
 * and basic lifecycle queries.
 * </p>
 * 
 * <p>
 * All concrete session implementations implement this interface. Specialized session types add capabilities
 * such as shutdown, scheduling, awaiting, or deletion through additional marker interfaces.
 * </p>
 * 
 * <p>Thread-safety: All methods are safe for concurrent access from any thread.</p>
 * 
 * @see SystemSession          for top-level sessions with full lifecycle control
 * @see ServiceSession        for managed component sessions with limited controls
 * @see ClientSession           for read-only status views
 * @see SessionState            for detailed state and transition information
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public interface Session {
	
	Duration DEFAULT_TIMEOUT = Duration.ofMillis(500);


    /**
     * Returns whether this session is currently active (able to perform work).
     * 
     * <p>
     * A session is active from the moment it is started until shutdown begins.
     * Once shutdown is initiated (graceful or forceful), this method returns {@code false}.
     * </p>
     * 
     * @return {@code true} if the session is active, {@code false} otherwise
     */
    boolean isActive();

    /**
     * Returns the current state machine managing this session's lifecycle.
     * 
     * <p>
     * The returned {@link SessionState} provides detailed information about the current state,
     * previous state, transition history, and any scheduled shutdowns.
     * </p>
     * 
     * <p>Use this method for advanced state inspection or debugging. For simple checks,
     * prefer {@link #isActive()}, {@link SystemSession#isShutdown()}, etc.</p>
     * 
     * @return the session state machine (never null)
     * @see SessionState
     */
    SessionState state();

    /**
     * Returns the parent session in the hierarchy, if any.
     * 
     * <p>
     * Top-level sessions (e.g., {@link SystemSession} roots) return {@code null}.
     * Child sessions (e.g., captures, channels, tasks) return their parent.
     * </p>
     * 
     * <p>Hierarchy is used for shutdown propagation and resource management.</p>
     * 
     * @return the parent session, or {@code null} if this is a root session
     */
    default Session parentSession() {
        return null;
    }
}
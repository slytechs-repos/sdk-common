package com.slytechs.sdk.common.session;
/**
 * Read-only view of a session, providing status and state queries only.
 * 
 * <p>
 * {@code ClientSession} instances are safe for concurrent access from user/application threads
 * and do not allow any mutating operations (start, stop, shutdown, delete, etc.).
 * </p>
 * 
 * <p>
 * Typically obtained from a parent session or factory for monitoring or status reporting.
 * </p>
 * 
 * <p>Thread-safety: All methods are safe for concurrent access from any thread.</p>
 * 
 * @see SystemSession          for full control sessions
 * @see ServiceSession        for managed component sessions
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public interface ClientSession extends Session {
    // No additional methods — inherits isActive() and state() from Session
}
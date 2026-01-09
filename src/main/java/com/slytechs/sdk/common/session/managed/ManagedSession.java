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
package com.slytechs.sdk.common.session.managed;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.slytechs.sdk.common.session.Session;
import com.slytechs.sdk.common.session.message.RenderMode;
import com.slytechs.sdk.common.util.Registration;

/**
 * Extended session interface that provides tree structure, event hooks, and
 * diagnostic rendering for internal session management.
 * 
 * <p>
 * ManagedSession is the internal API used by session implementations to coordinate
 * lifecycle, track dependencies, and provide debugging capabilities. It is not
 * exposed to end users, who see only the {@link Session} interface.
 * </p>
 * 
 * <p>
 * Key features:
 * <ul>
 * <li><b>Tree structure</b> - Navigate parent/child relationships</li>
 * <li><b>Event hooks</b> - Register callbacks for shutdown, termination, etc.</li>
 * <li><b>Diagnostic rendering</b> - Generate tree visualization of session state</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Example hierarchy and rendering:
 * <pre>
 * PcapBackend [name=pcap, state=RUNNING→SHUTDOWN, active=3]
 * ├── PcapCapture [name=hello-capture, state=RUNNING]
 * │   └── dispatch-loop: waiting for pcap_dispatch [2.3s]
 * ├── PacketChannel [name=hello-channel, state=DRAINING]
 * │   ├── backend: BlockingBackChannel→DrainingBackChannel
 * │   └── queue-drain: draining (15→3) packets [1.1s]
 * └── TaskScope [name=scope-1, state=TERMINATED]
 * </pre>
 * </p>
 * 
 * <p>
 * <b>Thread Safety:</b> All methods are designed for concurrent access. Event
 * callbacks are invoked asynchronously and should not block.
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see Session
 * @see ManagedState
 */
public interface ManagedSession extends Session {

	/**
	 * Returns the managed state for this session.
	 *
	 * @return the managed state
	 */
	ManagedState managedState();

	/**
	 * Returns the parent session in the hierarchy, if any.
	 *
	 * @return the parent session, or empty if this is a root
	 */
	Optional<ManagedSession> parentSession();

	/**
	 * Returns an unmodifiable list of child sessions.
	 *
	 * @return the list of children
	 */
	List<ManagedSession> childSessions();

	/**
	 * Registers a callback to be invoked when shutdown is initiated.
	 * 
	 * <p>
	 * The callback is invoked after {@link #shutdown()} is called but before
	 * the session begins its shutdown sequence.
	 * </p>
	 *
	 * @param action the callback to invoke
	 * @return this session for method chaining
	 */
	ManagedSession onShutdown(Runnable action);

	/**
	 * Registers a callback to be invoked when the session terminates.
	 * 
	 * <p>
	 * The callback is invoked after all children have terminated and this
	 * session is fully stopped.
	 * </p>
	 *
	 * @param action the callback to invoke
	 * @return this session for method chaining
	 */
	ManagedSession onTermination(Runnable action);

	/**
	 * Registers a callback to be invoked when a child session is attached.
	 *
	 * @param action the callback to invoke with the attached child
	 * @return a registration to remove the listener
	 */
	Registration onChildAttached(Consumer<ManagedSession> action);

	/**
	 * Registers a callback to be invoked when a child session is detached.
	 *
	 * @param action the callback to invoke with the detached child
	 * @return a registration to remove the listener
	 */
	Registration onChildDetached(Consumer<ManagedSession> action);

	/**
	 * Renders a tree visualization of this session and its descendants.
	 * 
	 * <p>
	 * Uses {@link RenderMode#TRANSITION} for showing state changes.
	 * </p>
	 *
	 * @return the rendered tree string
	 */
	default String renderTree() {
		return renderTree(RenderMode.TRANSITION);
	}

	/**
	 * Renders a tree visualization with the specified render mode.
	 *
	 * @param mode the render mode for lazy arguments
	 * @return the rendered tree string
	 */
	String renderTree(RenderMode mode);

	/**
	 * Renders a single-line summary of this session's state.
	 * 
	 * <p>
	 * Example: {@code "PacketChannel [name=hello-channel, state=DRAINING, waits=1]"}
	 * </p>
	 *
	 * @return the summary string
	 */
	String renderSummary();
}
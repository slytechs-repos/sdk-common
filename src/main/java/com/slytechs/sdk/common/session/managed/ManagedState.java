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

import com.slytechs.sdk.common.session.message.MessageRecord;
import com.slytechs.sdk.common.util.Registration;

/**
 * Extended session state interface that provides tree structure, wait tracking,
 * and freeze semantics for managed sessions.
 * 
 * <p>
 * ManagedState builds on {@link com.slytechs.sdk.common.session.SessionState} by adding:
 * <ul>
 * <li><b>Tree structure</b> - Parent/child relationships for session hierarchy</li>
 * <li><b>Wait tracking</b> - Information about what is blocking termination</li>
 * <li><b>Freeze semantics</b> - Safe handling of terminated session state</li>
 * <li><b>Event notification</b> - Callbacks for state transitions</li>
 * </ul>
 * </p>
 * 
 * <p>
 * The tree structure enables visualization of the complete session hierarchy:
 * <pre>
 * PcapBackend [name=pcap, state=RUNNING→SHUTDOWN]
 * ├── PcapCapture [name=hello-capture, state=RUNNING]
 * │   └── dispatch-loop: waiting for pcap_dispatch [2.3s]
 * └── PacketChannel [name=hello-channel, state=DRAINING]
 *     └── queue-drain: draining (15→3) packets [1.1s]
 * </pre>
 * </p>
 * 
 * <p>
 * Wait tracking captures diagnostic information about pending operations:
 * <pre>{@code
 * state.register("dispatch-loop", 
 *     MessageRecord.of("waiting for pcap_dispatch on port {}", portName));
 * }</pre>
 * When the operation completes, the registration is deregistered.
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see ManagedSession
 * @see ManagedStateMachine
 */
public interface ManagedState {

	/**
	 * State transition events for notification callbacks.
	 */
	enum StateTransition {
		/** Session started running */
		STARTED,
		/** Shutdown was scheduled */
		SHUTDOWN_SCHEDULED,
		/** Shutdown was initiated */
		SHUTDOWN_INITIATED,
		/** Session terminated */
		TERMINATED,
		/** Session was frozen (diagnostic state preserved) */
		FROZEN
	}

	/**
	 * Returns the name of this state (typically matches the session name).
	 *
	 * @return the state name
	 */
	String name();

	/**
	 * Returns the parent state in the hierarchy, if any.
	 *
	 * @return the parent state, or empty if this is a root
	 */
	Optional<ManagedState> parent();

	/**
	 * Returns an unmodifiable list of child states.
	 *
	 * @return the list of children
	 */
	List<ManagedState> children();

	/**
	 * Adds a child state to this state's hierarchy.
	 *
	 * @param child the child state to add
	 * @throws IllegalStateException if this state is terminated
	 * @throws IllegalArgumentException if the child already has a parent
	 */
	void addChild(ManagedState child);

	/**
	 * Removes a child state from this state's hierarchy.
	 *
	 * @param child the child state to remove
	 */
	void removeChild(ManagedState child);

	/**
	 * Registers a wait condition with a descriptive message.
	 * 
	 * <p>
	 * Use this to track what is blocking session termination:
	 * <pre>{@code
	 * Registration reg = state.register("queue-drain",
	 *     MessageRecord.of("draining {} packets", lazy(queue::size)));
	 * 
	 * // When complete:
	 * reg.unregister();
	 * }</pre>
	 * </p>
	 *
	 * @param name    the identifier for this wait condition
	 * @param message the log record describing what is being waited for
	 * @return a registration that can be used to deregister when complete
	 */
	Registration register(String name, MessageRecord message);

	/**
	 * Returns an unmodifiable list of pending wait conditions.
	 *
	 * @return the list of pending waits
	 */
	List<WaitInfo> pendingWaits();

	/**
	 * Checks if there are any pending wait conditions.
	 *
	 * @return true if there are pending waits
	 */
	default boolean hasPendingWaits() {
		return !pendingWaits().isEmpty();
	}

	/**
	 * Freezes this state, causing all wait messages to use snapshot values.
	 * 
	 * <p>
	 * This is called when the session terminates to preserve diagnostic
	 * information in a safe state that won't throw exceptions when rendered.
	 * </p>
	 */
	void freeze();

	/**
	 * Checks if this state is frozen.
	 *
	 * @return true if frozen
	 */
	boolean isFrozen();

	/**
	 * Registers a callback for state transitions.
	 *
	 * @param listener the callback to invoke on state changes
	 * @return a registration to remove the listener
	 */
	Registration onStateChange(Consumer<StateTransition> listener);

	/**
	 * Checks if the session is currently running.
	 *
	 * @return true if running
	 */
	boolean isRunning();

	/**
	 * Checks if shutdown has been initiated.
	 *
	 * @return true if shutdown
	 */
	boolean isShutdown();

	/**
	 * Checks if a shutdown is scheduled.
	 *
	 * @return true if shutdown scheduled
	 */
	boolean isShutdownScheduled();

	/**
	 * Checks if the session is terminated.
	 *
	 * @return true if terminated
	 */
	boolean isTerminated();

	/**
	 * Returns a string representation of the current state flags.
	 * 
	 * <p>
	 * Example: "RUNNING" or "RUNNING→SHUTDOWN" for transitions
	 * </p>
	 *
	 * @return the state string
	 */
	String stateString();
}
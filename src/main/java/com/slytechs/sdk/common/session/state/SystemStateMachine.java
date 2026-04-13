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

import com.slytechs.sdk.common.session.state.StateHierarchyTree.ZeroCountCallback;
import com.slytechs.sdk.common.session.state.SystemStateMachine.SystemState;
import com.slytechs.sdk.common.session.state.recorder.LogLevel;
import com.slytechs.sdk.common.util.Registration;

/**
 * State machine for session lifecycle management.
 * 
 * <p>
 * Manages the standard lifecycle states: CREATED → RUNNING → SHUTDOWN →
 * TERMINATED
 * </p>
 * 
 * <p>
 * Composes with:
 * </p>
 * <ul>
 * <li>{@link StateHierarchyTree} - parent/child tracking and component counting</li>
 * <li>{@link TransitionScheduler} - scheduled shutdown (shutdownAfter/At)</li>
 * <li>{@link StateWaitBarrier} - await termination</li>
 * </ul>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class SystemStateMachine
		extends StateMachine<SystemState>
		implements SessionState, HierarchalState, CountableState {

	public enum SystemState implements State<SystemState> {
		CREATED(true),
		RUNNING(true),
		SHUTDOWN(true) {
			@Override
			public boolean canTransistion(SystemState newState) {
				return newState == TERMINATED;
			}
		},
		TERMINATED(false) {
			@Override
			public String futureTense() {
				return "termination";
			}

			@Override
			public String pastTense() {
				return "terminated";
			}

			@Override
			public String presentTense() {
				return "terminating";
			}
		};

		private final boolean transitionsAllowed;

		SystemState(boolean transitionsAllowed) {
			this.transitionsAllowed = transitionsAllowed;
		}

		@Override
		public boolean canTransistion(SystemState newState) {
			return transitionsAllowed;
		}
	}

	private final StateHierarchyTree<SystemState> components;
	private final TransitionScheduler<SystemState> shutdownScheduler;
	private final StateWaitBarrier<SystemState> terminatedBarrier;

	/**
	 * Creates a lifecycle state machine.
	 *
	 * @param name                    the session name
	 * @param scheduledShutdownAction action to execute on shutdown transition
	 */
	public SystemStateMachine(String name, ZeroCountCallback scheduledShutdownAction) {
		super(name, SystemState.CREATED);

		// On zero, transition to TERMINATED
		this.components = new StateHierarchyTree<>(this, SystemState.TERMINATED);

		// When scheduled is triggered, call this action
		this.shutdownScheduler = new TransitionScheduler<>(this, scheduledShutdownAction);

		// Trigger signal, and awake barrier when terminated is reached
		this.terminatedBarrier = new StateWaitBarrier<>(this, SystemState.TERMINATED);
	}

	/**
	 * Awaits termination indefinitely.
	 *
	 * @throws InterruptedException if interrupted while waiting
	 */
	public void awaitTerminated() throws InterruptedException {
		terminatedBarrier.await(SystemState.TERMINATED);
	}

	/**
	 * @see com.slytechs.sdk.common.session.state.SessionState#awaitTerminated(java.time.Duration)
	 */
	public boolean awaitTerminated(Duration timeout) throws InterruptedException {
		return terminatedBarrier.await(timeout, SystemState.TERMINATED);
	}

	public void cancelScheduledShutdown() {
		shutdownScheduler.cancel();
	}

	/**
	 * Returns the component hierarchy for this lifecycle.
	 *
	 * @return the component tree
	 */
	@Override
	public StateHierarchyTree<SystemState> components() {
		return components;
	}

	@Override
	public void decrement() {
		components.decrement();
	}

	/**
	 * Deregisters a component, decrementing the active count. When count reaches
	 * zero, transitions to TERMINATED.
	 */
	public void deregister() {
		components.decrement();
	}

	@Override
	public void increment() {
		components.increment();
	}

	public boolean isCreated() {
		return currentState() == SystemState.CREATED;
	}

	public boolean isRunning() {
		return currentState() == SystemState.RUNNING;
	}

	public boolean isShutdown() {
		return currentState() == SystemState.SHUTDOWN;
	}

	public boolean isShutdownScheduled() {
		return shutdownScheduler.isScheduled();
	}

	public boolean isTerminated() {
		return currentState() == SystemState.TERMINATED;
	}

	/**
	 * Registers a component, incrementing the active count.
	 */
	public void register() {
		components.increment();
	}

	/**
	 * @see com.slytechs.sdk.common.session.state.HierarchalState#registerParent(com.slytechs.sdk.common.session.state.StateHierarchyTree)
	 */
	@Override
	public Registration registerParent(HierarchalState parent) {
		return components.registerParent(parent.components());
	}

	/**
	 * Registers this lifecycle as a child of a parent lifecycle.
	 *
	 * @param parent the parent lifecycle
	 * @return registration to detach from parent
	 */
	public Registration registerParent(SystemStateMachine parent) {
		return components.registerParent(parent.components); // Correct - passes StateHierarchyTree
	}

	/**
	 * Renders this lifecycle and its children as an ASCII tree.
	 *
	 * @return formatted tree string
	 */
	public String renderTree() {
		return renderTree(LogLevel.INFO);
	}

	/**
	 * Renders this lifecycle and its children as an ASCII tree.
	 *
	 * @return formatted tree string
	 */
	public String renderTree(LogLevel level) {
		return StateTreeRenderer.builder()
				.showRecords(true)
				.showThreadInfo(true)
				.threshold(level)
				.render(components);
	}

	/**
	 * Transitions to SHUTDOWN state.
	 *
	 * @return true if transition occurred
	 */
	public boolean shutdown() {
		shutdownScheduler.cancel();
		return transitionTo(SystemState.SHUTDOWN);
	}

	/**
	 * Schedules a shutdown after the specified duration.
	 *
	 * @param duration time until shutdown
	 * @return registration to cancel the scheduled shutdown
	 */
	public Registration shutdownAfter(Duration duration) {
		return shutdownScheduler.scheduleAfter(duration);
	}

	/**
	 * Schedules a shutdown at the specified time.
	 *
	 * @param atTime the time to shutdown
	 * @return registration to cancel the scheduled shutdown
	 */
	public Registration shutdownAt(Instant atTime) {
		return shutdownScheduler.shutdownAt(atTime);
	}

	/**
	 * Transitions to RUNNING state.
	 *
	 * @return true if transition occurred
	 */
	public boolean start() {
		return transitionTo(SystemState.RUNNING);
	}
	
	/**
	 * Forces immediate transition to TERMINATED state.
	 * Used by shutdownNow() to bypass component drain wait.
	 *
	 * @return true if transition occurred
	 */
	public boolean terminate() {
	    return transitionTo(SystemState.TERMINATED);
	}
}
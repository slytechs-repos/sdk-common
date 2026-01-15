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
import java.util.concurrent.TimeUnit;

import com.slytechs.sdk.common.session.SessionState;
import com.slytechs.sdk.common.session.state.ComponentTree.ZeroCountCallback;
import com.slytechs.sdk.common.session.state.LifecycleStateMachine.LifecycleState;
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
 * <li>{@link ComponentTree} - parent/child tracking and component counting</li>
 * <li>{@link TransitionScheduler} - scheduled shutdown (shutdownAfter/At)</li>
 * <li>{@link StateZeroCountBarrier} - await termination</li>
 * </ul>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class LifecycleStateMachine
		extends StateMachine<LifecycleState>
		implements SessionState, HierarchalState, CountableState {

	public enum LifecycleState implements State<LifecycleState> {
		CREATED(true),
		RUNNING(true),
		SHUTDOWN(true) {
			@Override
			public boolean canTransistion(LifecycleState newState) {
				return newState == TERMINATED;
			}
		},
		TERMINATED(false) {
			@Override
			public String futureTense() {
				return "termination";
			}

			@Override
			public String presentTense() {
				return "terminating";
			}

			@Override
			public String pastTense() {
				return "terminated";
			}
		};

		private final boolean transitionsAllowed;

		LifecycleState(boolean transitionsAllowed) {
			this.transitionsAllowed = transitionsAllowed;
		}

		@Override
		public boolean canTransistion(LifecycleState newState) {
			return transitionsAllowed;
		}
	}

	private final ComponentTree<LifecycleState> components;
	private final TransitionScheduler<LifecycleState> shutdownScheduler;
	private final StateZeroCountBarrier<LifecycleState> terminatedBarrier;

	/**
	 * Creates a lifecycle state machine.
	 *
	 * @param name                    the session name
	 * @param scheduledShutdownAction action to execute on shutdown transition
	 */
	public LifecycleStateMachine(String name, ZeroCountCallback scheduledShutdownAction) {
		super(name, LifecycleState.CREATED);

		// On zero, transition to TERMINATED
		this.components = new ComponentTree<>(this, LifecycleState.TERMINATED);

		// When scheduled is triggered, call this action
		this.shutdownScheduler = new TransitionScheduler<>(this, scheduledShutdownAction);

		// Trigger signal, and awake barrier when terminated is reached
		this.terminatedBarrier = new StateZeroCountBarrier<>(this, LifecycleState.TERMINATED);
	}

	/**
	 * Returns the component hierarchy for this lifecycle.
	 *
	 * @return the component tree
	 */
	@Override
	public ComponentTree<LifecycleState> components() {
		return components;
	}

	/**
	 * Transitions to RUNNING state.
	 *
	 * @return true if transition occurred
	 */
	public boolean start() {
		return transitionTo(LifecycleState.RUNNING);
	}

	/**
	 * Transitions to SHUTDOWN state.
	 *
	 * @return true if transition occurred
	 */
	public boolean shutdown() {
		shutdownScheduler.cancel();
		return transitionTo(LifecycleState.SHUTDOWN);
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

	public void cancelScheduledShutdown() {
		shutdownScheduler.cancel();
	}

	/**
	 * Registers a component, incrementing the active count.
	 */
	public void register() {
		components.increment();
	}

	/**
	 * Deregisters a component, decrementing the active count. When count reaches
	 * zero, transitions to TERMINATED.
	 */
	public void deregister() {
		components.decrement();
	}

	/**
	 * Registers this lifecycle as a child of a parent lifecycle.
	 *
	 * @param parent the parent lifecycle
	 * @return registration to detach from parent
	 */
	public Registration registerParent(LifecycleStateMachine parent) {
		return components.registerParent(parent.components); // Correct - passes ComponentTree
	}

	/**
	 * Awaits termination indefinitely.
	 *
	 * @throws InterruptedException if interrupted while waiting
	 */
	public void await() throws InterruptedException {
		terminatedBarrier.await(LifecycleState.TERMINATED);
	}

	/**
	 * @see com.slytechs.sdk.common.session.SessionState#await(long,
	 *      java.util.concurrent.TimeUnit)
	 */
	public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
		return terminatedBarrier.await(LifecycleState.TERMINATED, timeout, unit);
	}

	/**
	 * @see com.slytechs.sdk.common.session.SessionState#await(java.time.Duration)
	 */
	public boolean await(Duration timeout) throws InterruptedException {
		return terminatedBarrier.await(LifecycleState.TERMINATED, timeout.toMillis(), TimeUnit.MILLISECONDS);
	}

	public boolean isCreated() {
		return currentState() == LifecycleState.CREATED;
	}

	public boolean isRunning() {
		return currentState() == LifecycleState.RUNNING;
	}

	public boolean isShutdown() {
		return currentState() == LifecycleState.SHUTDOWN;
	}

	public boolean isShutdownScheduled() {
		return shutdownScheduler.isScheduled();
	}

	public boolean isTerminated() {
		return currentState() == LifecycleState.TERMINATED;
	}

	/**
	 * Renders this lifecycle and its children as an ASCII tree.
	 *
	 * @return formatted tree string
	 */
	public String renderTree() {
		return TreeRenderer.render(components);
	}

	@Override
	public void increment() {
		components.increment();
	}

	@Override
	public void decrement() {
		components.decrement();
	}

	/**
	 * @see com.slytechs.sdk.common.session.state.HierarchalState#registerParent(com.slytechs.sdk.common.session.state.ComponentTree)
	 */
	@Override
	public Registration registerParent(HierarchalState parent) {
		return components.registerParent(parent.components());
	}
}
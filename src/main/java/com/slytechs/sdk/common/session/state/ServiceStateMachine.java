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
import java.util.function.Function;

import com.slytechs.sdk.common.session.state.Recovery.Default;
import com.slytechs.sdk.common.session.state.ServiceStateMachine.ServiceState;
import com.slytechs.sdk.common.util.Registration;

/**
 * State machine for restartable services and background processes.
 * 
 * <p>
 * Manages the lifecycle of any service that can be started, stopped, and
 * restarted. The STOPPED state preserves resources while pausing activity,
 * allowing restart without reinitialization. The SHUTDOWN state enables orderly
 * termination from any active state.
 * </p>
 * 
 * <h2>State Transitions</h2>
 * 
 * <pre>
 *            start()
 * CREATED ──────────────→ ACTIVE ←─────────┐
 *                           │              │
 *                    stop() │              │ restart()
 *                           ▼              │
 *                        STOPPED ──────────┘
 *                           │
 *         ┌─────────────────┼─────────────────┐
 *         │                 │                 │
 *         │ shutdown()      │ shutdown()      │ shutdown()
 *         ▼                 ▼                 ▼
 *      (from ACTIVE)   (from STOPPED)   (from CREATED)
 *         │                 │                 │
 *         └────────────────▶│◀────────────────┘
 *                           ▼
 *                       SHUTDOWN
 *                           │
 *                terminate()│
 *                           ▼
 *                      TERMINATED
 * </pre>
 * 
 * <h2>Common Use Cases</h2>
 * <ul>
 * <li>RX/TX port threads (pcap_dispatch, rx_burst, inject loops)</li>
 * <li>Background daemon threads</li>
 * <li>Polling loops and schedulers</li>
 * <li>Connection handlers</li>
 * <li>Any pausable/resumable processing loop</li>
 * </ul>
 * 
 * <h2>Reactive State Handling</h2>
 * <p>
 * State transitions trigger callbacks via {@link #observeTransisions}.
 * Implementations use this to acquire/release resources reactively:
 * <ul>
 * <li>ACTIVE - open handles, start processing</li>
 * <li>STOPPED - close handles, pause processing</li>
 * <li>SHUTDOWN/TERMINATED - cleanup all resources</li>
 * </ul>
 * </p>
 * 
 * <h2>Await Methods</h2>
 * <p>
 * Provides blocking await methods for thread synchronization:
 * <ul>
 * <li>{@link #awaitActive()} - blocks until ACTIVE</li>
 * <li>{@link #awaitActiveOrShutdown()} - blocks until ACTIVE, SHUTDOWN, or
 * TERMINATED</li>
 * <li>{@link #awaitTermination()} - blocks until TERMINATED</li>
 * </ul>
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see SystemStateMachine
 * @see TaskStateMachine
 */
public class ServiceStateMachine extends StateMachine<ServiceState>
		implements CountableState, HierarchalState {

	public enum ServiceState implements State<ServiceState> {

		/** Handle allocated, not yet capturing. */
		CREATED(true) {
			@Override
			public boolean canTransistion(ServiceState newState) {
				return newState == ACTIVE || newState == TERMINATED;
			}
		},

		/** Dispatch/poll loop active, packets flowing. */
		ACTIVE(true) {
			@Override
			public boolean canTransistion(ServiceState newState) {
				return newState == STOPPED || newState == TERMINATED || newState == SHUTDOWN;
			}
		},

		/** Loop exited via breakloop, handle still open. Can restart. */
		STOPPED(true) {
			@Override
			public boolean canTransistion(ServiceState newState) {
				return newState == ACTIVE || newState == TERMINATED || newState == SHUTDOWN;
			}
		},

		ERROR(true) {
			@Override
			public boolean canTransistion(ServiceState newState) {
				return newState == ACTIVE || newState == TERMINATED || newState == SHUTDOWN;
			}

		},

		/** Indicates the port is shutting down followed by transition to terminated */
		SHUTDOWN(true) {
			@Override
			public boolean canTransistion(ServiceState newState) {
				return newState == TERMINATED;
			}

			@Override
			public String presentTense() {
				return "shutting down";
			}
		},

		/** Handle closed, resources freed. Terminal state. */
		TERMINATED(false) {
			@Override
			public String futureTense() {
				return "termination";
			}

			@Override
			public String presentTense() {
				return "terminating";
			}
		};

		private final boolean transitionsAllowed;

		ServiceState(boolean transitionsAllowed) {
			this.transitionsAllowed = transitionsAllowed;
		}

		@Override
		public boolean canTransistion(ServiceState newState) {
			return transitionsAllowed;
		}
	}

	private final StateHierarchyTree<ServiceState> components;
	private final StateWaitBarrier<ServiceState> barrier;
	private final TransitionScheduler<ServiceState> scheduler;
	private final ErrorPolicy<Recovery.Default> errorPolicy;

	/**
	 * Creates a new source state machine.
	 *
	 * @param name the source name (e.g., port name)
	 */
	public ServiceStateMachine(String name) {
		super(name, ServiceState.CREATED);
		this.components = new StateHierarchyTree<>(this, ServiceState.TERMINATED);
		this.barrier = new StateWaitBarrier<>(this, ServiceState.TERMINATED, ServiceState.ACTIVE);
		this.scheduler = new TransitionScheduler<>(this, ServiceState.TERMINATED);
		this.errorPolicy = new ErrorPolicy<>(this, Default.FAIL);
	}

	/**
	 * @return
	 * @see com.slytechs.sdk.common.session.state.TransitionScheduler#isScheduled()
	 */
	public boolean isScheduled() {
		return scheduler.isScheduled();
	}

	/**
	 * @param duration
	 * @return
	 * @throws IllegalStateException
	 * @see com.slytechs.sdk.common.session.state.TransitionScheduler#scheduleAfter(java.time.Duration)
	 */
	public Registration shutdownAfter(Duration duration) throws IllegalStateException {
		return scheduler.scheduleAfter(duration);
	}

	/**
	 * 
	 * @see com.slytechs.sdk.common.session.state.TransitionScheduler#cancel()
	 */
	public void cancelScheduledShutdown() {
		scheduler.cancel();
	}

	public void awaitActive() throws InterruptedException {
		barrier.await(ServiceState.ACTIVE);
	}

	public void awaitActiveOrShutdown() throws InterruptedException {
		barrier.await(ServiceState.SHUTDOWN, ServiceState.TERMINATED, ServiceState.ACTIVE);
	}

	public void awaitTermination() throws InterruptedException {
		barrier.await(ServiceState.TERMINATED);
	}

	public boolean awaitTermination(Duration timeout) throws InterruptedException {
		return barrier.await(timeout, ServiceState.TERMINATED);
	}

	/**
	 * @see com.slytechs.sdk.common.session.state.HierarchalState#components()
	 */
	@Override
	public StateHierarchyTree<?> components() {
		return components;
	}

	@Override
	public void decrement() {
		components.decrement();
	}

	public boolean error() {
		return transitionTo(ServiceState.ERROR);
	}

	public ErrorPolicy<Recovery.Default> errorPolicy() {
		return this.errorPolicy;
	}

	/**
	 * @return
	 * @see com.slytechs.sdk.common.session.state.ErrorPolicy#hasError()
	 */
	public boolean hasError() {
		return errorPolicy.hasError();
	}

	@Override
	public void increment() {
		components.increment();
	}

	/**
	 * Checks if source is active (not terminated).
	 *
	 * @return true if CREATED, ACTIVE, or STOPPED
	 */
	public boolean isActive() {
		return currentState() != ServiceState.TERMINATED;
	}

	public boolean isCreated() {
		return currentState() == ServiceState.CREATED;
	}

	/**
	 * Checks if source is running (capturing packets).
	 *
	 * @return true if ACTIVE
	 */
	public boolean isRunning() {
		return currentState() == ServiceState.ACTIVE;
	}

	public boolean isShutdown() {
		return currentState() == ServiceState.SHUTDOWN;
	}

	public boolean isStopped() {
		return currentState() == ServiceState.STOPPED;
	}

	public boolean isTerminated() {
		return currentState() == ServiceState.TERMINATED;
	}

	/**
	 * @param handler
	 * @return
	 * @see com.slytechs.sdk.common.session.state.ErrorPolicy#onError(java.util.function.Function)
	 */
	public ErrorPolicy<Default> onError(Function<ErrorContext, Default> handler) {
		return errorPolicy.onError(handler);
	}

	/**
	 * @see com.slytechs.sdk.common.session.state.HierarchalState#registerParent(com.slytechs.sdk.common.session.state.HierarchalState)
	 */
	@Override
	public Registration registerParent(HierarchalState parent) {
		return components.registerParent(parent.components());
	}

	@Override
	public synchronized void reset() {
		super.reset();
	}

	/**
	 * Transitions from STOPPED back to ACTIVE. Resumes capture loop.
	 *
	 * @return true if transition succeeded
	 */
	public boolean restart() {
		return transitionTo(ServiceState.ACTIVE);
	}

	/**
	 * Transitions from STOPPED or ACTIVE to SHUTDOWN. Exits capture loop and
	 * transitions to TERMINATED.
	 *
	 * @return true if transition succeeded
	 */
	public boolean shutdown() {
		return transitionTo(ServiceState.SHUTDOWN);
	}

	/**
	 * Transitions to ACTIVE state. Starts the capture loop.
	 *
	 * @return true if transition succeeded
	 */
	public boolean start() {
		return transitionTo(ServiceState.ACTIVE);
	}

	/**
	 * Transitions to STOPPED state. Pauses capture, handle remains open.
	 *
	 * @return true if transition succeeded
	 */
	public boolean stop() {
		return transitionTo(ServiceState.STOPPED);
	}

	/**
	 * Transitions to TERMINATED state. Closes handle, frees resources.
	 *
	 * @return true if transition succeeded
	 */
	public boolean terminate() {
		return transitionTo(ServiceState.TERMINATED);
	}

	/**
	 * @return
	 * @see com.slytechs.sdk.common.session.state.ErrorPolicy#uncaughtException()
	 */
	public Throwable uncaughtException() {
		return errorPolicy.uncaughtException();
	}

	/**
	 * @throws InterruptedException
	 * 
	 */
	public void awaitStop() throws InterruptedException {
		barrier.await(ServiceState.STOPPED);
	}

}
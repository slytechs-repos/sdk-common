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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.slytechs.sdk.common.session.SessionState;
import com.slytechs.sdk.common.session.StateMachine;
import com.slytechs.sdk.common.session.message.MessageRecord;
import com.slytechs.sdk.common.util.Registration;

/**
 * Implementation of {@link ManagedState} that wraps a {@link StateMachine}
 * and adds tree structure, wait tracking, and freeze semantics.
 * 
 * <p>
 * ManagedStateMachine provides the observability layer for session management
 * without modifying the core {@link StateMachine} implementation. It uses
 * composition to delegate state operations while adding:
 * <ul>
 * <li>Hard references to parent and children for tree navigation</li>
 * <li>Wait tracking with lazy-evaluated messages</li>
 * <li>Freeze semantics for safe terminated state rendering</li>
 * <li>State transition notifications</li>
 * </ul>
 * </p>
 * 
 * <p>
 * <b>Thread Safety:</b> All operations are thread-safe. Collections use
 * copy-on-write semantics for safe iteration during concurrent modification.
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public final class ManagedStateMachine implements ManagedState, SessionState {

	private final StateMachine delegate;
	private final AtomicReference<ManagedState> parent = new AtomicReference<>();
	private final CopyOnWriteArrayList<ManagedState> children = new CopyOnWriteArrayList<>();
	private final CopyOnWriteArrayList<WaitInfo> pendingWaits = new CopyOnWriteArrayList<>();
	private final CopyOnWriteArrayList<Consumer<StateTransition>> stateListeners = new CopyOnWriteArrayList<>();
	private final AtomicBoolean frozen = new AtomicBoolean(false);

	private volatile String previousState = null;

	/**
	 * Creates a new ManagedStateMachine wrapping the given StateMachine.
	 *
	 * @param delegate the underlying state machine
	 */
	public ManagedStateMachine(StateMachine delegate) {
		this.delegate = Objects.requireNonNull(delegate, "delegate");
		this.previousState = captureStateString();
	}

	/**
	 * Creates a new ManagedStateMachine with a new StateMachine.
	 *
	 * @param name           the session name
	 * @param shutdownAction the action to run on scheduled shutdown
	 */
	public ManagedStateMachine(String name, Runnable shutdownAction) {
		this(new StateMachine(name, shutdownAction));
	}

	/**
	 * Returns the underlying StateMachine delegate.
	 *
	 * @return the delegate state machine
	 */
	public StateMachine delegate() {
		return delegate;
	}

	@Override
	public String name() {
		return delegate.name();
	}

	@Override
	public Optional<ManagedState> parent() {
		return Optional.ofNullable(parent.get());
	}

	@Override
	public List<ManagedState> children() {
		return Collections.unmodifiableList(children);
	}

	@Override
	public void addChild(ManagedState child) {
		Objects.requireNonNull(child, "child");

		if (delegate.isTerminated()) {
			throw new IllegalStateException("Cannot add child to terminated session: " + name());
		}

		if (child instanceof ManagedStateMachine managedChild) {
			if (!managedChild.parent.compareAndSet(null, this)) {
				throw new IllegalArgumentException("Child already has a parent: " + child.name());
			}
		}

		children.add(child);
		delegate.register();
	}

	@Override
	public void removeChild(ManagedState child) {
		Objects.requireNonNull(child, "child");

		if (children.remove(child)) {
			if (child instanceof ManagedStateMachine managedChild) {
				managedChild.parent.set(null);
			}
			delegate.deregister();
		}
	}

	@Override
	public Registration register(String name, MessageRecord message) {
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(message, "message");

		WaitInfo waitInfo = new WaitInfo(name, message);
		pendingWaits.add(waitInfo);
		delegate.register();

		return () -> {
			if (pendingWaits.remove(waitInfo)) {
				delegate.deregister();
			}
		};
	}

	@Override
	public List<WaitInfo> pendingWaits() {
		return Collections.unmodifiableList(pendingWaits);
	}

	@Override
	public void freeze() {
		if (frozen.compareAndSet(false, true)) {
			pendingWaits.forEach(WaitInfo::freeze);
			notifyListeners(StateTransition.FROZEN);
		}
	}

	@Override
	public boolean isFrozen() {
		return frozen.get();
	}

	@Override
	public Registration onStateChange(Consumer<StateTransition> listener) {
		Objects.requireNonNull(listener, "listener");
		stateListeners.add(listener);
		return () -> stateListeners.remove(listener);
	}

	private void notifyListeners(StateTransition transition) {
		for (Consumer<StateTransition> listener : stateListeners) {
			try {
				listener.accept(transition);
			} catch (Exception e) {
				// Log but don't propagate
			}
		}
	}

	@Override
	public boolean isRunning() {
		return delegate.isRunning();
	}

	@Override
	public boolean isShutdown() {
		return delegate.isShutdown();
	}

	@Override
	public boolean isShutdownScheduled() {
		return delegate.isShutdownScheduled();
	}

	@Override
	public boolean isTerminated() {
		return delegate.isTerminated();
	}

	@Override
	public void await() throws InterruptedException {
		delegate.await();
	}

	@Override
	public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
		return delegate.await(timeout, unit);
	}

	@Override
	public String stateString() {
		String current = captureStateString();

		if (previousState != null && !previousState.equals(current)) {
			return previousState + "→" + current;
		}

		return current;
	}

	private String captureStateString() {
		if (isTerminated()) {
			return "TERMINATED";
		} else if (isShutdown()) {
			return "SHUTDOWN";
		} else if (isShutdownScheduled()) {
			return "SHUTDOWN_SCHEDULED";
		} else if (isRunning()) {
			return "RUNNING";
		} else {
			return "CREATED";
		}
	}

	/**
	 * Updates the previous state for transition tracking.
	 * Call this after state changes to capture the transition.
	 */
	public void captureState() {
		this.previousState = captureStateString();
	}

	/**
	 * Enables the state machine (delegates to StateMachine).
	 *
	 * @return this instance for chaining
	 */
	public ManagedStateMachine enable() {
		String before = captureStateString();
		delegate.enable();
		if (!before.equals(captureStateString())) {
			notifyListeners(StateTransition.STARTED);
		}
		return this;
	}

	/**
	 * Initiates shutdown (delegates to StateMachine).
	 *
	 * @return true if shutdown was initiated
	 */
	public boolean shutdown() {
		boolean result = delegate.shutdown();
		if (result) {
			notifyListeners(StateTransition.SHUTDOWN_INITIATED);
		}
		return result;
	}

	/**
	 * Initiates immediate shutdown (delegates to StateMachine).
	 *
	 * @return true if shutdown was initiated
	 */
	public boolean shutdownNow() {
		boolean result = delegate.shutdownNow();
		if (result) {
			freeze();
			notifyListeners(StateTransition.TERMINATED);
		}
		return result;
	}

	/**
	 * Closes and cleans up resources.
	 */
	public void close() {
		freeze();
		delegate.close();
	}

	@Override
	public String toString() {
		return "ManagedStateMachine [" +
				"name=" + name() +
				", state=" + stateString() +
				", children=" + children.size() +
				", waits=" + pendingWaits.size() +
				", frozen=" + frozen.get() +
				"]";
	}
}
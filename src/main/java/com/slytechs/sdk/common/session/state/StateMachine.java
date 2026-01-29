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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import com.slytechs.sdk.common.session.state.recorder.StateRecorder;
import com.slytechs.sdk.common.session.state.recorder.StateSnapshot;
import com.slytechs.sdk.common.util.Named;
import com.slytechs.sdk.common.util.Registration;

/**
 * Generic state machine with transition rules, observers, and state-triggered
 * actions.
 * 
 * <p>
 * StateMachine provides:
 * </p>
 * <ul>
 * <li>Type-safe states via enum implementing {@link State}</li>
 * <li>Transition validation based on state rules</li>
 * <li>Observer notifications on transitions</li>
 * <li>Per-state action callbacks</li>
 * <li>Previous state tracking for transition rendering</li>
 * </ul>
 * 
 * <p>
 * Subclasses define domain-specific state enums and compose with helper
 * components like {@link StateHierarchyTree}, {@link TransitionScheduler}, and
 * {@link StateWaitBarrier}.
 * </p>
 *
 * @param <T> the state enum type
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class StateMachine<T extends Enum<T> & State<T>> implements Named, SessionState {

	private final String name;
	private final T initialState;
	private final AtomicReference<T> currentState = new AtomicReference<>();
	private final Map<T, List<Runnable>> actions = new HashMap<>();
	private final Collection<TransitionObserver<T>> observers = new LinkedList<>();
	private final StateRecorder recorder;
	private long generationId;

	private volatile T previousState;

	/**
	 * Creates a state machine with the given name and initial state.
	 * 
	 * <p>
	 * The machine starts in a "not yet initialized" state. Call {@link #reset()} to
	 * transition to the initial state and trigger any registered actions.
	 * </p>
	 *
	 * @param name         the machine name for identification
	 * @param initialState the state to use on reset
	 */
	public StateMachine(String name, T initialState) {
		this.name = name;
		this.initialState = initialState;
		this.recorder = new StateRecorder(this);
		this.currentState.set(initialState);

		observeTransisions(this::recordStateTransitions);
	}

	private void recordStateTransitions(StateMachine<T> source, T oldState, T newState) {
		recorder.log("{} changed state {} -> {}", name(), oldState, newState);
	}

	/**
	 * Returns the current state.
	 *
	 * @return current state, or null if not yet initialized
	 */
	public final T currentState() {
		return currentState.get();
	}

	public final long generationId() {
		return this.generationId;
	}

	public T initialState() {
		return initialState;
	}

	@Override
	public final String name() {
		return name;
	}

	/**
	 * Returns the previous state before the last transition.
	 *
	 * @return previous state, or null if no transitions have occurred
	 */
	public final T previousState() {
		return previousState;
	}

	public StateRecorder recorder() {
		return recorder;
	}

	/**
	 * Registers an action to execute when entering a specific state.
	 *
	 * @param state  the state that triggers the action
	 * @param action the action to execute
	 * @return registration handle to remove the action
	 */
	public final synchronized Registration registerAction(T state, Runnable action) {
		List<Runnable> list = actions.computeIfAbsent(state, _ -> new ArrayList<>());
		list.add(action);
		return () -> list.remove(action);
	}

	/**
	 * Registers an observer for state transitions.
	 *
	 * @param observer the observer to notify
	 * @return registration handle to remove the observer
	 */
	public final synchronized Registration registerTransitionObserver(TransitionObserver<T> observer) {
		observers.add(observer);
		return () -> observers.remove(observer);
	}

	/**
	 * Registers an observer for state transitions including the initial state if
	 * its current. The initial state notification happens immediately on
	 * registration if the current state is at initial and notify flag is true.
	 *
	 * @param observer             the observer to notify transitions, including the
	 *                             initial if desired
	 * @param notifyOnInitialState the notify on initial state
	 * @return this state machine for fluent method chaining
	 */
	public final StateMachine<T> observeTransisions(TransitionObserver<T> observer, boolean notifyOnInitialState) {
		registerTransitionObserver(observer);

		if (notifyOnInitialState && currentState() == initialState)
			observer.onStateTransition(this, null, initialState);

		return this;
	}

	/**
	 * Registers an observer for state transitions.
	 *
	 * @param observer the observer
	 * @return this state machine for fluent method chaining
	 */
	public final StateMachine<T> observeTransisions(TransitionObserver<T> observer) {
		return observeTransisions(observer, false);
	}

	/**
	 * Resets the state machine to its initial state.
	 * 
	 * <p>
	 * This clears current and previous state, then transitions to the initial
	 * state. All actions and observers for the initial state will be triggered.
	 * </p>
	 */
	protected synchronized void reset() {
		this.currentState.set(null);
		this.previousState = null;
		this.generationId++;

		transitionTo(initialState);
	}

	/**
	 * A read-only snapshot of this current state machine's state.
	 *
	 * @return the current state machine info
	 */
	public StateSnapshot<T> stateSnapshot() {
		return new StateSnapshot<>(this);
	}

	@Override
	public String toString() {
		T current = currentState.get();
		T previous = previousState;

		String stateStr = (previous != null && previous != current)
				? previous + "→" + current
				: String.valueOf(current);

		return "%s[name=%s, state=%s]".formatted(
				getClass().getSimpleName(), name, stateStr);
	}

	/**
	 * Attempts to transition to a new state.
	 * 
	 * <p>
	 * The transition will:
	 * </p>
	 * <ol>
	 * <li>Validate the transition is allowed by the current state</li>
	 * <li>Update current and previous state</li>
	 * <li>Execute any registered actions for the new state</li>
	 * <li>Notify all registered observers</li>
	 * </ol>
	 *
	 * @param newState the target state
	 * @return true if transition occurred, false if already in that state or
	 *         transition is not allowed
	 */
	protected final synchronized boolean tryTransitionTo(T newState) {
		Objects.requireNonNull(newState, "newState");

		if (!currentState.get().canTransistion(newState))
			return false;

		return transitionTo(newState);
	}

	/**
	 * Attempts to transition to a new state.
	 * 
	 * <p>
	 * The transition will:
	 * </p>
	 * <ol>
	 * <li>Validate the transition is allowed by the current state</li>
	 * <li>Update current and previous state</li>
	 * <li>Execute any registered actions for the new state</li>
	 * <li>Notify all registered observers</li>
	 * </ol>
	 *
	 * @param newState the target state
	 * @return true if transition occurred, false if already in that state
	 * @throws IllegalStateException if transition is not allowed
	 */
	protected final synchronized boolean transitionTo(T newState) throws IllegalStateException {
		Objects.requireNonNull(newState, "newState");

		T current = currentState.get();
		if (current == newState)
			return false;

		if (!current.canTransistion(newState))
			throw new IllegalStateException(
					"Cannot transition from %s to %s in machine '%s'"
							.formatted(current.presentTense(), newState.presentTense(), name));

		this.previousState = current;
		this.currentState.set(newState);

		// Execute state-entry actions
		List<Runnable> stateActions = actions.get(newState);
		if (stateActions != null) {
			stateActions.forEach(Runnable::run);
		}

		// Notify observers
		final T prevState = previousState;
		observers.forEach(o -> o.onStateTransition(this, prevState, newState));

		return true;
	}
}
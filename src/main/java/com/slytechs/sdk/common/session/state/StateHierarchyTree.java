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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.slytechs.sdk.common.util.Named;
import com.slytechs.sdk.common.util.Registration;

/**
 * Tree-based implementation of {@link ComponentHierarchy} with single parent.
 * 
 * <p>
 * StateHierarchyTree tracks parent/child relationships and component counts
 * within a single state domain. When the component count reaches zero, a
 * configurable callback is invoked - typically to trigger a state transition.
 * </p>
 * 
 * <p>
 * Component counting propagates up the tree: when a node's count goes from 0→1,
 * it increments its parent's count. When it goes from 1→0, it decrements the
 * parent and invokes the zero-callback.
 * </p>
 *
 * @param <T> the state enum type
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class StateHierarchyTree<T extends Enum<T> & State<T>> implements Named {

	public interface ZeroCountCallback {
		void run() throws Exception;
	}

	private final StateMachine<T> stateMachine;
	private final ZeroCountCallback onZeroCallback;

	private StateHierarchyTree<?> parent;
	private Registration parentRegistration;
	private final List<StateHierarchyTree<?>> children = new LinkedList<>();
	private long count = 0;

	/**
	 * Creates a tree node linked to a state machine with auto-transition on zero.
	 * 
	 * <p>
	 * When component count reaches zero, the state machine will transition to the
	 * specified state.
	 * </p>
	 *
	 * @param machine         the owning state machine
	 * @param transitionState state to transition to when count reaches zero
	 */
	public StateHierarchyTree(StateMachine<T> machine, T transitionState) {
		this(machine, () -> machine.transitionTo(transitionState));
	}

	/**
	 * Creates a tree node linked to a state machine with custom zero-callback.
	 *
	 * @param machine        the owning state machine
	 * @param onZeroCallback callback invoked when component count reaches zero
	 */
	public StateHierarchyTree(StateMachine<T> machine, ZeroCountCallback onZeroCallback) {
		this.stateMachine = machine;
		this.onZeroCallback = onZeroCallback;
	}

	@Override
	public String name() {
		return stateMachine.name();
	}

	public StateMachine<T> stateMachine() {
		return stateMachine;
	}

	public synchronized Optional<StateHierarchyTree<?>> parent() {
		return Optional.ofNullable(parent);
	}

	public synchronized List<StateHierarchyTree<?>> children() {
		return Collections.unmodifiableList(children);
	}

	public synchronized long count() {
		return count;
	}

	public synchronized Registration registerParent(StateHierarchyTree<?> newParent) {
		if (this.parent != null)
			throw new IllegalStateException(
					"Cannot register with parent '%s', already registered with '%s'"
							.formatted(newParent.name(), this.parent.name()));

		this.parent = newParent;
		this.parentRegistration = newParent.registerChild(this);

		// If we have components, notify parent
		if (count > 0)
			parent.increment();

		return () -> deregisterParent(newParent);
	}

	private synchronized void deregisterParent(StateHierarchyTree<?> previousParent) {
		if (parent != previousParent)
			return;

		// If we have components, decrement parent before detaching
		if (count > 0)
			parent.decrement();

		parentRegistration.unregister();
		parentRegistration = null;
		parent = null;
	}

	public synchronized Registration registerChild(StateHierarchyTree<?> child) {
		children.add(child);
		return () -> children.remove(child);
	}

	public synchronized void increment() {
		count++;

		// First component: propagate to parent
		if (count == 1 && parent != null)
			parent.increment();
	}

	public synchronized void decrement() {
		if (count <= 0)
			throw new IllegalStateException(
					"Component count underflow in '%s' - possible double deregister".formatted(name()));

		count--;

		try {
			if (count == 0) {
				onZeroCallback.run();

				// Propagate to parent
				if (parent != null)
					parent.decrement();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public String toString() {
		return "StateHierarchyTree[name=%s, state=%s, count=%d, children=%d]".formatted(
				name(),
				stateMachine.currentState(),
				count,
				children.size());
	}
}
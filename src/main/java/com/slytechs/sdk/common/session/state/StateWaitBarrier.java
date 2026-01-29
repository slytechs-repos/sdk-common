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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.slytechs.sdk.common.session.state.recorder.StateRecord;

/**
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class StateWaitBarrier<T extends Enum<T> & State<T>> {

	@SafeVarargs
	private static <T> Set<T> ofSet(T first, T... args) {
		Set<T> set = new HashSet<>();
		set.add(first);
		set.addAll(Set.of(args));

		return set;
	}

	private final ReentrantLock monitorLock = new ReentrantLock();
	private final Condition awaitCondition = monitorLock.newCondition();
	private final Set<T> monitorStates;
	private final StateMachine<T> state;

	private final AtomicReference<StateRecord> topRecord = new AtomicReference<>();

	public StateWaitBarrier(StateMachine<T> state, Collection<T> monitorStates) {
		if (monitorStates.isEmpty())
			throw new IllegalArgumentException("monitor states is empty, must provide atleast 1 await state");

		this.monitorStates = new HashSet<>(monitorStates);
		this.state = state;

		state.registerTransitionObserver(this::onStateTransition);
	}

	@SafeVarargs
	public StateWaitBarrier(StateMachine<T> state, T monitorState, T... monitorStates) {
		this(state, ofSet(monitorState, monitorStates));
	}

	@SuppressWarnings("unchecked")
	public boolean await(Duration timeout, T awaitState) throws InterruptedException {
		return awaitAnyState(timeout, awaitState);
	}

	@SuppressWarnings("unchecked")
	public void await(T awaitState, T... awaitStates) throws InterruptedException {
		Set<T> waitSet = ofSet(awaitState, awaitStates);
		if (!monitorStates.containsAll(waitSet) || waitSet.isEmpty())
			throw new IllegalArgumentException("Not monitoring the supplied awaitState "
					+ waitSet + ", monitoring=" + monitorStates);

		if (topRecord.get() == null)
			topRecord.compareAndSet(null, state.recorder()
					.log("Initiated wait state for state transition {} at {}", awaitStates, Instant.now()));

		StateRecord top = topRecord.get();

		top.log("Thread {} waiting on transition to {} at {}",
				Thread.currentThread().getName(), awaitStates, Instant.now());

		monitorLock.lock();

		try {
			while (!waitSet.contains(state.currentState())) {
				awaitCondition.await();
			}

			top.log("Thread {} received signal for transition to {} at {}",
					Thread.currentThread().getName(), awaitStates, Instant.now());

		} catch (InterruptedException e) {
			top.log("Interrupted thread {} waiting on transition to {} at {}",
					Thread.currentThread().getName(), awaitStates, Instant.now());

			Thread.interrupted();
			throw e;
		} finally {
			monitorLock.unlock();
			topRecord.compareAndSet(top, null);
		}
	}

	@SuppressWarnings("unchecked")
	public boolean awaitAnyState(Duration timeout, T awaitState, T... awaitStates)
			throws InterruptedException {

		long timeoutAt = System.nanoTime() + timeout.toNanos();
		Set<T> waitSet = ofSet(awaitState, awaitStates);
		if (!monitorStates.containsAll(waitSet) || waitSet.isEmpty())
			throw new IllegalArgumentException("Not monitoring the supplied awaitStates "
					+ waitSet + ", monitoring=" + monitorStates);

		monitorLock.lock();

		try {
			long remaining = timeoutAt - System.nanoTime();
			while (remaining > 0) {

				boolean signalled = awaitCondition.await(remaining, TimeUnit.NANOSECONDS);
				if (signalled && waitSet.contains(state.currentState()))
					return true;

				remaining = timeoutAt - System.nanoTime();
			}

			return false;

		} finally {
			monitorLock.unlock();
		}
	}

	private void onStateTransition(StateMachine<T> source, T oldState, T newState) {
		if (monitorStates.contains(newState)) {
			monitorLock.lock();
			try {
				if (topRecord.get() != null)
					topRecord.get().log("Signal waiters transitioned from {} to {} at {}",
							oldState, newState, Instant.now());

				awaitCondition.signalAll();
			} finally {
				monitorLock.unlock();
			}
		}
	}
}

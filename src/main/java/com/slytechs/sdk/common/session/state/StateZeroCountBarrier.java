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

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.slytechs.sdk.common.session.state.recorder.StateRecord;

/**
 * 
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class StateZeroCountBarrier<T extends Enum<T> & State<T>> {

	private final ReentrantLock monitorLock = new ReentrantLock();
	private final Condition awaitCondition = monitorLock.newCondition();
	private final T awaitState;
	private final StateMachine<T> state;
	private final AtomicReference<StateRecord> topRecord = new AtomicReference<>();;

	/**
	 * 
	 */
	public StateZeroCountBarrier(StateMachine<T> state, T awaitState) {
		this.awaitState = awaitState;
		this.state = state;

		state.registerTransitionObserver(this::onStateTransition);
	}

	private void onStateTransition(StateMachine<T> source, T oldState, T newState) {
		if (awaitState == newState) {
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

	public void await(T awaitState) throws InterruptedException {

		if (topRecord.get() == null)
			topRecord.compareAndSet(null, state.recorder()
					.log("Initiated wait state for state transition {} at {}", awaitState, Instant.now()));

		StateRecord top = topRecord.get();

		top.log("Thread {} waiting on transition to {} at {}",
				Thread.currentThread().getName(), awaitState, Instant.now());

		monitorLock.lock();

		try {
			while (state.currentState() != awaitState) {
				awaitCondition.await();
			}

			top.log("Thread {} received signal for transition to {} at {}",
					Thread.currentThread().getName(), awaitState, Instant.now());

		} catch (InterruptedException e) {
			top.log("Interrupted thread {} waiting on transition to {} at {}",
					Thread.currentThread().getName(), awaitState, Instant.now());

			Thread.interrupted();
			throw e;
		} finally {
			monitorLock.unlock();
			topRecord.compareAndSet(top, null);
		}
	}

	public boolean await(T awaitState, long timeout, TimeUnit unit) throws InterruptedException {
		monitorLock.lock();

		try {
			if (state.currentState() == awaitState) {
				return true;
			}

			boolean signalled = awaitCondition.await(timeout, unit);
			if (signalled) {
				return true;
			}

			return false;

		} finally {
			monitorLock.unlock();
		}
	}
}

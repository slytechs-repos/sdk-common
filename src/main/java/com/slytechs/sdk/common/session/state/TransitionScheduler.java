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
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.slytechs.sdk.common.session.state.ComponentTree.ZeroCountCallback;
import com.slytechs.sdk.common.session.state.recorder.StateRecord;
import com.slytechs.sdk.common.util.Registration;

/**
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class TransitionScheduler<T extends Enum<T> & State<T>> {
	private final ScheduledExecutorService scheduler;
	private final StateMachine<T> state;
	private final T transitionState;
	private final ZeroCountCallback scheduledAction;
	private ScheduledFuture<Void> scheduledFuture;
	private StateRecord topLevelRecord;

	public TransitionScheduler(StateMachine<T> state, T transitionState) {
		this.transitionState = Objects.requireNonNull(transitionState, "transitionState");
		this.scheduledAction = null;
		this.state = state;
		this.scheduler = Executors.newSingleThreadScheduledExecutor(
				Thread.ofVirtual()
						.name(state.name() + "-scheduler")
						.factory());

	}

	public TransitionScheduler(StateMachine<T> state, ZeroCountCallback action) {
		this.transitionState = null;
		this.scheduledAction = Objects.requireNonNull(action, "action");
		this.state = state;
		this.scheduler = Executors.newSingleThreadScheduledExecutor(
				Thread.ofVirtual()
						.name(state.name() + "-scheduler")
						.factory());

	}

	public synchronized boolean isScheduled() {
		return scheduledFuture != null && !scheduledFuture.isDone();
	}

	public synchronized Registration scheduleAfter(Duration duration) throws IllegalStateException {
		cancel();

		this.topLevelRecord = transitionState == null
				? state.recorder().log("Scheduled action after {}", duration)
				: state.recorder().log("Scheduled transition to {} after {}", transitionState, duration);

		ScheduledFuture<Void> future = scheduler.schedule(() -> {

			if (transitionState == null) {
				this.topLevelRecord.log("Executed action at {}", Instant.now());
				scheduledAction.run();
			} else {
				this.topLevelRecord.log("Transitioning from {} to {} at {}",
						state.currentState(),
						transitionState,
						Instant.now());
				state.transitionTo(transitionState);

			}

			reset();

			return null;
		}, duration.toMillis(), TimeUnit.MILLISECONDS);

		this.scheduledFuture = future;

		return () -> cancelFuture(future);
	}

	private void reset() {
		scheduledFuture = null;
		topLevelRecord = null;
	}

	public Registration shutdownAt(Instant atTime) {
		var now = Instant.now();
		Duration duration = Duration.between(now, atTime);

		if (atTime.isBefore(now))
			throw new IllegalArgumentException("%s atTime must be in the future: %s".formatted(
					transitionState.futureTense(),
					atTime));

		return scheduleAfter(duration);

	}

	private synchronized void cancelFuture(ScheduledFuture<Void> future) {
		if (future == null || scheduledFuture != future)
			return; // empty or previously cancelled

		if (transitionState == null)
			this.topLevelRecord.log("Cancelled action at {}", Instant.now());
		else
			this.topLevelRecord.log("Cancelled transition to {} at {}", transitionState, Instant.now());

		scheduledFuture.cancel(true);
		reset();
	}

	public synchronized void cancel() {
		cancelFuture(scheduledFuture);
	}
}

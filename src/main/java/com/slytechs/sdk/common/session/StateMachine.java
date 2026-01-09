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
package com.slytechs.sdk.common.session;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.slytechs.sdk.common.util.Named;
import com.slytechs.sdk.common.util.Registration;

/**
 * Manages the lifecycle of a {@link Session} in the jNetworks SDK, tracking
 * states: running, shutdown scheduled, shutdown, and terminated. Uses
 * {@link AtomicInteger} for dynamic component tracking and {@link Condition}
 * for waiting on completion. Supports multi-tier hierarchies with dynamic
 * parent-child linking, allowing lower tiers to shut down independently while
 * upper tiers wait for all children to complete. All operations are thread-safe
 * for concurrent access in multi-threaded environments.
 *
 * <p>
 * <b>Lifecycle Stages:</b>
 * </p>
 * <ul>
 * <li><b>Running ({@link #isRunning()}):</b> Session is active (e.g., capturing
 * packets). True after opening, false after shutdown.</li>
 * <li><b>Shutdown Scheduled ({@link #isShutdownScheduled()}):</b> Shutdown is
 * scheduled but not yet initiated.</li>
 * <li><b>Shutdown ({@link #isShutdown()}):</b> Shutdown has started, stopping
 * new operations while completing in-progress tasks.</li>
 * <li><b>Terminated ({@link #isTerminated()}):</b> All tasks and components
 * have completed after shutdown.</li>
 * </ul>
 *
 * <p>
 * <b>Hierarchy and Component Tracking:</b>
 * </p>
 * <p>
 * Supports dynamic registration of components (e.g., sub-sessions) via
 * {@link #register()} and {@link #deregister()}. Lower tiers can register with
 * a parent via {@link #registerWithParent(Session)} or
 * {@link #addChild(StateMachine)}, enabling upper tiers to wait for
 * children. Simple sessions (e.g., Config) may return constant states or no-op
 * for shutdown.
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public final class StateMachine implements Named, SessionState {

	private static final Logger logger = LoggerFactory.getLogger(StateMachine.class);

	private static Thread createDaemonThread(Runnable r) {
		Thread t = new Thread(r);
		t.setDaemon(true); // Use daemon threads
		return t;
	}

	private final String name;
	private final Runnable action;
	private final AtomicBoolean isRunning = new AtomicBoolean();
	private final AtomicBoolean isShutdownScheduled = new AtomicBoolean();
	private final AtomicBoolean isShutdown = new AtomicBoolean();
	private final AtomicBoolean isTerminated = new AtomicBoolean();
	private final AtomicInteger activeComponents;
	private final ReentrantLock lock = new ReentrantLock();
	private final Condition completed = lock.newCondition();
	private final AtomicReference<ScheduledFuture<Boolean>> future = new AtomicReference<>();
	private final AtomicReference<StateMachine> parent = new AtomicReference<>();
	private final ScheduledExecutorService scheduler = Executors
			.newSingleThreadScheduledExecutor(StateMachine::createDaemonThread);

	/**
	 * Constructs a new StateMachine with an initial number of components and no
	 * parent.
	 *
	 * @param name           the name of the session
	 * @param count          the initial number of components
	 * @param shutdownAction executed when scheduled shutdowns are triggered
	 */
	public StateMachine(String name, int count, Runnable shutdownAction) {
		this.name = name;
		this.action = shutdownAction;
		this.activeComponents = new AtomicInteger(count);
	}

	/**
	 * Constructs a new StateMachine with no initial components and no parent.
	 *
	 * @param name           the name of the session
	 * @param shutdownAction executed when scheduled shutdowns are triggered
	 */
	public StateMachine(String name, Runnable shutdownAction) {
		this(name, 0, shutdownAction);
	}

	/**
	 * Registers a child StateMachine with this StateMachine, setting its
	 * parent to this instance.
	 *
	 * @param child the child StateMachine
	 * @return a Registration to unregister the child
	 * @throws IllegalStateException if this session is terminated or the child
	 *                               already has a parent
	 */
	public StateMachine addChild(StateMachine child) {
		lock.lock();
		try {
			if (isTerminated.get()) {
				throw new IllegalStateException("Cannot add child to terminated session: " + name);
			}
			logger.trace("Adding child {} to parent '{}'", child.name(), name);
			activeComponents.incrementAndGet();
			if (!child.parent.compareAndSet(null, this)) {
				throw new IllegalStateException("Child already has a parent: " + child.name());
			}
		} finally {
			lock.unlock();
		}

		return this;
	}

	/**
	 * Waits indefinitely until all components have completed (activeComponents=0).
	 *
	 * @throws InterruptedException if the thread is interrupted
	 */
	@Override
	public void await() throws InterruptedException {
		lock.lock();

		try {
			while (!isTerminated.get()) {
				logger.debug("Awaiting termination of session '{}' (active components: {})", name, activeComponents.get());
				completed.await();
			}
			logger.debug("Session '{}' has terminated", name);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Waits until all components have completed or the specified timeout elapses.
	 *
	 * @param timeout the maximum time to wait
	 * @param unit    the time unit of the timeout
	 * @return true if all components completed, false if timeout elapsed
	 * @throws InterruptedException if the thread is interrupted
	 */
	@Override
	public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
		lock.lock();

		try {
			if (isTerminated.get()) {
				logger.debug("Session '{}' already terminated", name);
				return true;
			}
			logger.debug("Awaiting termination of session '{}' with timeout {} {}", name, timeout, unit);

			boolean signalled = completed.await(timeout, unit);
			if (signalled && activeComponents.get() == 0) {
				isTerminated.set(true);
				logger.debug("Session '{}' terminated within timeout", name);
				return true;
			}
			logger.debug("Timeout elapsed awaiting session '{}' termination (active components: {})", name, activeComponents.get());
			return false;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Cancels any scheduled shutdown if one is active.
	 */
	public void cancelScheduledShutdown() {
		if (!isShutdownScheduled.compareAndSet(true, false))
			return;

		ScheduledFuture<Boolean> f = future.getAndSet(null);
		if (f != null) {
			f.cancel(false);
			logger.debug("Cancelled scheduled shutdown for session '{}'", name);
		}
	}

	/**
	 * Closes the session state, releasing internal resources.
	 */
	public void close() {
		scheduler.shutdownNow();
		logger.debug("Scheduler shutdown for session '{}'", name);
	}

	/**
	 * Deregisters a component, marking it as complete. If all components are
	 * complete, sets terminated=true and signals the parent if present.
	 */
	public void deregister() {
		int remaining = activeComponents.decrementAndGet();
		logger.trace("Component deregistered from session '{}' (remaining: {})", name, remaining);

		if (remaining == 0) {
			lock.lock();
			try {
				isTerminated.set(true);
				completed.signalAll();

				logger.info("Session '{}' has fully terminated (all components complete)", name);

			} finally {
				lock.unlock();
			}

			StateMachine parentState = parent.get();
			if (parentState != null) {
				logger.debug("Notifying parent session '{}' of child termination", parentState.name());
				parentState.deregister();
			}
		}
	}

	/**
	 * Enables the running state if not already running.
	 *
	 * @return true if the running state was enabled, false if already running
	 */
	public StateMachine enable() {
		start();

		return this;
	}

	private IllegalArgumentException invalidSessionError() {
		return new IllegalArgumentException("Invalid session state, must be of type StateMachine");
	}

	/**
	 * Checks if the session is running.
	 *
	 * @return true if running, false otherwise
	 */
	@Override
	public boolean isRunning() {
		return isRunning.get();
	}

	/**
	 * Checks if shutdown has been initiated.
	 *
	 * @return true if shutdown, false otherwise
	 */
	@Override
	public boolean isShutdown() {
		return isShutdown.get();
	}

	/**
	 * Checks if a shutdown is scheduled.
	 *
	 * @return true if scheduled, false otherwise
	 */
	@Override
	public boolean isShutdownScheduled() {
		return isShutdownScheduled.get();
	}

	/**
	 * Checks if the session is terminated.
	 *
	 * @return true if terminated, false otherwise
	 */
	@Override
	public boolean isTerminated() {
		return isTerminated.get();
	}

	/**
	 * Returns the session name.
	 *
	 * @return the name
	 */
	@Override
	public String name() {
		return name;
	}

	/**
	 * Registers a new component for tracking.
	 */
	public void register() {
		if (!isTerminated.get()) {
			int count = activeComponents.incrementAndGet();
			logger.trace("Component registered with session '{}' (active components: {})", name, count);
		}
	}

	/**
	 * Registers this StateMachine with a parent Session.
	 *
	 * @param parent the parent Session
	 * @return a Registration to unregister from the parent
	 * @throws IllegalArgumentException if the parent is not a
	 *                                  StateMachine-based session
	 * @throws IllegalStateException    if this session already has a parent or the
	 *                                  parent is terminated
	 */
	public StateMachine registerWithParent(StateMachine parent) {
		parent.addChild(this);

		return this;
	}

	/**
	 * Registers this StateMachine with a parent Session.
	 *
	 * @param parent the parent Session
	 * @return a Registration to unregister from the parent
	 * @throws IllegalArgumentException if the parent is not a
	 *                                  StateMachine-based session
	 * @throws IllegalStateException    if this session already has a parent or the
	 *                                  parent is terminated
	 */
	public StateMachine registerWithParent(SessionState parent) {
		return registerWithParent((StateMachine) parent);
	}

	/**
	 * Resets the state for session restart.
	 *
	 * @throws IllegalStateException if not terminated
	 */
	public void reset() {
		if (!isTerminated.get() || activeComponents.get() != 0) {
			throw new IllegalStateException("Cannot reset session state when not fully terminated");
		}
		enable();
	}

	/**
	 * Initiates a graceful shutdown.
	 *
	 * @return true if shutdown was initiated, false if already shutdown
	 */
	public boolean shutdown() {
		cancelScheduledShutdown();
		boolean wasShutdown = isShutdown.compareAndSet(false, true);
		if (wasShutdown) {
			isRunning.set(false);
			logger.info("Graceful shutdown initiated for session {}", name);
		}

		// Auto-terminate if nothing registered
		if (wasShutdown && activeComponents.get() == 0) {
			lock.lock();
			try {
				isTerminated.set(true);
				completed.signalAll();
				logger.info("Session {} auto-terminated (no active components)", name);
			} finally {
				lock.unlock();
			}
		}

		return wasShutdown;
	}

	/**
	 * Schedules a shutdown after the specified duration.
	 *
	 * @param duration the duration to wait before shutting down
	 * @return a Registration to cancel the scheduled shutdown
	 * @throws IllegalStateException    if already shutdown
	 * @throws NullPointerException     if duration is null
	 * @throws IllegalArgumentException if duration is negative or zero
	 */
	public Registration shutdownAfter(Duration duration) {
		if (action == null)
			throw new UnsupportedOperationException();
		if (isShutdown.get())
			throw new IllegalStateException("session already shutdown " + name);
		if (duration == null)
			throw new NullPointerException("duration is null");
		if (duration.isNegative() || duration.isZero())
			throw new IllegalArgumentException("duration must be positive");

		cancelScheduledShutdown();
		isShutdownScheduled.set(true);

		ScheduledFuture<Boolean> scheduledFuture = scheduler.schedule(() -> {
			try {
				action.run();
				return true;
			} finally {
				isShutdownScheduled.set(false);
			}
		}, duration.toMillis(), TimeUnit.MILLISECONDS);

		this.future.set(scheduledFuture);

		logger.debug("Scheduled shutdown for session {} in {} ms", name, duration.toMillis());

		return () -> {
			if (this.future.get() != scheduledFuture)
				return;
			cancelScheduledShutdown();
		};
	}

	/**
	 * Schedules a shutdown at the specified instant.
	 *
	 * @param atTime the instant at which to shutdown
	 * @return a Registration to cancel the scheduled shutdown
	 * @throws IllegalStateException    if already shutdown
	 * @throws IllegalArgumentException if atTime is in the past
	 * @throws NullPointerException     if atTime is null
	 */
	public Registration shutdownAt(Instant atTime) {
		if (action == null)
			throw new UnsupportedOperationException();
		if (atTime == null)
			throw new NullPointerException("atTime is null");

		var now = Instant.now();
		Duration duration = Duration.between(now, atTime);

		if (atTime.isBefore(now))
			throw new IllegalArgumentException("shutdown atTime must be in the future: " + atTime);

		return shutdownAfter(duration);
	}

	/**
	 * Initiates an immediate shutdown.
	 *
	 * @return true if shutdown was initiated, false if already terminated
	 */
	public boolean shutdownNow() {
		if (!isTerminated.compareAndSet(false, true))
			return false;

		cancelScheduledShutdown();
		isShutdown.set(true);
		isRunning.set(false);

		lock.lock();
		try {
			completed.signalAll();
		} finally {
			lock.unlock();
		}

		logger.info("Immediate shutdown executed for session {}", name);

		return true;
	}

	/**
	 * Starts the session, transitioning to the running state.
	 *
	 * @return true if started successfully, false if already running
	 */
	public boolean start() {
		if (!isRunning.compareAndSet(false, true))
			return false;

		logger.debug("Session {} started (now running)", name);
		cancelScheduledShutdown();

		isShutdown.set(false);
		isTerminated.set(false);

		return true;
	}

	@Override
	public String toString() {
		return "StateMachine ["
				+ "name=" + name
				+ ", running=" + isRunning
				+ ", shutdownScheduled=" + isShutdownScheduled
				+ ", shutdown=" + isShutdown
				+ ", terminated=" + isTerminated
				+ ", activeComponents=" + activeComponents
				+ (parent.get() != null ? ", parent=" + parent.get().name() : "")
				+ "]";
	}
}
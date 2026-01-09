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

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Tests for {@link StateMachine} await functionality.
 */
class StateMachineAwaitTest {

	private StateMachine state;

	@BeforeEach
	void setUp() {
		state = new StateMachine("test-session", () -> {});
	}

	@AfterEach
	void tearDown() {
		state.close();
	}

	@Test
	@Timeout(5)
	void awaitReturnsImmediatelyWhenTerminated() throws InterruptedException {
		state.enable();
		state.shutdownNow();

		// Should return immediately
		state.await();
		assertTrue(state.isTerminated());
	}

	@Test
	@Timeout(5)
	void awaitWithTimeoutReturnsImmediatelyWhenTerminated() throws InterruptedException {
		state.enable();
		state.shutdownNow();

		assertTrue(state.await(1, TimeUnit.SECONDS));
	}

	@Test
	@Timeout(5)
	void awaitWithTimeoutReturnsFalseOnTimeout() throws InterruptedException {
		state.enable();
		state.register(); // Prevent auto-terminate
		state.shutdown();

		assertFalse(state.await(100, TimeUnit.MILLISECONDS));
		assertFalse(state.isTerminated());
	}

	@Test
	@Timeout(5)
	void awaitBlocksUntilTermination() throws InterruptedException {
		state.enable();
		state.register();

		AtomicBoolean awaitReturned = new AtomicBoolean(false);
		CountDownLatch started = new CountDownLatch(1);

		Thread waiter = new Thread(() -> {
			try {
				started.countDown();
				state.await();
				awaitReturned.set(true);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});

		waiter.start();
		started.await();

		// Give waiter time to block
		Thread.sleep(50);
		assertFalse(awaitReturned.get());

		// Terminate
		state.shutdown();
		state.deregister();

		waiter.join(1000);
		assertTrue(awaitReturned.get());
	}

	@Test
	@Timeout(5)
	void awaitWithTimeoutBlocksUntilTermination() throws InterruptedException {
		state.enable();
		state.register();

		AtomicBoolean result = new AtomicBoolean(false);
		CountDownLatch started = new CountDownLatch(1);

		Thread waiter = new Thread(() -> {
			try {
				started.countDown();
				result.set(state.await(5, TimeUnit.SECONDS));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});

		waiter.start();
		started.await();

		// Terminate before timeout
		Thread.sleep(50);
		state.shutdown();
		state.deregister();

		waiter.join(1000);
		assertTrue(result.get());
	}

	@Test
	@Timeout(5)
	void awaitCanBeInterrupted() throws InterruptedException {
		state.enable();
		state.register();

		AtomicBoolean wasInterrupted = new AtomicBoolean(false);
		CountDownLatch started = new CountDownLatch(1);

		Thread waiter = new Thread(() -> {
			try {
				started.countDown();
				state.await();
			} catch (InterruptedException e) {
				wasInterrupted.set(true);
			}
		});

		waiter.start();
		started.await();

		Thread.sleep(50);
		waiter.interrupt();

		waiter.join(1000);
		assertTrue(wasInterrupted.get());
	}

	@Test
	@Timeout(5)
	void multipleWaitersAllSignaled() throws InterruptedException {
		state.enable();
		state.register();

		int waiterCount = 5;
		CountDownLatch allStarted = new CountDownLatch(waiterCount);
		CountDownLatch allFinished = new CountDownLatch(waiterCount);

		for (int i = 0; i < waiterCount; i++) {
			new Thread(() -> {
				try {
					allStarted.countDown();
					state.await();
					allFinished.countDown();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}).start();
		}

		allStarted.await();
		Thread.sleep(50);

		// Terminate
		state.shutdown();
		state.deregister();

		assertTrue(allFinished.await(2, TimeUnit.SECONDS));
	}
}
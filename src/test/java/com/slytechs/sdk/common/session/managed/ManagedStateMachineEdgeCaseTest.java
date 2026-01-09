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

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.slytechs.sdk.common.session.message.MessageRecord;

/**
 * Edge case tests for {@link ManagedStateMachine}.
 */
class ManagedStateMachineEdgeCaseTest {

	@Test
	void addChildToTerminatedParent_throws() {
		ManagedStateMachine parent = new ManagedStateMachine("parent", () -> {});
		parent.enable();
		parent.shutdownNow();

		ManagedStateMachine child = new ManagedStateMachine("child", () -> {});

		assertThrows(IllegalStateException.class, () -> parent.addChild(child));
	}

	@Test
	void addSameChildTwice_throws() {
		ManagedStateMachine parent = new ManagedStateMachine("parent", () -> {});
		parent.enable();

		ManagedStateMachine child = new ManagedStateMachine("child", () -> {});
		parent.addChild(child);

		// Try to add to another parent
		ManagedStateMachine parent2 = new ManagedStateMachine("parent2", () -> {});
		parent2.enable();

		assertThrows(IllegalArgumentException.class, () -> parent2.addChild(child));
	}

	@Test
	void removeChildThatWasNeverAdded_noOp() {
		ManagedStateMachine parent = new ManagedStateMachine("parent", () -> {});
		ManagedStateMachine notChild = new ManagedStateMachine("not-child", () -> {});

		// Should not throw
		parent.removeChild(notChild);
		assertTrue(parent.children().isEmpty());
	}

	@Test
	void deeplyNestedHierarchy() {
		ManagedStateMachine root = new ManagedStateMachine("level-0", () -> {});
		root.enable();

		ManagedStateMachine current = root;
		for (int i = 1; i <= 10; i++) {
			ManagedStateMachine child = new ManagedStateMachine("level-" + i, () -> {});
			current.addChild(child);
			current = child;
		}

		// Verify depth
		int depth = 0;
		ManagedStateMachine node = root;
		while (!node.children().isEmpty()) {
			depth++;
			node = (ManagedStateMachine) node.children().get(0);
		}
		assertEquals(10, depth);
	}

	@Test
	void manyChildrenSameLevel() {
		ManagedStateMachine parent = new ManagedStateMachine("parent", () -> {});
		parent.enable();

		for (int i = 0; i < 100; i++) {
			ManagedStateMachine child = new ManagedStateMachine("child-" + i, () -> {});
			parent.addChild(child);
		}

		assertEquals(100, parent.children().size());
	}

	@Test
	void registerUnregisterRapidly() {
		ManagedStateMachine state = new ManagedStateMachine("test", () -> {});
		state.enable();

		for (int i = 0; i < 1000; i++) {
			var reg = state.register("wait-" + i, MessageRecord.of("msg"));
			reg.unregister();
		}

		assertTrue(state.pendingWaits().isEmpty());
	}

	@Test
	void unregisterSameRegistrationTwice() {
		ManagedStateMachine state = new ManagedStateMachine("test", () -> {});
		state.enable();

		var reg = state.register("wait", MessageRecord.of("msg"));
		assertEquals(1, state.pendingWaits().size());

		reg.unregister();
		assertEquals(0, state.pendingWaits().size());

		// Second unregister should be no-op
		reg.unregister();
		assertEquals(0, state.pendingWaits().size());
	}

	@Test
	void stateTransitionsAreIdempotent() {
		ManagedStateMachine state = new ManagedStateMachine("test", () -> {});

		// Multiple enables
		state.enable();
		state.enable();
		state.enable();
		assertTrue(state.isRunning());

		// Multiple shutdowns
		state.shutdown();
		state.shutdown();
		state.shutdown();
		assertTrue(state.isShutdown());
	}

	@Test
	void shutdownNowMultipleTimes() {
		ManagedStateMachine state = new ManagedStateMachine("test", () -> {});
		state.enable();

		assertTrue(state.shutdownNow());  // First time returns true
		assertFalse(state.shutdownNow()); // Subsequent calls return false

		assertTrue(state.isTerminated());
		assertTrue(state.isFrozen());
	}

	@Test
	void freezeMultipleTimes() {
		ManagedStateMachine state = new ManagedStateMachine("test", () -> {});

		state.freeze();
		state.freeze();
		state.freeze();

		assertTrue(state.isFrozen());
	}

	@Test
	void stateStringBeforeAnyTransition() {
		ManagedStateMachine state = new ManagedStateMachine("test", () -> {});

		// No captureState called yet, previousState is initial
		String stateStr = state.stateString();
		assertEquals("CREATED", stateStr);
	}

	@Test
	void captureStatePreservesForNextTransition() {
		ManagedStateMachine state = new ManagedStateMachine("test", () -> {});
		state.enable();

		// Without capture - shows transition
		assertTrue(state.stateString().contains("→"));

		// Capture current state
		state.captureState();

		// Now no transition shown (previous == current)
		assertEquals("RUNNING", state.stateString());

		// Shutdown
		state.shutdown();

		// Shows transition from captured state
		assertEquals("RUNNING→TERMINATED", state.stateString());
	}

	@Test
	void stateChangeListenersCalled() {
		ManagedStateMachine state = new ManagedStateMachine("test", () -> {});
		AtomicInteger callCount = new AtomicInteger(0);

		state.onStateChange(transition -> callCount.incrementAndGet());

		state.enable();
		state.shutdown();
		state.shutdownNow();

		assertTrue(callCount.get() >= 2); // At least STARTED and TERMINATED
	}

	@Test
	void stateChangeListenerException_doesNotPropagate() {
		ManagedStateMachine state = new ManagedStateMachine("test", () -> {});

		state.onStateChange(transition -> {
			throw new RuntimeException("Listener failed");
		});

		// Should not throw
		assertDoesNotThrow(() -> state.enable());
		assertDoesNotThrow(() -> state.shutdownNow());
	}

	@Test
	void removeStateChangeListener() {
		ManagedStateMachine state = new ManagedStateMachine("test", () -> {});
		AtomicInteger callCount = new AtomicInteger(0);

		var reg = state.onStateChange(transition -> callCount.incrementAndGet());

		state.enable();
		int countAfterEnable = callCount.get();

		reg.unregister();

		state.shutdownNow();

		// Count should not have increased after unregister
		assertEquals(countAfterEnable, callCount.get());
	}

	@Test
	void concurrentChildAddRemove() throws InterruptedException {
		ManagedStateMachine parent = new ManagedStateMachine("parent", () -> {});
		parent.enable();

		CountDownLatch latch = new CountDownLatch(1);
		AtomicInteger errors = new AtomicInteger(0);

		Thread adder = new Thread(() -> {
			try {
				latch.await();
				for (int i = 0; i < 100; i++) {
					ManagedStateMachine child = new ManagedStateMachine("add-" + i, () -> {});
					parent.addChild(child);
				}
			} catch (Exception e) {
				errors.incrementAndGet();
			}
		});

		Thread remover = new Thread(() -> {
			try {
				latch.await();
				for (int i = 0; i < 100; i++) {
					var children = parent.children();
					if (!children.isEmpty()) {
						parent.removeChild(children.get(0));
					}
				}
			} catch (Exception e) {
				errors.incrementAndGet();
			}
		});

		adder.start();
		remover.start();
		latch.countDown();

		adder.join(5000);
		remover.join(5000);

		assertEquals(0, errors.get());
	}

	@Test
	void concurrentWaitRegistration() throws InterruptedException {
		ManagedStateMachine state = new ManagedStateMachine("test", () -> {});
		state.enable();

		CountDownLatch latch = new CountDownLatch(1);
		AtomicInteger errors = new AtomicInteger(0);

		Thread[] threads = new Thread[10];
		for (int i = 0; i < threads.length; i++) {
			final int threadId = i;
			threads[i] = new Thread(() -> {
				try {
					latch.await();
					for (int j = 0; j < 100; j++) {
						var reg = state.register("wait-" + threadId + "-" + j,
								MessageRecord.of("msg"));
						Thread.yield();
						reg.unregister();
					}
				} catch (Exception e) {
					errors.incrementAndGet();
				}
			});
		}

		for (Thread t : threads) t.start();
		latch.countDown();
		for (Thread t : threads) t.join(5000);

		assertEquals(0, errors.get());
	}

	@Test
	void awaitWithTimeout() throws InterruptedException {
		ManagedStateMachine state = new ManagedStateMachine("test", () -> {});
		state.enable();
		state.delegate().register(); // Prevent auto-terminate

		// Should timeout since we have active component
		boolean completed = state.await(100, TimeUnit.MILLISECONDS);
		assertFalse(completed);

		// Now terminate
		state.shutdownNow();

		// Should return immediately
		completed = state.await(100, TimeUnit.MILLISECONDS);
		assertTrue(completed);
	}

	@Test
	void closeReleasesResources() {
		ManagedStateMachine state = new ManagedStateMachine("test", () -> {});
		state.enable();
		state.register("wait", MessageRecord.of("msg"));

		state.close();

		assertTrue(state.isFrozen());
	}

	@Test
	void toStringContainsAllInfo() {
		ManagedStateMachine state = new ManagedStateMachine("test-session", () -> {});
		state.enable();

		ManagedStateMachine child = new ManagedStateMachine("child", () -> {});
		state.addChild(child);

		state.register("wait1", MessageRecord.of("msg1"));
		state.register("wait2", MessageRecord.of("msg2"));

		String str = state.toString();

		assertTrue(str.contains("test-session"));
		assertTrue(str.contains("children=1"));
		assertTrue(str.contains("waits=2"));
	}
}
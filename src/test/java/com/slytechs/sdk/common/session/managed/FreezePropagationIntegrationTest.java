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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.slytechs.sdk.common.session.message.MessageRecord;
import com.slytechs.sdk.common.session.message.RenderMode;

/**
 * Integration tests for freeze propagation and snapshot behavior.
 */
class FreezePropagationIntegrationTest {

	@Test
	void freezePreservesSnapshotValues() {
		AtomicInteger counter = new AtomicInteger(100);

		ManagedStateMachine state = new ManagedStateMachine("test", () -> {});
		state.register("counter-watch",
				MessageRecord.of("counter value: {}",
						(java.util.function.Supplier<Integer>) counter::get));

		// Value changes
		counter.set(50);

		// Before freeze - shows transition
		String beforeFreeze = state.pendingWaits().get(0).render(RenderMode.TRANSITION);
		System.out.println("Before freeze: " + beforeFreeze);
		assertTrue(beforeFreeze.contains("(100→50)"));

		// Freeze
		state.freeze();

		// After freeze - value changes but render shows snapshot
		counter.set(999);

		String afterFreeze = state.pendingWaits().get(0).render(RenderMode.CURRENT);
		System.out.println("After freeze: " + afterFreeze);
		assertTrue(afterFreeze.contains("100")); // Snapshot
		assertTrue(afterFreeze.contains("frozen"));
		assertFalse(afterFreeze.contains("999")); // Not the new value
	}

	@Test
	void freezeProtectsAgainstExceptions() {
		AtomicInteger counter = new AtomicInteger(42);

		ManagedStateMachine state = new ManagedStateMachine("test", () -> {});
		state.register("risky-supplier",
				MessageRecord.of("value: {}",
						(java.util.function.Supplier<Integer>) () -> {
							if (state.isFrozen()) {
								throw new RuntimeException("Session terminated!");
							}
							return counter.get();
						}));

		// Before freeze - works
		String before = state.pendingWaits().get(0).render(RenderMode.CURRENT);
		System.out.println("Before freeze (works): " + before);
		assertEquals("value: 42", before);

		// Freeze
		state.freeze();

		// After freeze - supplier throws, but we get snapshot
		String after = state.pendingWaits().get(0).render(RenderMode.CURRENT);
		System.out.println("After freeze (fallback to snapshot): " + after);
		assertTrue(after.contains("42")); // Snapshot value
	}

	@Test
	void multipleWaitsFreezeIndependently() {
		AtomicInteger counter1 = new AtomicInteger(10);
		AtomicInteger counter2 = new AtomicInteger(20);

		ManagedStateMachine state = new ManagedStateMachine("test", () -> {});

		state.register("wait1",
				MessageRecord.of("c1={}",
						(java.util.function.Supplier<Integer>) counter1::get));
		state.register("wait2",
				MessageRecord.of("c2={}",
						(java.util.function.Supplier<Integer>) counter2::get));

		// Change values
		counter1.set(100);
		counter2.set(200);

		System.out.println("Before freeze:");
		for (WaitInfo wait : state.pendingWaits()) {
			System.out.println("  " + wait.name() + ": " + wait.render(RenderMode.TRANSITION));
		}

		// Freeze all
		state.freeze();

		// All should be frozen
		for (WaitInfo wait : state.pendingWaits()) {
			assertTrue(wait.isFrozen());
		}

		// Change values again
		counter1.set(999);
		counter2.set(888);

		System.out.println("After freeze:");
		for (WaitInfo wait : state.pendingWaits()) {
			String rendered = wait.render(RenderMode.CURRENT);
			System.out.println("  " + wait.name() + ": " + rendered);
			assertTrue(rendered.contains("frozen"));
		}
	}

	@Test
	void shutdownNowFreezesState() {
		AtomicInteger counter = new AtomicInteger(50);

		ManagedStateMachine state = new ManagedStateMachine("test", () -> {});
		state.enable();
		state.register("watch",
				MessageRecord.of("value={}",
						(java.util.function.Supplier<Integer>) counter::get));

		assertFalse(state.isFrozen());

		// shutdownNow should freeze
		state.shutdownNow();

		assertTrue(state.isFrozen());
		assertTrue(state.isTerminated());

		// Verify waits are frozen too
		assertTrue(state.pendingWaits().get(0).isFrozen());
	}

	@Test
	void frozenStateShowsCorrectTransitions() {
		AtomicInteger packets = new AtomicInteger(1000);
		AtomicInteger queue = new AtomicInteger(50);

		ManagedStateMachine state = new ManagedStateMachine("channel", () -> {});
		state.enable();

		state.register("processed",
				MessageRecord.of("processed {} packets",
						(java.util.function.Supplier<Integer>) packets::get));
		state.register("queue",
				MessageRecord.of("queue size {}",
						(java.util.function.Supplier<Integer>) queue::get));

		// Simulate activity
		packets.set(5000);
		queue.set(10);

		System.out.println("=== Before Freeze ===");
		for (WaitInfo wait : state.pendingWaits()) {
			System.out.println(wait.renderFull(RenderMode.TRANSITION));
		}

		// Freeze at this point
		state.freeze();

		// More changes happen (but should be ignored)
		packets.set(99999);
		queue.set(0);

		System.out.println("\n=== After Freeze (values changed but frozen) ===");
		for (WaitInfo wait : state.pendingWaits()) {
			String rendered = wait.renderFull(RenderMode.TRANSITION);
			System.out.println(rendered);
			// Should show frozen snapshot, not current values
			assertFalse(rendered.contains("99999"));
			assertFalse(rendered.contains("queue size 0"));
		}
	}
}
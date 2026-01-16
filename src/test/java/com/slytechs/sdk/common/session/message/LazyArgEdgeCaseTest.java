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
package com.slytechs.sdk.common.session.message;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.slytechs.sdk.common.session.text.LazyArg;
import com.slytechs.sdk.common.session.text.LazyArg.Mode;

/**
 * Edge case tests for {@link LazyArg}.
 */
class LazyArgEdgeCaseTest {

	@Test
	void supplierThrowsException_returnSnapshot() {
		AtomicBoolean shouldThrow = new AtomicBoolean(false);
		AtomicInteger value = new AtomicInteger(42);

		LazyArg<Integer> arg = LazyArg.of(() -> {
			if (shouldThrow.get()) {
				throw new RuntimeException("Simulated failure");
			}
			return value.get();
		});

		assertEquals(42, arg.current());

		shouldThrow.set(true);
		assertEquals(42, arg.current()); // Falls back to snapshot
	}

	@Test
	void supplierThrowsError_returnSnapshot() {
		AtomicBoolean shouldThrow = new AtomicBoolean(false);

		LazyArg<String> arg = LazyArg.of(() -> {
			if (shouldThrow.get()) {
				throw new OutOfMemoryError("Simulated OOM");
			}
			return "original";
		});

		assertEquals("original", arg.current());

		shouldThrow.set(true);
		assertEquals("original", arg.current()); // Falls back to snapshot
	}

	@Test
	void supplierReturnsNull_snapshotIsNull() {
		LazyArg<String> arg = LazyArg.of(() -> null);

		assertNull(arg.snapshot());
		assertNull(arg.current());
		assertEquals("null", arg.render(Mode.CURRENT));
	}

	@Test
	void snapshotCaptureFailure_snapshotIsNull() {
		LazyArg<String> arg = LazyArg.of(() -> {
			throw new RuntimeException("Always fails");
		});

		assertNull(arg.snapshot()); // Failed capture results in null
		assertNull(arg.current());  // Still fails, returns null snapshot
	}

	@Test
	void transitionFromNullToValue() {
		AtomicReference<String> ref = new AtomicReference<>(null);
		LazyArg<String> arg = LazyArg.of(ref::get);

		ref.set("now has value");

		assertEquals("(null→now has value)", arg.render(Mode.TRANSITION));
	}

	@Test
	void transitionFromValueToNull() {
		AtomicReference<String> ref = new AtomicReference<>("had value");
		LazyArg<String> arg = LazyArg.of(ref::get);

		ref.set(null);

		assertEquals("(had value→null)", arg.render(Mode.TRANSITION));
	}

	@Test
	void frozenAfterSupplierStartsFailing() {
		AtomicBoolean shouldThrow = new AtomicBoolean(false);
		AtomicBoolean frozen = new AtomicBoolean(false);
		AtomicInteger value = new AtomicInteger(100);

		LazyArg<Integer> arg = LazyArg.of(() -> {
			if (shouldThrow.get()) {
				throw new RuntimeException("Dead");
			}
			return value.get();
		}, frozen);

		value.set(200);
		assertEquals("(100→200)", arg.render(Mode.TRANSITION));

		// Now supplier fails
		shouldThrow.set(true);
		// Should still show snapshot fallback
		assertTrue(arg.render(Mode.CURRENT).contains("100"));

		// Freeze it
		frozen.set(true);
		String rendered = arg.render(Mode.CURRENT);
		assertTrue(rendered.contains("100"));
		assertTrue(rendered.contains("frozen"));
	}

	@Test
	void hasChanged_withNullValues() {
		AtomicReference<String> ref = new AtomicReference<>(null);
		LazyArg<String> arg = LazyArg.of(ref::get);

		assertFalse(arg.hasChanged()); // null == null

		ref.set("value");
		assertTrue(arg.hasChanged()); // null != "value"

		ref.set(null);
		assertFalse(arg.hasChanged()); // null == null again
	}

	@Test
	void concurrentFreezeAndEvaluate() throws InterruptedException {
		AtomicBoolean frozen = new AtomicBoolean(false);
		AtomicInteger counter = new AtomicInteger(0);

		LazyArg<Integer> arg = LazyArg.of(counter::incrementAndGet, frozen);

		// Snapshot captured at creation (value = 1)
		assertEquals(1, arg.snapshot());

		// Multiple threads reading
		Thread[] threads = new Thread[10];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread(() -> {
				for (int j = 0; j < 100; j++) {
					arg.current(); // Should not throw
					arg.render(Mode.TRANSITION);
				}
			});
		}

		for (Thread t : threads) t.start();

		// Freeze midway
		Thread.sleep(5);
		frozen.set(true);

		for (Thread t : threads) t.join();

		// Should complete without exception
		assertTrue(arg.isFrozen());
	}

	@Test
	void renderEmptyString() {
		LazyArg<String> arg = LazyArg.of(() -> "");

		assertEquals("", arg.snapshot());
		assertEquals("", arg.current());
		assertEquals("", arg.render(Mode.CURRENT));
	}

	@Test
	void renderSpecialCharacters() {
		LazyArg<String> arg = LazyArg.of(() -> "line1\nline2\ttab");

		String rendered = arg.render(Mode.CURRENT);
		assertTrue(rendered.contains("\n"));
		assertTrue(rendered.contains("\t"));
	}

	@Test
	void renderVeryLongString() {
		String longString = "x".repeat(10000);
		LazyArg<String> arg = LazyArg.of(() -> longString);

		assertEquals(10000, arg.render(Mode.CURRENT).length());
	}

	@Test
	void objectToStringUsedForRender() {
		record CustomObject(int id, String name) {
			@Override
			public String toString() {
				return "Custom[" + id + ":" + name + "]";
			}
		}

		LazyArg<CustomObject> arg = LazyArg.of(() -> new CustomObject(42, "test"));

		assertEquals("Custom[42:test]", arg.render(Mode.CURRENT));
	}

	@Test
	void transitionWithSameObjectDifferentState() {
		AtomicInteger mutableState = new AtomicInteger(10);

		// Supplier returns the same object reference but its state changes
		LazyArg<AtomicInteger> arg = LazyArg.of(() -> mutableState);

		// Snapshot captured the AtomicInteger with value 10
		mutableState.set(20);

		// Both snapshot and current point to same object, so equals returns true
		// This is expected behavior - LazyArg tracks object identity, not deep state
		assertFalse(arg.hasChanged()); // Same object reference
	}
}
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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LazyArg} basic functionality.
 */
class LazyArgTest {

	@Test
	void createWithSupplier() {
		LazyArg<String> arg = LazyArg.of(() -> "hello");
		assertNotNull(arg);
	}

	@Test
	void snapshotCapturedAtCreation() {
		AtomicInteger counter = new AtomicInteger(10);
		LazyArg<Integer> arg = LazyArg.of(counter::get);

		assertEquals(10, arg.snapshot());

		counter.set(20);
		assertEquals(10, arg.snapshot()); // Snapshot unchanged
	}

	@Test
	void currentReturnsLiveValue() {
		AtomicInteger counter = new AtomicInteger(10);
		LazyArg<Integer> arg = LazyArg.of(counter::get);

		assertEquals(10, arg.current());

		counter.set(20);
		assertEquals(20, arg.current()); // Current reflects change
	}

	@Test
	void hasChangedDetectsChange() {
		AtomicInteger counter = new AtomicInteger(10);
		LazyArg<Integer> arg = LazyArg.of(counter::get);

		assertFalse(arg.hasChanged());

		counter.set(20);
		assertTrue(arg.hasChanged());
	}

	@Test
	void hasChangedFalseWhenSameValue() {
		AtomicInteger counter = new AtomicInteger(10);
		LazyArg<Integer> arg = LazyArg.of(counter::get);

		counter.set(5);
		counter.set(10); // Back to original
		assertFalse(arg.hasChanged());
	}

	@Test
	void isFrozenInitiallyFalse() {
		LazyArg<String> arg = LazyArg.of(() -> "test");
		assertFalse(arg.isFrozen());
	}

	@Test
	void nullSupplierThrows() {
		assertThrows(NullPointerException.class, () -> LazyArg.of(null));
	}

	@Test
	void supplierReturningNullWorks() {
		LazyArg<String> arg = LazyArg.of(() -> null);
		assertNull(arg.snapshot());
		assertNull(arg.current());
	}
}
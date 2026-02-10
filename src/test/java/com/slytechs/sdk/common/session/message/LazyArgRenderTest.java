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

import org.junit.jupiter.api.Test;

import com.slytechs.sdk.common.text.LazyArg;
import com.slytechs.sdk.common.text.LazyArg.Mode;

/**
 * Unit tests for {@link LazyArg} rendering functionality.
 */
class LazyArgRenderTest {

	@Test
	void renderCurrentShowsCurrentValue() {
		AtomicInteger counter = new AtomicInteger(10);
		LazyArg<Integer> arg = LazyArg.of(counter::get);

		counter.set(20);
		assertEquals("20", arg.render(Mode.CURRENT));
	}

	@Test
	void renderSnapshotShowsInitialValue() {
		AtomicInteger counter = new AtomicInteger(10);
		LazyArg<Integer> arg = LazyArg.of(counter::get);

		counter.set(20);
		assertEquals("10", arg.render(Mode.SNAPSHOT));
	}

	@Test
	void renderTransitionShowsChangeArrow() {
		AtomicInteger counter = new AtomicInteger(10);
		LazyArg<Integer> arg = LazyArg.of(counter::get);

		counter.set(20);
		assertEquals("(10→20)", arg.render(Mode.TRANSITION));
	}

	@Test
	void renderTransitionNoArrowWhenUnchanged() {
		AtomicInteger counter = new AtomicInteger(10);
		LazyArg<Integer> arg = LazyArg.of(counter::get);

		assertEquals("10", arg.render(Mode.TRANSITION));
	}

	@Test
	void toStringUsesTransitionMode() {
		AtomicInteger counter = new AtomicInteger(10);
		LazyArg<Integer> arg = LazyArg.of(counter::get);

		counter.set(20);
		assertEquals("(10→20)", arg.toString());
	}

	@Test
	void renderFrozenShowsFrozenIndicator() {
		AtomicBoolean frozen = new AtomicBoolean(false);
		AtomicInteger counter = new AtomicInteger(10);
		LazyArg<Integer> arg = LazyArg.of(counter::get, frozen);

		counter.set(20);
		frozen.set(true);

		String rendered = arg.render(Mode.CURRENT);
		assertTrue(rendered.contains("frozen"));
		assertTrue(rendered.contains("10")); // Shows snapshot, not current
	}

	@Test
	void renderSnapshotNotAffectedByFrozen() {
		AtomicBoolean frozen = new AtomicBoolean(true);
		AtomicInteger counter = new AtomicInteger(10);
		LazyArg<Integer> arg = LazyArg.of(counter::get, frozen);

		assertEquals("10", arg.render(Mode.SNAPSHOT));
	}
}
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
 * Unit tests for {@link MessageRecord} rendering functionality.
 */
class MessageRecordRenderTest {

	@Test
	void renderNoPlaceholders() {
		MessageRecord record = MessageRecord.of("hello world");
		assertEquals("hello world", record.render());
	}

	@Test
	void renderSinglePlaceholder() {
		MessageRecord record = MessageRecord.of("hello {}", "world");
		assertEquals("hello world", record.render());
	}

	@Test
	void renderMultiplePlaceholders() {
		MessageRecord record = MessageRecord.of("{} {} {}", "a", "b", "c");
		assertEquals("a b c", record.render());
	}

	@Test
	void renderMixedTextAndPlaceholders() {
		MessageRecord record = MessageRecord.of("draining {} packets from {}", 15, "channel-0");
		assertEquals("draining 15 packets from channel-0", record.render());
	}

	@Test
	void renderMorePlaceholdersThanArgs() {
		MessageRecord record = MessageRecord.of("{} {} {}", "only", "two");
		assertEquals("only two {}", record.render());
	}

	@Test
	void renderMoreArgsThanPlaceholders() {
		MessageRecord record = MessageRecord.of("{}", "first", "second", "third");
		assertEquals("first", record.render());
	}

	@Test
	void renderWithLazySupplier() {
		AtomicInteger counter = new AtomicInteger(10);
		MessageRecord record = MessageRecord.of("count: {}", (java.util.function.Supplier<Integer>) counter::get);

		assertEquals("count: 10", record.render());

		counter.set(20);
		assertEquals("count: 20", record.render());
	}

	@Test
	void renderTransitionWithLazyArg() {
		AtomicInteger counter = new AtomicInteger(10);
		MessageRecord record = MessageRecord.of("count: {}", (java.util.function.Supplier<Integer>) counter::get);

		counter.set(20);
		assertEquals("count: (10→20)", record.render(RenderMode.TRANSITION));
	}

	@Test
	void renderDefaultUsesCurrent() {
		AtomicInteger counter = new AtomicInteger(10);
		MessageRecord record = MessageRecord.of("count: {}", (java.util.function.Supplier<Integer>) counter::get);

		counter.set(20);
		assertEquals("count: 20", record.render());
		assertEquals("count: 20", record.render(RenderMode.CURRENT));
	}

	@Test
	void toStringUsesTransition() {
		AtomicInteger counter = new AtomicInteger(10);
		MessageRecord record = MessageRecord.of("count: {}", (java.util.function.Supplier<Integer>) counter::get);

		counter.set(20);
		assertEquals("count: (10→20)", record.toString());
	}

	@Test
	void renderNullArg() {
		MessageRecord record = MessageRecord.of("value: {}", (Object) null);
		assertEquals("value: null", record.render());
	}
}
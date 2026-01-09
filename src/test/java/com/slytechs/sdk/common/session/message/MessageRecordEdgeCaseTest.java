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
 * Edge case tests for {@link MessageRecord}.
 */
class MessageRecordEdgeCaseTest {

	@Test
	void emptyTemplate() {
		MessageRecord record = MessageRecord.of("");
		assertEquals("", record.render());
	}

	@Test
	void templateWithOnlyPlaceholders() {
		MessageRecord record = MessageRecord.of("{}{}{}", "a", "b", "c");
		assertEquals("abc", record.render());
	}

	@Test
	void placeholderAtStart() {
		MessageRecord record = MessageRecord.of("{} is the value", 42);
		assertEquals("42 is the value", record.render());
	}

	@Test
	void placeholderAtEnd() {
		MessageRecord record = MessageRecord.of("value is {}", 42);
		assertEquals("value is 42", record.render());
	}

	@Test
	void adjacentPlaceholders() {
		MessageRecord record = MessageRecord.of("{}{}", "hello", "world");
		assertEquals("helloworld", record.render());
	}

	@Test
	void noPlaceholdersWithArgs() {
		MessageRecord record = MessageRecord.of("no placeholders", "ignored", "args");
		assertEquals("no placeholders", record.render());
	}

	@Test
	void manyPlaceholders() {
		MessageRecord record = MessageRecord.of("{} {} {} {} {} {} {} {} {} {}",
				0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
		assertEquals("0 1 2 3 4 5 6 7 8 9", record.render());
	}

	@Test
	void placeholderLooksLikeButIsnt() {
		// Single brace should not be treated as placeholder
		MessageRecord record = MessageRecord.of("value { is } here", "unused");
		assertEquals("value { is } here", record.render());
	}

	@Test
	void escapedBraces() {
	    // {{}} is not special - the inner {} gets replaced
	    MessageRecord record = MessageRecord.of("{{}} and {}", "value");
	    assertEquals("{value} and {}", record.render());
	}

	@Test
	void nullArgument() {
		MessageRecord record = MessageRecord.of("value: {}", (Object) null);
		assertEquals("value: null", record.render());
	}

	@Test
	void supplierReturningNull() {
		MessageRecord record = MessageRecord.of("value: {}",
				(java.util.function.Supplier<String>) () -> null);
		assertEquals("value: null", record.render());
	}

	@Test
	void mixedConcreteAndLazyArgs() {
		AtomicInteger counter = new AtomicInteger(10);

		MessageRecord record = MessageRecord.of("static={}, lazy={}, static2={}",
				"fixed",
				(java.util.function.Supplier<Integer>) counter::get,
				100);

		assertEquals("static=fixed, lazy=10, static2=100", record.render());

		counter.set(20);
		assertEquals("static=fixed, lazy=20, static2=100", record.render());
	}

	@Test
	void freezeAffectsOnlyLazyArgs() {
		AtomicInteger counter = new AtomicInteger(10);

		MessageRecord record = MessageRecord.of("static={}, lazy={}",
				"fixed",
				(java.util.function.Supplier<Integer>) counter::get);

		counter.set(20);
		record.freeze();
		counter.set(30);

		String rendered = record.render();
		assertTrue(rendered.contains("static=fixed")); // Static unchanged
		assertTrue(rendered.contains("10")); // Lazy shows snapshot
		assertTrue(rendered.contains("frozen"));
	}

	@Test
	void supplierThrowsException() {
		AtomicInteger callCount = new AtomicInteger(0);

		MessageRecord record = MessageRecord.of("value: {}",
				(java.util.function.Supplier<String>) () -> {
					if (callCount.incrementAndGet() > 1) {
						throw new RuntimeException("Failed");
					}
					return "initial";
				});

		// First render captures snapshot
		assertEquals("value: initial", record.render());

		// Second render - supplier throws, falls back to snapshot
		String rendered = record.render();
		assertEquals("value: initial", rendered);
	}

	@Test
	void veryLongTemplate() {
		String template = "start " + "x".repeat(10000) + " {} end";
		MessageRecord record = MessageRecord.of(template, "middle");

		String rendered = record.render();
		assertTrue(rendered.startsWith("start "));
		assertTrue(rendered.contains("middle"));
		assertTrue(rendered.endsWith(" end"));
	}

	@Test
	void unicodeInTemplate() {
		MessageRecord record = MessageRecord.of("日本語: {}, emoji: {}", "テスト", "🎉");
		assertEquals("日本語: テスト, emoji: 🎉", record.render());
	}

	@Test
	void newlinesInTemplate() {
		MessageRecord record = MessageRecord.of("line1: {}\nline2: {}", "a", "b");
		assertEquals("line1: a\nline2: b", record.render());
	}

	@Test
	void templateMethod_createsLazyArgBoundToRecord() {
		AtomicInteger counter = new AtomicInteger(10);
		MessageRecord record = MessageRecord.of("test");

		LazyArg<Integer> arg = record.lazy(counter::get);

		// Freeze the record
		record.freeze();

		// The LazyArg should also be frozen
		assertTrue(arg.isFrozen());
	}

	@Test
	void concurrentRenderAndFreeze() throws InterruptedException {
		AtomicInteger counter = new AtomicInteger(0);

		MessageRecord record = MessageRecord.of("count: {}",
				(java.util.function.Supplier<Integer>) counter::incrementAndGet);

		Thread[] threads = new Thread[10];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread(() -> {
				for (int j = 0; j < 100; j++) {
					record.render();
					record.render(RenderMode.TRANSITION);
				}
			});
		}

		for (Thread t : threads) t.start();

		Thread.sleep(5);
		record.freeze();

		for (Thread t : threads) t.join();

		// Should complete without exception
		assertTrue(record.isFrozen());
	}

	@Test
	void transitionModeWithUnchangedValue() {
		AtomicInteger counter = new AtomicInteger(42);

		MessageRecord record = MessageRecord.of("value: {}",
				(java.util.function.Supplier<Integer>) counter::get);

		// Value hasn't changed
		String rendered = record.render(RenderMode.TRANSITION);
		assertEquals("value: 42", rendered); // No arrow when unchanged
	}

	@Test
	void transitionModeWithChangedValue() {
		AtomicInteger counter = new AtomicInteger(10);

		MessageRecord record = MessageRecord.of("value: {}",
				(java.util.function.Supplier<Integer>) counter::get);

		counter.set(20);

		String rendered = record.render(RenderMode.TRANSITION);
		assertEquals("value: (10→20)", rendered);
	}

	@Test
	void snapshotModeAlwaysShowsInitial() {
		AtomicInteger counter = new AtomicInteger(10);

		MessageRecord record = MessageRecord.of("value: {}",
				(java.util.function.Supplier<Integer>) counter::get);

		counter.set(9999);

		String rendered = record.render(RenderMode.SNAPSHOT);
		assertEquals("value: 10", rendered);
	}
}
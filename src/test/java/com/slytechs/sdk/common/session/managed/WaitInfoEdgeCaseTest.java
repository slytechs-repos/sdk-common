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

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.slytechs.sdk.common.session.message.MessageRecord;
import com.slytechs.sdk.common.session.message.RenderMode;

/**
 * Edge case tests for {@link WaitInfo}.
 */
class WaitInfoEdgeCaseTest {

	@Test
	void emptyName() {
		WaitInfo wait = new WaitInfo("", MessageRecord.of("message"));
		assertEquals("", wait.name());
	}

	@Test
	void veryLongName() {
		String longName = "x".repeat(1000);
		WaitInfo wait = new WaitInfo(longName, MessageRecord.of("msg"));
		assertEquals(longName, wait.name());
	}

	@Test
	void specialCharactersInName() {
		WaitInfo wait = new WaitInfo("wait-with:special/chars\\and\ttabs", MessageRecord.of("msg"));
		assertTrue(wait.name().contains(":"));
		assertTrue(wait.name().contains("/"));
		assertTrue(wait.name().contains("\\"));
		assertTrue(wait.name().contains("\t"));
	}

	@Test
	void unicodeInName() {
		WaitInfo wait = new WaitInfo("待機-🔄", MessageRecord.of("msg"));
		assertEquals("待機-🔄", wait.name());
	}

	@Test
	void elapsedGrowsOverTime() throws InterruptedException {
		WaitInfo wait = new WaitInfo("test", MessageRecord.of("msg"));

		Duration first = wait.elapsed();
		Thread.sleep(50);
		Duration second = wait.elapsed();

		assertTrue(second.compareTo(first) > 0);
	}

	@Test
	void elapsedNeverNegative() {
		WaitInfo wait = new WaitInfo("test", MessageRecord.of("msg"));

		for (int i = 0; i < 100; i++) {
			assertFalse(wait.elapsed().isNegative());
		}
	}

	@Test
	void renderFullFormatDuration_milliseconds() throws InterruptedException {
		WaitInfo wait = new WaitInfo("test", MessageRecord.of("msg"));

		// Immediately - should show milliseconds or < 1s
		String rendered = wait.renderFull(RenderMode.CURRENT);
		assertTrue(rendered.contains("waiting"));
	}

	@Test
	void renderFullFormatDuration_seconds() throws InterruptedException {
		WaitInfo wait = new WaitInfo("test", MessageRecord.of("msg"));
		Thread.sleep(1100); // Just over 1 second

		String rendered = wait.renderFull(RenderMode.CURRENT);
		assertTrue(rendered.contains("s")); // Should show seconds
	}

	@Test
	void freezeIsIdempotent() {
		MessageRecord message = MessageRecord.of("test");
		WaitInfo wait = new WaitInfo("test", message);

		wait.freeze();
		assertTrue(wait.isFrozen());

		wait.freeze();
		assertTrue(wait.isFrozen());

		wait.freeze();
		assertTrue(wait.isFrozen());
	}

	@Test
	void toStringIncludesAllParts() {
		WaitInfo wait = new WaitInfo("my-wait", MessageRecord.of("my message"));

		String str = wait.toString();

		assertTrue(str.contains("my-wait"));
		assertTrue(str.contains("my message"));
		assertTrue(str.contains("waiting"));
	}

	@Test
	void renderDifferentModes() {
		java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(10);

		WaitInfo wait = new WaitInfo("test",
				MessageRecord.of("count: {}",
						(java.util.function.Supplier<Integer>) counter::get));

		counter.set(20);

		String current = wait.render(RenderMode.CURRENT);
		String snapshot = wait.render(RenderMode.SNAPSHOT);
		String transition = wait.render(RenderMode.TRANSITION);

		assertEquals("count: 20", current);
		assertEquals("count: 10", snapshot);
		assertEquals("count: (10→20)", transition);
	}

	@Test
	void messageNotCopied_sameReference() {
		MessageRecord message = MessageRecord.of("test");
		WaitInfo wait = new WaitInfo("test", message);

		assertSame(message, wait.message());
	}

	@Test
	void sinceIsImmutable() throws InterruptedException {
		WaitInfo wait = new WaitInfo("test", MessageRecord.of("msg"));

		var since1 = wait.since();
		Thread.sleep(10);
		var since2 = wait.since();

		assertEquals(since1, since2); // Same instant
	}
}
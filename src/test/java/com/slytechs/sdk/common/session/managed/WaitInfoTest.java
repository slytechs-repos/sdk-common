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
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.slytechs.sdk.common.session.message.MessageRecord;
import com.slytechs.sdk.common.session.message.RenderMode;

/**
 * Unit tests for {@link WaitInfo} basic functionality.
 */
class WaitInfoTest {

	@Test
	void createWithNameAndMessage() {
		MessageRecord message = MessageRecord.of("waiting for something");
		WaitInfo wait = new WaitInfo("test-wait", message);

		assertNotNull(wait);
		assertEquals("test-wait", wait.name());
		assertSame(message, wait.message());
	}

	@Test
	void nullNameThrows() {
		MessageRecord message = MessageRecord.of("test");
		assertThrows(NullPointerException.class, () -> new WaitInfo(null, message));
	}

	@Test
	void nullMessageThrows() {
		assertThrows(NullPointerException.class, () -> new WaitInfo("test", null));
	}

	@Test
	void sinceSetAtCreation() {
		Instant before = Instant.now();
		WaitInfo wait = new WaitInfo("test", MessageRecord.of("msg"));
		Instant after = Instant.now();

		assertNotNull(wait.since());
		assertFalse(wait.since().isBefore(before));
		assertFalse(wait.since().isAfter(after));
	}

	@Test
	void elapsedReturnsPositiveDuration() throws InterruptedException {
		WaitInfo wait = new WaitInfo("test", MessageRecord.of("msg"));
		Thread.sleep(10);

		Duration elapsed = wait.elapsed();
		assertNotNull(elapsed);
		assertTrue(elapsed.toMillis() >= 10);
	}

	@Test
	void isFrozenDelegatesToMessage() {
		MessageRecord message = MessageRecord.of("test");
		WaitInfo wait = new WaitInfo("test", message);

		assertFalse(wait.isFrozen());

		message.freeze();
		assertTrue(wait.isFrozen());
	}

	@Test
	void freezeDelegatesToMessage() {
		MessageRecord message = MessageRecord.of("test");
		WaitInfo wait = new WaitInfo("test", message);

		wait.freeze();

		assertTrue(message.isFrozen());
		assertTrue(wait.isFrozen());
	}

	@Test
	void renderDelegatesToMessage() {
		MessageRecord message = MessageRecord.of("waiting for {}", "completion");
		WaitInfo wait = new WaitInfo("test", message);

		assertEquals("waiting for completion", wait.render(RenderMode.CURRENT));
	}

	@Test
	void renderFullIncludesNameAndElapsed() {
		MessageRecord message = MessageRecord.of("waiting");
		WaitInfo wait = new WaitInfo("my-wait", message);

		String full = wait.renderFull(RenderMode.CURRENT);

		assertTrue(full.contains("my-wait"));
		assertTrue(full.contains("waiting"));
		assertTrue(full.contains("waiting"));
	}

	@Test
	void toStringUsesRenderFull() {
		MessageRecord message = MessageRecord.of("waiting");
		WaitInfo wait = new WaitInfo("test", message);

		String str = wait.toString();
		assertTrue(str.contains("test"));
		assertTrue(str.contains("waiting"));
	}
}
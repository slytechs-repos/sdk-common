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

import static com.slytechs.sdk.common.session.message.MessageTemplate.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MessageTemplate} factory methods.
 */
class MessageTemplateTest {

	@Test
	void ofCreatesMessageRecord() {
		MessageRecord record = of("test message");
		assertNotNull(record);
		assertEquals("test message", record.template());
	}

	@Test
	void ofWithArgsCreatesMessageRecord() {
		MessageRecord record = of("hello {}", "world");
		assertEquals("hello world", record.render());
	}

	@Test
	void lazyWrapsSupplier() {
		AtomicInteger counter = new AtomicInteger(10);
		MessageRecord record = of("count: {}", lazy(counter::get));

		counter.set(20);
		assertEquals("count: 20", record.render());
	}

	@Test
	void lazyArgCreatesStandaloneLazyArg() {
		AtomicInteger counter = new AtomicInteger(10);
		LazyArg<Integer> arg = lazyArg(counter::get);

		assertNotNull(arg);
		assertEquals(10, arg.snapshot());

		counter.set(20);
		assertEquals(20, arg.current());
	}

	@Test
	void messageCreatesNoArgRecord() {
		MessageRecord record = message("static message");
		assertEquals("static message", record.render());
		assertEquals(0, record.argCount());
	}

	@Test
	void combinedUsage() {
		AtomicInteger packets = new AtomicInteger(15);
		String channel = "hello-channel";

		MessageRecord record = of("draining {} packets from {}",
				lazy(packets::get),
				channel);

		assertEquals("draining 15 packets from hello-channel", record.render());

		packets.set(3);
		assertEquals("draining (15→3) packets from hello-channel", record.render(RenderMode.TRANSITION));
	}
}
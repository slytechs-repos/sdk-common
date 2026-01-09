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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.slytechs.sdk.common.session.message.MessageRecord;
import com.slytechs.sdk.common.util.Registration;

/**
 * Unit tests for {@link ManagedStateMachine} wait registration.
 */
class ManagedStateMachineWaitTest {

	private ManagedStateMachine state;

	@BeforeEach
	void setUp() {
		state = new ManagedStateMachine("test-session", () -> {});
		state.enable();
	}

	@Test
	void registerAddsWait() {
		MessageRecord message = MessageRecord.of("waiting for something");
		state.register("test-wait", message);

		assertEquals(1, state.pendingWaits().size());
		assertTrue(state.hasPendingWaits());
	}

	@Test
	void registerReturnsRegistration() {
		MessageRecord message = MessageRecord.of("waiting");
		Registration reg = state.register("test", message);

		assertNotNull(reg);
	}

	@Test
	void unregisterRemovesWait() {
		MessageRecord message = MessageRecord.of("waiting");
		Registration reg = state.register("test", message);

		reg.unregister();

		assertTrue(state.pendingWaits().isEmpty());
		assertFalse(state.hasPendingWaits());
	}

	@Test
	void multipleRegistrations() {
		state.register("wait1", MessageRecord.of("msg1"));
		state.register("wait2", MessageRecord.of("msg2"));
		state.register("wait3", MessageRecord.of("msg3"));

		assertEquals(3, state.pendingWaits().size());
	}

	@Test
	void unregisterOnlyRemovesOne() {
		Registration reg1 = state.register("wait1", MessageRecord.of("msg1"));
		state.register("wait2", MessageRecord.of("msg2"));

		reg1.unregister();

		assertEquals(1, state.pendingWaits().size());
		assertEquals("wait2", state.pendingWaits().get(0).name());
	}

	@Test
	void registerNullNameThrows() {
		assertThrows(NullPointerException.class, 
				() -> state.register(null, MessageRecord.of("msg")));
	}

	@Test
	void registerNullMessageThrows() {
		assertThrows(NullPointerException.class, 
				() -> state.register("test", null));
	}

	@Test
	void pendingWaitsListIsUnmodifiable() {
		state.register("test", MessageRecord.of("msg"));

		assertThrows(UnsupportedOperationException.class,
				() -> state.pendingWaits().clear());
	}

	@Test
	void freezeFreezesPendingWaits() {
		state.register("wait1", MessageRecord.of("msg1"));
		state.register("wait2", MessageRecord.of("msg2"));

		state.freeze();

		for (WaitInfo wait : state.pendingWaits()) {
			assertTrue(wait.isFrozen());
		}
	}
}
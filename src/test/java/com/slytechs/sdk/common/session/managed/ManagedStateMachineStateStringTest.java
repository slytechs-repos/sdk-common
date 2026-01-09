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

/**
 * Unit tests for {@link ManagedStateMachine} state string rendering.
 */
class ManagedStateMachineStateStringTest {

	private ManagedStateMachine state;

	@BeforeEach
	void setUp() {
		state = new ManagedStateMachine("test-session", () -> {});
	}

	@Test
	void initialStateString() {
		String stateStr = state.stateString();
		assertEquals("CREATED", stateStr);
	}

	@Test
	void runningStateString() {
		state.enable();
		state.captureState(); // Capture so no transition shown

		assertEquals("RUNNING", state.stateString());
	}

	@Test
	void shutdownEmptySessionTerminatesImmediately() {
		state.enable();
		state.captureState();
		state.shutdown();
		state.captureState();

		assertEquals("TERMINATED", state.stateString()); // Auto-terminates with no components
	}

	@Test
	void terminatedStateString() {
		state.enable();
		state.shutdownNow();
		state.captureState();

		assertEquals("TERMINATED", state.stateString());
	}

	@Test
	void transitionShowsArrow() {
		state.enable();
		// Don't capture - should show transition

		String stateStr = state.stateString();
		assertTrue(stateStr.contains("→"));
		assertTrue(stateStr.contains("CREATED"));
		assertTrue(stateStr.contains("RUNNING"));
	}

	@Test
	void toStringIncludesAllInfo() {
		state.enable();
		state.register("wait1", com.slytechs.sdk.common.session.message.MessageRecord.of("msg"));

		ManagedStateMachine child = new ManagedStateMachine("child", () -> {});
		state.addChild(child);

		String str = state.toString();

		assertTrue(str.contains("test-session"));
		assertTrue(str.contains("children=1"));
		assertTrue(str.contains("waits=1"));
	}
}
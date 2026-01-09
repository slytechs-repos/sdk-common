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
package com.slytechs.sdk.common.session;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StateMachine} reset and restart functionality.
 */
class StateMachineResetTest {

	private StateMachine state;

	@BeforeEach
	void setUp() {
		state = new StateMachine("test-session", () -> {});
	}

	@AfterEach
	void tearDown() {
		state.close();
	}

	@Test
	void resetAfterTermination() {
		state.enable();
		state.shutdownNow();

		assertTrue(state.isTerminated());

		state.reset();

		assertTrue(state.isRunning());
		assertFalse(state.isShutdown());
		assertFalse(state.isTerminated());
	}

	@Test
	void resetBeforeTerminationThrows() {
		state.enable();

		assertThrows(IllegalStateException.class, () -> state.reset());
	}

	@Test
	void resetWithActiveComponentsThrows() {
		state.enable();
		state.register();
		state.shutdown();

		// Not terminated because component still active
		assertFalse(state.isTerminated());

		assertThrows(IllegalStateException.class, () -> state.reset());
	}

	@Test
	void canShutdownAgainAfterReset() {
		state.enable();
		state.shutdownNow();
		state.reset();

		assertTrue(state.isRunning());

		state.shutdown();

		assertTrue(state.isShutdown());
		assertTrue(state.isTerminated()); // Auto-terminates with no components
	}

	@Test
	void canRegisterComponentsAfterReset() {
		state.enable();
		state.shutdownNow();
		state.reset();

		state.register();
		state.shutdown();

		assertFalse(state.isTerminated()); // Component still active

		state.deregister();
		assertTrue(state.isTerminated());
	}

	@Test
	void startAfterShutdownResetsState() {
		state.enable();
		state.shutdownNow();

		assertTrue(state.isTerminated());

		state.start();

		assertTrue(state.isRunning());
		assertFalse(state.isShutdown());
		assertFalse(state.isTerminated());
	}
}
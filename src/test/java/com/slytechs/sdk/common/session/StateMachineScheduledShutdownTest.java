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

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.slytechs.sdk.common.util.Registration;

/**
 * Tests for {@link StateMachine} scheduled shutdown functionality.
 */
class StateMachineScheduledShutdownTest {

	private AtomicBoolean actionExecuted;
	private StateMachine state;

	@BeforeEach
	void setUp() {
		actionExecuted = new AtomicBoolean(false);
		state = new StateMachine("test-session", () -> actionExecuted.set(true));
		state.enable();
	}

	@AfterEach
	void tearDown() {
		state.close();
	}

	@Test
	void shutdownAfterSetsScheduledFlag() {
		state.shutdownAfter(Duration.ofSeconds(10));

		assertTrue(state.isShutdownScheduled());
		assertTrue(state.isRunning()); // Still running until deadline
	}

	@Test
	@Timeout(5)
	void shutdownAfterExecutesAction() throws InterruptedException {
		state.shutdownAfter(Duration.ofMillis(100));

		Thread.sleep(200);

		assertTrue(actionExecuted.get());
		assertFalse(state.isShutdownScheduled()); // Cleared after execution
	}

	@Test
	void shutdownAfterReturnsRegistration() {
		Registration reg = state.shutdownAfter(Duration.ofSeconds(10));
		assertNotNull(reg);
	}

	@Test
	void cancelScheduledShutdown() {
		state.shutdownAfter(Duration.ofSeconds(10));
		assertTrue(state.isShutdownScheduled());

		state.cancelScheduledShutdown();

		assertFalse(state.isShutdownScheduled());
	}

	@Test
	@Timeout(5)
	void cancelledShutdownDoesNotExecute() throws InterruptedException {
		state.shutdownAfter(Duration.ofMillis(100));
		state.cancelScheduledShutdown();

		Thread.sleep(200);

		assertFalse(actionExecuted.get());
	}

	@Test
	void registrationCancelsShutdown() {
		Registration reg = state.shutdownAfter(Duration.ofSeconds(10));
		assertTrue(state.isShutdownScheduled());

		reg.unregister();

		assertFalse(state.isShutdownScheduled());
	}

	@Test
	void shutdownAfterNullDurationThrows() {
		assertThrows(NullPointerException.class, () -> state.shutdownAfter(null));
	}

	@Test
	void shutdownAfterNegativeDurationThrows() {
		assertThrows(IllegalArgumentException.class, 
				() -> state.shutdownAfter(Duration.ofMillis(-100)));
	}

	@Test
	void shutdownAfterZeroDurationThrows() {
		assertThrows(IllegalArgumentException.class, 
				() -> state.shutdownAfter(Duration.ZERO));
	}

	@Test
	void shutdownAfterWhenAlreadyShutdownThrows() {
		state.shutdown();

		assertThrows(IllegalStateException.class, 
				() -> state.shutdownAfter(Duration.ofSeconds(1)));
	}

	@Test
	void shutdownAtSetsScheduledFlag() {
		Instant future = Instant.now().plusSeconds(10);
		state.shutdownAt(future);

		assertTrue(state.isShutdownScheduled());
	}

	@Test
	void shutdownAtNullInstantThrows() {
		assertThrows(NullPointerException.class, () -> state.shutdownAt(null));
	}

	@Test
	void shutdownAtPastInstantThrows() {
		Instant past = Instant.now().minusSeconds(10);

		assertThrows(IllegalArgumentException.class, () -> state.shutdownAt(past));
	}

	@Test
	void newScheduledShutdownCancelsPrevious() {
		state.shutdownAfter(Duration.ofSeconds(10));
		assertTrue(state.isShutdownScheduled());

		// Schedule another
		state.shutdownAfter(Duration.ofSeconds(20));
		assertTrue(state.isShutdownScheduled());

		// Cancel should only cancel the latest
		state.cancelScheduledShutdown();
		assertFalse(state.isShutdownScheduled());
	}

	@Test
	void shutdownCancelsScheduledShutdown() {
		state.shutdownAfter(Duration.ofSeconds(10));
		assertTrue(state.isShutdownScheduled());

		state.shutdown();

		assertFalse(state.isShutdownScheduled());
		assertTrue(state.isShutdown());
	}

	@Test
	void shutdownNowCancelsScheduledShutdown() {
		state.shutdownAfter(Duration.ofSeconds(10));
		assertTrue(state.isShutdownScheduled());

		state.shutdownNow();

		assertFalse(state.isShutdownScheduled());
		assertTrue(state.isTerminated());
	}

	@Test
	void startCancelsScheduledShutdown() {
		state.shutdownAfter(Duration.ofSeconds(10));
		assertTrue(state.isShutdownScheduled());

		// Restart (resets state)
		state.shutdownNow();
		state.start();

		assertFalse(state.isShutdownScheduled());
		assertTrue(state.isRunning());
	}
}
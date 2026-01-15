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
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.slytechs.sdk.common.session.state.LifecycleStateMachine;

/**
 * Tests for {@link LifecycleSession} interface default methods.
 */
class SessionTest {

	private TestSession session;

	@BeforeEach
	void setUp() {
		session = new TestSession("test-session");
		session.state.start();
	}

	@AfterEach
	void tearDown() {
		session.state.shutdown();
	}

	@Test
	void isRunningDelegatesToState() {
		assertTrue(session.isRunning());

		session.shutdown();
		assertFalse(session.isRunning());
	}

	@Test
	void isShutdownDelegatesToState() {
		assertFalse(session.isShutdown());

		session.shutdown();
		assertTrue(session.isShutdown());
	}

	@Test
	void isShutdownScheduledDelegatesToState() {
		assertFalse(session.isShutdownScheduled());

		session.shutdownAfter(Duration.ofSeconds(10));
		assertTrue(session.isShutdownScheduled());
	}

	@Test
	void isTerminatedDelegatesToState() {
		assertFalse(session.isTerminated());

		session.shutdownNow();
		assertTrue(session.isTerminated());
	}

	@Test
	void shutdownAfterReturnsThis() {
		LifecycleSession result = session.shutdownAfter(Duration.ofSeconds(10));
		assertSame(session, result);
	}

	@Test
	void shutdownAtReturnsThis() {
		LifecycleSession result = session.shutdownAt(Instant.now().plusSeconds(10));
		assertSame(session, result);
	}

	@Test
	void cancelShutdownReturnsThis() {
		session.shutdownAfter(Duration.ofSeconds(10));
		session.cancelShutdown();
		assertFalse(session.isShutdownScheduled());
	}

	@Test
	@Timeout(5)
	void awaitCompletionDelegatesToState() throws InterruptedException {
		session.shutdownNow();
		session.awaitCompletion(); // Should return immediately
		assertTrue(session.isTerminated());
	}

	@Test
	@Timeout(5)
	void awaitCompletionWithTimeoutDelegatesToState() throws InterruptedException {
		session.shutdownNow();
		assertTrue(session.awaitCompletion(1, TimeUnit.SECONDS));
	}

	@Test
	@Timeout(5)
	void awaitCompletionTimeoutReturnsFalse() throws InterruptedException {
		session.state.register();
		session.shutdown();

		assertFalse(session.awaitCompletion(100, TimeUnit.MILLISECONDS));
	}

	/**
	 * Simple LifecycleSession implementation for testing.
	 */
	static class TestSession implements LifecycleSession {
		final LifecycleStateMachine state;

		TestSession(String name) {
			this.state = new LifecycleStateMachine(name, () -> {});
		}

		@Override
		public boolean shutdown() {
			return state.shutdown();
		}

		@Override
		public void shutdownNow() {
			state.shutdown();
		}

		@Override
		public LifecycleStateMachine state() {
			return state;
		}

		/**
		 * @see com.slytechs.sdk.common.session.Session#isActive()
		 */
		@Override
		public boolean isActive() {
			return state.isRunning();
		}
	}
}
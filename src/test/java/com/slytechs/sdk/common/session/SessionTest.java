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

import com.slytechs.sdk.common.session.state.SystemStateMachine;

/**
 * Tests for {@link SystemSession} interface default methods.
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
		if (!session.isTerminated())
			session.state.shutdown();
	}

	@Test
	void isRunningDelegatesToState() {
		assertTrue(session.state().isRunning());

		session.shutdown();
		assertFalse(session.state().isRunning());
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
	void isTerminatedDelegatesToState() throws InterruptedException {
		assertFalse(session.isTerminated());

		session.shutdownNow();
		assertTrue(session.isTerminated());
	}

	@Test
	void shutdownAfterReturnsThis() {
		var result = session.shutdownAfter(Duration.ofSeconds(10));
		assertSame(session, result);
	}

	@Test
	void shutdownAtReturnsThis() {
		var result = session.shutdownAt(Instant.now().plusSeconds(10));
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

		assertFalse(session.awaitCompletion(Duration.ofSeconds(1)));
	}

	/**
	 * Simple SystemSession implementation for testing.
	 */
	static class TestSession implements SystemSession {
		final SystemStateMachine state;

		TestSession(String name) {
			this.state = new SystemStateMachine(name, () -> {});
		}

		@Override
		public boolean awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException {
			return state.awaitTerminated(Duration.of(timeout, unit.toChronoUnit()));
		}

		@Override
		public boolean shutdown() {
			return state.shutdown();
		}

		@Override
		public SystemStateMachine state() {
			return state;
		}

		/**
		 * @see com.slytechs.sdk.common.session.Session#isActive()
		 */
		@Override
		public boolean isActive() {
			return state.isRunning();
		}

		/**
		 * @see com.slytechs.sdk.common.session.Shutdownable#isShutdown()
		 */
		@Override
		public boolean isShutdown() {
			return state.isShutdown();
		}

		/**
		 * @see com.slytechs.sdk.common.session.Shutdownable#isTerminated()
		 */
		@Override
		public boolean isTerminated() {
			return state.isTerminated();
		}

		/**
		 * @see com.slytechs.sdk.common.session.Schedulable#shutdownAfter(java.time.Duration)
		 */
		@Override
		public TestSession shutdownAfter(Duration duration) throws SessionSchedulingException,
				IllegalArgumentException {
			state.shutdownAfter(duration);

			return this;
		}

		/**
		 * @see com.slytechs.sdk.common.session.Awaitable#awaitCompletion()
		 */
		@Override
		public void awaitCompletion() throws InterruptedException, SessionAwaitException {
			state.awaitTerminated();
		}

		@Override
		public boolean awaitCompletion(Duration timeout) throws InterruptedException {
			return state.awaitTerminated(timeout);
		}

		/**
		 * @see com.slytechs.sdk.common.session.CloseableSession#close()
		 */
		public void close() throws SessionException, InterruptedException {
			shutdown();
		}

		@Override
		public boolean isShutdownScheduled() {
			return state.isShutdownScheduled();
		}

		@Override
		public TestSession cancelShutdown() {
			state.cancelScheduledShutdown();
			return this;
		}

		@Override
		public void shutdownNow() throws InterruptedException {
		    state.shutdown();
		    state.terminate();
		}
	}
}
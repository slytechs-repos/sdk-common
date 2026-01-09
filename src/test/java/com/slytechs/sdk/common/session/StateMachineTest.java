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
 * Basic tests for {@link StateMachine} lifecycle management.
 */
class StateMachineTest {

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
	void createWithName() {
		assertEquals("test-session", state.name());
	}

	@Test
	void initialStateNotRunning() {
		assertFalse(state.isRunning());
		assertFalse(state.isShutdown());
		assertFalse(state.isShutdownScheduled());
		assertFalse(state.isTerminated());
	}

	@Test
	void enableSetsRunning() {
		state.enable();

		assertTrue(state.isRunning());
		assertFalse(state.isShutdown());
		assertFalse(state.isTerminated());
	}

	@Test
	void startSetsRunning() {
		assertTrue(state.start());
		assertTrue(state.isRunning());
	}

	@Test
	void startReturnsFalseIfAlreadyRunning() {
		state.start();
		assertFalse(state.start());
	}

	@Test
	void enableReturnsThis() {
		assertSame(state, state.enable());
	}

	@Test
	void shutdownSetsFlags() {
		state.enable();
		assertTrue(state.shutdown());

		assertFalse(state.isRunning());
		assertTrue(state.isShutdown());
	}

	@Test
	void shutdownReturnsFalseIfAlreadyShutdown() {
		state.enable();
		state.shutdown();
		assertFalse(state.shutdown());
	}

	@Test
	void shutdownWithNoComponentsAutoTerminates() {
		state.enable();
		state.shutdown();

		assertTrue(state.isTerminated());
	}

	@Test
	void shutdownNowTerminatesImmediately() {
		state.enable();
		assertTrue(state.shutdownNow());

		assertFalse(state.isRunning());
		assertTrue(state.isShutdown());
		assertTrue(state.isTerminated());
	}

	@Test
	void shutdownNowReturnsFalseIfAlreadyTerminated() {
		state.enable();
		state.shutdownNow();
		assertFalse(state.shutdownNow());
	}

	@Test
	void registerIncrementsActiveComponents() {
		state.enable();
		state.register();
		state.shutdown();

		// Should not auto-terminate because there's an active component
		assertFalse(state.isTerminated());
	}

	@Test
	void deregisterDecrementsAndMayTerminate() {
		state.enable();
		state.register();
		state.shutdown();

		assertFalse(state.isTerminated());

		state.deregister();
		assertTrue(state.isTerminated());
	}

	@Test
	void multipleRegisterDeregister() {
		state.enable();
		state.register();
		state.register();
		state.register();

		state.shutdown();
		assertFalse(state.isTerminated());

		state.deregister();
		assertFalse(state.isTerminated());

		state.deregister();
		assertFalse(state.isTerminated());

		state.deregister();
		assertTrue(state.isTerminated());
	}

	@Test
	void toStringContainsName() {
		String str = state.toString();
		assertTrue(str.contains("test-session"));
	}

	@Test
	void toStringContainsStateFlags() {
		state.enable();
		String str = state.toString();

		assertTrue(str.contains("running=true"));
		assertTrue(str.contains("shutdown=false"));
		assertTrue(str.contains("terminated=false"));
	}
}
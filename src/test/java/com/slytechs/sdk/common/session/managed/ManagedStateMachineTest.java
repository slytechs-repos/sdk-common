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

import com.slytechs.sdk.common.session.StateMachine;

/**
 * Unit tests for {@link ManagedStateMachine} basic functionality.
 */
class ManagedStateMachineTest {

	private ManagedStateMachine state;

	@BeforeEach
	void setUp() {
		state = new ManagedStateMachine("test-session", () -> {});
	}

	@Test
	void createWithNameAndAction() {
		assertNotNull(state);
		assertEquals("test-session", state.name());
	}

	@Test
	void createWithStateMachineDelegate() {
		StateMachine delegate = new StateMachine("delegate", () -> {});
		ManagedStateMachine managed = new ManagedStateMachine(delegate);

		assertEquals("delegate", managed.name());
		assertSame(delegate, managed.delegate());
	}

	@Test
	void nullDelegateThrows() {
		assertThrows(NullPointerException.class, () -> new ManagedStateMachine(null));
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
	}

	@Test
	void shutdownSetsFlags() {
		state.enable();
		state.shutdown();

		assertFalse(state.isRunning());
		assertTrue(state.isShutdown());
	}

	@Test
	void shutdownNowTerminates() {
		state.enable();
		state.shutdownNow();

		assertFalse(state.isRunning());
		assertTrue(state.isShutdown());
		assertTrue(state.isTerminated());
	}

	@Test
	void isFrozenInitiallyFalse() {
		assertFalse(state.isFrozen());
	}

	@Test
	void freezeSetsFlag() {
		state.freeze();
		assertTrue(state.isFrozen());
	}

	@Test
	void parentInitiallyEmpty() {
		assertTrue(state.parent().isEmpty());
	}

	@Test
	void childrenInitiallyEmpty() {
		assertTrue(state.children().isEmpty());
	}

	@Test
	void pendingWaitsInitiallyEmpty() {
		assertTrue(state.pendingWaits().isEmpty());
		assertFalse(state.hasPendingWaits());
	}
}
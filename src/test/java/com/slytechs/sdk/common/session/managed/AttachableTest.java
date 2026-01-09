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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.slytechs.sdk.common.util.Registration;

/**
 * Unit tests for {@link Attachable} interface contract.
 */
class AttachableTest {

	/**
	 * Simple test implementation of Attachable.
	 */
	static class TestAttachable implements Attachable<ManagedSession> {
		private final List<ManagedSession> children = new ArrayList<>();

		@Override
		public Registration attachChild(ManagedSession child) {
			children.add(child);
			return () -> children.remove(child);
		}

		@Override
		public void detachChild(ManagedSession child) {
			children.remove(child);
		}

		@Override
		public boolean hasChild(ManagedSession child) {
			return children.contains(child);
		}

		@Override
		public int childCount() {
			return children.size();
		}
	}

	@Test
	void attachChildIncrementsCount() {
		TestAttachable attachable = new TestAttachable();
		ManagedSession mockChild = createMockSession("child");

		attachable.attachChild(mockChild);

		assertEquals(1, attachable.childCount());
		assertTrue(attachable.hasChild(mockChild));
	}

	@Test
	void detachChildDecrementsCount() {
		TestAttachable attachable = new TestAttachable();
		ManagedSession mockChild = createMockSession("child");

		attachable.attachChild(mockChild);
		attachable.detachChild(mockChild);

		assertEquals(0, attachable.childCount());
		assertFalse(attachable.hasChild(mockChild));
	}

	@Test
	void registrationUnregistersChild() {
		TestAttachable attachable = new TestAttachable();
		ManagedSession mockChild = createMockSession("child");

		Registration reg = attachable.attachChild(mockChild);
		reg.unregister();

		assertEquals(0, attachable.childCount());
		assertFalse(attachable.hasChild(mockChild));
	}

	@Test
	void multipleChildren() {
		TestAttachable attachable = new TestAttachable();
		ManagedSession child1 = createMockSession("child1");
		ManagedSession child2 = createMockSession("child2");
		ManagedSession child3 = createMockSession("child3");

		attachable.attachChild(child1);
		attachable.attachChild(child2);
		attachable.attachChild(child3);

		assertEquals(3, attachable.childCount());
		assertTrue(attachable.hasChild(child1));
		assertTrue(attachable.hasChild(child2));
		assertTrue(attachable.hasChild(child3));
	}

	@Test
	void hasChildReturnsFalseForNonChild() {
		TestAttachable attachable = new TestAttachable();
		ManagedSession child = createMockSession("child");
		ManagedSession notChild = createMockSession("not-child");

		attachable.attachChild(child);

		assertTrue(attachable.hasChild(child));
		assertFalse(attachable.hasChild(notChild));
	}

	/**
	 * Creates a minimal mock ManagedSession for testing.
	 */
	private ManagedSession createMockSession(String name) {
		// Return a minimal implementation that just provides managedState
		return new ManagedSession() {
			private final ManagedStateMachine state = new ManagedStateMachine(name, () -> {});

			@Override
			public ManagedState managedState() {
				return state;
			}

			@Override
			public java.util.Optional<ManagedSession> parentSession() {
				return java.util.Optional.empty();
			}

			@Override
			public List<ManagedSession> childSessions() {
				return List.of();
			}

			@Override
			public ManagedSession onShutdown(Runnable action) {
				return this;
			}

			@Override
			public ManagedSession onTermination(Runnable action) {
				return this;
			}

			@Override
			public Registration onChildAttached(java.util.function.Consumer<ManagedSession> action) {
				return () -> {};
			}

			@Override
			public Registration onChildDetached(java.util.function.Consumer<ManagedSession> action) {
				return () -> {};
			}

			@Override
			public String renderTree(com.slytechs.sdk.common.session.message.RenderMode mode) {
				return name;
			}

			@Override
			public String renderSummary() {
				return name;
			}

			@Override
			public void shutdown() {}

			@Override
			public void shutdownNow() {}

			@Override
			public com.slytechs.sdk.common.session.SessionState state() {
				return state;
			}
		};
	}
}
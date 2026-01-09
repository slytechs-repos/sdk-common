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
 * Unit tests for {@link ManagedStateMachine} tree structure.
 */
class ManagedStateMachineTreeTest {

	private ManagedStateMachine parent;
	private ManagedStateMachine child1;
	private ManagedStateMachine child2;

	@BeforeEach
	void setUp() {
		parent = new ManagedStateMachine("parent", () -> {});
		child1 = new ManagedStateMachine("child1", () -> {});
		child2 = new ManagedStateMachine("child2", () -> {});
		parent.enable();
	}

	@Test
	void addChildSetsParent() {
		parent.addChild(child1);

		assertTrue(child1.parent().isPresent());
		assertEquals(parent, child1.parent().get());
	}

	@Test
	void addChildAddsToList() {
		parent.addChild(child1);
		parent.addChild(child2);

		assertEquals(2, parent.children().size());
		assertTrue(parent.children().contains(child1));
		assertTrue(parent.children().contains(child2));
	}

	@Test
	void addNullChildThrows() {
		assertThrows(NullPointerException.class, () -> parent.addChild(null));
	}

	@Test
	void addChildWithExistingParentThrows() {
		parent.addChild(child1);

		ManagedStateMachine otherParent = new ManagedStateMachine("other", () -> {});
		otherParent.enable();

		assertThrows(IllegalArgumentException.class, () -> otherParent.addChild(child1));
	}

	@Test
	void removeChildClearsParent() {
		parent.addChild(child1);
		parent.removeChild(child1);

		assertTrue(child1.parent().isEmpty());
	}

	@Test
	void removeChildRemovesFromList() {
		parent.addChild(child1);
		parent.addChild(child2);
		parent.removeChild(child1);

		assertEquals(1, parent.children().size());
		assertFalse(parent.children().contains(child1));
		assertTrue(parent.children().contains(child2));
	}

	@Test
	void removeNonExistentChildNoOp() {
		parent.removeChild(child1); // Should not throw
		assertTrue(parent.children().isEmpty());
	}

	@Test
	void childrenListIsUnmodifiable() {
		parent.addChild(child1);

		assertThrows(UnsupportedOperationException.class, 
				() -> parent.children().add(child2));
	}
}
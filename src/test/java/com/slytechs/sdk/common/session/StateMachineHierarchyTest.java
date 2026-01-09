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
 * Tests for {@link StateMachine} parent-child hierarchy.
 */
class StateMachineHierarchyTest {

	private StateMachine parent;
	private StateMachine child1;
	private StateMachine child2;

	@BeforeEach
	void setUp() {
	    parent = new StateMachine("parent", 1, () -> {});  // 1 for itself
	    child1 = new StateMachine("child1", 1, () -> {});  // 1 for itself
	    child2 = new StateMachine("child2", 1, () -> {});  // 1 for itself
	    parent.enable();
	}
	
	@AfterEach
	void tearDown() {
		parent.close();
		child1.close();
		child2.close();
	}

	@Test
	void addChildRegistersWithParent() {
		parent.addChild(child1);

		// Parent should have one active component (the child)
		parent.shutdown();
		assertFalse(parent.isTerminated()); // Child not deregistered yet
	}

	@Test
	void childDeregisterNotifiesParent() {
		parent.addChild(child1);
		parent.shutdown();

		assertFalse(parent.isTerminated());

		// Child terminates
		child1.deregister();
		parent.deregister();

		assertTrue(parent.isTerminated());
	}

	@Test
	void multipleChildrenAllMustDeregister() {
		parent.addChild(child1);
		parent.addChild(child2);
		parent.shutdown();

		assertFalse(parent.isTerminated());

		child1.deregister();
		assertFalse(parent.isTerminated());

		child2.deregister();
		parent.deregister();
		assertTrue(parent.isTerminated());
	}

	@Test
	void addChildToTerminatedParentThrows() {
		parent.shutdownNow();

		assertThrows(IllegalStateException.class, () -> parent.addChild(child1));
	}

	@Test
	void childWithExistingParentThrows() {
		parent.addChild(child1);

		StateMachine parent2 = new StateMachine("parent2", () -> {});
		parent2.enable();

		try {
			assertThrows(IllegalStateException.class, () -> parent2.addChild(child1));
		} finally {
			parent2.close();
		}
	}

	@Test
	void registerWithParentSetsParent() {
		child1.registerWithParent(parent);

		parent.shutdown();
		assertFalse(parent.isTerminated());

		child1.deregister();
		parent.deregister();

		assertTrue(parent.isTerminated());
	}

	@Test
	void registerWithParentReturnsChild() {
		assertSame(child1, child1.registerWithParent(parent));
	}

	@Test
	void addChildReturnsParent() {
		assertSame(parent, parent.addChild(child1));
	}

	@Test
	void toStringShowsParent() {
		child1.registerWithParent(parent);

		String str = child1.toString();
		assertTrue(str.contains("parent=parent"));
	}

	@Test
	void nestedHierarchy() {
		// parent -> child1 -> child2
		parent.addChild(child1);
		child1.addChild(child2);

		parent.shutdown();

		// None terminated yet
		assertFalse(parent.isTerminated());
		assertFalse(child1.isTerminated());

		// Terminate from bottom up
		child2.deregister();
		assertFalse(child1.isTerminated()); // child1 waits for child2

		child1.deregister();
		parent.deregister();

		assertTrue(parent.isTerminated()); // Now parent can terminate
	}
}
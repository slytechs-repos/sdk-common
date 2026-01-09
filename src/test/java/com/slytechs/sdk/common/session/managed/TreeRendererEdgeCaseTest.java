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
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.slytechs.sdk.common.session.SessionState;
import com.slytechs.sdk.common.session.message.MessageRecord;
import com.slytechs.sdk.common.session.message.RenderMode;
import com.slytechs.sdk.common.util.Registration;

/**
 * Edge case tests for {@link TreeRenderer}.
 */
class TreeRendererEdgeCaseTest {

	@Test
	void renderEmptyTree() {
		TestSession root = new TestSession("root");

		String tree = TreeRenderer.ascii().render(root);

		System.out.println("=== Empty Tree ===");
		System.out.println(tree);

		assertTrue(tree.contains("root"));
		assertFalse(tree.contains("├──"));
		assertFalse(tree.contains("└──"));
	}

	@Test
	void renderDeeplyNestedTree() {
		TestSession root = new TestSession("level-0");

		TestSession current = root;
		for (int i = 1; i <= 10; i++) {
			TestSession child = new TestSession("level-" + i);
			current.addChild(child);
			current = child;
		}

		String tree = TreeRenderer.ascii().render(root);

		System.out.println("=== Deeply Nested (10 levels) ===");
		System.out.println(tree);

		// Verify all levels present
		for (int i = 0; i <= 10; i++) {
			assertTrue(tree.contains("level-" + i));
		}

		// Verify proper indentation (lots of │)
		// Verify all levels present and proper line count
		String[] lines = tree.split("\n");
		assertEquals(11, lines.length); // 11 levels (0-10)	
	}
	
	@Test
	void renderWideTree() {
		TestSession root = new TestSession("root");

		for (int i = 0; i < 20; i++) {
			root.addChild(new TestSession("child-" + i));
		}

		String tree = TreeRenderer.ascii().render(root);

		System.out.println("=== Wide Tree (20 children) ===");
		System.out.println(tree);

		// All children present
		for (int i = 0; i < 20; i++) {
			assertTrue(tree.contains("child-" + i));
		}

		// Last child uses └──
		assertTrue(tree.contains("└── TestSession [name=child-19"));
	}

	@Test
	void renderMixedWaitsAndChildren() {
		TestSession root = new TestSession("root");
		root.state.enable();

		root.state.register("wait1", MessageRecord.of("first wait"));
		root.addChild(new TestSession("child1"));
		root.state.register("wait2", MessageRecord.of("second wait"));
		root.addChild(new TestSession("child2"));

		String tree = TreeRenderer.ascii().render(root);

		System.out.println("=== Mixed Waits and Children ===");
		System.out.println(tree);

		assertTrue(tree.contains("wait1"));
		assertTrue(tree.contains("wait2"));
		assertTrue(tree.contains("child1"));
		assertTrue(tree.contains("child2"));
	}

	@Test
	void renderOnlyWaits() {
		TestSession root = new TestSession("root");
		root.state.enable();

		root.state.register("wait1", MessageRecord.of("msg1"));
		root.state.register("wait2", MessageRecord.of("msg2"));
		root.state.register("wait3", MessageRecord.of("msg3"));

		String tree = TreeRenderer.ascii().render(root);

		System.out.println("=== Only Waits ===");
		System.out.println(tree);

		assertTrue(tree.contains("wait1"));
		assertTrue(tree.contains("wait2"));
		assertTrue(tree.contains("wait3"));
		assertTrue(tree.contains("└── wait3")); // Last wait uses └──
	}

	@Test
	void renderVeryLongNames() {
		String longName = "x".repeat(200);
		TestSession root = new TestSession(longName);

		String tree = TreeRenderer.ascii().render(root);

		System.out.println("=== Very Long Name (200 chars) ===");
		System.out.println(tree.substring(0, Math.min(300, tree.length())) + "...");

		assertTrue(tree.contains(longName));
	}

	@Test
	void renderSpecialCharactersInNames() {
		TestSession root = new TestSession("root<>&\"'");
		root.addChild(new TestSession("child\twith\ttabs"));
		root.state.register("wait:with:colons", MessageRecord.of("msg"));

		String tree = TreeRenderer.ascii().render(root);

		System.out.println("=== Special Characters ===");
		System.out.println(tree);

		assertTrue(tree.contains("<>&"));
		assertTrue(tree.contains("\t"));
		assertTrue(tree.contains(":"));
	}

	@Test
	void renderUnicodeNames() {
		TestSession root = new TestSession("根节点");
		root.addChild(new TestSession("子节点-🎉"));
		root.state.register("待機", MessageRecord.of("メッセージ"));

		String tree = TreeRenderer.ascii().render(root);

		System.out.println("=== Unicode Names ===");
		System.out.println(tree);

		assertTrue(tree.contains("根节点"));
		assertTrue(tree.contains("子节点"));
		assertTrue(tree.contains("🎉"));
		assertTrue(tree.contains("待機"));
		assertTrue(tree.contains("メッセージ"));
	}

	@Test
	void renderAllModes() {
		java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(10);

		TestSession root = new TestSession("root");
		root.state.enable();
		root.state.register("counter",
				MessageRecord.of("value: {}",
						(java.util.function.Supplier<Integer>) counter::get));

		counter.set(20);

		System.out.println("=== CURRENT Mode ===");
		System.out.println(TreeRenderer.ascii().render(root, RenderMode.CURRENT));

		System.out.println("=== SNAPSHOT Mode ===");
		System.out.println(TreeRenderer.ascii().render(root, RenderMode.SNAPSHOT));

		System.out.println("=== TRANSITION Mode ===");
		System.out.println(TreeRenderer.ascii().render(root, RenderMode.TRANSITION));

		String transition = TreeRenderer.ascii().render(root, RenderMode.TRANSITION);
		assertTrue(transition.contains("(10→20)"));
	}

	@Test
	void detailedRendererShowsMoreInfo() {
		TestSession root = new TestSession("root");
		root.state.enable();

		String ascii = TreeRenderer.ascii().render(root);
		String detailed = TreeRenderer.detailed().render(root);

		System.out.println("=== ASCII ===");
		System.out.println(ascii);

		System.out.println("=== Detailed ===");
		System.out.println(detailed);

		// Detailed has more info
		assertTrue(detailed.length() > ascii.length());
		assertTrue(detailed.contains("running="));
		assertTrue(detailed.contains("shutdown="));
		assertTrue(detailed.contains("terminated="));

		// ASCII doesn't have these
		assertFalse(ascii.contains("running="));
	}

	@Test
	void renderComplexMixedTree() {
		TestSession root = new TestSession("Net");
		root.state.enable();

		TestSession capture = new TestSession("Capture");
		TestSession channel1 = new TestSession("Channel-0");
		TestSession channel2 = new TestSession("Channel-1");
		TestSession scope = new TestSession("TaskScope");
		TestSession worker0 = new TestSession("worker-0");
		TestSession worker1 = new TestSession("worker-1");
		TestSession worker2 = new TestSession("worker-2");

		root.addChild(capture);
		root.addChild(channel1);
		root.addChild(channel2);
		root.addChild(scope);

		scope.addChild(worker0);
		scope.addChild(worker1);
		scope.addChild(worker2);

		capture.state.register("dispatch", MessageRecord.of("capturing"));
		channel1.state.register("queue", MessageRecord.of("depth: 5"));
		worker0.state.register("proc", MessageRecord.of("processing"));
		worker1.state.register("proc", MessageRecord.of("processing"));

		String tree = TreeRenderer.ascii().render(root);

		System.out.println("=== Complex Mixed Tree ===");
		System.out.println(tree);

		// Verify structure
		assertTrue(tree.contains("Net"));
		assertTrue(tree.contains("Capture"));
		assertTrue(tree.contains("Channel-0"));
		assertTrue(tree.contains("Channel-1"));
		assertTrue(tree.contains("TaskScope"));
		assertTrue(tree.contains("worker-0"));
		assertTrue(tree.contains("worker-1"));
		assertTrue(tree.contains("worker-2"));

		// Verify waits
		assertTrue(tree.contains("dispatch"));
		assertTrue(tree.contains("queue"));
		assertTrue(tree.contains("proc"));
	}

	@Test
	void defaultRenderModeIsTransition() {
		java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(10);

		TestSession root = new TestSession("root");
		root.state.enable();
		root.state.register("counter",
				MessageRecord.of("value: {}",
						(java.util.function.Supplier<Integer>) counter::get));

		counter.set(20);

		String defaultRender = TreeRenderer.ascii().render(root);
		String transitionRender = TreeRenderer.ascii().render(root, RenderMode.TRANSITION);

		assertEquals(defaultRender, transitionRender);
	}

	/**
	 * Simple test ManagedSession implementation.
	 */
	static class TestSession implements ManagedSession {
		final ManagedStateMachine state;
		private final List<TestSession> children = new ArrayList<>();
		private TestSession parent;

		TestSession(String name) {
			this.state = new ManagedStateMachine(name, () -> {});
		}

		void addChild(TestSession child) {
			state.addChild(child.state);
			children.add(child);
			child.parent = this;
		}

		@Override
		public ManagedState managedState() {
			return state;
		}

		@Override
		public Optional<ManagedSession> parentSession() {
			return Optional.ofNullable(parent);
		}

		@Override
		@SuppressWarnings("unchecked")
		public List<ManagedSession> childSessions() {
			return (List<ManagedSession>) (List<?>) List.copyOf(children);
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
		public Registration onChildAttached(Consumer<ManagedSession> action) {
			return () -> {};
		}

		@Override
		public Registration onChildDetached(Consumer<ManagedSession> action) {
			return () -> {};
		}

		@Override
		public String renderTree(RenderMode mode) {
			return TreeRenderer.ascii().render(this, mode);
		}

		@Override
		public String renderSummary() {
			return state.toString();
		}

		@Override
		public void shutdown() {
			state.shutdown();
		}

		@Override
		public void shutdownNow() {
			state.shutdownNow();
		}

		@Override
		public SessionState state() {
			return state;
		}
	}
}
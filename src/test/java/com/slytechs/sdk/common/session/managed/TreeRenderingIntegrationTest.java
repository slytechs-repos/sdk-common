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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.slytechs.sdk.common.session.SessionState;
import com.slytechs.sdk.common.session.message.MessageRecord;
import com.slytechs.sdk.common.session.message.RenderMode;
import com.slytechs.sdk.common.util.Registration;

/**
 * Integration tests for tree rendering with nested session hierarchies.
 */
class TreeRenderingIntegrationTest {

	private TestManagedSession root;

	@BeforeEach
	void setUp() {
		root = new TestManagedSession("PcapBackend");
		((ManagedStateMachine) root.managedState()).enable();
	}

	@Test
	void renderSingleNode() {
		String tree = root.renderTree();

		System.out.println("=== Single Node ===");
		System.out.println(tree);

		assertTrue(tree.contains("PcapBackend"));
		assertTrue(tree.contains("RUNNING"));
	}

	@Test
	void renderWithOneChild() {
		TestManagedSession capture = new TestManagedSession("PcapCapture");
		root.addChild(capture);

		String tree = root.renderTree();

		System.out.println("=== One Child ===");
		System.out.println(tree);

		assertTrue(tree.contains("PcapBackend"));
		assertTrue(tree.contains("PcapCapture"));
		assertTrue(tree.contains("└──") || tree.contains("├──"));
	}

	@Test
	void renderWithMultipleChildren() {
		TestManagedSession capture = new TestManagedSession("PcapCapture");
		TestManagedSession channel = new TestManagedSession("PacketChannel");
		TestManagedSession scope = new TestManagedSession("TaskScope");

		root.addChild(capture);
		root.addChild(channel);
		root.addChild(scope);

		String tree = root.renderTree();

		System.out.println("=== Multiple Children ===");
		System.out.println(tree);

		assertTrue(tree.contains("PcapBackend"));
		assertTrue(tree.contains("PcapCapture"));
		assertTrue(tree.contains("PacketChannel"));
		assertTrue(tree.contains("TaskScope"));
	}

	@Test
	void renderNestedHierarchy() {
		TestManagedSession capture = new TestManagedSession("PcapCapture");
		TestManagedSession channel = new TestManagedSession("PacketChannel");
		TestManagedSession scope = new TestManagedSession("TaskScope");
		TestManagedSession task1 = new TestManagedSession("Task-0");
		TestManagedSession task2 = new TestManagedSession("Task-1");

		root.addChild(capture);
		capture.addChild(channel);
		root.addChild(scope);
		scope.addChild(task1);
		scope.addChild(task2);

		String tree = root.renderTree();

		System.out.println("=== Nested Hierarchy ===");
		System.out.println(tree);

		assertTrue(tree.contains("PcapBackend"));
		assertTrue(tree.contains("PcapCapture"));
		assertTrue(tree.contains("PacketChannel"));
		assertTrue(tree.contains("TaskScope"));
		assertTrue(tree.contains("Task-0"));
		assertTrue(tree.contains("Task-1"));
	}

	@Test
	void renderWithWaitInfo() {
		TestManagedSession capture = new TestManagedSession("PcapCapture");
		root.addChild(capture);

		capture.state.register("dispatch-loop",
				MessageRecord.of("waiting for pcap_dispatch on port {}", "enp15s0"));

		String tree = root.renderTree();

		System.out.println("=== With Wait Info ===");
		System.out.println(tree);

		assertTrue(tree.contains("dispatch-loop"));
		assertTrue(tree.contains("pcap_dispatch"));
		assertTrue(tree.contains("enp15s0"));
	}

	@Test
	void renderWithLazyArgs() {
		AtomicInteger queueSize = new AtomicInteger(15);

		TestManagedSession channel = new TestManagedSession("PacketChannel");
		root.addChild(channel);

		channel.state.register("queue-drain",
				MessageRecord.of("draining {} packets", 
						(java.util.function.Supplier<Integer>) queueSize::get));

		String tree = root.renderTree(RenderMode.CURRENT);

		System.out.println("=== With Lazy Args (initial) ===");
		System.out.println(tree);

		assertTrue(tree.contains("draining 15 packets"));

		// Change the value
		queueSize.set(3);

		tree = root.renderTree(RenderMode.CURRENT);

		System.out.println("=== With Lazy Args (after change) ===");
		System.out.println(tree);

		assertTrue(tree.contains("draining 3 packets"));
	}

	@Test
	void renderWithTransitions() {
		AtomicInteger queueSize = new AtomicInteger(15);

		TestManagedSession channel = new TestManagedSession("PacketChannel");
		root.addChild(channel);

		channel.state.register("queue-drain",
				MessageRecord.of("draining {} packets",
						(java.util.function.Supplier<Integer>) queueSize::get));

		// Change the value to see transition
		queueSize.set(3);

		String tree = root.renderTree(RenderMode.TRANSITION);

		System.out.println("=== With Transitions ===");
		System.out.println(tree);

		assertTrue(tree.contains("(15→3)"));
	}

	@Test
	void renderStateTransitions() {
		TestManagedSession capture = new TestManagedSession("PcapCapture");
		root.addChild(capture);

		capture.state.enable();

		// Trigger state change
		capture.state.shutdown();

		String tree = root.renderTree();

		System.out.println("=== State Transitions ===");
		System.out.println(tree);

		// Should show state transition
		assertTrue(tree.contains("PcapCapture"));
	}

	@Test
	void renderComplexScenario() {
		AtomicInteger captureCount = new AtomicInteger(0);
		AtomicInteger queueSize = new AtomicInteger(15);

		// Build hierarchy
		TestManagedSession capture = new TestManagedSession("PcapCapture");
		TestManagedSession channel = new TestManagedSession("PacketChannel");
		TestManagedSession scope = new TestManagedSession("TaskScope");
		TestManagedSession task0 = new TestManagedSession("worker-0");
		TestManagedSession task1 = new TestManagedSession("worker-1");

		root.addChild(capture);
		root.addChild(channel);
		root.addChild(scope);
		scope.addChild(task0);
		scope.addChild(task1);

		// Add wait info
		capture.state.register("dispatch-loop",
				MessageRecord.of("captured {} packets on port {}",
						(java.util.function.Supplier<Integer>) captureCount::get,
						"enp15s0"));

		channel.state.register("queue-drain",
				MessageRecord.of("draining {} packets",
						(java.util.function.Supplier<Integer>) queueSize::get));

		task0.state.register("processing",
				MessageRecord.of("blocked on channel.acquire()"));

		// Simulate some activity
		captureCount.set(12847);
		queueSize.set(3);

		// Simulate state changes
		scope.state.enable();
		scope.state.shutdown();

		String tree = root.renderTree(RenderMode.TRANSITION);

		System.out.println("=== Complex Scenario ===");
		System.out.println(tree);

		// Verify key elements present
		assertTrue(tree.contains("PcapBackend"));
		assertTrue(tree.contains("PcapCapture"));
		assertTrue(tree.contains("PacketChannel"));
		assertTrue(tree.contains("TaskScope"));
		assertTrue(tree.contains("worker-0"));
		assertTrue(tree.contains("worker-1"));
		assertTrue(tree.contains("dispatch-loop"));
		assertTrue(tree.contains("queue-drain"));
		assertTrue(tree.contains("(0→12847)")); // Capture count transition
		assertTrue(tree.contains("(15→3)"));    // Queue size transition
	}

	@Test
	void renderDetailedVsAscii() {
		TestManagedSession capture = new TestManagedSession("PcapCapture");
		root.addChild(capture);
		capture.state.enable();

		String ascii = TreeRenderer.ascii().render(root);
		String detailed = TreeRenderer.detailed().render(root);

		System.out.println("=== ASCII Renderer ===");
		System.out.println(ascii);

		System.out.println("=== Detailed Renderer ===");
		System.out.println(detailed);

		// Detailed should have more info
		assertTrue(detailed.length() > ascii.length());
		assertTrue(detailed.contains("running="));
		assertTrue(detailed.contains("shutdown="));
		assertTrue(detailed.contains("terminated="));
	}

	@Test
	void renderAfterFreeze() {
		AtomicInteger counter = new AtomicInteger(10);

		TestManagedSession channel = new TestManagedSession("PacketChannel");
		root.addChild(channel);

		channel.state.register("queue-drain",
				MessageRecord.of("draining {} packets",
						(java.util.function.Supplier<Integer>) counter::get));

		// Freeze the state
		channel.state.freeze();

		// Change value after freeze
		counter.set(999);

		String tree = root.renderTree(RenderMode.CURRENT);

		System.out.println("=== After Freeze ===");
		System.out.println(tree);

		// Should show snapshot value (10) not current (999)
		assertTrue(tree.contains("10"));
		assertTrue(tree.contains("frozen"));
		assertFalse(tree.contains("999"));
	}

	/**
	 * Simple test implementation of ManagedSession for testing.
	 */
	static class TestManagedSession implements ManagedSession {
		final ManagedStateMachine state;
		private final List<TestManagedSession> children = new java.util.ArrayList<>();
		private TestManagedSession parent;
		private Runnable shutdownHook;
		private Runnable terminationHook;

		TestManagedSession(String name) {
			this.state = new ManagedStateMachine(name, () -> {});
		}

		void addChild(TestManagedSession child) {
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
			this.shutdownHook = action;
			return this;
		}

		@Override
		public ManagedSession onTermination(Runnable action) {
			this.terminationHook = action;
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
			if (shutdownHook != null) shutdownHook.run();
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
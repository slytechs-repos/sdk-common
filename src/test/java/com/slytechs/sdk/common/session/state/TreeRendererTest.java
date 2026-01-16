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
package com.slytechs.sdk.common.session.state;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TreeRenderer}.
 */
class TreeRendererTest {

	@Test
	void renderJNetWorksHierarchy() {
		// Simulate: Net -> Capture, Channel -> Task, Task
		
		// Root: PcapBackend
		SessionStateMachine net = new SessionStateMachine("PcapBackend", () -> {});
		net.start();
		net.register(); // Count for self
		
		// Child: Capture
		SessionStateMachine capture = new SessionStateMachine("hello-capture", () -> {});
		capture.registerParent(net);
		capture.start();
		capture.register();
		
		// Child: Channel
		SessionStateMachine channel = new SessionStateMachine("hello-channel", () -> {});
		channel.registerParent(net);
		channel.start();
		channel.register();
		
		// Grandchildren: Tasks under Channel
		SessionStateMachine worker0 = new SessionStateMachine("worker-0", () -> {});
		worker0.registerParent(channel);
		worker0.start();
		worker0.register();
		
		SessionStateMachine worker1 = new SessionStateMachine("worker-1", () -> {});
		worker1.registerParent(channel);
		worker1.start();
		worker1.register();
		
		// Render the tree
		String tree = net.renderTree();
		System.out.println("=== Running State ===");
		System.out.println(tree);
		
		// Verify structure
		assertTrue(tree.contains("PcapBackend"));
		assertTrue(tree.contains("hello-capture"));
		assertTrue(tree.contains("hello-channel"));
		assertTrue(tree.contains("worker-0"));
		assertTrue(tree.contains("worker-1"));
		assertTrue(tree.contains("RUNNING"));
		
		// Verify tree characters present
		assertTrue(tree.contains("├──") || tree.contains("└──"));
		
		// Now simulate shutdown - workers terminate first
		worker0.shutdown();
		worker0.deregister();
		
		worker1.shutdown();
		worker1.deregister();
		
		// Channel draining (still has component count from self)
		channel.shutdown();
		
		String shutdownTree = net.renderTree();
		System.out.println("=== During Shutdown ===");
		System.out.println(shutdownTree);
		
		// Verify transitions shown
		assertTrue(shutdownTree.contains("RUNNING→SHUTDOWN") || shutdownTree.contains("SHUTDOWN"));
		assertTrue(shutdownTree.contains("TERMINATED") || shutdownTree.contains("SHUTDOWN→TERMINATED"));
	}
	
	@Test
	void renderShowsComponentCount() {
		SessionStateMachine parent = new SessionStateMachine("parent", () -> {});
		parent.start();
		parent.register();
		parent.register();
		parent.register(); // 3 components
		
		String tree = parent.renderTree();
		System.out.println("=== Component Count ===");
		System.out.println(tree);
		
		assertTrue(tree.contains("components=3"));
	}
	
	@Test
	void renderShowsStateTransition() {
		SessionStateMachine machine = new SessionStateMachine("test", () -> {});
		machine.start();
		machine.register();
		machine.shutdown();
		
		String tree = machine.renderTree();
		System.out.println("=== State Transition ===");
		System.out.println(tree);
		
		// Should show transition arrow
		assertTrue(tree.contains("RUNNING→SHUTDOWN"));
	}
}
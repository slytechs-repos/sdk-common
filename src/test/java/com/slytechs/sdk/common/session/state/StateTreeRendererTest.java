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
 * Tests for {@link StateTreeRenderer}.
 */
class StateTreeRendererTest {

	@Test
	void renderHierarchyWithRecords() {
		// Build hierarchy
		SessionStateMachine net = new SessionStateMachine("PcapBackend", () -> {});
		net.start();
		net.register();
		
		// Log some events on net
		net.recorder().info("Backend initialized with {} ports", 4);
		net.recorder().debug("Port discovery completed");

		SessionStateMachine capture = new SessionStateMachine("hello-capture", () -> {});
		capture.registerParent(net);
		capture.start();
		capture.register();
		
		// Log events on capture
		capture.recorder().info("Capture started on port {}", "en0");
		capture.recorder().trace("Dispatch loop entered");

		SessionStateMachine channel = new SessionStateMachine("hello-channel", () -> {});
		channel.registerParent(net);
		channel.start();
		channel.register();
		
		// Log events on channel
		channel.recorder().info("Channel attached to capture");
		channel.recorder().debug("Queue capacity set to {}", 1024);

		// Render with INFO level (should hide TRACE and DEBUG)
		System.out.println("=== INFO Level (default) ===");
		String infoTree = StateTreeRenderer.builder()
				.info()
				.render(net.components());
		System.out.println(infoTree);

		assertTrue(infoTree.contains("Backend initialized"));
		assertTrue(infoTree.contains("Capture started"));
		assertFalse(infoTree.contains("Port discovery")); // DEBUG filtered
		assertFalse(infoTree.contains("Dispatch loop")); // TRACE filtered

		// Render with DEBUG level
		System.out.println("=== DEBUG Level ===");
		String debugTree = StateTreeRenderer.builder()
				.debug()
				.render(net.components());
		System.out.println(debugTree);

		assertTrue(debugTree.contains("Port discovery")); // DEBUG included
		assertFalse(debugTree.contains("Dispatch loop")); // TRACE still filtered

		// Render with TRACE level (everything)
		System.out.println("=== TRACE Level ===");
		String traceTree = StateTreeRenderer.builder()
				.trace()
				.render(net.components());
		System.out.println(traceTree);

		assertTrue(traceTree.contains("Dispatch loop")); // TRACE included

		// Render hierarchy only (no records)
		System.out.println("=== Hierarchy Only ===");
		String hierarchyOnly = StateTreeRenderer.renderHierarchyOnly(net.components());
		System.out.println(hierarchyOnly);

		assertFalse(hierarchyOnly.contains("Backend initialized"));
		assertTrue(hierarchyOnly.contains("PcapBackend"));
	}

	@Test
	void renderNestedRecords() {
		SessionStateMachine machine = new SessionStateMachine("test-machine", () -> {});
		machine.start();
		machine.register();

		// Create nested records
		var parentRecord = machine.recorder().info("Starting operation");
		parentRecord.debug("Step 1: preparing");
		parentRecord.debug("Step 2: executing");
		var stepRecord = parentRecord.info("Step 3: finalizing");
		stepRecord.trace("Cleanup started");
		stepRecord.trace("Cleanup completed");

		System.out.println("=== Nested Records (DEBUG) ===");
		String tree = StateTreeRenderer.builder()
				.debug()
				.render(machine.components());
		System.out.println(tree);

		assertTrue(tree.contains("Starting operation"));
		assertTrue(tree.contains("Step 1"));
		assertTrue(tree.contains("Step 3"));
		assertFalse(tree.contains("Cleanup")); // TRACE filtered
	}

	@Test
	void renderWithThreadInfo() {
		SessionStateMachine machine = new SessionStateMachine("threaded", () -> {});
		machine.start();
		machine.register();

		machine.recorder().info("Main thread operation");

		System.out.println("=== With Thread Info ===");
		String tree = StateTreeRenderer.builder()
				.info()
				.showThreadInfo(true)
				.render(machine.components());
		System.out.println(tree);

		// Should contain thread name
		assertTrue(tree.contains("@"));
	}

	@Test
	void renderDuringShutdown() {
		SessionStateMachine net = new SessionStateMachine("PcapBackend", () -> {});
		net.start();
		net.register();
		net.recorder().info("Backend started");

		SessionStateMachine worker = new SessionStateMachine("worker-0", () -> {});
		worker.registerParent(net);
		worker.start();
		worker.register();
		worker.recorder().info("Worker started processing");

		// Simulate shutdown
		worker.shutdown();
		worker.recorder().warn("Worker interrupted during packet processing");
		worker.deregister();

		net.shutdown();
		net.recorder().info("Backend shutdown initiated");

		System.out.println("=== During Shutdown ===");
		String tree = StateTreeRenderer.builder()
				.info()
				.render(net.components());
		System.out.println(tree);

		assertTrue(tree.contains("RUNNING→SHUTDOWN") || tree.contains("SHUTDOWN"));
		assertTrue(tree.contains("WARN"));
		assertTrue(tree.contains("interrupted"));
	}
}
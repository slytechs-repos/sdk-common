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

import static com.slytechs.sdk.common.session.message.MessageTemplate.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.slytechs.sdk.common.session.SessionState;
import com.slytechs.sdk.common.session.message.RenderMode;
import com.slytechs.sdk.common.util.Registration;

/**
 * Integration test simulating a real packet capture scenario with
 * Net -> Capture -> Channel -> TaskScope -> Tasks hierarchy.
 */
class CaptureScenarioIntegrationTest {

	@Test
	void simulatePacketCaptureLifecycle() {
		System.out.println("========================================");
		System.out.println("  Packet Capture Lifecycle Simulation");
		System.out.println("========================================\n");

		// Metrics that change during capture
		AtomicLong packetsReceived = new AtomicLong(0);
		AtomicInteger queueDepth = new AtomicInteger(0);
		AtomicLong worker0Processed = new AtomicLong(0);
		AtomicLong worker1Processed = new AtomicLong(0);

		// Build session hierarchy
		SimSession net = new SimSession("PcapBackend");
		SimSession capture = new SimSession("PcapCapture");
		SimSession channel = new SimSession("PacketChannel");
		SimSession scope = new SimSession("TaskScope");
		SimSession worker0 = new SimSession("worker-0");
		SimSession worker1 = new SimSession("worker-1");

		net.addChild(capture);
		net.addChild(channel);
		net.addChild(scope);
		scope.addChild(worker0);
		scope.addChild(worker1);

		// Enable all
		net.state.enable();
		capture.state.enable();
		channel.state.enable();
		scope.state.enable();
		worker0.state.enable();
		worker1.state.enable();

		// Register wait info with lazy args
		capture.state.register("dispatch-loop",
				of("capturing packets on port {} (received: {})",
						"enp15s0",
						lazy(packetsReceived::get)));

		channel.state.register("queue-monitor",
				of("queue depth: {} packets",
						lazy(queueDepth::get)));

		worker0.state.register("processing",
				of("processed {} packets",
						lazy(worker0Processed::get)));

		worker1.state.register("processing",
				of("processed {} packets",
						lazy(worker1Processed::get)));

		// === Phase 1: Initial state ===
		System.out.println("--- Phase 1: Initial State ---");
		System.out.println(net.renderTree(RenderMode.CURRENT));

		// === Phase 2: Capture running ===
		System.out.println("--- Phase 2: Capture Running ---");
		packetsReceived.set(12847);
		queueDepth.set(15);
		worker0Processed.set(6400);
		worker1Processed.set(6432);
		System.out.println(net.renderTree(RenderMode.TRANSITION));

		// === Phase 3: Scope shutdown initiated ===
		System.out.println("--- Phase 3: Scope Shutdown ---");
		scope.state.captureState();
		scope.state.shutdown();
		queueDepth.set(3);
		System.out.println(net.renderTree(RenderMode.TRANSITION));

		// === Phase 4: Workers terminating ===
		System.out.println("--- Phase 4: Workers Terminating ---");
		worker0.state.captureState();
		worker0.state.shutdownNow();
		worker1.state.captureState();
		worker1.state.shutdownNow();
		queueDepth.set(0);
		worker0Processed.set(6408);
		worker1Processed.set(6439);
		System.out.println(net.renderTree(RenderMode.TRANSITION));

		// === Phase 5: Full shutdown ===
		System.out.println("--- Phase 5: Full Shutdown ---");
		capture.state.captureState();
		capture.state.shutdown();
		channel.state.captureState();
		channel.state.freeze(); // Channel freezes when drained
		net.state.captureState();
		net.state.shutdown();
		System.out.println(net.renderTree(RenderMode.TRANSITION));

		// Verify final state
		assertTrue(worker0.state.isTerminated());
		assertTrue(worker1.state.isTerminated());
		assertTrue(channel.state.isFrozen());
	}

	@Test
	void simulateDetailedVsAsciiOutput() {
		System.out.println("========================================");
		System.out.println("  Detailed vs ASCII Renderer Comparison");
		System.out.println("========================================\n");

		AtomicInteger queue = new AtomicInteger(10);

		SimSession net = new SimSession("PcapBackend");
		SimSession capture = new SimSession("PcapCapture");
		SimSession channel = new SimSession("PacketChannel");

		net.addChild(capture);
		net.addChild(channel);

		net.state.enable();
		capture.state.enable();
		channel.state.enable();

		channel.state.register("queue",
				of("depth: {}", lazy(queue::get)));

		queue.set(5);

		System.out.println("--- ASCII Renderer ---");
		System.out.println(TreeRenderer.ascii().render(net, RenderMode.TRANSITION));

		System.out.println("--- Detailed Renderer ---");
		System.out.println(TreeRenderer.detailed().render(net, RenderMode.TRANSITION));
	}

	@Test
	void simulateOrphanedPacketScenario() {
		System.out.println("========================================");
		System.out.println("  Orphaned Packet Scenario");
		System.out.println("========================================\n");

		AtomicInteger queueSize = new AtomicInteger(0);
		AtomicInteger packetsOrphaned = new AtomicInteger(0);

		SimSession net = new SimSession("PcapBackend");
		SimSession capture = new SimSession("PcapCapture");
		SimSession channel = new SimSession("BlockingBackChannel");

		net.addChild(capture);
		net.addChild(channel);

		net.state.enable();
		capture.state.enable();
		channel.state.enable();

		capture.state.register("dispatch",
				of("dispatching packets"));

		channel.state.register("queue",
				of("queue: {} packets, orphaned: {}",
						lazy(queueSize::get),
						lazy(packetsOrphaned::get)));

		// Worker attached
		System.out.println("--- Worker Attached ---");
		queueSize.set(0);
		System.out.println(net.renderTree(RenderMode.CURRENT));

		// Worker detaches, packet arrives
		System.out.println("--- Worker Detached, Packet Arrives ---");
		queueSize.set(1);
		System.out.println(net.renderTree(RenderMode.TRANSITION));

		// Packet orphaned and released
		System.out.println("--- Orphan Released ---");
		queueSize.set(0);
		packetsOrphaned.set(1);
		System.out.println(net.renderTree(RenderMode.TRANSITION));
	}

	/**
	 * Simplified ManagedSession for simulation.
	 */
	static class SimSession implements ManagedSession {
		final ManagedStateMachine state;
		private final List<SimSession> children = new ArrayList<>();
		private SimSession parent;

		SimSession(String name) {
			this.state = new ManagedStateMachine(name, () -> {});
		}

		void addChild(SimSession child) {
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
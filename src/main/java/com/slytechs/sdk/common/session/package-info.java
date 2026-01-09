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

/**
 * Core session lifecycle management for Sly Technologies SDKs.
 * 
 * <h2>Overview</h2>
 * 
 * <p>
 * This package provides the foundational infrastructure for managing the lifecycle
 * of sessions across all Sly Technologies products including jNetWorks, jNetPcap,
 * Protocol modules, and ExaScale. Sessions represent long-running operations such as
 * packet capture, transmission, inline forwarding, protocol processing, file operations,
 * and task execution. The session framework provides:
 * </p>
 * <ul>
 * <li>Unified lifecycle states: running, shutdown scheduled, shutdown, terminated</li>
 * <li>Hierarchical session management with parent-child relationships</li>
 * <li>Thread-safe state transitions and coordination</li>
 * <li>Scheduled and immediate shutdown capabilities</li>
 * <li>Await mechanisms for synchronization</li>
 * </ul>
 * 
 * <h2>Package Structure</h2>
 * 
 * <p>
 * The session package is organized into three tiers:
 * </p>
 * 
 * <h3>Core Package (com.slytechs.sdk.common.session)</h3>
 * <p>
 * Public API for session lifecycle management:
 * </p>
 * <ul>
 * <li>{@link com.slytechs.sdk.common.session.Session} - Primary interface for
 *     lifecycle operations (shutdown, await, scheduling)</li>
 * <li>{@link com.slytechs.sdk.common.session.SessionState} - Read-only state queries
 *     (isRunning, isShutdown, isTerminated)</li>
 * <li>{@link com.slytechs.sdk.common.session.StateMachine} - Counter-based state
 *     tracking with loose parent-child coupling</li>
 * <li>{@link com.slytechs.sdk.common.session.SessionException} - Base exception for
 *     session errors</li>
 * <li>{@link com.slytechs.sdk.common.session.SessionShutdownException} - Thrown when
 *     operations are attempted on shutdown sessions</li>
 * </ul>
 * 
 * <h3>Managed Package (com.slytechs.sdk.common.session.managed)</h3>
 * <p>
 * Internal API for observable session hierarchies with debugging support:
 * </p>
 * <ul>
 * <li>{@link com.slytechs.sdk.common.session.managed.ManagedSession} - Extended session
 *     with tree structure, event hooks, and rendering</li>
 * <li>{@link com.slytechs.sdk.common.session.managed.ManagedState} - Extended state with
 *     wait tracking and freeze semantics</li>
 * <li>{@link com.slytechs.sdk.common.session.managed.ManagedStateMachine} - Implementation
 *     wrapping StateMachine with observability</li>
 * <li>{@link com.slytechs.sdk.common.session.managed.WaitInfo} - Diagnostic record tracking
 *     what is blocking termination</li>
 * <li>{@link com.slytechs.sdk.common.session.managed.TreeRenderer} - Visualization of
 *     session hierarchies</li>
 * </ul>
 * 
 * <h3>Message Package (com.slytechs.sdk.common.session.message)</h3>
 * <p>
 * SLF4J-style message templating with lazy evaluation:
 * </p>
 * <ul>
 * <li>{@link com.slytechs.sdk.common.session.message.MessageRecord} - Template with
 *     placeholder arguments</li>
 * <li>{@link com.slytechs.sdk.common.session.message.LazyArg} - Deferred evaluation with
 *     snapshot capture</li>
 * <li>{@link com.slytechs.sdk.common.session.message.RenderMode} - Output format control
 *     (CURRENT, SNAPSHOT, TRANSITION)</li>
 * </ul>
 * 
 * <h2>Lifecycle States</h2>
 * 
 * <p>
 * Sessions progress through distinct, non-overlapping lifecycle stages:
 * </p>
 * 
 * <pre>
 *     CREATED ──► RUNNING ──► SHUTDOWN_SCHEDULED ──► SHUTDOWN ──► TERMINATED
 *                    │                                   ▲
 *                    └───────────────────────────────────┘
 *                         (direct shutdown, no scheduling)
 * </pre>
 * 
 * <table border="1" cellpadding="5">
 * <caption>Lifecycle State Definitions</caption>
 * <tr><th>State</th><th>Query Method</th><th>Description</th></tr>
 * <tr>
 *   <td>RUNNING</td>
 *   <td>{@code isRunning()}</td>
 *   <td>Session is active and operational</td>
 * </tr>
 * <tr>
 *   <td>SHUTDOWN_SCHEDULED</td>
 *   <td>{@code isShutdownScheduled()}</td>
 *   <td>Shutdown scheduled but not yet initiated</td>
 * </tr>
 * <tr>
 *   <td>SHUTDOWN</td>
 *   <td>{@code isShutdown()}</td>
 *   <td>Shutdown initiated, completing in-progress operations</td>
 * </tr>
 * <tr>
 *   <td>TERMINATED</td>
 *   <td>{@code isTerminated()}</td>
 *   <td>All operations complete, resources released</td>
 * </tr>
 * </table>
 * 
 * <h2>Session Hierarchy</h2>
 * 
 * <p>
 * Sessions form hierarchical trees where parent sessions wait for children to
 * terminate before completing their own termination. This pattern is used across
 * all Sly Technologies products:
 * </p>
 * 
 * <pre>
 * Root Session (Net, PcapHandle, ExaVolume, etc.)
 * ├── Operation Session (Capture, Inspector, Inline, FileReader, etc.)
 * │   └── Channel/Stream Session (data flow)
 * ├── Operation Session
 * │   └── Channel/Stream Session
 * └── TaskScope (worker management)
 *     ├── Task (worker thread)
 *     └── Task
 * </pre>
 * 
 * <h3>Coupling Models</h3>
 * 
 * <p>
 * The package provides two coupling models for parent-child relationships:
 * </p>
 * 
 * <table border="1" cellpadding="5">
 * <caption>Parent-Child Coupling Models</caption>
 * <tr><th>Model</th><th>Class</th><th>Characteristics</th></tr>
 * <tr>
 *   <td>Loose Coupling</td>
 *   <td>{@link com.slytechs.sdk.common.session.StateMachine}</td>
 *   <td>Counter-based tracking, no hard references, lightweight</td>
 * </tr>
 * <tr>
 *   <td>Rich Coupling</td>
 *   <td>{@link com.slytechs.sdk.common.session.managed.ManagedStateMachine}</td>
 *   <td>Hard references, tree navigation, wait tracking, debugging</td>
 * </tr>
 * </table>
 * 
 * <h2>Usage Patterns</h2>
 * 
 * <h3>Basic Session Lifecycle</h3>
 * <pre>{@code
 * try (Net net = new PcapBackend()) {
 *     
 *     Capture capture = net.capture("analysis")
 *         .ports("en0")
 *         .assignTo(channel)
 *         .apply();
 *     
 *     try (TaskScope scope = new TaskScope(net)) {
 *         scope.fork(channel, this::processPackets)
 *             .shutdownAfter(Duration.ofMinutes(5))
 *             .awaitCompletion();
 *     }
 *     
 * } // Net.close() triggers hierarchical shutdown
 * }</pre>
 * 
 * <h3>Worker Loop Pattern</h3>
 * <pre>{@code
 * void processPackets(PacketChannel channel) {
 *     while (channel.isActive()) {
 *         try {
 *             Packet packet = channel.acquire();
 *             process(packet);
 *             channel.release(packet);
 *         } catch (SessionShutdownException e) {
 *             break; // Normal shutdown, exit loop
 *         }
 *     }
 * }
 * }</pre>
 * 
 * <h3>Diagnostic Tree Rendering</h3>
 * <pre>{@code
 * // Render session hierarchy for debugging
 * if (net instanceof ManagedSession managed) {
 *     System.out.println(managed.renderTree());
 * }
 * 
 * // Output:
 * // PcapBackend [name=pcap, state=RUNNING→SHUTDOWN]
 * // ├── PcapCapture [name=analysis, state=RUNNING]
 * // │   └── dispatch-loop: waiting for pcap_dispatch [2.3s]
 * // ├── PacketChannel [name=channel-0, state=DRAINING]
 * // │   └── queue-drain: draining (15→3) packets [1.1s]
 * // └── TaskScope [name=scope-1, state=TERMINATED]
 * }</pre>
 * 
 * <h3>Wait Tracking</h3>
 * <pre>{@code
 * // Register diagnostic wait info
 * ManagedState state = ((ManagedSession) capture).managedState();
 * 
 * Registration reg = state.register("dispatch-loop",
 *     MessageRecord.of("waiting for pcap_dispatch on port {}", portName));
 * 
 * try {
 *     pcap_dispatch(...);
 * } finally {
 *     reg.unregister();
 * }
 * }</pre>
 * 
 * <h2>Thread Safety</h2>
 * 
 * <p>
 * All session classes are designed for concurrent access:
 * </p>
 * <ul>
 * <li>State queries are atomic and lock-free</li>
 * <li>Shutdown operations are idempotent</li>
 * <li>Await methods handle spurious wakeups</li>
 * <li>Collections use copy-on-write semantics where iteration during modification
 *     is expected</li>
 * </ul>
 * 
 * <h2>API Visibility</h2>
 * 
 * <p>
 * The session package distinguishes between public and internal APIs:
 * </p>
 * 
 * <table border="1" cellpadding="5">
 * <caption>API Visibility</caption>
 * <tr><th>API</th><th>Audience</th><th>Interfaces</th></tr>
 * <tr>
 *   <td>Public</td>
 *   <td>End users</td>
 *   <td>{@code Session}, {@code SessionState}</td>
 * </tr>
 * <tr>
 *   <td>Internal</td>
 *   <td>Backend implementations</td>
 *   <td>{@code ManagedSession}, {@code ManagedState}</td>
 * </tr>
 * </table>
 * 
 * <p>
 * Users interact with {@code Session} and {@code SessionState}. Backend
 * implementations (PcapBackend, DpdkBackend, NtapiBackend) implement
 * {@code ManagedSession} for internal coordination but expose only
 * {@code Session} to users.
 * </p>
 * 
 * <h2>Integration with Sly Technologies Products</h2>
 * 
 * <p>
 * The session framework integrates across all Sly Technologies products:
 * </p>
 * 
 * <table border="1" cellpadding="5">
 * <caption>Product Integration</caption>
 * <tr><th>Product</th><th>Root Session</th><th>Operation Sessions</th></tr>
 * <tr>
 *   <td>jNetWorks</td>
 *   <td>Net (PcapBackend, DpdkBackend, NtapiBackend)</td>
 *   <td>Capture, Inspector, Inline, Channel, TaskScope</td>
 * </tr>
 * <tr>
 *   <td>jNetPcap</td>
 *   <td>PcapHandle, Pcap</td>
 *   <td>PcapLive, PcapOffline, PcapDumper</td>
 * </tr>
 * <tr>
 *   <td>Protocol Modules</td>
 *   <td>ProtocolStack</td>
 *   <td>Dissector, Reassembler, Decoder</td>
 * </tr>
 * <tr>
 *   <td>ExaScale</td>
 *   <td>ExaVolume, ExaCluster</td>
 *   <td>ExaReader, ExaWriter, ExaQuery, ExaIndex</td>
 * </tr>
 * </table>
 * 
 * <h3>Common Session Behaviors</h3>
 * 
 * <table border="1" cellpadding="5">
 * <caption>Session Type Behaviors</caption>
 * <tr><th>Session Type</th><th>Lifecycle Behavior</th><th>Examples</th></tr>
 * <tr>
 *   <td>Root</td>
 *   <td>Parent of all child sessions, close triggers hierarchical shutdown</td>
 *   <td>Net, PcapHandle, ExaVolume</td>
 * </tr>
 * <tr>
 *   <td>Bound Operation</td>
 *   <td>Stops on scope shutdown (autoShutdown=true)</td>
 *   <td>Capture, PcapLive</td>
 * </tr>
 * <tr>
 *   <td>Independent Operation</td>
 *   <td>Runs until explicit stop/delete (autoShutdown=false)</td>
 *   <td>Inspector, Inline, ExaQuery</td>
 * </tr>
 * <tr>
 *   <td>Channel/Stream</td>
 *   <td>Drains on shutdown, transitions through BackChannel states</td>
 *   <td>PacketChannel, BufferChannel</td>
 * </tr>
 * <tr>
 *   <td>TaskScope</td>
 *   <td>Manages worker threads, coordinates shutdown order</td>
 *   <td>TaskScope</td>
 * </tr>
 * </table>
 * 
 * <h2>Design Principles</h2>
 * 
 * <ul>
 * <li><b>Composition over inheritance</b> - ManagedStateMachine wraps StateMachine</li>
 * <li><b>Explicit intent</b> - capture(), inspector(), inline() declare lifecycle behavior</li>
 * <li><b>Safe defaults</b> - capture stops with scope, inline continues autonomously</li>
 * <li><b>Observable state</b> - tree rendering and wait tracking for debugging</li>
 * <li><b>Freeze semantics</b> - terminated sessions preserve diagnostic snapshots</li>
 * </ul>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see com.slytechs.sdk.common.session.Session
 * @see com.slytechs.sdk.common.session.SessionState
 * @see com.slytechs.sdk.common.session.managed.ManagedSession
 */
package com.slytechs.sdk.common.session;
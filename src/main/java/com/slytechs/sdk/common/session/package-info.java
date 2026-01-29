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
 * This package provides foundational infrastructure for managing the lifecycle
 * of sessions across all Sly Technologies products including jNetWorks, jNetPcap,
 * Protocol modules, and ExaScale. Sessions represent long-running operations such as
 * packet capture, transmission, inline forwarding, protocol processing, file operations,
 * and task execution. The session framework provides:
 * </p>
 * <ul>
 * <li>Unified lifecycle states: created, running, shutdown, terminated</li>
 * <li>Hierarchical session management with parent-child relationships</li>
 * <li>Thread-safe state transitions and coordination</li>
 * <li>Scheduled and immediate shutdown capabilities</li>
 * <li>Await mechanisms for synchronization</li>
 * <li>Diagnostic tree rendering with filtered log records</li>
 * </ul>
 * 
 * <h2>Package Structure</h2>
 * 
 * <h3>Core Package (com.slytechs.sdk.common.session)</h3>
 * <p>
 * Public API for session lifecycle management:
 * </p>
 * <ul>
 * <li>{@link com.slytechs.sdk.common.session.Session} - Primary interface for
 *     lifecycle operations (shutdown, await, scheduling)</li>
 * <li>{@link com.slytechs.sdk.common.session.SystemSession} - Extended session
 *     with closeable support for try-with-resources</li>
 * <li>{@link com.slytechs.sdk.common.session.state.SessionState} - Read-only state queries
 *     (isRunning, isShutdown, isTerminated)</li>
 * <li>{@link com.slytechs.sdk.common.session.SessionException} - Base exception for
 *     session errors</li>
 * <li>{@link com.slytechs.sdk.common.session.SessionShutdownException} - Thrown when
 *     operations are attempted on shutdown sessions</li>
 * </ul>
 * 
 * <h3>State Package (com.slytechs.sdk.common.session.state)</h3>
 * <p>
 * Generic state machine infrastructure with hierarchical tracking:
 * </p>
 * <ul>
 * <li>{@link com.slytechs.sdk.common.session.state.State} - Interface for state enums
 *     with transition rules</li>
 * <li>{@link com.slytechs.sdk.common.session.state.StateMachine} - Generic state machine
 *     base with observers and actions</li>
 * <li>{@link com.slytechs.sdk.common.session.state.SystemStateMachine} - Standard
 *     CREATED/RUNNING/SHUTDOWN/TERMINATED lifecycle</li>
 * <li>{@link com.slytechs.sdk.common.session.state.StateHierarchyTree} - Parent-child hierarchy
 *     with counter-based tracking</li>
 * <li>{@link com.slytechs.sdk.common.session.state.CountableState} - Interface for states
 *     that support increment/decrement counting</li>
 * <li>{@link com.slytechs.sdk.common.session.state.HierarchalState} - Interface for states
 *     with parent-child relationships</li>
 * <li>{@link com.slytechs.sdk.common.session.state.TransitionObserver} - Callback for
 *     state transitions</li>
 * <li>{@link com.slytechs.sdk.common.session.state.TransitionScheduler} - Scheduled
 *     state transitions (shutdown after duration)</li>
 * <li>{@link com.slytechs.sdk.common.session.state.StateWaitBarrier} - Concurrent
 *     barrier that waits for zero count</li>
 * <li>{@link com.slytechs.sdk.common.session.state.StateTreeRenderer} - ASCII tree
 *     visualization with filtered log records</li>
 * <li>{@link com.slytechs.sdk.common.session.state.TreeRenderer} - Hierarchy-only
 *     tree visualization</li>
 * </ul>
 * 
 * <h3>State Recorder Package (com.slytechs.sdk.common.session.state.recorder)</h3>
 * <p>
 * Hierarchical logging and diagnostic recording:
 * </p>
 * <ul>
 * <li>{@link com.slytechs.sdk.common.session.state.recorder.LogLevel} - Log levels
 *     (TRACE, DEBUG, INFO, WARN, ERROR, OFF)</li>
 * <li>{@link com.slytechs.sdk.common.session.state.recorder.StateRecord} - Hierarchical
 *     log entry with thread and state snapshots</li>
 * <li>{@link com.slytechs.sdk.common.session.state.recorder.StateRecorder} - Per-machine
 *     record collection with SLF4J-style logging</li>
 * <li>{@link com.slytechs.sdk.common.session.state.recorder.StateLogger} - Interface for
 *     recording log entries</li>
 * <li>{@link com.slytechs.sdk.common.session.state.recorder.StateSnapshot} - Captured
 *     state at record time</li>
 * <li>{@link com.slytechs.sdk.common.session.state.recorder.ThreadSnapshot} - Captured
 *     thread info at record time</li>
 * </ul>
 * 
 * <h2>Lifecycle States</h2>
 * 
 * <p>
 * Sessions progress through distinct lifecycle stages:
 * </p>
 * 
 * <pre>
 * CREATED ──► RUNNING ──► SHUTDOWN ──► TERMINATED
 * </pre>
 * 
 * <table border="1" cellpadding="5">
 * <caption>Lifecycle State Definitions</caption>
 * <tr><th>State</th><th>Query Method</th><th>Description</th></tr>
 * <tr>
 *   <td>CREATED</td>
 *   <td>-</td>
 *   <td>Initial state before start</td>
 * </tr>
 * <tr>
 *   <td>RUNNING</td>
 *   <td>{@code isRunning()}</td>
 *   <td>Session is active and operational</td>
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
 * terminate before completing their own termination:
 * </p>
 * 
 * <pre>
 * Root Session (Net, PcapHandle, ExaVolume)
 * ├── Operation Session (Capture, Inspector, Inline)
 * │   └── Channel Session (PacketChannel, BufferChannel)
 * ├── TaskExecutor
 * │   ├── TaskGroup
 * │   │   ├── Task
 * │   │   └── Task
 * │   └── Task
 * ├── Application (user state machines)
 * └── Sandbox (unmanaged channel access)
 * </pre>
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
 *     try (TaskExecutor executor = net.executor("packet-task")
 *             .onTaskException(this::handleErrors)
 *             .maxRestarts(3)) {
 *         
 *         executor.fork(channel, this::processPackets)
 *             .shutdownAfter(Duration.ofMinutes(5))
 *             .awaitCompletion();
 *     }
 *     
 * } // Net.close() triggers hierarchical shutdown
 * }</pre>
 * 
 * <h3>Worker Loop Pattern</h3>
 * <pre>{@code
 * void processPackets(PacketChannel channel) throws Exception {
 *     while (channel.isActive()) {
 *         Packet packet = channel.acquire();
 *         process(packet);
 *         channel.release(packet);
 *     }
 * }
 * }</pre>
 * 
 * <h3>Task Groups</h3>
 * <pre>{@code
 * try (TaskExecutor executor = net.executor("pipeline")) {
 *     
 *     // Group related tasks
 *     TaskGroup analyzers = executor.group("analyzers")
 *         .fork(channel1, this::analyzeHttp)
 *         .fork(channel2, this::analyzeDns);
 *     
 *     TaskGroup writers = executor.group("file-writers")
 *         .fork(pcapChannel, this::writePcap);
 *     
 *     // Independent shutdown per group
 *     writers.shutdownAfter(Duration.ofMinutes(5));
 *     
 *     executor.awaitCompletion();
 * }
 * }</pre>
 * 
 * <h3>Error Handling and Recovery</h3>
 * <pre>{@code
 * TaskRecovery handleErrors(TaskContext ctx, Throwable e) {
 *     if (e instanceof OutOfMemoryError) {
 *         System.gc();
 *         return TaskRecovery.RESTART_DELAYED;
 *     }
 *     if (ctx.restartCount() < 3) {
 *         return TaskRecovery.RESTART;
 *     }
 *     logger.error("Task {} failed: {}", ctx.name(), e);
 *     return TaskRecovery.FAIL;
 * }
 * }</pre>
 * 
 * <h3>Diagnostic Tree Rendering</h3>
 * <pre>{@code
 * // Render session hierarchy with log records
 * System.out.println(net.state().renderTree(LogLevel.DEBUG));
 * 
 * // Output:
 * // PcapBackend [name=pcap, state=RUNNING→SHUTDOWN, count=3]
 * // │   [DEBUG] PcapBackend changed state RUNNING -> SHUTDOWN
 * // │   [INFO ] Backend initialized with 4 ports
 * // ├── Capture [name=analysis, state=RUNNING, count=1]
 * // │   └── [INFO ] Capture started on port en0
 * // └── TaskExecutor [name=pipeline, state=RUNNING, count=2]
 * //     ├── TaskGroup [name=analyzers, state=RUNNING, count=2]
 * //     │   ├── Task [name=analyzeHttp, state=ACTIVE]
 * //     │   └── Task [name=analyzeDns, state=ACTIVE]
 * //     └── Task [name=writer, state=TERMINATED]
 * }</pre>
 * 
 * <h2>Thread Safety</h2>
 * 
 * <p>
 * All session classes are designed for concurrent access:
 * </p>
 * <ul>
 * <li>State queries are atomic and lock-free</li>
 * <li>Shutdown operations are idempotent (return true only on first call)</li>
 * <li>Await methods handle spurious wakeups</li>
 * <li>Collections use copy-on-write semantics</li>
 * <li>Counter operations are atomic</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * 
 * <table border="1" cellpadding="5">
 * <caption>Integration Points</caption>
 * <tr><th>Type</th><th>Purpose</th><th>Example</th></tr>
 * <tr>
 *   <td>TaskExecutor</td>
 *   <td>Managed task execution with error recovery</td>
 *   <td>{@code net.executor("name")}</td>
 * </tr>
 * <tr>
 *   <td>TaskGroup</td>
 *   <td>Group related tasks with independent lifecycle</td>
 *   <td>{@code executor.group("name")}</td>
 * </tr>
 * <tr>
 *   <td>Application</td>
 *   <td>Attach user state machines to lifecycle</td>
 *   <td>{@code net.application("name")}</td>
 * </tr>
 * <tr>
 *   <td>Sandbox</td>
 *   <td>Unmanaged channel access with optional state</td>
 *   <td>{@code net.sandbox("name")}</td>
 * </tr>
 * <tr>
 *   <td>Plugin</td>
 *   <td>Extend jNetWorks functionality</td>
 *   <td>{@code net.plugin(new CustomExporter())}</td>
 * </tr>
 * </table>
 * 
 * <h2>Product Integration</h2>
 * 
 * <table border="1" cellpadding="5">
 * <caption>Product Integration</caption>
 * <tr><th>Product</th><th>Root Session</th><th>Operations</th></tr>
 * <tr>
 *   <td>jNetWorks</td>
 *   <td>Net (PcapBackend, DpdkBackend, NtapiBackend)</td>
 *   <td>Capture, Inspector, Inline, Channel, TaskExecutor</td>
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
 * <h2>Design Principles</h2>
 * 
 * <ul>
 * <li><b>Composition over inheritance</b> - StateMachine composes with StateHierarchyTree</li>
 * <li><b>Generic state machines</b> - State&lt;T&gt; enables type-safe custom states</li>
 * <li><b>Counter-based coordination</b> - increment/decrement for parent-child tracking</li>
 * <li><b>Observable state</b> - Tree rendering with filtered log records</li>
 * <li><b>Recovery options</b> - FAIL, RESTART, RESTART_DELAYED, SHUTDOWN_GROUP</li>
 * <li><b>Virtual threads</b> - Default ThreadFactory uses virtual threads</li>
 * </ul>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see com.slytechs.sdk.common.session.Session
 * @see com.slytechs.sdk.common.session.state.SessionState
 * @see com.slytechs.sdk.common.session.state.StateMachine
 * @see com.slytechs.sdk.common.session.state.SystemStateMachine
 */
package com.slytechs.sdk.common.session;
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
 * Generic state machine infrastructure with hierarchical tracking and diagnostics.
 * 
 * <h2>Overview</h2>
 * 
 * <p>
 * This package provides a flexible, type-safe state machine framework that supports:
 * </p>
 * <ul>
 * <li>Generic state types via {@link State}&lt;T&gt; interface</li>
 * <li>Parent-child hierarchy with counter-based coordination</li>
 * <li>Transition observers and scheduled transitions</li>
 * <li>Diagnostic recording with log levels</li>
 * <li>ASCII tree rendering for debugging</li>
 * </ul>
 * 
 * <h2>Core Components</h2>
 * 
 * <h3>State Interface</h3>
 * <p>
 * The {@link State}&lt;T&gt; interface defines the contract for state enums:
 * </p>
 * <ul>
 * <li>{@code canTransition(T newState)} - validates state transitions</li>
 * <li>{@code futureTense()}, {@code presentTense()}, {@code pastTense()} - human-readable descriptions</li>
 * </ul>
 * 
 * <h3>StateMachine</h3>
 * <p>
 * The {@link StateMachine}&lt;T&gt; base class provides:
 * </p>
 * <ul>
 * <li>Thread-safe state transitions with atomic operations</li>
 * <li>Transition observers for state change notifications</li>
 * <li>State entry/exit actions</li>
 * <li>Integrated {@link StateRecorder} for diagnostic logging</li>
 * <li>Reset capability for task restart scenarios</li>
 * </ul>
 * 
 * <h3>ComponentTree</h3>
 * <p>
 * The {@link ComponentTree}&lt;T&gt; class manages parent-child relationships:
 * </p>
 * <ul>
 * <li>Counter-based tracking via {@code increment()}/{@code decrement()}</li>
 * <li>Parent registration for hierarchical shutdown coordination</li>
 * <li>Children list for tree traversal and rendering</li>
 * <li>Automatic state transitions when count reaches zero</li>
 * </ul>
 * 
 * <h2>Pre-built State Machines</h2>
 * 
 * <h3>LifecycleStateMachine</h3>
 * <p>
 * Standard session lifecycle with states:
 * </p>
 * <pre>
 * CREATED ──► RUNNING ──► SHUTDOWN ──► TERMINATED
 * </pre>
 * <p>
 * Used by Net, Capture, Channel, TaskExecutor, and other session types.
 * Provides convenience methods: {@code start()}, {@code shutdown()}, 
 * {@code isRunning()}, {@code isShutdown()}, {@code isTerminated()}.
 * </p>
 * 
 * <h3>TaskStateMachine</h3>
 * <p>
 * Task execution lifecycle with error handling:
 * </p>
 * <pre>
 * CREATED ──► ACTIVE ──► TERMINATED (normal completion)
 *                    └──► ERROR (exception thrown)
 * </pre>
 * <p>
 * Both ERROR and TERMINATED are terminal states. Supports reset for task restart.
 * </p>
 * 
 * <h2>Supporting Classes</h2>
 * 
 * <table border="1" cellpadding="5">
 * <caption>Supporting Classes</caption>
 * <tr><th>Class</th><th>Purpose</th></tr>
 * <tr>
 *   <td>{@link TransitionObserver}</td>
 *   <td>Callback interface for state transition notifications</td>
 * </tr>
 * <tr>
 *   <td>{@link TransitionScheduler}</td>
 *   <td>Schedules future state transitions (e.g., shutdown after duration)</td>
 * </tr>
 * <tr>
 *   <td>{@link StateZeroCountBarrier}</td>
 *   <td>Concurrent barrier that awaits zero count for termination</td>
 * </tr>
 * <tr>
 *   <td>{@link CountableState}</td>
 *   <td>Interface for states supporting increment/decrement</td>
 * </tr>
 * <tr>
 *   <td>{@link HierarchalState}</td>
 *   <td>Interface for states with parent-child relationships</td>
 * </tr>
 * <tr>
 *   <td>{@link RenderMode}</td>
 *   <td>Output format control: CURRENT, SNAPSHOT, TRANSITION</td>
 * </tr>
 * </table>
 * 
 * <h2>Tree Rendering</h2>
 * 
 * <h3>StateTreeRenderer</h3>
 * <p>
 * Renders component hierarchy with filtered log records:
 * </p>
 * <pre>{@code
 * StateTreeRenderer renderer = new StateTreeRenderer()
 *     .threshold(LogLevel.DEBUG)
 *     .showRecords(true);
 * 
 * System.out.println(renderer.render(rootComponent));
 * }</pre>
 * 
 * <p>Output example:</p>
 * {@snippet :
 * PcapBackend [name=pcap, state=RUNNING→SHUTDOWN, count=2]
 * │   [DEBUG] PcapBackend changed state RUNNING -> SHUTDOWN
 * │   [INFO ] Backend initialized with 4 ports
 * ├── Capture [name=analysis, state=RUNNING, count=1]
 * │   └── [INFO ] Capture started on port en0
 * └── TaskExecutor [name=pipeline, state=RUNNING, count=2]
 *     ├── Task [name=task-0, state=ACTIVE]
 *     └── Task [name=task-1, state=TERMINATED]
 * }
 * 
 * <h3>TreeRenderer</h3>
 * <p>
 * Renders hierarchy only, without log records:
 * </p>
 * {@snippet :
 * PcapBackend [name=pcap, state=RUNNING]
 * ├── Capture [name=analysis, state=RUNNING]
 * └── TaskExecutor [name=pipeline, state=RUNNING]
 * }
 * 
 * <h2>Usage Examples</h2>
 * 
 * <h3>Creating a Custom State Machine</h3>
 * {@snippet :
 * public enum ChannelState implements State<ChannelState> {
 *     CREATED(true),
 *     ATTACHED(true),
 *     DRAINING(true) {
 *         @Override
 *         public boolean canTransition(ChannelState newState) {
 *             return newState == DRAINED;
 *         }
 *     },
 *     DRAINED(true),
 *     DETACHED(false);
 *     
 *     private final boolean transitionsAllowed;
 *     
 *     ChannelState(boolean transitionsAllowed) {
 *         this.transitionsAllowed = transitionsAllowed;
 *     }
 *     
 *     @Override
 *     public boolean canTransition(ChannelState newState) {
 *         return transitionsAllowed;
 *     }
 * }
 * 
 * public class ChannelStateMachine extends StateMachine<ChannelState> {
 *     public ChannelStateMachine(String name) {
 *         super(name, ChannelState.CREATED);
 *     }
 *     
 *     public boolean attach() {
 *         return transitionTo(ChannelState.ATTACHED);
 *     }
 *     
 *     public boolean drain() {
 *         return transitionTo(ChannelState.DRAINING);
 *     }
 * }
 * }
 * 
 * <h3>Parent-Child Registration</h3>
 * {@snippet :
 * // Parent creates child and registers
 * LifecycleStateMachine parent = new LifecycleStateMachine("parent");
 * LifecycleStateMachine child = new LifecycleStateMachine("child");
 * 
 * child.registerParent(parent);  // Increments parent's count
 * child.start();
 * 
 * // ... child does work ...
 * 
 * child.shutdown();
 * // When child terminates, parent's count decrements
 * // Parent can terminate when count reaches zero
 * }
 * 
 * <h3>Scheduled Shutdown</h3>
 * {@snippet :
 * LifecycleStateMachine machine = new LifecycleStateMachine("timed");
 * machine.start();
 * 
 * // Shutdown after 5 minutes
 * machine.shutdownAfter(Duration.ofMinutes(5));
 * 
 * // Or at specific time
 * machine.shutdownAt(Instant.now().plus(Duration.ofHours(1)));
 * 
 * // Cancel scheduled shutdown
 * machine.cancelScheduledShutdown();
 * }
 * 
 * <h3>Transition Observers</h3>
 * {@snippet :
 * machine.observeTransitions((source, oldState, newState) -> {
 *     logger.info("{} transitioned {} -> {}", 
 *         source.name(), oldState, newState);
 * });
 * }
 * 
 * <h2>Thread Safety</h2>
 * 
 * <p>
 * All classes in this package are designed for concurrent access:
 * </p>
 * <ul>
 * <li>State transitions use atomic compare-and-swap</li>
 * <li>Counter operations are atomic</li>
 * <li>Observer lists use copy-on-write semantics</li>
 * <li>Await operations handle spurious wakeups</li>
 * </ul>
 * 
 * <h2>Recorder Subpackage</h2>
 * 
 * <p>
 * The {@code recorder} subpackage provides diagnostic logging:
 * </p>
 * <ul>
 * <li>{@link com.slytechs.sdk.common.session.state.recorder.LogLevel} - TRACE, DEBUG, INFO, WARN, ERROR, OFF</li>
 * <li>{@link com.slytechs.sdk.common.session.state.recorder.StateRecorder} - SLF4J-style logging per machine</li>
 * <li>{@link com.slytechs.sdk.common.session.state.recorder.StateRecord} - Hierarchical log entry</li>
 * </ul>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see com.slytechs.sdk.common.session.state.State
 * @see com.slytechs.sdk.common.session.state.StateMachine
 * @see com.slytechs.sdk.common.session.state.LifecycleStateMachine
 * @see com.slytechs.sdk.common.session.state.ComponentTree
 * @see com.slytechs.sdk.common.session.state.StateTreeRenderer
 */
package com.slytechs.sdk.common.session.state;
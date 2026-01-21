/*
 * Sly Technologies Free License
 * 
 * Copyright 2025 Sly Technologies Inc.
 *
 * Licensed under the Sly Technologies Free License (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.slytechs.com/free-license-text
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * Core SDK infrastructure module providing foundational components for all Sly
 * Technologies SDK modules.
 * 
 * <h2>State Machine Framework</h2>
 * <p>
 * Composable state machine infrastructure with building blocks:
 * </p>
 * <ul>
 * <li>{@code StateMachine} - Base state machine with transition validation</li>
 * <li>{@code SessionStateMachine} - Top-level lifecycle (CREATED → RUNNING →
 * SHUTDOWN → TERMINATED)</li>
 * <li>{@code ServiceStateMachine} - Restartable services (ACTIVE ⇄ STOPPED →
 * TERMINATED)</li>
 * <li>{@code TaskStateMachine} - One-shot tasks with error handling</li>
 * <li>{@code ComponentTree} - Parent/child hierarchy with auto-termination</li>
 * <li>{@code StateWaitBarrier} - Thread synchronization on state
 * transitions</li>
 * <li>{@code ErrorPolicy} - Configurable error handling and recovery</li>
 * <li>{@code StateRecorder} - Audit trail and diagnostics</li>
 * <li>{@code TransitionScheduler} - Delayed/scheduled transitions</li>
 * <li>{@code StateTreeRenderer} - Debug visualization</li>
 * </ul>
 * 
 * <h2>Memory Management</h2>
 * <p>
 * High-performance memory pools and native memory utilities:
 * </p>
 * <ul>
 * <li>{@code MemoryPool} - Pooled fixed-size memory segments</li>
 * <li>{@code SlabAllocator} - Efficient slab allocation</li>
 * <li>{@code FixedMemory} - Bounded memory segments with headroom/tailroom</li>
 * </ul>
 * 
 * <h2>Session Management</h2>
 * <p>
 * Session lifecycle interfaces and implementations:
 * </p>
 * <ul>
 * <li>{@code Session} - Base session interface</li>
 * <li>{@code LifecycleSession} - Session with managed lifecycle</li>
 * <li>{@code SessionState} - Session state access</li>
 * </ul>
 * 
 * <h2>Utilities</h2>
 * <ul>
 * <li>{@code time} - Timestamp handling and precision</li>
 * <li>{@code format} - Formatting utilities</li>
 * <li>{@code foreign} - FFM/Panama native interop helpers</li>
 * <li>{@code settings} - Configuration framework</li>
 * <li>{@code detail} - Detail rendering for diagnostics</li>
 * <li>{@code util} - Collections, functions, and general utilities</li>
 * </ul>
 * 
 * @author Mark Bednarczyk
 * @author Sly Technologies Inc.
 */
module com.slytechs.sdk.common {
	exports com.slytechs.sdk.common.memory;
	exports com.slytechs.sdk.common.memory.pool;
	exports com.slytechs.sdk.common.format;
	exports com.slytechs.sdk.common.time;
	exports com.slytechs.sdk.common.foreign;
	exports com.slytechs.sdk.common.util;
	exports com.slytechs.sdk.common.util.function;
	exports com.slytechs.sdk.common.util.collection;
	exports com.slytechs.sdk.common.settings;
	exports com.slytechs.sdk.common.detail;
	exports com.slytechs.sdk.common.detail.render;
	exports com.slytechs.sdk.common.license;
	exports com.slytechs.sdk.common.session;
	exports com.slytechs.sdk.common.session.state;
	exports com.slytechs.sdk.common.session.state.recorder;
	exports com.slytechs.sdk.common.spec;

	requires org.slf4j;
	requires lexactivator;
}
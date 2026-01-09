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
 * Managed session infrastructure for tree-structured lifecycle coordination.
 * 
 * <p>
 * This package provides the internal API for session management with:
 * <ul>
 * <li>Tree structure - parent/child session hierarchies</li>
 * <li>Wait tracking - diagnostic information about blocking operations</li>
 * <li>Event hooks - callbacks for lifecycle events</li>
 * <li>Tree rendering - visualization of session state</li>
 * </ul>
 * </p>
 * 
 * <p>
 * This is an internal package. End users interact with the public
 * {@link com.slytechs.sdk.common.session.Session} interface. Backend
 * implementations use {@link ManagedSession} for coordination.
 * </p>
 * 
 * <p>
 * Key classes:
 * <ul>
 * <li>{@link com.slytechs.sdk.common.session.managed.ManagedSession} - Extended session interface</li>
 * <li>{@link com.slytechs.sdk.common.session.managed.ManagedState} - Extended state interface</li>
 * <li>{@link com.slytechs.sdk.common.session.managed.ManagedStateMachine} - Implementation</li>
 * <li>{@link com.slytechs.sdk.common.session.managed.WaitInfo} - Wait tracking record</li>
 * <li>{@link com.slytechs.sdk.common.session.managed.TreeRenderer} - Visualization</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Example tree output:
 * <pre>
 * PcapBackend [name=pcap, state=RUNNING→SHUTDOWN]
 * ├── PcapCapture [name=hello-capture, state=RUNNING]
 * │   └── dispatch-loop: waiting for pcap_dispatch [2.3s]
 * ├── PacketChannel [name=hello-channel, state=DRAINING]
 * │   └── queue-drain: draining (15→3) packets [1.1s]
 * └── TaskScope [name=scope-1, state=TERMINATED]
 * </pre>
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
package com.slytechs.sdk.common.session.managed;
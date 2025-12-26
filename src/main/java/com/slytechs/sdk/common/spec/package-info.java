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
 * Specification lifecycle marker interfaces.
 * 
 * <p>
 * This package defines the three-stage specification lifecycle pattern used
 * throughout the SDK for configurable components. The pattern provides a
 * consistent approach to user configuration, system validation, and runtime
 * instantiation.
 * </p>
 * 
 * <h2>The Three-Stage Lifecycle</h2>
 * <pre>
 * ┌──────────────┐      resolve()      ┌────────────────┐      build()      ┌───────────────┐
 * │     Spec     │ ──────────────────► │  ResolvedSpec  │ ────────────────► │  RuntimeSpec  │
 * │              │                     │                │                   │               │
 * │ User config  │                     │   Validated    │                   │ Instantiated  │
 * │ Mutable      │                     │   Immutable    │                   │ Stateful      │
 * │ Fluent API   │                     │ Backend-aware  │                   │ Operational   │
 * └──────────────┘                     └────────────────┘                   └───────────────┘
 * </pre>
 * 
 * <h2>Stage Descriptions</h2>
 * 
 * <h3>{@link com.slytechs.sdk.common.spec.Spec} - User Configuration</h3>
 * <p>
 * The initial stage where users configure components through fluent APIs.
 * Specifications at this stage are mutable and may have incomplete settings
 * that rely on defaults.
 * </p>
 * 
 * <h3>{@link com.slytechs.sdk.common.spec.ResolvedSpec} - Validated Configuration</h3>
 * <p>
 * The intermediate stage after SPI validation. Backend constraints are applied,
 * defaults are resolved, and the configuration is frozen. Invalid configurations
 * are rejected at this stage.
 * </p>
 * 
 * <h3>{@link com.slytechs.sdk.common.spec.RuntimeSpec} - Operational Instance</h3>
 * <p>
 * The final stage with fully instantiated resources. Pools are allocated,
 * objects are pre-bound, and the component is ready for hot-path operations.
 * </p>
 * 
 * <h2>SPI Integration Pattern</h2>
 * <p>
 * Each domain provides an SPI service that manages transitions between stages:
 * </p>
 * <pre>{@code
 * // Domain: Packet Policy
 * ResolvedPacketPolicy resolved = PacketPolicyService.resolve(packetPolicy, backendContext);
 * RuntimePacketPolicy runtime = PacketPolicyService.build(resolved, streamContext);
 * 
 * // Domain: Protocol Stack  
 * ProtocolTree tree = ProtocolStackService.resolve(stack, backendContext);
 * ProcessorTree processors = ProtocolStackService.build(tree, streamContext);
 * }</pre>
 * 
 * <h2>Domain Implementations</h2>
 * <table border="1">
 * <caption>Specification types across domains</caption>
 * <tr>
 *   <th>Domain</th>
 *   <th>Spec</th>
 *   <th>ResolvedSpec</th>
 *   <th>RuntimeSpec</th>
 * </tr>
 * <tr>
 *   <td>Packet</td>
 *   <td>{@code PacketPolicy}</td>
 *   <td>{@code ResolvedPacketPolicy}</td>
 *   <td>{@code RuntimePacketPolicy}</td>
 * </tr>
 * <tr>
 *   <td>Protocol</td>
 *   <td>{@code ProtocolStack}</td>
 *   <td>{@code ProtocolTree}</td>
 *   <td>{@code ProcessorTree}</td>
 * </tr>
 * <tr>
 *   <td>Analyzer</td>
 *   <td>{@code Analyzer}</td>
 *   <td>{@code ResolvedAnalyzer}</td>
 *   <td>{@code RuntimeAnalyzer}</td>
 * </tr>
 * <tr>
 *   <td>Processor</td>
 *   <td>{@code Processor}</td>
 *   <td>{@code ResolvedProcessor}</td>
 *   <td>{@code RuntimeProcessor}</td>
 * </tr>
 * </table>
 * 
 * <h2>Benefits of This Pattern</h2>
 * <ul>
 * <li><b>Separation of concerns:</b> User config, validation, and runtime are distinct</li>
 * <li><b>Early validation:</b> Errors caught at resolve time, not runtime</li>
 * <li><b>Backend optimization:</b> SPI can optimize for specific backends</li>
 * <li><b>Immutability:</b> Resolved specs are frozen, preventing runtime surprises</li>
 * <li><b>Resource management:</b> Clear lifecycle for pool allocation/cleanup</li>
 * <li><b>Consistency:</b> Same pattern across all configurable components</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Stage 1: User configures (Spec)
 * PacketPolicy policy = PacketPolicy.zeroCopy()
 *     .usePacketPool(new PacketPoolSettings()
 *         .capacity(100_000)
 *         .maxPacketSize(9000))
 *     .descriptorType(DescriptorTypeInfo.NET);
 * 
 * stack.setPacketPolicy(policy);
 * 
 * // Stage 2: System validates (ResolvedSpec)
 * // Performed internally when stack is applied to backend
 * // - Validates against NTAPI/DPDK/PCAP constraints
 * // - Applies backend-specific optimizations
 * // - Clamps values to backend limits
 * 
 * // Stage 3: Runtime instantiation (RuntimeSpec)
 * // Performed internally when capture starts
 * // - Allocates pools
 * // - Pre-binds packets
 * // - Returns operational policy
 * 
 * // Hot-path usage
 * Packet packet = runtimePolicy.acquire(hdr, data, descType);
 * runtimePolicy.dissect(packet, L2FrameType.ETHER);
 * // ... process ...
 * runtimePolicy.release(packet);
 * }</pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see com.slytechs.sdk.common.spec.Spec
 * @see com.slytechs.sdk.common.spec.ResolvedSpec
 * @see com.slytechs.sdk.common.spec.RuntimeSpec
 */
package com.slytechs.sdk.common.spec;
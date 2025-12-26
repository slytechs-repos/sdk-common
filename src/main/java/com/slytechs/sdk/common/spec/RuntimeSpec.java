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
package com.slytechs.sdk.common.spec;

/**
 * Marker interface for fully instantiated runtime specifications.
 * 
 * <p>
 * A {@code RuntimeSpec} represents the final stage in the three-stage
 * specification lifecycle, where all resources are allocated and ready for operation:
 * </p>
 * 
 * <pre>
 * Spec → ResolvedSpec → RuntimeSpec
 * </pre>
 * 
 * <h2>Characteristics</h2>
 * <ul>
 * <li><b>Instantiated:</b> All pools, buffers, and objects allocated</li>
 * <li><b>Stateful:</b> Maintains operational state (counters, tables, etc.)</li>
 * <li><b>Operational:</b> Ready to perform work (acquire, process, release)</li>
 * <li><b>Stream-specific:</b> Typically one instance per stream/thread</li>
 * </ul>
 * 
 * <h2>Build Process</h2>
 * <p>
 * During build, the SPI performs:
 * </p>
 * <ul>
 * <li><b>Pool allocation:</b> Create memory pools, view pools</li>
 * <li><b>Pre-binding:</b> Pre-allocate and bind packet/descriptor objects</li>
 * <li><b>State initialization:</b> Initialize tables, counters, dissectors</li>
 * <li><b>Resource registration:</b> Register with lifecycle management</li>
 * </ul>
 * 
 * <h2>SPI Integration</h2>
 * <p>
 * Building is performed by domain-specific SPI services:
 * </p>
 * <pre>{@code
 * // Building: ResolvedSpec → RuntimeSpec
 * RuntimePacketPolicy runtime = PacketPolicyService.build(resolved, streamContext);
 * 
 * // Stream context provides:
 * // - Stream ID for naming/metrics
 * // - Thread affinity hints
 * // - Shared resources (if any)
 * }</pre>
 * 
 * <h2>Lifecycle Management</h2>
 * <p>
 * Runtime specifications typically manage resources that require cleanup:
 * </p>
 * <pre>{@code
 * try (RuntimePacketPolicy policy = PacketPolicyService.build(resolved, ctx)) {
 *     while (capturing) {
 *         Packet packet = policy.acquire(hdr, data, descType);
 *         process(packet);
 *         policy.release(packet);
 *     }
 * } // Resources cleaned up
 * }</pre>
 * 
 * <h2>Domain Examples</h2>
 * <table border="1">
 * <caption>Runtime specification types by domain</caption>
 * <tr><th>Domain</th><th>Spec</th><th>ResolvedSpec</th><th>RuntimeSpec</th></tr>
 * <tr><td>Packet</td><td>PacketPolicy</td><td>ResolvedPacketPolicy</td><td>RuntimePacketPolicy</td></tr>
 * <tr><td>Protocol</td><td>ProtocolStack</td><td>ProtocolTree</td><td>ProcessorTree</td></tr>
 * <tr><td>Analyzer</td><td>Analyzer</td><td>ResolvedAnalyzer</td><td>RuntimeAnalyzer</td></tr>
 * </table>
 * 
 * <h2>Runtime Operations by Domain</h2>
 * <table border="1">
 * <caption>Primary operations provided by runtime specifications</caption>
 * <tr><th>RuntimeSpec</th><th>Primary Operations</th></tr>
 * <tr><td>RuntimePacketPolicy</td><td>acquire(), dissect(), release()</td></tr>
 * <tr><td>ProcessorTree</td><td>processPacket(), processProtocol(), clearState()</td></tr>
 * <tr><td>RuntimeAnalyzer</td><td>analyze(), tick(), flush()</td></tr>
 * </table>
 * 
 * <h2>Implementation Guidelines</h2>
 * <ul>
 * <li>Implement {@link AutoCloseable} if resources need cleanup</li>
 * <li>Design for single-threaded access (one per stream)</li>
 * <li>Avoid allocation in hot-path methods</li>
 * <li>Provide statistics/metrics access</li>
 * <li>Support state management (clear, flush) for file seeking</li>
 * </ul>
 * 
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class RuntimePacketPolicy implements RuntimeSpec, AutoCloseable {
 *     
 *     private final ViewPool<Packet> packetPool;
 *     private final PacketDissector dissector;
 *     private final PoolPolicy descriptorPolicy;
 *     private final PoolPolicy memoryPolicy;
 *     
 *     // Hot-path operations - no allocation
 *     public Packet acquire(MemorySegment hdr, MemorySegment data, int descType) {
 *         Packet packet = packetPool.allocate();
 *         // bind/copy based on policy
 *         return packet;
 *     }
 *     
 *     public void release(Packet packet) {
 *         packetPool.release(packet);
 *     }
 *     
 *     public void dissect(Packet packet, int l2Type) {
 *         dissector.dissect(packet, l2Type);
 *     }
 *     
 *     @Override
 *     public void close() {
 *         // Release pool resources
 *     }
 * }
 * }</pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see Spec
 * @see ResolvedSpec
 */
public interface RuntimeSpec {
    // Marker interface
}
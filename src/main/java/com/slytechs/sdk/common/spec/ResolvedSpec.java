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
 * Marker interface for validated and resolved specifications.
 * 
 * <p>
 * A {@code ResolvedSpec} represents the intermediate stage in the three-stage
 * specification lifecycle, after user configuration but before runtime instantiation:
 * </p>
 * 
 * <pre>
 * Spec → ResolvedSpec → RuntimeSpec
 * </pre>
 * 
 * <h2>Characteristics</h2>
 * <ul>
 * <li><b>Validated:</b> All settings verified against backend constraints</li>
 * <li><b>Immutable:</b> No further modifications allowed</li>
 * <li><b>Complete:</b> All defaults resolved, no missing values</li>
 * <li><b>Backend-aware:</b> Optimized for target backend (DPDK, NTAPI, PCAP)</li>
 * </ul>
 * 
 * <h2>Resolution Process</h2>
 * <p>
 * During resolution, the SPI performs:
 * </p>
 * <ul>
 * <li><b>Validation:</b> Check settings against backend capabilities</li>
 * <li><b>Constraint application:</b> Clamp values to backend limits</li>
 * <li><b>Optimization:</b> Select optimal strategies for target backend</li>
 * <li><b>Default resolution:</b> Fill in unspecified values</li>
 * <li><b>Conflict detection:</b> Reject incompatible setting combinations</li>
 * </ul>
 * 
 * <h2>SPI Integration</h2>
 * <p>
 * Resolution is performed by domain-specific SPI services:
 * </p>
 * <pre>{@code
 * // Resolution: Spec → ResolvedSpec
 * ResolvedPacketPolicy resolved = PacketPolicyService.resolve(packetPolicy, backendContext);
 * 
 * // Backend context provides constraints
 * // - NTAPI: freeListPool max 64K, native descriptor format
 * // - DPDK: hugepage alignment, mbuf limits
 * // - PCAP: single packet for sync dispatch
 * }</pre>
 * 
 * <h2>Backend Constraints Examples</h2>
 * <table border="1">
 * <caption>Example backend constraints applied during resolution</caption>
 * <tr><th>Backend</th><th>Constraint</th><th>Resolution Action</th></tr>
 * <tr><td>NTAPI</td><td>FreeListPool max 64K entries</td><td>Clamp capacity to 65536</td></tr>
 * <tr><td>NTAPI</td><td>Native descriptor format</td><td>Use NTAPI descriptor type</td></tr>
 * <tr><td>DPDK</td><td>Hugepage alignment</td><td>Align freeListPool size to 2MB boundary</td></tr>
 * <tr><td>PCAP</td><td>Synchronous dispatch</td><td>FreeListPool capacity 1 sufficient</td></tr>
 * <tr><td>jNetPcap</td><td>L4 cap</td><td>Limit dissector depth to L4</td></tr>
 * </table>
 * 
 * <h2>Domain Examples</h2>
 * <table border="1">
 * <caption>Resolved specification types by domain</caption>
 * <tr><th>Domain</th><th>Spec</th><th>ResolvedSpec</th><th>RuntimeSpec</th></tr>
 * <tr><td>Packet</td><td>PacketPolicy</td><td>ResolvedPacketPolicy</td><td>RuntimePacketPolicy</td></tr>
 * <tr><td>Protocol</td><td>ProtocolStack</td><td>ProtocolTree</td><td>ProcessorTree</td></tr>
 * <tr><td>Analyzer</td><td>Analyzer</td><td>ResolvedAnalyzer</td><td>RuntimeAnalyzer</td></tr>
 * </table>
 * 
 * <h2>Implementation Guidelines</h2>
 * <ul>
 * <li>Make all fields final (immutable after construction)</li>
 * <li>Provide only getters, no setters</li>
 * <li>Consider using Java records for simple resolved specs</li>
 * <li>Store the original Spec for reference if needed</li>
 * <li>Include backend context information for debugging</li>
 * </ul>
 * 
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public record ResolvedPacketPolicy(
 *     DescriptorTypeInfo descriptorType,
 *     int poolCapacity,
 *     int maxPacketSize,
 *     int dissectorDepth,
 *     PoolPolicy descriptorPolicy,
 *     PoolPolicy memoryPolicy,
 *     String backendType
 * ) implements ResolvedSpec {
 *     
 *     // Immutable by design - record provides getters
 * }
 * }</pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see Spec
 * @see RuntimeSpec
 */
public interface ResolvedSpec {
    // Marker interface
}
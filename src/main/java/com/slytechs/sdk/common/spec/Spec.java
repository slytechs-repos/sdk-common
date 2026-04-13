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
 * Marker interface for user-configurable specifications.
 * 
 * <p>
 * A {@code Spec} represents the initial, user-facing configuration stage in the
 * three-stage specification lifecycle:
 * </p>
 * 
 * <pre>
 * Spec → ResolvedSpec → RuntimeSpec
 * </pre>
 * 
 * <h2>Characteristics</h2>
 * <ul>
 * <li><b>User-facing:</b> Provides fluent API for configuration</li>
 * <li><b>Mutable:</b> Users can modify settings until resolution</li>
 * <li><b>Incomplete:</b> May have unset values that use defaults</li>
 * <li><b>Unvalidated:</b> No backend or system validation applied yet</li>
 * </ul>
 * 
 * <h2>Lifecycle</h2>
 * <p>
 * Specifications progress through three stages:
 * </p>
 * <ol>
 * <li><b>Spec (this stage):</b> User configures via fluent setters</li>
 * <li><b>ResolvedSpec:</b> System validates and optimizes for target backend</li>
 * <li><b>RuntimeSpec:</b> Fully instantiated with pools, state, ready for operation</li>
 * </ol>
 * 
 * <h2>SPI Integration</h2>
 * <p>
 * Each domain provides an SPI service that transitions specifications between stages:
 * </p>
 * <pre>{@code
 * // Resolution: Spec → ResolvedSpec
 * ResolvedPacketPolicy resolved = PacketPolicyService.resolve(packetPolicy, backendContext);
 * 
 * // Building: ResolvedSpec → RuntimeSpec  
 * RuntimePacketPolicy runtime = PacketPolicyService.build(resolved, streamContext);
 * }</pre>
 * 
 * <h2>Domain Examples</h2>
 * <table border="1">
 * <caption>Specification types by domain</caption>
 * <tr><th>Domain</th><th>Spec</th><th>ResolvedSpec</th><th>RuntimeSpec</th></tr>
 * <tr><td>Packet</td><td>PacketPolicy</td><td>ResolvedPacketPolicy</td><td>RuntimePacketPolicy</td></tr>
 * <tr><td>Protocol</td><td>ProtocolStack</td><td>ProtocolTree</td><td>ProcessorTree</td></tr>
 * <tr><td>Analyzer</td><td>Analyzer</td><td>ResolvedAnalyzer</td><td>RuntimeAnalyzer</td></tr>
 * </table>
 * 
 * <h2>Implementation Guidelines</h2>
 * <ul>
 * <li>Provide fluent setters that return {@code this} for chaining</li>
 * <li>Use sensible defaults for optional settings</li>
 * <li>Do not validate against backend constraints (that's resolution stage)</li>
 * <li>Consider extending {@link com.slytechs.sdk.common.settings.Settings} for
 *     layered configuration resolution</li>
 * </ul>
 * 
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public interface PacketPolicy extends Spec {
 *     
 *     PacketPolicy usePacketPool(PacketPoolSettings settings);
 *     PacketPolicy descriptorType(DescriptorTypeInfo type);
 *     PacketPolicy dissectorDepth(int maxLayer);
 *     
 *     // Factory methods for base strategies
 *     static PacketPolicy zeroCopy() { return new ZeroCopyPacketPolicy(); }
 *     static PacketPolicy memoryCopy() { return new MemoryCopyPacketPolicy(); }
 * }
 * }</pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see ResolvedSpec
 * @see RuntimeSpec
 */
public interface Spec {
    // Marker interface
}
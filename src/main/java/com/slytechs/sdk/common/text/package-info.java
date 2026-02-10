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
 * Protocol-aware text rendering framework for producing Wireshark-style output
 * from packet data. This package provides a declarative DSL for defining how
 * protocol headers emit structured text, a resolver-based template system for
 * cross-protocol field references, and a pluggable renderer architecture that
 * separates emission logic from output formatting.
 *
 * <h2>Architecture Overview</h2>
 *
 * <p>
 * The framework follows a three-stage pipeline: <em>define</em>,
 * <em>emit</em>, <em>render</em>. Protocol headers define their text layout
 * once using the {@link com.slytechs.sdk.common.text.DataEmitter} DSL. At
 * render time, the emitter walks the target object and calls back into a
 * {@link com.slytechs.sdk.common.text.DataEmitter.DataRender DataRender}
 * implementation (such as
 * {@link com.slytechs.sdk.common.text.renderer.TextRenderer TextRenderer})
 * which produces the final output as an immutable {@link com.slytechs.sdk.common.text.Text}
 * snapshot.
 *
 * <h2>Key Classes</h2>
 *
 * <p>
 * {@link com.slytechs.sdk.common.text.DataEmitter DataEmitter} is the central
 * builder class. Each protocol header declares a static {@code DataEmitter}
 * instance that defines its fields, sections, bitfields, macros, and
 * delegates. The emitter accumulates a lambda chain during construction that
 * is executed against a live target object at emit time.
 *
 * {@snippet :
 * private static final DataEmitter<Ip4> EMITTER;
 * static {
 *     EMITTER = new DataEmitter<>();
 *     EMITTER.macro("ip.dsfield.dscp.name", Macro.enumLookup(Map.of(0, "CS0", 46, "EF")));
 *
 *     EMITTER.section("Internet Protocol Version 4, Src: {ip.src}, Dst: {ip.dst}", sec -> sec
 *             .bitfield("{/1111 ..../} = Version: {>>}", "ip.version", 0, 4, Ip4::versionIhl)
 *             .bitfield("{/.... 1111/} = Header Length: {>> * 4} bytes ({>>})", "ip.hdr_len", 4, 4, Ip4::versionIhl)
 *             .field("Total Length", Ip4::totalLength, "ip.len")
 *             .field("Source Address", Ip4::src, "ip.src")
 *             .field("Destination Address", Ip4::dst, "ip.dst"));
 * }
 * }
 *
 * <p>
 * {@link com.slytechs.sdk.common.text.Textual Textual} is the interface that
 * protocol headers implement to participate in the rendering pipeline. It
 * provides both standalone rendering ({@code toText()}) and delegation
 * rendering ({@code emitText()}) for composing protocol layers within a
 * packet.
 *
 * {@snippet :
 * // Standalone rendering
 * Ip4 ip4 = ...;
 * Text text = ip4.toText(Detail.HIGH);
 * System.out.println(text);
 *
 * // Delegation within a Packet
 * DataEmitter<Packet> PACKET_EMITTER = new DataEmitter<Packet>()
 *         .section("Frame ...", sec -> sec.field(...))
 *         .delegate((render, pkt, contexts) -> {
 *             for (Textual hdr : pkt.headers())
 *                 hdr.emitText(render, contexts);
 *             return render;
 *         });
 * }
 *
 * <p>
 * {@link com.slytechs.sdk.common.text.Template Template} handles two-phase
 * text formatting: named references are resolved against a
 * {@link com.slytechs.sdk.common.text.DataResolver DataResolver} chain, then
 * the resolved values are passed to the format layer for final string
 * rendering. Templates support expressions ({@code {name * 8}},
 * {@code {name >> 4}}) and cross-protocol references
 * ({@code {frame.cap_len}}, {@code {ip.src}}).
 *
 * <p>
 * {@link com.slytechs.sdk.common.text.Text Text} is the immutable snapshot
 * produced by rendering. It holds no reference to the source object, making
 * it safe to retain after pooled protocol headers are recycled.
 *
 * <h2>Resolver Composition</h2>
 *
 * <p>
 * Each {@code DataEmitter} builds a {@link com.slytechs.sdk.common.text.DataResolver
 * DataResolver} from its registered field refs. Resolvers compose via
 * {@code combine()}, forming a chain where the first non-null result wins.
 * When rendering a packet, the Packet emitter's resolver is combined with
 * each delegated header's resolver, enabling templates in any protocol to
 * reference fields from any other protocol in the stack.
 *
 * <h2>Ref Linking</h2>
 *
 * <p>
 * Templates that reference field names (e.g., {@code {tcp.flags}}) trigger
 * automatic stub registration via the linker mechanism. When a template is
 * compiled within a {@code DataEmitter}, each discovered reference name is
 * passed to {@code ensureRef()}, which creates an unlinked stub if no ref
 * exists yet. When the actual field is subsequently defined with a getter,
 * {@code registerRef()} replaces the stub. This allows forward references
 * in section summaries and field-section labels.
 *
 * <h2>Rendering</h2>
 *
 * <p>
 * The {@link com.slytechs.sdk.common.text.renderer} sub-package provides
 * renderer implementations.
 * {@link com.slytechs.sdk.common.text.renderer.TextRenderer TextRenderer}
 * produces plain-text output matching Wireshark's {@code tshark} format.
 * Renderers are stateless with respect to the target type and reusable
 * across any {@code DataEmitter}. A synchronized default renderer is
 * available for casual use; high-throughput capture loops should allocate
 * per-thread renderer instances.
 *
 * <h2>Format Sub-package</h2>
 *
 * <p>
 * The {@link com.slytechs.sdk.common.text.format} sub-package provides the
 * low-level formatting engine:
 * {@link com.slytechs.sdk.common.text.format.TextFormat TextFormat} for
 * printf-style and macro-based string formatting,
 * {@link com.slytechs.sdk.common.text.format.BitFormat BitFormat} for
 * bitfield patterns with mask visualization, and
 * {@link com.slytechs.sdk.common.text.format.Macro Macro} for named
 * value-to-string transformations (enum lookups, flag lists, custom logic).
 *
 * <h2>Thread Safety</h2>
 *
 * <p>
 * {@code DataEmitter} instances are immutable after construction and safe to
 * share across threads. {@code Text} snapshots are immutable. Renderers are
 * stateful during a single {@code render()} call; the default singleton
 * renderer is synchronized for safe casual use, but dedicated per-thread
 * instances should be used in performance-critical capture loops.
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see com.slytechs.sdk.common.text.DataEmitter
 * @see com.slytechs.sdk.common.text.Textual
 * @see com.slytechs.sdk.common.text.Template
 * @see com.slytechs.sdk.common.text.Text
 * @see com.slytechs.sdk.common.text.DataResolver
 * @see com.slytechs.sdk.common.text.renderer.TextRenderer
 */
package com.slytechs.sdk.common.text;
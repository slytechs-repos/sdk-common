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
 * Renderer implementations for converting
 * {@link com.slytechs.sdk.common.text.DataEmitter} output into formatted text.
 * Renderers implement the
 * {@link com.slytechs.sdk.common.text.DataEmitter.DataRender DataRender}
 * callback interface and produce immutable {@link com.slytechs.sdk.common.text.Text}
 * snapshots that are safe to retain after pooled protocol headers are recycled.
 *
 * <h2>Architecture</h2>
 *
 * <p>
 * Renderers are decoupled from the emission logic. A
 * {@link com.slytechs.sdk.common.text.DataEmitter} defines <em>what</em> to
 * output (fields, sections, bitfields, delegates); the renderer decides
 * <em>how</em> to format it (indentation, tree characters, coloring). This
 * separation allows the same emitter definition to produce plain text, HTML,
 * JSON, or any other output format by swapping the renderer.
 *
 * <p>
 * The rendering flow is:
 *
 * {@snippet :
 * // 1. Emitter walks the target object, calling DataRender methods
 * emitter.emit(target, render, contexts);
 *
 * // 2. Renderer accumulates structured output
 * render.summary("Internet Protocol Version 4, Src: 192.168.1.1, Dst: 10.0.0.1");
 * render.push();
 * render.field("Version", "4");
 * render.field("Header Length", "20 bytes (5)");
 * render.pop();
 *
 * // 3. Renderer produces an immutable Text snapshot
 * Text result = render.build();
 * }
 *
 * <h2>TextRenderer</h2>
 *
 * <p>
 * {@link com.slytechs.sdk.common.text.renderer.TextRenderer} is the primary
 * renderer implementation, producing plain-text output that matches
 * Wireshark's {@code tshark -V} format. It renders indented, tree-structured
 * protocol dissection with section summaries, labeled fields, bitfield
 * visualizations, and metadata annotations.
 *
 * {@snippet :
 * // Standalone rendering of a single header
 * Ip4 ip4 = packet.getHeader(new Ip4());
 * Text text = ip4.toText(Detail.HIGH);
 * System.out.println(text);
 *
 * // Full packet rendering with delegation
 * Text packetText = packet.toText();
 * System.out.println(packetText);
 * }
 *
 * <p>
 * Example output:
 * <pre>
 * Internet Protocol Version 4, Src: 192.168.1.140, Dst: 174.143.213.184
 *     0100 .... = Version: 4
 *     .... 0101 = Header Length: 20 bytes (5)
 *     Differentiated Services Field: 0x00
 *         0000 00.. = Differentiated Services Codepoint: CS0 (0)
 *         .... ..00 = Explicit Congestion Notification: Not-ECT (0)
 *     Total Length: 186
 *     Identification: 0xCB5D (52061)
 *     Flags: 0x4000 (DF)
 *         0... .... .... .... = Reserved: Not Set
 *         .1.. .... .... .... = Don't Fragment: Set
 *         ..0. .... .... .... = More Fragments: Not Set
 *         ...0 0000 0000 0000 = Fragment Offset: 0
 *     Time to Live: 64
 *     Protocol: Transmission Control Protocol (TCP)
 *     Header Checksum: 0x2864
 *     [Header checksum status: Unverified]
 *     Source Address: 192.168.1.140
 *     Destination Address: 174.143.213.184
 * </pre>
 *
 * <h2>Default Singleton</h2>
 *
 * <p>
 * {@link com.slytechs.sdk.common.text.Textual} provides a default renderer
 * via {@code Textual.DEFAULT_RENDERER} for casual use. This singleton is
 * synchronized, making it safe for single-threaded debugging and interactive
 * use without allocating a renderer per call.
 *
 * {@snippet :
 * // Uses the synchronized default singleton
 * String output = packet.toText().toString();
 * }
 *
 * <h2>Per-Thread Renderers</h2>
 *
 * <p>
 * In high-throughput capture loops, the synchronized default singleton becomes
 * a contention point. Allocate a dedicated renderer per thread to avoid lock
 * contention:
 *
 * {@snippet :
 * // Per-thread renderer for high-throughput capture
 * TextRenderer renderer = new TextRenderer();
 *
 * captureLoop.forEach(packet -> {
 *     Text text = renderer.render(packet);
 *     // process text...
 * });
 * }
 *
 * <h2>AbstractRenderer</h2>
 *
 * <p>
 * {@link com.slytechs.sdk.common.text.renderer.AbstractRenderer} provides the
 * base implementation for renderers. It manages the
 * {@link com.slytechs.sdk.common.text.DataEmitter.DataRender DataRender}
 * callback state (indentation depth, line accumulation) and delegates
 * final output assembly to subclasses. Custom renderers (e.g., HTML, JSON,
 * or colorized terminal output) extend this class and override the
 * formatting methods.
 *
 * <h2>Thread Safety</h2>
 *
 * <p>
 * Renderer instances are stateful during a single {@code render()} call and
 * must not be shared across concurrent render operations. The default singleton
 * renderer synchronizes its {@code render()} method for safe casual use.
 * Dedicated per-thread instances require no synchronization. The
 * {@link com.slytechs.sdk.common.text.Text} snapshots produced by renderers
 * are immutable and safe to share freely across threads.
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see com.slytechs.sdk.common.text.DataEmitter
 * @see com.slytechs.sdk.common.text.Textual
 * @see com.slytechs.sdk.common.text.Text
 */
package com.slytechs.sdk.common.text.renderer;
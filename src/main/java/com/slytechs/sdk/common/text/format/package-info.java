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
 * Text and bit-level format string processing with a compile/apply pattern for
 * efficient reuse. This package provides the low-level formatting engine used
 * by the {@link com.slytechs.sdk.common.text.DataEmitter} DSL and
 * {@link com.slytechs.sdk.common.text.Template} resolver to produce
 * Wireshark-style protocol field output.
 *
 * <p>
 * Two formatters share a common parsing infrastructure provided by
 * {@code BaseFormatParser}:
 *
 * <p>
 * {@link com.slytechs.sdk.common.text.format.BitFormat} handles Wireshark-style
 * bit-level field display with {@code {/pattern/}} directives. It is used by
 * {@link com.slytechs.sdk.common.text.DataEmitter#bitfield
 * DataEmitter.bitfield()} to render individual flag bits, protocol version/IHL
 * nibbles, and multi-bit fields with mask visualization.
 *
 * <p>
 * {@link com.slytechs.sdk.common.text.format.TextFormat} handles SLF4J-style
 * {@code {}} sequential placeholder substitution. It is used by
 * {@link com.slytechs.sdk.common.text.Template} to compile the format portion
 * of template strings after named references have been resolved, and by
 * {@link com.slytechs.sdk.common.text.DataEmitter} for field labels and section
 * summaries.
 *
 * <p>
 * Both produce {@link com.slytechs.sdk.common.text.format.FormatPattern}
 * instances that are immutable and reusable.
 *
 * <h2>Integration with DataEmitter</h2>
 *
 * <p>
 * When a {@link com.slytechs.sdk.common.text.DataEmitter} defines fields, the
 * format layer operates at two levels. The
 * {@link com.slytechs.sdk.common.text.Template} handles named reference
 * resolution (e.g., {@code {ip.src}} becomes the resolved IP address), then
 * delegates to a {@link com.slytechs.sdk.common.text.format.FormatPattern} for
 * printf-style formatting and macro expansion on the resolved values.
 *
 * {@snippet :
 * // Template string: "Flags: {tcp.flags:0x%03X} ({tcp.flags:@tcp.flags.str})"
 * //
 * // Phase 1 — Template resolves "tcp.flags" via DataResolver → value 0x002
 * // Phase 2 — FormatPattern formats:
 * //   {:0x%03X}         → "0x002"           (printf on resolved value)
 * //   {:@tcp.flags.str} → "SYN"             (macro on resolved value)
 * //
 * // Final output: "Flags: 0x002 (SYN)"
 * }
 *
 * <p>
 * For bitfield lines, the DataEmitter compiles a
 * {@link com.slytechs.sdk.common.text.format.BitFormat} pattern that combines
 * bit visualization, auto-shift extraction, expression chains, and macros in a
 * single compiled pattern:
 *
 * {@snippet :
 * // Bitfield template: "{/.... 1111/} = Header Length: {>> * 4} bytes ({>>})"
 * //
 * // With value 0x45 (Version=4, IHL=5):
 * //   {/.... 1111/}  → ".... 0101"       (bit visualization, mask=0x0F)
 * //   {>> * 4}       → "20"              (auto-shift: 5, then multiply 4 = 20)
 * //   {>>}           → "5"               (auto-shift: 5)
 * //
 * // Final output: ".... 0101 = Header Length: 20 bytes (5)"
 * }
 *
 * <h2>Placeholder Grammar</h2>
 *
 * All content inside {@code \{...\}} is a directive. Text outside braces is
 * literal. Escaped braces {@code \\\{} and {@code \\\}} produce literal brace
 * characters.
 *
 * <h3>Placeholder Forms</h3>
 *
 * <table border="1" cellpadding="4">
 * <caption>Placeholder syntax reference</caption>
 * <tr>
 * <th>Syntax</th>
 * <th>Description</th>
 * <th>Example Output</th>
 * </tr>
 * <tr>
 * <td>{@code {}}</td>
 * <td>Primary value (BitFormat) or next sequential arg (TextFormat)</td>
 * <td>{@code 42}</td>
 * </tr>
 * <tr>
 * <td>{@code {:fmt}}</td>
 * <td>Primary value or next arg with printf format</td>
 * <td>{@code 0x00FF}</td>
 * </tr>
 * <tr>
 * <td>{@code {:@name}}</td>
 * <td>Next auto-arg with macro expansion</td>
 * <td>{@code SYN}</td>
 * </tr>
 * <tr>
 * <td>{@code {N}}</td>
 * <td>Arg at 1-based index N</td>
 * <td>{@code arg1}</td>
 * </tr>
 * <tr>
 * <td>{@code {N:fmt}}</td>
 * <td>Arg at index N with printf format</td>
 * <td>{@code 0x0800}</td>
 * </tr>
 * <tr>
 * <td>{@code {@name}}</td>
 * <td>Macro expansion on primary value</td>
 * <td>{@code Set}</td>
 * </tr>
 * <tr>
 * <td>{@code {N:@name}}</td>
 * <td>Macro expansion on arg N</td>
 * <td>{@code TCP}</td>
 * </tr>
 * <tr>
 * <td>{@code {/pattern/}}</td>
 * <td>Bit field (BitFormat only)</td>
 * <td>{@code 1010 ....}</td>
 * </tr>
 * </table>
 *
 * <p>
 * Note: the {@code {:@name}} form uses auto-arg indexing, meaning the macro
 * receives the positional argument value from the args array, not the primary
 * value. This is significant when used via
 * {@link com.slytechs.sdk.common.text.Template}, where the primary value is
 * typically null and all resolved references are passed as positional args.
 *
 * <h2>Expressions</h2>
 *
 * <p>
 * Expressions transform the value before display. They appear at the start of a
 * placeholder, before the optional {@code :fmt} format specifier. Expressions
 * always apply the current bitmask before the operation.
 *
 * <h3>Important: Expression placement rule</h3>
 *
 * <p>
 * Expressions go <b>before</b> the colon, format specifiers go <b>after</b>.
 * The colon {@code :} is strictly a separator between expression and format.
 * Writing {@code \{:>>0x%X\}} does NOT auto-shift — the {@code >>0x%X} is
 * treated as a literal printf format string (which will fail).
 *
 * {@snippet :
 * // CORRECT — expression before colon
 * "{>>:0x%X}"      // auto-shift, then format as hex
 * "{* 4:%d bytes}" // multiply by 4, then format as "N bytes"
 *
 * // WRONG — expression after colon, treated as format string
 * "{:>>0x%X}"      // ">>" is NOT parsed as an expression here
 * "{:* 4}"         // "* 4" is NOT parsed as an expression here
 * }
 *
 * <h3>Expression Operators</h3>
 *
 * <table border="1" cellpadding="4">
 * <caption>Expression operator reference</caption>
 * <tr>
 * <th>Syntax</th>
 * <th>Operation</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>{@code {>>}}</td>
 * <td>{@code (value & mask) >>> trailingZeros(mask)}</td>
 * <td>Auto-shift: masks the value and shifts right by the number of trailing
 * zeros in the current mask. This extracts the field value from its bit
 * position. If the mask has no trailing zeros (e.g. {@code 0x0F}), only the AND
 * is applied.</td>
 * </tr>
 * <tr>
 * <td>{@code {>> N}}</td>
 * <td>{@code (value & mask) >>> N}</td>
 * <td>Explicit right shift by N bits. A space is required before N to
 * distinguish from {@code {>>fmt}} (auto-shift with format).</td>
 * </tr>
 * <tr>
 * <td>{@code {<< N}}</td>
 * <td>{@code (value & mask) << N}</td>
 * <td>Left shift by N bits.</td>
 * </tr>
 * <tr>
 * <td>{@code {* N}}</td>
 * <td>{@code (value & mask) * N}</td>
 * <td>Multiply by N.</td>
 * </tr>
 * <tr>
 * <td>{@code {& N}}</td>
 * <td>{@code value & N}</td>
 * <td>AND with literal N. Supports hex: {@code {& 0xFF}}. Note: applies the
 * literal N directly, not the current flowing mask.</td>
 * </tr>
 * <tr>
 * <td>{@code {~}}</td>
 * <td>{@code ~value}</td>
 * <td>Bitwise invert.</td>
 * </tr>
 * </table>
 *
 * <h3>Chained Expressions</h3>
 *
 * <p>
 * The auto-shift operator {@code >>} can be chained with one additional
 * operator. The chain is written without spaces between the auto-shift and the
 * next operator:
 *
 * {@snippet :
 * // Auto-shift then multiply: extract IHL field, then convert to bytes
 * "{>>* 4}"       // (value & mask) >>> trailingZeros(mask), then result * 4
 *
 * // Auto-shift then left shift
 * "{>><< 3}"      // auto-shift, then left shift by 3
 *
 * // Auto-shift then AND
 * "{>>& 0xFF}"    // auto-shift, then AND with 0xFF
 * }
 *
 * <p>
 * <b>Mask handling in chains:</b> The first operation (auto-shift) applies the
 * current flowing mask via {@code value & mask}. The second operation receives
 * the already-masked-and-shifted result and operates on it directly — it does
 * <b>not</b> re-apply the mask. This ensures correct extract-then-transform
 * semantics.
 *
 * <p>
 * <b>Example trace</b> for {@code {>>* 4}} with mask {@code 0x0F} and value
 * {@code 0x45} (IPv4 IHL field):
 * <ol>
 * <li>Auto-shift: {@code (0x45 & 0x0F) >>> 0 = 5} (trailing zeros of
 * {@code 0x0F} is 0, so no shift, but mask IS applied)</li>
 * <li>Multiply: {@code 5 * 4 = 20} (the 20 bytes of IPv4 header)</li>
 * </ol>
 *
 * <p>
 * <b>Order matters:</b> {@code >>*} means "extract field value, then scale."
 * This is the correct order for protocol fields where the raw extracted value
 * needs unit conversion (IHL * 4 = bytes, fragment offset * 8 = byte offset).
 * The hypothetical reverse ({@code *>>}) would multiply the raw masked value
 * before extracting, which is rarely meaningful for protocol fields.
 *
 * <h3>Expressions with Format Specifiers</h3>
 *
 * <p>
 * Expressions combine with format specifiers using the colon separator:
 *
 * {@snippet :
 * "{>>:0x%X}"        // auto-shift, format as hex
 * "{>> 4:0x%04X}"    // shift right 4, format as 4-digit hex
 * "{* 4:%d bytes}"   // multiply by 4, format as "N bytes"
 * "{>>* 4:%d}"       // chain: auto-shift then multiply, format as decimal
 * "{& 0xFF:0x%02X}"  // AND mask, format as 2-digit hex
 * }
 *
 * <h3>Expressions with Arg Index</h3>
 *
 * <p>
 * Expressions can operate on a specific argument instead of the primary value:
 *
 * {@snippet :
 * "{1 >> 4:0x%X}"    // arg 1, shift right 4, format hex
 * "{2 * 8}"          // arg 2, multiply by 8
 * }
 *
 * <h2>Mask Flow</h2>
 *
 * <p>
 * Within a format line, each segment receives the mask set by the previous
 * segment and may replace it. This is the "mask flow" mechanism that enables
 * multiple bit fields on a single line:
 *
 * <ul>
 * <li>Initial mask is {@code ~0L} (all bits set, 0xFFFFFFFFFFFFFFFF)</li>
 * <li>A bit field {@code {/pattern/}} computes its mask from the active bit
 * positions and replaces the flowing mask</li>
 * <li>Subsequent value refs, expressions, and macros use this flowing mask</li>
 * <li>Another bit field replaces the mask again</li>
 * <li>Mask resets to {@code ~0L} at each new line in multi-line patterns</li>
 * </ul>
 *
 * {@snippet :
 * // Two fields on one line — each bitfield sets a different mask:
 * "{/1111 ..../} = {>>:0x%X} | {/.... 1111/} = {>>:0x%X}"
 *
 * // With value 0xA5:
 * // {/1111 ..../} → mask becomes 0xF0, renders "1010 ...."
 * // {>>:0x%X}    → uses mask 0xF0: (0xA5 & 0xF0) >>> 4 = 0xA → "0xA"
 * // {/.... 1111/} → mask becomes 0x0F, renders ".... 0101"
 * // {>>:0x%X}    → uses mask 0x0F: (0xA5 & 0x0F) >>> 0 = 0x5 → "0x5"
 * //
 * // Output: "1010 .... = 0xA | .... 0101 = 0x5"
 * }
 *
 * <p>
 * The hidden bitfield modifier ({@code hide}) participates in mask flow without
 * producing output. This is useful when you need to set the mask for subsequent
 * placeholders without displaying the bit pattern:
 *
 * {@snippet :
 * "{/.... 1111/hide}{:0x%X}"  // sets mask to 0x0F, displays "0xB" for 0xAB
 * }
 *
 * <h2>Bit Field Patterns (BitFormat only)</h2>
 *
 * <p>
 * Bit fields use {@code {/pattern/modifiers}} syntax. The pattern is a sequence
 * of characters where each non-space character represents one bit, MSB first
 * (most significant bit is leftmost, matching network byte order and RFC
 * conventions):
 *
 * <table border="1" cellpadding="4">
 * <caption>Pattern characters</caption>
 * <tr>
 * <th>Character</th>
 * <th>Meaning</th>
 * </tr>
 * <tr>
 * <td>{@code 0, 1}</td>
 * <td>Active bit position (contributes to mask)</td>
 * </tr>
 * <tr>
 * <td>{@code .}</td>
 * <td>Masked/inactive bit position (not in mask)</td>
 * </tr>
 * <tr>
 * <td>(space)</td>
 * <td>Visual separator, no bit consumed</td>
 * </tr>
 * <tr>
 * <td>{@code +}</td>
 * <td>Skip: consumes a bit position but produces no output</td>
 * </tr>
 * <tr>
 * <td>{@code =}</td>
 * <td>Align: consumes a bit position, outputs a space</td>
 * </tr>
 * <tr>
 * <td>Any other letter</td>
 * <td>Custom on-character for that bit position (e.g., flag letters RECUAPRSF
 * for TCP flags)</td>
 * </tr>
 * </table>
 *
 * <h3>Bit Field Modifiers</h3>
 *
 * <p>
 * Modifiers follow the closing {@code /} and precede {@code \}}:
 *
 * <table border="1" cellpadding="4">
 * <caption>Bitfield modifiers</caption>
 * <tr>
 * <th>Modifier</th>
 * <th>Effect</th>
 * </tr>
 * <tr>
 * <td>{@code 0=char}</td>
 * <td>Override the "off" (bit is 0) display character</td>
 * </tr>
 * <tr>
 * <td>{@code 1=char}</td>
 * <td>Override the "on" (bit is 1) display character</td>
 * </tr>
 * <tr>
 * <td>{@code .=char}</td>
 * <td>Override the "masked" (not in mask) display character</td>
 * </tr>
 * <tr>
 * <td>{@code on=char}</td>
 * <td>Same as {@code 1=char}</td>
 * </tr>
 * <tr>
 * <td>{@code off=char}</td>
 * <td>Same as {@code 0=char}</td>
 * </tr>
 * <tr>
 * <td>{@code mask=char}</td>
 * <td>Same as {@code .=char}</td>
 * </tr>
 * <tr>
 * <td>{@code hide}</td>
 * <td>Sets the mask but produces no visible output</td>
 * </tr>
 * <tr>
 * <td>{@code ~}</td>
 * <td>Inverts the computed mask (bitwise NOT)</td>
 * </tr>
 * </table>
 *
 * {@snippet :
 * // Standard bit display
 * "{/1111 ..../}"              // → "1010 ...." for 0xA0
 *
 * // Custom flag characters (TCP flags as single letters)
 * "{/RECUAPRSF/0=.}"          // → "....AP..." for ACK+PSH
 *
 * // Hidden bitfield (sets mask for subsequent placeholders, no output)
 * "{/.... 1111/hide}{:0x%X}"  // → "0xB" for 0xAB
 *
 * // Inverted mask: pattern specifies high nibble, ~ inverts to low nibble
 * "{/1111 ..../~}"            // mask becomes ~0xF0 = 0x0F
 * }
 *
 * <h2>Macros</h2>
 *
 * <p>
 * Macros are named functions referenced as {@code {@name}} in format strings.
 * They receive the current value (as {@link java.lang.Object}) and the current
 * flowing mask, and produce a display string. The
 * {@link com.slytechs.sdk.common.text.format.Macro} interface supports any
 * value type — numeric, byte arrays, strings, or domain objects.
 *
 * <p>
 * In the context of the {@link com.slytechs.sdk.common.text.DataEmitter} DSL,
 * macros are registered on the emitter instance and automatically propagated to
 * the format layer during pattern compilation. Common macro factories include:
 *
 * {@snippet :
 * // Enum lookup — maps integer values to display strings
 * emitter.macro("ip.dsfield.dscp.name", Macro.enumLookup(Map.of(
 * 		0, "CS0", 8, "CS1", 46, "EF")));
 *
 * // Flag list — produces comma-separated active flag names
 * emitter.macro("tcp.flags.str", Macro.flagList(
 * 		new long[] {
 * 				0x100,
 * 				0x080,
 * 				0x040,
 * 				0x020,
 * 				0x010,
 * 				0x008,
 * 				0x004,
 * 				0x002,
 * 				0x001
 * 		},
 * 		new String[] {
 * 				"NS",
 * 				"CWR",
 * 				"ECE",
 * 				"URG",
 * 				"ACK",
 * 				"PSH",
 * 				"RST",
 * 				"SYN",
 * 				"FIN"
 * 		},
 * 		"none"));
 * }
 *
 * <h3>Built-in Macros</h3>
 *
 * <p>
 * The following macros are auto-imported by all formatter instances via
 * {@link com.slytechs.sdk.common.text.format.BuiltinMacros}:
 *
 * <table border="1" cellpadding="4">
 * <caption>Built-in macros</caption>
 * <tr>
 * <th>Macro</th>
 * <th>Input Type</th>
 * <th>Output Example</th>
 * </tr>
 * <tr>
 * <td>{@code @hex}</td>
 * <td>Number</td>
 * <td>{@code 0x1A}</td>
 * </tr>
 * <tr>
 * <td>{@code @hex02}</td>
 * <td>Number</td>
 * <td>{@code 0x1A}</td>
 * </tr>
 * <tr>
 * <td>{@code @hex04}</td>
 * <td>Number</td>
 * <td>{@code 0x001A}</td>
 * </tr>
 * <tr>
 * <td>{@code @hex08}</td>
 * <td>Number</td>
 * <td>{@code 0x0000001A}</td>
 * </tr>
 * <tr>
 * <td>{@code @hex16}</td>
 * <td>Number</td>
 * <td>{@code 0x000000000000001A}</td>
 * </tr>
 * <tr>
 * <td>{@code @dec}</td>
 * <td>Number</td>
 * <td>{@code 26}</td>
 * </tr>
 * <tr>
 * <td>{@code @dec,}</td>
 * <td>Number</td>
 * <td>{@code 1,000,000}</td>
 * </tr>
 * <tr>
 * <td>{@code @oct}</td>
 * <td>Number</td>
 * <td>{@code 032}</td>
 * </tr>
 * <tr>
 * <td>{@code @bin}</td>
 * <td>Number</td>
 * <td>{@code 11010}</td>
 * </tr>
 * <tr>
 * <td>{@code @set}</td>
 * <td>Number</td>
 * <td>{@code Set} / {@code Not Set} (mask-aware)</td>
 * </tr>
 * <tr>
 * <td>{@code @bool}</td>
 * <td>Number</td>
 * <td>{@code True} / {@code False} (mask-aware)</td>
 * </tr>
 * <tr>
 * <td>{@code @enabled}</td>
 * <td>Number</td>
 * <td>{@code Enabled} / {@code Disabled} (mask-aware)</td>
 * </tr>
 * <tr>
 * <td>{@code @yes}</td>
 * <td>Number</td>
 * <td>{@code Yes} / {@code No} (mask-aware)</td>
 * </tr>
 * <tr>
 * <td>{@code @mac}</td>
 * <td>byte[6]</td>
 * <td>{@code 00:1d:60:b3:01:84}</td>
 * </tr>
 * <tr>
 * <td>{@code @mac.oui}</td>
 * <td>byte[3+]</td>
 * <td>{@code 00:1d:60}</td>
 * </tr>
 * <tr>
 * <td>{@code @ipv4}</td>
 * <td>Number or byte[4]</td>
 * <td>{@code 192.168.1.1}</td>
 * </tr>
 * <tr>
 * <td>{@code @ipv6}</td>
 * <td>byte[16]</td>
 * <td>{@code 2001:db8::1} (:: compressed)</td>
 * </tr>
 * <tr>
 * <td>{@code @bytes}</td>
 * <td>byte[]</td>
 * <td>{@code 00:26:62:2f:47:87}</td>
 * </tr>
 * <tr>
 * <td>{@code @bytes.}</td>
 * <td>byte[]</td>
 * <td>{@code 00.26.62.2f.47.87}</td>
 * </tr>
 * <tr>
 * <td>{@code @bytes-}</td>
 * <td>byte[]</td>
 * <td>{@code 00-26-62-2f-47-87}</td>
 * </tr>
 * </table>
 *
 * <h3>Macro Sharing</h3>
 *
 * <p>
 * A {@link com.slytechs.sdk.common.text.format.TextFormat} can be constructed
 * with a {@link com.slytechs.sdk.common.text.format.BitFormat} to share the
 * same macro registry. Macros added to either formatter are visible to both:
 *
 * {@snippet :
 * BitFormat bitFmt = new BitFormat();
 * TextFormat textFmt = new TextFormat(bitFmt); // shares macros
 *
 * bitFmt.setMacro("proto", Macro.ofLong(v -> switch ((int) v) {
 * case 6 -> "TCP";
 * case 17 -> "UDP";
 * default -> "Unknown";
 * }));
 *
 * // Both can now use @proto
 * bitFmt.format("{@proto}", 6);          // → "TCP"
 * textFmt.compile("{@proto}").format(6); // → "TCP"
 * }
 *
 * <h2>TextFormat vs BitFormat</h2>
 *
 * <table border="1" cellpadding="4">
 * <caption>Behavioral differences between the two formatters</caption>
 * <tr>
 * <th>Feature</th>
 * <th>TextFormat</th>
 * <th>BitFormat</th>
 * </tr>
 * <tr>
 * <td>Empty {@code {}}</td>
 * <td>Next sequential arg (auto-increment index)</td>
 * <td>Primary value</td>
 * </tr>
 * <tr>
 * <td>{@code {:@name}}</td>
 * <td>Macro on next auto-arg (positional)</td>
 * <td>Macro on primary value</td>
 * </tr>
 * <tr>
 * <td>Bit fields {@code {/pattern/}}</td>
 * <td>Not supported</td>
 * <td>Supported</td>
 * </tr>
 * <tr>
 * <td>Primary value</td>
 * <td>Any Object (for macros)</td>
 * <td>Typically long (for bit operations)</td>
 * </tr>
 * <tr>
 * <td>Typical use</td>
 * <td>Summary lines, labels, field-section headers</td>
 * <td>Bit-level field display, flags</td>
 * </tr>
 * </table>
 *
 * <h2>Compile/Apply Pattern</h2>
 *
 * <p>
 * For repeated formatting (e.g., protocol headers applied to thousands of
 * packets), compile the pattern once and reuse it:
 *
 * {@snippet :
 * // Compile once (during protocol header class initialization)
 * FormatPattern.Line ihlLine = bitFmt.compileLine(
 * 		"{/.... 1111/} = Header Length: {>>* 4} bytes ({>>})");
 *
 * // Apply many times (per packet)
 * String output = ihlLine.format(packetByte0); // fast, no parsing
 * }
 *
 * <h2>Null Safety</h2>
 *
 * <p>
 * {@link com.slytechs.sdk.common.text.format.FormatValue} guards against null
 * values when invoking macros. If the resolved value is null (e.g., from an
 * unlinked ref stub or missing context), the macro produces the string
 * {@code "null"} rather than throwing a {@link java.lang.NullPointerException}.
 * This ensures rendering degrades gracefully when optional protocol context is
 * unavailable.
 *
 * <h2>Thread Safety</h2>
 *
 * <p>
 * {@link com.slytechs.sdk.common.text.format.FormatPattern} and
 * {@link com.slytechs.sdk.common.text.format.FormatPattern.Line} instances are
 * immutable and safe to share across threads. The formatter instances
 * ({@link com.slytechs.sdk.common.text.format.BitFormat} and
 * {@link com.slytechs.sdk.common.text.format.TextFormat}) hold a mutable macro
 * registry. Register all macros during initialization before sharing compiled
 * patterns. Each thread should use its own formatter instance if macros are
 * being registered concurrently.
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see com.slytechs.sdk.common.text.DataEmitter
 * @see com.slytechs.sdk.common.text.Template
 */
package com.slytechs.sdk.common.text.format;
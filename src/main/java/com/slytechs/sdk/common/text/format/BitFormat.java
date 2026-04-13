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
package com.slytechs.sdk.common.text.format;

import java.util.HashMap;
import java.util.Map;

/**
 * Formatter for bit-level field display, producing Wireshark-style output for
 * protocol headers. Supports a format string DSL with bit patterns, macros,
 * value/argument references, and per-line character overrides.
 *
 * <p>
 * Use as a factory to compile format strings into reusable {@link FormatPattern}
 * instances, or call {@link #format} directly for one-off formatting.
 *
 * {@snippet :
 * BitFormat fmt = new BitFormat();
 *
 * // Built-in macros are available by default: @set, @hex, @hex04, etc.
 * FormatPattern pattern = fmt.compile("""
 *     Flags: {:0x%03X}
 *         {/1111 ..../} = High nibble: {@set}
 *         {/.... 1111/} = Low nibble: {@set}
 *     """);
 *
 * String output = pattern.format(0xA5);
 * }
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see TextFormat
 * @see FormatPattern
 */
public final class BitFormat {

	private final char on;
	private final char off;
	private final char masked;
	private final Map<String, Macro> macros;

	public BitFormat() {
		this('1', '0', '.');
	}

	public BitFormat(char on, char off, char masked) {
		this.on = on;
		this.off = off;
		this.masked = masked;
		this.macros = new HashMap<>(BuiltinMacros.defaults());
	}

	public void setMacro(String name, Macro macro) {
		macros.put(name, macro);
	}

	public void setMacro(String name, Macro.LongMacro macro) {
		macros.put(name, Macro.ofLong(macro));
	}

	public void setMacro(String name, Macro.BytesMacro macro) {
		macros.put(name, Macro.ofBytes(macro));
	}

	public FormatPattern compile(String format) {
		return newParser().parse(format);
	}

	public FormatPattern.Line compileLine(String format) {
		return newParser().parseLine(format);
	}

	public String format(String format, long value, Object... args) {
		return compile(format).format(value, args);
	}

	public String formatMasked(String format, long value, long mask, Object... args) {
		return compile(format).formatMasked(value, mask, args);
	}

	public String[] formatLines(String format, long value, Object... args) {
		return compile(format).formatLines(value, args);
	}

	Map<String, Macro> macros() {
		return macros;
	}

	private BitFormatParser newParser() {
		return new BitFormatParser(on, off, masked, macros);
	}
}
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
 * Text formatter with {@code {}} placeholder substitution. Supports sequential
 * auto-incrementing arguments, explicit argument references, printf-style
 * formatting, and macros.
 *
 * <p>
 * Similar to SLF4J's {@code {}} syntax but with additional features like
 * format specifiers, macros, and support for both primitive and {@code byte[]}
 * values.
 *
 * {@snippet :
 * TextFormat fmt = new TextFormat();
 *
 * // Sequential placeholders (auto-incrementing)
 * String out = fmt.format("Frame {} of {} ({} bytes)", 1, 100, 1518);
 * // → "Frame 1 of 100 (1518 bytes)"
 *
 * // With format specifiers
 * String hex = fmt.format("Src: {:0x%04X} Dst: {:0x%04X}", 0x0800, 0x0806);
 * // → "Src: 0x0800 Dst: 0x0806"
 *
 * // With macros
 * String mac = fmt.format("MAC: {@mac}", new byte[]{0x00,0x1d,0x60,(byte)0xb3,0x01,(byte)0x84});
 * // → "MAC: 00:1d:60:b3:01:84"
 *
 * // Compile for reuse
 * FormatPattern pattern = fmt.compile("Frame {} ({} bytes)");
 * String out1 = pattern.format(0, 1, 1518);  // value=0 unused, args sequential
 * }
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see BitFormat
 * @see FormatPattern
 */
public final class TextFormat {

	private final Map<String, Macro> macros;

	public TextFormat() {
		this.macros = new HashMap<>(BuiltinMacros.defaults());
	}

	/**
	 * Creates a TextFormat that shares macros with a BitFormat instance.
	 *
	 * @param sharedWith the BitFormat to share macros with
	 */
	public TextFormat(BitFormat sharedWith) {
		this.macros = sharedWith.macros();
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

	/**
	 * Formats with sequential arguments. The value parameter is not used for
	 * sequential formatting — use args for all values.
	 *
	 * @param format the format string
	 * @param args   sequential arguments for {@code {}} placeholders
	 * @return formatted output
	 */
	public String format(String format, Object... args) {
		return compile(format).format(0, args);
	}

	/**
	 * Formats with a byte[] primary value and sequential arguments.
	 *
	 * @param format the format string
	 * @param value  the primary byte[] value (for {@code {@macro}} refs)
	 * @param args   sequential arguments for {@code {}} placeholders
	 * @return formatted output
	 */
	public String format(String format, byte[] value, Object... args) {
		return compile(format).format(value, args);
	}

	private BaseFormatParser newParser() {
		return new BaseFormatParser(macros, true);
	}
}
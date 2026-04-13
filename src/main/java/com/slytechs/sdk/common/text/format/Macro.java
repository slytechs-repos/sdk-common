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

import java.util.Map;
import java.util.StringJoiner;

/**
 * Functional interface for macro expansion within format strings. Macros are
 * referenced as {@code {@name}} in format strings and produce a formatted
 * string from the current value.
 *
 * <p>
 * Named macros pair a suffix name with a macro function for auto-registration
 * by the DSL. When a {@link Named} macro with suffix {@code "name"} is
 * attached to field {@code "ip.dsfield.dscp"}, the DSL registers it as
 * {@code @ip.dsfield.dscp.name}, accessible in format strings as
 * {@code {$name}}.
 *
 * {@snippet :
 * // Enum lookup macro — auto-registers as @ip.dsfield.dscp.name
 * Macro.named("name", Map.of(0, "CS0", 8, "CS1", 46, "EF"))
 *
 * // Enum with custom fallback
 * Macro.named("name", Map.of(6, "TCP", 17, "UDP"), "Proto(%d)")
 *
 * // Flag list macro — auto-registers as @ip.flags.list
 * Macro.named("list", Macro.flagList(Map.of(0x4, "DF", 0x2, "MF")))
 *
 * // Custom logic macro
 * Macro.named("name", Macro.ofLong(v -> v < 5 ? "Short" : "Long"))
 * }
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
@FunctionalInterface
public interface Macro {

	String accept(Object value, long mask);

	default String accept(Object value) {
		return accept(value, ~0L);
	}

	/**
	 * A macro paired with a suffix name for auto-registration by the DSL. The
	 * DSL prepends the field name to the suffix when registering.
	 *
	 * {@snippet :
	 * // Attached to field "ip.dsfield.dscp", registers as @ip.dsfield.dscp.name
	 * Macro.named("name", Map.of(0, "CS0", 8, "CS1", 46, "EF"))
	 * }
	 *
	 * @param suffix the macro suffix (e.g., "name", "list", "desc")
	 * @param macro  the macro function
	 */
	record Named(String suffix, Macro macro) {}

	// --- Named factories ---

	/**
	 * Creates a named macro from an existing macro function.
	 *
	 * @param suffix the macro suffix for DSL registration
	 * @param macro  the macro function
	 * @return a named macro
	 */
	static Named named(String suffix, Macro macro) {
		return new Named(suffix, macro);
	}

	/**
	 * Creates a named enum lookup macro from a map. Looks up the integer value
	 * in the map and returns the string. Unknown values produce
	 * {@code "Unknown (N)"}.
	 *
	 * {@snippet :
	 * Macro.named("name", Map.of(0, "CS0", 8, "CS1", 46, "EF"))
	 * }
	 *
	 * @param suffix the macro suffix
	 * @param map    value-to-name mapping
	 * @return a named macro
	 */
	static Named named(String suffix, Map<Integer, String> map) {
		return named(suffix, map, "Unknown (%d)");
	}

	/**
	 * Creates a named enum lookup macro with a custom fallback format.
	 *
	 * @param suffix   the macro suffix
	 * @param map      value-to-name mapping
	 * @param fallback printf format for unmapped values, receives the int value
	 * @return a named macro
	 */
	static Named named(String suffix, Map<Integer, String> map, String fallback) {
		return new Named(suffix, enumLookup(map, fallback));
	}

	// --- Macro factories ---

	static Macro of(SimpleMacro macro) {
		return (v, m) -> macro.accept(v);
	}

	static Macro ofMasked(Macro macro) {
		return macro;
	}

	static Macro ofLong(LongMacro macro) {
		return (v, m) -> macro.accept(toLong(v), m);
	}

	static Macro ofLong(SimpleLongMacro macro) {
		return (v, m) -> macro.accept(toLong(v));
	}

	static Macro ofBytes(BytesMacro macro) {
		return (v, m) -> macro.accept(toBytes(v), m);
	}

	static Macro ofBytes(SimpleBytesMacro macro) {
		return (v, m) -> macro.accept(toBytes(v));
	}

	/**
	 * Creates an enum lookup macro from a map. Looks up the integer value and
	 * returns the mapped name, or a fallback string for unmapped values.
	 *
	 * @param map      value-to-name mapping
	 * @param fallback printf format for unmapped values
	 * @return a macro
	 */
	static Macro enumLookup(Map<Integer, String> map, String fallback) {
		return (v, m) -> {
			long raw = ((Number) v).longValue();
			int key;
			if (m != 0 && m != ~0L) {
				key = (int) ((raw & m) >>> Long.numberOfTrailingZeros(m));
			} else {
				key = (int) (raw & m);
			}
			String name = map.get(key);
			return name != null ? name : fallback.formatted(key);
		};
	}

	/**
	 * Creates an enum lookup macro with default "Unknown (N)" fallback.
	 *
	 * @param map value-to-name mapping
	 * @return a macro
	 */
	static Macro enumLookup(Map<Integer, String> map) {
		return enumLookup(map, "Unknown (%d)");
	}

	/**
	 * Creates a flag list macro that produces a comma-separated list of flag
	 * names for all set bits. The map keys are bitmasks for each flag.
	 *
	 * {@snippet :
	 * Macro flags = Macro.flagList(Map.of(
	 *     0x4, "DF",
	 *     0x2, "MF",
	 *     0x1, "RSV"
	 * ));
	 * flags.accept(0x6, ~0L); // → "DF, MF"
	 * flags.accept(0x0, ~0L); // → "None"
	 * }
	 *
	 * @param map bitmask-to-name mapping
	 * @return a macro
	 */
	static Macro flagList(Map<Integer, String> map) {
		return flagList(map, "None");
	}

	/**
	 * Creates a flag list macro with a custom empty string.
	 *
	 * @param map   bitmask-to-name mapping
	 * @param empty string to return when no flags are set
	 * @return a macro
	 */
	static Macro flagList(Map<Integer, String> map, String empty) {
		return (v, m) -> {
			long val = ((Number) v).longValue();
			StringJoiner sj = new StringJoiner(", ");
			map.forEach((mask, name) -> {
				if ((val & mask) != 0)
					sj.add(name);
			});
			return sj.length() == 0 ? empty : sj.toString();
		};
	}

	/**
	 * Creates a flag list macro from parallel arrays for deterministic ordering.
	 *
	 * @param masks bitmask values
	 * @param names flag names in display order
	 * @return a macro
	 */
	static Macro flagList(long[] masks, String[] names) {
		return flagList(masks, names, "None");
	}

	/**
	 * Creates a flag list macro from parallel arrays with custom empty string.
	 *
	 * @param masks bitmask values
	 * @param names flag names in display order
	 * @param empty string to return when no flags are set
	 * @return a macro
	 */
	static Macro flagList(long[] masks, String[] names, String empty) {
		return (v, m) -> {
			long val = ((Number) v).longValue();
			StringJoiner sj = new StringJoiner(", ");
			for (int i = 0; i < masks.length; i++) {
				if ((val & masks[i]) != 0)
					sj.add(names[i]);
			}
			return sj.length() == 0 ? empty : sj.toString();
		};
	}

	// --- Utility ---

	private static long toLong(Object v) {
		if (v instanceof Number n) return n.longValue();
		throw new IllegalArgumentException("expected Number, got " + v.getClass().getSimpleName());
	}

	private static byte[] toBytes(Object v) {
		if (v instanceof byte[] b) return b;
		throw new IllegalArgumentException("expected byte[], got " + v.getClass().getSimpleName());
	}

	// --- Functional interfaces ---

	@FunctionalInterface
	interface SimpleMacro {
		String accept(Object value);
	}

	@FunctionalInterface
	interface LongMacro {
		String accept(long value, long mask);
	}

	@FunctionalInterface
	interface SimpleLongMacro {
		String accept(long value);
	}

	@FunctionalInterface
	interface BytesMacro {
		String accept(byte[] value, long mask);
	}

	@FunctionalInterface
	interface SimpleBytesMacro {
		String accept(byte[] value);
	}
}
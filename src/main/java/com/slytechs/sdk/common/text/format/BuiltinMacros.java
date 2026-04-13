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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Built-in macros that are auto-imported by all formatters.
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public final class BuiltinMacros {

	private static final Map<String, Macro> MACROS;

	static {
		Map<String, Macro> m = new HashMap<>();

		m.put("hex", Macro.ofLong(v -> "0x%X".formatted(v)));
		m.put("hex02", Macro.ofLong(v -> "0x%02X".formatted(v)));
		m.put("hex04", Macro.ofLong(v -> "0x%04X".formatted(v)));
		m.put("hex08", Macro.ofLong(v -> "0x%08X".formatted(v)));
		m.put("hex16", Macro.ofLong(v -> "0x%016X".formatted(v)));

		m.put("dec", Macro.ofLong(v -> Long.toString(v)));
		m.put("dec,", Macro.ofLong(v -> "%,d".formatted(v)));

		m.put("oct", Macro.ofLong(v -> "0%o".formatted(v)));
		m.put("bin", Macro.ofLong(v -> Long.toBinaryString(v)));

		m.put("set", Macro.ofLong((v, mask) -> (v & mask) == 0 ? "Not Set" : "Set"));
		m.put("bool", Macro.ofLong((v, mask) -> (v & mask) == 0 ? "False" : "True"));
		m.put("enabled", Macro.ofLong((v, mask) -> (v & mask) == 0 ? "Disabled" : "Enabled"));
		m.put("yes", Macro.ofLong((v, mask) -> (v & mask) == 0 ? "No" : "Yes"));

		m.put("mac", Macro.ofBytes(v -> formatMac(v, 0, Math.min(v.length, 6))));
		m.put("mac.oui", Macro.ofBytes(v -> formatMac(v, 0, Math.min(v.length, 3))));

		m.put("ipv4", (v, mask) -> {
			if (v instanceof byte[] b) {
				if (b.length < 4)
					throw new IllegalArgumentException("IPv4 requires 4 bytes");
				return "%d.%d.%d.%d".formatted(b[0] & 0xFF, b[1] & 0xFF, b[2] & 0xFF, b[3] & 0xFF);
			}
			if (v instanceof Number n) {
				long ip = n.longValue();
				return "%d.%d.%d.%d".formatted(
						(ip >> 24) & 0xFF, (ip >> 16) & 0xFF,
						(ip >> 8) & 0xFF, ip & 0xFF);
			}
			return v.toString();
		});

		m.put("ipv6", Macro.ofBytes(BuiltinMacros::formatIpv6));

		m.put("bytes", Macro.ofBytes(v -> formatBytes(v, ":")));
		m.put("bytes.", Macro.ofBytes(v -> formatBytes(v, ".")));
		m.put("bytes-", Macro.ofBytes(v -> formatBytes(v, "-")));

		MACROS = Collections.unmodifiableMap(m);
	}

	static Map<String, Macro> defaults() {
		return MACROS;
	}

	private static String formatMac(byte[] data, int offset, int len) {
		StringBuilder sb = new StringBuilder();
		for (int i = offset; i < offset + len; i++) {
			if (sb.length() > 0) sb.append(':');
			sb.append("%02x".formatted(data[i] & 0xFF));
		}
		return sb.toString();
	}

	private static String formatBytes(byte[] data, String separator) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < data.length; i++) {
			if (i > 0) sb.append(separator);
			sb.append("%02x".formatted(data[i] & 0xFF));
		}
		return sb.toString();
	}

	private static String formatIpv6(byte[] data) {
		if (data.length < 16)
			throw new IllegalArgumentException("IPv6 requires 16 bytes");

		int[] groups = new int[8];
		for (int i = 0; i < 8; i++)
			groups[i] = ((data[i * 2] & 0xFF) << 8) | (data[i * 2 + 1] & 0xFF);

		int bestStart = -1, bestLen = 0;
		int runStart = -1, runLen = 0;

		for (int i = 0; i < 8; i++) {
			if (groups[i] == 0) {
				if (runStart < 0) runStart = i;
				runLen++;
				if (runLen > bestLen) {
					bestStart = runStart;
					bestLen = runLen;
				}
			} else {
				runStart = -1;
				runLen = 0;
			}
		}

		if (bestLen < 2) bestStart = -1;

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 8; i++) {
			if (bestStart >= 0 && i == bestStart) {
				sb.append("::");
				i += bestLen - 1;
				continue;
			}
			if (i > 0 && sb.length() > 0 && sb.charAt(sb.length() - 1) != ':')
				sb.append(':');
			sb.append(Integer.toHexString(groups[i]));
		}

		return sb.toString();
	}

	private BuiltinMacros() {
	}
}
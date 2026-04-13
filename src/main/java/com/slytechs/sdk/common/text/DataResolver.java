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
package com.slytechs.sdk.common.text;

/**
 * Resolves named references to values. Implementations are composable via
 * {@link #combine(DataResolver)} and travel through {@link DataEmitter.DataContexts}
 * as a context type, enabling protocol-level resolver chaining.
 *
 * {@snippet :
 * // Each protocol builds its own resolver
 * DataResolver packetResolver = name -> switch (name) {
 *     case "frame.number" -> pkt.frameNumber();
 *     case "frame.cap_len" -> pkt.captureLength();
 *     default -> null;
 * };
 *
 * DataResolver ip4Resolver = name -> switch (name) {
 *     case "ip.src" -> ip4.src();
 *     case "ip.dst" -> ip4.dst();
 *     default -> null;
 * };
 *
 * // Compose — first non-null wins
 * DataResolver combined = packetResolver.combine(ip4Resolver);
 * combined.resolve("frame.number"); // → from packet
 * combined.resolve("ip.src");       // → from ip4
 * }
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
@FunctionalInterface
public interface DataResolver {

	DataResolver EMPTY = name -> null;

	/**
	 * Resolves a named reference to a value.
	 *
	 * @param name the reference name (e.g., "frame.cap_len", "ip.src")
	 * @return the resolved value, or {@code null} if not found
	 */
	Object resolve(String name);

	/**
	 * Combines this resolver with another. This resolver is checked first; if
	 * it returns {@code null}, the other resolver is consulted.
	 *
	 * @param other the fallback resolver
	 * @return a combined resolver
	 */
	default DataResolver combine(DataResolver other) {
		if (other == null || other == EMPTY)
			return this;

		return name -> {
			Object val = resolve(name);
			return val != null ? val : other.resolve(name);
		};
	}
}
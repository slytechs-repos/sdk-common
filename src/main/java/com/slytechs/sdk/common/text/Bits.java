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

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.util.function.Function;

import com.slytechs.sdk.common.memory.MemoryHandle;

/**
 * 
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public record Bits(long offset, long length) {

	public record BitsLookup(MemoryLayout layout, Function<String[], Bits> other) {
		public Bits bits(String... path) {

			/*
			 * Memory layout has only byte level granularity, also other meta fields are not
			 * represented by MemoryLayout. Other allows an inline lookup for custom fields
			 * such as ip.version & ip.hdr_hlen which are 4 bits each.
			 */
			Bits otherBits = other.apply(path);
			if (otherBits != null)
				return otherBits;

			PathElement[] elements = MemoryHandle.parsePath(path);

			long byteOffset = layout.byteOffset(elements);
			long byteSize = layout.select(elements).byteSize();

			return new Bits(byteOffset * 8, byteSize * 8);

		}
	}

	public static final Bits UNKNOWN = new Bits(0, Long.MAX_VALUE);

	public boolean isUnknown() {
		return this == UNKNOWN;
	}

	public boolean isKnown() {
		return this != UNKNOWN;
	}

	public static Bits fromLayout(MemoryLayout layout, String... path) {
		PathElement[] elements = MemoryHandle.parsePath(path);

		long byteOffset = layout.byteOffset(elements);
		long byteSize = layout.select(elements).byteSize();

		return new Bits(byteOffset * 8, byteSize * 8);
	}

}

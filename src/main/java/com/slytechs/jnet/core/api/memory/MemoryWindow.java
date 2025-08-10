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
package com.slytechs.jnet.core.api.memory;

/**
 * Interface defining memory window and bounds management capabilities.
 * 
 * <p>
 * MemoryWindow provides fine-grained control over memory positioning and data
 * bounds, enabling efficient memory slicing and windowing operations without
 * copying data. This interface is particularly valuable in scenarios requiring
 * precise memory region control, such as protocol parsing, buffer management,
 * and zero-copy data processing.
 * </p>
 * 
 * <h2>Memory Layout Concepts</h2>
 * <p>
 * A memory window defines two sets of bounds within the underlying memory:
 * </p>
 * <ul>
 * <li><strong>Memory Bounds:</strong> The total addressable region
 * [memoryOffset, memoryEnd)</li>
 * <li><strong>Data Bounds:</strong> The currently active data region
 * [memoryDataOffset, memoryDataEnd)</li>
 * </ul>
 * 
 * <pre>{@code
 * Memory Layout:
 * |---------- Total Memory Capacity ----------|
 * |  unused  |---- Data Region ----|  unused  |
 *            ↑                     ↑
 *      memoryDataOffset      memoryDataEnd
 * }</pre>
 * 
 * <h2>Common Usage Patterns</h2>
 * <ul>
 * <li><strong>Buffer Management:</strong> Track consumed/remaining data in
 * streaming scenarios</li>
 * <li><strong>Protocol Parsing:</strong> Define header boundaries within larger
 * packets</li>
 * <li><strong>Zero-Copy Operations:</strong> Create views without memory
 * allocation</li>
 * <li><strong>Memory Pooling:</strong> Reuse memory regions with different
 * active bounds</li>
 * </ul>
 * 
 * <h2>Bounds Relationship</h2>
 * <p>
 * The following invariants are maintained:
 * </p>
 * <ul>
 * <li>{@code memoryOffset() ≤ memoryDataOffset() ≤ memoryDataEnd() ≤ memoryEnd()}</li>
 * <li>{@code memoryCapacity() = memoryEnd() - memoryOffset()}</li>
 * <li>{@code memoryDataLength() = memoryDataEnd() - memoryDataOffset()}</li>
 * </ul>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 * @see MemoryView for content access
 * @see MemoryRef for lifecycle management
 */
public interface MemoryWindow {

	/**
	 * Returns the total capacity of the memory region in bytes.
	 * 
	 * <p>
	 * The capacity represents the maximum addressable space within this memory
	 * window, calculated as {@code memoryEnd() - memoryOffset()}. This value
	 * remains constant for the lifetime of the memory window and defines the upper
	 * bound for all data operations.
	 * </p>
	 * 
	 * <p>
	 * <strong>Note:</strong> This refers to a single memory segment's capacity. For
	 * chained memory structures, use chain-specific methods to get total capacity.
	 * </p>
	 * 
	 * @return the total capacity in bytes (always ≥ 0)
	 * @throws IllegalStateException if this memory window is closed or invalid
	 * 
	 * @see #memoryOffset() for the start boundary
	 * @see #memoryEnd() for the end boundary
	 */
	long memoryCapacity();

	/**
	 * Returns the absolute ending offset of the memory region.
	 * 
	 * <p>
	 * This represents the first byte position beyond the addressable memory region.
	 * The valid memory addresses range from {@link #memoryOffset()} (inclusive) to
	 * {@code memoryEnd()} (exclusive), following standard Java array indexing
	 * conventions.
	 * </p>
	 * 
	 * @return the absolute ending offset (exclusive bound)
	 * @throws IllegalStateException if this memory window is closed or invalid
	 * 
	 * @see #memoryOffset() for the starting offset
	 * @see #memoryCapacity() for the total span
	 */
	long memoryEnd();

	/**
	 * Returns the absolute starting offset of the memory region.
	 * 
	 * <p>
	 * This represents the first addressable byte position within the memory window.
	 * All relative offsets and data operations are calculated relative to this base
	 * offset.
	 * </p>
	 * 
	 * @return the absolute starting offset (inclusive bound)
	 * @throws IllegalStateException if this memory window is closed or invalid
	 * 
	 * @see #memoryEnd() for the ending offset
	 * @see #memoryCapacity() for the total span
	 */
	long memoryOffset();

	/**
	 * Returns the absolute ending offset of the current data region.
	 * 
	 * <p>
	 * This represents the first byte position beyond the currently active data,
	 * similar to a buffer's limit. The data region can be adjusted independently of
	 * the memory bounds, enabling flexible windowing operations.
	 * </p>
	 * 
	 * <p>
	 * The default implementation returns {@link #memoryEnd()}, meaning the entire
	 * memory region is considered active data. Implementations may override this to
	 * provide more sophisticated data boundary management.
	 * </p>
	 * 
	 * @return the absolute data ending offset (exclusive bound)
	 * @throws IllegalStateException if this memory window is closed or invalid
	 * 
	 * @see #memoryDataOffset() for the data start
	 * @see #memoryDataLength() for the data span
	 */
	default long memoryDataEnd() {
		return memoryEnd();
	}

	/**
	 * Returns the length of the current data region in bytes.
	 * 
	 * <p>
	 * Calculates the span of currently active data as
	 * {@code memoryDataEnd() - memoryDataOffset()}. This represents the amount of
	 * meaningful data within the memory window, analogous to a buffer's remaining
	 * bytes.
	 * </p>
	 * 
	 * @return the current data length in bytes (always ≥ 0)
	 * @throws IllegalStateException if this memory window is closed or invalid
	 * 
	 * @see #memoryDataOffset() for the data start
	 * @see #memoryDataEnd() for the data end
	 */
	default long memoryDataLength() {
		return memoryDataEnd() - memoryDataOffset();
	}

	/**
	 * Returns the absolute starting offset of the current data region.
	 * 
	 * <p>
	 * This represents the first byte of currently active data within the memory
	 * window, similar to a buffer's position. The data region can be a subset of
	 * the total memory region, enabling flexible data windowing.
	 * </p>
	 * 
	 * <p>
	 * The default implementation returns {@link #memoryOffset()}, meaning data
	 * starts at the beginning of the memory region. Implementations may override
	 * this to provide more sophisticated data boundary management.
	 * </p>
	 * 
	 * @return the absolute data starting offset (inclusive bound)
	 * @throws IllegalStateException if this memory window is closed or invalid
	 * 
	 * @see #memoryDataEnd() for the data end
	 * @see #memoryDataLength() for the data span
	 */
	default long memoryDataOffset() {
		return memoryOffset();
	}

	/**
	 * Returns the data offset within the memory segment containing the specified
	 * chain offset.
	 * 
	 * <p>
	 * For chained memory structures, this method translates a chain-relative offset
	 * to the corresponding data offset within the specific memory segment that
	 * contains that position. This is essential for maintaining data boundary
	 * semantics across chain boundaries.
	 * </p>
	 * 
	 * <p>
	 * The default implementation simply returns {@link #memoryDataOffset()}, which
	 * is appropriate for non-chained memory or when chain offset mapping is not
	 * required.
	 * </p>
	 * 
	 * @param chainOffset the offset across the entire chain
	 * @return the local data offset within the containing memory segment
	 * @throws IllegalArgumentException if chainOffset is out of bounds
	 * @throws IllegalStateException    if this memory window is closed or invalid
	 * 
	 * @see MemoryView#asMemorySegmentAt(long) for accessing the containing segment
	 */
	default long memoryDataOffsetAt(long chainOffset) {
		return memoryDataOffset();
	}
}
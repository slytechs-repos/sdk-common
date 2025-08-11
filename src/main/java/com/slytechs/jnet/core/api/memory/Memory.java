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

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;

/**
 * Comprehensive interface for memory abstractions, supporting both single
 * segments and chained structures.
 * 
 * <p>
 * Memory combines all memory management capabilities into a unified interface,
 * providing a complete abstraction for high-performance memory operations. This
 * interface is designed for scenarios requiring full memory control, including
 * content access, bounds management, reference counting, and chain operations.
 * </p>
 * 
 * <h2>Design Philosophy</h2>
 * <p>
 * Memory interface follows a zero-allocation design principle, enabling
 * high-performance operations without garbage collection overhead. Key design
 * elements include:
 * </p>
 * <ul>
 * <li><strong>Unified Interface:</strong> Combines view, window, and reference
 * capabilities</li>
 * <li><strong>Chain Support:</strong> Native support for linked memory
 * structures</li>
 * <li><strong>Pool Integration:</strong> Designed for memory pool
 * compatibility</li>
 * <li><strong>Reference Safety:</strong> Automatic cleanup via reference
 * counting</li>
 * </ul>
 * 
 * <h2>Common Usage Scenarios</h2>
 * <ul>
 * <li><strong>Network Packet Processing:</strong> Zero-copy packet parsing and
 * manipulation</li>
 * <li><strong>Streaming Data:</strong> Efficient buffer management for
 * continuous data flows</li>
 * <li><strong>Protocol Implementation:</strong> Header parsing with automatic
 * bounds checking</li>
 * <li><strong>Memory Pooling:</strong> High-performance allocation/deallocation
 * cycles</li>
 * </ul>
 * 
 * <h2>Chain Operations</h2>
 * <p>
 * Memory chains enable handling of fragmented data without copying:
 * </p>
 * 
 * <pre>{@code
 * // Process entire chain
 * long totalSize = memory.chainDataLength();
 * for (Memory segment = memory; segment != null; segment = segment.nextMemory()) {
 * 	ByteBuffer buffer = segment.asByteBuffer();
 * 	processSegment(buffer);
 * }
 * 
 * // Random access across chain
 * MemorySegment specificSegment = memory.asMemorySegmentAt(offset);
 * }</pre>
 * 
 * <h2>Implementation Guidelines</h2>
 * <p>
 * Implementations should prioritize:
 * </p>
 * <ul>
 * <li>Thread-safe reference counting operations</li>
 * <li>Efficient chain traversal algorithms</li>
 * <li>Minimal memory allocation during operations</li>
 * <li>Clear error handling for boundary violations</li>
 * </ul>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 * @see MemoryView for view-only operations
 * @see MemoryWindow for bounds management
 * @see MemoryRef for lifecycle control
 */
public interface Memory extends MemoryView, MemoryWindow, MemoryRef {

	/**
	 * Static utility method to check if a MemorySegment represents null memory.
	 * 
	 * <p>
	 * This convenience method provides consistent null checking logic across the
	 * memory API, testing both for Java null references and native NULL pointers
	 * (address = 0). This is essential when working with native memory or
	 * memory-mapped regions.
	 * </p>
	 * 
	 * @param segment the MemorySegment to test, may be {@code null}
	 * @return {@code true} if segment is {@code null} or has address 0
	 */
	static boolean isNull(MemorySegment segment) {
		return segment == null || segment.address() == 0;
	}

	/**
	 * Creates a Memory wrapper for the specified MemorySegment starting at the
	 * given offset.
	 * 
	 * <p>
	 * This factory method creates a Memory view that spans from the specified
	 * offset to the end of the MemorySegment. The resulting Memory object provides
	 * full access to the segment's capabilities through the Memory interface.
	 * </p>
	 * 
	 * @param segment the backing MemorySegment
	 * @param offset  the starting offset within the segment
	 * @return a new Memory object wrapping the specified region
	 * @throws NullPointerException     if segment is {@code null}
	 * @throws IllegalArgumentException if offset is invalid
	 */
	static Memory of(MemorySegment segment, long offset) {
		return new MemoryWrapper(segment, offset, segment.byteSize() - offset);
	}

	/**
	 * Creates a Memory wrapper for the specified MemorySegment region.
	 * 
	 * <p>
	 * This factory method creates a Memory view with explicit bounds, enabling
	 * precise control over the accessible memory region. The capacity parameter
	 * defines the size of the accessible region starting from the offset.
	 * </p>
	 * 
	 * @param segment  the backing MemorySegment
	 * @param offset   the starting offset within the segment
	 * @param capacity the size of the accessible region
	 * @return a new Memory object wrapping the specified region
	 * @throws NullPointerException     if segment is {@code null}
	 * @throws IllegalArgumentException if offset or capacity is invalid
	 */
	static Memory of(MemorySegment segment, long offset, long capacity) {
		return new MemoryWrapper(segment, offset, offset + capacity);
	}

	/**
	 * Returns the total capacity across all segments in the memory chain.
	 * 
	 * <p>
	 * This method traverses the entire memory chain, summing the individual
	 * capacities of each segment to provide the total addressable space. Unlike
	 * {@link #memoryCapacity()}, which returns the capacity of a single segment,
	 * this method considers the entire chain structure.
	 * </p>
	 * 
	 * <p>
	 * <strong>Performance Note:</strong> This operation requires chain traversal
	 * and has O(n) complexity where n is the number of segments in the chain. For
	 * frequently accessed values, consider caching the result.
	 * </p>
	 * 
	 * @return the total capacity across all chain segments
	 * @throws IllegalStateException if this memory is closed or invalid
	 * 
	 * @see #memoryCapacity() for single segment capacity
	 * @see #chainDataLength() for total usable data across the chain
	 */
	default long chainCapacity() {
		long total = 0;
		for (Memory mem = this; mem != null; mem = mem.nextMemory()) {
			total += mem.memoryCapacity();
		}
		return total;
	}

	/**
	 * Returns the total usable data length across all segments in the memory chain.
	 * 
	 * <p>
	 * This method traverses the entire memory chain, summing the data lengths of
	 * each segment to provide the total amount of usable data. This is the primary
	 * method for determining the addressable range when using chain-relative
	 * offsets in methods like {@link #asMemorySegmentAt(long)}.
	 * </p>
	 * 
	 * <p>
	 * <strong>Data vs Capacity:</strong> While {@link #chainCapacity()} returns
	 * total allocated space, this method returns only the currently active data
	 * regions, which may be smaller due to data bounds management.
	 * </p>
	 * 
	 * @return the total usable data length across all chain segments
	 * @throws IllegalStateException if this memory is closed or invalid
	 * 
	 * @see #chainCapacity() for total allocated space
	 * @see #memoryDataLength() for single segment data length
	 */
	default long chainDataLength() {
		long total = 0;
		for (Memory mem = this; mem != null; mem = mem.nextMemory()) {
			total += mem.memoryDataLength();
		}
		return total;
	}

	/**
	 * Returns the number of Memory objects in the memory chain.
	 * 
	 * <p>
	 * This method counts the total number of linked Memory segments, providing
	 * useful information for debugging, performance analysis, and memory allocation
	 * strategies. For single (non-chained) memory objects, this returns 1.
	 * </p>
	 * 
	 * <p>
	 * <strong>Performance Note:</strong> This operation requires chain traversal
	 * and has O(n) complexity. The default implementation returns 1, which is
	 * correct for non-chained memory implementations.
	 * </p>
	 * 
	 * @return the number of Memory objects in the chain (≥ 1)
	 * @throws IllegalStateException if this memory is closed or invalid
	 * 
	 * @see #chainDataLength() for total data across the chain
	 * @see #chainCapacity() for total capacity across the chain
	 */
	default int chainMemoryCount() {
		return 1;
	}

	/**
	 * Returns a ByteBuffer view of the memory's current data region.
	 * 
	 * <p>
	 * Creates a ByteBuffer that provides access to the memory's usable data,
	 * positioned at {@link #memoryDataOffset()} with limit set to correspond to
	 * {@link #memoryDataEnd()}. The returned buffer shares the same underlying
	 * memory as this Memory object.
	 * </p>
	 * 
	 * <p>
	 * The default implementation creates a slice of the underlying MemorySegment
	 * covering the data region. Implementations may override this for custom
	 * ByteBuffer creation logic.
	 * </p>
	 * 
	 * @return a ByteBuffer positioned at the start of usable data
	 * @throws UnsupportedOperationException if ByteBuffer access is not supported
	 * @throws IllegalStateException         if this memory is closed or invalid
	 */
	@Override
	default ByteBuffer asByteBuffer() {
		return asMemorySegment().asSlice(memoryOffset(), memoryCapacity()).asByteBuffer();
	}

	/**
	 * Returns the MemorySegment containing the specified offset within the memory
	 * chain.
	 * 
	 * <p>
	 * This method provides efficient random access to any position within a chained
	 * memory structure. The offset is relative to the start of the entire chain,
	 * and this method automatically locates and returns the appropriate segment.
	 * </p>
	 * 
	 * <p>
	 * The default implementation handles both single and chained memory cases,
	 * using {@link #seekMemory(long)} for navigation in chained structures.
	 * </p>
	 * 
	 * @param chainOffset the byte offset from the start of the entire chain
	 *                    (0-based)
	 * @return the MemorySegment containing the specified offset
	 * @throws IllegalArgumentException if chainOffset is negative or exceeds the
	 *                                  total chain data length
	 * @throws IllegalStateException    if this memory is closed or invalid
	 */
	@Override
	default MemorySegment asMemorySegmentAt(long chainOffset) {
		if (chainOffset < 0 || chainOffset >= chainDataLength())
			throw new IllegalArgumentException("chainOffset out of bounds: " + chainOffset);

		if (chainMemoryCount() == 1) {
			return asMemorySegment();
		}
		Memory sought = seekMemory(chainOffset);
		return sought.asMemorySegment();
	}

	/**
	 * Checks if this memory represents a null or invalid memory reference.
	 * 
	 * <p>
	 * Uses the static {@link #isNull(MemorySegment)} method to check the underlying
	 * MemorySegment for null conditions.
	 * </p>
	 * 
	 * @return {@code true} if this represents a null or invalid memory reference
	 */
	@Override
	default boolean isNull() {
		return isNull(asMemorySegment());
	}

	/**
	 * Returns the total capacity of the memory region in bytes.
	 * 
	 * <p>
	 * The default implementation calculates capacity as the difference between
	 * memory end and offset: {@code memoryEnd() - memoryOffset()}.
	 * </p>
	 * 
	 * @return the total capacity in bytes (always ≥ 0)
	 * @throws IllegalStateException if this memory is closed or invalid
	 */
	@Override
	default long memoryCapacity() {
		return memoryEnd() - memoryOffset();
	}

}
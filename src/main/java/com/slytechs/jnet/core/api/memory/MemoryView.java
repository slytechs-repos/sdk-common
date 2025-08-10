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
 * Core interface providing read-only view capabilities for memory access.
 * 
 * <p>MemoryView defines the fundamental contract for accessing memory content through
 * various representations (ByteBuffer, MemorySegment) and provides navigation capabilities
 * for chained memory structures. This interface is designed for scenarios where only
 * read access to memory content is required, without lifecycle or bounds management.</p>
 * 
 * <h2>Key Capabilities</h2>
 * <ul>
 *   <li><strong>Content Access:</strong> Convert memory to ByteBuffer or MemorySegment views</li>
 *   <li><strong>Chain Navigation:</strong> Traverse linked memory segments efficiently</li>
 *   <li><strong>Chain Addressing:</strong> Access specific segments by chain-relative offsets</li>
 *   <li><strong>State Inspection:</strong> Check for null pointers and zero-sized segments</li>
 * </ul>
 * 
 * <h2>Chain Navigation Pattern</h2>
 * <p>Memory chains are linked lists of memory segments, commonly used in network packet
 * processing where data may be fragmented across multiple buffers:</p>
 * <pre>{@code
 * // Navigate through all segments in a chain
 * for (Memory current = memoryView; current != null; current = current.nextMemory()) {
 *     ByteBuffer buffer = current.asByteBuffer();
 *     // Process buffer content...
 * }
 * 
 * // Access specific offset across the entire chain
 * MemorySegment segment = memoryView.asMemorySegmentAt(1500); // May span segments
 * }</pre>
 * 
 * <h2>Performance Characteristics</h2>
 * <ul>
 *   <li>Zero-allocation navigation through chains</li>
 *   <li>Lazy evaluation of chain operations</li>
 *   <li>Efficient random access via seekMemory()</li>
 * </ul>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 * @see MemoryWindow for bounds and positioning
 * @see MemoryRef for lifecycle management
 */
public interface MemoryView {

    /**
     * Returns a ByteBuffer view of the memory's current data region.
     * 
     * <p>Creates a ByteBuffer that provides access to the memory's usable data,
     * positioned at {@link MemoryWindow#memoryDataOffset()} with limit set to 
     * {@link MemoryWindow#memoryDataEnd()}. The returned buffer shares the same 
     * underlying memory as this view.</p>
     * 
     * <p><strong>Warning:</strong> For chained memory structures, this method may only
     * provide access to the current segment. Use chain navigation to access all segments.</p>
     * 
     * @return a ByteBuffer positioned at the start of usable data
     * @throws UnsupportedOperationException if ByteBuffer access is not supported
     *         (e.g., for certain chained memory implementations)
     * @throws IllegalStateException if this memory view is closed or invalid
     * 
     * @see #asMemorySegment() for MemorySegment access
     */
    ByteBuffer asByteBuffer();

    /**
     * Returns a MemorySegment view of the memory's current data region.
     * 
     * <p>Creates a MemorySegment that provides direct access to the memory's usable data,
     * starting at {@link MemoryWindow#memoryDataOffset()} with size 
     * {@link MemoryWindow#memoryDataLength()}. The returned segment shares the same 
     * underlying memory as this view.</p>
     * 
     * @return a MemorySegment representing the usable data region
     * @throws IllegalStateException if this memory view is closed or invalid
     * 
     * @see #asByteBuffer() for ByteBuffer access
     * @see #asMemorySegmentAt(long) for chain-relative access
     */
    MemorySegment asMemorySegment();

    /**
     * Returns the MemorySegment containing the specified offset within the memory chain.
     * 
     * <p>This method provides efficient random access to any position within a chained
     * memory structure. The offset is relative to the start of the entire chain, and
     * this method automatically locates and returns the appropriate segment.</p>
     * 
     * <p><strong>Example:</strong> In a chain with segments of sizes [1000, 500, 800],
     * calling {@code asMemorySegmentAt(1200)} would return the MemorySegment for the
     * third segment, as offset 1200 falls within that segment (1000 + 500 = 1500 > 1200).</p>
     * 
     * @param chainOffset the byte offset from the start of the entire chain (0-based)
     * @return the MemorySegment containing the specified offset
     * @throws IllegalArgumentException if chainOffset is negative or exceeds the
     *         total chain data length
     * @throws IllegalStateException if this memory view is closed or invalid
     * 
     * @see #seekMemory(long) for navigation to the containing memory object
     */
    MemorySegment asMemorySegmentAt(long chainOffset);

    /**
     * Returns the next Memory object in the chain, or {@code null} if this is the last segment.
     * 
     * <p>Memory chains form a singly-linked list structure, with each segment pointing
     * to the next. This method provides the primary navigation mechanism for traversing
     * the entire chain.</p>
     * 
     * <p><strong>Usage Pattern:</strong></p>
     * <pre>{@code
     * Memory current = startOfChain;
     * while (current != null) {
     *     // Process current segment
     *     processSegment(current);
     *     current = current.nextMemory(); // Move to next
     * }
     * }</pre>
     * 
     * @return the next Memory in the chain, or {@code null} if none exists
     * @throws IllegalStateException if this memory view is closed or invalid
     * 
     * @see #hasNextMemory() to check for next segment existence
     * @see #seekMemory(long) for direct navigation by offset
     */
    Memory nextMemory();

    /**
     * Checks whether there is a next Memory object in the chain.
     * 
     * <p>This method provides a convenient way to test for chain continuation
     * without modifying navigation state, useful in conditional processing scenarios.</p>
     * 
     * @return {@code true} if {@link #nextMemory()} would return a non-null value
     * @throws IllegalStateException if this memory view is closed or invalid
     * 
     * @see #nextMemory() to get the actual next segment
     */
    boolean hasNextMemory();

    /**
     * Locates and returns the Memory object containing the specified chain offset.
     * 
     * <p>This method performs efficient navigation through the memory chain to find
     * the segment that contains the specified byte offset. Unlike {@link #asMemorySegmentAt(long)},
     * this returns the Memory wrapper object rather than the raw MemorySegment.</p>
     * 
     * <p>The method uses optimized traversal algorithms to minimize navigation overhead,
     * particularly for sequential access patterns common in streaming scenarios.</p>
     * 
     * @param chainOffset the byte offset from the start of the entire chain (0-based)
     * @return the Memory object containing the specified offset
     * @throws IllegalArgumentException if chainOffset is negative or exceeds the
     *         total chain data length
     * @throws IllegalStateException if this memory view is closed or invalid
     * 
     * @see #asMemorySegmentAt(long) for direct MemorySegment access
     */
    Memory seekMemory(long chainOffset);

    /**
     * Checks if this memory view represents a null or invalid memory reference.
     * 
     * <p>A memory view is considered null if:</p>
     * <ul>
     *   <li>The underlying MemorySegment is {@code null}</li>
     *   <li>The MemorySegment's address is 0 (native NULL pointer)</li>
     *   <li>The view has been explicitly closed or invalidated</li>
     * </ul>
     * 
     * <p>This method is essential for safe memory operations, particularly when
     * working with native memory or optional memory references.</p>
     * 
     * @return {@code true} if this represents a null or invalid memory reference
     * 
     * @see #isPointer() for checking zero-sized memory
     */
    boolean isNull();

    /**
     * Checks if this memory represents a pointer (zero-sized memory reference).
     * 
     * <p>A memory view is considered a pointer if the underlying MemorySegment
     * has zero byte size, indicating it represents a memory address rather than
     * an actual data region. This is common when dealing with native pointers
     * or memory-mapped structures.</p>
     * 
     * <p>Pointer memory typically cannot be directly accessed for data operations
     * but may be used for address arithmetic or as references to other memory.</p>
     * 
     * @return {@code true} if this represents a pointer (zero-sized memory)
     * @throws IllegalStateException if this memory view is closed or invalid
     * 
     * @see #isNull() for checking null memory references
     */
    boolean isPointer();
}
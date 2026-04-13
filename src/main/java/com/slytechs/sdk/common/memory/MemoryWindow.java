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
package com.slytechs.sdk.common.memory;

/**
 * Defines memory region boundaries and chain-wide projections for efficient memory management.
 * 
 * <p>
 * MemoryWindow provides a unified view of memory organization through segment boundaries
 * and data regions. Each memory segment has immutable boundaries (byteOffset/byteSize) that
 * define the total addressable space, and mutable data boundaries (start/end) that define
 * the currently active data within that space.
 * </p>
 * 
 * <h2>Memory Layout</h2>
 * <pre>{@code
 * Segment: [  headroom  ][    active data    ][  tailroom  ]
 *          ↑             ↑                    ↑            ↑
 *     byteOffset      start()              end()    byteOffset+byteSize
 * 
 * Invariants:
 * • byteOffset ≤ start() ≤ end() ≤ byteOffset + byteSize
 * • headroom = start() - byteOffset
 * • tailroom = (byteOffset + byteSize) - end()
 * • length = end() - start()
 * }</pre>
 * 
 * <h2>Design Principles</h2>
 * <ul>
 * <li><strong>Immutable boundaries:</strong> byteOffset() and byteSize() are set at
 * construction and never change</li>
 * <li><strong>Mutable data region:</strong> start() and end() can be adjusted to
 * expand or contract the active data</li>
 * <li><strong>DPDK-style operations:</strong> Headroom allows prepending headers,
 * tailroom allows appending data</li>
 * <li><strong>Chain projections:</strong> Total methods aggregate across linked segments</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * 
 * <h3>Protocol Header Operations</h3>
 * <pre>{@code
 * // Prepend VLAN tag using headroom
 * if (memory.headroom() >= 4) {
 *     memory.start(memory.start() - 4);
 *     // Write VLAN tag at new start position
 * }
 * 
 * // Consume processed data
 * long consumed = processData(memory);
 * memory.start(memory.start() + consumed);
 * }</pre>
 * 
 * <h3>Buffer Management</h3>
 * <pre>{@code
 * // Append data using tailroom
 * if (memory.tailroom() >= dataSize) {
 *     writeData(memory.segment(), memory.end(), data);
 *     memory.end(memory.end() + dataSize);
 * }
 * }</pre>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 */
public interface MemoryWindow {

    // ==================== Data Boundaries (Mutable) ====================

    /**
     * Returns the starting offset of active data within this segment.
     * 
     * <p>
     * This mutable boundary represents the first byte of currently active data.
     * The value must satisfy: {@code byteOffset() ≤ start() ≤ end()}.
     * </p>
     * 
     * <p>
     * The default implementation returns {@link #byteOffset()}, meaning active
     * data starts at the beginning of the segment.
     * </p>
     * 
     * @return the absolute offset where active data begins (inclusive)
     * @see #start(long) to adjust the starting offset
     * @see #headroom() for available space before active data
     */
    default long start() {
        return byteOffset();
    }

    /**
     * Sets the starting offset of active data within this segment.
     * 
     * <p>
     * Adjusts the beginning of the active data region, typically used when
     * consuming data from the front or prepending headers using headroom.
     * The new offset must satisfy: {@code byteOffset() ≤ newStart ≤ end()}.
     * </p>
     * 
     * @param newStart the new starting offset for active data
     * @return the new start offset (same as parameter for chaining)
     * @throws IllegalArgumentException if newStart violates segment boundaries
     */
    long start(long newStart);

    /**
     * Returns the ending offset of active data within this segment.
     * 
     * <p>
     * This mutable boundary represents the first byte position beyond the currently
     * active data (exclusive). The value must satisfy: {@code start() ≤ end() ≤ byteOffset() + byteSize()}.
     * </p>
     * 
     * <p>
     * The default implementation returns {@code byteOffset() + byteSize()}, meaning
     * active data extends to the end of the segment.
     * </p>
     * 
     * @return the absolute offset where active data ends (exclusive)
     * @see #end(long) to adjust the ending offset
     * @see #tailroom() for available space after active data
     */
    default long end() {
        return byteOffset() + byteSize();
    }

    /**
     * Sets the ending offset of active data within this segment.
     * 
     * <p>
     * Adjusts the end of the active data region, typically used when appending data
     * or truncating the active region. The new offset must satisfy:
     * {@code start() ≤ newEnd ≤ byteOffset() + byteSize()}.
     * </p>
     * 
     * @param newEnd the new ending offset for active data
     * @return the new end offset (same as parameter for chaining)
     * @throws IllegalArgumentException if newEnd violates segment boundaries
     */
    long end(long newEnd);

    /**
     * Returns the length of active data in this segment.
     * 
     * <p>
     * Calculates the span of currently active data as {@code end() - start()}.
     * This represents the actual amount of meaningful data within the memory segment.
     * </p>
     * 
     * @return the active data length in bytes (always ≥ 0)
     * @see #totalLength() for chain-wide active data
     */
    default long length() {
        return end() - start();
    }

    // ==================== Segment Boundaries (Immutable) ====================

    /**
     * Returns the starting offset of this memory segment.
     * 
     * <p>
     * This immutable boundary represents the first addressable byte position within
     * the memory segment. Matches the semantics of {@link java.lang.foreign.MemorySegment#byteOffset()}.
     * Once set (typically in the constructor), this value remains constant.
     * </p>
     * 
     * @return the absolute starting offset of the segment (inclusive)
     */
    long byteOffset();

    /**
     * Returns the total size of this memory segment in bytes.
     * 
     * <p>
     * This immutable value represents the maximum addressable space within this
     * memory segment. Matches the semantics of {@link java.lang.foreign.MemorySegment#byteSize()}.
     * The segment boundaries span from byteOffset() to byteOffset() + byteSize().
     * </p>
     * 
     * @return the total segment size in bytes (always ≥ 0)
     * @see #totalSize() for chain-wide capacity
     */
    long byteSize();

    // ==================== Expansion Space ====================

    /**
     * Returns the available space before active data (headroom).
     * 
     * <p>
     * Headroom represents unused space at the beginning of the segment that can be
     * utilized for prepending headers or expanding the active region backward.
     * Calculated as {@code start() - byteOffset()}.
     * </p>
     * 
     * <p>
     * This pattern is commonly used in network protocol processing where headers
     * need to be added to existing packet data without copying.
     * </p>
     * 
     * @return available space before active data in bytes (always ≥ 0)
     * @see #tailroom() for space after active data
     */
    default long headroom() {
        return start() - byteOffset();
    }

    /**
     * Returns the available space after active data (tailroom).
     * 
     * <p>
     * Tailroom represents unused space at the end of the segment that can be
     * utilized for appending data or expanding the active region forward.
     * Calculated as {@code (byteOffset() + byteSize()) - end()}.
     * </p>
     * 
     * @return available space after active data in bytes (always ≥ 0)
     * @see #headroom() for space before active data
     */
    default long tailroom() {
        return (byteOffset() + byteSize()) - end();
    }

    // ==================== State Queries ====================

    /**
     * Checks if this segment contains active data.
     * 
     * @return {@code true} if {@code length() > 0}
     */
    default boolean hasData() {
        return length() > 0;
    }

    /**
     * Checks if this segment has available headroom.
     * 
     * @return {@code true} if {@code headroom() > 0}
     */
    default boolean hasHeadroom() {
        return headroom() > 0;
    }

    /**
     * Checks if this segment has available tailroom.
     * 
     * @return {@code true} if {@code tailroom() > 0}
     */
    default boolean hasTailroom() {
        return tailroom() > 0;
    }

    // ==================== Chain Projections ====================

    /**
     * Returns the total active data across all segments in the memory chain.
     * 
     * <p>
     * This projection aggregates the {@link #length()} of each segment
     * in the chain, providing the total amount of currently utilized data.
     * For single (non-chained) memory objects, this returns the same as length().
     * </p>
     * 
     * @return the sum of all active data lengths in the chain
     * @see #length() for single segment active data
     */
    default long totalLength() {
        if (!(this instanceof Memory)) {
            return length();
        }
        long total = 0;
        for (Memory mem = (Memory) this; mem != null; mem = mem.nextSegment()) {
            total += mem.length();
        }
        return total;
    }

    /**
     * Returns the total capacity across all segments in the memory chain.
     * 
     * <p>
     * This projection aggregates the {@link #byteSize()} of each segment in the
     * chain, providing the total allocated memory regardless of usage.
     * For single (non-chained) memory objects, this returns the same as byteSize().
     * </p>
     * 
     * @return the sum of all segment sizes in the chain
     * @see #byteSize() for single segment capacity
     */
    default long totalSize() {
        if (!(this instanceof Memory)) {
            return byteSize();
        }
        long total = 0;
        for (Memory mem = (Memory) this; mem != null; mem = mem.nextSegment()) {
            total += mem.byteSize();
        }
        return total;
    }

    /**
     * Returns the number of memory segments in the chain.
     * 
     * <p>
     * Counts all linked memory segments starting from the current segment.
     * For single (non-chained) memory objects, this returns 1.
     * </p>
     * 
     * @return the number of segments in the chain (always ≥ 1)
     */
    default int segmentCount() {
        return 1;
    }
}
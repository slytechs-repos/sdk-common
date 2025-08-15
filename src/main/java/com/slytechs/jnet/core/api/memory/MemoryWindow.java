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
 * Interface defining memory region boundaries and chain-wide projections for
 * efficient memory management.
 * 
 * <p>
 * MemoryWindow provides a comprehensive view of memory organization through two
 * fundamental concepts: <strong>regions</strong> (segment-local boundaries) and
 * <strong>projections</strong> (chain-wide aggregations). This dual-level
 * abstraction enables precise control over memory boundaries while supporting
 * efficient zero-copy operations across linked memory structures.
 * </p>
 * 
 * <h2>Memory Organization Model</h2>
 * 
 * <h3>Single Segment Region</h3>
 * <p>
 * Each memory segment defines a <strong>region</strong> with two distinct
 * boundary sets:
 * </p>
 * <ul>
 * <li><strong>Segment Boundaries:</strong> The immutable, total addressable
 * region [segmentOffset, segmentEnd)</li>
 * <li><strong>Active Bytes:</strong> The mutable, currently utilized data
 * region [activeBytesStart, activeBytesEnd)</li>
 * </ul>
 * 
 * <pre>{@code
* SEGMENT REGION:
* ┌────────────────────────────────────────────────────────┐
* │                    segmentSize                         │
* │  ┌──────────┬─────────────────────┬──────────┐         │
* │  │ headroom │  activeBytesLength  │ tailroom │         │
* │  └──────────┴─────────────────────┴──────────┘         │
* └────────────────────────────────────────────────────────┘
*    ↑          ↑                     ↑          ↑
* segmentOffset activeBytesStart activeBytesEnd segmentEnd
* 
* Invariants:
* • segmentOffset ≤ activeBytesStart ≤ activeBytesEnd ≤ segmentEnd
* • headroom = activeBytesStart - segmentOffset
* • tailroom = segmentEnd - activeBytesEnd
* • segmentSize = segmentEnd - segmentOffset (immutable)
* }</pre>
 * 
 * <h3>Chain-Wide Projection</h3>
 * <p>
 * Multiple segments can be linked to form a chain, where
 * <strong>projections</strong> aggregate properties across all segments:
 * </p>
 * 
 * <pre>{@code
* CHAIN PROJECTION (3 segments):
* 
* Segment 1           Segment 2           Segment 3
* ┌──────────┐       ┌──────────┐       ┌─────────────┐
* │ ┌─────┐  │  ───► │  ┌─────┐ │  ───► │   ┌─────┐   │
* │ │bytes│  │       │  │bytes│ │       │   │bytes│   │
* │ └─────┘  │       │  └─────┘ │       │   └─────┘   │
* └──────────┘       └──────────┘       └─────────────┘
* 
* totalActiveBytes() = sum of all activeBytesLength()
* totalSegmentSize() = sum of all segmentSize()
* segmentCount() = 3
* }</pre>
 * 
 * <h2>Key Concepts</h2>
 * 
 * <h3>Regions (Segment-Local)</h3>
 * <p>
 * A <strong>region</strong> represents the memory boundaries within a single
 * segment:
 * </p>
 * <ul>
 * <li><strong>Segment boundaries</strong> are immutable after construction,
 * defining the maximum addressable space</li>
 * <li><strong>Active bytes boundaries</strong> are mutable, representing the
 * currently utilized portion of the segment</li>
 * <li><strong>Headroom/Tailroom</strong> represent available expansion space
 * for protocols that may need to prepend headers or append trailers</li>
 * </ul>
 * 
 * <h3>Projections (Chain-Wide)</h3>
 * <p>
 * A <strong>projection</strong> aggregates properties across all linked
 * segments:
 * </p>
 * <ul>
 * <li><strong>totalSegmentSize()</strong> - Total allocated memory across
 * chain</li>
 * <li><strong>totalActiveBytes()</strong> - Total utilized bytes across
 * chain</li>
 * <li><strong>segmentCount()</strong> - Number of segments in chain</li>
 * </ul>
 * 
 * <h2>Mutability Model</h2>
 * <p>
 * The interface distinguishes between immutable and mutable boundaries:
 * </p>
 * 
 * <table border="1">
 * <caption>Boundary Mutability</caption> <thead>
 * <tr>
 * <th>Property</th>
 * <th>Mutability</th>
 * <th>Typical Setting</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>segmentOffset()</td>
 * <td>Immutable</td>
 * <td>Constructor</td>
 * </tr>
 * <tr>
 * <td>segmentEnd()</td>
 * <td>Immutable</td>
 * <td>Constructor</td>
 * </tr>
 * <tr>
 * <td>activeBytesStart()</td>
 * <td>Mutable</td>
 * <td>Runtime adjustment</td>
 * </tr>
 * <tr>
 * <td>activeBytesEnd()</td>
 * <td>Mutable</td>
 * <td>Runtime adjustment</td>
 * </tr>
 * </tbody>
 * </table>
 * 
 * <p>
 * <strong>Exception:</strong> MemoryProxy allows rebinding to different memory,
 * effectively changing segment boundaries through rebinding rather than
 * mutation.
 * </p>
 * 
 * <h2>Common Usage Patterns</h2>
 * 
 * <h3>Protocol Header Processing</h3>
 * 
 * <pre>{@code
 * // Utilize headroom for VLAN tag insertion
 * if (memory.headroom() >= 4) {
 * 	memory.activeBytesStart(memory.activeBytesStart() - 4);
 * 	// Write VLAN tag in newly exposed headroom
 * }
 * }</pre>
 * 
 * <h3>Buffer Management</h3>
 * 
 * <pre>{@code
 * // Track consumed data by adjusting active bytes
 * long consumed = processData(memory);
 * memory.activeBytesStart(memory.activeBytesStart() + consumed);
 * 
 * // Check remaining active data
 * if (memory.activeBytesLength() > 0) {
 * 	// More data to process
 * }
 * }</pre>
 * 
 * <h3>Chain Analysis</h3>
 * 
 * <pre>{@code
 * // Analyze entire chain
 * long totalData = memory.totalActiveBytes();
 * int fragments = memory.segmentCount();
 * double utilization = (double) totalData / memory.totalSegmentSize();
 * }</pre>
 * 
 * <h2>Design Rationale</h2>
 * <p>
 * The naming convention deliberately uses distinctive terms to avoid conflicts
 * with domain-specific properties in subclasses:
 * </p>
 * <ul>
 * <li><strong>segment*</strong> - Clearly indicates memory segment
 * boundaries</li>
 * <li><strong>activeBytes*</strong> - Distinguishes utilized data from protocol
 * payloads</li>
 * <li><strong>headroom/tailroom</strong> - Industry-standard terms from
 * DPDK/kernel</li>
 * <li><strong>total*</strong> - Explicitly indicates chain-wide
 * projections</li>
 * </ul>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 * @see MemoryView for content access operations
 * @see MemoryRef for lifecycle and reference management
 * @see Memory for the complete memory abstraction
 */
public interface MemoryWindow {

	/**
	 * Returns the ending offset of the active bytes within this segment.
	 * 
	 * <p>
	 * This mutable boundary represents the first byte position beyond the currently
	 * active data. The active bytes region spans from {@link #activeBytesStart()}
	 * (inclusive) to {@code activeBytesEnd()} (exclusive). The value must satisfy:
	 * {@code activeBytesStart() ≤ activeBytesEnd() ≤ segmentEnd()}.
	 * </p>
	 * 
	 * <p>
	 * The default implementation returns {@link #segmentEnd()}, meaning active data
	 * extends to the end of the segment.
	 * </p>
	 * 
	 * @return the absolute offset where active bytes end (exclusive)
	 * @throws IllegalStateException if this memory window is closed or invalid
	 * 
	 * @see #activeBytesEnd(long) to adjust the ending offset
	 * @see #activeBytesStart() for the starting boundary
	 * @see #tailroom() for available space after active bytes
	 */
	default long activeBytesEnd() {
		return segmentEnd();
	}

	/**
	 * Sets the ending offset of the active bytes within this segment.
	 * 
	 * <p>
	 * Adjusts the end of the active data region, typically used when appending data
	 * or truncating the active region. The new offset must satisfy the constraint:
	 * {@code activeBytesStart() ≤ newEnd ≤ segmentEnd()}.
	 * </p>
	 * 
	 * @param newEnd the new ending offset for active bytes
	 * @return the new end offset (same as parameter for chaining)
	 * @throws IllegalArgumentException if newEnd violates segment boundaries
	 * @throws IllegalStateException    if this memory window is closed or invalid
	 * 
	 * @see #activeBytesEnd() to read the current offset
	 */
	long activeBytesEnd(long newEnd);

	/**
	 * Returns the length of active bytes in this segment.
	 * 
	 * <p>
	 * Calculates the span of currently active data as
	 * {@code activeBytesEnd() - activeBytesStart()}. This represents the actual
	 * amount of meaningful data within the memory segment, as opposed to the total
	 * allocated space.
	 * </p>
	 * 
	 * <pre>{@code
	 * Example:
	 * Segment: [headroom:100][active:500][tailroom:400]
	 * activeBytesLength() = 500
	 * }</pre>
	 * 
	 * @return the active bytes length in bytes (always ≥ 0)
	 * @throws IllegalStateException if this memory window is closed or invalid
	 * 
	 * @see #totalActiveBytes() for chain-wide active bytes
	 * @see #segmentSize() for total allocated space
	 */
	default long activeBytesLength() {
		return activeBytesEnd() - activeBytesStart();
	}

	/**
	 * Returns the starting offset of the active bytes within this segment.
	 * 
	 * <p>
	 * This mutable boundary represents the first byte of currently active data
	 * within the memory segment. The active bytes region can be adjusted at runtime
	 * to reflect data consumption, production, or protocol header operations. The
	 * value must satisfy:
	 * {@code segmentOffset() ≤ activeBytesStart() ≤ activeBytesEnd()}.
	 * </p>
	 * 
	 * <p>
	 * The default implementation returns {@link #segmentOffset()}, meaning active
	 * data starts at the beginning of the segment.
	 * </p>
	 * 
	 * @return the absolute offset where active bytes begin (inclusive)
	 * @throws IllegalStateException if this memory window is closed or invalid
	 * 
	 * @see #activeBytesStart(long) to adjust the starting offset
	 * @see #activeBytesEnd() for the ending boundary
	 * @see #headroom() for available space before active bytes
	 */
	default long activeBytesStart() {
		return segmentOffset();
	}

	/**
	 * Sets the starting offset of the active bytes within this segment.
	 * 
	 * <p>
	 * Adjusts the beginning of the active data region, typically used when
	 * consuming data from the front or prepending headers using headroom. The new
	 * offset must satisfy the constraint:
	 * {@code segmentOffset() ≤ newOffset ≤ activeBytesEnd()}.
	 * </p>
	 * 
	 * @param newOffset the new starting offset for active bytes
	 * @return the new offset (same as parameter for chaining)
	 * @throws IllegalArgumentException if newOffset violates segment boundaries
	 * @throws IllegalStateException    if this memory window is closed or invalid
	 * 
	 * @see #activeBytesStart() to read the current offset
	 */
	long activeBytesStart(long newOffset);

	/**
	 * Checks if this segment contains active bytes.
	 * 
	 * @return {@code true} if {@code activeBytesLength() > 0}
	 */
	default boolean hasActiveBytes() {
		return activeBytesLength() > 0;
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

	/**
	 * Checks if the entire chain contains active bytes.
	 * 
	 * @return {@code true} if {@code totalActiveBytes() > 0}
	 */
	default boolean hasTotalActiveBytes() {
		return totalActiveBytes() > 0;
	}

	/**
	 * Returns the available space before the active bytes (headroom).
	 * 
	 * <p>
	 * Headroom represents unused space at the beginning of the segment that can be
	 * utilized for prepending headers or expanding the active region backward. This
	 * is particularly useful in network protocol processing where headers need to
	 * be added to existing data.
	 * </p>
	 * 
	 * <pre>{@code
	 * Segment: [headroom:100][active:500][tailroom:400]
	 *          ↑              ↑
	 *    segmentOffset   activeBytesStart
	 *    
	 * headroom() = activeBytesStart - segmentOffset = 100
	 * }</pre>
	 * 
	 * @return available space before active bytes in bytes (always ≥ 0)
	 * @throws IllegalStateException if this memory window is closed or invalid
	 * 
	 * @see #tailroom() for space after active bytes
	 * @see #hasHeadroom() for convenience check
	 */
	default long headroom() {
		return activeBytesStart() - segmentOffset();
	}

	/**
	 * Returns the number of memory segments in the chain.
	 * 
	 * <p>
	 * This method counts all linked memory segments starting from the current
	 * segment and following the chain via {@link Memory#nextSegment()}. For single
	 * (non-chained) memory objects, this returns 1. For chained structures, it
	 * provides the total fragmentation count.
	 * </p>
	 * 
	 * <pre>{@code
	* Single segment:
	* [Segment] 
	* segmentCount() = 1
	* 
	* Chained segments:
	* [Segment1] → [Segment2] → [Segment3]
	* segmentCount() = 3
	* }</pre>
	 * 
	 * <p>
	 * This metric is useful for:
	 * </p>
	 * <ul>
	 * <li>Understanding memory fragmentation levels</li>
	 * <li>Debugging chain structure and linkage</li>
	 * <li>Performance analysis (fragmented chains may impact processing)</li>
	 * <li>Memory allocation strategies (deciding when to consolidate)</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Performance Note:</strong> This operation requires traversing the
	 * entire chain and has O(n) complexity where n is the number of segments. The
	 * default implementation returns 1, which is correct for non-chained memory
	 * implementations that don't override this method.
	 * </p>
	 * 
	 * @return the number of segments in the chain (always ≥ 1)
	 * @throws IllegalStateException if this memory is closed or invalid
	 * 
	 * @see #totalActiveBytes() for aggregate data size across all segments
	 * @see #totalSegmentSize() for aggregate capacity across all segments
	 * @see Memory#nextSegment() for chain traversal
	 */
	default int segmentCount() {
		return 1;
	}

	/**
	 * Returns the ending offset of this memory segment region.
	 * 
	 * <p>
	 * This immutable boundary represents the first byte position beyond the
	 * addressable memory region. The valid memory addresses range from
	 * {@link #segmentOffset()} (inclusive) to {@code segmentEnd()} (exclusive),
	 * following standard Java conventions.
	 * </p>
	 * 
	 * @return the absolute ending offset of the segment (exclusive)
	 * @throws IllegalStateException if this memory window is closed or invalid
	 * 
	 * @see #segmentOffset() for the starting boundary
	 * @see #segmentSize() for the total span
	 */
	long segmentEnd();

	/**
	 * Returns the starting offset of this memory segment region.
	 * 
	 * <p>
	 * This immutable boundary represents the first addressable byte position within
	 * the memory segment. All other offsets and positions are calculated relative
	 * to this base offset. Once set (typically in the constructor), this value
	 * remains constant for the lifetime of the memory object.
	 * </p>
	 * 
	 * @return the absolute starting offset of the segment (inclusive)
	 * @throws IllegalStateException if this memory window is closed or invalid
	 * 
	 * @see #segmentEnd() for the ending boundary
	 * @see #segmentSize() for the total span
	 */
	long segmentOffset();

	/**
	 * Returns the total size of this memory segment region in bytes.
	 * 
	 * <p>
	 * This immutable value represents the maximum addressable space within this
	 * memory segment, calculated as {@code segmentEnd() - segmentOffset()}. The
	 * segment size defines the upper bound for all data operations and remains
	 * constant for the lifetime of the memory object.
	 * </p>
	 * 
	 * <p>
	 * <strong>Note:</strong> This refers to a single segment's capacity. For the
	 * total capacity across a chain, use {@link #totalSegmentSize()}.
	 * </p>
	 * 
	 * @return the total segment size in bytes (always ≥ 0)
	 * @throws IllegalStateException if this memory window is closed or invalid
	 * 
	 * @see #totalSegmentSize() for chain-wide projection
	 */
	long segmentSize();

	/**
	 * Returns the available space after the active bytes (tailroom).
	 * 
	 * <p>
	 * Tailroom represents unused space at the end of the segment that can be
	 * utilized for appending data or expanding the active region forward. This is
	 * useful for adding trailers, padding, or accumulating incoming data.
	 * </p>
	 * 
	 * <pre>{@code
	 * Segment: [headroom:100][active:500][tailroom:400]
	 *                                     ↑            ↑
	 *                              activeBytesEnd  segmentEnd
	 *    
	 * tailroom() = segmentEnd - activeBytesEnd = 400
	 * }</pre>
	 * 
	 * @return available space after active bytes in bytes (always ≥ 0)
	 * @throws IllegalStateException if this memory window is closed or invalid
	 * 
	 * @see #headroom() for space before active bytes
	 * @see #hasTailroom() for convenience check
	 */
	default long tailroom() {
		return segmentEnd() - activeBytesEnd();
	}

	/**
	 * Returns the total active bytes across all segments in the memory chain.
	 * 
	 * <p>
	 * This projection aggregates the {@link #activeBytesLength()} of each segment
	 * in the chain, providing the total amount of currently utilized data. This is
	 * the key metric for understanding actual data content across fragmented memory
	 * structures.
	 * </p>
	 * 
	 * <pre>{@code
	 * Chain: [Seg1: 500 active] → [Seg2: 1500 active] → [Seg3: 200 active]
	 * totalActiveBytes() = 500 + 1500 + 200 = 2200 bytes
	 * }</pre>
	 * 
	 * @return the sum of all active byte lengths in the chain
	 * @throws IllegalStateException if this memory window is closed or invalid
	 * 
	 * @see #activeBytesLength() for single segment active bytes
	 * @see #totalSegmentSize() for total allocated space
	 */
	default long totalActiveBytes() {
		long total = 0;
		for (Memory mem = (Memory) this; mem != null; mem = mem.nextSegment()) {
			total += mem.activeBytesLength();
		}
		return total;
	}

	/**
	 * Returns the total segment size across all segments in the memory chain.
	 * 
	 * <p>
	 * This projection aggregates the {@link #segmentSize()} of each segment in the
	 * chain, providing the total allocated memory regardless of how much is
	 * currently active. This represents the maximum possible data that could be
	 * stored across the entire chain structure.
	 * </p>
	 * 
	 * <pre>{@code
	 * Chain: [Segment1: 1024] → [Segment2: 2048] → [Segment3: 512]
	 * totalSegmentSize() = 1024 + 2048 + 512 = 3584 bytes
	 * }</pre>
	 * 
	 * @return the sum of all segment sizes in the chain (in bytes)
	 * @throws IllegalStateException if this memory window is closed or invalid
	 * 
	 * @see #segmentSize() for single segment size
	 * @see #totalActiveBytes() for utilized data across chain
	 */
	default long totalSegmentSize() {
		long total = 0;
		for (Memory mem = (Memory) this; mem != null; mem = mem.nextSegment()) {
			total += mem.segmentSize();
		}
		return total;
	}
}
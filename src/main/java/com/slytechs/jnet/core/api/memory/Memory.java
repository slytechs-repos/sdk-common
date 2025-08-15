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
 * Comprehensive interface for high-performance memory management supporting
 * both single segments and chained structures.
 * 
 * <p>
 * Memory provides a unified abstraction for memory operations, combining
 * content access, boundary management, and lifecycle control into a single
 * cohesive interface. This design enables zero-allocation, high-performance
 * memory operations essential for network packet processing, streaming data,
 * and other performance-critical applications.
 * </p>
 * 
 * <h2>Memory Model Architecture</h2>
 * 
 * <h3>Single Segment Structure</h3>
 * 
 * <pre>{@code
* Memory Segment Region:
* ┌─────────────────────────────────────────────────────┐
* │                  segmentSize()                      │
* │  ┌──────────┬─────────────────────┬──────────┐      │
* │  │ headroom │ activeBytesLength() │ tailroom │      │
* │  └──────────┴─────────────────────┴──────────┘      │
* └─────────────────────────────────────────────────────┘
*    ↑          ↑                     ↑          ↑
* segmentOffset activeBytesStart activeBytesEnd segmentEnd
* 
* Relationships:
* • segmentSize() = segmentEnd() - segmentOffset()
* • activeBytesLength() = activeBytesEnd() - activeBytesStart()
* • headroom() + activeBytesLength() + tailroom() = segmentSize()
* }</pre>
 * 
 * <h3>Chained Memory Structure</h3>
 * 
 * <pre>{@code
* Memory Chain:
* ┌─────────┐      ┌─────────┐      ┌─────────┐
* │ Memory  │ next │ Memory  │ next │ Memory  │ next
* │ Seg #1  │ ───> │ Seg #2  │ ───> │ Seg #3  │ ───> null
* └─────────┘      └─────────┘      └─────────┘
*     1KB             2KB              512B
* 
* Chain Projections:
* • totalSegmentSize() = 1KB + 2KB + 512B = 3.5KB
* • totalActiveBytes() = sum of all activeBytesLength()
* • segmentCount() = 3
* }</pre>
 * 
 * <h2>Design Philosophy</h2>
 * 
 * <h3>Zero-Allocation Operations</h3>
 * <p>
 * All core operations are designed to avoid memory allocation, enabling
 * sustained high-performance operation without garbage collection pressure:
 * </p>
 * <ul>
 * <li>Navigation through chains creates no new objects</li>
 * <li>View conversions return shared references</li>
 * <li>Boundary adjustments modify existing state</li>
 * </ul>
 * 
 * <h3>Reference Counting</h3>
 * <p>
 * Thread-safe reference counting ensures proper lifecycle management:
 * </p>
 * <ul>
 * <li>Automatic cleanup when reference count reaches zero</li>
 * <li>Integration with memory pools for efficient reuse</li>
 * <li>Safe sharing across threads and components</li>
 * </ul>
 * 
 * <h3>Chain Support</h3>
 * <p>
 * Native support for fragmented memory without copying:
 * </p>
 * <ul>
 * <li>Efficient traversal through linked segments</li>
 * <li>Position-based access across entire chain</li>
 * <li>Aggregated metrics via projections</li>
 * </ul>
 * 
 * <h2>Common Usage Patterns</h2>
 * 
 * <h3>Network Packet Processing</h3>
 * 
 * <pre>{@code
 * // Zero-copy packet handling
 * Memory packet = receivePacket();
 * 
 * // Check for VLAN tag insertion capability
 * if (packet.headroom() >= 4) {
 * 	// Expand active bytes into headroom
 * 	packet.activeBytesStart(packet.activeBytesStart() - 4);
 * 	// Write VLAN tag
 * 	packet.asByteBuffer().putInt(0, vlanTag);
 * }
 * 
 * // Process entire packet chain
 * for (Memory seg = packet; seg != null; seg = seg.nextSegment()) {
 * 	processSegment(seg);
 * }
 * }</pre>
 * 
 * <h3>Memory Pool Integration</h3>
 * 
 * <pre>{@code
 * // Allocate from pool
 * Memory buffer = pool.allocate();
 * try {
 * 	// Use buffer
 * 	fillBuffer(buffer);
 * 	processBuffer(buffer);
 * } finally {
 * 	// Automatic return to pool when refcount reaches 0
 * 	buffer.decrementRef();
 * }
 * }</pre>
 * 
 * <h3>Streaming Data Processing</h3>
 * 
 * <pre>{@code
 * // Process streaming data with active bytes tracking
 * Memory stream = getStreamBuffer();
 * 
 * while (stream.hasActiveBytes()) {
 * 	long consumed = processData(stream);
 * 
 * 	// Advance active bytes start to mark consumed data
 * 	stream.activeBytesStart(stream.activeBytesStart() + consumed);
 * 
 * 	// Check if we need more data
 * 	if (stream.activeBytesLength() < MIN_THRESHOLD) {
 * 		refillBuffer(stream);
 * 	}
 * }
 * }</pre>
 * 
 * <h2>Factory Methods</h2>
 * <p>
 * The Memory interface provides static factory methods for creating Memory
 * instances from Java FFM MemorySegments:
 * </p>
 * 
 * <pre>{@code
 * // Wrap entire MemorySegment
 * MemorySegment segment = Arena.global().allocate(1024);
 * Memory memory = Memory.of(segment, 0);
 * 
 * // Wrap specific region
 * Memory slice = Memory.of(segment, 100, 500); // 500 bytes starting at offset 100
 * }</pre>
 * 
 * <h2>Implementation Requirements</h2>
 * <p>
 * Implementations must ensure:
 * </p>
 * <ul>
 * <li><strong>Thread Safety:</strong> Reference counting operations must be
 * atomic</li>
 * <li><strong>Boundary Invariants:</strong> segmentOffset ≤ activeBytesStart ≤
 * activeBytesEnd ≤ segmentEnd</li>
 * <li><strong>Chain Integrity:</strong> Chain structure must remain valid
 * during traversal</li>
 * <li><strong>Resource Cleanup:</strong> Proper resource release when refcount
 * reaches zero</li>
 * </ul>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 * @see MemoryView for content access and chain navigation
 * @see MemoryWindow for boundary management and projections
 * @see MemoryRef for lifecycle and reference counting
 */
public interface Memory extends MemoryView, MemoryWindow, MemoryRef {

	/**
	 * Checks if a MemorySegment represents null memory.
	 * 
	 * <p>
	 * This utility method provides consistent null checking across the Memory API,
	 * testing for both Java null references and native NULL pointers (address = 0).
	 * This dual check is essential when working with native memory allocations or
	 * pointers returned from foreign functions.
	 * </p>
	 * 
	 * <pre>{@code
	 * MemorySegment nativePtr = getNativePointer();
	 * if (Memory.isNull(nativePtr)) {
	 * 	// Handle null pointer case
	 * 	return Memory.NULL;
	 * }
	 * // Safe to wrap in Memory
	 * Memory memory = Memory.of(nativePtr, 0);
	 * }</pre>
	 * 
	 * @param segment the MemorySegment to test, may be {@code null}
	 * @return {@code true} if segment is {@code null} or has address 0,
	 *         {@code false} otherwise
	 */
	static boolean isNull(MemorySegment segment) {
		return segment == null || segment.address() == 0;
	}

	/**
	 * Creates a Memory wrapper for a MemorySegment starting at the specified
	 * offset.
	 * 
	 * <p>
	 * This factory method creates a Memory view that spans from the specified
	 * offset to the end of the MemorySegment. The segment boundaries are set to the
	 * specified region, with active bytes initially matching the segment boundaries
	 * (no headroom or tailroom).
	 * </p>
	 * 
	 * <pre>{@code
	 * MemorySegment segment = arena.allocate(1024);
	 * 
	 * // Wrap entire segment
	 * Memory full = Memory.of(segment, 0);
	 * // segmentSize() = 1024, activeBytesLength() = 1024
	 * 
	 * // Wrap from offset 256 to end
	 * Memory partial = Memory.of(segment, 256);
	 * // segmentSize() = 768, activeBytesLength() = 768
	 * }</pre>
	 * 
	 * @param segment the backing MemorySegment, must not be null
	 * @param offset  the starting offset within the segment, must be ≥ 0
	 * @return a new Memory object wrapping the specified region
	 * @throws NullPointerException     if segment is {@code null}
	 * @throws IllegalArgumentException if offset is negative or exceeds segment
	 *                                  size
	 * 
	 * @see #of(MemorySegment, long, long) for explicit size specification
	 */
	static Memory of(MemorySegment segment, long offset) {
		long start = offset, stop = segment.byteSize() - offset;

		return new MemoryByteBuffer(null, segment, start, stop, start, stop);
	}

	/**
	 * Creates a Memory wrapper for a specific region of a MemorySegment.
	 * 
	 * <p>
	 * This factory method creates a Memory view with explicit bounds, providing
	 * precise control over the accessible memory region. The segment boundaries are
	 * set to [offset, offset+size), with active bytes initially matching these
	 * boundaries.
	 * </p>
	 * 
	 * <pre>{@code
	 * MemorySegment segment = arena.allocate(2048);
	 * 
	 * // Wrap middle 1024 bytes (offset 512, size 1024)
	 * Memory middle = Memory.of(segment, 512, 1024);
	 * // segmentOffset() = 512
	 * // segmentEnd() = 1536
	 * // segmentSize() = 1024
	 * // activeBytesLength() = 1024 (initially)
	 * 
	 * // Later can adjust active bytes within segment bounds
	 * middle.activeBytesStart(600); // Create 88 bytes of headroom
	 * middle.activeBytesEnd(1400); // Create 136 bytes of tailroom
	 * }</pre>
	 * 
	 * @param segment the backing MemorySegment, must not be null
	 * @param offset  the starting offset within the segment, must be ≥ 0
	 * @param size    the size of the accessible region, must be ≥ 0
	 * @return a new Memory object wrapping the specified region
	 * @throws NullPointerException     if segment is {@code null}
	 * @throws IllegalArgumentException if offset is negative, size is negative, or
	 *                                  offset+size exceeds segment bounds
	 * 
	 * @see #of(MemorySegment, long) for offset-to-end wrapping
	 */
	static Memory of(MemorySegment segment, long offset, long size) {
		long start = offset, stop = start + size;

		return new MemoryByteBuffer(null, segment, start, stop, start, stop);
	}

	/**
	 * Returns a ByteBuffer view of this segment's active bytes.
	 * 
	 * <p>
	 * Creates a ByteBuffer that provides access to the currently active data within
	 * this memory segment. The buffer's position is set to 0 and its limit
	 * corresponds to {@link #activeBytesLength()}. The returned buffer shares the
	 * underlying memory with this Memory object.
	 * </p>
	 * 
	 * <p>
	 * The default implementation creates an appropriate slice of the underlying
	 * MemorySegment. Implementations may override this for optimized buffer
	 * creation or to handle special cases.
	 * </p>
	 * 
	 * @return a ByteBuffer view of the active bytes region
	 * @throws UnsupportedOperationException if ByteBuffer access is not supported
	 * @throws IllegalStateException         if this memory is closed or invalid
	 * 
	 * @see #asMemorySegment() for MemorySegment view
	 */
	@Override
	default ByteBuffer asByteBuffer() {
		return asMemorySegment().asSlice(segmentOffset(), segmentSize()).asByteBuffer();
	}

	/**
	 * Returns the MemorySegment at the specified position within the memory chain.
	 * 
	 * <p>
	 * Provides position-based random access across the entire memory chain,
	 * automatically locating and returning the segment containing the specified
	 * byte position. The position is relative to the start of the chain's active
	 * bytes (position 0 = first byte of first segment's active region).
	 * </p>
	 * 
	 * <pre>{@code
	 * // Access data at position 1500 in a 3-segment chain
	 * MemorySegment target = memory.asMemorySegmentAt(1500);
	 * 
	 * // Read value at that position
	 * int value = target.get(ValueLayout.JAVA_INT, 0);
	 * }</pre>
	 * 
	 * <p>
	 * The default implementation optimizes for single-segment memories and
	 * delegates to {@link #seekSegment(long)} for multi-segment chains.
	 * </p>
	 * 
	 * @param position the byte position from chain start (0-based)
	 * @return the MemorySegment containing the specified position
	 * @throws IllegalArgumentException if position is negative or exceeds
	 *                                  {@link #totalActiveBytes()}
	 * @throws IllegalStateException    if this memory is closed or invalid
	 * 
	 * @see #seekSegment(long) to get the containing Memory object
	 * @see #totalActiveBytes() for valid position range
	 */
	@Override
	default MemorySegment asMemorySegmentAt(long position) {
		if (position < 0 || position >= totalActiveBytes())
			throw new IllegalArgumentException("position out of bounds: " + position);

		if (segmentCount() == 1) {
			return asMemorySegment();
		}
		Memory sought = seekSegment(position);
		return sought.asMemorySegment();
	}

	/**
	 * Checks if this memory represents a null or invalid reference.
	 * 
	 * <p>
	 * Delegates to the static {@link #isNull(MemorySegment)} method to check the
	 * underlying MemorySegment. This method is essential for safe operations when
	 * working with memory that may be uninitialized or deallocated.
	 * </p>
	 * 
	 * <pre>{@code
	 * Memory memory = getMemoryFromNative();
	 * if (!memory.isNull()) {
	 * 	// Safe to use memory
	 * 	processMemory(memory);
	 * }
	 * }</pre>
	 * 
	 * @return {@code true} if this represents null or invalid memory, {@code false}
	 *         if the memory is valid and accessible
	 * 
	 * @see #isPointer() for checking zero-sized memory
	 * @see #isNull(MemorySegment) for static null checking
	 */
	@Override
	default boolean isNull() {
		return isNull(asMemorySegment());
	}

	/**
	 * Returns the total active bytes across all segments in the memory chain.
	 * 
	 * <p>
	 * This projection aggregates the {@link #activeBytesLength()} of each segment
	 * in the chain, providing the total amount of currently utilized data. This is
	 * the key metric for understanding actual content size in fragmented memory
	 * structures and determines valid positions for chain-relative operations.
	 * </p>
	 * 
	 * <pre>{@code
	 * Memory Chain with different utilization:
	 * Segment 1: [headroom:100][active:500][tailroom:424]  = 1024 total
	 * Segment 2: [active:1500][tailroom:548]               = 2048 total
	 * Segment 3: [headroom:50][active:200][tailroom:262]   = 512 total
	 * 
	 * totalActiveBytes() = 500 + 1500 + 200 = 2200 bytes
	 * totalSegmentSize() = 1024 + 2048 + 512 = 3584 bytes
	 * 
	 * Utilization = 2200 / 3584 = 61.4%
	 * }</pre>
	 * 
	 * <p>
	 * This value defines the valid range [0, totalActiveBytes()) for position-based
	 * operations like {@link #asMemorySegmentAt(long)} and
	 * {@link #seekSegment(long)}.
	 * </p>
	 * 
	 * @return the sum of all active byte lengths in the chain
	 * @throws IllegalStateException if this memory is closed or invalid
	 * 
	 * @see #activeBytesLength() for single segment active data
	 * @see #totalSegmentSize() for total allocated capacity
	 */
	@Override
	default long totalActiveBytes() {
		long total = 0;
		for (Memory mem = this; mem != null; mem = mem.nextSegment()) {
			total += mem.activeBytesLength();
		}
		return total;
	}

	/**
	 * Returns the total segment size across all segments in the memory chain.
	 * 
	 * <p>
	 * This projection aggregates the {@link #segmentSize()} of each segment in the
	 * chain, providing the total allocated memory regardless of current
	 * utilization. This represents the maximum possible data capacity across the
	 * entire chain structure.
	 * </p>
	 * 
	 * <pre>{@code
	 * Memory Chain:
	 * [Segment1: 1KB] → [Segment2: 2KB] → [Segment3: 512B]
	 * 
	 * totalSegmentSize() = 1024 + 2048 + 512 = 3584 bytes
	 * 
	 * Even if active bytes are less:
	 * [Active: 500B]  → [Active: 1KB]  → [Active: 200B]
	 * totalSegmentSize() still = 3584 bytes (unchanged)
	 * }</pre>
	 * 
	 * <p>
	 * <strong>Performance Note:</strong> This operation requires traversing the
	 * entire chain with O(n) complexity. Consider caching the result if accessed
	 * frequently.
	 * </p>
	 * 
	 * @return the sum of all segment sizes in the chain (in bytes)
	 * @throws IllegalStateException if this memory is closed or invalid
	 * 
	 * @see #segmentSize() for single segment capacity
	 * @see #totalActiveBytes() for actual data across chain
	 */
	@Override
	default long totalSegmentSize() {
		long total = 0;
		for (Memory mem = this; mem != null; mem = mem.nextSegment()) {
			total += mem.segmentSize();
		}
		return total;
	}
}
/*
 * Sly Technologies Free License
 * 
 * Copyright 2024 Sly Technologies Inc.
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
 * Core interface providing read-only view capabilities for memory access and
 * chain navigation.
 * 
 * <p>
 * MemoryView defines the fundamental contract for accessing memory content
 * through various representations (ByteBuffer, MemorySegment) and provides
 * navigation capabilities for chained memory structures. This interface serves
 * as the foundation for all memory access operations, focusing on content
 * retrieval and chain traversal without modification capabilities.
 * </p>
 * 
 * <h2>Memory Organization</h2>
 * <p>
 * MemoryView operates on two levels of memory organization:
 * </p>
 * 
 * <h3>Segment Level (Local)</h3>
 * <p>
 * Individual memory segments that can be accessed through various views:
 * </p>
 * 
 * <pre>{@code
 * Single Segment:
 * ┌────────────────────────────────────┐
 * │     Memory Segment (Region)        │
 * │  ┌──────────────────────────────┐  │
 * │  │    Active Bytes (Data)       │  │
 * │  └──────────────────────────────┘  │
 * └────────────────────────────────────┘
 *    ↑                              ↑
 * segmentOffset()              segmentEnd()
 * }</pre>
 * 
 * <h3>Chain Level (Global)</h3>
 * <p>
 * Multiple segments linked to form a continuous logical memory space:
 * </p>
 * 
 * <pre>{@code
 * Memory Chain:
 * [Segment 1] ──next──> [Segment 2] ──next──> [Segment 3] ──null
 *      ↑                     ↑                     ↑
 *   offset=0            offset=1024           offset=2048
 * 
 * Chain navigation: nextSegment(), hasNextSegment(), seekSegment(offset)
 * }</pre>
 * 
 * <h2>Key Capabilities</h2>
 * <ul>
 * <li><strong>Content Access:</strong> Convert memory to ByteBuffer or
 * MemorySegment views for direct data access</li>
 * <li><strong>Chain Navigation:</strong> Traverse linked memory segments using
 * {@link #nextSegment()} iteration</li>
 * <li><strong>Position-Based Access:</strong> Access specific positions within
 * the chain using {@link #asMemorySegmentAt(long)}</li>
 * <li><strong>State Inspection:</strong> Check for null pointers and zero-sized
 * segments using {@link #isNull()} and {@link #isPointer()}</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * 
 * <h3>Sequential Chain Processing</h3>
 * 
 * <pre>{@code
 * // Process all segments in a chain
 * Memory current = memoryView.asMemory();
 * while (current != null) {
 * 	ByteBuffer buffer = current.asByteBuffer();
 * 	processBuffer(buffer);
 * 	current = current.nextSegment();
 * }
 * }</pre>
 * 
 * <h3>Random Access Within Chain</h3>
 * 
 * <pre>{@code
 * // Access data at specific position in chain
 * long targetPosition = 1500; // Byte position from chain start
 * 
 * // Direct segment access
 * MemorySegment segment = memoryView.asMemorySegmentAt(targetPosition);
 * 
 * // Or navigate to containing Memory object
 * Memory containing = memoryView.seekSegment(targetPosition);
 * }</pre>
 * 
 * <h3>Active Bytes Access</h3>
 * 
 * <pre>{@code
 * // Access only the active bytes within a segment
 * MemorySegment activeData = memoryView.asMemorySegment();
 * // Segment is already sliced to activeBytesStart/End boundaries
 * 
 * ByteBuffer activeBuffer = memoryView.asByteBuffer();
 * // Buffer position and limit reflect active bytes region
 * }</pre>
 * 
 * <h2>Performance Characteristics</h2>
 * <ul>
 * <li><strong>Zero-allocation:</strong> Navigation operations create no new
 * objects</li>
 * <li><strong>Lazy evaluation:</strong> Chain properties computed only when
 * accessed</li>
 * <li><strong>O(1) local access:</strong> Segment-level operations are constant
 * time</li>
 * <li><strong>O(n) chain traversal:</strong> Finding segments by position
 * requires traversal</li>
 * </ul>
 * 
 * <h2>Thread Safety</h2>
 * <p>
 * MemoryView operations are generally thread-safe for reading if the underlying
 * memory is not being modified. However, chain structure modifications
 * (adding/removing segments) require external synchronization.
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see MemoryWindow for segment boundaries and active bytes management
 * @see MemoryRef for lifecycle and reference counting
 * @see Memory for the complete memory abstraction
 * @since 1.0
 */
public interface MemoryView {

   /**
    * Returns a ByteBuffer view of this segment's active bytes region.
    * 
    * <p>
    * Creates a ByteBuffer that provides access to the currently active data
    * within this memory segment. The buffer is positioned at the start of
    * the active bytes (corresponding to {@link MemoryWindow#activeBytesStart()})
    * with its limit set to the end of active bytes (corresponding to
    * {@link MemoryWindow#activeBytesEnd()}). The returned buffer shares the
    * same underlying memory as this view.
    * </p>
    * 
    * <pre>{@code
    * Memory Segment:
    * [headroom][── active bytes ──][tailroom]
    *           ↑                   ↑
    *      buffer.position()   buffer.limit()
    * }</pre>
    * 
    * <p>
    * <strong>Chain Consideration:</strong> For chained memory structures, this
    * method only provides access to the current segment's active bytes. To process
    * all segments, iterate through the chain using {@link #nextSegment()}.
    * </p>
    * 
    * @return a ByteBuffer positioned at active bytes with appropriate limit
    * @throws UnsupportedOperationException if ByteBuffer access is not supported
    *         (e.g., for certain native memory types)
    * @throws IllegalStateException if this memory view is closed or invalid
    * 
    * @see #asMemorySegment() for MemorySegment access
    * @see MemoryWindow#activeBytesStart() for buffer position
    * @see MemoryWindow#activeBytesEnd() for buffer limit
    */
   ByteBuffer asByteBuffer();

   /**
    * Returns the Memory object at the current view position.
    * 
    * <p>
    * For standard Memory implementations, this returns {@code this}. For proxy
    * objects like {@link MemoryProxy}, this returns the currently bound Memory
    * object. This method provides access to the complete Memory interface when
    * working with view-only references.
    * </p>
    * 
    * @return the Memory object at current position
    * @throws IllegalStateException if this memory view is closed or invalid
    * 
    * @see #nextSegment() for chain navigation
    */
   Memory asMemory();

   /**
    * Returns a MemorySegment view of this segment's active bytes region.
    * 
    * <p>
    * Creates a MemorySegment that provides direct access to the currently active
    * data within this memory segment. The returned segment is automatically sliced
    * to contain only the active bytes region, starting at
    * {@link MemoryWindow#activeBytesStart()} with a byte size of
    * {@link MemoryWindow#activeBytesLength()}.
    * </p>
    * 
    * <pre>{@code
    * Original Segment: [headroom:100][active:500][tailroom:400]
    * Returned Segment: [active:500]  // Pre-sliced to active region
    * }</pre>
    * 
    * @return a MemorySegment representing only the active bytes region
    * @throws IllegalStateException if this memory view is closed or invalid
    * 
    * @see #asByteBuffer() for ByteBuffer access
    * @see #asMemorySegmentAt(long) for position-based access across chain
    * @see MemoryWindow#activeBytesLength() for segment size
    */
   MemorySegment asMemorySegment();

   /**
    * Returns the MemorySegment containing the specified position within the memory chain.
    * 
    * <p>
    * This method provides position-based access across the entire memory chain,
    * automatically locating the correct segment that contains the specified byte
    * position. The position is relative to the start of the chain (position 0 is
    * the first byte of the first segment's active bytes).
    * </p>
    * 
    * <h3>Position Calculation Example</h3>
    * <pre>{@code
    * Chain Structure:
    * [Segment1: 1000 bytes] → [Segment2: 500 bytes] → [Segment3: 800 bytes]
    * 
    * Position Mapping:
    * • Position 0-999:    Returns Segment1
    * • Position 1000-1499: Returns Segment2  
    * • Position 1500-2299: Returns Segment3
    * • Position ≥2300:     Throws IllegalArgumentException
    * }</pre>
    * 
    * <p>
    * The returned MemorySegment is sliced to start at the requested position
    * within its containing segment, extending to the end of that segment's
    * active bytes.
    * </p>
    * 
    * @param position the byte position from the start of the chain (0-based)
    * @return the MemorySegment containing the specified position, sliced from
    *         that position to the segment's end
    * @throws IllegalArgumentException if position is negative or exceeds
    *         {@link MemoryWindow#totalActiveBytes()}
    * @throws IllegalStateException if this memory view is closed or invalid
    * 
    * @see #seekSegment(long) to get the containing Memory object
    * @see MemoryWindow#totalActiveBytes() for chain bounds
    */
   MemorySegment asMemorySegmentAt(long position);

   /**
    * Checks whether there is a next Memory segment in the chain.
    * 
    * <p>
    * This method provides a convenient way to test for chain continuation without
    * actually retrieving the next segment. It's particularly useful in conditional
    * processing scenarios or when building iterators.
    * </p>
    * 
    * <pre>{@code
    * if (memory.hasNextSegment()) {
    *     // Safe to call nextSegment()
    *     Memory next = memory.nextSegment();
    * }
    * }</pre>
    * 
    * @return {@code true} if {@link #nextSegment()} would return a non-null value,
    *         {@code false} if this is the last segment in the chain
    * @throws IllegalStateException if this memory view is closed or invalid
    * 
    * @see #nextSegment() to retrieve the actual next segment
    */
   boolean hasNextSegment();

   /**
    * Checks if this memory view represents a null or invalid memory reference.
    * 
    * <p>
    * A memory view is considered null in the following cases:
    * </p>
    * <ul>
    * <li>The underlying MemorySegment is {@code null}</li>
    * <li>The MemorySegment's address is 0 (representing a native NULL pointer)</li>
    * <li>The view has been explicitly closed or invalidated</li>
    * </ul>
    * 
    * <pre>{@code
    * // Safe memory access pattern
    * if (!memory.isNull()) {
    *     // Safe to access memory content
    *     ByteBuffer buffer = memory.asByteBuffer();
    *     processBuffer(buffer);
    * }
    * }</pre>
    * 
    * <p>
    * This check is essential when working with:
    * </p>
    * <ul>
    * <li>Native memory allocations that may fail</li>
    * <li>Optional memory references in data structures</li>
    * <li>Memory returned from native functions</li>
    * </ul>
    * 
    * @return {@code true} if this represents a null or invalid memory reference,
    *         {@code false} if the memory is valid and accessible
    * 
    * @see #isPointer() for checking zero-sized memory references
    * @see Memory#isNull(MemorySegment) for static null checking
    */
   boolean isNull();

   /**
    * Checks if this memory represents a pointer (zero-sized memory reference).
    * 
    * <p>
    * A memory view is considered a pointer when the underlying MemorySegment has
    * a byte size of zero. This typically occurs when:
    * </p>
    * <ul>
    * <li>Working with native pointers that haven't been dereferenced</li>
    * <li>Memory-mapped structures with pointer fields</li>
    * <li>Addresses returned from native functions without size information</li>
    * </ul>
    * 
    * <pre>{@code
    * // Example: Native pointer handling
    * Memory ptr = getNativePointer();
    * if (ptr.isPointer()) {
    *     // Cannot directly access data - need to reinterpret with size
    *     Memory data = ptr.reinterpret(expectedSize);
    *     // Now can access data
    * }
    * }</pre>
    * 
    * <p>
    * <strong>Important:</strong> Pointer memory cannot be directly accessed for
    * data operations. Attempting to read or write to a pointer will result in
    * exceptions. The pointer must first be reinterpreted with a proper size.
    * </p>
    * 
    * @return {@code true} if this represents a pointer (zero-sized memory),
    *         {@code false} if the memory has a positive size
    * @throws IllegalStateException if this memory view is closed or invalid
    * 
    * @see #isNull() for checking null memory references
    * @see MemoryWindow#segmentSize() for actual memory size
    */
   boolean isPointer();

   /**
    * Returns the next Memory segment in the chain, or {@code null} if this is the last segment.
    * 
    * <p>
    * Memory chains form a singly-linked list structure where each segment maintains
    * a reference to the next. This method provides the primary mechanism for sequential
    * traversal through all segments in a chain.
    * </p>
    * 
    * <h3>Traversal Pattern</h3>
    * <pre>{@code
    * // Process entire chain
    * Memory current = chainHead;
    * while (current != null) {
    *     processSegment(current);
    *     current = current.nextSegment();
    * }
    * 
    * // Or using for-loop style
    * for (Memory seg = chainHead; seg != null; seg = seg.nextSegment()) {
    *     processSegment(seg);
    * }
    * }</pre>
    * 
    * <p>
    * <strong>Note:</strong> The chain structure should not be modified during
    * traversal without proper synchronization.
    * </p>
    * 
    * @return the next Memory in the chain, or {@code null} if none exists
    * @throws IllegalStateException if this memory view is closed or invalid
    * 
    * @see #hasNextSegment() to check without retrieving
    * @see #seekSegment(long) for position-based navigation
    * @see MemoryWindow#segmentCount() for total segments in chain
    */
   Memory nextSegment();

   /**
    * Locates and returns the Memory segment containing the specified position within the chain.
    * 
    * <p>
    * This method performs efficient navigation through the memory chain to find the
    * segment that contains the specified byte position. Unlike {@link #asMemorySegmentAt(long)},
    * which returns a MemorySegment view, this returns the complete Memory object with
    * all its capabilities (bounds management, reference counting, etc.).
    * </p>
    * 
    * <h3>Position Resolution</h3>
    * <pre>{@code
    * Chain: [Seg1: 1000] → [Seg2: 500] → [Seg3: 800]
    * 
    * seekSegment(0)    returns Seg1  // First byte of chain
    * seekSegment(999)  returns Seg1  // Last byte of Seg1
    * seekSegment(1000) returns Seg2  // First byte of Seg2
    * seekSegment(1250) returns Seg2  // Middle of Seg2
    * seekSegment(2299) returns Seg3  // Last byte of chain
    * }</pre>
    * 
    * <p>
    * <strong>Performance Note:</strong> This method may cache navigation state
    * for improved performance on sequential access patterns.
    * </p>
    * 
    * @param position the byte position from the start of the chain (0-based)
    * @return the Memory segment containing the specified position
    * @throws IllegalArgumentException if position is negative or exceeds
    *         {@link MemoryWindow#totalActiveBytes()}
    * @throws IllegalStateException if this memory view is closed or invalid
    * 
    * @see #asMemorySegmentAt(long) for direct MemorySegment access
    * @see MemoryWindow#totalActiveBytes() for valid position range
    */
   Memory seekSegment(long position);
}
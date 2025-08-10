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

/**
 * Memory implementation with mutable data bounds within fixed memory boundaries.
 * 
 * <p>MemorySlice provides a Memory implementation that maintains fixed memory bounds
 * (inherited from AbstractMemory) while allowing dynamic adjustment of data bounds
 * within those limits. This enables efficient buffer-like operations without memory
 * allocation, making it ideal for streaming data processing and protocol parsing
 * where the active data region needs to be adjusted independently of the total
 * memory capacity.</p>
 * 
 * <h2>Key Features</h2>
 * <ul>
 *   <li><strong>Fixed Memory Bounds:</strong> Immutable capacity and addressable region (from AbstractMemory)</li>
 *   <li><strong>Mutable Data Bounds:</strong> Independent, adjustable data start and end positions</li>
 *   <li><strong>Zero Copy:</strong> No memory allocation during bound adjustments</li>
 *   <li><strong>Non-Pooled:</strong> Simple reference counting without pool integration</li>
 * </ul>
 * 
 * <h2>Memory Layout and Bounds Relationship</h2>
 * <p>MemorySlice maintains two independent sets of bounds within the underlying memory:</p>
 * <pre>{@code
 * |-------- Fixed Memory Bounds (AbstractMemory) --------|
 * |  gap  |------- Data Bounds (MemorySlice) -------|  gap  |
 *        ↑                                        ↑
 * memoryDataOffset                        memoryDataEnd
 * 
 * Bounds Relationship:
 * memoryOffset() ≤ memoryDataOffset ≤ memoryDataEnd ≤ memoryEnd()
 * 
 * Space Calculations:
 * - Leading Gap  = memoryDataOffset - memoryOffset()
 * - Data Region  = memoryDataEnd - memoryDataOffset  
 * - Trailing Gap = memoryEnd() - memoryDataEnd
 * - Total Capacity = memoryEnd() - memoryOffset()
 * }</pre>
 * 
 * <h2>Common Usage Patterns</h2>
 * 
 * <h3>Buffer Position Management</h3>
 * <pre>{@code
 * // Create slice with initial data bounds
 * MemorySlice slice = new MemorySlice(segment, 0, 1024, 0, 0);
 * 
 * // Extend data region as data is received
 * slice.memoryDataEnd(bytesReceived);
 * 
 * // Advance start position as data is consumed
 * int processed = processData(slice.asByteBuffer());
 * slice.memoryDataOffset(slice.memoryDataOffset() + processed);
 * }</pre>
 * 
 * <h3>Protocol Header Parsing</h3>
 * <pre>{@code
 * // Position slice at specific protocol header within packet
 * slice.memoryDataOffset(packetOffset + 14); // Skip Ethernet header
 * slice.memoryDataEnd(slice.memoryDataOffset() + 20); // IP header size
 * 
 * // Access IP header data
 * ByteBuffer ipHeader = slice.asByteBuffer();
 * MemorySegment headerSegment = slice.asMemorySegment();
 * }</pre>
 * 
 * <h3>Windowing Operations</h3>
 * <pre>{@code
 * // Create sliding window over large data buffer
 * MemorySlice window = new MemorySlice(largeBuffer, 0, largeBuffer.byteSize(), 0, windowSize);
 * 
 * while (window.memoryDataEnd() < window.memoryEnd()) {
 *     processWindow(window.asByteBuffer());
 *     
 *     // Slide window forward
 *     window.memoryDataOffset(window.memoryDataOffset() + stepSize);
 *     window.memoryDataEnd(Math.min(window.memoryDataEnd() + stepSize, window.memoryEnd()));
 * }
 * }</pre>
 * 
 * <h2>Performance Characteristics</h2>
 * <ul>
 *   <li><strong>Construction:</strong> O(1) - Field assignment and validation</li>
 *   <li><strong>Bounds Adjustment:</strong> O(1) - Simple field updates with validation</li>
 *   <li><strong>Memory Access:</strong> O(1) - Direct delegation to AbstractMemory</li>
 *   <li><strong>Memory Footprint:</strong> AbstractMemory + 2 long fields (16 bytes)</li>
 * </ul>
 * 
 * <h2>Thread Safety</h2>
 * <p>MemorySlice provides the same thread safety guarantees as AbstractMemory for
 * reference counting and general operations. However, data bound modifications are
 * <strong>not synchronized</strong>. Applications requiring concurrent access to
 * data bounds must provide external synchronization.</p>
 * 
 * <h2>Comparison with Other Implementations</h2>
 * <table border="1">
 * <tr><th>Feature</th><th>MemorySlice</th><th>MemoryWrapper</th><th>MemoryBuffer</th></tr>
 * <tr><td>Memory Bounds</td><td>Fixed</td><td>Fixed</td><td>Fixed</td></tr>
 * <tr><td>Data Bounds</td><td>Mutable</td><td>Fixed</td><td>Mutable + synchronized</td></tr>
 * <tr><td>Additional Fields</td><td>2 (data bounds)</td><td>0</td><td>3 (data + capacity + pool)</td></tr>
 * <tr><td>Pool Support</td><td>No</td><td>No</td><td>Yes</td></tr>
 * <tr><td>Synchronization</td><td>None</td><td>None needed</td><td>Data bounds only</td></tr>
 * <tr><td>Primary Use Case</td><td>Windowing</td><td>Simple wrapping</td><td>Buffer management</td></tr>
 * </table>
 * 
 * <h2>When to Use MemorySlice</h2>
 * <ul>
 *   <li><strong>Data Windowing:</strong> When you need to adjust the active data region within fixed memory</li>
 *   <li><strong>Protocol Parsing:</strong> When parsing headers at different offsets within packets</li>
 *   <li><strong>Stream Processing:</strong> When consuming data incrementally from buffers</li>
 *   <li><strong>Non-Pooled Scenarios:</strong> When you don't need memory pool integration</li>
 *   <li><strong>Simple Threading:</strong> When external synchronization is acceptable</li>
 * </ul>
 * 
 * <h2>Alternatives</h2>
 * <ul>
 *   <li><strong>MemoryWrapper:</strong> When bounds never change (lighter weight)</li>
 *   <li><strong>MemoryBuffer:</strong> When you need pool integration and synchronized bounds</li>
 *   <li><strong>MemoryProxy:</strong> When you need to rebind to different memory locations</li>
 * </ul>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 * @see AbstractMemory for inherited functionality
 * @see MemoryWrapper for immutable bounds
 * @see MemoryBuffer for poolable buffer management
 */
final class MemorySlice extends AbstractMemory {

    /**
     * The absolute starting offset of the current data region (mutable).
     * 
     * <p>This field defines where the active data begins within the fixed
     * memory bounds. It can be adjusted to reflect data consumption or
     * to position the slice at different data regions.</p>
     */
    private long memoryDataOffset;

    /**
     * The absolute ending offset of the current data region (mutable, exclusive).
     * 
     * <p>This field defines where the active data ends within the fixed
     * memory bounds. It can be adjusted to reflect data production or
     * to resize the active data window.</p>
     */
    private long memoryDataEnd;

    /**
     * Constructs a MemorySlice with specified memory and data bounds.
     * 
     * <p>Creates a memory slice with fixed memory boundaries (managed by AbstractMemory)
     * and initial data boundaries that can be adjusted later. The memory bounds define
     * the total addressable region, while data bounds define the currently active
     * portion within that region.</p>
     * 
     * <p><strong>Bounds Validation:</strong> The constructor validates that:</p>
     * <ul>
     *   <li>Memory bounds are valid (validated by AbstractMemory constructor)</li>
     *   <li>memoryOffset() ≤ memoryDataOffset ≤ memoryDataEnd ≤ memoryEnd()</li>
     * </ul>
     * 
     * <p><strong>Initial State:</strong> The slice starts with reference count = 1
     * and the specified data bounds. Data bounds can be modified after construction
     * using {@link #memoryDataOffset(long)} and {@link #memoryDataEnd(long)}.</p>
     * 
     * @param memorySegment the backing MemorySegment, must not be null
     * @param memoryOffset the starting offset of the memory region (inclusive)
     * @param memoryEnd the ending offset of the memory region (exclusive)
     * @param memoryDataOffset the initial starting offset of the data region (inclusive)
     * @param memoryDataEnd the initial ending offset of the data region (exclusive)
     * @throws NullPointerException if memorySegment is null
     * @throws IndexOutOfBoundsException if memory bounds are invalid
     * @throws IllegalArgumentException if data bounds are invalid
     * 
     * @see AbstractMemory#AbstractMemory(MemorySegment, long, long) for memory bounds validation
     */
    public MemorySlice(MemorySegment memorySegment, long memoryOffset, long memoryEnd,
            long memoryDataOffset, long memoryDataEnd) {
        super(memorySegment, memoryOffset, memoryEnd);
        
        // Validate data bounds against memory bounds
        if (memoryDataOffset < memoryOffset || memoryDataOffset > memoryEnd) {
            throw new IllegalArgumentException("memoryDataOffset out of bounds: " + memoryDataOffset 
                + " (valid range: " + memoryOffset + " to " + memoryEnd + ")");
        }
        if (memoryDataEnd < memoryDataOffset || memoryDataEnd > memoryEnd) {
            throw new IllegalArgumentException("memoryDataEnd out of bounds: " + memoryDataEnd 
                + " (valid range: " + memoryDataOffset + " to " + memoryEnd + ")");
        }
        
        this.memoryDataOffset = memoryDataOffset;
        this.memoryDataEnd = memoryDataEnd;
    }

    /**
     * Returns the absolute ending offset of the current data region.
     * 
     * <p>This method overrides AbstractMemory to return the slice-specific data end
     * rather than the memory end. This represents the first byte position beyond
     * the currently active data within the slice.</p>
     * 
     * @return the absolute data ending offset (exclusive bound)
     * @throws IllegalStateException if this memory slice is closed
     * 
     * @see #memoryDataEnd(long) to modify the data end position
     * @see #memoryDataOffset() for the data start position
     */
    @Override
    public long memoryDataEnd() {
        checkNotClosed();
        return memoryDataEnd;
    }

    /**
     * Returns the absolute starting offset of the current data region.
     * 
     * <p>This method overrides AbstractMemory to return the slice-specific data offset
     * rather than the memory offset. This represents the first byte of currently
     * active data within the slice.</p>
     * 
     * @return the absolute data starting offset (inclusive bound)
     * @throws IllegalStateException if this memory slice is closed
     * 
     * @see #memoryDataOffset(long) to modify the data start position
     * @see #memoryDataEnd() for the data end position
     */
    @Override
    public long memoryDataOffset() {
        checkNotClosed();
        return memoryDataOffset;
    }

    /**
     * Sets the absolute starting offset of the current data region.
     * 
     * <p>This method allows dynamic adjustment of where the active data region begins
     * within the fixed memory bounds. The operation validates that the new offset
     * falls within acceptable bounds and does not invalidate the current data end.</p>
     * 
     * <p><strong>Bounds Validation:</strong> The new offset must satisfy:</p>
     * <ul>
     *   <li>{@code memoryOffset() ≤ newOffset ≤ memoryDataEnd()}</li>
     * </ul>
     * 
     * <p><strong>Common Usage:</strong> Advance the data offset after consuming
     * data from the beginning of the slice:</p>
     * <pre>{@code
     * int consumed = processData(slice.asByteBuffer());
     * slice.memoryDataOffset(slice.memoryDataOffset() + consumed);
     * }</pre>
     * 
     * @param newOffset the new data starting offset
     * @return the new data offset (same as parameter)
     * @throws IllegalStateException if this memory slice is closed
     * @throws IllegalArgumentException if newOffset is out of bounds
     * 
     * @see #memoryDataOffset() to query current position
     * @see #memoryDataEnd(long) to modify the data end position
     */
    public long memoryDataOffset(long newOffset) {
        checkNotClosed();
        if (newOffset < super.memoryOffset() || newOffset > memoryDataEnd) {
            throw new IllegalArgumentException("newOffset out of bounds: " + newOffset 
                + " (valid range: " + super.memoryOffset() + " to " + memoryDataEnd + ")");
        }
        this.memoryDataOffset = newOffset;
        return memoryDataOffset;
    }

    /**
     * Sets the absolute ending offset of the current data region.
     * 
     * <p>This method allows dynamic adjustment of where the active data region ends
     * within the fixed memory bounds. The operation validates that the new end
     * falls within acceptable bounds and does not invalidate the current data offset.</p>
     * 
     * <p><strong>Bounds Validation:</strong> The new end must satisfy:</p>
     * <ul>
     *   <li>{@code memoryDataOffset() ≤ newEnd ≤ memoryEnd()}</li>
     * </ul>
     * 
     * <p><strong>Common Usage:</strong> Extend the data region after writing
     * new data to the slice:</p>
     * <pre>{@code
     * int written = writeData(slice, newData);
     * slice.memoryDataEnd(slice.memoryDataEnd() + written);
     * }</pre>
     * 
     * @param newEnd the new data ending offset
     * @return the new data end (same as parameter)
     * @throws IllegalStateException if this memory slice is closed
     * @throws IllegalArgumentException if newEnd is out of bounds
     * 
     * @see #memoryDataEnd() to query current end position
     * @see #memoryDataOffset(long) to modify the data start position
     */
    public long memoryDataEnd(long newEnd) {
        checkNotClosed();
        if (newEnd < memoryDataOffset || newEnd > super.memoryEnd()) {
            throw new IllegalArgumentException("newEnd out of bounds: " + newEnd 
                + " (valid range: " + memoryDataOffset + " to " + super.memoryEnd() + ")");
        }
        this.memoryDataEnd = newEnd;
        return memoryDataEnd;
    }
}
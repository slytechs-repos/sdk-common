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
 * Poolable memory buffer implementation with full buffer management
 * capabilities.
 * 
 * <p>
 * MemoryBuffer provides a comprehensive Memory implementation designed for
 * high-performance buffer management scenarios, particularly in memory pooling
 * environments. It combines mutable data bounds with automatic pool
 * integration, making it ideal for streaming data processing, network packet
 * handling, and other scenarios requiring efficient buffer reuse.
 * </p>
 * 
 * <h2>Key Features</h2>
 * <ul>
 * <li><strong>Pool Integration:</strong> Native support for memory pool
 * allocation and release</li>
 * <li><strong>Mutable Data Bounds:</strong> Dynamic adjustment of active data
 * region</li>
 * <li><strong>Buffer-like Operations:</strong> Leading/trailing space
 * management</li>
 * <li><strong>Thread-Safe Bounds:</strong> Synchronized data bound
 * modifications</li>
 * <li><strong>Automatic Cleanup:</strong> Pool return on reference count
 * reaching zero</li>
 * </ul>
 * 
 * <h2>Memory Layout and Space Management</h2>
 * <p>
 * MemoryBuffer maintains sophisticated space tracking for efficient buffer
 * operations:
 * </p>
 * 
 * <pre>{@code
 * |------ Total Memory Capacity ------|
 * | leading |---- Data Region ----| trailing |
 * |  space  |                    |   space   |
 *          ↑                    ↑
 *   memoryDataOffset      memoryDataEnd
 * 
 * Space calculations:
 * - Leading Space  = memoryDataOffset - memoryOffset
 * - Data Length    = memoryDataEnd - memoryDataOffset  
 * - Trailing Space = memoryEnd - memoryDataEnd
 * - Total Capacity = memoryEnd - memoryOffset
 * }</pre>
 * 
 * <h2>Pool Integration Pattern</h2>
 * <p>
 * MemoryBuffer is designed to work seamlessly with {@link MemoryPool}:
 * </p>
 * 
 * <pre>{@code
 * // Pool allocation - buffer starts with refcount = 1
 * MemoryBuffer buffer = pool.allocate();
 * try {
 * 	// Fill buffer with data
 * 	buffer.memoryDataEnd(bytesReceived);
 * 
 * 	// Process data
 * 	processData(buffer.asByteBuffer());
 * 
 * 	// Advance position as data is consumed
 * 	buffer.memoryDataOffset(buffer.memoryDataOffset() + bytesProcessed);
 * 
 * } finally {
 * 	// Automatic pool return when refcount reaches 0
 * 	buffer.decrementRef();
 * }
 * }</pre>
 * 
 * <h2>Buffer Management Operations</h2>
 * 
 * <h3>Space Utilization</h3>
 * 
 * <pre>{@code
 * // Check available space for data expansion
 * if (buffer.hasMemoryTrailingSpace()) {
 * 	long available = buffer.memoryTrailingSpace();
 * 	// Extend data region
 * 	buffer.memoryDataEnd(buffer.memoryDataEnd() + Math.min(available, needed));
 * }
 * 
 * // Reclaim leading space after consumption
 * if (buffer.hasMemoryLeadingSpace() && buffer.memoryLeadingSpace() > threshold) {
 * 	// Compact buffer - move data to start
 * 	compactBuffer(buffer);
 * 	buffer.memoryDataOffset(buffer.memoryOffset());
 * }
 * }</pre>
 * 
 * <h3>Streaming Data Processing</h3>
 * 
 * <pre>{@code
 * // Receive data into buffer
 * MemoryBuffer receiveBuffer = pool.allocate();
 * int received = channel.read(receiveBuffer.asByteBuffer());
 * receiveBuffer.memoryDataEnd(receiveBuffer.memoryDataOffset() + received);
 * 
 * // Process complete packets
 * while (receiveBuffer.hasMemoryDataRemaining()) {
 * 	int packetLength = parsePacketLength(receiveBuffer);
 * 	if (receiveBuffer.memoryDataLength() >= packetLength) {
 * 		processPacket(receiveBuffer, packetLength);
 * 		receiveBuffer.memoryDataOffset(receiveBuffer.memoryDataOffset() + packetLength);
 * 	} else {
 * 		break; // Wait for more data
 * 	}
 * }
 * }</pre>
 * 
 * <h2>Performance Characteristics</h2>
 * <ul>
 * <li><strong>Allocation:</strong> O(1) from pool (pre-allocated segments)</li>
 * <li><strong>Bounds Adjustment:</strong> O(1) synchronized operations</li>
 * <li><strong>Space Queries:</strong> O(1) calculation-based results</li>
 * <li><strong>Pool Return:</strong> O(1) lock-free list operations</li>
 * </ul>
 * 
 * <h2>Thread Safety</h2>
 * <p>
 * MemoryBuffer provides thread-safe operations for:
 * </p>
 * <ul>
 * <li><strong>Data Bound Modification:</strong> Synchronized setters prevent
 * inconsistent states</li>
 * <li><strong>Reference Counting:</strong> Atomic operations from
 * AbstractMemory</li>
 * <li><strong>Pool Operations:</strong> Thread-safe allocation and release</li>
 * </ul>
 * 
 * <p>
 * <strong>Note:</strong> While bound modifications are synchronized,
 * applications should coordinate access patterns to avoid performance
 * bottlenecks in high-concurrency scenarios.
 * </p>
 * 
 * <h2>Comparison with Other Implementations</h2>
 * <table border="1">
 * <tr>
 * <th>Feature</th>
 * <th>MemoryBuffer</th>
 * <th>MemorySlice</th>
 * <th>MemoryWrapper</th>
 * </tr>
 * <tr>
 * <td>Pool Support</td>
 * <td>Yes</td>
 * <td>No</td>
 * <td>No</td>
 * </tr>
 * <tr>
 * <td>Data Bounds</td>
 * <td>Mutable</td>
 * <td>Mutable</td>
 * <td>Fixed</td>
 * </tr>
 * <tr>
 * <td>Space Management</td>
 * <td>Full</td>
 * <td>Basic</td>
 * <td>None</td>
 * </tr>
 * <tr>
 * <td>Auto Cleanup</td>
 * <td>Pool return</td>
 * <td>Simple close</td>
 * <td>Simple close</td>
 * </tr>
 * <tr>
 * <td>Use Case</td>
 * <td>Buffer mgmt</td>
 * <td>Windowing</td>
 * <td>Simple wrapping</td>
 * </tr>
 * </table>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 * @see MemoryPool for pool management
 * @see MemoryPool.MemoryPoolable for poolable interface
 * @see MemorySlice for non-pooled mutable bounds
 */
public class MemoryBuffer extends AbstractMemory implements MemoryPool.MemoryPoolable {

	/**
	 * The absolute starting offset of the current data region (mutable).
	 */
	private volatile long memoryDataOffset;

	/**
	 * The absolute ending offset of the current data region (mutable, exclusive).
	 */
	private volatile long memoryDataEnd;

	/**
	 * The fixed capacity of this memory buffer.
	 */
	private final long memoryCapacity;

	/**
	 * The memory pool that owns this buffer, or null if not pooled.
	 */
	private final MemoryPool<MemoryBuffer> owningPool;

	/**
	 * Constructs a pooled MemoryBuffer wrapping a MemorySegment region.
	 * 
	 * <p>
	 * This constructor is typically called by {@link MemoryPool} implementations
	 * during buffer allocation. The buffer starts with data bounds set to cover the
	 * entire memory region, and with reference count = 1.
	 * </p>
	 * 
	 * <p>
	 * <strong>Pool Integration:</strong> When the reference count reaches 0, this
	 * buffer will be automatically returned to the owning pool for reuse.
	 * </p>
	 * 
	 * @param owningPool    the owning MemoryPool, or {@code null} if not pooled
	 * @param memorySegment the backing MemorySegment, must not be null
	 * @param offset        the starting offset within the segment (inclusive)
	 * @param length        the length of the memory region
	 * @throws NullPointerException     if memorySegment is null
	 * @throws IllegalArgumentException if offset or length is invalid
	 */
	public MemoryBuffer(MemoryPool<MemoryBuffer> owningPool, MemorySegment memorySegment,
			long offset, long length) {
		super(memorySegment, offset, offset + length);
		this.memoryDataOffset = super.memoryOffset();
		this.memoryDataEnd = super.memoryEnd();
		this.memoryCapacity = length;
		this.owningPool = owningPool;

		asByteBuffer(); // Preallocates byte ByteBuffer
	}

	/**
	 * Constructs a non-pooled MemoryBuffer wrapping the entire Memory object.
	 * 
	 * <p>
	 * Creates a buffer that covers the complete capacity of the provided Memory.
	 * This buffer will not be returned to any pool when closed, and increments the
	 * reference count of the wrapped Memory.
	 * </p>
	 * 
	 * @param memory the backing Memory object, must not be null
	 * @throws NullPointerException if memory is null
	 */
	public MemoryBuffer(Memory memory) {
		this(memory, 0, memory.memoryCapacity());
	}

	/**
	 * Constructs a non-pooled MemoryBuffer wrapping a slice of the Memory object.
	 * 
	 * <p>
	 * Creates a buffer that covers a specific region within the provided Memory.
	 * This buffer will not be returned to any pool when closed, and increments the
	 * reference count of the wrapped Memory to ensure validity.
	 * </p>
	 * 
	 * @param memory the backing Memory object, must not be null
	 * @param offset the starting offset within the memory (inclusive)
	 * @param length the length of the memory region
	 * @throws NullPointerException     if memory is null
	 * @throws IllegalArgumentException if offset or length is invalid
	 */
	public MemoryBuffer(Memory memory, long offset, long length) {
		super(memory.asMemorySegment(), memory.memoryOffset() + offset,
				memory.memoryOffset() + offset + length);
		if (offset < 0 || length < 0 || offset + length > memory.memoryCapacity()) {
			throw new IllegalArgumentException("invalid offset or length: offset="
					+ offset + ", length=" + length + ", capacity=" + memory.memoryCapacity());
		}
		this.memoryDataOffset = super.memoryOffset();
		this.memoryDataEnd = super.memoryEnd();
		this.memoryCapacity = length;
		this.owningPool = null;
		memory.incrementRef();

		asByteBuffer(); // Preallocates byte ByteBuffer
	}

	/**
	 * Returns the owning memory pool for this buffer.
	 * 
	 * <p>
	 * This method fulfills the {@link MemoryPool.MemoryPoolable} contract, enabling
	 * the pool to properly manage this buffer's lifecycle.
	 * </p>
	 * 
	 * @return the owning MemoryPool, or {@code null} if this buffer is not pooled
	 */
	@Override
	public MemoryPool<MemoryBuffer> getOwningPool() {
		return owningPool;
	}

	/**
	 * Returns the fixed capacity of this memory buffer.
	 * 
	 * <p>
	 * The capacity represents the total addressable space within this buffer, which
	 * remains constant throughout the buffer's lifetime. This value defines the
	 * maximum bounds for all data operations.
	 * </p>
	 * 
	 * @return the total capacity in bytes (always ≥ 0)
	 * @throws IllegalStateException if this buffer is closed
	 */
	@Override
	public long memoryCapacity() {
		checkNotClosed();
		return memoryCapacity;
	}

	/**
	 * Returns the absolute starting offset of the current data region.
	 * 
	 * <p>
	 * This represents the first byte of currently active data within the buffer,
	 * similar to a ByteBuffer's position. The data region can be adjusted to
	 * reflect consumption of data from the beginning of the buffer.
	 * </p>
	 * 
	 * @return the absolute data starting offset (inclusive bound)
	 * @throws IllegalStateException if this buffer is closed
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
	 * <p>
	 * This method allows dynamic adjustment of where active data begins within the
	 * buffer, enabling efficient data consumption patterns. The operation is
	 * synchronized to ensure thread-safe modification.
	 * </p>
	 * 
	 * <p>
	 * <strong>Bounds Validation:</strong> The new offset must satisfy:
	 * </p>
	 * <ul>
	 * <li>{@code memoryOffset() ≤ newOffset ≤ memoryDataEnd()}</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Common Usage:</strong> Advance the data offset after consuming data
	 * from the beginning of the buffer:
	 * </p>
	 * 
	 * <pre>{@code
	 * int consumed = processData(buffer.asByteBuffer());
	 * buffer.memoryDataOffset(buffer.memoryDataOffset() + consumed);
	 * }</pre>
	 * 
	 * @param newOffset the new data starting offset
	 * @return the new data offset (same as parameter)
	 * @throws IllegalStateException    if this buffer is closed
	 * @throws IllegalArgumentException if newOffset is out of bounds
	 * 
	 * @see #memoryDataOffset() to query current position
	 * @see #hasMemoryDataRemaining() to check for remaining data
	 */
	public synchronized long memoryDataOffset(long newOffset) {
		checkNotClosed();
		if (newOffset < super.memoryOffset() || newOffset > memoryDataEnd) {
			throw new IllegalArgumentException("newOffset out of bounds: " + newOffset
					+ " (valid range: " + super.memoryOffset() + " to " + memoryDataEnd + ")");
		}
		this.memoryDataOffset = newOffset;
		return memoryDataOffset;
	}

	/**
	 * Returns the absolute ending offset of the current data region.
	 * 
	 * <p>
	 * This represents the first byte position beyond the currently active data,
	 * similar to a ByteBuffer's limit. The data region can be adjusted to reflect
	 * the addition of new data to the buffer.
	 * </p>
	 * 
	 * @return the absolute data ending offset (exclusive bound)
	 * @throws IllegalStateException if this buffer is closed
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
	 * Sets the absolute ending offset of the current data region.
	 * 
	 * <p>
	 * This method allows dynamic adjustment of where active data ends within the
	 * buffer, enabling efficient data production patterns. The operation is
	 * synchronized to ensure thread-safe modification.
	 * </p>
	 * 
	 * <p>
	 * <strong>Bounds Validation:</strong> The new end must satisfy:
	 * </p>
	 * <ul>
	 * <li>{@code memoryDataOffset() ≤ newEnd ≤ memoryEnd()}</li>
	 * </ul>
	 * 
	 * <p>
	 * <strong>Common Usage:</strong> Extend the data region after writing new data
	 * to the buffer:
	 * </p>
	 * 
	 * <pre>{@code
	 * int written = channel.read(buffer.asByteBuffer());
	 * buffer.memoryDataEnd(buffer.memoryDataEnd() + written);
	 * }</pre>
	 * 
	 * @param newEnd the new data ending offset
	 * @return the new data end (same as parameter)
	 * @throws IllegalStateException    if this buffer is closed
	 * @throws IllegalArgumentException if newEnd is out of bounds
	 * 
	 * @see #memoryDataEnd() to query current end position
	 * @see #hasMemoryTrailingSpace() to check for available space
	 */
	public synchronized long memoryDataEnd(long newEnd) {
		checkNotClosed();
		if (newEnd < memoryDataOffset || newEnd > super.memoryEnd()) {
			throw new IllegalArgumentException("newEnd out of bounds: " + newEnd
					+ " (valid range: " + memoryDataOffset + " to " + super.memoryEnd() + ")");
		}
		this.memoryDataEnd = newEnd;
		return memoryDataEnd;
	}

	/**
	 * Returns the length of the current data region.
	 * 
	 * <p>
	 * Calculates the amount of currently active data as
	 * {@code memoryDataEnd() - memoryDataOffset()}. This represents the usable data
	 * within the buffer, analogous to a ByteBuffer's remaining bytes.
	 * </p>
	 * 
	 * @return the current data length in bytes (always ≥ 0)
	 * @throws IllegalStateException if this buffer is closed
	 * 
	 * @see #hasMemoryDataRemaining() for a boolean check
	 * @see #memoryCapacity() for total buffer capacity
	 */
	@Override
	public long memoryDataLength() {
		checkNotClosed();
		return memoryDataEnd - memoryDataOffset;
	}

	/**
	 * Checks if there are remaining usable data bytes in this buffer.
	 * 
	 * <p>
	 * Provides a convenient boolean test for data availability, equivalent to
	 * {@code memoryDataLength() > 0}. Useful in loop conditions and conditional
	 * processing scenarios.
	 * </p>
	 * 
	 * @return {@code true} if there are remaining data bytes
	 * @throws IllegalStateException if this buffer is closed
	 * 
	 * @see #memoryDataLength() for the exact remaining count
	 */
	public boolean hasMemoryDataRemaining() {
		checkNotClosed();
		return memoryDataOffset < memoryDataEnd;
	}

	/**
	 * Returns the amount of unused space before the current data region.
	 * 
	 * <p>
	 * Calculates the leading space as {@code memoryDataOffset - memoryOffset}. This
	 * space represents previously consumed data or intentionally reserved space at
	 * the beginning of the buffer.
	 * </p>
	 * 
	 * <p>
	 * <strong>Buffer Optimization:</strong> Leading space can be reclaimed by
	 * moving data toward the beginning of the buffer and adjusting bounds.
	 * </p>
	 * 
	 * @return the leading space in bytes (always ≥ 0)
	 * @throws IllegalStateException if this buffer is closed
	 * 
	 * @see #hasMemoryLeadingSpace() for a boolean check
	 * @see #memoryTrailingSpace() for trailing space
	 */
	public long memoryLeadingSpace() {
		checkNotClosed();
		return memoryDataOffset - super.memoryOffset();
	}

	/**
	 * Checks if there is unused space before the current data region.
	 * 
	 * <p>
	 * Provides a convenient boolean test for leading space availability, equivalent
	 * to {@code memoryLeadingSpace() > 0}.
	 * </p>
	 * 
	 * @return {@code true} if there is leading space available
	 * @throws IllegalStateException if this buffer is closed
	 * 
	 * @see #memoryLeadingSpace() for the exact space amount
	 */
	public boolean hasMemoryLeadingSpace() {
		checkNotClosed();
		return memoryLeadingSpace() > 0;
	}

	/**
	 * Returns the amount of unused space after the current data region.
	 * 
	 * <p>
	 * Calculates the trailing space as {@code memoryEnd - memoryDataEnd}. This
	 * space represents available capacity for extending the data region without
	 * buffer reallocation.
	 * </p>
	 * 
	 * <p>
	 * <strong>Data Extension:</strong> Trailing space can be used to extend the
	 * data region when writing additional data to the buffer.
	 * </p>
	 * 
	 * @return the trailing space in bytes (always ≥ 0)
	 * @throws IllegalStateException if this buffer is closed
	 * 
	 * @see #hasMemoryTrailingSpace() for a boolean check
	 * @see #memoryLeadingSpace() for leading space
	 */
	public long memoryTrailingSpace() {
		checkNotClosed();
		return super.memoryEnd() - memoryDataEnd;
	}

	/**
	 * Checks if there is unused space after the current data region.
	 * 
	 * <p>
	 * Provides a convenient boolean test for trailing space availability,
	 * equivalent to {@code memoryTrailingSpace() > 0}. Useful for determining if
	 * the buffer can accept additional data.
	 * </p>
	 * 
	 * @return {@code true} if there is trailing space available
	 * @throws IllegalStateException if this buffer is closed
	 * 
	 * @see #memoryTrailingSpace() for the exact space amount
	 */
	public boolean hasMemoryTrailingSpace() {
		checkNotClosed();
		return memoryTrailingSpace() > 0;
	}

	/**
	 * Resets this buffer for reuse by a memory pool.
	 * 
	 * <p>
	 * This method resets the buffer to its initial state, setting the reference
	 * count to 1 and restoring data bounds to cover the entire memory region. This
	 * method is typically called by memory pools when preparing buffers for
	 * reallocation.
	 * </p>
	 * 
	 * <p>
	 * <strong>Pool Integration:</strong> This method is automatically called by
	 * {@link MemoryPool#allocate()} before returning buffers to applications.
	 * </p>
	 * 
	 * @throws IllegalStateException if refcount is not 0
	 */
	@Override
	protected void resetForReuse() {
		super.resetForReuse();
		memoryDataOffset = super.memoryOffset();
		memoryDataEnd = super.memoryEnd();
	}

	/**
	 * Closes this buffer, releasing it to the owning pool if present.
	 * 
	 * <p>
	 * This method performs cleanup operations and, for pooled buffers, triggers
	 * return to the originating pool for reuse. The operation ensures proper
	 * resource management and prevents memory leaks.
	 * </p>
	 * 
	 * <p>
	 * <strong>Automatic Pool Return:</strong> If this buffer was allocated from a
	 * pool, it will be automatically returned to that pool for reuse.
	 * </p>
	 * 
	 * @throws IllegalStateException if refcount is not 0
	 */
	@Override
	public void close() {
		super.close();
		if (owningPool != null) {
			owningPool.release(this);
		}
	}

	/**
	 * Returns a string representation of this MemoryBuffer.
	 * 
	 * <p>
	 * The string includes current data bounds, capacity, and reference count for
	 * debugging and monitoring purposes.
	 * </p>
	 * 
	 * @return a string representation of this buffer
	 */
	@Override
	public String toString() {
		return String.format("MemoryBuffer[dataOffset=%d, dataEnd=%d, capacity=%d, refCount=%d]",
				memoryDataOffset, memoryDataEnd, memoryCapacity, refCount());
	}
}
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
import java.util.function.Consumer;

import com.slytechs.jnet.core.api.memory.MemoryPool.MemoryPoolable;

/**
 * Control layer for buffer management with positioning, error handling, and
 * multi-segment support.
 * 
 * <p>
 * MemoryBuffer provides the control infrastructure for buffer-style operations,
 * following the java.nio.Buffer pattern. This abstract class manages buffer
 * state (position, limit, mark), error accumulation, and multi-segment chain
 * navigation. Concrete subclasses like MemoryByteBuffer build upon this
 * foundation to provide data access operations.
 * </p>
 * 
 * <h2>Architecture Overview</h2>
 * <p>
 * Following the java.nio pattern:
 * </p>
 * 
 * <pre>{@code
 * java.nio:           Our Design:
 * Buffer              MemoryBuffer        (control layer - THIS CLASS)
 *   ↓                   ↓
 * ByteBuffer          MemoryByteBuffer    (accessor layer)
 * }</pre>
 * 
 * <h2>Key Features</h2>
 * <ul>
 * <li><strong>Buffer State Management:</strong> Position, limit, mark, capacity
 * tracking</li>
 * <li><strong>Error Accumulation:</strong> Non-throwing operations with
 * deferred error handling</li>
 * <li><strong>Multi-Segment Support:</strong> Transparent chain navigation and
 * spanning detection</li>
 * <li><strong>Zero-Allocation Design:</strong> No object creation in critical
 * paths</li>
 * </ul>
 * 
 * <h2>Error Handling Philosophy</h2>
 * <p>
 * All operations are non-throwing to support high-performance scenarios:
 * </p>
 * 
 * <pre>{@code
 * buffer.put(header) // Never throws
 * 		.putLong(timestamp) // Accumulates errors
 * 		.put(payload) // Skips if error set
 * 		.orElseThrow(); // Throws accumulated error
 * }</pre>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 */
public abstract class MemoryBuffer extends AbstractMemory implements MemoryEditable, MemoryPoolable {

	// Developer Note: Buffer state management - similar to java.nio.Buffer
	// These track our position within the buffer for sequential operations
	private long position = 0; // Current position in buffer
	private long limit; // Upper bound for operations
	private long mark = -1; // Saved position for reset()

	// Developer Note: Error state management for non-throwing operations
	// This enables fluent chains to continue even when errors occur
	private volatile boolean hasError = false;
	private volatile BufferOperationException pendingError = null;

	// Developer Note: Multi-segment chain navigation cache
	// We cache the current segment to avoid repeated lookups during sequential
	// access
	private Memory currentSegment; // Current segment in chain
	private long currentSegmentOffset = 0; // Offset of current segment in chain

	// Developer Note: Performance metrics for monitoring
	// These help track error patterns in production without logs
	private final BufferMetrics metrics = new BufferMetrics();

	private final MemoryPool<? extends MemoryBuffer> owningPool;

	/**
	 * Constructs a MemoryBuffer with specified bounds.
	 * 
	 * @param memorySegment the backing memory segment
	 * @param memoryOffset  starting offset within segment
	 * @param memoryEnd     ending offset within segment (exclusive)
	 */
	protected MemoryBuffer(MemoryPool<? extends MemoryBuffer> owningPool, MemorySegment memorySegment,
			long memoryOffset, long memoryEnd) {
		super(memorySegment, memoryOffset, memoryEnd);
		this.owningPool = owningPool;
		this.limit = memoryEnd - memoryOffset;
		this.currentSegment = this;
	}

	// ==================== Core Error State Methods (4) ====================

	/**
	 * @see com.slytechs.jnet.core.api.memory.MemoryWindow#memoryDataOffset(long)
	 */
	@Override
	public long memoryDataOffset(long newOffset) {
		return currentSegment.memoryDataOffset(newOffset);
	}

	/**
	 * @see com.slytechs.jnet.core.api.memory.MemoryWindow#memoryDataEnd(long)
	 */
	@Override
	public long memoryDataEnd(long newEnd) {
		return currentSegment.memoryDataEnd(newEnd);
	}

	/**
	 * @see com.slytechs.jnet.core.api.memory.MemoryPool.MemoryPoolable#getOwningPool()
	 */
	@Override
	public MemoryPool<?> getOwningPool() {
		return owningPool;
	}

	/**
	 * Checks if this buffer has an accumulated error.
	 * 
	 * <p>
	 * When true, most operations become no-ops to maintain the fluent chain. The
	 * error state persists until explicitly cleared or thrown.
	 * </p>
	 * 
	 * @return true if an error has been accumulated
	 */
	public boolean hasError() {
		return hasError;
	}

	/**
	 * Returns the accumulated error if present.
	 * 
	 * <p>
	 * This method returns the first error that occurred in the operation chain.
	 * Subsequent errors are typically ignored to preserve the original failure
	 * context.
	 * </p>
	 * 
	 * @return the accumulated error, or null if no error
	 */
	public BufferOperationException getError() {
		return pendingError;
	}

	/**
	 * Clears any accumulated error state.
	 * 
	 * <p>
	 * Resets the buffer to a non-error state, allowing operations to proceed
	 * normally. This is useful for recovery scenarios where you want to continue
	 * using the buffer after handling an error.
	 * </p>
	 * 
	 * <pre>{@code
	 * if (buffer.hasError()) {
	 * 	handleError(buffer.getError());
	 * 	buffer.clearError(); // Reset for continued use
	 * }
	 * }</pre>
	 * 
	 * @return this buffer for method chaining
	 */
	public MemoryBuffer clearError() {
		this.hasError = false;
		this.pendingError = null;
		return this;
	}

	/**
	 * Throws the accumulated error if present. Clears the error state.
	 * 
	 * <p>
	 * This is typically the final call in a fluent chain, converting accumulated
	 * errors into thrown exceptions. If no error is present, returns normally.
	 * </p>
	 * 
	 * <pre>{@code
	 * buffer.putInt(header)
	 * 		.putLong(timestamp)
	 * 		.put(payload)
	 * 		.orElseThrow(); // Throws if any operation failed
	 * }</pre>
	 * 
	 * @return this buffer for method chaining
	 * @throws BufferOperationException if an error has been accumulated
	 */
	public MemoryBuffer orElseThrow() throws BufferOperationException {
		if (hasError && pendingError != null) {
			BufferOperationException toThrow = pendingError;
			clearError(); // Clear before throwing
			throw toThrow;
		}
		return this;
	}

	// ==================== Chain-Ending Error Handlers (2) ====================

	/**
	 * Handles accumulated error with the provided handler.
	 * 
	 * <p>
	 * This chain-ending method attempts to recover from an accumulated error. If
	 * the handler executes successfully (doesn't throw), the error is cleared. If
	 * the handler throws, the new exception replaces the current error.
	 * </p>
	 * 
	 * <pre>{@code
	 * buffer.tryInsertSpace(100, expansionHandler)
	 * 		.put(data)
	 * 		.onError((buf, error) -> {
	 * 			// Attempt recovery
	 * 			if (canRecover(error)) {
	 * 				performRecovery(buf);
	 * 				// No exception = success, error cleared
	 * 			} else {
	 * 				throw new BufferOperationException("Unrecoverable", error);
	 * 			}
	 * 		});
	 * }</pre>
	 * 
	 * Developer Note: This is a terminal operation for error handling chains. It
	 * either clears the error (on successful recovery) or replaces it.
	 * 
	 * @param handler the error handler to execute if error present
	 * @return this buffer for method chaining
	 */
	public MemoryBuffer onError(BufferErrorHandler handler) {
		if (hasError && pendingError != null) {
			try {
				handler.handle(this, pendingError);
				// Handler succeeded without throwing - clear error
				clearError();
			} catch (BufferOperationException e) {
				// Handler threw new/transformed error - replace current
				this.pendingError = e;
				// hasError remains true
			}
		}
		return this;
	}

	/**
	 * Monitors accumulated error without clearing it.
	 * 
	 * <p>
	 * This non-intrusive method allows observation of errors without affecting the
	 * error state. Perfect for logging, metrics, or debugging. The error remains
	 * accumulated after this call, allowing subsequent error handlers to process
	 * it.
	 * </p>
	 * 
	 * <pre>{@code
	 * buffer.tryInsertSpace(100, expansionHandler)
	 * 		.ifError(System.err::println) // Log error
	 * 		.ifError(metrics::recordError) // Update metrics
	 * 		.onError(recoveryHandler); // Handle error
	 * }</pre>
	 * 
	 * Developer Note: Named 'ifError' to follow Optional.ifPresent() pattern. This
	 * is purely observational - it never clears the error state.
	 * 
	 * @param errorConsumer the consumer to execute if error present
	 * @return this buffer for method chaining
	 */
	public MemoryBuffer ifError(Consumer<BufferOperationException> errorConsumer) {
		if (hasError && pendingError != null) {
			try {
				errorConsumer.accept(pendingError);
			} catch (Exception e) {
				// If the consumer throws, wrap and replace current error
				this.pendingError = new BufferOperationException(
						"Error monitor failed: " + e.getMessage(), e);
			}
		}
		return this;
	}

	// ==================== Buffer Positioning Methods ====================

	/**
	 * Returns the current position in this buffer.
	 * 
	 * <p>
	 * The position is the index of the next element to be read or written. Position
	 * is always between 0 and limit (inclusive).
	 * </p>
	 * 
	 * @return the current position
	 */
	public long position() {
		return position;
	}

	/**
	 * Sets this buffer's position.
	 * 
	 * <p>
	 * If the mark is defined and larger than the new position, it is discarded.
	 * Setting position beyond limit will cause subsequent operations to fail.
	 * </p>
	 * 
	 * Developer Note: We don't throw here to maintain non-throwing philosophy.
	 * Invalid positions will cause operations to accumulate errors instead.
	 * 
	 * @param newPosition the new position value
	 * @return this buffer for method chaining
	 */
	public MemoryBuffer position(long newPosition) {
		if (hasError)
			return this;

		if (newPosition < 0 || newPosition > limit) {
			setError(new BufferOperationException(
					"Position out of bounds: " + newPosition + " (limit=" + limit + ")"));
			return this;
		}

		this.position = newPosition;
		if (mark > position) {
			mark = -1; // Discard mark if it's beyond new position
		}

		// Update segment cache for new position
		updateCurrentSegment();
		return this;
	}

	/**
	 * Returns this buffer's limit.
	 * 
	 * <p>
	 * The limit is the index of the first element that should not be read or
	 * written. Limit is always between 0 and capacity (inclusive).
	 * </p>
	 * 
	 * @return the limit of this buffer
	 */
	public long limit() {
		return limit;
	}

	/**
	 * Sets this buffer's limit.
	 * 
	 * <p>
	 * If position is larger than the new limit, position is set to the new limit.
	 * If mark is defined and larger than the new limit, it is discarded.
	 * </p>
	 * 
	 * @param newLimit the new limit value
	 * @return this buffer for method chaining
	 */
	public MemoryBuffer limit(long newLimit) {
		if (hasError)
			return this;

		if (newLimit < 0 || newLimit > capacity()) {
			setError(new BufferOperationException(
					"Limit out of bounds: " + newLimit + " (capacity=" + capacity() + ")"));
			return this;
		}

		this.limit = newLimit;
		if (position > limit) {
			position = limit; // Adjust position if beyond new limit
		}
		if (mark > limit) {
			mark = -1; // Discard mark if beyond new limit
		}

		return this;
	}

	/**
	 * Returns the number of elements between current position and limit.
	 * 
	 * @return the number of elements remaining in this buffer
	 */
	public long remaining() {
		return limit - position;
	}

	/**
	 * Tells whether there are elements between position and limit.
	 * 
	 * @return true if there is at least one element remaining
	 */
	public boolean hasRemaining() {
		return position < limit;
	}

	/**
	 * Returns this buffer's capacity.
	 * 
	 * @return the capacity of this buffer
	 */
	public long capacity() {
		return memoryCapacity();
	}

	// ==================== Buffer Management Methods ====================

	/**
	 * Clears this buffer.
	 * 
	 * <p>
	 * Sets position to zero, limit to capacity, and discards mark. This prepares
	 * the buffer for a new sequence of channel-write or get operations.
	 * </p>
	 * 
	 * Developer Note: This doesn't clear error state - use clearError() for that.
	 * 
	 * @return this buffer for method chaining
	 */
	public MemoryBuffer clear() {
		position = 0;
		limit = capacity();
		mark = -1;
		currentSegment = this;
		currentSegmentOffset = 0;
		return this;
	}

	/**
	 * Flips this buffer.
	 * 
	 * <p>
	 * Sets limit to current position and position to zero. This prepares the buffer
	 * for a new sequence of channel-read or put operations.
	 * </p>
	 * 
	 * <pre>{@code
	 * buffer.put(data); // Fill buffer
	 * buffer.flip(); // Prepare for reading
	 * channel.write(buffer.asByteBuffer());
	 * }</pre>
	 * 
	 * @return this buffer for method chaining
	 */
	public MemoryBuffer flip() {
		limit = position;
		position = 0;
		mark = -1;
		updateCurrentSegment();
		return this;
	}

	/**
	 * Rewinds this buffer.
	 * 
	 * <p>
	 * Sets position to zero and discards mark. Limit remains unchanged. This
	 * prepares the buffer for re-reading the same data.
	 * </p>
	 * 
	 * @return this buffer for method chaining
	 */
	public MemoryBuffer rewind() {
		position = 0;
		mark = -1;
		updateCurrentSegment();
		return this;
	}

	/**
	 * Marks the current position.
	 * 
	 * <p>
	 * Sets mark to current position. A subsequent reset() will restore the position
	 * to this marked value.
	 * </p>
	 * 
	 * @return this buffer for method chaining
	 */
	public MemoryBuffer mark() {
		mark = position;
		return this;
	}

	/**
	 * Resets position to the previously marked position.
	 * 
	 * <p>
	 * If no mark has been set, this operation fails with an error.
	 * </p>
	 * 
	 * @return this buffer for method chaining
	 */
	public MemoryBuffer reset() {
		if (hasError)
			return this;

		if (mark < 0) {
			setError(new BufferOperationException("No mark set"));
			return this;
		}

		position = mark;
		updateCurrentSegment();
		return this;
	}

	// ==================== Position Movement Methods ====================

	/**
	 * Adjusts position by the specified delta.
	 * 
	 * <p>
	 * Moves position forward (positive delta) or backward (negative delta). The
	 * resulting position must remain within [0, limit].
	 * </p>
	 * 
	 * Developer Note: Useful for alignment adjustments or relative positioning.
	 * 
	 * @param delta the amount to adjust position by
	 * @return this buffer for method chaining
	 */
	public MemoryBuffer adjustPosition(long delta) {
		if (hasError)
			return this;

		long newPos = position + delta;
		if (newPos < 0 || newPos > limit) {
			setError(new BufferOperationException(
					"Position adjustment out of bounds: " + newPos));
			return this;
		}

		position = newPos;
		updateCurrentSegment();
		return this;
	}

	/**
	 * Skips the specified number of bytes.
	 * 
	 * <p>
	 * Advances position by the specified amount. Equivalent to
	 * adjustPosition(bytes) but only allows forward movement.
	 * </p>
	 * 
	 * @param bytes the number of bytes to skip
	 * @return this buffer for method chaining
	 */
	public MemoryBuffer skip(long bytes) {
		if (hasError)
			return this;

		if (bytes < 0) {
			setError(new BufferOperationException("Cannot skip negative bytes: " + bytes));
			return this;
		}

		return adjustPosition(bytes);
	}

	/**
	 * Moves position backward by the specified number of bytes.
	 * 
	 * <p>
	 * Equivalent to adjustPosition(-bytes). The position cannot go below 0.
	 * </p>
	 * 
	 * @param bytes the number of bytes to move backward
	 * @return this buffer for method chaining
	 */
	public MemoryBuffer backup(long bytes) {
		if (hasError)
			return this;

		if (bytes < 0) {
			setError(new BufferOperationException("Cannot backup negative bytes: " + bytes));
			return this;
		}

		return adjustPosition(-bytes);
	}

	/**
	 * Positions at the start of the specified memory proxy.
	 * 
	 * <p>
	 * Sets position to the offset of the provided MemoryProxy within this buffer.
	 * Useful for positioning at protocol headers or known structures.
	 * </p>
	 * 
	 * Developer Note: This enables pattern like
	 * buffer.positionAt(ipHeader).putShort(newChecksum)
	 * 
	 * @param proxy the memory proxy to position at
	 * @return this buffer for method chaining
	 */
	public MemoryBuffer positionAt(MemoryProxy proxy) {
		if (hasError)
			return this;

		if (proxy == null || !proxy.isBound()) {
			setError(new BufferOperationException("Invalid or unbound proxy"));
			return this;
		}

		// Calculate offset of proxy within our buffer
		long proxyOffset = proxy.memoryOffset() - this.memoryOffset();
		return position(proxyOffset);
	}

	// ==================== Multi-Segment Chain Methods ====================

	/**
	 * Returns the total remaining bytes across all segments in the chain.
	 * 
	 * <p>
	 * Unlike remaining() which returns bytes in current segment, this returns total
	 * bytes from position to end of entire chain.
	 * </p>
	 * 
	 * Developer Note: Essential for operations that span segments.
	 * 
	 * @return total remaining bytes in chain
	 */
	public long chainRemaining() {
		if (currentSegment == null)
			return 0;

		long total = currentSegment.memoryDataEnd() -
				(currentSegment.memoryDataOffset() + localPosition());

		Memory next = currentSegment.nextMemory();
		while (next != null) {
			total += next.memoryDataLength();
			next = next.nextMemory();
		}

		return Math.min(total, limit - position);
	}

	/**
	 * Positions at the specified offset within the entire chain.
	 * 
	 * <p>
	 * Unlike position() which works within current segment, this positions anywhere
	 * in the multi-segment chain.
	 * </p>
	 * 
	 * @param chainOffset the offset within the entire chain
	 * @return this buffer for method chaining
	 */
	public MemoryBuffer chainPosition(long chainOffset) {
		if (hasError)
			return this;

		if (chainOffset < 0 || chainOffset > chainCapacity()) {
			setError(new BufferOperationException(
					"Chain position out of bounds: " + chainOffset));
			return this;
		}

		// Find segment containing this offset
		Memory segment = seekMemory(chainOffset);
		if (segment != null) {
			currentSegment = segment;
			currentSegmentOffset = calculateSegmentOffset(segment);
			position = chainOffset;
		}

		return this;
	}

	/**
	 * Returns the chain-wide limit.
	 * 
	 * <p>
	 * The maximum position that can be set across the entire chain.
	 * </p>
	 * 
	 * @return the chain limit
	 */
	public long chainLimit() {
		return Math.min(limit, chainCapacity());
	}

	// ==================== Space Management Methods ====================

	/**
	 * Ensures the specified amount of space is available.
	 * 
	 * <p>
	 * Checks if sufficient space exists from current position. If not, accumulates
	 * an error.
	 * </p>
	 * 
	 * @param required the required space in bytes
	 * @return this buffer for method chaining
	 */
	public MemoryBuffer ensureRemaining(long required) {
		if (hasError)
			return this;

		if (remaining() < required) {
			setError(new BufferOperationException(
					"Insufficient space: need " + required + ", have " + remaining()));
			metrics.recordInsufficientSpace();
		}

		return this;
	}

	/**
	 * Compacts this buffer.
	 * 
	 * <p>
	 * Moves remaining data to the beginning of the buffer. Position is set to
	 * remaining(), limit to capacity.
	 * </p>
	 * 
	 * Developer Note: This is a potentially expensive operation as it moves data.
	 * Only use when necessary for buffer reuse.
	 * 
	 * @return this buffer for method chaining
	 */
	public MemoryBuffer compact() {
		if (hasError)
			return this;

		long rem = remaining();
		if (rem > 0 && position > 0) {
			// Move remaining data to beginning
			MemorySegment.copy(memorySegment, position, memorySegment, 0, rem);
		}

		position = rem;
		limit = capacity();
		mark = -1;
		updateCurrentSegment();

		return this;
	}

	// ==================== Protected Helper Methods ====================

	/**
	 * Sets the error state with the provided exception.
	 * 
	 * Developer Note: We only set the first error to preserve original failure
	 * context. Subsequent errors are ignored unless error is cleared.
	 * 
	 * @param error the error to set
	 */
	protected void setError(BufferOperationException error) {
		if (!hasError) { // Only set first error
			this.pendingError = error;
			this.hasError = true;
		}
	}

	/**
	 * Checks if error state is set.
	 * 
	 * Developer Note: Fast check for early return in operations.
	 * 
	 * @return true if error is set
	 */
	protected boolean checkError() {
		return hasError;
	}

	/**
	 * Updates the current segment cache based on position.
	 * 
	 * Developer Note: Critical for multi-segment performance. We cache the current
	 * segment to avoid repeated lookups.
	 */
	protected void updateCurrentSegment() {
		if (nextMemory == null) {
			currentSegment = this;
			currentSegmentOffset = 0;
			return;
		}

		// Find segment containing current position
		long offset = 0;
		Memory segment = this;

		while (segment != null && position >= offset + segment.memoryDataLength()) {
			offset += segment.memoryDataLength();
			segment = segment.nextMemory();
		}

		if (segment != null) {
			currentSegment = segment;
			currentSegmentOffset = offset;
		}
	}

	/**
	 * Returns the current segment in the chain.
	 * 
	 * @return the current memory segment
	 */
	protected Memory currentSegment() {
		return currentSegment != null ? currentSegment : this;
	}

	/**
	 * Returns the local position within the current segment.
	 * 
	 * @return position relative to current segment
	 */
	protected long localPosition() {
		return position - currentSegmentOffset;
	}

	/**
	 * Checks if an operation of the specified size needs spanning.
	 * 
	 * Developer Note: Key optimization point - we avoid spanning logic when
	 * operation fits in current segment.
	 * 
	 * @param size the size of the operation
	 * @return true if operation would span segments
	 */
	protected boolean needsSpanning(long size) {
		if (currentSegment == null)
			return false;

		long localPos = localPosition();
		long segmentRemaining = currentSegment.memoryDataEnd() -
				(currentSegment.memoryDataOffset() + localPos);

		return size > segmentRemaining;
	}

	/**
	 * Ensures the specified space is available for writing.
	 * 
	 * @param size the required size
	 * @return true if space is available
	 */
	protected boolean ensureSpace(long size) {
		if (remaining() < size) {
			setError(new BufferOperationException(
					"Insufficient space: need " + size + ", have " + remaining()));
			metrics.recordInsufficientSpace();
			return false;
		}
		return true;
	}

	/**
	 * Calculates the offset of a segment within the chain.
	 * 
	 * @param target the target segment
	 * @return the byte offset of the segment start
	 */
	private long calculateSegmentOffset(Memory target) {
		long offset = 0;
		Memory current = this;

		while (current != null && current != target) {
			offset += current.memoryDataLength();
			current = current.nextMemory();
		}

		return offset;
	}

	/**
	 * Returns a string representation of this buffer's state.
	 * 
	 * @return a string describing this buffer
	 */
	@Override
	public String toString() {
		return String.format("MemoryBuffer[pos=%d, lim=%d, cap=%d, err=%s]",
				position, limit, capacity(), hasError ? pendingError : "none");
	}
}
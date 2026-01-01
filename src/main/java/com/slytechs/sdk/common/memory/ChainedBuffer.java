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
package com.slytechs.sdk.common.memory;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Abstract base class for memory buffers with composition-based delegation.
 * 
 * <p>
 * ChainedBuffer provides a base for buffer types that delegate position-based
 * operations to a MemoryBuffer instance. This enables specialized buffer types to
 * focus on their specific data operations while inheriting standard buffer
 * management through composition.
 * </p>
 * 
 * <h2>Design Intent</h2>
 * <p>
 * This class implements a delegation pattern where buffer operations are
 * forwarded to a MemoryBuffer instance:
 * </p>
 * <ul>
 * <li><strong>Concrete implementation:</strong> MemoryBuffer contains
 * position/limit/mark/error fields and overrides delegating methods with direct
 * implementations</li>
 * <li><strong>Composite implementation:</strong> Specialized buffer types
 * extend ChainedBuffer and provide a MemoryBuffer via constructor for delegation</li>
 * </ul>
 * 
 * <h2>Buffer Properties</h2>
 * <p>
 * A buffer is defined by four properties (delegated to MemoryBuffer):
 * </p>
 * <ul>
 * <li><b>Capacity:</b> The total number of bytes in the buffer</li>
 * <li><b>Position:</b> The index of the next byte to be read or written</li>
 * <li><b>Limit:</b> The index of the first byte that should not be read or
 * written</li>
 * <li><b>Mark:</b> A remembered position that can be recalled later</li>
 * </ul>
 * 
 * <p>
 * Invariant: {@code 0 <= mark <= position <= limit <= capacity}
 * </p>
 * 
 * <h2>Delegation Architecture</h2>
 * <p>
 * ChainedBuffer uses constructor-injected MemoryBuffer for buffer operations:
 * </p>
 * 
 * <pre>{@code
 * public class CustomBuffer extends ChainedBuffer {
 * 	public CustomBuffer(MemoryBuffer byteBuf) {
 * 		super(byteBuf); // Pass MemoryBuffer to base
 * 	}
 * 
 * 	public CustomType getCustomType() {
 * 		// Use asByteBuf() for byte access
 * 		int size = asByteBuf().getInt();
 * 		// ... read custom data
 * 	}
 * }
 * }</pre>
 * 
 * <h2>Error Handling Philosophy</h2>
 * <p>
 * Errors are accumulated rather than thrown immediately, allowing processing
 * pipelines to continue without expensive exception handling:
 * </p>
 * 
 * <pre>{@code
 * buffer.position(1000) // May exceed limit
 * 		.skip(100) // Continues despite error
 * 		.adjustPosition(50) // Still executes
 * 		.orElseThrow(); // Throws accumulated error here
 * }</pre>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 */
public abstract class ChainedBuffer {

	/**
	 * The terminal MemoryBuffer for all delegated operations.
	 * 
	 * <p>
	 * Will be null only for MemoryBuffer itself, which overrides all delegating methods
	 * with concrete implementations.
	 * </p>
	 */
	protected final MemoryBuffer terminal;

	/**
	 * Constructs a ChainedBuffer with a terminal MemoryBuffer for delegation.
	 * 
	 * @param terminal the terminal MemoryBuffer, or null for MemoryBuffer itself
	 */
	protected ChainedBuffer(MemoryBuffer terminal) {
		this.terminal = terminal;
	}

	/**
	 * Sets an error state.
	 */
	protected void setError(Exception e) {
		terminal.setError(e);
	}

	/**
	 * Returns the terminal ByteBuffer for byte-level JDK compatible operations.
	 * 
	 * <p>
	 * Default implementation returns the terminal field. ByteBuffer overrides to
	 * return itself since terminal is null.
	 * </p>
	 * 
	 * @return the terminal MemoryBuffer
	 */
	public ByteBuffer asByteBuffer() {
		return terminal.asByteBuffer();
	}

	/**
	 * Returns a sparse array for gather NIO calls for all of the chained memory
	 * segments making up this memory buf.
	 *
	 * @return the sparse array make up of all data areas.
	 */
	public ByteBuffer[] toArray() {
		return terminal.toArray();
	}

	/**
	 * Returns the terminal MemoryBuffer for byte-level operations.
	 * 
	 * <p>
	 * Default implementation returns the terminal field. MemoryBuffer overrides to
	 * return itself since terminal is null.
	 * </p>
	 * 
	 * @return the terminal MemoryBuffer
	 */
	public MemoryBuffer asByteBuf() {
		return terminal;
	}

	// ==================== Delegating Buffer Operations ====================

	/**
	 * Returns this buffer's capacity.
	 * 
	 * @return the capacity of this buffer
	 */
	public long capacity() {
		return terminal.capacity();
	}

	/**
	 * Adjusts the position by a relative amount.
	 * 
	 * @param delta the amount to adjust by (positive or negative)
	 * @return this buffer for method chaining
	 */
	public ChainedBuffer adjustPosition(long delta) {
		terminal.adjustPosition(delta);
		return this;
	}

	/**
	 * Moves the position backward by the specified amount.
	 * 
	 * @param n the number of bytes to move backward
	 * @return this buffer for method chaining
	 */
	public ChainedBuffer backup(long n) {
		terminal.backup(n);
		return this;
	}

	/**
	 * Clears this buffer.
	 * 
	 * <p>
	 * Resets the buffer for writing: position=0, limit=capacity, mark=-1
	 * </p>
	 * 
	 * @return this buffer for method chaining
	 */
	public ChainedBuffer clear() {
		terminal.clear();
		return this;
	}

	/**
	 * Clears any accumulated error state.
	 * 
	 * @return this buffer for method chaining
	 */
	public ChainedBuffer clearError() {
		terminal.clearError();
		return this;
	}

	/**
	 * Compacts this buffer.
	 * 
	 * @return this buffer for method chaining
	 */
	public ChainedBuffer compact() {
		terminal.compact();
		return this;
	}

	/**
	 * Ensures that at least the specified number of bytes remain.
	 * 
	 * @param required the minimum number of bytes that must remain
	 * @return this buffer for method chaining
	 */
	public ChainedBuffer ensureRemaining(long required) {
		terminal.ensureRemaining(required);
		return this;
	}

	/**
	 * Flips this buffer.
	 * 
	 * <p>
	 * Prepares buffer for reading after writing: limit=position, position=0,
	 * mark=-1
	 * </p>
	 * 
	 * @return this buffer for method chaining
	 */
	public ChainedBuffer flip() {
		terminal.flip();
		return this;
	}

	/**
	 * Returns the currently accumulated error, if any.
	 * 
	 * @return the accumulated BufferOperationException, or null if no error
	 */
	public BufferOperationException getError() {
		return terminal.getError();
	}

	/**
	 * Tells whether an error has been accumulated.
	 * 
	 * @return true if an error has been accumulated
	 */
	public boolean hasError() {
		return terminal.hasError();
	}

	/**
	 * Tells whether there are any bytes between position and limit.
	 * 
	 * @return true if there is at least one byte remaining
	 */
	public boolean hasRemaining() {
		return terminal.hasRemaining();
	}

	/**
	 * Executes the given action if an error is accumulated.
	 * 
	 * @param monitor the action to execute if an error exists
	 * @return this buffer for method chaining
	 */
	public ChainedBuffer ifError(Consumer<BufferOperationException> monitor) {
		terminal.ifError(monitor);
		return this;
	}

	/**
	 * Returns this buffer's limit.
	 * 
	 * @return the limit of this buffer
	 */
	public long limit() {
		return terminal.limit();
	}

	/**
	 * Sets this buffer's limit.
	 * 
	 * @param newLimit the new limit value
	 * @return this buffer for method chaining
	 */
	public ChainedBuffer limit(long newLimit) {
		terminal.limit(newLimit);
		return this;
	}

	/**
	 * Sets this buffer's mark at its position.
	 * 
	 * @return this buffer for method chaining
	 */
	public ChainedBuffer mark() {
		terminal.mark();
		return this;
	}

	/**
	 * Handles an accumulated error with the given handler.
	 * 
	 * @param handler the error handler receiving (buffer, error)
	 * @return this buffer for method chaining
	 */
	public ChainedBuffer onError(BiConsumer<? super ChainedBuffer, BufferOperationException> handler) {
		terminal.onError((buf, err) -> handler.accept(this, err));
		return this;
	}

	/**
	 * Throws any accumulated error or returns this buffer.
	 * 
	 * @return this buffer if no error is accumulated
	 * @throws BufferOperationException if an error has been accumulated
	 */
	public ChainedBuffer orElseThrow() throws BufferOperationException {
		terminal.orElseThrow();
		return this;
	}

	/**
	 * Returns this buffer's position.
	 * 
	 * @return the position of this buffer
	 */
	public long position() {
		return terminal.position();
	}

	/**
	 * Sets this buffer's position.
	 * 
	 * @param newPosition the new position value
	 * @return this buffer for method chaining
	 */
	public ChainedBuffer position(long newPosition) {
		terminal.position(newPosition);
		return this;
	}

	/**
	 * Returns the number of bytes between position and limit.
	 * 
	 * @return the number of bytes remaining
	 */
	public long remaining() {
		return terminal.remaining();
	}

	/**
	 * Resets this buffer's position to the previously-marked position.
	 * 
	 * @return this buffer for method chaining
	 */
	public ChainedBuffer reset() {
		terminal.reset();
		return this;
	}

	/**
	 * Rewinds this buffer.
	 * 
	 * <p>
	 * Sets position to zero and discards mark.
	 * </p>
	 * 
	 * @return this buffer for method chaining
	 */
	public ChainedBuffer rewind() {
		terminal.rewind();
		return this;
	}

	/**
	 * Increments the position by the specified number of bytes.
	 * 
	 * @param n the number of bytes to skip
	 * @return this buffer for method chaining
	 */
	public ChainedBuffer skip(long n) {
		terminal.skip(n);
		return this;
	}
}
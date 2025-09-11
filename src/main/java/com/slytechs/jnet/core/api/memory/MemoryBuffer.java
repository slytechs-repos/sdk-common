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
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Data accessor providing ByteBuffer-like operations with multi-segment
 * spanning.
 * 
 * <p>
 * MemoryBuffer extends BoundView to provide positioned access to memory with
 * error accumulation and multi-segment support. It maintains position, limit,
 * and mark similar to ByteBuffer but operates on native memory segments with
 * higher performance through VarHandle optimizations.
 * </p>
 * 
 * <h2>Key Features</h2>
 * <ul>
 * <li><strong>ByteBuffer semantics:</strong> Familiar position/limit/mark
 * operations</li>
 * <li><strong>Error accumulation:</strong> Operations continue after errors for
 * performance</li>
 * <li><strong>Multi-segment support:</strong> Transparent spanning across
 * chained memory</li>
 * <li><strong>VarHandle optimization:</strong> Direct memory access without JNI
 * overhead</li>
 * <li><strong>Zero allocation:</strong> All operations work with pre-allocated
 * structures</li>
 * </ul>
 * 
 * <h2>Error Handling Philosophy</h2>
 * <p>
 * Errors are accumulated rather than thrown immediately, allowing processing
 * pipelines to continue without expensive exception handling. Check for errors
 * at strategic points:
 * </p>
 * 
 * <pre>{@code
 * buffer.getInt(); // May encounter error
 * buffer.getShort(); // Continues despite previous error
 * buffer.getLong(); // Still continues
 * 
 * if (buffer.hasError()) {
 * 	// Handle accumulated errors
 * 	Exception e = buffer.getError();
 * 	buffer.clearError();
 * }
 * }</pre>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 */
public class MemoryBuffer extends BoundView {

	// ==================== VarHandle Optimization ====================

	/** Little-endian handles */
	private static final VarHandle BYTE_HANDLE = ValueLayout.JAVA_BYTE.varHandle();
	private static final VarHandle SHORT_HANDLE = ValueLayout.JAVA_SHORT_UNALIGNED.varHandle();
	private static final VarHandle INT_HANDLE = ValueLayout.JAVA_INT_UNALIGNED.varHandle();
	private static final VarHandle LONG_HANDLE = ValueLayout.JAVA_LONG_UNALIGNED.varHandle();
	private static final VarHandle FLOAT_HANDLE = ValueLayout.JAVA_FLOAT_UNALIGNED.varHandle();
	private static final VarHandle DOUBLE_HANDLE = ValueLayout.JAVA_DOUBLE_UNALIGNED.varHandle();
	private static final VarHandle CHAR_HANDLE = ValueLayout.JAVA_CHAR_UNALIGNED.varHandle();

	/** Big-endian handles for network byte order */
	private static final VarHandle SHORT_BE_HANDLE = ValueLayout.JAVA_SHORT_UNALIGNED
			.withOrder(ByteOrder.BIG_ENDIAN).varHandle();
	private static final VarHandle INT_BE_HANDLE = ValueLayout.JAVA_INT_UNALIGNED
			.withOrder(ByteOrder.BIG_ENDIAN).varHandle();
	private static final VarHandle LONG_BE_HANDLE = ValueLayout.JAVA_LONG_UNALIGNED
			.withOrder(ByteOrder.BIG_ENDIAN).varHandle();

	// ==================== Buffer State ====================

	/** Current read/write position */
	protected long position = 0;

	/** Upper bound for operations */
	protected long limit = 0;

	/** Marked position for reset */
	protected long mark = -1;

	/** Accumulated error state */
	protected Exception error = null;

	// Add these methods to MemoryBuffer.java

	// ==================== Navigation Methods ====================

	/**
	 * Constructs an unbound MemoryBuffer.
	 */
	public MemoryBuffer() {
		super();
	}

	/**
	 * Constructs a MemoryBuffer bound to a segment.
	 * 
	 * @param segment the memory segment
	 */
	public MemoryBuffer(MemorySegment segment) {
		super();
		bind(Memory.of(segment, 0, segment.byteSize()));
	}

	public MemoryBuffer adjustPosition(long delta) {
		if (hasError())
			return this;

		// Check for overflow/underflow before adding
		long newPos;
		try {
			newPos = Math.addExact(position, delta); // This throws on overflow
		} catch (ArithmeticException e) {
			setError(new BufferOperationException(
					"Position adjustment would overflow: " + position + " + " + delta));
			return this;
		}

		if (newPos < 0) {
			setError(new BufferOperationException(
					"Position adjustment out of bounds: " + newPos + " < 0"));
			return this;
		}
		if (newPos > limit) {
			setError(new BufferOperationException(
					"Position adjustment out of bounds: " + newPos + " > " + limit));
			return this;
		}

		position = newPos;
		if (mark > position) {
			mark = -1;
		}
		return this;
	}

	// Update backup method error handling:
	public MemoryBuffer backup(long n) {
		if (hasError())
			return this;
		if (n < 0) {
			setError(new BufferOperationException("Backup amount cannot be negative: " + n));
			return this;
		}
		if (n > position) {
			setError(new BufferOperationException(
					"Backup beyond start: " + n + " > " + position));
			return this;
		}
		position = position - n;
		if (mark > position) {
			mark = -1;
		}
		return this;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Resets buffer state when binding to new memory.
	 * </p>
	 */
	@Override
	public void bind(Memory memory) {
		super.bind(memory);
		position = 0;
		limit = view.length;
		mark = -1;
		error = null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Resets buffer state when binding to new memory region.
	 * </p>
	 */
	@Override
	public void bind(Memory memory, long offset, long length) {
		super.bind(memory, offset, length);
		position = 0;
		limit = length;
		mark = -1;
		error = null;
	}

	// ==================== Indexed Get Operations ====================

	/**
	 * Returns the capacity.
	 * 
	 * @return the capacity
	 */
	public long capacity() {
		return view != null ? view.length : 0;
	}

	public MemoryBuffer clear() {
		if (hasError())
			return this;
		position = 0;
		limit = capacity();
		mark = -1;
		return this;
	}

	/**
	 * Clears the error state.
	 * 
	 * @return this buffer
	 */
	public MemoryBuffer clearError() {
		this.error = null;
		return this;
	}

	/**
	 * Compacts the buffer (not implemented - just for compatibility).
	 * 
	 * @return this buffer
	 */
	public MemoryBuffer compact() {
		if (hasError())
			return this;
		// Compact operation would move remaining data to beginning
		// For now, just a placeholder
		return this;
	}

	/**
	 * Ensures remaining capacity.
	 * 
	 * @param required the required remaining bytes
	 * @return this buffer
	 */
	public MemoryBuffer ensureRemaining(long required) {
		if (hasError())
			return this;
		if (remaining() < required) {
			setError(new BufferOperationException(
					"Insufficient remaining: " + remaining() + " < " + required));
		}
		return this;
	}

	// ==================== Relative Primitive Operations ====================

	// Update flip() to preserve state on error:
	public MemoryBuffer flip() {
		if (hasError())
			return this;
		limit = position;
		position = 0;
		mark = -1;
		return this;
	}

	/**
	 * Reads a byte at the current position.
	 * 
	 * @return the byte value
	 */
	public byte get() {
		if (hasError())
			return 0;
		if (remaining() < 1) {
			setError(new IllegalStateException("Buffer underflow"));
			return 0;
		}

		byte result = (byte) BYTE_HANDLE.get(view.segment, view.start + position);
		position++;
		return result;
	}

	/**
	 * Reads bytes into array.
	 */
	public MemoryBuffer get(byte[] dst) {
		if (dst == null) {
			setError(new BufferOperationException("Destination array is null"));
			return this;
		}
		return get(dst, 0, dst.length);
	}

	// ==================== Big-Endian Operations ====================

	/**
	 * Reads bytes into array portion.
	 */
	public MemoryBuffer get(byte[] dst, int offset, int length) {
		if (hasError())
			return this;

		// Null check
		if (dst == null) {
			setError(new BufferOperationException("Destination array is null"));
			return this;
		}

		// Bounds checking
		if (offset < 0 || length < 0 || offset + length > dst.length) {
			setError(new BufferOperationException(
					String.format("Invalid array bounds: offset=%d, length=%d, array.length=%d",
							offset, length, dst.length)));
			return this;
		}

		// Check buffer has enough data
		if (remaining() < length) {
			setError(new BufferOperationException("Buffer underflow"));
			return this;
		}

		// Only copy if length > 0
		if (length > 0) {
			try {
				MemorySegment.copy(view.segment, view.start + position,
						MemorySegment.ofArray(dst), offset, length);
				position += length;
			} catch (Exception e) {
				setError(new BufferOperationException("Failed to read from buffer", e));
			}
		}

		return this;
	}

	/**
	 * Reads a byte at the specified index.
	 * 
	 * @param index the index
	 * @return the byte value
	 */
	public byte get(long index) {
		if (hasError())
			return 0;
		if (index < 0 || index >= limit) {
			setError(new IllegalArgumentException("Index out of bounds"));
			return 0;
		}

		return (byte) BYTE_HANDLE.get(view.segment, view.start + index);
	}

	/**
	 * Reads a char at the current position.
	 */
	public char getChar() {
		if (hasError())
			return 0;
		if (remaining() < 2) {
			setError(new IllegalStateException("Buffer underflow"));
			return 0;
		}
		char result = (char) CHAR_HANDLE.get(view.segment, view.start + position);
		position += 2;
		return result;
	}

	// ==================== Array Operations ====================

	/**
	 * Reads a char at the specified index.
	 */
	public char getChar(long index) {
		if (hasError())
			return 0;
		if (index < 0 || index + 2 > limit) {
			setError(new IllegalArgumentException("Index out of bounds"));
			return 0;
		}
		return (char) CHAR_HANDLE.get(view.segment, view.start + index);
	}

	/**
	 * Reads a double at the current position.
	 */
	public double getDouble() {
		if (hasError())
			return 0;
		if (remaining() < 8) {
			setError(new IllegalStateException("Buffer underflow"));
			return 0;
		}
		double result = (double) DOUBLE_HANDLE.get(view.segment, view.start + position);
		position += 8;
		return result;
	}

	/**
	 * Reads a double at the specified index.
	 */
	public double getDouble(long index) {
		if (hasError())
			return 0;
		if (index < 0 || index + 8 > limit) {
			setError(new IllegalArgumentException("Index out of bounds"));
			return 0;
		}
		return (double) DOUBLE_HANDLE.get(view.segment, view.start + index);
	}

	/**
	 * Gets the current error as BufferOperationException.
	 * 
	 * @return the error or null
	 */
	public BufferOperationException getError() {
		return (BufferOperationException) error;
	}

	/**
	 * Reads a float at the current position.
	 */
	public float getFloat() {
		if (hasError())
			return 0;
		if (remaining() < 4) {
			setError(new IllegalStateException("Buffer underflow"));
			return 0;
		}
		float result = (float) FLOAT_HANDLE.get(view.segment, view.start + position);
		position += 4;
		return result;
	}

	/**
	 * Reads a float at the specified index.
	 */
	public float getFloat(long index) {
		if (hasError())
			return 0;
		if (index < 0 || index + 4 > limit) {
			setError(new IllegalArgumentException("Index out of bounds"));
			return 0;
		}
		return (float) FLOAT_HANDLE.get(view.segment, view.start + index);
	}

	/**
	 * Reads an int at the current position.
	 * 
	 * @return the int value
	 */
	public int getInt() {
		if (hasError())
			return 0;
		if (remaining() < 4) {
			setError(new IllegalStateException("Buffer underflow"));
			return 0;
		}

		int result = (int) INT_HANDLE.get(view.segment, view.start + position);
		position += 4;
		return result;
	}

	// ==================== Buffer Control ====================

	/**
	 * Reads an int at the specified index.
	 */
	public int getInt(long index) {
		if (hasError())
			return 0;
		if (index < 0 || index + 4 > limit) {
			setError(new IllegalArgumentException("Index out of bounds"));
			return 0;
		}
		return (int) INT_HANDLE.get(view.segment, view.start + index);
	}

	/**
	 * Reads an int in big-endian byte order.
	 * 
	 * @return the int value
	 */
	public int getIntBE() {
		if (hasError())
			return 0;
		if (remaining() < 4) {
			setError(new IllegalStateException("Buffer underflow"));
			return 0;
		}

		int result = (int) INT_BE_HANDLE.get(view.segment, view.start + position);
		position += 4;
		return result;
	}

	/**
	 * Reads an int at the specified index in big-endian byte order.
	 */
	public int getIntBE(long index) {
		if (hasError())
			return 0;
		if (index < 0 || index + 4 > limit) {
			setError(new IllegalArgumentException("Index out of bounds"));
			return 0;
		}
		return (int) INT_BE_HANDLE.get(view.segment, view.start + index);
	}

	/**
	 * Reads a long at the current position.
	 * 
	 * @return the long value
	 */
	public long getLong() {
		if (hasError())
			return 0;
		if (remaining() < 8) {
			setError(new IllegalStateException("Buffer underflow"));
			return 0;
		}

		long result = (long) LONG_HANDLE.get(view.segment, view.start + position);
		position += 8;
		return result;
	}

	/**
	 * Reads a long at the specified index.
	 */
	public long getLong(long index) {
		if (hasError())
			return 0;
		if (index < 0 || index + 8 > limit) {
			setError(new IllegalArgumentException("Index out of bounds"));
			return 0;
		}
		return (long) LONG_HANDLE.get(view.segment, view.start + index);
	}

	/**
	 * Reads a long in big-endian byte order.
	 * 
	 * @return the long value
	 */
	public long getLongBE() {
		if (hasError())
			return 0;
		if (remaining() < 8) {
			setError(new IllegalStateException("Buffer underflow"));
			return 0;
		}

		long result = (long) LONG_BE_HANDLE.get(view.segment, view.start + position);
		position += 8;
		return result;
	}

	/**
	 * Reads a long at the specified index in big-endian byte order.
	 */
	public long getLongBE(long index) {
		if (hasError())
			return 0;
		if (index < 0 || index + 8 > limit) {
			setError(new IllegalArgumentException("Index out of bounds"));
			return 0;
		}
		return (long) LONG_BE_HANDLE.get(view.segment, view.start + index);
	}

	/**
	 * Reads a short at the current position.
	 * 
	 * @return the short value
	 */
	public short getShort() {
		if (hasError())
			return 0;
		if (remaining() < 2) {
			setError(new IllegalStateException("Buffer underflow"));
			return 0;
		}

		short result = (short) SHORT_HANDLE.get(view.segment, view.start + position);
		position += 2;
		return result;
	}

	/**
	 * Reads a short at the specified index.
	 */
	public short getShort(long index) {
		if (hasError())
			return 0;
		if (index < 0 || index + 2 > limit) {
			setError(new IllegalArgumentException("Index out of bounds"));
			return 0;
		}
		return (short) SHORT_HANDLE.get(view.segment, view.start + index);
	}

	/**
	 * Reads a short in big-endian byte order.
	 * 
	 * @return the short value
	 */
	public short getShortBE() {
		if (hasError())
			return 0;
		if (remaining() < 2) {
			setError(new IllegalStateException("Buffer underflow"));
			return 0;
		}

		short result = (short) SHORT_BE_HANDLE.get(view.segment, view.start + position);
		position += 2;
		return result;
	}

	/**
	 * Reads a short at the specified index in big-endian byte order.
	 */
	public short getShortBE(long index) {
		if (hasError())
			return 0;
		if (index < 0 || index + 2 > limit) {
			setError(new IllegalArgumentException("Index out of bounds"));
			return 0;
		}
		return (short) SHORT_BE_HANDLE.get(view.segment, view.start + index);
	}

	// ==================== Error Management ====================

	/**
	 * Checks if buffer has an error.
	 * 
	 * @return true if error exists
	 */
	public boolean hasError() {
		return error != null;
	}

	/**
	 * Checks if there are remaining bytes.
	 * 
	 * @return true if remaining > 0
	 */
	public boolean hasRemaining() {
		return position < limit;
	}

	// For the ifError exception replacement issue, update the ifError method:
	public MemoryBuffer ifError(Consumer<BufferOperationException> monitor) {
		if (hasError()) {
			try {
				monitor.accept((BufferOperationException) error);
			} catch (RuntimeException e) {
				// Replace error with monitor exception
				error = new BufferOperationException("Monitor failed: " + e.getMessage(), e);
			} catch (Exception e) {
				// Wrap checked exceptions
				error = new BufferOperationException("Monitor failed", e);
			}
		}
		return this;
	}

	// ==================== Get Operations ====================

	/**
	 * Returns the limit.
	 * 
	 * @return the limit
	 */
	public long limit() {
		return limit;
	}

	// Update limit method error handling:
	public MemoryBuffer limit(long newLimit) {
		if (hasError())
			return this;
		if (newLimit < 0 || newLimit > capacity()) {
			setError(new BufferOperationException(
					"Limit out of bounds: " + newLimit + " (capacity=" + capacity() + ")"));
			return this;
		}
		this.limit = newLimit;
		if (position > limit) {
			position = limit;
		}
		if (mark > limit) {
			mark = -1;
		}
		return this;
	}

	/**
	 * Marks the current position.
	 * 
	 * @return this buffer
	 */
	public MemoryBuffer mark() {
		mark = position;
		return this;
	}

	/**
	 * Handles accumulated error with a custom handler.
	 * 
	 * @param handler the error handler (buffer, error) -> void
	 * @return this buffer
	 */
	public MemoryBuffer onError(BiConsumer<MemoryBuffer, BufferOperationException> handler) {
		if (hasError()) {
			try {
				handler.accept(this, (BufferOperationException) error);
				error = null; // Clear on successful handling
			} catch (Exception e) {
				// Replace error with handler exception
				error = e instanceof BufferOperationException ? (BufferOperationException) e
						: new BufferOperationException("Error handler failed", e);
			}
		}
		return this;
	}

	/**
	 * Throws the accumulated error if present.
	 * 
	 * @return this buffer if no error
	 * @throws BufferOperationException if an error is accumulated
	 */
	public MemoryBuffer orElseThrow() throws BufferOperationException {
		if (hasError()) {
			BufferOperationException e = (BufferOperationException) error;
			error = null; // Clear after throwing
			throw e;
		}
		return this;
	}

	// ==================== Put Operations ====================

	/**
	 * Returns the current position.
	 * 
	 * @return the position
	 */
	public long position() {
		return position;
	}

	// Fix the position() method to not change position on error:
	public MemoryBuffer position(long newPosition) {
		if (hasError())
			return this;
		if (newPosition < 0 || newPosition > limit) {
			setError(new BufferOperationException(
					"Position out of bounds: " + newPosition + " (limit=" + limit + ")"));
			// Don't change position on error
			return this;
		}
		this.position = newPosition;
		if (mark > position) {
			mark = -1;
		}
		return this;
	}

	// ==================== Network Byte Order ====================

	/**
	 * Writes a byte at the current position.
	 * 
	 * @param value the byte to write
	 * @return this buffer
	 */
	public MemoryBuffer put(byte value) {
		if (hasError())
			return this;
		if (remaining() < 1) {
			setError(new IllegalStateException("Buffer overflow"));
			return this;
		}

		BYTE_HANDLE.set(view.segment, view.start + position, value);
		position++;
		return this;
	}

	/**
	 * Writes a byte array.
	 */
	public MemoryBuffer put(byte[] src) {
		if (src == null) {
			setError(new BufferOperationException("Source array is null"));
			return this;
		}
		return put(src, 0, src.length);
	}

	// ==================== Lifecycle ====================

	/**
	 * Writes a portion of a byte array.
	 */
	public MemoryBuffer put(byte[] src, int offset, int length) {
		if (hasError())
			return this;

		// Null check
		if (src == null) {
			setError(new BufferOperationException("Source array is null"));
			return this;
		}

		// Bounds checking
		if (offset < 0 || length < 0 || offset + length > src.length) {
			setError(new BufferOperationException(
					String.format("Invalid array bounds: offset=%d, length=%d, array.length=%d",
							offset, length, src.length)));
			return this;
		}

		// Check buffer has enough space
		if (remaining() < length) {
			setError(new BufferOperationException("Buffer overflow"));
			return this;
		}

		// Only copy if length > 0
		if (length > 0) {
			try {
				MemorySegment.copy(MemorySegment.ofArray(src), offset,
						view.segment, view.start + position, length);
				position += length;
			} catch (Exception e) {
				setError(new BufferOperationException("Failed to write to buffer", e));
			}
		}

		return this;
	}

	// Add these methods to MemoryBuffer.java

	// ==================== Error Management Methods ====================

	/**
	 * Writes a byte at the specified index.
	 */
	public MemoryBuffer put(long index, byte value) {
		if (hasError())
			return this;
		if (index < 0 || index >= limit) {
			setError(new IllegalArgumentException("Index out of bounds"));
			return this;
		}
		BYTE_HANDLE.set(view.segment, view.start + index, value);
		return this;
	}

	/**
	 * Writes a char at the current position.
	 */
	public MemoryBuffer putChar(char value) {
		if (hasError())
			return this;
		if (remaining() < 2) {
			setError(new IllegalStateException("Buffer overflow"));
			return this;
		}
		CHAR_HANDLE.set(view.segment, view.start + position, value);
		position += 2;
		return this;
	}

	/**
	 * Writes a char at the specified index.
	 */
	public MemoryBuffer putChar(long index, char value) {
		if (hasError())
			return this;
		if (index < 0 || index + 2 > limit) {
			setError(new IllegalArgumentException("Index out of bounds"));
			return this;
		}
		CHAR_HANDLE.set(view.segment, view.start + index, value);
		return this;
	}

	/**
	 * Writes a double at the current position.
	 */
	public MemoryBuffer putDouble(double value) {
		if (hasError())
			return this;
		if (remaining() < 8) {
			setError(new IllegalStateException("Buffer overflow"));
			return this;
		}
		DOUBLE_HANDLE.set(view.segment, view.start + position, value);
		position += 8;
		return this;
	}

	/**
	 * Writes a double at the specified index.
	 */
	public MemoryBuffer putDouble(long index, double value) {
		if (hasError())
			return this;
		if (index < 0 || index + 8 > limit) {
			setError(new IllegalArgumentException("Index out of bounds"));
			return this;
		}
		DOUBLE_HANDLE.set(view.segment, view.start + index, value);
		return this;
	}

	// ==================== Update setError to use BufferOperationException
	// ====================

	/**
	 * Writes a float at the current position.
	 */
	public MemoryBuffer putFloat(float value) {
		if (hasError())
			return this;
		if (remaining() < 4) {
			setError(new IllegalStateException("Buffer overflow"));
			return this;
		}
		FLOAT_HANDLE.set(view.segment, view.start + position, value);
		position += 4;
		return this;
	}

	/**
	 * Writes a float at the specified index.
	 */
	public MemoryBuffer putFloat(long index, float value) {
		if (hasError())
			return this;
		if (index < 0 || index + 4 > limit) {
			setError(new IllegalArgumentException("Index out of bounds"));
			return this;
		}
		FLOAT_HANDLE.set(view.segment, view.start + index, value);
		return this;
	}

	// ==================== Update existing methods to use BufferOperationException
	// ====================

	/**
	 * Writes an int at the current position.
	 * 
	 * @param value the int to write
	 * @return this buffer
	 */
	public MemoryBuffer putInt(int value) {
		if (hasError())
			return this;
		if (remaining() < 4) {
			setError(new IllegalStateException("Buffer overflow"));
			return this;
		}

		INT_HANDLE.set(view.segment, view.start + position, value);
		position += 4;
		return this;
	}

	/**
	 * Writes an int at the specified index.
	 */
	public MemoryBuffer putInt(long index, int value) {
		if (hasError())
			return this;
		if (index < 0 || index + 4 > limit) {
			setError(new IllegalArgumentException("Index out of bounds"));
			return this;
		}
		INT_HANDLE.set(view.segment, view.start + index, value);
		return this;
	}

	/**
	 * Writes an int in big-endian byte order.
	 */
	public MemoryBuffer putIntBE(int value) {
		if (hasError())
			return this;
		if (remaining() < 4) {
			setError(new IllegalStateException("Buffer overflow"));
			return this;
		}
		INT_BE_HANDLE.set(view.segment, view.start + position, value);
		position += 4;
		return this;
	}

	/**
	 * Writes a long at the current position.
	 * 
	 * @param value the long to write
	 * @return this buffer
	 */
	public MemoryBuffer putLong(long value) {
		if (hasError())
			return this;
		if (remaining() < 8) {
			setError(new IllegalStateException("Buffer overflow"));
			return this;
		}

		LONG_HANDLE.set(view.segment, view.start + position, value);
		position += 8;
		return this;
	}

	/**
	 * Writes a long at the specified index.
	 */
	public MemoryBuffer putLong(long index, long value) {
		if (hasError())
			return this;
		if (index < 0 || index + 8 > limit) {
			setError(new IllegalArgumentException("Index out of bounds"));
			return this;
		}
		LONG_HANDLE.set(view.segment, view.start + index, value);
		return this;
	}

	// Similar pattern for other methods - don't modify state when error occurs

	/**
	 * Writes a long in big-endian byte order.
	 */
	public MemoryBuffer putLongBE(long value) {
		if (hasError())
			return this;
		if (remaining() < 8) {
			setError(new IllegalStateException("Buffer overflow"));
			return this;
		}
		LONG_BE_HANDLE.set(view.segment, view.start + position, value);
		position += 8;
		return this;
	}

	/**
	 * Writes a short at the specified index.
	 */
	public MemoryBuffer putShort(long index, short value) {
		if (hasError())
			return this;
		if (index < 0 || index + 2 > limit) {
			setError(new IllegalArgumentException("Index out of bounds"));
			return this;
		}
		SHORT_HANDLE.set(view.segment, view.start + index, value);
		return this;
	}

	/**
	 * Writes a short at the current position.
	 * 
	 * @param value the short to write
	 * @return this buffer
	 */
	public MemoryBuffer putShort(short value) {
		if (hasError())
			return this;
		if (remaining() < 2) {
			setError(new IllegalStateException("Buffer overflow"));
			return this;
		}

		SHORT_HANDLE.set(view.segment, view.start + position, value);
		position += 2;
		return this;
	}

	/**
	 * Writes a short in big-endian byte order.
	 */
	public MemoryBuffer putShortBE(short value) {
		if (hasError())
			return this;
		if (remaining() < 2) {
			setError(new IllegalStateException("Buffer overflow"));
			return this;
		}
		SHORT_BE_HANDLE.set(view.segment, view.start + position, value);
		position += 2;
		return this;
	}

	/**
	 * Returns remaining bytes.
	 * 
	 * @return bytes remaining
	 */
	public long remaining() {
		return limit - position;
	}

	// Update reset method error handling:
	public MemoryBuffer reset() {
		if (hasError())
			return this;
		if (mark < 0) {
			setError(new BufferOperationException("Mark not set"));
			return this;
		}
		position = mark;
		return this;
	}

	// Fix the array put/get methods in MemoryBuffer.java:

	// Update rewind() to preserve state on error:
	public MemoryBuffer rewind() {
		if (hasError())
			return this;
		position = 0;
		mark = -1;
		return this;
	}

	/**
	 * Sets an error.
	 * 
	 * @param e the error
	 */
	protected void setError(Exception e) {
		if (this.error == null) { // Keep first error
			if (e instanceof BufferOperationException) {
				this.error = e;
			} else {
				this.error = new BufferOperationException(e.getMessage(), e);
			}
		}
	}

	public MemoryBuffer skip(long n) {
		if (hasError())
			return this;
		if (n < 0) {
			setError(new BufferOperationException("Skip amount cannot be negative: " + n));
			return this;
		}

		// Check for overflow before adding
		long newPos;
		try {
			newPos = Math.addExact(position, n); // This throws on overflow
		} catch (ArithmeticException e) {
			setError(new BufferOperationException(
					"Position adjustment would overflow: " + position + " + " + n));
			return this;
		}

		if (newPos > limit) {
			setError(new BufferOperationException(
					"Position adjustment out of bounds: " + newPos + " > " + limit));
			return this;
		}

		position = newPos;
		if (mark > position) {
			mark = -1;
		}
		return this;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Clears buffer state when unbinding.
	 * </p>
	 */
	@Override
	public void unbind() {
		super.unbind();
		position = 0;
		limit = 0;
		mark = -1;
		error = null;
	}
}
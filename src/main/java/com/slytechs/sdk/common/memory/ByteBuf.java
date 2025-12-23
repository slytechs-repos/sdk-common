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

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * High-performance buffer for native memory with ByteBuffer-like semantics.
 * 
 * <p>
 * ByteBuf provides concrete buffer state management and byte-level data access.
 * It implements BindableView for memory binding and serves as the foundation
 * for composite buffer types while maintaining zero-allocation operation.
 * </p>
 * 
 * <h2>Architecture</h2>
 * <p>
 * ByteBuf provides core buffer functionality:
 * </p>
 * <ul>
 * <li><strong>Buffer state:</strong> Contains concrete
 * position/limit/mark/error fields with direct implementations</li>
 * <li><strong>Memory binding:</strong> Implements BindableView for direct
 * memory access</li>
 * <li><strong>Data access:</strong> Provides byte-level get/put operations
 * using VarHandle optimization</li>
 * </ul>
 * 
 * <h2>Performance Characteristics</h2>
 * <ul>
 * <li><strong>Zero allocation:</strong> All operations use pre-allocated
 * structures</li>
 * <li><strong>VarHandle access:</strong> Direct memory operations without JNI
 * overhead</li>
 * <li><strong>Silent error accumulation:</strong> No exception throwing in hot
 * paths</li>
 * <li><strong>Optimized binding:</strong> Direct view sharing for zero-offset
 * bindings</li>
 * <li><strong>Native byte order:</strong> Default little-endian with explicit
 * BE methods for network protocols</li>
 * </ul>
 * 
 * <h2>Error Accumulation Pattern</h2>
 * <p>
 * Unlike traditional buffers that throw exceptions immediately, ByteBuf
 * accumulates errors to maintain performance in high-frequency processing
 * pipelines. Errors are checked at strategic boundaries rather than on every
 * operation:
 * </p>
 * 
 * <pre>{@code
 * // Bulk operations continue despite errors
 * buffer.putInt(header)
 * 		.putShort(flags) // Continues even if previous failed
 * 		.put(payload) // All operations execute
 * 		.putInt(checksum)
 * 		.orElseThrow(); // Single error check at boundary
 * 
 * // Alternative: Handle errors without exceptions
 * buffer.onError((buf, error) -> {
 * 	log.warn("Buffer operation failed at position {}", buf.position());
 * 	buf.clearError(); // Recovery and continue
 * });
 * }</pre>
 * 
 * <h2>Memory Binding</h2>
 * <p>
 * The buffer must be bound to a Memory object before use. Binding can occur at
 * construction or dynamically during processing:
 * </p>
 * 
 * <pre>{@code
 * // Static allocation
 * ByteBuf buffer = ByteBuf.allocate(1024);
 * 
 * // Dynamic binding to existing memory
 * Memory memory = getMemoryFromPool();
 * buffer.bind(memory, offset, length);
 * 
 * // Rebinding for packet processing
 * while (hasPackets()) {
 * 	buffer.bind(nextPacket());
 * 	processPacket(buffer);
 * 	buffer.unbind(); // Optional - can rebind directly
 * }
 * }</pre>
 * 
 * <h2>Composition Pattern Support</h2>
 * <p>
 * ByteBuf supports composite buffers that extend MemoryBuf. Composite types
 * delegate buffer management operations while providing type-specific
 * operations:
 * </p>
 * 
 * <pre>{@code
 * // ByteBuf provides concrete implementation
 * ByteBuf byteBuf = ByteBuf.allocate(1024);
 * 
 * // Composite buffers delegate operations
 * BlockBuf blockBuf = new BlockBuf(byteBuf);
 * blockBuf.position(100); // Delegates to byteBuf.position(100)
 * Block block = blockBuf.getBlock(); // Type-specific operation
 * 
 * // Both share the same position state
 * assert blockBuf.position() == byteBuf.position();
 * }</pre>
 * 
 * <h2>Network Protocol Support</h2>
 * <p>
 * Explicit big-endian methods support network protocol processing without byte
 * swapping overhead:
 * </p>
 * 
 * <pre>{@code
 * // Reading network packet header
 * int length = buffer.getIntBE(); // Network byte order
 * short port = buffer.getShortBE();
 * long timestamp = buffer.getLongBE();
 * 
 * // Local processing in native order
 * int localValue = buffer.getInt(); // Platform native (usually LE)
 * }</pre>
 * 
 * <h2>Thread Safety</h2>
 * <p>
 * ByteBuf is NOT thread-safe. Each thread should maintain its own buffer
 * instance. The underlying memory's reference counting is thread-safe, allowing
 * multiple buffers to safely bind to the same memory from different threads.
 * </p>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see MemoryBuf for abstract base with delegation pattern
 * @see BindableView for memory binding interface
 * @see Memory for memory lifecycle management
 * @since 1.0
 */
public class ByteBuf extends MemoryBuf implements BindableView {

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

	// ==================== Binding Support ====================

	protected final BoundView boundView = new BoundView();

	// ==================== Buffer State Fields ====================

	/** Current read/write position */
	protected long position = 0;

	/** Upper bound for operations */
	protected long limit = 0;

	/** Marked position for reset */
	protected long mark = -1;

	/** Accumulated error state */
	protected Exception error = null;

	// ==================== Static Factory Methods ====================

	/**
	 * Allocates a new ByteBuf with the specified size in bytes.
	 */
	public static ByteBuf allocate(long byteSize) {
		var mem = new FixedMemory(Arena.ofAuto().allocate(byteSize));
		var buf = new ByteBuf();
		buf.bind(mem);
		return buf;
	}

	/**
	 * Allocates a new ByteBuf with the specified size using the provided Arena.
	 */
	public static ByteBuf allocate(long byteSize, Arena arena) {
		var mem = new FixedMemory(arena.allocate(byteSize));
		var buf = new ByteBuf();
		buf.bind(mem);
		return buf;
	}

	// ==================== Constructors ====================

	/**
	 * Constructs an unbound ByteBuf.
	 */
	public ByteBuf() {
		super(null); // Terminal doesn't need delegation
	}

	/**
	 * Constructs a ByteBuf bound to a segment.
	 */
	public ByteBuf(MemorySegment segment) {
		super(null); // Terminal doesn't need delegation
		bind(Memory.of(segment, 0, segment.byteSize()));
	}

	// ==================== BindableView Implementation ====================

	@Override
	public BoundView boundView() {
		return boundView;
	}

	@Override
	public void onBind() {
		initializeBuffer(boundView.length());
	}

	@Override
	public void onUnbind() {
		position = 0;
		limit = 0;
		mark = -1;
		error = null;
	}

	// ==================== Terminal Overrides ====================

	@Override
	public ByteBuf asByteBuf() {
		return this; // Terminal returns itself
	}

	@Override
	public long capacity() {
		return isBound() ? boundView.length() : 0;
	}

	// ==================== Buffer State Management (Concrete) ====================

	@Override
	public long position() {
		return position;
	}

	@Override
	public ByteBuf position(long newPosition) {
		if (hasError())
			return this;
		if (newPosition < 0 || newPosition > limit) {
			setError(new BufferOperationException(
					"Position out of bounds: " + newPosition + " (limit=" + limit + ")"));
			return this;
		}
		this.position = newPosition;
		if (mark > position) {
			mark = -1;
		}
		return this;
	}

	@Override
	public long limit() {
		return limit;
	}

	@Override
	public ByteBuf limit(long newLimit) {
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

	@Override
	public ByteBuf mark() {
		mark = position;
		return this;
	}

	@Override
	public ByteBuf reset() {
		if (hasError())
			return this;
		if (mark < 0) {
			setError(new BufferOperationException("Mark not set"));
			return this;
		}
		position = mark;
		return this;
	}

	@Override
	public ByteBuf rewind() {
		if (hasError())
			return this;
		position = 0;
		mark = -1;
		return this;
	}

	@Override
	public ByteBuf clear() {
		if (hasError())
			return this;
		position = 0;
		limit = capacity();
		mark = -1;
		return this;
	}

	@Override
	public ByteBuf flip() {
		if (hasError())
			return this;
		limit = position;
		position = 0;
		mark = -1;
		return this;
	}

	@Override
	public long remaining() {
		return limit - position;
	}

	@Override
	public boolean hasRemaining() {
		return position < limit;
	}

	@Override
	public ByteBuf skip(long n) {
		if (hasError())
			return this;
		if (n < 0) {
			setError(new BufferOperationException("Skip amount cannot be negative: " + n));
			return this;
		}

		long newPos;
		try {
			newPos = Math.addExact(position, n);
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

	@Override
	public ByteBuf adjustPosition(long delta) {
		if (hasError())
			return this;

		long newPos;
		try {
			newPos = Math.addExact(position, delta);
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

	@Override
	public ByteBuf backup(long n) {
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

	@Override
	public ByteBuf compact() {
		if (hasError())
			return this;
		// Compact operation would move remaining data to beginning
		// Subclasses can override with actual implementation
		return this;
	}

	@Override
	public ByteBuf ensureRemaining(long required) {
		if (hasError())
			return this;
		if (remaining() < required) {
			setError(new BufferOperationException(
					"Insufficient remaining: " + remaining() + " < " + required));
		}
		return this;
	}

	// ==================== Error Handling (Concrete) ====================

	@Override
	public BufferOperationException getError() {
		return (BufferOperationException) error;
	}

	@Override
	public boolean hasError() {
		return error != null;
	}

	@Override
	public ByteBuf clearError() {
		this.error = null;
		return this;
	}

	@Override
	public ByteBuf ifError(Consumer<BufferOperationException> monitor) {
		if (hasError()) {
			try {
				monitor.accept((BufferOperationException) error);
			} catch (RuntimeException e) {
				error = new BufferOperationException("Monitor failed: " + e.getMessage(), e);
			} catch (Exception e) {
				error = new BufferOperationException("Monitor failed", e);
			}
		}
		return this;
	}

	@Override
	public ByteBuf onError(BiConsumer<? super MemoryBuf, BufferOperationException> handler) {
		if (hasError()) {
			try {
				handler.accept(this, (BufferOperationException) error);
				error = null;
			} catch (Exception e) {
				error = e instanceof BufferOperationException ? (BufferOperationException) e
						: new BufferOperationException("Error handler failed", e);
			}
		}
		return this;
	}

	@Override
	public ByteBuf orElseThrow() throws BufferOperationException {
		if (hasError()) {
			BufferOperationException e = (BufferOperationException) error;
			error = null;
			throw e;
		}
		return this;
	}

	/**
	 * Sets an error state.
	 */
	@Override
	protected void setError(Exception e) {
		if (this.error == null) {
			if (e instanceof BufferOperationException) {
				this.error = e;
			} else {
				this.error = new BufferOperationException(e.getMessage(), e);
			}
		}
	}

	/**
	 * Returns a sparse array for gather NIO calls for all of the chained memory
	 * segments making up this memory buf.
	 *
	 * @return the sparse array make up of all data areas.
	 */
	@Override
	public ByteBuffer[] toArray() {
		var memory = boundMemory();
		var arr = new ByteBuffer[memory.segmentCount()];

		// Track position within the chain
		long chainPosition = 0;
		long bufPosition = position();
		long bufLimit = limit();

		for (int i = 0; i < arr.length; i++) {
			var byteBuffer = memory.asByteBuffer();
			arr[i] = byteBuffer;

			long segmentLength = memory.length();

			// Adjust position for first segment
			if (i == 0 && bufPosition > chainPosition) {
				// Position is within this segment
				long adjustedPos = bufPosition - chainPosition;
				byteBuffer.position((int) adjustedPos);
			}

			// Adjust limit for last segment or when limit falls within current segment
			if (bufLimit <= chainPosition + segmentLength) {
				// Limit is within this segment
				long adjustedLimit = bufLimit - chainPosition;
				byteBuffer.limit((int) adjustedLimit);

				// If we've reached the limit, subsequent segments should be empty
				if (i < arr.length - 1) {
					// Truncate array to exclude segments beyond limit
					var truncated = new ByteBuffer[i + 1];
					System.arraycopy(arr, 0, truncated, 0, i + 1);
					return truncated;
				}
			}

			chainPosition += segmentLength;
			memory = memory.nextSegment();

			assert memory != null || (memory == null && i == arr.length - 1);
		}

		return arr;
	}

	/**
	 * Called when buffer is initialized with capacity.
	 */
	protected void initializeBuffer(long capacity) {
		this.position = 0;
		this.limit = capacity;
		this.mark = -1;
		this.error = null;
	}

	// ==================== Helper Methods for Memory Access ====================

	/**
	 * Gets the memory view for direct access.
	 */
	@Override
	public MemoryView view() {
		if (!isBound()) {
			throw new IllegalStateException("Buffer is not bound to memory");
		}
		return boundView.view();
	}

	/**
	 * Gets the memory segment for VarHandle operations.
	 */
	public MemorySegment segment() {
		return view().segment;
	}

	/**
	 * Gets the base offset in the segment.
	 */
	public long start() {
		return view().start;
	}

	// ==================== Relative Get Operations ====================

	/**
	 * Reads a byte at the current position.
	 */
	public byte get() {
		if (hasError())
			return 0;
		if (remaining() < 1) {
			setError(new IllegalStateException("Buffer underflow"));
			return 0;
		}

		byte result = (byte) BYTE_HANDLE.get(segment(), start() + position);
		position++;
		return result;
	}

	/**
	 * Reads bytes into array.
	 */
	public ByteBuf get(byte[] dst) {
		if (dst == null) {
			setError(new BufferOperationException("Destination array is null"));
			return this;
		}
		return get(dst, 0, dst.length);
	}

	/**
	 * Reads bytes into array portion.
	 */
	public ByteBuf get(byte[] dst, int offset, int length) {
		if (hasError())
			return this;

		if (dst == null) {
			setError(new BufferOperationException("Destination array is null"));
			return this;
		}

		if (offset < 0 || length < 0 || offset + length > dst.length) {
			setError(new BufferOperationException(
					String.format("Invalid array bounds: offset=%d, length=%d, array.length=%d",
							offset, length, dst.length)));
			return this;
		}

		if (remaining() < length) {
			setError(new BufferOperationException("Buffer underflow"));
			return this;
		}

		if (length > 0) {
			try {
				MemorySegment.copy(segment(), start() + position,
						MemorySegment.ofArray(dst), offset, length);
				position += length;
			} catch (Exception e) {
				setError(new BufferOperationException("Failed to read from buffer", e));
			}
		}

		return this;
	}

	/**
	 * Fills the memory buffer between position and limit with fillValue.
	 */
	public ByteBuf fill(byte fillValue) {
		if (position == 0 && limit == capacity())
			view().segment
					.fill(fillValue);
		else
			view().segment
					.asSlice(view().start + position, remaining())
					.fill(fillValue);

		return this;
	}

	/**
	 * Reads a short at the current position.
	 */
	public short getShort() {
		if (hasError())
			return 0;
		if (remaining() < 2) {
			setError(new IllegalStateException("Buffer underflow"));
			return 0;
		}

		short result = (short) SHORT_HANDLE.get(segment(), start() + position);
		position += 2;
		return result;
	}

	/**
	 * Reads an int at the current position.
	 */
	public int getInt() {
		if (hasError())
			return 0;
		if (remaining() < 4) {
			setError(new IllegalStateException("Buffer underflow"));
			return 0;
		}

		int result = (int) INT_HANDLE.get(segment(), start() + position);
		position += 4;
		return result;
	}

	/**
	 * Reads a long at the current position.
	 */
	public long getLong() {
		if (hasError())
			return 0;
		if (remaining() < 8) {
			setError(new IllegalStateException("Buffer underflow"));
			return 0;
		}

		long result = (long) LONG_HANDLE.get(segment(), start() + position);
		position += 8;
		return result;
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

		float result = (float) FLOAT_HANDLE.get(segment(), start() + position);
		position += 4;
		return result;
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

		double result = (double) DOUBLE_HANDLE.get(segment(), start() + position);
		position += 8;
		return result;
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

		char result = (char) CHAR_HANDLE.get(segment(), start() + position);
		position += 2;
		return result;
	}

	// ==================== Indexed Get Operations ====================

	/**
	 * Reads a byte at the specified index.
	 */
	public byte get(long index) {
		if (hasError())
			return 0;
		if (index < 0 || index >= limit) {
			setError(new IllegalArgumentException("Index out of bounds"));
			return 0;
		}

		return (byte) BYTE_HANDLE.get(segment(), start() + index);
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
		return (short) SHORT_HANDLE.get(segment(), start() + index);
	}

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
		return (int) INT_HANDLE.get(segment(), start() + index);
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
		return (long) LONG_HANDLE.get(segment(), start() + index);
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
		return (float) FLOAT_HANDLE.get(segment(), start() + index);
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
		return (double) DOUBLE_HANDLE.get(segment(), start() + index);
	}

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
		return (char) CHAR_HANDLE.get(segment(), start() + index);
	}

	// ==================== Big-Endian Get Operations ====================

	/**
	 * Reads a short in big-endian byte order.
	 */
	public short getShortBE() {
		if (hasError())
			return 0;
		if (remaining() < 2) {
			setError(new IllegalStateException("Buffer underflow"));
			return 0;
		}

		short result = (short) SHORT_BE_HANDLE.get(segment(), start() + position);
		position += 2;
		return result;
	}

	/**
	 * Reads an int in big-endian byte order.
	 */
	public int getIntBE() {
		if (hasError())
			return 0;
		if (remaining() < 4) {
			setError(new IllegalStateException("Buffer underflow"));
			return 0;
		}

		int result = (int) INT_BE_HANDLE.get(segment(), start() + position);
		position += 4;
		return result;
	}

	/**
	 * Reads a long in big-endian byte order.
	 */
	public long getLongBE() {
		if (hasError())
			return 0;
		if (remaining() < 8) {
			setError(new IllegalStateException("Buffer underflow"));
			return 0;
		}

		long result = (long) LONG_BE_HANDLE.get(segment(), start() + position);
		position += 8;
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
		return (short) SHORT_BE_HANDLE.get(segment(), start() + index);
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
		return (int) INT_BE_HANDLE.get(segment(), start() + index);
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
		return (long) LONG_BE_HANDLE.get(segment(), start() + index);
	}

	// ==================== Relative Put Operations ====================

	/**
	 * Writes a byte at the current position.
	 */
	public ByteBuf put(byte value) {
		if (hasError())
			return this;
		if (remaining() < 1) {
			setError(new IllegalStateException("Buffer overflow"));
			return this;
		}

		BYTE_HANDLE.set(segment(), start() + position, value);
		position++;
		return this;
	}

	/**
	 * Writes a byte array.
	 */
	public ByteBuf put(byte[] src) {
		if (src == null) {
			setError(new BufferOperationException("Source array is null"));
			return this;
		}
		return put(src, 0, src.length);
	}

	/**
	 * Writes a portion of a byte array.
	 */
	public ByteBuf put(byte[] src, int offset, int length) {
		if (hasError())
			return this;

		if (src == null) {
			setError(new BufferOperationException("Source array is null"));
			return this;
		}

		if (offset < 0 || length < 0 || offset + length > src.length) {
			setError(new BufferOperationException(
					String.format("Invalid array bounds: offset=%d, length=%d, array.length=%d",
							offset, length, src.length)));
			return this;
		}

		if (remaining() < length) {
			setError(new BufferOperationException("Buffer overflow"));
			return this;
		}

		if (length > 0) {
			try {
				MemorySegment.copy(MemorySegment.ofArray(src), offset,
						segment(), start() + position, length);
				position += length;
			} catch (Exception e) {
				setError(new BufferOperationException("Failed to write to buffer", e));
			}
		}

		return this;
	}

	/**
	 * Writes from another ByteBuf.
	 */
	public ByteBuf put(ByteBuf src) {
		if (src == null) {
			setError(new BufferOperationException("Source buffer is null"));
			return this;
		}
		return put(src, 0, src.remaining());
	}

	/**
	 * Writes a portion of another ByteBuf.
	 */
	public ByteBuf put(ByteBuf src, long offset, long length) {
		if (hasError())
			return this;

		if (src == null) {
			setError(new BufferOperationException("Source buffer is null"));
			return this;
		}

		if (!src.isBound()) {
			setError(new BufferOperationException("Source buffer is not bound"));
			return this;
		}

		if (offset < 0 || length < 0 || offset + length > src.limit()) {
			setError(new BufferOperationException(
					String.format("Invalid bounds: offset=%d, length=%d, src.limit=%d",
							offset, length, src.limit())));
			return this;
		}

		if (remaining() < length) {
			setError(new BufferOperationException("Buffer overflow"));
			return this;
		}

		if (length > 0) {
			try {
				MemorySegment.copy(src.segment(), src.start() + offset,
						segment(), start() + position, length);
				position += length;
			} catch (Exception e) {
				setError(new BufferOperationException("Failed to write to buffer", e));
			}
		}

		return this;
	}

	/**
	 * Writes a short at the current position.
	 */
	public ByteBuf putShort(short value) {
		if (hasError())
			return this;
		if (remaining() < 2) {
			setError(new IllegalStateException("Buffer overflow"));
			return this;
		}

		SHORT_HANDLE.set(segment(), start() + position, value);
		position += 2;
		return this;
	}

	/**
	 * Writes an int at the current position.
	 */
	public ByteBuf putInt(int value) {
		if (hasError())
			return this;
		if (remaining() < 4) {
			setError(new IllegalStateException("Buffer overflow"));
			return this;
		}

		INT_HANDLE.set(segment(), start() + position, value);
		position += 4;
		return this;
	}

	/**
	 * Writes a long at the current position.
	 */
	public ByteBuf putLong(long value) {
		if (hasError())
			return this;
		if (remaining() < 8) {
			setError(new IllegalStateException("Buffer overflow"));
			return this;
		}

		LONG_HANDLE.set(segment(), start() + position, value);
		position += 8;
		return this;
	}

	/**
	 * Writes a float at the current position.
	 */
	public ByteBuf putFloat(float value) {
		if (hasError())
			return this;
		if (remaining() < 4) {
			setError(new IllegalStateException("Buffer overflow"));
			return this;
		}

		FLOAT_HANDLE.set(segment(), start() + position, value);
		position += 4;
		return this;
	}

	/**
	 * Writes a double at the current position.
	 */
	public ByteBuf putDouble(double value) {
		if (hasError())
			return this;
		if (remaining() < 8) {
			setError(new IllegalStateException("Buffer overflow"));
			return this;
		}

		DOUBLE_HANDLE.set(segment(), start() + position, value);
		position += 8;
		return this;
	}

	/**
	 * Writes a char at the current position.
	 */
	public ByteBuf putChar(char value) {
		if (hasError())
			return this;
		if (remaining() < 2) {
			setError(new IllegalStateException("Buffer overflow"));
			return this;
		}

		CHAR_HANDLE.set(segment(), start() + position, value);
		position += 2;
		return this;
	}

	// ==================== Indexed Put Operations ====================

	/**
	 * Writes a byte at the specified index.
	 */
	public ByteBuf put(long index, byte value) {
		if (hasError())
			return this;
		if (index < 0 || index >= limit) {
			setError(new IllegalArgumentException("Index out of bounds"));
			return this;
		}
		BYTE_HANDLE.set(segment(), start() + index, value);
		return this;
	}

	/**
	 * Writes a short at the specified index.
	 */
	public ByteBuf putShort(long index, short value) {
		if (hasError())
			return this;
		if (index < 0 || index + 2 > limit) {
			setError(new IllegalArgumentException("Index out of bounds"));
			return this;
		}
		SHORT_HANDLE.set(segment(), start() + index, value);
		return this;
	}

	/**
	 * Writes an int at the specified index.
	 */
	public ByteBuf putInt(long index, int value) {
		if (hasError())
			return this;
		if (index < 0 || index + 4 > limit) {
			setError(new IllegalArgumentException("Index out of bounds"));
			return this;
		}
		INT_HANDLE.set(segment(), start() + index, value);
		return this;
	}

	/**
	 * Writes a long at the specified index.
	 */
	public ByteBuf putLong(long index, long value) {
		if (hasError())
			return this;
		if (index < 0 || index + 8 > limit) {
			setError(new IllegalArgumentException("Index out of bounds"));
			return this;
		}
		LONG_HANDLE.set(segment(), start() + index, value);
		return this;
	}

	/**
	 * Writes a float at the specified index.
	 */
	public ByteBuf putFloat(long index, float value) {
		if (hasError())
			return this;
		if (index < 0 || index + 4 > limit) {
			setError(new IllegalArgumentException("Index out of bounds"));
			return this;
		}
		FLOAT_HANDLE.set(segment(), start() + index, value);
		return this;
	}

	/**
	 * Writes a double at the specified index.
	 */
	public ByteBuf putDouble(long index, double value) {
		if (hasError())
			return this;
		if (index < 0 || index + 8 > limit) {
			setError(new IllegalArgumentException("Index out of bounds"));
			return this;
		}
		DOUBLE_HANDLE.set(segment(), start() + index, value);
		return this;
	}

	/**
	 * Writes a char at the specified index.
	 */
	public ByteBuf putChar(long index, char value) {
		if (hasError())
			return this;
		if (index < 0 || index + 2 > limit) {
			setError(new IllegalArgumentException("Index out of bounds"));
			return this;
		}
		CHAR_HANDLE.set(segment(), start() + index, value);
		return this;
	}

	// ==================== Big-Endian Put Operations ====================

	/**
	 * Writes a short in big-endian byte order.
	 */
	public ByteBuf putShortBE(short value) {
		if (hasError())
			return this;
		if (remaining() < 2) {
			setError(new IllegalStateException("Buffer overflow"));
			return this;
		}
		SHORT_BE_HANDLE.set(segment(), start() + position, value);
		position += 2;
		return this;
	}

	/**
	 * Writes an int in big-endian byte order.
	 */
	public ByteBuf putIntBE(int value) {
		if (hasError())
			return this;
		if (remaining() < 4) {
			setError(new IllegalStateException("Buffer overflow"));
			return this;
		}
		INT_BE_HANDLE.set(segment(), start() + position, value);
		position += 4;
		return this;
	}

	/**
	 * Writes a long in big-endian byte order.
	 */
	public ByteBuf putLongBE(long value) {
		if (hasError())
			return this;
		if (remaining() < 8) {
			setError(new IllegalStateException("Buffer overflow"));
			return this;
		}
		LONG_BE_HANDLE.set(segment(), start() + position, value);
		position += 8;
		return this;
	}

	// ==================== ByteBuffer Conversion ====================

	@Override
	public ByteBuffer asByteBuffer() {
		long off = view().start + position;
		long length = remaining();

		return segment().asSlice(off, length)
				.asByteBuffer();
	}

	public ByteBuffer asByteBuffer(long position, long length) {
		long off = view().start + position;

		return segment().asSlice(off, length)
				.asByteBuffer();
	}
}
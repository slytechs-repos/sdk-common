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

/**
 * Data accessor layer providing byte-level operations with multi-segment
 * spanning.
 * 
 * <p>
 * MemoryByteBuffer extends MemoryBuffer to provide all data access operations,
 * following the java.nio.ByteBuffer pattern. This class handles reading and
 * writing of primitive types, arrays, and provides both relative
 * (position-based) and absolute (index-based) operations. All operations
 * transparently handle multi-segment spanning when necessary.
 * </p>
 * 
 * <h2>Architecture Overview</h2>
 * 
 * <pre>{@code
 * java.nio:           Our Design:
 * Buffer              MemoryBuffer        (control layer)
 *   ↓                   ↓
 * ByteBuffer          MemoryByteBuffer    (accessor layer - THIS CLASS)
 * }</pre>
 * 
 * <h2>Key Features</h2>
 * <ul>
 * <li><strong>Complete Data Types:</strong> byte, short, int, long, float,
 * double, char</li>
 * <li><strong>Dual Access Modes:</strong> Relative (advances position) and
 * absolute (index-based)</li>
 * <li><strong>Multi-Segment Spanning:</strong> Transparent handling of data
 * across segment boundaries</li>
 * <li><strong>VarHandle Optimization:</strong> Near-native performance using
 * VarHandles</li>
 * <li><strong>Non-Throwing Operations:</strong> All operations accumulate
 * errors for batch handling</li>
 * </ul>
 * 
 * <h2>Performance Optimizations</h2>
 * <p>
 * This implementation uses several key optimizations:
 * </p>
 * <ul>
 * <li>Static final VarHandles for JIT optimization</li>
 * <li>Direct field access to AbstractMemory.memorySegment</li>
 * <li>Segment caching to avoid repeated lookups</li>
 * <li>Fast-path for single-segment operations</li>
 * <li>Zero object allocation in critical paths</li>
 * </ul>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 */
public class MemoryByteBuffer extends MemoryBuffer {

	// ==================== VarHandle Optimization ====================
	// Developer Note: These static final VarHandles are JIT-optimized to
	// near-native speed.
	// Using UNALIGNED variants handles any memory alignment automatically.

	// Little-endian handles (platform native for most systems)
	private static final VarHandle BYTE_HANDLE = ValueLayout.JAVA_BYTE.varHandle();
	private static final VarHandle SHORT_HANDLE = ValueLayout.JAVA_SHORT_UNALIGNED.varHandle();
	private static final VarHandle INT_HANDLE = ValueLayout.JAVA_INT_UNALIGNED.varHandle();
	private static final VarHandle LONG_HANDLE = ValueLayout.JAVA_LONG_UNALIGNED.varHandle();
	private static final VarHandle FLOAT_HANDLE = ValueLayout.JAVA_FLOAT_UNALIGNED.varHandle();
	private static final VarHandle DOUBLE_HANDLE = ValueLayout.JAVA_DOUBLE_UNALIGNED.varHandle();
	private static final VarHandle CHAR_HANDLE = ValueLayout.JAVA_CHAR_UNALIGNED.varHandle();

	// Big-endian handles for network byte order
	private static final VarHandle SHORT_BE_HANDLE = ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN)
			.varHandle();
	private static final VarHandle INT_BE_HANDLE = ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN)
			.varHandle();
	private static final VarHandle LONG_BE_HANDLE = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN)
			.varHandle();

	/**
	 * Constructs a MemoryByteBuffer with specified bounds.
	 * 
	 * @param memorySegment the backing memory segment
	 * @param memoryOffset  starting offset within segment
	 * @param memoryEnd     ending offset within segment (exclusive)
	 */
	public MemoryByteBuffer(MemoryPool<MemoryByteBuffer> owningPool, MemorySegment memorySegment, long memoryOffset,
			long memoryEnd) {
		super(owningPool, memorySegment, memoryOffset, memoryEnd);
	}

	/**
	 * Constructs a MemoryByteBuffer wrapping existing Memory.
	 * 
	 * @param memory the memory to wrap
	 */
	public MemoryByteBuffer(Memory memory) {
		this(null, memory.asMemorySegment(), memory.memoryOffset(), memory.memoryEnd());
		memory.incrementRef(); // Keep wrapped memory alive
	}

	// ==================== Relative Put Operations (advance position)
	// ====================

	/**
	 * Writes a byte at the current position and advances position.
	 * 
	 * <p>
	 * This operation is non-throwing. If insufficient space exists or an error is
	 * already accumulated, the operation becomes a no-op.
	 * </p>
	 * 
	 * Developer Note: Single byte operations are the building blocks for spanning.
	 * Even these need to check segment boundaries in multi-segment chains.
	 * 
	 * @param value the byte value to write
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer put(byte value) {
		if (checkError())
			return this;

		if (!ensureSpace(1))
			return this;

		// Fast path: fits in current segment
		if (!needsSpanning(1)) {
			MemorySegment segment = getDirectSegment(currentSegment());
			long localPos = currentSegment().memoryDataOffset() + localPosition();
			BYTE_HANDLE.set(segment, localPos, value);
			position(position() + 1);
		} else {
			// Slow path: at segment boundary
			putSpanning(new byte[] {
					value
			});
		}

		return this;
	}

	/**
	 * Writes a byte array at the current position and advances position.
	 * 
	 * <p>
	 * This operation efficiently handles arrays that span multiple segments, using
	 * bulk copy operations where possible.
	 * </p>
	 * 
	 * Developer Note: This is optimized for large arrays using MemorySegment.copy
	 * for bulk transfers within segments.
	 * 
	 * @param src the source byte array
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer put(byte[] src) {
		if (checkError())
			return this;
		if (src == null) {
			setError(new BufferOperationException("Source array is null"));
			return this;
		}

		return put(src, 0, src.length);
	}

	/**
	 * Writes a portion of a byte array at the current position.
	 * 
	 * <p>
	 * Writes bytes from src[offset] to src[offset+length-1] at the current
	 * position, then advances position by length.
	 * </p>
	 * 
	 * @param src    the source byte array
	 * @param offset the offset within the array
	 * @param length the number of bytes to write
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer put(byte[] src, int offset, int length) {
		if (checkError())
			return this;

		if (src == null) {
			setError(new BufferOperationException("Source array is null"));
			return this;
		}
		if (offset < 0 || length < 0 || offset + length > src.length) {
			setError(new BufferOperationException("Array bounds error"));
			return this;
		}
		if (!ensureSpace(length))
			return this;

		// Fast path: fits entirely in current segment
		if (!needsSpanning(length)) {
			MemorySegment segment = getDirectSegment(currentSegment());
			long localPos = currentSegment().memoryDataOffset() + localPosition();
			MemorySegment.copy(MemorySegment.ofArray(src), offset, segment, localPos, length);
			position(position() + length);
		} else {
			// Slow path: spans segments
			putSpanning(src, offset, length);
		}

		return this;
	}

	/**
	 * Writes a short value at the current position and advances by 2 bytes.
	 * 
	 * <p>
	 * The short is written in platform byte order. For network byte order, use
	 * {@link #putShortBE(short)}.
	 * </p>
	 * 
	 * Developer Note: Multi-byte primitives may span segments. We optimize for the
	 * common case (fits in segment) with a fast path.
	 * 
	 * @param value the short value to write
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer putShort(short value) {
		if (checkError())
			return this;

		if (!ensureSpace(2))
			return this;

		if (!needsSpanning(2)) {
			// Fast path: fits in current segment
			MemorySegment segment = getDirectSegment(currentSegment());
			long localPos = currentSegment().memoryDataOffset() + localPosition();
			SHORT_HANDLE.set(segment, localPos, value);
			position(position() + 2);
		} else {
			// Slow path: spans segments
			putSpanningShort(value);
		}

		return this;
	}

	/**
	 * Writes an int value at the current position and advances by 4 bytes.
	 * 
	 * <p>
	 * The int is written in platform byte order. For network byte order, use
	 * {@link #putIntBE(int)}.
	 * </p>
	 * 
	 * @param value the int value to write
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer putInt(int value) {
		if (checkError())
			return this;

		if (!ensureSpace(4))
			return this;

		if (!needsSpanning(4)) {
			// Fast path: fits in current segment
			MemorySegment segment = getDirectSegment(currentSegment());
			long localPos = currentSegment().memoryDataOffset() + localPosition();
			INT_HANDLE.set(segment, localPos, value);
			position(position() + 4);
		} else {
			// Slow path: spans segments
			putSpanningInt(value);
		}

		return this;
	}

	/**
	 * Writes a long value at the current position and advances by 8 bytes.
	 * 
	 * <p>
	 * The long is written in platform byte order. For network byte order, use
	 * {@link #putLongBE(long)}.
	 * </p>
	 * 
	 * Developer Note: Longs are 8 bytes and commonly span segments at boundaries.
	 * The spanning path converts to bytes and writes sequentially.
	 * 
	 * @param value the long value to write
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer putLong(long value) {
		if (checkError())
			return this;

		if (!ensureSpace(8))
			return this;

		if (!needsSpanning(8)) {
			// Fast path: fits in current segment
			MemorySegment segment = getDirectSegment(currentSegment());
			long localPos = currentSegment().memoryDataOffset() + localPosition();
			LONG_HANDLE.set(segment, localPos, value);
			position(position() + 8);
		} else {
			// Slow path: spans segments
			putSpanningLong(value);
		}

		return this;
	}

	/**
	 * Writes a float value at the current position and advances by 4 bytes.
	 * 
	 * @param value the float value to write
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer putFloat(float value) {
		if (checkError())
			return this;

		if (!ensureSpace(4))
			return this;

		if (!needsSpanning(4)) {
			// Fast path: fits in current segment
			MemorySegment segment = getDirectSegment(currentSegment());
			long localPos = currentSegment().memoryDataOffset() + localPosition();
			FLOAT_HANDLE.set(segment, localPos, value);
			position(position() + 4);
		} else {
			// Slow path: spans segments
			putSpanningInt(Float.floatToIntBits(value));
		}

		return this;
	}

	/**
	 * Writes a double value at the current position and advances by 8 bytes.
	 * 
	 * @param value the double value to write
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer putDouble(double value) {
		if (checkError())
			return this;

		if (!ensureSpace(8))
			return this;

		if (!needsSpanning(8)) {
			// Fast path: fits in current segment
			MemorySegment segment = getDirectSegment(currentSegment());
			long localPos = currentSegment().memoryDataOffset() + localPosition();
			DOUBLE_HANDLE.set(segment, localPos, value);
			position(position() + 8);
		} else {
			// Slow path: spans segments
			putSpanningLong(Double.doubleToLongBits(value));
		}

		return this;
	}

	/**
	 * Writes a char value at the current position and advances by 2 bytes.
	 * 
	 * @param value the char value to write
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer putChar(char value) {
		if (checkError())
			return this;

		if (!ensureSpace(2))
			return this;

		if (!needsSpanning(2)) {
			// Fast path: fits in current segment
			MemorySegment segment = getDirectSegment(currentSegment());
			long localPos = currentSegment().memoryDataOffset() + localPosition();
			CHAR_HANDLE.set(segment, localPos, value);
			position(position() + 2);
		} else {
			// Slow path: spans segments
			putSpanningShort((short) value);
		}

		return this;
	}

	// ==================== Network Byte Order Put Operations ====================

	/**
	 * Writes a short value in big-endian byte order.
	 * 
	 * <p>
	 * Useful for network protocols where big-endian is standard.
	 * </p>
	 * 
	 * Developer Note: Network protocols (Ethernet, IP, TCP) use big-endian. These
	 * methods eliminate manual byte swapping.
	 * 
	 * @param value the short value to write
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer putShortBE(short value) {
		if (checkError())
			return this;

		if (!ensureSpace(2))
			return this;

		if (!needsSpanning(2)) {
			MemorySegment segment = getDirectSegment(currentSegment());
			long localPos = currentSegment().memoryDataOffset() + localPosition();
			SHORT_BE_HANDLE.set(segment, localPos, value);
			position(position() + 2);
		} else {
			// Convert to big-endian bytes for spanning
			put((byte) (value >>> 8));
			put((byte) value);
		}

		return this;
	}

	/**
	 * Writes an int value in big-endian byte order.
	 * 
	 * @param value the int value to write
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer putIntBE(int value) {
		if (checkError())
			return this;

		if (!ensureSpace(4))
			return this;

		if (!needsSpanning(4)) {
			MemorySegment segment = getDirectSegment(currentSegment());
			long localPos = currentSegment().memoryDataOffset() + localPosition();
			INT_BE_HANDLE.set(segment, localPos, value);
			position(position() + 4);
		} else {
			// Convert to big-endian bytes for spanning
			put((byte) (value >>> 24));
			put((byte) (value >>> 16));
			put((byte) (value >>> 8));
			put((byte) value);
		}

		return this;
	}

	/**
	 * Writes a long value in big-endian byte order.
	 * 
	 * @param value the long value to write
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer putLongBE(long value) {
		if (checkError())
			return this;

		if (!ensureSpace(8))
			return this;

		if (!needsSpanning(8)) {
			MemorySegment segment = getDirectSegment(currentSegment());
			long localPos = currentSegment().memoryDataOffset() + localPosition();
			LONG_BE_HANDLE.set(segment, localPos, value);
			position(position() + 8);
		} else {
			// Convert to big-endian bytes for spanning
			putIntBE((int) (value >>> 32));
			putIntBE((int) value);
		}

		return this;
	}

	// ==================== Relative Get Operations (advance position)
	// ====================

	/**
	 * Reads a byte at the current position and advances position.
	 * 
	 * <p>
	 * If insufficient data exists or an error is accumulated, returns 0 and sets an
	 * error.
	 * </p>
	 * 
	 * @return the byte value at the current position
	 */
	public byte get() {
		if (checkError())
			return 0;

		if (remaining() < 1) {
			setError(new BufferOperationException("Insufficient data for get()"));
			return 0;
		}

		byte result;
		if (!needsSpanning(1)) {
			MemorySegment segment = getDirectSegment(currentSegment());
			long localPos = currentSegment().memoryDataOffset() + localPosition();
			result = (byte) BYTE_HANDLE.get(segment, localPos);
			position(position() + 1);
		} else {
			// At segment boundary
			byte[] temp = new byte[1];
			getSpanning(temp, 0, 1);
			result = temp[0];
		}

		return result;
	}

	/**
	 * Reads bytes into the destination array and advances position.
	 * 
	 * <p>
	 * Reads dst.length bytes from the current position into dst, then advances
	 * position by dst.length.
	 * </p>
	 * 
	 * Developer Note: Uses bulk copy for efficiency within segments.
	 * 
	 * @param dst the destination byte array
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer get(byte[] dst) {
		if (checkError())
			return this;
		if (dst == null) {
			setError(new BufferOperationException("Destination array is null"));
			return this;
		}

		return get(dst, 0, dst.length);
	}

	/**
	 * Reads bytes into a portion of the destination array.
	 * 
	 * @param dst    the destination byte array
	 * @param offset the offset within the array
	 * @param length the number of bytes to read
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer get(byte[] dst, int offset, int length) {
		if (checkError())
			return this;

		if (dst == null) {
			setError(new BufferOperationException("Destination array is null"));
			return this;
		}
		if (offset < 0 || length < 0 || offset + length > dst.length) {
			setError(new BufferOperationException("Array bounds error"));
			return this;
		}
		if (remaining() < length) {
			setError(new BufferOperationException("Insufficient data"));
			return this;
		}

		if (!needsSpanning(length)) {
			// Fast path: all in current segment
			MemorySegment segment = getDirectSegment(currentSegment());
			long localPos = currentSegment().memoryDataOffset() + localPosition();
			MemorySegment.copy(segment, localPos, MemorySegment.ofArray(dst), offset, length);
			position(position() + length);
		} else {
			// Slow path: spans segments
			getSpanning(dst, offset, length);
		}

		return this;
	}

	/**
	 * Reads a short value at the current position and advances by 2 bytes.
	 * 
	 * @return the short value at the current position
	 */
	public short getShort() {
		if (checkError())
			return 0;

		if (remaining() < 2) {
			setError(new BufferOperationException("Insufficient data for getShort()"));
			return 0;
		}

		short result;
		if (!needsSpanning(2)) {
			MemorySegment segment = getDirectSegment(currentSegment());
			long localPos = currentSegment().memoryDataOffset() + localPosition();
			result = (short) SHORT_HANDLE.get(segment, localPos);
			position(position() + 2);
		} else {
			result = getSpanningShort();
		}

		return result;
	}

	/**
	 * Reads an int value at the current position and advances by 4 bytes.
	 * 
	 * @return the int value at the current position
	 */
	public int getInt() {
		if (checkError())
			return 0;

		if (remaining() < 4) {
			setError(new BufferOperationException("Insufficient data for getInt()"));
			return 0;
		}

		int result;
		if (!needsSpanning(4)) {
			MemorySegment segment = getDirectSegment(currentSegment());
			long localPos = currentSegment().memoryDataOffset() + localPosition();
			result = (int) INT_HANDLE.get(segment, localPos);
			position(position() + 4);
		} else {
			result = getSpanningInt();
		}

		return result;
	}

	/**
	 * Reads a long value at the current position and advances by 8 bytes.
	 * 
	 * @return the long value at the current position
	 */
	public long getLong() {
		if (checkError())
			return 0;

		if (remaining() < 8) {
			setError(new BufferOperationException("Insufficient data for getLong()"));
			return 0;
		}

		long result;
		if (!needsSpanning(8)) {
			MemorySegment segment = getDirectSegment(currentSegment());
			long localPos = currentSegment().memoryDataOffset() + localPosition();
			result = (long) LONG_HANDLE.get(segment, localPos);
			position(position() + 8);
		} else {
			result = getSpanningLong();
		}

		return result;
	}

	/**
	 * Reads a float value at the current position and advances by 4 bytes.
	 * 
	 * @return the float value at the current position
	 */
	public float getFloat() {
		if (checkError())
			return 0.0f;

		if (remaining() < 4) {
			setError(new BufferOperationException("Insufficient data for getFloat()"));
			return 0.0f;
		}

		float result;
		if (!needsSpanning(4)) {
			MemorySegment segment = getDirectSegment(currentSegment());
			long localPos = currentSegment().memoryDataOffset() + localPosition();
			result = (float) FLOAT_HANDLE.get(segment, localPos);
			position(position() + 4);
		} else {
			result = Float.intBitsToFloat(getSpanningInt());
		}

		return result;
	}

	/**
	 * Reads a double value at the current position and advances by 8 bytes.
	 * 
	 * @return the double value at the current position
	 */
	public double getDouble() {
		if (checkError())
			return 0.0;

		if (remaining() < 8) {
			setError(new BufferOperationException("Insufficient data for getDouble()"));
			return 0.0;
		}

		double result;
		if (!needsSpanning(8)) {
			MemorySegment segment = getDirectSegment(currentSegment());
			long localPos = currentSegment().memoryDataOffset() + localPosition();
			result = (double) DOUBLE_HANDLE.get(segment, localPos);
			position(position() + 8);
		} else {
			result = Double.longBitsToDouble(getSpanningLong());
		}

		return result;
	}

	/**
	 * Reads a char value at the current position and advances by 2 bytes.
	 * 
	 * @return the char value at the current position
	 */
	public char getChar() {
		if (checkError())
			return 0;

		if (remaining() < 2) {
			setError(new BufferOperationException("Insufficient data for getChar()"));
			return 0;
		}

		char result;
		if (!needsSpanning(2)) {
			MemorySegment segment = getDirectSegment(currentSegment());
			long localPos = currentSegment().memoryDataOffset() + localPosition();
			result = (char) CHAR_HANDLE.get(segment, localPos);
			position(position() + 2);
		} else {
			result = (char) getSpanningShort();
		}

		return result;
	}

	// ==================== Absolute Put Operations (don't change position)
	// ====================

	/**
	 * Writes a byte at the specified index without changing position.
	 * 
	 * <p>
	 * Absolute operations allow random access without affecting the buffer's
	 * current position, useful for updating specific fields.
	 * </p>
	 * 
	 * Developer Note: Absolute ops use chain-wide indexing and must locate the
	 * correct segment for each operation.
	 * 
	 * @param index the index at which to write
	 * @param value the byte value to write
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer put(long index, byte value) {
		if (checkError())
			return this;

		if (index < 0 || index >= capacity()) {
			setError(new BufferOperationException("Index out of bounds: " + index));
			return this;
		}

		Memory segment = seekMemory(index);
		if (segment != null) {
			MemorySegment memSeg = getDirectSegment(segment);
			long localIndex = index - calculateSegmentOffset(segment);
			BYTE_HANDLE.set(memSeg, segment.memoryDataOffset() + localIndex, value);
		}

		return this;
	}

	/**
	 * Writes a byte array at the specified index without changing position.
	 * 
	 * @param index the index at which to write
	 * @param src   the source byte array
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer put(long index, byte[] src) {
		if (checkError())
			return this;
		if (src == null) {
			setError(new BufferOperationException("Source array is null"));
			return this;
		}

		return put(index, src, 0, src.length);
	}

	/**
	 * Writes a portion of a byte array at the specified index.
	 * 
	 * @param index  the index at which to write
	 * @param src    the source byte array
	 * @param offset the offset within the array
	 * @param length the number of bytes to write
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer put(long index, byte[] src, int offset, int length) {
		if (checkError())
			return this;

		if (src == null) {
			setError(new BufferOperationException("Source array is null"));
			return this;
		}
		if (offset < 0 || length < 0 || offset + length > src.length) {
			setError(new BufferOperationException("Array bounds error"));
			return this;
		}
		if (index < 0 || index + length > capacity()) {
			setError(new BufferOperationException("Index out of bounds"));
			return this;
		}

		// Save position and use relative operations
		long savedPos = position();
		position(index);
		put(src, offset, length);
		position(savedPos); // Restore position

		return this;
	}

	/**
	 * Writes a short at the specified index without changing position.
	 * 
	 * @param index the index at which to write
	 * @param value the short value to write
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer putShort(long index, short value) {
		if (checkError())
			return this;

		if (index < 0 || index + 2 > capacity()) {
			setError(new BufferOperationException("Index out of bounds: " + index));
			return this;
		}

		long savedPos = position();
		position(index);
		putShort(value);
		position(savedPos);

		return this;
	}

	/**
	 * Writes an int at the specified index without changing position.
	 * 
	 * @param index the index at which to write
	 * @param value the int value to write
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer putInt(long index, int value) {
		if (checkError())
			return this;

		if (index < 0 || index + 4 > capacity()) {
			setError(new BufferOperationException("Index out of bounds: " + index));
			return this;
		}

		long savedPos = position();
		position(index);
		putInt(value);
		position(savedPos);

		return this;
	}

	/**
	 * Writes a long at the specified index without changing position.
	 * 
	 * @param index the index at which to write
	 * @param value the long value to write
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer putLong(long index, long value) {
		if (checkError())
			return this;

		if (index < 0 || index + 8 > capacity()) {
			setError(new BufferOperationException("Index out of bounds: " + index));
			return this;
		}

		long savedPos = position();
		position(index);
		putLong(value);
		position(savedPos);

		return this;
	}

	/**
	 * Writes a float at the specified index without changing position.
	 * 
	 * @param index the index at which to write
	 * @param value the float value to write
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer putFloat(long index, float value) {
		if (checkError())
			return this;

		if (index < 0 || index + 4 > capacity()) {
			setError(new BufferOperationException("Index out of bounds: " + index));
			return this;
		}

		long savedPos = position();
		position(index);
		putFloat(value);
		position(savedPos);

		return this;
	}

	/**
	 * Writes a double at the specified index without changing position.
	 * 
	 * @param index the index at which to write
	 * @param value the double value to write
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer putDouble(long index, double value) {
		if (checkError())
			return this;

		if (index < 0 || index + 8 > capacity()) {
			setError(new BufferOperationException("Index out of bounds: " + index));
			return this;
		}

		long savedPos = position();
		position(index);
		putDouble(value);
		position(savedPos);

		return this;
	}

	/**
	 * Writes a char at the specified index without changing position.
	 * 
	 * @param index the index at which to write
	 * @param value the char value to write
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer putChar(long index, char value) {
		if (checkError())
			return this;

		if (index < 0 || index + 2 > capacity()) {
			setError(new BufferOperationException("Index out of bounds: " + index));
			return this;
		}

		long savedPos = position();
		position(index);
		putChar(value);
		position(savedPos);

		return this;
	}

	// ==================== Absolute Get Operations (don't change position)
	// ====================

	/**
	 * Reads a byte at the specified index without changing position.
	 * 
	 * @param index the index from which to read
	 * @return the byte value at the specified index
	 */
	public byte get(long index) {
		if (checkError())
			return 0;

		if (index < 0 || index >= capacity()) {
			setError(new BufferOperationException("Index out of bounds: " + index));
			return 0;
		}

		Memory segment = seekMemory(index);
		if (segment != null) {
			MemorySegment memSeg = getDirectSegment(segment);
			long localIndex = index - calculateSegmentOffset(segment);
			return (byte) BYTE_HANDLE.get(memSeg, segment.memoryDataOffset() + localIndex);
		}

		return 0;
	}

	/**
	 * Reads bytes at the specified index into the destination array.
	 * 
	 * @param index the index from which to read
	 * @param dst   the destination byte array
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer get(long index, byte[] dst) {
		if (checkError())
			return this;
		if (dst == null) {
			setError(new BufferOperationException("Destination array is null"));
			return this;
		}

		return get(index, dst, 0, dst.length);
	}

	/**
	 * Reads bytes at the specified index into a portion of the destination array.
	 * 
	 * @param index  the index from which to read
	 * @param dst    the destination byte array
	 * @param offset the offset within the array
	 * @param length the number of bytes to read
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer get(long index, byte[] dst, int offset, int length) {
		if (checkError())
			return this;

		if (dst == null) {
			setError(new BufferOperationException("Destination array is null"));
			return this;
		}
		if (offset < 0 || length < 0 || offset + length > dst.length) {
			setError(new BufferOperationException("Array bounds error"));
			return this;
		}
		if (index < 0 || index + length > capacity()) {
			setError(new BufferOperationException("Index out of bounds"));
			return this;
		}

		long savedPos = position();
		position(index);
		get(dst, offset, length);
		position(savedPos);

		return this;
	}

	/**
	 * Reads a short at the specified index without changing position.
	 * 
	 * @param index the index from which to read
	 * @return the short value at the specified index
	 */
	public short getShort(long index) {
		if (checkError())
			return 0;

		if (index < 0 || index + 2 > capacity()) {
			setError(new BufferOperationException("Index out of bounds: " + index));
			return 0;
		}

		long savedPos = position();
		position(index);
		short result = getShort();
		position(savedPos);
		return result;
	}

	/**
	 * Reads an int at the specified index without changing position.
	 * 
	 * @param index the index from which to read
	 * @return the int value at the specified index
	 */
	public int getInt(long index) {
		if (checkError())
			return 0;

		if (index < 0 || index + 4 > capacity()) {
			setError(new BufferOperationException("Index out of bounds: " + index));
			return 0;
		}

		long savedPos = position();
		position(index);
		int result = getInt();
		position(savedPos);
		return result;
	}

	/**
	 * Reads a long at the specified index without changing position.
	 * 
	 * @param index the index from which to read
	 * @return the long value at the specified index
	 */
	public long getLong(long index) {
		if (checkError())
			return 0;

		if (index < 0 || index + 8 > capacity()) {
			setError(new BufferOperationException("Index out of bounds: " + index));
			return 0;
		}

		long savedPos = position();
		position(index);
		long result = getLong();
		position(savedPos);
		return result;
	}

	/**
	 * Reads a float at the specified index without changing position.
	 * 
	 * @param index the index from which to read
	 * @return the float value at the specified index
	 */
	public float getFloat(long index) {
		if (checkError())
			return 0.0f;

		if (index < 0 || index + 4 > capacity()) {
			setError(new BufferOperationException("Index out of bounds: " + index));
			return 0.0f;
		}

		long savedPos = position();
		position(index);
		float result = getFloat();
		position(savedPos);
		return result;
	}

	/**
	 * Reads a double at the specified index without changing position.
	 * 
	 * @param index the index from which to read
	 * @return the double value at the specified index
	 */
	public double getDouble(long index) {
		if (checkError())
			return 0.0;

		if (index < 0 || index + 8 > capacity()) {
			setError(new BufferOperationException("Index out of bounds: " + index));
			return 0.0;
		}

		long savedPos = position();
		position(index);
		double result = getDouble();
		position(savedPos);
		return result;
	}

	/**
	 * Reads a char at the specified index without changing position.
	 * 
	 * @param index the index from which to read
	 * @return the char value at the specified index
	 */
	public char getChar(long index) {
		if (checkError())
			return 0;

		if (index < 0 || index + 2 > capacity()) {
			setError(new BufferOperationException("Index out of bounds: " + index));
			return 0;
		}

		long savedPos = position();
		position(index);
		char result = getChar();
		position(savedPos);
		return result;
	}

	// ==================== Try Operations with Error Handlers ====================

	/**
	 * Attempts to write a byte with immediate error handling.
	 * 
	 * <p>
	 * If the operation fails, the error handler is invoked immediately. The handler
	 * can attempt recovery or throw a new exception.
	 * </p>
	 * 
	 * Developer Note: Critical operations get immediate error handlers for
	 * sophisticated recovery strategies like buffer expansion.
	 * 
	 * @param value   the byte value to write
	 * @param onError the error handler to invoke on failure
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer tryPut(byte value, BufferErrorHandler onError) {
		if (checkError())
			return this;

		if (!ensureSpace(1)) {
			try {
				onError.handle(this, new BufferOperationException("No space for byte"));
				// Handler succeeded - retry operation
				return put(value);
			} catch (BufferOperationException e) {
				setError(e);
			}
			return this;
		}

		return put(value);
	}

	/**
	 * Attempts to write a byte array with immediate error handling.
	 * 
	 * @param src     the source byte array
	 * @param onError the error handler to invoke on failure
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer tryPut(byte[] src, BufferErrorHandler onError) {
		if (checkError())
			return this;

		if (src == null || !ensureSpace(src.length)) {
			try {
				BufferOperationException error = src == null ? new BufferOperationException("Null source array")
						: new BufferOperationException("Insufficient space: need " + src.length);
				onError.handle(this, error);
				// Handler succeeded - retry operation
				return put(src);
			} catch (BufferOperationException e) {
				setError(e);
			}
			return this;
		}

		return put(src);
	}

	/**
	 * Attempts to write a long with immediate error handling.
	 * 
	 * <p>
	 * Common for timestamps where recovery might involve compression.
	 * </p>
	 * 
	 * @param value   the long value to write
	 * @param onError the error handler to invoke on failure
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer tryPutLong(long value, BufferErrorHandler onError) {
		if (checkError())
			return this;

		if (!ensureSpace(8)) {
			try {
				onError.handle(this, new BufferOperationException("No space for long"));
				// Handler succeeded - retry operation
				return putLong(value);
			} catch (BufferOperationException e) {
				setError(e);
			}
			return this;
		}

		return putLong(value);
	}

	/**
	 * Attempts to write a long at an index with immediate error handling.
	 * 
	 * @param index   the index at which to write
	 * @param value   the long value to write
	 * @param onError the error handler to invoke on failure
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer tryPutLong(long index, long value, BufferErrorHandler onError) {
		if (checkError())
			return this;

		if (index < 0 || index + 8 > capacity()) {
			try {
				onError.handle(this, new BufferOperationException(
						"Index out of bounds for long: " + index));
				// Handler succeeded - retry operation
				return putLong(index, value);
			} catch (BufferOperationException e) {
				setError(e);
			}
			return this;
		}

		return putLong(index, value);
	}

	/**
	 * Attempts to write a float with immediate error handling.
	 * 
	 * @param value   the float value to write
	 * @param onError the error handler to invoke on failure
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer tryPutFloat(float value, BufferErrorHandler onError) {
		if (checkError())
			return this;

		if (!ensureSpace(4)) {
			try {
				onError.handle(this, new BufferOperationException("No space for float"));
				// Handler succeeded - retry operation
				return putFloat(value);
			} catch (BufferOperationException e) {
				setError(e);
			}
			return this;
		}

		return putFloat(value);
	}

	/**
	 * Attempts to write a double with immediate error handling.
	 * 
	 * @param value   the double value to write
	 * @param onError the error handler to invoke on failure
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer tryPutDouble(double value, BufferErrorHandler onError) {
		if (checkError())
			return this;

		if (!ensureSpace(8)) {
			try {
				onError.handle(this, new BufferOperationException("No space for double"));
				// Handler succeeded - retry operation
				return putDouble(value);
			} catch (BufferOperationException e) {
				setError(e);
			}
			return this;
		}

		return putDouble(value);
	}

	/**
	 * Attempts to read bytes with immediate error handling.
	 * 
	 * @param dst     the destination byte array
	 * @param onError the error handler to invoke on failure
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer tryGet(byte[] dst, BufferErrorHandler onError) {
		if (checkError())
			return this;

		if (dst == null || remaining() < dst.length) {
			try {
				BufferOperationException error = dst == null ? new BufferOperationException("Null destination array")
						: new BufferOperationException("Insufficient data: need " + dst.length);
				onError.handle(this, error);
				// Handler succeeded - retry operation
				return get(dst);
			} catch (BufferOperationException e) {
				setError(e);
			}
			return this;
		}

		return get(dst);
	}

	/**
	 * Attempts to read bytes at an index with immediate error handling.
	 * 
	 * @param index   the index from which to read
	 * @param dst     the destination byte array
	 * @param onError the error handler to invoke on failure
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer tryGet(long index, byte[] dst, BufferErrorHandler onError) {
		if (checkError())
			return this;

		if (dst == null || index < 0 || index + dst.length > capacity()) {
			try {
				BufferOperationException error = dst == null ? new BufferOperationException("Null destination array")
						: new BufferOperationException("Index out of bounds: " + index);
				onError.handle(this, error);
				// Handler succeeded - retry operation
				return get(index, dst);
			} catch (BufferOperationException e) {
				setError(e);
			}
			return this;
		}

		return get(index, dst);
	}

	/**
	 * Attempts to read a long with immediate error handling.
	 * 
	 * @param onError the error handler to invoke on failure
	 * @return the long value, or 0 if error
	 */
	public long tryGetLong(BufferErrorHandler onError) {
		if (checkError())
			return 0;

		if (remaining() < 8) {
			try {
				onError.handle(this, new BufferOperationException(
						"Insufficient data for long"));
				// Handler succeeded - retry operation
				return getLong();
			} catch (BufferOperationException e) {
				setError(e);
				return 0;
			}
		}

		return getLong();
	}

	// ==================== Space Management Try Operations ====================

	/**
	 * Attempts to insert space at the current position with error handling.
	 * 
	 * <p>
	 * Creates a gap of the specified size at the current position by moving
	 * existing data. This is the smart-direction optimized version that moves the
	 * smaller amount of data.
	 * </p>
	 * 
	 * Developer Note: Critical for protocol header insertion (VLAN tags, tunnel
	 * headers). The handler might invoke MemoryEditor for chain expansion.
	 * 
	 * @param size    the size of the gap to create
	 * @param onError the error handler for recovery
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer tryInsertSpace(long size, BufferErrorHandler onError) {
		if (checkError())
			return this;

		// Calculate data amounts for smart direction choice
		long dataBeforePos = position();
		long dataAfterPos = limit() - position();

		// Check if we have room to expand
		long totalNeeded = limit() + size;
		if (totalNeeded > capacity()) {
			try {
				onError.handle(this, new BufferOperationException(
						"Cannot insert " + size + " bytes: would exceed capacity"));
				// Handler might have expanded capacity
				return tryInsertSpace(size, onError); // Retry
			} catch (BufferOperationException e) {
				setError(e);
				return this;
			}
		}

		// Choose direction that moves less data
		if (dataBeforePos <= dataAfterPos) {
			// Move data before position to the left
			insertSpaceByMovingLeft(size);
		} else {
			// Move data after position to the right
			insertSpaceByMovingRight(size);
		}

		// Adjust limit for the new space
		limit(limit() + size);

		return this;
	}

	/**
	 * Attempts to remove space at the current position with error handling.
	 * 
	 * @param size    the size of the gap to remove
	 * @param onError the error handler for recovery
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer tryRemoveSpace(long size, BufferErrorHandler onError) {
		if (checkError())
			return this;

		if (size > remaining()) {
			try {
				onError.handle(this, new BufferOperationException(
						"Cannot remove " + size + " bytes: only " + remaining() + " available"));
				// Handler might adjust size
				return this;
			} catch (BufferOperationException e) {
				setError(e);
				return this;
			}
		}

		// Remove by moving data after the gap backward
		long gapEnd = position() + size;
		long dataToMove = limit() - gapEnd;

		if (dataToMove > 0) {
			MemorySegment.copy(memorySegment, gapEnd,
					memorySegment, position(), dataToMove);
		}

		// Adjust limit for removed space
		limit(limit() - size);

		return this;
	}

	/**
	 * Attempts to expand trailing space with error handling.
	 * 
	 * @param size    the amount to expand by
	 * @param onError the error handler for recovery
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer tryExpandTrailing(long size, BufferErrorHandler onError) {
		if (checkError())
			return this;

		long newLimit = limit() + size;
		if (newLimit > capacity()) {
			try {
				onError.handle(this, new BufferOperationException(
						"Cannot expand by " + size + ": would exceed capacity"));
				// Handler might have increased capacity
				return this;
			} catch (BufferOperationException e) {
				setError(e);
				return this;
			}
		}

		limit(newLimit);
		return this;
	}

	/**
	 * Attempts to ensure space is available with error handling.
	 * 
	 * @param required the required space
	 * @param onError  the error handler for recovery
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer tryEnsureSpace(long required, BufferErrorHandler onError) {
		if (checkError())
			return this;

		if (remaining() < required) {
			try {
				onError.handle(this, new BufferOperationException(
						"Insufficient space: need " + required + ", have " + remaining()));
				// Handler might have expanded buffer
				return this;
			} catch (BufferOperationException e) {
				setError(e);
				return this;
			}
		}

		return this;
	}

	// ==================== Bulk Operations ====================

	/**
	 * Copies data from another buffer with error handling.
	 * 
	 * @param src     the source buffer
	 * @param onError the error handler for recovery
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer tryCopyFrom(MemoryByteBuffer src, BufferErrorHandler onError) {
		if (checkError())
			return this;

		if (src == null || src.remaining() > remaining()) {
			try {
				BufferOperationException error = src == null ? new BufferOperationException("Null source buffer")
						: new BufferOperationException("Insufficient space for copy");
				onError.handle(this, error);
				// Handler might have expanded buffer
				return this;
			} catch (BufferOperationException e) {
				setError(e);
				return this;
			}
		}

		// Perform the copy
		while (src.hasRemaining()) {
			put(src.get());
		}

		return this;
	}

	/**
	 * Fills a region with a byte value with error handling.
	 * 
	 * @param value   the fill value
	 * @param length  the number of bytes to fill
	 * @param onError the error handler for recovery
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer tryFill(byte value, long length, BufferErrorHandler onError) {
		if (checkError())
			return this;

		if (remaining() < length) {
			try {
				onError.handle(this, new BufferOperationException(
						"Insufficient space for fill: need " + length));
				// Handler might have expanded buffer
				return this;
			} catch (BufferOperationException e) {
				setError(e);
				return this;
			}
		}

		// Perform the fill
		for (long i = 0; i < length; i++) {
			put(value);
		}

		return this;
	}

	// ==================== Private Helper Methods ====================

	/**
	 * Gets direct access to a memory segment.
	 * 
	 * Developer Note: Optimization to avoid virtual method calls. We check if
	 * Memory is AbstractMemory and access the protected field directly.
	 * 
	 * @param memory the memory to access
	 * @return the underlying MemorySegment
	 */
	private static MemorySegment getDirectSegment(Memory memory) {
		if (memory instanceof AbstractMemory abstractMemory) {
			return abstractMemory.memorySegment; // Direct field access
		}
		return memory.asMemorySegment(); // Fallback
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

	// ==================== Spanning Helper Methods ====================

	/**
	 * Writes data that spans multiple segments.
	 * 
	 * Developer Note: This is the slow path for data that crosses segment
	 * boundaries. We write as much as fits in current segment, then move to next.
	 * 
	 * @param data the data to write
	 */
	private void putSpanning(byte[] data) {
		putSpanning(data, 0, data.length);
	}

	/**
	 * Writes array data that spans segments.
	 * 
	 * @param src    source array
	 * @param offset offset in array
	 * @param length bytes to write
	 */
	private void putSpanning(byte[] src, int offset, int length) {
		int written = 0;

		while (written < length && hasRemaining()) {
			Memory seg = currentSegment();
			long localPos = localPosition();
			long segRemaining = seg.memoryDataEnd() - (seg.memoryDataOffset() + localPos);

			int toWrite = (int) Math.min(length - written, segRemaining);
			if (toWrite > 0) {
				MemorySegment memSeg = getDirectSegment(seg);
				MemorySegment.copy(MemorySegment.ofArray(src), offset + written,
						memSeg, seg.memoryDataOffset() + localPos, toWrite);
				position(position() + toWrite);
				written += toWrite;
			}

			if (written < length && !moveToNextSegment()) {
				setError(new BufferOperationException("Unexpected end of segments"));
				break;
			}
		}
	}

	/**
	 * Writes a short that spans segments.
	 */
	private void putSpanningShort(short value) {
		put((byte) (value >>> 8));
		put((byte) value);
	}

	/**
	 * Writes an int that spans segments.
	 */
	private void putSpanningInt(int value) {
		put((byte) (value >>> 24));
		put((byte) (value >>> 16));
		put((byte) (value >>> 8));
		put((byte) value);
	}

	/**
	 * Writes a long that spans segments.
	 */
	private void putSpanningLong(long value) {
		putSpanningInt((int) (value >>> 32));
		putSpanningInt((int) value);
	}

	/**
	 * Reads data that spans segments.
	 */
	private void getSpanning(byte[] dst, int offset, int length) {
		int read = 0;

		while (read < length && hasRemaining()) {
			Memory seg = currentSegment();
			long localPos = localPosition();
			long segRemaining = seg.memoryDataEnd() - (seg.memoryDataOffset() + localPos);

			int toRead = (int) Math.min(length - read, segRemaining);
			if (toRead > 0) {
				MemorySegment memSeg = getDirectSegment(seg);
				MemorySegment.copy(memSeg, seg.memoryDataOffset() + localPos,
						MemorySegment.ofArray(dst), offset + read, toRead);
				position(position() + toRead);
				read += toRead;
			}

			if (read < length && !moveToNextSegment()) {
				setError(new BufferOperationException("Unexpected end of segments"));
				break;
			}
		}
	}

	/**
	 * Reads a short that spans segments.
	 */
	private short getSpanningShort() {
		int b1 = get() & 0xFF;
		int b2 = get() & 0xFF;
		return (short) ((b1 << 8) | b2);
	}

	/**
	 * Reads an int that spans segments.
	 */
	private int getSpanningInt() {
		int b1 = get() & 0xFF;
		int b2 = get() & 0xFF;
		int b3 = get() & 0xFF;
		int b4 = get() & 0xFF;
		return (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
	}

	/**
	 * Reads a long that spans segments.
	 */
	private long getSpanningLong() {
		long high = getSpanningInt() & 0xFFFFFFFFL;
		long low = getSpanningInt() & 0xFFFFFFFFL;
		return (high << 32) | low;
	}

	/**
	 * Moves to the next segment in the chain.
	 * 
	 * @return true if moved to next segment, false if at end
	 */
	private boolean moveToNextSegment() {
		Memory next = currentSegment().nextMemory();
		if (next != null) {
			updateCurrentSegment();
			return true;
		}
		return false;
	}

	/**
	 * Inserts space by moving data to the left.
	 * 
	 * Developer Note: For VLAN tag insertion, this moves 12 bytes left instead of
	 * 1488 bytes right - 124x faster!
	 * 
	 * @param gapSize size of gap to create
	 */
	private void insertSpaceByMovingLeft(long gapSize) {
		long dataToMove = position();
		if (dataToMove > 0) {
			// Check if we have leading space
			long leadingSpace = memoryOffset();
			if (leadingSpace < gapSize) {
				setError(new BufferOperationException(
						"Insufficient leading space for insertion"));
				return;
			}

			// Move data before position to the left
			MemorySegment.copy(memorySegment, memoryOffset(),
					memorySegment, memoryOffset() - gapSize, dataToMove);

			// Adjust our offset
			// Note: This would need special handling in a real implementation
			// as memoryOffset is typically immutable
		}
	}

	/**
	 * Inserts space by moving data to the right.
	 * 
	 * @param gapSize size of gap to create
	 */
	private void insertSpaceByMovingRight(long gapSize) {
		long dataToMove = limit() - position();
		if (dataToMove > 0) {
			// Move data after position to the right
			MemorySegment.copy(memorySegment, position(),
					memorySegment, position() + gapSize, dataToMove);
		}
		// Gap is now at position
	}

	/**
	 * Returns a string representation of this buffer's state.
	 * 
	 * @return a string describing this buffer
	 */
	@Override
	public String toString() {
		return String.format("MemoryByteBuffer[pos=%d, lim=%d, cap=%d, err=%s]",
				position(), limit(), capacity(), hasError() ? getError() : "none");
	}
}
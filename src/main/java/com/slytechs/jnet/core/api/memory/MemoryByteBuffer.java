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
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

/**
 * Data accessor layer providing byte-level operations with multi-segment
 * spanning and editing capabilities.
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
 * <li><strong>Buffer Editing:</strong> insertSpace, removeSpace, and split
 * operations</li>
 * <li><strong>Performance Metrics:</strong> Track data movement and bounds
 * adjustments</li>
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
 * <li>Smart direction choice for data movement</li>
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

	/** The Constant BYTE_HANDLE. */
	// Little-endian handles (platform native for most systems)
	private static final VarHandle BYTE_HANDLE = ValueLayout.JAVA_BYTE.varHandle();

	/** The Constant SHORT_HANDLE. */
	private static final VarHandle SHORT_HANDLE = ValueLayout.JAVA_SHORT_UNALIGNED.varHandle();

	/** The Constant INT_HANDLE. */
	private static final VarHandle INT_HANDLE = ValueLayout.JAVA_INT_UNALIGNED.varHandle();

	/** The Constant LONG_HANDLE. */
	private static final VarHandle LONG_HANDLE = ValueLayout.JAVA_LONG_UNALIGNED.varHandle();

	/** The Constant FLOAT_HANDLE. */
	private static final VarHandle FLOAT_HANDLE = ValueLayout.JAVA_FLOAT_UNALIGNED.varHandle();

	/** The Constant DOUBLE_HANDLE. */
	private static final VarHandle DOUBLE_HANDLE = ValueLayout.JAVA_DOUBLE_UNALIGNED.varHandle();

	/** The Constant CHAR_HANDLE. */
	private static final VarHandle CHAR_HANDLE = ValueLayout.JAVA_CHAR_UNALIGNED.varHandle();

	/** The Constant SHORT_BE_HANDLE. */
	// Big-endian handles for network byte order
	private static final VarHandle SHORT_BE_HANDLE = ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN)
			.varHandle();

	/** The Constant INT_BE_HANDLE. */
	private static final VarHandle INT_BE_HANDLE = ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN)
			.varHandle();

	/** The Constant LONG_BE_HANDLE. */
	private static final VarHandle LONG_BE_HANDLE = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN)
			.varHandle();

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
			return abstractMemory.segment; // Direct field access
		}
		return memory.asMemorySegment(); // Fallback
	}

	/**
	 * Instantiates a new memory byte buffer.
	 */
	public MemoryByteBuffer() {
		super(null);

	}

	/**
	 * Constructs a MemoryByteBuffer with specified bounds.
	 * 
	 * @param owningPool       the pool that owns this buffer
	 * @param memorySegment    the backing memory segment
	 * @param memoryOffset     starting offset within segment
	 * @param memoryEnd        ending offset within segment (exclusive)
	 * @param memoryDataOffset starting offset of data region
	 * @param memoryDataEnd    ending offset of data region (exclusive)
	 */
	public MemoryByteBuffer(MemoryPool<MemoryByteBuffer> owningPool,
			MemorySegment memorySegment,
			long memoryOffset, long memoryEnd,
			long memoryDataOffset, long memoryDataEnd) {
		super(owningPool, memorySegment, memoryOffset, memoryEnd, memoryDataOffset, memoryDataEnd);
	}

	/**
	 * Convenience constructor that uses the entire segment as both memory and data
	 * bounds.
	 *
	 * @param memorySegment the backing memory segment
	 */
	public MemoryByteBuffer(MemorySegment memorySegment) {
		super(null, memorySegment, 0, memorySegment.byteSize(), 0, memorySegment.byteSize());
	}

	/**
	 * Convenience constructor with custom memory bounds but data fills entire
	 * region.
	 * 
	 * @param memorySegment the backing memory segment
	 * @param memoryOffset  starting offset within segment
	 * @param memoryEnd     ending offset within segment (exclusive)
	 */
	public MemoryByteBuffer(MemorySegment memorySegment, long memoryOffset, long memoryEnd) {
		super(null, memorySegment, memoryOffset, memoryEnd, memoryOffset, memoryEnd);
	}

	/**
	 * Allocate gap and overflow.
	 *
	 * @param gapRemaining the gap remaining
	 * @param prevSegment  the prev segment
	 */
	private void allocateGapAndOverflow(long gapRemaining, MemoryByteBuffer prevSegment) {
		if (gapRemaining <= 0)
			return;

		MemoryPool<MemoryByteBuffer> pool = (MemoryPool<MemoryByteBuffer>) getOwningPool();
		MemoryByteBuffer newSegment = pool.allocate();

		if (newSegment == null) {
			metrics.recordOutOfMemory();
			setError(new BufferOperationException("Pool exhausted"));
			return;
		}

		metrics.recordSegmentAllocated();
		long activeLen = newSegment.activeBytesLength();
		if (gapRemaining > activeLen) {
			// Allocate all of the needed gap space
			allocateGapAndOverflow(gapRemaining - activeLen, newSegment);

			if (checkError()) {
				newSegment.setNextMemory(null);
				newSegment.decrementRef();

				return;
			}

		} else {

			// newSegment is the last of the gap segments, move data here
			long dataToMove = remaining();

			System.out.println("Segments to copy:");
			System.out.println(localSegment());
			System.out.println(newSegment);
			System.out.printf("localStart=%d, localPos=%d, newStart=%d, gapRem=%d, data=%d%n",
					activeBytesStart(), localPosition(),
					newSegment.activeBytesStart(), gapRemaining,
					dataToMove);
			// Add before the copy:
			byte testByte = segment.get(ValueLayout.JAVA_BYTE, position());
			System.out.printf("Byte at position %d: %d%n", position(), testByte);
			System.out.printf("Segment size: %d%n", segment.byteSize());

			metrics.recordBytesCopiedCrossSegment(dataToMove);
			MemorySegment.copy(
					segment, activeBytesStart() + position(), // Absolute segment offset
					newSegment.segment, newSegment.activeBytesStart() + gapRemaining,
					dataToMove);

			newSegment.activeBytesEnd(newSegment.activeBytesStart() + dataToMove + gapRemaining);
		}

		if (prevSegment != null) {
			// Link to new segment to end of allocated gap-chain
			prevSegment.setNextMemory(newSegment);

		} else {
			// Insert new segment into chain
			Memory oldNext = nextSegment();
			setNextMemory(newSegment);
			if (oldNext != null) {
				newSegment.setNextMemory(oldNext);
			}
		}
	}

	/**
	 * Allocate segment for overflow.
	 *
	 * @param totalSize the total size
	 */
	private void allocateSegmentForOverflow(long totalSize) {
		// Check pool availability before attempting allocation
		if (getOwningPool() == null) {
			setError(new BufferOperationException("No pool available for allocation"));
			return;
		}

		allocateGapAndOverflow(totalSize, null);

		long dataToMove = remaining();

		// Truncate current segment at insertion point since data was moved
		if (dataToMove > 0) {
			activeBytesEnd(activeBytesStart() + position());
		}

		limit(limit() + totalSize);
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
			offset += current.activeBytesLength();
			current = current.nextSegment();
		}

		return offset;
	}

	/**
	 * Can fit in current segment.
	 *
	 * @param size the size
	 * @return true, if successful
	 */
	private boolean canFitInCurrentSegment(long size) {
		return (headroom() >= size) || (tailroom() >= size);
	}

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
			long localPos = currentSegment().activeBytesStart() + localPosition();
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
			long localPos = currentSegment().activeBytesStart() + localPosition();
			MemorySegment.copy(segment, localPos, MemorySegment.ofArray(dst), offset, length);
			position(position() + length);
		} else {
			// Slow path: spans segments
			getSpanning(dst, offset, length);
		}

		return this;
	}

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

		Memory segment = seekSegment(index);
		if (segment != null) {
			MemorySegment memSeg = getDirectSegment(segment);
			long localIndex = index - calculateSegmentOffset(segment);
			return (byte) BYTE_HANDLE.get(memSeg, segment.activeBytesStart() + localIndex);
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
			long localPos = currentSegment().activeBytesStart() + localPosition();
			result = (char) CHAR_HANDLE.get(segment, localPos);
			position(position() + 2);
		} else {
			result = (char) getSpanningShort();
		}

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

	// ==================== Absolute Get Operations (don't change position)
	// ====================

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
			long localPos = currentSegment().activeBytesStart() + localPosition();
			result = (double) DOUBLE_HANDLE.get(segment, localPos);
			position(position() + 8);
		} else {
			result = Double.longBitsToDouble(getSpanningLong());
		}

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
			long localPos = currentSegment().activeBytesStart() + localPosition();
			result = (float) FLOAT_HANDLE.get(segment, localPos);
			position(position() + 4);
		} else {
			result = Float.intBitsToFloat(getSpanningInt());
		}

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
			long localPos = currentSegment().activeBytesStart() + localPosition();
			result = (int) INT_HANDLE.get(segment, localPos);
			position(position() + 4);
		} else {
			result = getSpanningInt();
		}

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
	 * Reads an int value in big-endian byte order at the current position.
	 * 
	 * <p>
	 * Reads 4 bytes in network byte order and advances position by 4. This is
	 * essential for reading network protocol headers and fields.
	 * </p>
	 * 
	 * @return the int value in big-endian byte order
	 */
	public int getIntBE() {
		if (checkError())
			return 0;

		if (remaining() < 4) {
			setError(new BufferOperationException("Insufficient data for getIntBE()"));
			return 0;
		}

		int result;
		if (!needsSpanning(4)) {
			MemorySegment segment = getDirectSegment(currentSegment());
			long localPos = currentSegment().activeBytesStart() + localPosition();
			result = (int) INT_BE_HANDLE.get(segment, localPos);
			position(position() + 4);
		} else {
			// Read bytes in big-endian order when spanning
			int b1 = get() & 0xFF;
			int b2 = get() & 0xFF;
			int b3 = get() & 0xFF;
			int b4 = get() & 0xFF;
			result = (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
		}

		return result;
	}

	/**
	 * Reads an int value in big-endian byte order at the specified index.
	 * 
	 * @param index the index from which to read
	 * @return the int value in big-endian byte order
	 */
	public int getIntBE(long index) {
		if (checkError())
			return 0;

		if (index < 0 || index + 4 > capacity()) {
			setError(new BufferOperationException("Index out of bounds: " + index));
			return 0;
		}

		long savedPos = position();
		position(index);
		int result = getIntBE();
		position(savedPos);
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
			long localPos = currentSegment().activeBytesStart() + localPosition();
			result = (long) LONG_HANDLE.get(segment, localPos);
			position(position() + 8);
		} else {
			result = getSpanningLong();
		}

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
	 * Reads a long value in big-endian byte order at the current position.
	 * 
	 * <p>
	 * Reads 8 bytes in network byte order and advances position by 8. This is
	 * essential for reading timestamps and large network protocol fields.
	 * </p>
	 * 
	 * @return the long value in big-endian byte order
	 */
	public long getLongBE() {
		if (checkError())
			return 0;

		if (remaining() < 8) {
			setError(new BufferOperationException("Insufficient data for getLongBE()"));
			return 0;
		}

		long result;
		if (!needsSpanning(8)) {
			MemorySegment segment = getDirectSegment(currentSegment());
			long localPos = currentSegment().activeBytesStart() + localPosition();
			result = (long) LONG_BE_HANDLE.get(segment, localPos);
			position(position() + 8);
		} else {
			// Read bytes in big-endian order when spanning
			long high = getIntBE() & 0xFFFFFFFFL;
			long low = getIntBE() & 0xFFFFFFFFL;
			result = (high << 32) | low;
		}

		return result;
	}

	/**
	 * Reads a long value in big-endian byte order at the specified index.
	 * 
	 * @param index the index from which to read
	 * @return the long value in big-endian byte order
	 */
	public long getLongBE(long index) {
		if (checkError())
			return 0;

		if (index < 0 || index + 8 > capacity()) {
			setError(new BufferOperationException("Index out of bounds: " + index));
			return 0;
		}

		long savedPos = position();
		position(index);
		long result = getLongBE();
		position(savedPos);
		return result;
	}

	// ==================== Relative Put Operations (advance position)
	// ====================

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
			long localPos = currentSegment().activeBytesStart() + localPosition();
			result = (short) SHORT_HANDLE.get(segment, localPos);
			position(position() + 2);
		} else {
			result = getSpanningShort();
		}

		return result;
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
	 * Reads a short value in big-endian byte order at the current position.
	 * 
	 * <p>
	 * Reads 2 bytes in network byte order and advances position by 2. This is
	 * essential for reading network protocol headers.
	 * </p>
	 * 
	 * @return the short value in big-endian byte order
	 */
	public short getShortBE() {
		if (checkError())
			return 0;

		if (remaining() < 2) {
			setError(new BufferOperationException("Insufficient data for getShortBE()"));
			return 0;
		}

		short result;
		if (!needsSpanning(2)) {
			MemorySegment segment = getDirectSegment(currentSegment());
			long localPos = currentSegment().activeBytesStart() + localPosition();
			result = (short) SHORT_BE_HANDLE.get(segment, localPos);
			position(position() + 2);
		} else {
			// Read bytes in big-endian order when spanning
			int b1 = get() & 0xFF;
			int b2 = get() & 0xFF;
			result = (short) ((b1 << 8) | b2);
		}

		return result;
	}

	/**
	 * Reads a short value in big-endian byte order at the specified index.
	 * 
	 * @param index the index from which to read
	 * @return the short value in big-endian byte order
	 */
	public short getShortBE(long index) {
		if (checkError())
			return 0;

		if (index < 0 || index + 2 > capacity()) {
			setError(new BufferOperationException("Index out of bounds: " + index));
			return 0;
		}

		long savedPos = position();
		position(index);
		short result = getShortBE();
		position(savedPos);
		return result;
	}

	/**
	 * Reads data that spans segments.
	 *
	 * @param dst    the dst
	 * @param offset the offset
	 * @param length the length
	 * @return the spanning
	 */
	private void getSpanning(byte[] dst, int offset, int length) {
		int read = 0;

		while (read < length && hasRemaining()) {
			Memory seg = currentSegment();
			long localPos = localPosition();
			long segRemaining = seg.activeBytesEnd() - (seg.activeBytesStart() + localPos);

			int toRead = (int) Math.min(length - read, segRemaining);
			if (toRead > 0) {
				MemorySegment memSeg = getDirectSegment(seg);
				MemorySegment.copy(memSeg, seg.activeBytesStart() + localPos,
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
	 * Reads an int that spans segments.
	 *
	 * @return the spanning int
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
	 *
	 * @return the spanning long
	 */
	private long getSpanningLong() {
		long high = getSpanningInt() & 0xFFFFFFFFL;
		long low = getSpanningInt() & 0xFFFFFFFFL;
		return (high << 32) | low;
	}

	/**
	 * Reads a short that spans segments.
	 *
	 * @return the spanning short
	 */
	private short getSpanningShort() {
		int b1 = get() & 0xFF;
		int b2 = get() & 0xFF;
		return (short) ((b1 << 8) | b2);
	}

	/**
	 * Inserts space at the current position by moving existing data.
	 * 
	 * <p>
	 * Creates a gap of the specified size at the current position. This operation
	 * intelligently chooses to move either data before or after the position based
	 * on which requires fewer bytes to be copied. Supports cross-segment operations
	 * with automatic overflow handling.
	 * </p>
	 * 
	 * @param size the size of the gap to create in bytes
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer insertSpace(long size) {
		if (checkError())
			return this;

		if (size <= 0) {
			return this; // No-op for zero or negative size
		}

		metrics.recordInsertSpace();

		// Check single segment strategies first
		if (canFitInCurrentSegment(size)) {
			insertSpaceInCurrentSegment(size);
			return this;
		}

		// Cross-segment strategy
		long availableHere = headroom() + tailroom();
		long overflow = size - availableHere;

		// Try to push overflow to next segment's headroom
		if (hasNextSegment() && nextSegment().headroom() >= overflow) {
			pushOverflowToNext(overflow);

			// Expand within segment bounds
			long newEnd = Math.min(activeBytesEnd() + size, segmentSize());
			activeBytesEnd(newEnd);
			limit(limit() + size);
			metrics.recordBoundsExpanded();
			return this;
		}

		// Need to allocate new segment for overflow
		allocateSegmentForOverflow(size);
		return this;
	}

	/**
	 * Insert space by moving left.
	 *
	 * @param gapSize the gap size
	 */
	private void insertSpaceByMovingLeft(long gapSize) {
		long dataSize = position();
		if (dataSize > 0) {
			metrics.recordBytesMovedLeft(dataSize);
			MemorySegment.copy(
					segment, activeBytesStart(),
					segment, activeBytesStart() - gapSize,
					dataSize);
		}
		activeBytesStart(activeBytesStart() - gapSize);
		metrics.recordBoundsExpanded();
	}

	/**
	 * Insert space by moving right.
	 *
	 * @param gapSize the gap size
	 */
	private void insertSpaceByMovingRight(long gapSize) {
		long dataStart = activeBytesStart() + position();
		long dataSize = remaining();
		if (dataSize > 0) {
			metrics.recordBytesMovedRight(dataSize);
			MemorySegment.copy(
					segment, dataStart,
					segment, dataStart + gapSize,
					dataSize);
		}
		activeBytesEnd(activeBytesEnd() + gapSize);
		metrics.recordBoundsExpanded();
	}

	/**
	 * Insert space in current segment.
	 *
	 * @param size the size
	 */
	private void insertSpaceInCurrentSegment(long size) {
		long dataBeforePos = position();
		long dataAfterPos = remaining();

		// Special case: at beginning with headroom available
		if (dataBeforePos == 0 && headroom() >= size) {
			// Just expand into headroom, no data movement needed
			activeBytesStart(activeBytesStart() - size);
			metrics.recordBoundsExpanded();
			// Don't change position - stays at 0
		} else if (dataBeforePos > 0 && dataBeforePos <= dataAfterPos && headroom() >= size) {
			// Move data before position to the left
			insertSpaceByMovingLeft(size);
			position(position() + size); // Maintain position after the gap
		} else if (dataAfterPos > 0 && tailroom() >= size) {
			// Move data after position to the right
			insertSpaceByMovingRight(size);
		} else if (dataAfterPos == 0 && tailroom() >= size) {
			// At end, just expand into tailroom
			activeBytesEnd(activeBytesEnd() + size);
			metrics.recordBoundsExpanded();
		} else {
			setError(new BufferOperationException("Insufficient room for insertion"));
		}

		// Update limit to reflect new size
		limit(limit() + size);
	}

	// ==================== Absolute Put Operations (don't change position)
	// ====================

	/**
	 * Moves to the next segment in the chain.
	 * 
	 * @return true if moved to next segment, false if at end
	 */
	private boolean moveToNextSegment() {
		Memory next = currentSegment().nextSegment();
		if (next != null) {
			updateCurrentSegment();
			return true;
		}
		return false;
	}

	/**
	 * Optimize empty segment bounds.
	 */
	private void optimizeEmptySegmentBounds() {
		optimizeEmptySegmentBounds(this);
	}

	/**
	 * Optimize empty segment bounds.
	 *
	 * @param segment the segment
	 */
	private void optimizeEmptySegmentBounds(Memory segment) {
		// When segment becomes empty, reset to optimal headroom/tailroom distribution
		long defaultHeadroom = getOwningPool() != null ? ((MemoryPool<?>) getOwningPool()).getDefaultHeadroom() : 128;
		segment.activeBytesStart(segment.segmentOffset() + defaultHeadroom);
		segment.activeBytesEnd(segment.segmentOffset() + defaultHeadroom);
		metrics.recordBoundsExpanded(); // Counts as optimization
	}

	/**
	 * Push overflow to next.
	 *
	 * @param spaceNeeded the space needed
	 */
	private void pushOverflowToNext(long spaceNeeded) {
		// We're trying to insert 'spaceNeeded' bytes but don't have room
		// Move data after position to next segment to make room

		Memory next = nextSegment();
		long dataAfterPosition = remaining();

		if (next == null || next.headroom() < dataAfterPosition) {
			setError(new BufferOperationException("Cannot push data to next segment"));
			return;
		}

		if (dataAfterPosition > 0) {
			metrics.recordBytesCopiedCrossSegment(dataAfterPosition);
			MemorySegment nextSeg = getDirectSegment(next); // Use getDirectSegment for consistency

			// Move all data after position to next segment's headroom
			// Use localPosition() instead of position()!
			MemorySegment.copy(
					segment, activeBytesStart() + localPosition(), // FIX: use localPosition()
					nextSeg, next.activeBytesStart() - dataAfterPosition,
					dataAfterPosition);

			// Adjust bounds
			next.activeBytesStart(next.activeBytesStart() - dataAfterPosition);
			activeBytesEnd(activeBytesStart() + localPosition()); // FIX: use localPosition() here too

			metrics.recordBoundsExpanded();
		}
	}

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
			long localPos = currentSegment().activeBytesStart() + localPosition();
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
			long localPos = currentSegment().activeBytesStart() + localPosition();
			MemorySegment.copy(MemorySegment.ofArray(src), offset, segment, localPos, length);
			position(position() + length);
		} else {
			// Slow path: spans segments
			putSpanning(src, offset, length);
		}

		return this;
	}

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

		Memory segment = seekSegment(index);
		if (segment != null) {
			MemorySegment memSeg = getDirectSegment(segment);
			long localIndex = index - calculateSegmentOffset(segment);
			BYTE_HANDLE.set(memSeg, segment.activeBytesStart() + localIndex, value);
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

	// ==================== Buffer Editing Operations ====================

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
			long localPos = currentSegment().activeBytesStart() + localPosition();
			CHAR_HANDLE.set(segment, localPos, value);
			position(position() + 2);
		} else {
			// Slow path: spans segments
			putSpanningShort((short) value);
		}

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

	// ==================== Private Helper Methods for Editing ====================

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
			long localPos = currentSegment().activeBytesStart() + localPosition();
			DOUBLE_HANDLE.set(segment, localPos, value);
			position(position() + 8);
		} else {
			// Slow path: spans segments
			putSpanningLong(Double.doubleToLongBits(value));
		}

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
			long localPos = currentSegment().activeBytesStart() + localPosition();
			FLOAT_HANDLE.set(segment, localPos, value);
			position(position() + 4);
		} else {
			// Slow path: spans segments
			putSpanningInt(Float.floatToIntBits(value));
		}

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
			long localPos = currentSegment().activeBytesStart() + localPosition();
			INT_HANDLE.set(segment, localPos, value);
			position(position() + 4);
		} else {
			// Slow path: spans segments
			putSpanningInt(value);
		}

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
			long localPos = currentSegment().activeBytesStart() + localPosition();
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
			long localPos = currentSegment().activeBytesStart() + localPosition();
			LONG_HANDLE.set(segment, localPos, value);
			position(position() + 8);
		} else {
			// Slow path: spans segments
			putSpanningLong(value);
		}

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
			long localPos = currentSegment().activeBytesStart() + localPosition();
			LONG_BE_HANDLE.set(segment, localPos, value);
			position(position() + 8);
		} else {
			// Convert to big-endian bytes for spanning
			putIntBE((int) (value >>> 32));
			putIntBE((int) value);
		}

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
			long localPos = currentSegment().activeBytesStart() + localPosition();
			SHORT_HANDLE.set(segment, localPos, value);
			position(position() + 2);
		} else {
			// Slow path: spans segments
			putSpanningShort(value);
		}

		return this;
	}

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
			long localPos = currentSegment().activeBytesStart() + localPosition();
			SHORT_BE_HANDLE.set(segment, localPos, value);
			position(position() + 2);
		} else {
			// Convert to big-endian bytes for spanning
			put((byte) (value >>> 8));
			put((byte) value);
		}

		return this;
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

		while (written < length) {
			// Check if we have space in current segment
			long segmentSpace = currentSegment().activeBytesEnd() -
					(currentSegment().activeBytesStart() + localPosition());

			if (segmentSpace > 0) {
				// Write what we can to current segment
				int toWrite = (int) Math.min(length - written, segmentSpace);
				MemorySegment memSeg = getDirectSegment(currentSegment());
				MemorySegment.copy(MemorySegment.ofArray(src), offset + written,
						memSeg, currentSegment().activeBytesStart() + localPosition(),
						toWrite);
				position(position() + toWrite);
				written += toWrite;
			} else {
				// Current segment full, move to next
				if (hasNextSegment()) {
					// Move to next segment and continue
					currentSegment = nextSegment();
					updateCurrentSegment();
				} else {
					// No more segments
					setError(new BufferOperationException(
							"Insufficient space: need " + (length - written) + " more bytes"));
					break;
				}
			}
		}
	}

	/**
	 * Writes an int that spans segments.
	 *
	 * @param value the value
	 */
	private void putSpanningInt(int value) {
		put((byte) (value >>> 24));
		put((byte) (value >>> 16));
		put((byte) (value >>> 8));
		put((byte) value);
	}

	/**
	 * Writes a long that spans segments.
	 *
	 * @param value the value
	 */
	private void putSpanningLong(long value) {
		putSpanningInt((int) (value >>> 32));
		putSpanningInt((int) value);
	}

	/**
	 * Writes a short that spans segments.
	 *
	 * @param value the value
	 */
	private void putSpanningShort(short value) {
		put((byte) (value >>> 8));
		put((byte) value);
	}

	/**
	 * Record allocation failure.
	 */
	private void recordAllocationFailure() {
		// Record in pool metrics if available
		if (getOwningPool() != null) {
			@SuppressWarnings("unchecked")
			MemoryPool<MemoryByteBuffer> pool = (MemoryPool<MemoryByteBuffer>) getOwningPool();
			pool.getPoolMetrics().recordAllocationFailure();
		}
	}

	/**
	 * Removes the cross segment.
	 *
	 * @param size the size
	 */
	private void removeCrossSegment(long size) {
		long remainingInCurrent = activeBytesLength() - position();

		// Remove from current segment
		if (remainingInCurrent > 0) {
			long toRemove = Math.min(size, remainingInCurrent);
			activeBytesEnd(activeBytesEnd() - toRemove);
			metrics.recordBoundsShrunk();
			size -= toRemove;
		}

		// Remove from subsequent segments
		Memory current = this;
		while (size > 0 && current.hasNextSegment()) {
			current = current.nextSegment();
			long segmentSize = current.activeBytesLength();

			if (size >= segmentSize) {
				// Remove entire segment content
				optimizeEmptySegmentBounds(current);
				size -= segmentSize;
			} else {
				// Partial removal from segment
				current.activeBytesStart(current.activeBytesStart() + size);
				metrics.recordBoundsShrunk();
				size = 0;
			}
		}
	}

	/**
	 * Removes the from beginning.
	 *
	 * @param size the size
	 */
	private void removeFromBeginning(long size) {
		// Special optimization: just adjust bounds, no data movement
		long toRemove = Math.min(size, activeBytesLength());
		activeBytesStart(activeBytesStart() + toRemove);
		metrics.recordBoundsShrunk();

		// Handle cross-segment case
		if (size > toRemove && hasNextSegment()) {
			Memory next = nextSegment();
			long remainingToRemove = size - toRemove;
			if (remainingToRemove <= next.activeBytesLength()) {
				next.activeBytesStart(next.activeBytesStart() + remainingToRemove);
				metrics.recordBoundsShrunk();
			}
		}

		// Check if segment is now empty and optimize bounds
		if (activeBytesLength() == 0) {
			optimizeEmptySegmentBounds();
		}
	}

	/**
	 * Removes the in current segment.
	 *
	 * @param size the size
	 */
	private void removeInCurrentSegment(long size) {
		long dataBeforePos = position();
		long dataAfterRemoval = activeBytesLength() - position() - size;

		// Choose optimal direction
		if (dataBeforePos < dataAfterRemoval && dataBeforePos > 0) {
			// Move left side right into the gap (expand headroom)
			metrics.recordBytesMovedRight(dataBeforePos);
			MemorySegment.copy(
					segment, activeBytesStart(),
					segment, activeBytesStart() + size,
					dataBeforePos);
			activeBytesStart(activeBytesStart() + size);
			metrics.recordBoundsExpanded();
		} else if (dataAfterRemoval > 0) {
			// Move right side left to fill the gap
			metrics.recordBytesMovedLeft(dataAfterRemoval);
			MemorySegment.copy(
					segment, activeBytesStart() + position() + size,
					segment, activeBytesStart() + position(),
					dataAfterRemoval);
			activeBytesEnd(activeBytesEnd() - size);
			metrics.recordBoundsShrunk();
		} else {
			// Just shrink bounds
			activeBytesEnd(activeBytesEnd() - size);
			metrics.recordBoundsShrunk();
		}

		// Adjust limit
		limit(Math.max(position(), limit() - size));
	}

	/**
	 * Removes space starting at the current position.
	 * 
	 * <p>
	 * Removes bytes starting at current position by moving data after the gap
	 * backward or adjusting bounds. Special optimization for position 0 which only
	 * adjusts bounds without data movement.
	 * </p>
	 * 
	 * @param size the number of bytes to remove
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer removeSpace(long size) {
		if (checkError())
			return this;

		if (size <= 0) {
			return this; // No-op for zero or negative size
		}

		metrics.recordRemoveSpace();

		// Special case: removing from beginning - just adjust bounds
		if (position() == 0) {
			removeFromBeginning(size);
			return this;
		}

		// Check if removal spans segments
		if (position() + size <= activeBytesLength()) {
			// Single segment removal
			removeInCurrentSegment(size);
		} else {
			// Cross-segment removal
			removeCrossSegment(size);
		}

		return this;
	}

	/**
	 * Splits this buffer at the current position by inserting a new segment.
	 * 
	 * <p>
	 * This operation splits the buffer into two segments at the current position.
	 * Data after the position is moved to a newly allocated segment from the pool,
	 * which is inserted into the chain after this segment.
	 * </p>
	 * 
	 * @return this buffer for method chaining
	 */
	public MemoryByteBuffer split() {
		if (checkError())
			return this;

		metrics.recordSplit();

		// Check if we're at a segment boundary
		if (position() == activeBytesLength()) {
			return this; // Already at boundary, no split needed
		}

		// Check if we have a pool to allocate from
		if (getOwningPool() == null) {
			setError(new BufferOperationException("Cannot split: no memory pool available"));
			return this;
		}

		// Calculate how much data needs to be moved
		long dataToMove = remaining();

		if (dataToMove == 0) {
			return this; // Nothing to split
		}

		// Allocate new segment from pool
		@SuppressWarnings("unchecked")
		MemoryPool<MemoryByteBuffer> pool = (MemoryPool<MemoryByteBuffer>) getOwningPool();
		MemoryByteBuffer newSegment = pool.allocate();

		if (newSegment == null) {
			metrics.recordOutOfMemory();
			setError(new BufferOperationException("Cannot split: pool exhausted"));
			return this;
		}

		metrics.recordSegmentAllocated();

		// Set up new segment with optimal headroom
		long defaultHeadroom = pool.getDefaultHeadroom();
		newSegment.activeBytesStart(defaultHeadroom);

		// Copy data after position to new segment
		long srcOffset = activeBytesStart() + position();
		metrics.recordBytesCopiedCrossSegment(dataToMove);
		MemorySegment.copy(
				segment, srcOffset,
				newSegment.segment, newSegment.activeBytesStart(),
				dataToMove);

		// Adjust bounds
		newSegment.activeBytesEnd(newSegment.activeBytesStart() + dataToMove);
		newSegment.position(0);
		newSegment.limit(dataToMove);

		// Shrink this segment's active bytes
		activeBytesEnd(activeBytesStart() + position());
		limit(position());

		// Insert new segment into chain
		Memory oldNext = nextSegment();
		setNextMemory(newSegment);
		if (oldNext != null) {
			newSegment.setNextMemory(oldNext);
		}

		return this;
	}

}
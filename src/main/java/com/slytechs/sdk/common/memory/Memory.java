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

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;

/**
 * Core memory interface providing lifecycle management, segment access, and
 * chaining support.
 * 
 * <p>
 * Memory represents owned memory segments with full lifecycle control through
 * reference counting. This interface extends {@link MemoryWindow} for boundary
 * management and adds ownership semantics, native segment access, and chain
 * support for fragmented memory.
 * </p>
 * 
 * <h2>Memory Ownership Model</h2>
 * <p>
 * Memory objects own their underlying segments and manage lifecycle through
 * reference counting. Each Memory starts with a reference count of 1 and is
 * released when the count reaches 0. This enables safe sharing across threads
 * and prevents premature deallocation.
 * </p>
 * 
 * <h2>Memory Types</h2>
 * <ul>
 * <li><strong>FixedMemory:</strong> Pre-allocated segments from memory
 * pools</li>
 * <li><strong>ScopedMemory:</strong> Wraps native memory (DPDK mbufs, etc.)
 * with scope-based lifecycle</li>
 * </ul>
 * 
 * <h2>Chaining Support</h2>
 * <p>
 * Memory segments can be linked to form chains, useful for:
 * </p>
 * <ul>
 * <li>Scatter-gather I/O operations</li>
 * <li>Fragmented packet reassembly</li>
 * <li>Zero-copy buffer concatenation</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * // Allocate from freeListPool
 * Memory memory = freeListPool.allocate();
 * 
 * // Write data
 * MemorySegment segment = memory.segment();
 * segment.set(ValueLayout.JAVA_INT, memory.start(), value);
 * 
 * // Share with another component (increases refcount)
 * memory.incrementRef();
 * processor.process(memory);
 * 
 * // Release when done
 * memory.decrementRef();
 * }</pre>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 */
public interface Memory extends MemoryWindow, MemoryRefCounter {

	/**
	 * Checks if a Memory or MemorySegment is null.
	 * 
	 * @param obj the object to check
	 * @return true if null or null pointer
	 */
	static boolean isNull(Object obj) {
		if (obj == null) {
			return true;
		}
		if (obj instanceof MemorySegment segment) {
			return segment.address() == 0;
		}
		if (obj instanceof Memory memory) {
			return memory.isNull();
		}
		return false;
	}

	/**
	 * Creates a Memory instance from a MemorySegment.
	 * 
	 * @param segment the memory segment
	 * @param offset  the offset within segment
	 * @return a new Memory instance
	 */
	static Memory of(MemorySegment segment, long offset) {
		return of(segment, offset, segment.byteSize() - offset);
	}

	/**
	 * Creates a Memory instance from a MemorySegment with bounds.
	 * 
	 * @param segment the memory segment
	 * @param offset  the offset within segment
	 * @param length  the length of the memory region
	 * @return a new Memory instance
	 */
	static Memory of(MemorySegment segment, long offset, long length) {
		return new FixedMemory(segment, offset, length);
	}

	static Memory of(long byteSize) {
		var seg = Arena.ofAuto().allocate(byteSize);

		return of(seg, 0);
	}

	static Memory of(long size, MemoryUnit unit) {
		return of(unit.toBytes(size));
	}

	/**
	 * Returns this memory as a ByteBuffer.
	 * 
	 * <p>
	 * Creates a ByteBuffer view of the active data region. Changes to the buffer
	 * will be reflected in the underlying memory segment.
	 * </p>
	 * 
	 * @return ByteBuffer view of active data
	 */
	default ByteBuffer asByteBuffer() {
		return segment().asSlice(start(), length()).asByteBuffer();
	}

	/**
	 * Checks if this is a FixedMemory with fixed native memory segment defined.
	 *
	 * @return true, if is fixed, otherwise false for scoped
	 */
	default boolean isFixed() {
		return false;
	}
	
	/**
	 * Unbind the bound memory segment if this memory is a ScopedMemory instance
	 * type. For FixedMemory this is a no-op.
	 */
	default void unbindIfScoped() {

	}
	
	default ScopedMemory asScopedMemory() {
		return (ScopedMemory) this;
	}
	
	default FixedMemory asFixedMemory() {
		return (FixedMemory) this;
	}

	/**
	 * Returns the underlying memory segment.
	 * 
	 * @return the memory segment
	 * @deprecated Use {@link #segment()} instead
	 */
	@Deprecated
	default MemorySegment asMemorySegment() {
		return segment();
	}

	/**
	 * Decrements the reference count.
	 * 
	 * <p>
	 * Call this method when done using the memory. When the count reaches 0, the
	 * memory is automatically released back to its freeListPool or freed.
	 * </p>
	 * 
	 * @return the new reference count
	 * @throws IllegalStateException if decrement would cause underflow
	 */
	@Override
	int decrementRef();

	/**
	 * Checks if this memory has a next segment.
	 * 
	 * @return true if chained to another segment
	 */
	default boolean hasNextSegment() {
		return nextSegment() != null;
	}

	/**
	 * Increments the reference count.
	 * 
	 * <p>
	 * Call this method when sharing memory with another component to prevent
	 * premature release. Each increment must be paired with a corresponding
	 * {@link #decrementRef()}.
	 * </p>
	 * 
	 * @return the new reference count
	 * @throws IllegalStateException if memory is already released (refCount == 0)
	 */
	@Override
	int incrementRef();

	/**
	 * Checks if this memory is null.
	 * 
	 * @return true if the underlying segment is null or has address 0
	 */
	default boolean isNull() {
		return segment() != null && segment().address() == 0;
	}

	/**
	 * Checks if this memory is a pointer (no size).
	 * 
	 * @return true if byteSize() == 0
	 */
	default boolean isPointer() {
		return byteSize() == 0;
	}

	// ==================== Lifecycle Management ====================

	/**
	 * Returns the next segment in the chain.
	 * 
	 * <p>
	 * Memory segments can be linked to form chains for scatter-gather operations or
	 * fragmented data. Returns null if this is the last segment.
	 * </p>
	 * 
	 * @return the next segment or null
	 */
	Memory nextSegment();

	/**
	 * Returns the current reference count.
	 * 
	 * <p>
	 * The reference count tracks the number of active references to this memory.
	 * When the count reaches 0, the memory is automatically released.
	 * </p>
	 * 
	 * @return the reference count (always ≥ 0)
	 */
	@Override
	int refCount();

	/**
	 * Seeks to a specific offset within the chain.
	 * 
	 * <p>
	 * Navigates through the segment chain to find the segment containing the
	 * specified offset. The offset is relative to the start of this segment's data
	 * region.
	 * </p>
	 * 
	 * @param offset the offset to seek to
	 * @return the Memory containing the offset, or null if beyond chain
	 */
	Memory seekSegment(long offset);

	// ==================== Chain Support ====================

	/**
	 * Returns the underlying memory segment.
	 * 
	 * <p>
	 * Provides direct access to the native memory segment for low-level operations.
	 * The segment remains valid as long as the Memory object has a non-zero
	 * reference count.
	 * </p>
	 * 
	 * @return the memory segment
	 */
	MemorySegment segment();

	/**
	 * Sets the next segment in the chain.
	 * 
	 * <p>
	 * Links this memory to another segment, forming or extending a chain. The
	 * implementation should handle reference counting appropriately.
	 * </p>
	 * 
	 * @param next the next segment or null to terminate the chain
	 */
	void nextSegment(Memory next);

	/**
	 * Returns this memory's view for binding operations.
	 * 
	 * <p>
	 * The view provides positioning information (segment, start, length) that can
	 * be used by BindableView implementations for efficient binding without data
	 * copying.
	 * </p>
	 * 
	 * @return the memory view
	 */
	MemoryView view();
}
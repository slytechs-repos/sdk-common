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
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

/**
 * Fixed memory implementation with immutable segment binding.
 * 
 * <p>
 * FixedMemory represents pre-allocated memory segments that are bound once at
 * construction and never change. This is the primary memory type for pooled
 * allocations where segments are allocated upfront and reused many times.
 * </p>
 * 
 * <h2>Characteristics</h2>
 * <ul>
 * <li><strong>Immutable segment:</strong> The memory segment is final and set
 * at construction</li>
 * <li><strong>FreeListPool-managed:</strong> Typically allocated from and returned to
 * memory pools</li>
 * <li><strong>Chainable:</strong> Supports linking multiple segments for
 * scatter-gather</li>
 * <li><strong>Reusable:</strong> Can be recycled and reused without
 * reallocation</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * 
 * <h3>FreeListPool Allocation</h3>
 * 
 * <pre>{@code
 * // Allocate from freeListPool
 * FixedMemory memory = freeListPool.allocate();
 * 
 * // Use the memory
 * memory.segment().set(ValueLayout.JAVA_INT, memory.start(), value);
 * 
 * // Memory automatically returns to freeListPool at refCount=0
 * memory.decrementRef();
 * }</pre>
 * 
 * <h3>Direct Creation</h3>
 * 
 * <pre>{@code
 * // Create with specific segment
 * MemorySegment segment = Arena.global().allocate(1024);
 * FixedMemory memory = new FixedMemory(segment);
 * }</pre>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 */
public class FixedMemory extends AbstractMemory {

	/** The immutable memory segment */
	protected final MemorySegment segment;

	/**
	 * Constructs a FixedMemory with the given segment.
	 * 
	 * <p>
	 * The entire segment becomes the active data region. Use {@link #start(long)}
	 * and {@link #end(long)} to adjust the active boundaries after construction.
	 * </p>
	 * 
	 * @param segment the memory segment
	 */
	public FixedMemory(MemorySegment segment) {
		this(segment, 0, segment.byteSize());
	}

	/**
	 * Constructs a FixedMemory with specified bounds.
	 * 
	 * <p>
	 * Creates a fixed memory with specific offset and length within the provided
	 * segment. The active data region initially encompasses the specified bounds.
	 * </p>
	 * 
	 * @param segment the memory segment
	 * @param offset  starting offset within segment
	 * @param length  length of the memory region
	 * @throws IllegalArgumentException if offset + length exceeds segment size
	 */
	public FixedMemory(MemorySegment segment, long offset, long length) {
		if (offset < 0 || length < 0 || offset + length > segment.byteSize()) {
			throw new IllegalArgumentException(
					String.format("Invalid bounds: offset=%d, length=%d, segment.byteSize=%d",
							offset, length, segment.byteSize()));
		}
		this.segment = segment;
		setSegmentBounds(offset, length);
	}

	public FixedMemory(MemoryLayout layout, Arena arena) {
		this(arena.allocate(layout.byteSize()));
	}

	public FixedMemory(long size) {
		this(Arena.ofAuto().allocate(size));
	}

	public FixedMemory(long size, MemoryUnit unit) {
		this(Arena.ofAuto().allocate(unit.toBytes(size)));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Returns the immutable segment associated with this fixed memory. The segment
	 * remains valid for the lifetime of this object.
	 * </p>
	 */
	@Override
	public MemorySegment segment() {
		return segment;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Called when reference count reaches zero. If this memory is freeListPool-managed, it
	 * automatically returns to the freeListPool. Otherwise, it simply clears chain
	 * references.
	 * </p>
	 */
	@Override
	protected void onRefCountZero() {
		// Clear any chain references first
		Memory nextMem = this.next;
		if (nextMem != null) {
			this.next = null; // Clear BEFORE decrementing to avoid races
			nextMem.decrementRef();
		}

		// Now return to freeListPool (if we have one)
		super.recycle();
	}

	/**
	 * Creates a string representation of this memory.
	 * 
	 * @return a string describing this fixed memory
	 */
	@Override
	public String toString() {
		return String.format("FixedMemory[offset=%d, size=%d, start=%d, end=%d, refCount=%d]",
				byteOffset(), byteSize(), start(), end(), refCount());
	}

	/**
	 * @see com.slytechs.sdk.common.memory.AbstractMemory#onRecycle()
	 */
	@Override
	protected void onRecycle() {}

}
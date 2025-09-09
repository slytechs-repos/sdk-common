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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract base implementation providing reference counting and boundary
 * management.
 * 
 * <p>
 * AbstractMemory provides the common implementation for memory ownership,
 * including reference counting, chain management, and boundary tracking.
 * Subclasses need only provide the segment() method and handle their specific
 * allocation strategy.
 * </p>
 * 
 * <h2>Reference Counting</h2>
 * <p>
 * Uses atomic operations for thread-safe reference counting. Memory starts with
 * refCount=1 and is released when it reaches 0. The {@link #onRefCountZero()}
 * method allows subclasses to implement cleanup logic.
 * </p>
 * 
 * <h2>Boundary Management</h2>
 * <p>
 * Maintains both immutable segment boundaries (set at construction) and mutable
 * data boundaries (adjustable at runtime). The data region can expand into
 * available headroom/tailroom without reallocation.
 * </p>
 * 
 * <h2>Chain Support</h2>
 * <p>
 * Supports linking memory segments into chains. Chain operations properly
 * manage reference counts to prevent premature release of linked segments.
 * </p>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 */
public abstract class AbstractMemory implements Memory {

	/** Reference counter for lifecycle management. */
	protected final AtomicInteger refCount = new AtomicInteger(1);

	/** Next segment in chain (optional). */
	protected Memory next;

	/** Memory view for efficient binding. */
	protected final MemoryView memView = new MemoryView();

	/** Data boundaries - mutable for expansion/contraction. */
	protected volatile long dataStart;

	/** The data end. */
	protected volatile long dataEnd;

	/** Segment boundaries - immutable after construction. */
	protected long segmentOffset;

	/** The segment size. */
	protected long segmentSize;

	/**
	 * Constructs an AbstractMemory with reference count of 1.
	 */
	protected AbstractMemory() {
		// RefCount starts at 1
	}

	/**
	 * Sets the segment boundaries during construction.
	 * 
	 * <p>
	 * This method should be called by subclasses during construction to establish
	 * the immutable segment boundaries. By default, the data region encompasses the
	 * entire segment.
	 * </p>
	 * 
	 * @param offset the segment starting offset
	 * @param size   the segment size in bytes
	 */
	protected void setSegmentBounds(long offset, long size) {
		this.segmentOffset = offset;
		this.segmentSize = size;
		this.dataStart = offset;
		this.dataEnd = offset + size;

		// Update the view
		updateView();
	}

	/**
	 * Updates the internal memory view after boundary changes.
	 */
	protected void updateView() {
		memView.segment = segment();
		memView.start = dataStart;
		memView.length = dataEnd - dataStart;
		memView.source = this;
	}

	/**
	 * Resets this memory for reuse.
	 * 
	 * <p>
	 * Resets data boundaries to full segment and clears chain references.
	 * Subclasses should override to add specific reset logic.
	 * </p>
	 */
	public void recycle() {
		// Reset data boundaries to full segment
		dataStart = segmentOffset;
		dataEnd = segmentOffset + segmentSize;
		next = null;
		updateView();
	}

	// ==================== MemoryWindow Implementation ====================

	/**
	 * @see com.slytechs.jnet.core.api.memory.MemoryWindow#byteOffset()
	 */
	@Override
	public long byteOffset() {
		return segmentOffset;
	}

	/**
	 * @see com.slytechs.jnet.core.api.memory.MemoryWindow#byteSize()
	 */
	@Override
	public long byteSize() {
		return segmentSize;
	}

	/**
	 * @see com.slytechs.jnet.core.api.memory.MemoryWindow#start()
	 */
	@Override
	public long start() {
		return dataStart;
	}

	/**
	 * @see com.slytechs.jnet.core.api.memory.MemoryWindow#start(long)
	 */
	@Override
	public long start(long newStart) {
		if (newStart < segmentOffset || newStart > dataEnd) {
			throw new IllegalArgumentException(
					String.format("Invalid start: %d (must be between %d and %d)",
							newStart, segmentOffset, dataEnd));
		}
		this.dataStart = newStart;
		updateView();
		return newStart;
	}

	/**
	 * @see com.slytechs.jnet.core.api.memory.MemoryWindow#end()
	 */
	@Override
	public long end() {
		return dataEnd;
	}

	/**
	 * @see com.slytechs.jnet.core.api.memory.MemoryWindow#end(long)
	 */
	@Override
	public long end(long newEnd) {
		if (newEnd < dataStart || newEnd > segmentOffset + segmentSize) {
			throw new IllegalArgumentException(
					String.format("Invalid end: %d (must be between %d and %d)",
							newEnd, dataStart, segmentOffset + segmentSize));
		}
		this.dataEnd = newEnd;
		updateView();
		return newEnd;
	}

	// ==================== Memory Implementation ====================

	/**
	 * @see com.slytechs.jnet.core.api.memory.Memory#view()
	 */
	@Override
	public MemoryView view() {
		return memView;
	}

	/**
	 * @see com.slytechs.jnet.core.api.memory.Memory#refCount()
	 */
	@Override
	public int refCount() {
		return refCount.get();
	}

	/**
	 * @see com.slytechs.jnet.core.api.memory.Memory#incrementRef()
	 */
	@Override
	public int incrementRef() {
		return ChainUtils.incrementRef(refCount);
	}

	/**
	 * @see com.slytechs.jnet.core.api.memory.Memory#decrementRef()
	 */
	@Override
	public int decrementRef() {
		int newCount = ChainUtils.decrementRef(refCount);
		if (newCount == 0) {
			onRefCountZero();
		}
		return newCount;
	}

	/**
	 * Called when reference count reaches zero.
	 * 
	 * <p>
	 * Subclasses override this method to implement cleanup logic such as returning
	 * to pool or releasing native resources. The default implementation clears
	 * chain references to prevent leaks.
	 * </p>
	 */
	protected void onRefCountZero() {
		// Clear chain reference
		if (next != null) {
			next.decrementRef();
			next = null;
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Walks the chain to find the segment containing the specified offset. Returns
	 * this segment if the offset falls within its bounds, otherwise continues to
	 * the next segment.
	 * </p>
	 */
	@Override
	public Memory seekSegment(long offset) {
		if (offset < 0) {
			return null;
		}

		Memory current = this;
		long accumulatedOffset = 0;

		while (current != null) {
			long currentLength = current.length();

			// Check if offset falls within this segment
			if (offset < accumulatedOffset + currentLength) {
				return current;
			}

			accumulatedOffset += currentLength;
			current = current.nextSegment();
		}

		return null; // Offset beyond chain
	}

	/**
	 * @see com.slytechs.jnet.core.api.memory.Memory#nextSegment()
	 */
	@Override
	public Memory nextSegment() {
		return next;
	}

	/**
	 * @see com.slytechs.jnet.core.api.memory.Memory#nextSegment(com.slytechs.jnet.core.api.memory.Memory)
	 */
	@Override
	public void nextSegment(Memory next) {
		if (this.next != null) {
			this.next.decrementRef();
		}
		if (next != null) {
			next.incrementRef();
		}
		this.next = next;
	}
}
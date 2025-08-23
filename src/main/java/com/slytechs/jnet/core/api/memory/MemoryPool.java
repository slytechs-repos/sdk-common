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

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The Class MemoryPool.
 *
 * @param <T> the generic type
 */
public class MemoryPool<T extends AbstractMemory & MemoryPoolable> {

	/** The free list head. */
	private final AtomicReference<T> freeListHead = new AtomicReference<>();
	
	/** The segment size. */
	private final long segmentSize;
	
	/** The segment count. */
	private final long segmentCount;
	
	/** The arena. */
	private final Arena arena;
	
	/** The leading space. */
	private final long leadingSpace;
	
	/** The trailing space. */
	private final long trailingSpace;

	/** The pool metrics. */
	// Single metrics instance shared by all pooled objects
	private final PoolMetrics poolMetrics;
	
	/** The buffer metrics. */
	private final BufferMetrics bufferMetrics = new BufferMetrics();
	
	/** The default headroom. */
	private long defaultHeadroom;
	
	/** The name. */
	private final String name;

	/**
	 * Constructs a MemoryPool with default spacing (no leading/trailing space).
	 *
	 * @param name           TODO
	 * @param segmentSize    the segment size
	 * @param segmentCount   the segment count
	 * @param arena          the arena
	 * @param elementFactory the element factory
	 */
	public MemoryPool(String name, long segmentSize, long segmentCount, Arena arena,
			MemoryPoolableFactory<T> elementFactory) {
		this(name, segmentSize, segmentCount, 0, 0, arena, elementFactory);
	}

	/**
	 * Constructs a MemoryPool with specified leading/trailing space.
	 *
	 * @param name           TODO
	 * @param segmentSize    the segment size
	 * @param segmentCount   the segment count
	 * @param leadingSpace   the leading space
	 * @param trailingSpace  the trailing space
	 * @param arena          the arena
	 * @param elementFactory the element factory
	 */
	public MemoryPool(String name, long segmentSize,
			long segmentCount, long leadingSpace,
			long trailingSpace,
			Arena arena, MemoryPoolableFactory<T> elementFactory) {

		// Validate parameters
		if (segmentSize <= 0 || segmentCount <= 0) {
			throw new IllegalArgumentException("segmentSize and segmentCount must be positive");
		}
		if (leadingSpace < 0 || trailingSpace < 0 || leadingSpace + trailingSpace >= segmentSize) {
			throw new IllegalArgumentException("Invalid leading/trailing space");
		}

		// Initialize fields
		this.name = name;
		this.segmentSize = segmentSize;
		this.segmentCount = segmentCount;
		this.leadingSpace = leadingSpace;
		this.trailingSpace = trailingSpace;
		this.arena = arena;

		// Single metrics instance
		this.poolMetrics = new PoolMetrics(segmentSize, segmentCount);

		// Initialize the pool
		initializePool(elementFactory);
	}

	/**
	 * Name.
	 *
	 * @return the string
	 */
	public String name() {
		return name;
	}

	/**
	 * Initializes the pool by pre-allocating all memory segments.
	 * 
	 * <p>
	 * This method:
	 * <ol>
	 * <li>Allocates a single large memory block</li>
	 * <li>Slices it into individual segments</li>
	 * <li>Creates memory objects via the factory</li>
	 * <li>Prepares them for pool storage (refcount = 0)</li>
	 * <li>Links them into the free list</li>
	 * </ol>
	 * 
	 * @param elementFactory factory for creating memory objects
	 */
	private void initializePool(MemoryPoolableFactory<T> elementFactory) {
		// Allocate the entire memory block
		MemorySegment totalMemory = arena.allocate(segmentCount * segmentSize);
		var allocator = SegmentAllocator.slicingAllocator(totalMemory);

		// Pre-allocate all segments
		for (int i = 0; i < segmentCount; i++) {
			T memory = createPoolElement(allocator, elementFactory);
			addToFreeList(memory);
		}
	}

	/**
	 * Creates a single pool element ready for storage.
	 * 
	 * <p>
	 * This method:
	 * <ol>
	 * <li>Allocates a segment slice</li>
	 * <li>Calculates memory and data bounds</li>
	 * <li>Creates the memory object via factory</li>
	 * <li>Prepares it for pool storage (refcount = 0)</li>
	 * </ol>
	 * 
	 * @param allocator      the slicing allocator for the memory block
	 * @param elementFactory factory for creating memory objects
	 * @return a memory object ready for pool storage
	 */
	private T createPoolElement(SegmentAllocator allocator, MemoryPoolableFactory<T> elementFactory) {
		// Allocate segment slice
		MemorySegment segment = allocator.allocate(segmentSize);

		// Calculate bounds
		long memoryOffset = 0;
		long memoryEnd = segmentSize;
		long memoryDataOffset = leadingSpace;
		long memoryDataEnd = segmentSize - trailingSpace;

		// Create memory object via factory
		T memory = elementFactory.newInstance(
				this, segment,
				memoryOffset, memoryEnd,
				memoryDataOffset, memoryDataEnd);

		// Prepare for pool storage
		prepareForStorage(memory);

		return memory;
	}

	/**
	 * Prepares a memory object for storage in the pool.
	 * 
	 * <p>
	 * Pool storage requires:
	 * <ul>
	 * <li>Reference count = 0 (inactive state)</li>
	 * <li>No chain linkage (will be set during list insertion)</li>
	 * <li>Reset internal state</li>
	 * </ul>
	 * 
	 * @param memory the memory object to prepare
	 */
	private void prepareForStorage(T memory) {
		// Set to storage state (refcount = 0)
		// Factory creates with refcount = 1, but pool storage needs 0
		memory.refCount.set(0);

		// Clear any chain linkage (will be set when adding to free list)
		memory.setNextSegmentRaw(null);

		// Note: Don't call resetForReuse() here as the object is freshly created
		// resetForReuse() is for when returning used objects to the pool
	}

	/**
	 * Adds a memory object to the free list.
	 * 
	 * <p>
	 * Uses lock-free CAS operation to maintain thread safety. The memory object
	 * must be in storage state (refcount = 0).
	 * 
	 * @param memory the memory object to add
	 */
	private void addToFreeList(T memory) {
		// Lock-free addition to head of list
		while (true) {
			T currentHead = freeListHead.get();
			memory.setNextSegmentRaw(currentHead);
			if (freeListHead.compareAndSet(currentHead, memory)) {
				return;
			}
			// Retry if CAS failed
		}
	}

	/**
	 * Allocates a memory object from the pool.
	 * 
	 * <p>
	 * This method performs lock-free allocation by atomically removing the head of
	 * the free list and preparing it for use. The returned memory object has its
	 * state reset and reference count set to 1.
	 * </p>
	 * 
	 * @return a memory object with reference count = 1, ready for use
	 */
	@SuppressWarnings("unchecked")
	public T allocate() {
		while (true) {
			T candidate = freeListHead.get();
			if (candidate == null) {
				// Pool exhausted
				if (poolMetrics != null) {
					poolMetrics.recordAllocationFailure();
				}
				return null;
			}

			// Cast to AbstractMemory to access nextMemoryRaw()
			AbstractMemory abstractCandidate = candidate;
			T nextInList = (T) abstractCandidate.nextSegmentRaw();

			if (freeListHead.compareAndSet(candidate, nextInList)) {
				// Successfully removed from list
				abstractCandidate.setNextSegmentRaw(null);
				abstractCandidate.prepareForUse(); // Set refcount to 1
				return candidate;
			}
			// CAS failed, retry
		}
	}

	/**
	 * Returns metrics for monitoring (not in hot path).
	 *
	 * @return the pool metrics
	 */
	public PoolMetrics getPoolMetrics() {
		return poolMetrics;
	}

	/**
	 * Releases a chain of memory segments in reverse order.
	 * 
	 * Uses a two-pass approach: first counts segments, then releases backwards
	 * using indexed access to avoid allocation.
	 * 
	 * @param mem the head of the chain to release
	 */
	private void releaseChainInReverseOrder2Pass(AbstractMemory mem) {
		if (mem.nextSegment() == null) {
			return; // Single segment, nothing to unlink
		}

		// Count segments
		int count = 0;
		AbstractMemory current = mem;
		while (current != null) {
			count++;
			current = (AbstractMemory) current.nextSegment();
		}

		// Walk to segment at (count-2) and release from there backwards
		while (count > 1) {
			current = mem;
			// Walk to the segment at position (count-2)
			for (int i = 0; i < count - 2; i++) {
				current = (AbstractMemory) current.nextSegmentRaw();
			}
			// Unlink the last segment
			current.setNextSegmentRaw(null);
			count--;
		}
	}

	/**
	 * Release chain in reverse order.
	 *
	 * @param mem   the mem
	 * @param depth the depth
	 */
	void releaseChainInReverseOrder(AbstractMemory mem, int depth) {
		if (mem.nextSegment() != null) {
			if (depth > 100)
				releaseChainInReverseOrder2Pass((AbstractMemory) mem.nextSegmentRaw());
			else
				releaseChainInReverseOrder((AbstractMemory) mem.nextSegmentRaw(), depth + 1);

			mem.setNextMemory(null);
		}
	}

	/**
	 * Releases a memory object back to the pool.
	 * 
	 * @param mem the memory object to release
	 */
	public void release(T mem) {
		if (mem.nextSegmentRaw() != null)
			releaseChainInReverseOrder(mem, 0);

		// Memory should already be reset by the caller (onRefCountZero)
		// Just add back to free list
		addToFreeList(mem);
	}

	/**
	 * Returns the current number of available segments.
	 * 
	 * @return the number of segments in the free list
	 */
	@SuppressWarnings("unchecked")
	public long getFreeListSize() {
		long count = 0;
		T current = freeListHead.get();
		while (current != null) {
			count++;
			// Use raw access to avoid validation on stored objects
			current = (T) current.nextSegmentRaw();
		}
		return count;
	}

	/**
	 * Gets the segment count.
	 *
	 * @return the segment count
	 */
	// Getters remain the same
	public long getSegmentCount() {
		return segmentCount;
	}

	/**
	 * Gets the segment size.
	 *
	 * @return the segment size
	 */
	public long getSegmentSize() {
		return segmentSize;
	}

	/**
	 * Gets the leading space.
	 *
	 * @return the leading space
	 */
	public long getLeadingSpace() {
		return leadingSpace;
	}

	/**
	 * Gets the trailing space.
	 *
	 * @return the trailing space
	 */
	public long getTrailingSpace() {
		return trailingSpace;
	}

	/**
	 * Gets the buffer metrics.
	 *
	 * @return the buffer metrics
	 */
	public BufferMetrics getBufferMetrics() {
		return bufferMetrics;
	}

	/**
	 * Gets the default headroom.
	 *
	 * @return the default headroom
	 */
	public long getDefaultHeadroom() {
		return defaultHeadroom;
	}
}
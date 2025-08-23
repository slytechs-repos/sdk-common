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

import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance metrics for buffer operations.
 * 
 * Tracks essential performance-sensitive operations to help users optimize
 * their buffer usage patterns.
 */
public final class BufferMetrics {

	/** The Constant GLOBAL. */
	private static final BufferMetrics GLOBAL = new BufferMetrics();

	/**
	 * Global statistics for all buffers not bound to a MemoryPool.
	 *
	 * @return the buffer metrics
	 */
	public static BufferMetrics global() {
		return GLOBAL;
	}

	/** The bytes moved left. */
	// Data movement metrics
	private final AtomicLong bytesMovedLeft = new AtomicLong();
	
	/** The bytes moved right. */
	private final AtomicLong bytesMovedRight = new AtomicLong();
	
	/** The bytes copied cross segment. */
	private final AtomicLong bytesCopiedCrossSegment = new AtomicLong();

	/** The segments allocated. */
	// Memory allocation metrics
	private final AtomicLong segmentsAllocated = new AtomicLong();
	
	/** The segments freed. */
	private final AtomicLong segmentsFreed = new AtomicLong();
	
	/** The out of memory count. */
	private final AtomicLong outOfMemoryCount = new AtomicLong();

	/** The bounds expanded. */
	// Bounds adjustment metrics (efficient operations)
	private final AtomicLong boundsExpanded = new AtomicLong();

	/**
	 * Gets the insert space ops.
	 *
	 * @return the insertSpaceOps
	 */
	public long getInsertSpaceOps() {
		return insertSpaceOps.get();
	}

	/** The bounds shrunk. */
	private final AtomicLong boundsShrunk = new AtomicLong();

	/** The insert space ops. */
	// Operation counters
	private final AtomicLong insertSpaceOps = new AtomicLong();
	
	/** The remove space ops. */
	private final AtomicLong removeSpaceOps = new AtomicLong();
	
	/** The split ops. */
	private final AtomicLong splitOps = new AtomicLong();

	/**
	 * Record bytes moved left.
	 *
	 * @param bytes the bytes
	 */
	// Recording methods
	public void recordBytesMovedLeft(long bytes) {
		bytesMovedLeft.addAndGet(bytes);
	}

	/**
	 * Record bytes moved right.
	 *
	 * @param bytes the bytes
	 */
	public void recordBytesMovedRight(long bytes) {
		bytesMovedRight.addAndGet(bytes);
	}

	/**
	 * Record bytes copied cross segment.
	 *
	 * @param bytes the bytes
	 */
	public void recordBytesCopiedCrossSegment(long bytes) {
		bytesCopiedCrossSegment.addAndGet(bytes);
	}

	/**
	 * Record segment allocated.
	 */
	public void recordSegmentAllocated() {
		segmentsAllocated.incrementAndGet();
	}

	/**
	 * Record segment freed.
	 */
	public void recordSegmentFreed() {
		segmentsFreed.incrementAndGet();
	}

	/**
	 * Record out of memory.
	 */
	public void recordOutOfMemory() {
		outOfMemoryCount.incrementAndGet();
	}

	/**
	 * Record bounds expanded.
	 */
	public void recordBoundsExpanded() {
		boundsExpanded.incrementAndGet();
	}

	/**
	 * Record bounds shrunk.
	 */
	public void recordBoundsShrunk() {
		boundsShrunk.incrementAndGet();
	}

	/**
	 * Record insert space.
	 */
	public void recordInsertSpace() {
		insertSpaceOps.incrementAndGet();
	}

	/**
	 * Record remove space.
	 */
	public void recordRemoveSpace() {
		removeSpaceOps.incrementAndGet();
	}

	/**
	 * Record split.
	 */
	public void recordSplit() {
		splitOps.incrementAndGet();
	}

	/**
	 * Gets the bytes moved total.
	 *
	 * @return the bytes moved total
	 */
	// Getters
	public long getBytesMovedTotal() {
		return bytesMovedLeft.get() + bytesMovedRight.get();
	}

	/**
	 * Gets the bytes moved left.
	 *
	 * @return the bytes moved left
	 */
	public long getBytesMovedLeft() {
		return bytesMovedLeft.get();
	}

	/**
	 * Gets the bytes moved right.
	 *
	 * @return the bytes moved right
	 */
	public long getBytesMovedRight() {
		return bytesMovedRight.get();
	}

	/**
	 * Gets the bytes copied cross segment.
	 *
	 * @return the bytes copied cross segment
	 */
	public long getBytesCopiedCrossSegment() {
		return bytesCopiedCrossSegment.get();
	}

	/**
	 * Gets the segments allocated.
	 *
	 * @return the segments allocated
	 */
	public long getSegmentsAllocated() {
		return segmentsAllocated.get();
	}

	/**
	 * Gets the out of memory count.
	 *
	 * @return the out of memory count
	 */
	public long getOutOfMemoryCount() {
		return outOfMemoryCount.get();
	}

	/**
	 * Gets the bounds adjustments.
	 *
	 * @return the bounds adjustments
	 */
	public long getBoundsAdjustments() {
		return boundsExpanded.get() + boundsShrunk.get();
	}

	/**
	 * Gets the efficiency ratio.
	 *
	 * @return the efficiency ratio
	 */
	public double getEfficiencyRatio() {
		long moves = getBytesMovedTotal();
		long adjustments = getBoundsAdjustments();
		if (moves == 0)
			return adjustments > 0 ? 1.0 : 0.0;
		return (double) adjustments / (adjustments + moves);
	}

	/**
	 * Reset.
	 */
	public void reset() {
		bytesMovedLeft.set(0);
		bytesMovedRight.set(0);
		bytesCopiedCrossSegment.set(0);
		segmentsAllocated.set(0);
		segmentsFreed.set(0);
		outOfMemoryCount.set(0);
		boundsExpanded.set(0);
		boundsShrunk.set(0);
		insertSpaceOps.set(0);
		removeSpaceOps.set(0);
		splitOps.set(0);
	}

	/**
	 * To string.
	 *
	 * @return the string
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format(
				"BufferMetrics[moves=%d, adjustments=%d, efficiency=%.2f%%, " +
						"allocations=%d, OOM=%d, ops(insert=%d, remove=%d, split=%d)]",
				getBytesMovedTotal(), getBoundsAdjustments(),
				getEfficiencyRatio() * 100,
				segmentsAllocated.get(), outOfMemoryCount.get(),
				insertSpaceOps.get(), removeSpaceOps.get(), splitOps.get());
	}
}
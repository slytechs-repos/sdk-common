package com.slytechs.jnet.core.api.memory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance metrics for buffer operations.
 * 
 * Tracks essential performance-sensitive operations to help users
 * optimize their buffer usage patterns.
 */
public class BufferMetrics {
    
    // Data movement metrics
    private final AtomicLong bytesMovedLeft = new AtomicLong();
    private final AtomicLong bytesMovedRight = new AtomicLong();
    private final AtomicLong bytesCopiedCrossSegment = new AtomicLong();
    
    // Memory allocation metrics
    private final AtomicLong segmentsAllocated = new AtomicLong();
    private final AtomicLong segmentsFreed = new AtomicLong();
    private final AtomicLong outOfMemoryCount = new AtomicLong();
    
    // Bounds adjustment metrics (efficient operations)
    private final AtomicLong boundsExpanded = new AtomicLong();
    /**
	 * @return the insertSpaceOps
	 */
	public long getInsertSpaceOps() {
		return insertSpaceOps.get();
	}

	private final AtomicLong boundsShrunk = new AtomicLong();
    
    // Operation counters
    private final AtomicLong insertSpaceOps = new AtomicLong();
    private final AtomicLong removeSpaceOps = new AtomicLong();
    private final AtomicLong splitOps = new AtomicLong();
    
    // Recording methods
    public void recordBytesMovedLeft(long bytes) {
        bytesMovedLeft.addAndGet(bytes);
    }
    
    public void recordBytesMovedRight(long bytes) {
        bytesMovedRight.addAndGet(bytes);
    }
    
    public void recordBytesCopiedCrossSegment(long bytes) {
        bytesCopiedCrossSegment.addAndGet(bytes);
    }
    
    public void recordSegmentAllocated() {
        segmentsAllocated.incrementAndGet();
    }
    
    public void recordSegmentFreed() {
        segmentsFreed.incrementAndGet();
    }
    
    public void recordOutOfMemory() {
        outOfMemoryCount.incrementAndGet();
    }
    
    public void recordBoundsExpanded() {
        boundsExpanded.incrementAndGet();
    }
    
    public void recordBoundsShrunk() {
        boundsShrunk.incrementAndGet();
    }
    
    public void recordInsertSpace() {
        insertSpaceOps.incrementAndGet();
    }
    
    public void recordRemoveSpace() {
        removeSpaceOps.incrementAndGet();
    }
    
    public void recordSplit() {
        splitOps.incrementAndGet();
    }
    
    // Getters
    public long getBytesMovedTotal() {
        return bytesMovedLeft.get() + bytesMovedRight.get();
    }
    
    public long getBytesMovedLeft() {
        return bytesMovedLeft.get();
    }
    
    public long getBytesMovedRight() {
        return bytesMovedRight.get();
    }
    
    public long getBytesCopiedCrossSegment() {
        return bytesCopiedCrossSegment.get();
    }
    
    public long getSegmentsAllocated() {
        return segmentsAllocated.get();
    }
    
    public long getOutOfMemoryCount() {
        return outOfMemoryCount.get();
    }
    
    public long getBoundsAdjustments() {
        return boundsExpanded.get() + boundsShrunk.get();
    }
    
    public double getEfficiencyRatio() {
        long moves = getBytesMovedTotal();
        long adjustments = getBoundsAdjustments();
        if (moves == 0) return adjustments > 0 ? 1.0 : 0.0;
        return (double) adjustments / (adjustments + moves);
    }
    
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
    
    @Override
    public String toString() {
        return String.format(
            "BufferMetrics[moves=%d, adjustments=%d, efficiency=%.2f%%, " +
            "allocations=%d, OOM=%d, ops(insert=%d, remove=%d, split=%d)]",
            getBytesMovedTotal(), getBoundsAdjustments(), 
            getEfficiencyRatio() * 100,
            segmentsAllocated.get(), outOfMemoryCount.get(),
            insertSpaceOps.get(), removeSpaceOps.get(), splitOps.get()
        );
    }
}
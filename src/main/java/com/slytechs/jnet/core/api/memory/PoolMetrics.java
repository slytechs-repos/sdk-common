/*
 * Sly Technologies Free License
 * 
 * Copyright 2025 Sly Technologies Inc.
 */
package com.slytechs.jnet.core.api.memory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Minimal metrics for memory pool operations.
 * 
 * <p>
 * Only tracks essential counters for production monitoring
 * without impacting 100M+ pps performance.
 * </p>
 */
public class PoolMetrics {
    // Only essential counters - minimal overhead
    private final AtomicLong allocationFailures = new AtomicLong();
    private final AtomicLong releaseErrors = new AtomicLong();
    
    // Pool configuration (immutable, for reporting)
    private final long segmentSize;
    private final long segmentCount;
    
    public PoolMetrics(long segmentSize, long segmentCount) {
        this.segmentSize = segmentSize;
        this.segmentCount = segmentCount;
    }
    
    // Fast increment methods (called in hot path)
    void recordAllocationFailure() { 
        allocationFailures.incrementAndGet(); 
    }
    
    void recordReleaseError() { 
        releaseErrors.incrementAndGet(); 
    }
    
    // Query methods (called infrequently for monitoring)
    public long getAllocationFailures() { return allocationFailures.get(); }
    public long getReleaseErrors() { return releaseErrors.get(); }
    public long getSegmentSize() { return segmentSize; }
    public long getSegmentCount() { return segmentCount; }
    
    public void reset() {
        allocationFailures.set(0);
        releaseErrors.set(0);
    }
}
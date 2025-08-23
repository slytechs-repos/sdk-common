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
 * Minimal metrics for memory pool operations.
 * 
 * <p>
 * Only tracks essential counters for production monitoring
 * without impacting 100M+ pps performance.
 * </p>
 */
public class PoolMetrics {
    
    /** The allocation failures. */
    // Only essential counters - minimal overhead
    private final AtomicLong allocationFailures = new AtomicLong();
    
    /** The release errors. */
    private final AtomicLong releaseErrors = new AtomicLong();
    
    /** The segment size. */
    // Pool configuration (immutable, for reporting)
    private final long segmentSize;
    
    /** The segment count. */
    private final long segmentCount;
    
    /**
	 * Instantiates a new pool metrics.
	 *
	 * @param segmentSize  the segment size
	 * @param segmentCount the segment count
	 */
    public PoolMetrics(long segmentSize, long segmentCount) {
        this.segmentSize = segmentSize;
        this.segmentCount = segmentCount;
    }
    
    /**
	 * Record allocation failure.
	 */
    // Fast increment methods (called in hot path)
    void recordAllocationFailure() { 
        allocationFailures.incrementAndGet(); 
    }
    
    /**
	 * Record release error.
	 */
    void recordReleaseError() { 
        releaseErrors.incrementAndGet(); 
    }
    
    /**
	 * Gets the allocation failures.
	 *
	 * @return the allocation failures
	 */
    // Query methods (called infrequently for monitoring)
    public long getAllocationFailures() { return allocationFailures.get(); }
    
    /**
	 * Gets the release errors.
	 *
	 * @return the release errors
	 */
    public long getReleaseErrors() { return releaseErrors.get(); }
    
    /**
	 * Gets the segment size.
	 *
	 * @return the segment size
	 */
    public long getSegmentSize() { return segmentSize; }
    
    /**
	 * Gets the segment count.
	 *
	 * @return the segment count
	 */
    public long getSegmentCount() { return segmentCount; }
    
    /**
	 * Reset.
	 */
    public void reset() {
        allocationFailures.set(0);
        releaseErrors.set(0);
    }
}
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

import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics for buffer operations, with atomic counters.
 */
class BufferMetrics {
    private final AtomicLong boundsViolations = new AtomicLong();
    private final AtomicLong insufficientData = new AtomicLong();
    private final AtomicLong insufficientSpace = new AtomicLong();
    private final AtomicLong ioFailures = new AtomicLong();

    void recordBoundsViolation() { boundsViolations.incrementAndGet(); }
    void recordInsufficientData() { insufficientData.incrementAndGet(); }
    void recordInsufficientSpace() { insufficientSpace.incrementAndGet(); }
    void recordIoFailure() { ioFailures.incrementAndGet(); }

    // Getters
    long getBoundsViolations() { return boundsViolations.get(); }
    long getInsufficientData() { return insufficientData.get(); }
    long getInsufficientSpace() { return insufficientSpace.get(); }
    long getIoFailures() { return ioFailures.get(); }

    @Override
    public String toString() {
        return String.format("BufferMetrics[boundsViolations=%d, insufficientData=%d, insufficientSpace=%d, ioFailures=%d]",
                getBoundsViolations(), getInsufficientData(), getInsufficientSpace(), getIoFailures());
    }
}
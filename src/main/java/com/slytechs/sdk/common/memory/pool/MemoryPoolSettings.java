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
package com.slytechs.sdk.common.memory.pool;

/**
 * Configuration settings for {@link MemoryPool}.
 * 
 * <p>
 * Extends {@link PoolSettings} with memory-specific options including
 * headroom and tailroom configuration.
 * </p>
 * 
 * <h2>Usage</h2>
 * 
 * <pre>{@code
 * MemoryPoolSettings settings = new MemoryPoolSettings()
 *     .minCapacity(64)
 *     .maxCapacity(1024)
 *     .segmentSize(9000)
 *     .headroom(64)
 *     .tailroom(4);
 * }</pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see MemoryPool
 * @see PoolSettings
 */
public class MemoryPoolSettings extends PoolSettings {

    private long headroom = 0;
    private long tailroom = 0;

    public MemoryPoolSettings() {
        segmentSize(9000); // Default jumbo frame size
    }

    /**
     * Returns the headroom.
     *
     * @return headroom in bytes
     */
    public long headroom() {
        return headroom;
    }

    /**
     * Sets the headroom.
     * 
     * <p>
     * Reserved space before the active data region. Useful for
     * prepending headers without copying.
     * </p>
     *
     * @param headroom headroom in bytes
     * @return this for chaining
     */
    public MemoryPoolSettings headroom(long headroom) {
        this.headroom = headroom;
        return this;
    }

    /**
     * Returns the tailroom.
     *
     * @return tailroom in bytes
     */
    public long tailroom() {
        return tailroom;
    }

    /**
     * Sets the tailroom.
     * 
     * <p>
     * Reserved space after the active data region. Useful for
     * appending trailers like CRC without copying.
     * </p>
     *
     * @param tailroom tailroom in bytes
     * @return this for chaining
     */
    public MemoryPoolSettings tailroom(long tailroom) {
        this.tailroom = tailroom;
        return this;
    }

    /**
     * Returns the usable data size.
     *
     * @return segmentSize - headroom - tailroom
     */
    public long usableSize() {
        return segmentSize() - headroom - tailroom;
    }

    /**
     * Validates the settings.
     *
     * @throws IllegalStateException if settings are invalid
     */
    public void validate() {
        if (segmentSize() <= 0) {
            throw new IllegalStateException("Segment size must be positive");
        }
        if (headroom < 0) {
            throw new IllegalStateException("Headroom cannot be negative");
        }
        if (tailroom < 0) {
            throw new IllegalStateException("Tailroom cannot be negative");
        }
        if (headroom + tailroom >= segmentSize()) {
            throw new IllegalStateException(
                    "Headroom + tailroom must be less than segment size");
        }
        if (minCapacity() < 0) {
            throw new IllegalStateException("Min capacity cannot be negative");
        }
        if (maxCapacity() < minCapacity()) {
            throw new IllegalStateException("Max capacity must be >= min capacity");
        }
    }

    // Override setters to return MemoryPoolSettings for chaining

    @Override
    public MemoryPoolSettings minCapacity(int minCapacity) {
        super.minCapacity(minCapacity);
        return this;
    }

    @Override
    public MemoryPoolSettings maxCapacity(int maxCapacity) {
        super.maxCapacity(maxCapacity);
        return this;
    }

    @Override
    public MemoryPoolSettings capacity(int capacity) {
        super.capacity(capacity);
        return this;
    }

    @Override
    public MemoryPoolSettings segmentSize(long segmentSize) {
        super.segmentSize(segmentSize);
        return this;
    }

    @Override
    public MemoryPoolSettings contractionEnabled(boolean enabled) {
        super.contractionEnabled(enabled);
        return this;
    }

    @Override
    public MemoryPoolSettings checkpointInterval(int interval) {
        super.checkpointInterval(interval);
        return this;
    }

    @Override
    public MemoryPoolSettings contractionCheckpoints(int checkpoints) {
        super.contractionCheckpoints(checkpoints);
        return this;
    }

    @Override
    public MemoryPoolSettings contractionThreshold(float threshold) {
        super.contractionThreshold(threshold);
        return this;
    }

    @Override
    public String toString() {
        return String.format(
                "MemoryPoolSettings[min=%d, max=%d, segment=%d, headroom=%d, tailroom=%d]",
                minCapacity(), maxCapacity(), segmentSize(), headroom, tailroom);
    }
}
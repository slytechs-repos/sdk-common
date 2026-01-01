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
 * Configuration settings for a {@link Pool}.
 * 
 * <p>
 * PoolSettings provides fluent configuration for pool capacity, memory sizing,
 * and automatic contraction behavior.
 * </p>
 * 
 * <h2>Capacity Settings</h2>
 * 
 * <ul>
 * <li>{@link #minCapacity(int)} - Floor capacity, preallocated at startup</li>
 * <li>{@link #maxCapacity(int)} - Ceiling capacity, pool grows up to this</li>
 * </ul>
 * 
 * <h2>Memory Settings</h2>
 * 
 * <ul>
 * <li>{@link #segmentSize(long)} - Size of each memory segment</li>
 * </ul>
 * 
 * <h2>Contraction Settings</h2>
 * 
 * <ul>
 * <li>{@link #contractionEnabled(boolean)} - Enable/disable auto-contraction</li>
 * <li>{@link #checkpointInterval(int)} - Cycles between contraction checks</li>
 * <li>{@link #contractionCheckpoints(int)} - Consecutive idle checks to trigger</li>
 * <li>{@link #contractionThreshold(float)} - Idle ratio threshold</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * 
 * <pre>{@code
 * PoolSettings settings = new PoolSettings()
 *     .minCapacity(100)
 *     .maxCapacity(10_000)
 *     .segmentSize(9000)
 *     .contractionEnabled(true)
 *     .checkpointInterval(1024);
 * 
 * Pool<Packet> pool = new FreeListPool<>(settings, Packet::new);
 * }</pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see Pool
 * @see ContractionStrategy
 */
public class PoolSettings {

    private int minCapacity = 64;
    private int maxCapacity = 1024;
    private long segmentSize = 0;

    private boolean contractionEnabled = false;
    private int checkpointInterval = 1024;
    private int contractionCheckpoints = 10;
    private float contractionThreshold = 0.75f;

    public PoolSettings() {
    }

    /**
     * Returns the minimum capacity.
     *
     * @return minimum capacity
     */
    public int minCapacity() {
        return minCapacity;
    }

    /**
     * Sets the minimum capacity.
     * 
     * <p>
     * The pool will not contract below this size. This capacity is
     * preallocated at pool creation.
     * </p>
     *
     * @param minCapacity minimum capacity
     * @return this for chaining
     */
    public PoolSettings minCapacity(int minCapacity) {
        this.minCapacity = minCapacity;
        return this;
    }

    /**
     * Returns the maximum capacity.
     *
     * @return maximum capacity
     */
    public int maxCapacity() {
        return maxCapacity;
    }

    /**
     * Sets the maximum capacity.
     * 
     * <p>
     * The pool will not grow beyond this size.
     * </p>
     *
     * @param maxCapacity maximum capacity
     * @return this for chaining
     */
    public PoolSettings maxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
        return this;
    }

    /**
     * Sets both min and max capacity to the same value (fixed size pool).
     *
     * @param capacity fixed capacity
     * @return this for chaining
     */
    public PoolSettings capacity(int capacity) {
        this.minCapacity = capacity;
        this.maxCapacity = capacity;
        return this;
    }

    /**
     * Returns the memory segment size.
     *
     * @return segment size in bytes, or 0 for non-memory pools
     */
    public long segmentSize() {
        return segmentSize;
    }

    /**
     * Sets the memory segment size.
     * 
     * <p>
     * For memory-backed pools, this is the size of each backing segment.
     * Set to 0 for non-memory pools.
     * </p>
     *
     * @param segmentSize segment size in bytes
     * @return this for chaining
     */
    public PoolSettings segmentSize(long segmentSize) {
        this.segmentSize = segmentSize;
        return this;
    }

    /**
     * Returns whether contraction is enabled.
     *
     * @return true if auto-contraction enabled
     */
    public boolean contractionEnabled() {
        return contractionEnabled;
    }

    /**
     * Enables or disables automatic contraction.
     * 
     * <p>
     * When disabled, the pool never automatically contracts. This is
     * recommended for small pools where contraction overhead isn't worthwhile.
     * </p>
     *
     * @param enabled true to enable
     * @return this for chaining
     */
    public PoolSettings contractionEnabled(boolean enabled) {
        this.contractionEnabled = enabled;
        return this;
    }

    /**
     * Returns the checkpoint interval.
     *
     * @return cycles between contraction checks
     */
    public int checkpointInterval() {
        return checkpointInterval;
    }

    /**
     * Sets the checkpoint interval.
     * 
     * <p>
     * Number of allocation cycles between contraction checks. Should be
     * a power of 2 for efficient bitmask operation. Higher values reduce
     * overhead but delay contraction response.
     * </p>
     *
     * @param interval checkpoint interval (power of 2 recommended)
     * @return this for chaining
     */
    public PoolSettings checkpointInterval(int interval) {
        this.checkpointInterval = interval;
        return this;
    }

    /**
     * Returns the contraction checkpoints threshold.
     *
     * @return consecutive idle checkpoints to trigger contraction
     */
    public int contractionCheckpoints() {
        return contractionCheckpoints;
    }

    /**
     * Sets the contraction checkpoints threshold.
     * 
     * <p>
     * Number of consecutive checkpoint intervals where excess capacity
     * must remain idle before triggering contraction.
     * </p>
     *
     * @param checkpoints consecutive idle checkpoints
     * @return this for chaining
     */
    public PoolSettings contractionCheckpoints(int checkpoints) {
        this.contractionCheckpoints = checkpoints;
        return this;
    }

    /**
     * Returns the contraction threshold.
     *
     * @return idle ratio threshold (0.0 to 1.0)
     */
    public float contractionThreshold() {
        return contractionThreshold;
    }

    /**
     * Sets the contraction threshold.
     * 
     * <p>
     * Ratio of excess capacity that must be idle to count as an idle
     * checkpoint. For example, 0.75 means 75% of excess must be unused.
     * </p>
     *
     * @param threshold idle ratio (0.0 to 1.0)
     * @return this for chaining
     */
    public PoolSettings contractionThreshold(float threshold) {
        this.contractionThreshold = threshold;
        return this;
    }

    /**
     * Creates the contraction strategy based on these settings.
     *
     * @return contraction strategy (disabled or active)
     */
    public ContractionStrategy createContractionStrategy() {
        return contractionEnabled
                ? ContractionStrategy.enabled(this)
                : ContractionStrategy.disabled();
    }

    @Override
    public String toString() {
        return String.format(
                "PoolSettings[min=%d, max=%d, segment=%d, contraction=%s]",
                minCapacity, maxCapacity, segmentSize, contractionEnabled);
    }
}
/*
 * Apache License, Version 2.0
 * 
 * Copyright 2005-2025 Sly Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.slytechs.sdk.common.memory.pool;

import com.slytechs.sdk.common.settings.BooleanProperty;
import com.slytechs.sdk.common.settings.FloatProperty;
import com.slytechs.sdk.common.settings.IntProperty;
import com.slytechs.sdk.common.settings.LongProperty;
import com.slytechs.sdk.common.settings.Settings;

/**
 * Configuration settings for a {@link Pool}.
 * 
 * <p>
 * PoolSettings extends the {@link Settings} framework providing type-safe,
 * hierarchical configuration with layered property resolution from system
 * properties, environment variables, configuration files, and coded defaults.
 * </p>
 * 
 * <h2>Property Resolution</h2>
 * <p>
 * Each property resolves its value using this priority order:
 * </p>
 * <ol>
 * <li>Explicit value set programmatically</li>
 * <li>System property: {@code -Dpool.<n>=value}</li>
 * <li>Environment variable: {@code POOL_<n>=value}</li>
 * <li>Loaded configuration file</li>
 * <li>Coded default</li>
 * </ol>
 * 
 * <h2>Capacity Settings</h2>
 * <ul>
 * <li>{@link #minCapacity(int)} - Floor capacity, preallocated at startup</li>
 * <li>{@link #maxCapacity(int)} - Ceiling capacity, pool grows up to this</li>
 * <li>{@link #preallocate(boolean)} - Whether to preallocate minCapacity at startup</li>
 * </ul>
 * 
 * <h2>Memory Settings</h2>
 * <ul>
 * <li>{@link #segmentSize(long)} - Size of each memory segment</li>
 * </ul>
 * 
 * <h2>Contraction Settings</h2>
 * <ul>
 * <li>{@link #contractionEnabled(boolean)} - Enable/disable auto-contraction</li>
 * <li>{@link #checkpointInterval(int)} - Cycles between contraction checks</li>
 * <li>{@link #contractionCheckpoints(int)} - Consecutive idle checks to trigger</li>
 * <li>{@link #contractionThreshold(float)} - Idle ratio threshold</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * PoolSettings settings = new PoolSettings()
 *     .minCapacity(100)
 *     .maxCapacity(10_000)
 *     .segmentSize(9000)
 *     .preallocate(true)
 *     .contractionEnabled(true);
 * 
 * Pool<Packet> pool = new FreeListPool<>(settings, Packet::new);
 * }</pre>
 * 
 * <h2>Configuration File</h2>
 * <pre>
 * # Pool settings
 * pool.minCapacity=100
 * pool.maxCapacity=10000
 * pool.segmentSize=9000
 * pool.preallocate=true
 * pool.contraction.enabled=true
 * pool.contraction.checkpointInterval=1024
 * </pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see Pool
 * @see ContractionStrategy
 * @see Settings
 */
public class PoolSettings extends Settings {

    public static final String DOMAIN = "pool";
    public static final String BASE_NAME = "pool";

    private final IntProperty minCapacity;
    private final IntProperty maxCapacity;
    private final LongProperty segmentSize;
    private final BooleanProperty preallocate;

    private final BooleanProperty contractionEnabled;
    private final IntProperty checkpointInterval;
    private final IntProperty contractionCheckpoints;
    private final FloatProperty contractionThreshold;

    public PoolSettings() {
        super(DOMAIN, BASE_NAME);
        setComment("Memory pool configuration settings");

        this.minCapacity = intProperty("minCapacity", 64)
                .comment("Minimum pool capacity (preallocated at startup if preallocate=true)");

        this.maxCapacity = intProperty("maxCapacity", 1024)
                .comment("Maximum pool capacity (growth ceiling)");

        this.segmentSize = longProperty("segmentSize", 0)
                .comment("Memory segment size in bytes (0 for non-memory pools)");

        this.preallocate = booleanProperty("preallocate", true)
                .comment("Preallocate minCapacity objects at pool creation");

        this.contractionEnabled = booleanProperty("contraction.enabled", false)
                .comment("Enable automatic pool contraction");

        this.checkpointInterval = intProperty("contraction.checkpointInterval", 1024)
                .comment("Allocation cycles between contraction checks (power of 2 recommended)");

        this.contractionCheckpoints = intProperty("contraction.checkpoints", 10)
                .comment("Consecutive idle checkpoints before contraction triggers");

        this.contractionThreshold = floatProperty("contraction.threshold", 0.75f)
                .comment("Idle ratio threshold for contraction (0.0 to 1.0)");
    }

    /**
     * Returns the minimum capacity.
     *
     * @return minimum capacity
     */
    public int minCapacity() {
        return minCapacity.getInt();
    }

    /**
     * Sets the minimum capacity.
     * 
     * <p>
     * The pool will not contract below this size. If {@link #preallocate()} is
     * true, this capacity is preallocated at pool creation.
     * </p>
     *
     * @param minCapacity minimum capacity
     * @return this for chaining
     */
    public PoolSettings minCapacity(int minCapacity) {
        this.minCapacity.setInt(minCapacity);
        return this;
    }

    /**
     * Returns the maximum capacity.
     *
     * @return maximum capacity
     */
    public int maxCapacity() {
        return maxCapacity.getInt();
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
        this.maxCapacity.setInt(maxCapacity);
        return this;
    }

    /**
     * Sets both min and max capacity to the same value (fixed size pool).
     *
     * @param capacity fixed capacity
     * @return this for chaining
     */
    public PoolSettings capacity(int capacity) {
        this.minCapacity.setInt(capacity);
        this.maxCapacity.setInt(capacity);
        return this;
    }

    /**
     * Returns the memory segment size.
     *
     * @return segment size in bytes, or 0 for non-memory pools
     */
    public long segmentSize() {
        return segmentSize.getLong();
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
        this.segmentSize.setLong(segmentSize);
        return this;
    }

    /**
     * Returns whether objects are preallocated at pool creation.
     * 
     * <p>
     * When true, {@link #minCapacity()} objects are created during pool
     * construction. When false, objects are created on demand up to
     * {@link #maxCapacity()}.
     * </p>
     *
     * @return true if objects are preallocated
     */
    public boolean preallocate() {
        return preallocate.getBoolean();
    }

    /**
     * Sets whether to preallocate objects at pool creation.
     * 
     * <p>
     * Preallocation ensures no allocation occurs in the hot path but increases
     * startup time and initial memory usage. Lazy allocation defers object
     * creation until first use.
     * </p>
     *
     * @param preallocate true to preallocate minCapacity objects
     * @return this for chaining
     */
    public PoolSettings preallocate(boolean preallocate) {
        this.preallocate.setBoolean(preallocate);
        return this;
    }

    /**
     * Returns whether contraction is enabled.
     *
     * @return true if auto-contraction enabled
     */
    public boolean contractionEnabled() {
        return contractionEnabled.getBoolean();
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
        this.contractionEnabled.setBoolean(enabled);
        return this;
    }

    /**
     * Returns the checkpoint interval.
     *
     * @return cycles between contraction checks
     */
    public int checkpointInterval() {
        return checkpointInterval.getInt();
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
        this.checkpointInterval.setInt(interval);
        return this;
    }

    /**
     * Returns the contraction checkpoints threshold.
     *
     * @return consecutive idle checkpoints to trigger contraction
     */
    public int contractionCheckpoints() {
        return contractionCheckpoints.getInt();
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
        this.contractionCheckpoints.setInt(checkpoints);
        return this;
    }

    /**
     * Returns the contraction threshold.
     *
     * @return idle ratio threshold (0.0 to 1.0)
     */
    public float contractionThreshold() {
        return contractionThreshold.getFloat();
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
        this.contractionThreshold.setFloat(threshold);
        return this;
    }

    /**
     * Creates the contraction strategy based on these settings.
     *
     * @return contraction strategy (disabled or active)
     */
    public ContractionStrategy createContractionStrategy() {
        return contractionEnabled()
                ? ContractionStrategy.enabled(this)
                : ContractionStrategy.disabled();
    }
}
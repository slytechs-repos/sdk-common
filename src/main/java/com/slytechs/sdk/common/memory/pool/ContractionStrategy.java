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
 * Strategy for automatic pool contraction.
 * 
 * <p>
 * ContractionStrategy determines when a pool should release excess capacity.
 * The strategy is invoked on each allocation/release cycle and decides whether
 * to trigger contraction based on usage patterns.
 * </p>
 * 
 * <h2>Hot Path Optimization</h2>
 * 
 * <p>
 * The {@link #disabled()} strategy returns a singleton that does nothing.
 * The JIT compiler completely eliminates calls to this no-op implementation,
 * ensuring zero overhead when contraction is disabled.
 * </p>
 * 
 * <h2>Active Strategy</h2>
 * 
 * <p>
 * The {@link #enabled(PoolSettings)} strategy performs periodic checks using
 * a bitmask counter (avoiding modulo operations). When excess capacity remains
 * idle for multiple consecutive checkpoints, contraction is triggered.
 * </p>
 * 
 * <pre>{@code
 * // No contraction overhead for small pools
 * Pool<Packet> smallPool = new FreeListPool<>(
 *     new PoolSettings().maxCapacity(1000).contractionEnabled(false),
 *     Packet::new);
 * 
 * // Auto-contraction for large pools
 * Pool<Packet> largePool = new FreeListPool<>(
 *     new PoolSettings().maxCapacity(100_000).contractionEnabled(true),
 *     Packet::new);
 * }</pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see Pool
 * @see PoolSettings
 */
public interface ContractionStrategy {

    /**
     * Called on each pool allocation.
     * 
     * <p>
     * The strategy may use this to track allocation cycles and periodically
     * check for contraction opportunities.
     * </p>
     *
     * @param pool the pool being allocated from
     */
    void onAllocate(Pool<?> pool);

    /**
     * Called on each pool release.
     *
     * @param pool the pool being released to
     */
    void onRelease(Pool<?> pool);

    /**
     * Returns a disabled (no-op) contraction strategy.
     * 
     * <p>
     * The returned singleton has empty method bodies. The JIT compiler
     * eliminates calls to this implementation entirely, ensuring zero
     * overhead when contraction is disabled.
     * </p>
     *
     * @return disabled strategy singleton
     */
    static ContractionStrategy disabled() {
        return DisabledContractionStrategy.INSTANCE;
    }

    /**
     * Returns an active contraction strategy with the given settings.
     *
     * @param settings pool settings containing contraction parameters
     * @return active contraction strategy
     */
    static ContractionStrategy enabled(PoolSettings settings) {
        return new ActiveContractionStrategy(settings);
    }
}

/**
 * No-op contraction strategy - JIT eliminates entirely.
 */
final class DisabledContractionStrategy implements ContractionStrategy {

    static final DisabledContractionStrategy INSTANCE = new DisabledContractionStrategy();

    private DisabledContractionStrategy() {
    }

    @Override
    public void onAllocate(Pool<?> pool) {
        // No-op - JIT eliminates
    }

    @Override
    public void onRelease(Pool<?> pool) {
        // No-op - JIT eliminates
    }
}

/**
 * Active contraction strategy with checkpoint-based idle detection.
 */
final class ActiveContractionStrategy implements ContractionStrategy {

    private final int checkpointMask;
    private final int contractionCheckpoints;
    private final float contractionThreshold;

    private int cycleCounter;
    private int idleCheckpoints;

    ActiveContractionStrategy(PoolSettings settings) {
        // Ensure power of 2 for bitmask
        int interval = Integer.highestOneBit(settings.checkpointInterval());
        this.checkpointMask = interval - 1;
        this.contractionCheckpoints = settings.contractionCheckpoints();
        this.contractionThreshold = settings.contractionThreshold();
    }

    @Override
    public void onAllocate(Pool<?> pool) {
        if ((++cycleCounter & checkpointMask) == 0) {
            checkContraction(pool);
        }
    }

    @Override
    public void onRelease(Pool<?> pool) {
        // Could also trigger on release - currently allocate-only
    }

    private void checkContraction(Pool<?> pool) {
        long capacity = pool.capacity();
        long minCapacity = pool.minCapacity();
        long excess = capacity - minCapacity;

        if (excess <= 0) {
            return; // At minimum, nothing to contract
        }

        long available = pool.available();
        long excessAvailable = available - minCapacity;

        if (excessAvailable <= 0) {
            idleCheckpoints = 0; // Excess is being used
            return;
        }

        float idleRatio = (float) excessAvailable / excess;

        if (idleRatio > contractionThreshold) {
            if (++idleCheckpoints >= contractionCheckpoints) {
                // Trigger contraction - 50% of unused excess
                pool.contractUnused(0.5f);
                idleCheckpoints = 0;
            }
        } else {
            idleCheckpoints = 0; // Reset - excess is being used
        }
    }
}
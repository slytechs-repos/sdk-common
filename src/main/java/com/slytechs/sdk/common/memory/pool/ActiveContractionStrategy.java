/*
 * Copyright 2005-2026 Sly Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slytechs.sdk.common.memory.pool;

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
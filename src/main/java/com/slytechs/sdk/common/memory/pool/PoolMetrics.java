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
 * Metrics and statistics for a {@link Pool}.
 * 
 * <p>
 * PoolMetrics tracks allocation patterns, growth/contraction events, and
 * exhaustion counts. For bucket-based pools, per-bucket metrics are available
 * via the bucket index methods.
 * </p>
 * 
 * <h2>RMON-Style Counters</h2>
 * 
 * <p>
 * Metrics follow RMON (Remote Monitoring) style - monotonically increasing
 * counters that can be sampled at intervals to compute rates:
 * </p>
 * 
 * <pre>{@code
 * // Sample every second
 * long prevAllocs = metrics.allocations();
 * Thread.sleep(1000);
 * long rate = metrics.allocations() - prevAllocs;  // allocations/sec
 * }</pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see Pool
 */
public interface PoolMetrics {

    /**
     * Returns total allocation count.
     *
     * @return number of successful allocations
     */
    long allocations();

    /**
     * Returns total release count.
     *
     * @return number of releases back to pool
     */
    long releases();

    /**
     * Returns exhaustion count.
     * 
     * <p>
     * Number of times allocation failed because pool was at max capacity
     * with no available objects.
     * </p>
     *
     * @return exhaustion count
     */
    long exhaustions();

    /**
     * Returns growth event count.
     * 
     * <p>
     * Number of times pool capacity was increased.
     * </p>
     *
     * @return growth count
     */
    long growthEvents();

    /**
     * Returns contraction event count.
     * 
     * <p>
     * Number of times pool capacity was decreased.
     * </p>
     *
     * @return contraction count
     */
    long contractions();

    /**
     * Returns total entries evicted during contractions.
     *
     * @return evicted entry count
     */
    long evictions();

    /**
     * Returns the number of buckets.
     * 
     * <p>
     * For non-bucket pools, returns 1 (the pool itself is the single bucket).
     * </p>
     *
     * @return bucket count
     */
    default int bucketCount() {
        return 1;
    }

    /**
     * Returns the byte size for a bucket.
     *
     * @param bucketIndex bucket index
     * @return byte size for that bucket
     */
    default long bucketSize(int bucketIndex) {
        return 0;
    }

    /**
     * Returns allocations for a specific bucket.
     *
     * @param bucketIndex bucket index
     * @return allocation count for that bucket
     */
    default long allocations(int bucketIndex) {
        return bucketIndex == 0 ? allocations() : 0;
    }

    /**
     * Returns releases for a specific bucket.
     *
     * @param bucketIndex bucket index
     * @return release count for that bucket
     */
    default long releases(int bucketIndex) {
        return bucketIndex == 0 ? releases() : 0;
    }

    /**
     * Returns exhaustions for a specific bucket.
     *
     * @param bucketIndex bucket index
     * @return exhaustion count for that bucket
     */
    default long exhaustions(int bucketIndex) {
        return bucketIndex == 0 ? exhaustions() : 0;
    }

    /**
     * Returns current capacity for a specific bucket.
     *
     * @param bucketIndex bucket index
     * @return capacity for that bucket
     */
    default long bucketCapacity(int bucketIndex) {
        return 0;
    }

    /**
     * Returns available count for a specific bucket.
     *
     * @param bucketIndex bucket index
     * @return available count for that bucket
     */
    default long bucketAvailable(int bucketIndex) {
        return 0;
    }
}
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
 * A pool of reusable {@link Poolable} objects.
 * 
 * <p>
 * Pool provides efficient object reuse through a free-list mechanism, avoiding
 * allocation overhead in hot paths. Pools support dynamic sizing between
 * configurable minimum and maximum capacities, with optional automatic
 * contraction when excess capacity is idle.
 * </p>
 * 
 * <h2>Allocation Model</h2>
 * 
 * <p>
 * Objects are allocated via {@link #allocate()} or {@link #allocate(long)} and
 * returned via {@link Poolable#recycle()} or {@link #release(Poolable)}. The
 * pool maintains a free-list of available objects, growing on demand up to
 * {@link #maxCapacity()}.
 * </p>
 * 
 * <h2>Size-Aware Allocation</h2>
 * 
 * <p>
 * For memory-backed pools, {@link #allocate(long)} requests a minimum byte
 * capacity. The pool returns an object meeting that requirement, or null if
 * unavailable. {@link #maxByteSize()} indicates the largest allocation the
 * pool can satisfy. Non-memory pools return 0 for {@link #maxByteSize()}.
 * </p>
 * 
 * <h2>Dynamic Sizing</h2>
 * 
 * <p>
 * Pools grow automatically when exhausted (up to {@link #maxCapacity()}) and
 * can contract when excess capacity is unused. Contraction is triggered via
 * {@link #contractUnused(float)} or {@link #contractUnused(long)}, which may
 * be called manually or by a {@link ContractionStrategy}.
 * </p>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * Pool<Packet> pool = new FreeListPool<>(settings, Packet::new);
 * 
 * // Allocate from pool
 * Packet packet = pool.allocate();
 * if (packet == null) {
 *     // Pool exhausted
 * }
 * 
 * // Use packet...
 * 
 * // Return to pool
 * packet.recycle();  // or pool.release(packet)
 * }</pre>
 *
 * @param <T> the type of poolable objects
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see Poolable
 * @see PoolEntry
 * @see FreeListPool
 */
public interface Pool<T extends Poolable> extends AutoCloseable {

    /**
     * Allocates an object from the pool.
     * 
     * <p>
     * Returns an available object from the free-list, or creates a new one
     * if the pool can grow. Returns null if the pool is exhausted (at max
     * capacity with no available objects).
     * </p>
     *
     * @return an allocated object, or null if pool exhausted
     */
    T allocate();

    /**
     * Allocates an object with at least the specified byte capacity.
     * 
     * <p>
     * For memory-backed pools, returns an object whose backing memory is at
     * least {@code requiredSize} bytes. For non-memory pools, delegates to
     * {@link #allocate()} if {@code requiredSize} is 0, otherwise returns null.
     * </p>
     *
     * @param requiredSize minimum required byte capacity
     * @return an allocated object meeting size requirement, or null if unavailable
     */
    default T allocate(long requiredSize) {
        if (requiredSize <= 0 || requiredSize <= maxByteSize()) {
            return allocate();
        }
        return null;
    }

    /**
     * Releases an object back to the pool.
     * 
     * <p>
     * Equivalent to calling {@link Poolable#recycle()} on the object.
     * </p>
     *
     * @param item the object to release
     */
    default void release(T item) {
        if (item != null) {
            item.recycle();
        }
    }

    /**
     * Releases a pool entry back to the pool.
     * 
     * <p>
     * Internal method called by {@link PoolEntry#recycle()}. Invokes
     * {@link PoolEntry#onRecycle()} and adds the entry to the free-list.
     * </p>
     *
     * @param entry the entry to release
     */
    void releaseEntry(PoolEntry entry);

    /**
     * Returns the minimum capacity.
     * 
     * <p>
     * The pool will not contract below this size. This capacity is
     * typically preallocated at pool creation.
     * </p>
     *
     * @return minimum capacity
     */
    long minCapacity();

    /**
     * Returns the maximum capacity.
     * 
     * <p>
     * The pool will not grow beyond this size. Allocation requests when
     * at max capacity with no available objects return null.
     * </p>
     *
     * @return maximum capacity
     */
    long maxCapacity();

    /**
     * Returns the current capacity.
     * 
     * <p>
     * The total number of objects currently managed by the pool (both
     * allocated and available). Between {@link #minCapacity()} and
     * {@link #maxCapacity()}.
     * </p>
     *
     * @return current capacity
     */
    long capacity();

    /**
     * Returns the number of available objects.
     * 
     * <p>
     * Objects currently in the free-list, ready for allocation.
     * </p>
     *
     * @return available count
     */
    long available();

    /**
     * Returns the maximum byte size this pool can provide.
     * 
     * <p>
     * For memory-backed pools, this is the size of the backing memory
     * segments. For non-memory pools, returns 0.
     * </p>
     *
     * @return max byte size, or 0 for non-memory pools
     */
    default long maxByteSize() {
        return 0;
    }

    /**
     * Contracts unused capacity by a percentage.
     * 
     * <p>
     * Removes up to the specified percentage of available (unused) objects
     * from the pool, respecting {@link #minCapacity()}. Evicted entries
     * have their slab memory freed.
     * </p>
     *
     * @param percent percentage of available to contract (0.0 to 1.0)
     * @return actual number of entries contracted
     */
    long contractUnused(float percent);

    /**
     * Contracts unused capacity by count.
     * 
     * <p>
     * Removes up to the specified number of available (unused) objects
     * from the pool, respecting {@link #minCapacity()}. Evicted entries
     * have their slab memory freed.
     * </p>
     *
     * @param count maximum number of entries to contract
     * @return actual number of entries contracted
     */
    long contractUnused(long count);

    /**
     * Grows the pool by the specified count.
     * 
     * <p>
     * Adds new objects to the pool up to {@link #maxCapacity()}.
     * </p>
     *
     * @param count number of entries to add
     * @return actual number of entries added
     */
    long grow(long count);

    /**
     * Returns the pool metrics.
     *
     * @return pool metrics
     */
    PoolMetrics metrics();

    /**
     * Returns whether the pool is closed.
     *
     * @return true if closed
     */
    boolean isClosed();

    /**
     * Closes the pool and releases all resources.
     * 
     * <p>
     * All managed objects are evicted and their slab memory freed.
     * After closing, allocation requests return null.
     * </p>
     */
    @Override
    void close();
}
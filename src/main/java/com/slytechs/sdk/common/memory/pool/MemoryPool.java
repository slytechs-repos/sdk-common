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

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import com.slytechs.sdk.common.memory.FixedMemory;

/**
 * High-performance pool of pre-allocated {@link FixedMemory} segments.
 * 
 * <p>
 * MemoryPool manages fixed-size memory segments allocated from {@link SlabAllocator}s.
 * Each FixedMemory is bound to its segment once at creation and stays bound until
 * eviction - no binding/unbinding occurs in hot paths.
 * </p>
 * 
 * <h2>Design</h2>
 * 
 * <ul>
 * <li><strong>Growth:</strong> SlabAllocator provides segments, FixedMemory wraps them permanently</li>
 * <li><strong>Allocate (hot):</strong> Pop from free-list, reset bounds, return</li>
 * <li><strong>Recycle (hot):</strong> Reset bounds, push to free-list</li>
 * <li><strong>Eviction:</strong> Free slab segment, FixedMemory becomes garbage</li>
 * </ul>
 * 
 * <h2>Headroom and Tailroom</h2>
 * 
 * <pre>
 * |--- headroom ---|--- active data region ---|--- tailroom ---|
 * ^                ^                          ^                ^
 * 0              start                       end            capacity
 * </pre>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * MemoryPoolSettings settings = new MemoryPoolSettings()
 *     .minCapacity(64)
 *     .maxCapacity(1024)
 *     .segmentSize(9000)
 *     .headroom(64)
 *     .tailroom(4);
 * 
 * MemoryPool pool = new MemoryPool(settings);
 * 
 * FixedMemory memory = pool.allocate();
 * // Use memory - start/end set with headroom/tailroom
 * 
 * memory.recycle();  // Returns to pool, segment stays bound
 * pool.close();
 * }</pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see FixedMemory
 * @see MemoryPoolSettings
 * @see SlabAllocator
 */
public class MemoryPool implements Pool<FixedMemory> {

    private static final VarHandle HEAD;

    static {
        try {
            HEAD = MethodHandles.lookup().findVarHandle(
                    MemoryPool.class, "head", PoolEntry.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final MemoryPoolSettings settings;
    private final ContractionStrategy contraction;
    private final int slabSize;
    private final Metrics metrics;

    private volatile PoolEntry head;
    private volatile long capacity;
    private volatile boolean closed;

    private SlabAllocator currentSlab;

    /**
     * Creates a memory pool with the specified settings.
     *
     * @param settings pool configuration
     */
    public MemoryPool(MemoryPoolSettings settings) {
        if (settings == null) {
            throw new NullPointerException("Settings cannot be null");
        }
        settings.validate();

        this.settings = settings;
        this.contraction = settings.createContractionStrategy();
        this.slabSize = computeSlabSize(settings);
        this.metrics = new Metrics();
        this.capacity = 0;
        this.closed = false;

        // Preallocate min capacity
        grow(settings.minCapacity());
    }

    @Override
    public FixedMemory allocate() {
        contraction.onAllocate(this);

        // Try to pop from free-list
        PoolEntry entry = pop();

        if (entry == null) {
            // Try to grow
            if (capacity < settings.maxCapacity()) {
                grow(slabSize);
                entry = pop();
            }
        }

        if (entry == null) {
            metrics.exhaustions++;
            return null;
        }

        // Reset bounds for new use (hot path - no rebinding)
        FixedMemory memory = (FixedMemory) entry.owner;
        initializeBounds(memory);

        entry.onAllocate();
        metrics.allocations++;

        return memory;
    }

    @Override
    public void releaseEntry(PoolEntry entry) {
        if (entry == null || closed) {
            return;
        }

        // Reset bounds (hot path - no unbinding)
        if (entry.owner instanceof FixedMemory memory) {
            initializeBounds(memory);
        }

        entry.onRecycle();
        push(entry);
        metrics.releases++;

        contraction.onRelease(this);
    }

    @Override
    public long grow(long count) {
        if (closed) {
            return 0;
        }

        long maxGrowth = settings.maxCapacity() - capacity;
        long actualGrowth = Math.min(count, maxGrowth);

        if (actualGrowth <= 0) {
            return 0;
        }

        // Create new slab if needed
        if (currentSlab == null || !currentSlab.hasCapacity()) {
            currentSlab = new SlabAllocator(settings.segmentSize(), slabSize);
        }

        long grown = 0;
        for (int i = 0; i < actualGrowth; i++) {
            MemorySegment segment = currentSlab.allocate(settings.segmentSize(), 8);
            if (segment == null) {
                // Current slab exhausted, create new one
                currentSlab = new SlabAllocator(settings.segmentSize(), slabSize);
                segment = currentSlab.allocate(settings.segmentSize(), 8);
            }

            if (segment != null) {
                // Create FixedMemory bound permanently to this segment
                FixedMemory memory = new FixedMemory(segment);
                initializeBounds(memory);

                // Set up pool entry for lifecycle management
                PoolEntry entry = memory.poolEntry();
                entry.owner = memory;
                entry.owningPool = this;
                entry.bindSlab(currentSlab, segment);

                push(entry);
                grown++;
            }
        }

        capacity += grown;
        if (grown > 0) {
            metrics.growthEvents++;
        }

        return grown;
    }

    @Override
    public long contractUnused(float percent) {
        long available = available();
        long count = (long) (available * percent);
        return contractUnused(count);
    }

    @Override
    public long contractUnused(long count) {
        if (closed || count <= 0) {
            return 0;
        }

        long available = available();
        long excess = capacity - settings.minCapacity();
        long maxContractable = Math.min(available, excess);
        long actualContract = Math.min(count, maxContractable);

        if (actualContract <= 0) {
            return 0;
        }

        long contracted = 0;
        for (int i = 0; i < actualContract; i++) {
            PoolEntry entry = pop();
            if (entry == null) {
                break;
            }

            // Evict - frees slab segment, FixedMemory becomes garbage
            entry.onEvict();
            contracted++;
        }

        capacity -= contracted;
        if (contracted > 0) {
            metrics.contractions++;
            metrics.evictions += contracted;
        }

        return contracted;
    }

    private void initializeBounds(FixedMemory memory) {
        long headroom = settings.headroom();
        long tailroom = settings.tailroom();
        long segSize = settings.segmentSize();

        memory.start(headroom);
        memory.end(segSize - tailroom);
    }

    private void push(PoolEntry entry) {
        PoolEntry oldHead;
        do {
            oldHead = head;
            entry.next = oldHead;
        } while (!HEAD.compareAndSet(this, oldHead, entry));
    }

    private PoolEntry pop() {
        PoolEntry oldHead;
        PoolEntry newHead;
        do {
            oldHead = head;
            if (oldHead == null) {
                return null;
            }
            newHead = oldHead.next;
        } while (!HEAD.compareAndSet(this, oldHead, newHead));

        oldHead.next = null;
        return oldHead;
    }

    private static int computeSlabSize(MemoryPoolSettings settings) {
        int tenPercent = settings.maxCapacity() / 10;
        int oneMbWorth = (int) (1024 * 1024 / settings.segmentSize());
        int slabSize = Math.min(tenPercent, oneMbWorth);
        return Math.max(slabSize, 16);
    }

    @Override
    public long minCapacity() {
        return settings.minCapacity();
    }

    @Override
    public long maxCapacity() {
        return settings.maxCapacity();
    }

    @Override
    public long capacity() {
        return capacity;
    }

    @Override
    public long available() {
        long count = 0;
        PoolEntry e = head;
        while (e != null) {
            count++;
            e = e.next;
        }
        return count;
    }

    @Override
    public long maxByteSize() {
        return settings.segmentSize();
    }

    @Override
    public PoolMetrics metrics() {
        return metrics;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;

        // Evict all entries
        PoolEntry entry;
        while ((entry = pop()) != null) {
            entry.onEvict();
        }

        // Close current slab if any remaining
        if (currentSlab != null) {
            currentSlab.close();
            currentSlab = null;
        }

        capacity = 0;
    }

    /**
     * Returns the segment size.
     *
     * @return size of each segment in bytes
     */
    public long segmentSize() {
        return settings.segmentSize();
    }

    /**
     * Returns the headroom.
     *
     * @return headroom in bytes
     */
    public long headroom() {
        return settings.headroom();
    }

    /**
     * Returns the tailroom.
     *
     * @return tailroom in bytes
     */
    public long tailroom() {
        return settings.tailroom();
    }

    /**
     * Returns the usable data size per segment.
     *
     * @return usable size in bytes (segmentSize - headroom - tailroom)
     */
    public long usableSize() {
        return settings.usableSize();
    }

    /**
     * Returns pool settings.
     *
     * @return settings instance
     */
    public MemoryPoolSettings settings() {
        return settings;
    }

    @Override
    public String toString() {
        return String.format(
                "MemoryPool[capacity=%d/%d, available=%d, segment=%d, headroom=%d, tailroom=%d]",
                capacity, settings.maxCapacity(), available(), segmentSize(), headroom(), tailroom());
    }

    /**
     * Internal metrics implementation.
     */
    private static class Metrics implements PoolMetrics {
        volatile long allocations;
        volatile long releases;
        volatile long exhaustions;
        volatile long growthEvents;
        volatile long contractions;
        volatile long evictions;

        @Override public long allocations() { return allocations; }
        @Override public long releases() { return releases; }
        @Override public long exhaustions() { return exhaustions; }
        @Override public long growthEvents() { return growthEvents; }
        @Override public long contractions() { return contractions; }
        @Override public long evictions() { return evictions; }
    }
}
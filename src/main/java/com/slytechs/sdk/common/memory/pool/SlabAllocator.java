/*
 * Apache License, Version 2.0
 * 
 * Copyright 2025 Sly Technologies Inc.
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

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;

/**
 * A slicing allocator with reference counting and automatic arena closure.
 * 
 * <p>
 * SlabAllocator allocates a bulk memory segment from an Arena at construction,
 * then slices individual segments from it. Each slice is reference counted -
 * when all slices are freed, the arena is automatically closed and memory
 * released. Once closed, the slab cannot be reused.
 * </p>
 * 
 * <h2>Lifecycle</h2>
 * 
 * <pre>
 * new SlabAllocator(size, count)
 *     │
 *     ▼ Arena + bulk allocated immediately
 * ACTIVE (arena != null)
 *     │
 *     ├──► allocate() - slice from bulk
 *     │
 *     ├──► free() - decrement outstanding
 *     │
 *     ▼ outstanding reaches 0
 * CLOSED (arena == null) - memory freed, slab dead
 * </pre>
 * 
 * <h2>Pool Integration</h2>
 * 
 * <p>
 * Pool creates a new SlabAllocator when growth is needed. Entries track their
 * slab via {@code PoolEntry.slab}. When entries are evicted during contraction,
 * they call {@code slab.free()}, which may trigger auto-close. The pool never
 * tracks old slabs - entries manage slab lifecycle automatically.
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class SlabAllocator implements SegmentAllocator, AutoCloseable {

    private Arena arena;
    private MemorySegment bulk;
    
    private final int capacity;
    private final long segmentSize;
    
    private long offset;
    private int allocated;
    private int outstanding;

    /**
     * Creates a slab allocator and allocates memory immediately.
     *
     * @param segmentSize size of each segment in bytes
     * @param capacity maximum number of segments in this slab
     */
    public SlabAllocator(long segmentSize, int capacity) {
        this(segmentSize, capacity, 8);
    }

    /**
     * Creates a slab allocator with custom alignment.
     *
     * @param segmentSize size of each segment in bytes
     * @param capacity maximum number of segments in this slab
     * @param alignment byte alignment for bulk allocation
     */
    public SlabAllocator(long segmentSize, int capacity, long alignment) {
        this.segmentSize = segmentSize;
        this.capacity = capacity;
        
        this.arena = Arena.ofShared();
        this.bulk = arena.allocate(segmentSize * capacity, alignment);
        this.offset = 0;
        this.allocated = 0;
        this.outstanding = 0;
    }

    @Override
    public MemorySegment allocate(long byteSize, long byteAlignment) {
        if (arena == null || allocated >= capacity) {
            return null;
        }
        
        long aligned = alignUp(offset, byteAlignment);
        long end = aligned + byteSize;
        
        if (end > bulk.byteSize()) {
            return null;
        }
        
        MemorySegment slice = bulk.asSlice(aligned, byteSize);
        offset = end;
        allocated++;
        outstanding++;
        
        return slice;
    }

    /**
     * Frees a previously allocated segment.
     * 
     * <p>
     * Decrements the outstanding count. When count reaches zero, the arena
     * is automatically closed and the slab becomes unusable.
     * </p>
     *
     * @param segment the segment to free (must have been allocated from this slab)
     * @return true if arena was auto-closed
     */
    public boolean free(MemorySegment segment) {
        if (arena == null || outstanding <= 0) {
            return false;
        }
        
        outstanding--;
        
        if (outstanding == 0) {
            close();
            return true;
        }
        
        return false;
    }

    /**
     * Checks if a segment belongs to this slab.
     *
     * @param segment the segment to check
     * @return true if segment was allocated from this slab
     */
    public boolean owns(MemorySegment segment) {
        if (segment == null || bulk == null) {
            return false;
        }
        
        long segAddr = segment.address();
        long bulkAddr = bulk.address();
        
        return segAddr >= bulkAddr && segAddr < bulkAddr + bulk.byteSize();
    }

    /**
     * Returns whether this slab is still active.
     *
     * @return true if arena is alive
     */
    public boolean isActive() {
        return arena != null;
    }

    /**
     * Returns whether this slab has capacity for more allocations.
     *
     * @return true if more segments can be allocated
     */
    public boolean hasCapacity() {
        return arena != null && allocated < capacity;
    }

    /**
     * Returns the number of segments allocated.
     *
     * @return allocated count
     */
    public int allocated() {
        return allocated;
    }

    /**
     * Returns the number of segments not yet freed.
     *
     * @return outstanding count
     */
    public int outstanding() {
        return outstanding;
    }

    /**
     * Returns the maximum segments this slab can hold.
     *
     * @return capacity
     */
    public int capacity() {
        return capacity;
    }

    /**
     * Returns the segment size.
     *
     * @return segment size in bytes
     */
    public long segmentSize() {
        return segmentSize;
    }

    /**
     * Closes this slab and releases all memory.
     * 
     * <p>
     * After closing, the slab cannot be used for allocations.
     * </p>
     */
    @Override
    public void close() {
        if (arena != null) {
            arena.close();
            arena = null;
            bulk = null;
        }
    }

    private static long alignUp(long offset, long alignment) {
        return (offset + alignment - 1) & ~(alignment - 1);
    }

    @Override
    public String toString() {
        return String.format("SlabAllocator[capacity=%d, allocated=%d, outstanding=%d, active=%s]",
                capacity, allocated, outstanding, arena != null);
    }
}
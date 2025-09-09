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

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * Pool for FixedMemory objects with pre-allocated segments.
 * 
 * <p>
 * FixedMemoryPool pre-allocates all memory segments during initialization,
 * providing predictable performance and memory usage. All segments are
 * allocated from a single Arena for efficient memory management.
 * </p>
 * 
 * <h2>Characteristics</h2>
 * <ul>
 * <li>All memory pre-allocated at construction</li>
 * <li>Fixed segment size for all allocations</li>
 * <li>Configurable headroom for protocol operations</li>
 * <li>Automatic recycling when refcount reaches zero</li>
 * </ul>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 */
public class FixedMemoryPool extends AbstractMemoryPool<FixedMemory> {
    
    /** Size of each segment in bytes */
    private final long segmentSize;
    
    /** Arena for memory allocation */
    private final Arena arena;
    
    /**
     * Creates a pool with specified capacity and segment size.
     * 
     * @param name pool name
     * @param capacity pool capacity
     * @param segmentSize size of each segment
     * @param defaultHeadroom default headroom
     */
    public FixedMemoryPool(String name, int capacity, long segmentSize, long defaultHeadroom) {
        super(name, capacity, defaultHeadroom);
        this.segmentSize = segmentSize;
        this.arena = Arena.ofShared();
        
        initializePool();
    }
    
    /**
     * Creates a pool with default headroom.
     * 
     * @param name pool name
     * @param capacity pool capacity
     * @param segmentSize size of each segment
     */
    public FixedMemoryPool(String name, int capacity, long segmentSize) {
        this(name, capacity, segmentSize, 128); // Default 128 bytes headroom
    }
    
    /**
     * Initializes the pool by pre-allocating all segments.
     */
    private void initializePool() {
        // Allocate one large block for better memory locality
        long totalSize = capacity * segmentSize;
        MemorySegment totalMemory = arena.allocate(totalSize);
        
        // Slice into individual segments
        for (int i = 0; i < capacity; i++) {
            long offset = i * segmentSize;
            MemorySegment segment = totalMemory.asSlice(offset, segmentSize);
            
            FixedMemory memory = new FixedMemory(this, segment, 0, segmentSize);
            
            // Set initial data boundaries with headroom
            memory.start(defaultHeadroom);
            memory.end(defaultHeadroom); // Empty initially
            
            addToFreeList(memory);
        }
    }
    
    @Override
    public FixedMemory allocate() {
        FixedMemory memory = allocateFromFreeList();
        if (memory != null) {
            // Reset for new use
            memory.start(defaultHeadroom);
            memory.end(defaultHeadroom);
        }
        return memory;
    }
    
    /**
     * Allocates a FixedMemory with specified data size.
     * 
     * @param dataSize required data size in bytes
     * @return allocated memory or null if too large or pool exhausted
     */
    public FixedMemory allocate(long dataSize) {
        if (dataSize > segmentSize - defaultHeadroom) {
            return null; // Too large for pool segments
        }
        
        FixedMemory memory = allocate();
        if (memory != null) {
            memory.end(defaultHeadroom + dataSize);
        }
        return memory;
    }
    
    /**
     * Returns the segment size.
     * 
     * @return segment size in bytes
     */
    public long getSegmentSize() {
        return segmentSize;
    }
    
    /**
     * Closes the pool and releases all memory.
     */
    public void close() {
        arena.close();
    }
}
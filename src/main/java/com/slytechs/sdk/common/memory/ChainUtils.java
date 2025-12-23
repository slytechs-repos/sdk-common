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
package com.slytechs.sdk.common.memory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility methods for memory chain operations and reference counting.
 * 
 * <p>
 * ChainUtils provides thread-safe reference counting operations and utilities
 * for navigating and manipulating memory chains. These utilities are used
 * internally by memory implementations to ensure correct lifecycle management
 * and efficient chain traversal.
 * </p>
 * 
 * <h2>Reference Counting</h2>
 * <p>
 * Provides atomic increment/decrement operations with proper error handling
 * for underflow and attempts to increment released memory.
 * </p>
 * 
 * <h2>Chain Navigation</h2>
 * <p>
 * Utilities for locating segments within chains, calculating total lengths,
 * and determining if operations span multiple segments.
 * </p>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 */
public final class ChainUtils {
    
    private ChainUtils() {
        // Utility class - no instantiation
    }
    
    // ==================== Reference Counting ====================
    
    /**
     * Thread-safely increments a reference counter.
     * 
     * <p>
     * Uses compare-and-swap to atomically increment the counter. Prevents
     * incrementing a released object (count = 0) to avoid use-after-free.
     * </p>
     * 
     * @param refCount the atomic reference counter
     * @return the new reference count
     * @throws IllegalStateException if memory is already released (count = 0)
     */
    public static int incrementRef(AtomicInteger refCount) {
        int current;
        do {
            current = refCount.get();
            if (current == 0) {
                throw new IllegalStateException("Cannot increment reference count on released memory");
            }
        } while (!refCount.compareAndSet(current, current + 1));
        return current + 1;
    }
    
    /**
     * Thread-safely decrements a reference counter.
     * 
     * <p>
     * Atomically decrements the counter and checks for underflow. If underflow
     * is detected, the counter is reset to 0 and an exception is thrown.
     * </p>
     * 
     * @param refCount the atomic reference counter
     * @return the new reference count
     * @throws IllegalStateException if decrement would cause underflow
     */
    public static int decrementRef(AtomicInteger refCount) {
        int newRef = refCount.decrementAndGet();
        if (newRef < 0) {
            refCount.set(0);
            throw new IllegalStateException("Reference count underflow - possible double-free");
        }
        return newRef;
    }
    
    // ==================== Chain Navigation ====================
    
    /**
     * Represents a location within a memory chain.
     * 
     * <p>
     * Encapsulates both the segment containing a specific offset and the
     * local offset within that segment.
     * </p>
     */
    public static class SegmentLocation {
        /** The segment containing the offset */
        public final Memory segment;
        
        /** The local offset within the segment */
        public final long localOffset;
        
        /**
         * Constructs a segment location.
         * 
         * @param segment the containing segment
         * @param localOffset the offset within the segment
         */
        public SegmentLocation(Memory segment, long localOffset) {
            this.segment = segment;
            this.localOffset = localOffset;
        }
    }
    
    /**
     * Locates the segment containing the specified offset within a chain.
     * 
     * <p>
     * Traverses the chain from the head, accumulating lengths until the
     * target offset is found. The offset is relative to active data lengths.
     * </p>
     * 
     * @param chain the head of the chain
     * @param offset the offset to locate
     * @return the segment and local offset
     * @throws IndexOutOfBoundsException if offset exceeds chain length
     */
    public static SegmentLocation locate(Memory chain, long offset) {
        Memory current = chain;
        long remaining = offset;
        
        while (current != null && remaining >= current.length()) {
            remaining -= current.length();
            current = current.nextSegment();
        }
        
        if (current == null) {
            throw new IndexOutOfBoundsException("Offset " + offset + " exceeds chain length");
        }
        
        return new SegmentLocation(current, remaining);
    }
    
    /**
     * Locates the segment containing the specified offset using segment sizes.
     * 
     * <p>
     * Similar to {@link #locate(Memory, long)} but uses total segment sizes
     * rather than active data lengths. Useful for operations that need to
     * access the full segment space including headroom/tailroom.
     * </p>
     * 
     * @param chain the head of the chain
     * @param offset the offset to locate
     * @return the segment and local offset
     * @throws IndexOutOfBoundsException if offset exceeds chain size
     */
    public static SegmentLocation locateBySegmentSize(Memory chain, long offset) {
        Memory current = chain;
        long remaining = offset;
        
        while (current != null && remaining >= current.byteSize()) {
            remaining -= current.byteSize();
            current = current.nextSegment();
        }
        
        if (current == null) {
            throw new IndexOutOfBoundsException("Offset " + offset + " exceeds chain size");
        }
        
        return new SegmentLocation(current, remaining);
    }
    
    /**
     * Calculates the total length of active data in a memory chain.
     * 
     * <p>
     * Traverses the entire chain summing the length() of each segment.
     * </p>
     * 
     * @param chain the head of the chain
     * @return total active data length in bytes
     */
    public static long calculateChainLength(Memory chain) {
        long total = 0;
        Memory current = chain;
        
        while (current != null) {
            total += current.length();
            current = current.nextSegment();
        }
        
        return total;
    }
    
    /**
     * Counts the number of segments in a chain.
     * 
     * <p>
     * Traverses the entire chain counting segments.
     * </p>
     * 
     * @param chain the head of the chain
     * @return the segment count
     */
    public static int countSegments(Memory chain) {
        int count = 0;
        Memory current = chain;
        
        while (current != null) {
            count++;
            current = current.nextSegment();
        }
        
        return count;
    }
    
    /**
     * Checks if data at the specified position spans multiple segments.
     * 
     * <p>
     * Determines if accessing dataSize bytes starting at localPosition would
     * extend beyond the current segment's boundaries, requiring multi-segment
     * operations.
     * </p>
     * 
     * @param memory the memory segment
     * @param localPosition position within the segment's active data
     * @param dataSize size of data to check
     * @return true if data spans segments
     */
    public static boolean needsSpanning(Memory memory, long localPosition, long dataSize) {
        long segmentRemaining = memory.end() - (memory.start() + localPosition);
        return dataSize > segmentRemaining;
    }
    
    /**
     * Creates a deep copy of a memory chain.
     * 
     * <p>
     * Allocates new memory for each segment and copies the active data.
     * Useful for creating independent copies that won't be affected by
     * changes to the original chain.
     * </p>
     * 
     * @param chain the chain to copy
     * @param allocator function to allocate new segments
     * @return the head of the copied chain
     */
    public static Memory copyChain(Memory chain, java.util.function.Function<Long, Memory> allocator) {
        if (chain == null) {
            return null;
        }
        
        Memory newHead = allocator.apply(chain.length());
        Memory current = chain;
        Memory newCurrent = newHead;
        
        // Copy first segment
        copySegmentData(current, newCurrent);
        
        // Copy remaining segments
        while (current.hasNextSegment()) {
            current = current.nextSegment();
            Memory newNext = allocator.apply(current.length());
            copySegmentData(current, newNext);
            newCurrent.nextSegment(newNext);
            newCurrent = newNext;
        }
        
        return newHead;
    }
    
    /**
     * Copies active data from source to destination segment.
     * 
     * @param source the source segment
     * @param dest the destination segment
     */
    private static void copySegmentData(Memory source, Memory dest) {
        long length = source.length();
        source.segment().asSlice(source.start(), length)
            .copyFrom(dest.segment().asSlice(dest.start(), length));
    }
}
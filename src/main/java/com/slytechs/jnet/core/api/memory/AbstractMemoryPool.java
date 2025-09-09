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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract base implementation for lock-free memory pools using CAS operations.
 * 
 * <p>
 * Provides a high-performance, lock-free pool implementation using a CAS-based
 * free list. This design enables efficient allocation and deallocation at high
 * packet rates without contention.
 * </p>
 * 
 * <h2>Design Principles</h2>
 * <ul>
 * <li><strong>Lock-free:</strong> Uses CAS operations for thread-safe access</li>
 * <li><strong>Pre-allocated:</strong> All objects created during initialization</li>
 * <li><strong>Zero allocation:</strong> No objects created after pool initialization</li>
 * <li><strong>O(1) operations:</strong> Constant time allocate/release</li>
 * </ul>
 * 
 * @param <T> the type of objects managed by this pool
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 */
public abstract class AbstractMemoryPool<T> implements MemoryPool<T> {
    
    /**
     * Node in the lock-free stack.
     */
    protected static class Node<T> {
        final T item;
        volatile Node<T> next;
        
        Node(T item) {
            this.item = item;
        }
    }
    
    /** Pool name for debugging and monitoring */
    protected final String name;
    
    /** Total capacity of the pool */
    protected final int capacity;
    
    /** Default headroom for allocations */
    protected final long defaultHeadroom;
    
    /** Head of the free list (lock-free stack) */
    protected final AtomicReference<Node<T>> head = new AtomicReference<>();
    
    /** Number of available objects */
    protected final AtomicInteger available = new AtomicInteger(0);
    
    /** Statistics counters */
    protected final AtomicInteger allocations = new AtomicInteger(0);
    protected final AtomicInteger releases = new AtomicInteger(0);
    protected final AtomicInteger exhaustions = new AtomicInteger(0);
    
    /**
     * Constructs a pool with specified parameters.
     * 
     * @param name pool name for identification
     * @param capacity maximum number of objects
     * @param defaultHeadroom default headroom for allocations
     */
    protected AbstractMemoryPool(String name, int capacity, long defaultHeadroom) {
        this.name = name;
        this.capacity = capacity;
        this.defaultHeadroom = defaultHeadroom;
    }
    
    /**
     * Adds an object to the free list.
     * 
     * <p>
     * Thread-safe addition using CAS operations.
     * </p>
     * 
     * @param item the item to add
     */
    protected void addToFreeList(T item) {
        Node<T> newNode = new Node<>(item);
        Node<T> currentHead;
        do {
            currentHead = head.get();
            newNode.next = currentHead;
        } while (!head.compareAndSet(currentHead, newNode));
        available.incrementAndGet();
    }
    
    /**
     * Allocates an object from the free list.
     * 
     * <p>
     * Thread-safe removal using CAS operations.
     * </p>
     * 
     * @return the allocated object or null if exhausted
     */
    protected T allocateFromFreeList() {
        Node<T> currentHead;
        Node<T> newHead;
        
        do {
            currentHead = head.get();
            if (currentHead == null) {
                exhaustions.incrementAndGet();
                return null; // Pool exhausted
            }
            newHead = currentHead.next;
        } while (!head.compareAndSet(currentHead, newHead));
        
        available.decrementAndGet();
        allocations.incrementAndGet();
        return currentHead.item;
    }
    
    @Override
    public void release(T memory) {
        if (memory == null) {
            return;
        }
        
        // Reset/recycle the object
        if (memory instanceof MemoryPoolable) {
            ((MemoryPoolable) memory).recycle();
        }
        
        addToFreeList(memory);
        releases.incrementAndGet();
    }
    
    @Override
    public int capacity() {
        return capacity;
    }
    
    @Override
    public int available() {
        return available.get();
    }
    
    @Override
    public long getDefaultHeadroom() {
        return defaultHeadroom;
    }
    
    /**
     * Returns pool statistics as a string.
     * 
     * @return statistics string
     */
    public String getStatistics() {
        return String.format("%s[capacity=%d, available=%d, allocations=%d, releases=%d, exhaustions=%d]",
            name, capacity, available.get(), allocations.get(), releases.get(), exhaustions.get());
    }
}
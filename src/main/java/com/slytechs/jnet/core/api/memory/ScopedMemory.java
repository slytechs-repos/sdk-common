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

import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Scoped memory implementation with pool-controlled lifecycle and backend binding.
 * 
 * <p>
 * ScopedMemory wraps native memory segments (such as DPDK mbufs or Napatech buffers)
 * with a scope-based lifecycle. Unlike FixedMemory, ScopedMemory can be rebound to
 * different native segments, making it ideal for packet processing pipelines where
 * memory is constantly arriving from hardware.
 * </p>
 * 
 * <h2>Characteristics</h2>
 * <ul>
 * <li><strong>Rebindable:</strong> Can be bound to different segments during its lifetime</li>
 * <li><strong>Scope tracking:</strong> Uses scope IDs to detect stale references</li>
 * <li><strong>Backend-aware:</strong> Designed for integration with native libraries</li>
 * <li><strong>Pool-managed:</strong> Allocated from and returned to specialized pools</li>
 * </ul>
 * 
 * <h2>Scope-Based Staleness Detection</h2>
 * <p>
 * Each binding generates a new scope ID, allowing headers and other views to
 * detect when their underlying memory has been rebound:
 * </p>
 * <pre>{@code
 * long originalScope = scoped.scopeId();
 * // ... later ...
 * if (scoped.scopeId() != originalScope) {
 *     // Memory has been rebound, view is stale
 * }
 * }</pre>
 * 
 * <h2>Backend Integration</h2>
 * <p>
 * Subclasses like MBufMemory override methods to integrate with native memory
 * management:
 * </p>
 * <ul>
 * <li>Override incrementRef()/decrementRef() for native refcounting</li>
 * <li>Override onRefCountZero() for native memory release</li>
 * <li>Track native handles alongside Java segments</li>
 * </ul>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 */
public class ScopedMemory extends AbstractMemory {
    
    /** Global scope ID counter for staleness detection */
    private static final AtomicLong SCOPE_COUNTER = new AtomicLong();
    
    /** The current bound segment (mutable) */
    protected MemorySegment segment;
    
    /** The original native segment for backend operations */
    protected MemorySegment originalSegment;
    
    /** Current scope ID for stale detection */
    protected volatile long scopeId;
    
    /** The owning pool */
    @SuppressWarnings("rawtypes")
    protected ScopedMemoryPool pool;
    
    /** Whether this memory is currently pinned */
    protected boolean isPinned;
    
    /**
     * Constructs an unbound ScopedMemory.
     * 
     * <p>
     * The memory starts in an unbound state and must be bound to a
     * segment before use.
     * </p>
     */
    public ScopedMemory() {
        // Start unbound
    }
    
    /**
     * Constructs a ScopedMemory with pool reference.
     * 
     * <p>
     * Used by pools to create instances that will automatically return
     * to the pool when their reference count reaches zero.
     * </p>
     * 
     * @param pool the owning pool
     */
    @SuppressWarnings("rawtypes")
    public ScopedMemory(ScopedMemoryPool pool) {
        this.pool = pool;
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>
     * Returns the currently bound segment. Throws an exception if
     * the memory is not bound.
     * </p>
     * 
     * @throws IllegalStateException if not bound to a segment
     */
    @Override
    public MemorySegment segment() {
        if (segment == null) {
            throw new IllegalStateException("ScopedMemory not bound");
        }
        return segment;
    }
    
    /**
     * Returns the current scope ID.
     * 
     * <p>
     * The scope ID changes each time the memory is bound to a new segment,
     * allowing detection of stale references. Views can compare their cached
     * scope ID with the current value to determine if rebinding occurred.
     * </p>
     * 
     * @return the current scope ID, or 0 if unbound
     */
    public long scopeId() {
        return scopeId;
    }
    
    /**
     * Binds this memory to a native segment.
     * 
     * <p>
     * Package-private method used by pools to bind memory to incoming
     * native segments. Generates a new scope ID for staleness detection.
     * </p>
     * 
     * @param segment the memory segment to bind
     * @param offset the offset within segment
     * @param length the length to bind
     */
    void bind(MemorySegment segment, long offset, long length) {
        this.originalSegment = segment;
        this.segment = segment;
        this.scopeId = SCOPE_COUNTER.incrementAndGet();
        this.isPinned = false;
        
        setSegmentBounds(offset, length);
        onBind();
    }
    
    /**
     * Unbinds this memory from its current segment.
     * 
     * <p>
     * Package-private method used by pools to prepare memory for reuse.
     * Clears all segment references and resets scope ID.
     * </p>
     */
    void unbind() {
        onUnbind();
        
        this.segment = null;
        this.originalSegment = null;
        this.scopeId = 0;
        this.isPinned = false;
        this.segmentOffset = 0;
        this.segmentSize = 0;
        this.dataStart = 0;
        this.dataEnd = 0;
        updateView();
    }
    
    /**
     * Checks if this memory is currently bound.
     * 
     * @return true if bound to a segment
     */
    public boolean isBound() {
        return segment != null;
    }
    
    /**
     * Pins this memory to prevent automatic release.
     * 
     * <p>
     * Pinning increments the reference count to prevent the memory from
     * being returned to the pool even when other references are released.
     * Useful for keeping memory alive during asynchronous operations.
     * </p>
     */
    public void pin() {
        if (!isPinned) {
            incrementRef();
            isPinned = true;
        }
    }
    
    /**
     * Unpins this memory.
     * 
     * <p>
     * Decrements the reference count added by pinning. The memory may
     * be released if this was the last reference.
     * </p>
     */
    public void unpin() {
        if (isPinned) {
            decrementRef();
            isPinned = false;
        }
    }
    
    /**
     * Creates a copy of this memory's content.
     * 
     * <p>
     * Allocates a new FixedMemory containing a copy of the active data.
     * Useful for creating persistent copies of transient packet data.
     * </p>
     * 
     * @return a new FixedMemory containing a copy of the data
     */
    public FixedMemory duplicate() {
        long dataLength = length();
        MemorySegment copySegment = MemorySegment.ofArray(new byte[(int)dataLength]);
        
        // Copy active data
        MemorySegment.copy(
            segment, start(),
            copySegment, 0,
            dataLength
        );
        
        return new FixedMemory(copySegment);
    }
    
    /**
     * Called after binding completes.
     * 
     * <p>
     * Subclasses can override to perform additional initialization
     * after a new segment is bound.
     * </p>
     */
    protected void onBind() {
        // Override in subclasses
    }
    
    /**
     * Called before unbinding.
     * 
     * <p>
     * Subclasses can override to perform cleanup before the segment
     * reference is cleared.
     * </p>
     */
    protected void onUnbind() {
        // Override in subclasses
    }
    
    /**
     * Sets the owning pool.
     * 
     * @param pool the pool that owns this memory
     */
    @SuppressWarnings("rawtypes")
    public void setPool(ScopedMemoryPool pool) {
        this.pool = pool;
    }
    
    /**
     * Returns the owning pool.
     * 
     * @return the pool that owns this memory
     */
    @SuppressWarnings("rawtypes")
    public ScopedMemoryPool getPool() {
        return pool;
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>
     * Prepares the memory for reuse by unbinding from current segment.
     * </p>
     */
    @Override
    public void recycle() {
        unbind();
        super.recycle();
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>
     * Returns this memory to its pool when reference count reaches zero.
     * Handles unpinning if necessary.
     * </p>
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void onRefCountZero() {
        super.onRefCountZero();
        if (isPinned) {
            unpin();
        }
        if (pool != null) {
            pool.release(this);
        }
    }
    
    /**
     * Creates a string representation of this memory.
     * 
     * @return a string describing this scoped memory
     */
    @Override
    public String toString() {
        if (!isBound()) {
            return "ScopedMemory[unbound]";
        }
        return String.format("ScopedMemory[scopeId=%d, offset=%d, size=%d, start=%d, end=%d, refCount=%d]",
            scopeId(), byteOffset(), byteSize(), start(), end(), refCount());
    }
}
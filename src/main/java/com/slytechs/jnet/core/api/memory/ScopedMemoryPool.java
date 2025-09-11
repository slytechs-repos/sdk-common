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

/**
 * Pool for ScopedMemory objects that wrap native memory segments.
 * 
 * <p>
 * ScopedMemoryPool manages a pool of reusable ScopedMemory wrapper objects that
 * can be bound to native memory segments from backends like DPDK or Napatech.
 * Unlike FixedMemoryPool which pre-allocates the actual memory, ScopedMemoryPool
 * only pre-allocates wrapper objects that are bound to external native memory
 * on demand.
 * </p>
 * 
 * <h2>Key Characteristics</h2>
 * <ul>
 * <li><strong>Pre-allocated wrappers:</strong> Pool contains wrapper objects, not memory</li>
 * <li><strong>External binding:</strong> Wrappers bind to native segments from backends</li>
 * <li><strong>Scope tracking:</strong> Each binding generates a unique scope ID</li>
 * <li><strong>Automatic unbinding:</strong> Memory unbinds when returned to pool</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * 
 * <h3>Basic Allocation and Binding</h3>
 * <pre>{@code
 * ScopedMemoryPool<ScopedMemory> pool = 
 *     new ScopedMemoryPool<>("packet-pool", ScopedMemory.class, 100, 128L);
 * 
 * // Allocate and bind in one step
 * MemorySegment nativeSegment = getNativeSegment(); // From DPDK/Napatech
 * ScopedMemory memory = pool.allocate(nativeSegment, 0, 1024);
 * 
 * // Use the memory
 * memory.segment().set(ValueLayout.JAVA_INT, memory.start(), value);
 * 
 * // Return to pool (automatically unbinds)
 * memory.decrementRef();
 * }</pre>
 * 
 * <h3>Deferred Binding</h3>
 * <pre>{@code
 * // Allocate wrapper without binding
 * ScopedMemory memory = pool.allocate();
 * 
 * // Bind later when native memory arrives
 * MemorySegment segment = backend.receivePacket();
 * memory.bind(segment, 0, segment.byteSize());
 * 
 * // Process and release
 * processPacket(memory);
 * memory.decrementRef();
 * }</pre>
 * 
 * <h3>Custom Factory for Specialized Types</h3>
 * <pre>{@code
 * class MBufMemory extends ScopedMemory {
 *     private long mbufHandle;
 *     
 *     @Override
 *     protected void onBind() {
 *         // Extract mbuf handle from segment
 *     }
 * }
 * 
 * ScopedMemoryPool<MBufMemory> mbufPool = 
 *     new ScopedMemoryPool<>("mbuf-pool", MBufMemory::new, 1000, 256L);
 * }</pre>
 * 
 * <h2>Thread Safety</h2>
 * <p>
 * The pool itself is thread-safe for allocation and release operations.
 * Individual ScopedMemory instances are not thread-safe and should not
 * be shared between threads without external synchronization.
 * </p>
 * 
 * @param <T> the type of ScopedMemory managed by this pool
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 */
public class ScopedMemoryPool<T extends ScopedMemory> extends AbstractMemoryPool<T> {
    
    /**
     * Factory interface for creating ScopedMemory instances.
     * 
     * @param <T> the type of ScopedMemory to create
     */
    public interface Factory<T extends ScopedMemory> {
        /**
         * Creates a new instance of ScopedMemory.
         * 
         * @return a new ScopedMemory instance
         */
        T newInstance();
    }
    
    /** The class type for reflection-based instantiation */
    private final Class<T> type;
    
    /** The factory for creating instances */
    private final Factory<T> factory;
    
    /**
     * Creates a pool using class-based instantiation.
     * 
     * <p>
     * This constructor uses reflection to create instances of the specified
     * class type. The class must have a no-argument constructor.
     * </p>
     * 
     * @param name the pool name for metrics
     * @param type the class of ScopedMemory to pool
     * @param capacity the number of wrapper objects to pre-allocate
     * @param defaultHeadroom default headroom for memory operations
     * @throws RuntimeException if instances cannot be created
     */
    public ScopedMemoryPool(String name, Class<T> type, int capacity, long defaultHeadroom) {
        super(name, capacity, defaultHeadroom);
        this.type = type;
        this.factory = null;
        
        initializePool();
    }
    
    /**
     * Creates a pool using factory-based instantiation.
     * 
     * <p>
     * This constructor uses a factory function to create instances, allowing
     * for more complex initialization or use of lambda expressions.
     * </p>
     * 
     * @param name the pool name for metrics
     * @param factory factory function for creating instances
     * @param capacity the number of wrapper objects to pre-allocate
     * @param defaultHeadroom default headroom for memory operations
     */
    public ScopedMemoryPool(String name, Factory<T> factory, int capacity, long defaultHeadroom) {
        super(name, capacity, defaultHeadroom);
        this.type = null;
        this.factory = factory;
        
        initializePool();
    }
    
    /**
     * Initializes the pool by pre-allocating wrapper objects.
     * 
     * <p>
     * Creates the specified number of ScopedMemory wrapper objects and
     * adds them to the free list. The wrappers are not bound to any
     * native memory at this point.
     * </p>
     * 
     * @throws RuntimeException if instance creation fails
     */
    @SuppressWarnings("unchecked")
    private void initializePool() {
        for (int i = 0; i < capacity; i++) {
            T instance;
            try {
                if (factory != null) {
                    instance = factory.newInstance();
                } else {
                    instance = type.getDeclaredConstructor().newInstance();
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to create pool instance", e);
            }
            
            instance.setPool(this);
            instance.refCount.set(0); // Start at 0 for pooling
            addToFreeList(instance);
        }
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>
     * Allocates an unbound ScopedMemory wrapper. The wrapper must be
     * bound to a native segment before use. This method is useful when
     * the native memory will be available later.
     * </p>
     * 
     * @return an unbound ScopedMemory wrapper, or null if pool is exhausted
     */
    @Override
    public T allocate() {
        T memory = allocateFromFreeList();
        if (memory != null) {
            metrics.recordAllocation();
        } else {
            metrics.recordExhaustion();
        }
        return memory;
    }
    
    /**
     * Allocates and binds a ScopedMemory to a native segment.
     * 
     * <p>
     * This is the preferred allocation method when the native memory
     * segment is already available. The wrapper is allocated from the
     * pool and immediately bound to the provided segment.
     * </p>
     * 
     * @param segment the native memory segment to bind
     * @param offset offset within the segment
     * @param length length of the memory region
     * @return a bound ScopedMemory, or null if pool is exhausted
     * @throws IllegalArgumentException if offset + length exceeds segment size
     */
    public T allocate(MemorySegment segment, long offset, long length) {
        if (segment == null) {
            throw new NullPointerException("Segment cannot be null");
        }
        if (offset < 0 || length < 0 || offset + length > segment.byteSize()) {
            throw new IllegalArgumentException(
                String.format("Invalid bounds: offset=%d, length=%d, segment.byteSize=%d",
                             offset, length, segment.byteSize()));
        }
        
        T memory = allocate();
        if (memory != null) {
            memory.bind(segment, offset, length);
        }
        return memory;
    }
    
    /**
     * Allocates and binds a ScopedMemory to an entire native segment.
     * 
     * <p>
     * Convenience method that binds to the entire segment starting at offset 0.
     * </p>
     * 
     * @param segment the native memory segment to bind
     * @return a bound ScopedMemory, or null if pool is exhausted
     */
    public T allocate(MemorySegment segment) {
        return allocate(segment, 0, segment.byteSize());
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>
     * Additionally ensures that the memory is unbound before being
     * returned to the pool's free list.
     * </p>
     */
    @Override
    protected void prepareForRelease(T item) {
        super.prepareForRelease(item);
        // Ensure memory is unbound when returning to pool
        if (item.isBound()) {
            item.unbind();
        }
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>
     * Prepares the allocated wrapper for use. If the wrapper was
     * previously bound, it will be in an unbound state after allocation.
     * </p>
     */
    @Override
    protected void prepareForAllocation(T item) {
        super.prepareForAllocation(item);
        // Wrapper starts unbound - will be bound by user
    }
    
    /**
     * Returns the type of ScopedMemory managed by this pool.
     * 
     * @return the class type, or null if using factory-based creation
     */
    public Class<T> getType() {
        return type;
    }
    
    /**
     * Creates a string representation of this pool.
     * 
     * @return a string describing the pool state
     */
    @Override
    public String toString() {
        return String.format("ScopedMemoryPool[name=%s, capacity=%d, available=%d, type=%s]",
                           name, capacity, available(),
                           type != null ? type.getSimpleName() : "factory-based");
    }
}
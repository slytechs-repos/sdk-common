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
 * ScopedMemoryPool manages a pool of reusable ScopedMemory objects that can
 * be bound to native memory segments from backends like DPDK or Napatech.
 * The pool pre-allocates wrapper objects but not the underlying memory.
 * </p>
 * 
 * <h2>Characteristics</h2>
 * <ul>
 * <li>Pre-allocated wrapper objects</li>
 * <li>Binds to external native memory</li>
 * <li>Scope-based lifecycle tracking</li>
 * <li>Supports backend-specific subclasses</li>
 * </ul>
 * 
 * @param <T> the type of ScopedMemory
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 */
public class ScopedMemoryPool<T extends ScopedMemory> extends AbstractMemoryPool<T> {
    
    /** Factory for creating instances */
    public interface Factory<T extends ScopedMemory> {
        T newInstance();
    }
    
    private final Class<T> type;
    private final Factory<T> factory;
    
    /**
     * Creates a pool using class-based instantiation.
     * 
     * @param name pool name
     * @param type the class of objects to pool
     * @param capacity pool capacity
     * @param defaultHeadroom default headroom
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
     * @param name pool name
     * @param factory factory for creating instances
     * @param capacity pool capacity
     * @param defaultHeadroom default headroom
     */
    public ScopedMemoryPool(String name, Factory<T> factory, int capacity, long defaultHeadroom) {
        super(name, capacity, defaultHeadroom);
        this.type = null;
        this.factory = factory;
        
        initializePool();
    }
    
    /**
     * Initializes the pool by pre-allocating wrapper objects.
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
            addToFreeList(instance);
        }
    }
    
    /**
     * Allocates an unbound ScopedMemory.
     * 
     * <p>
     * For ScopedMemory, allocation typically requires immediate binding
     * to a native segment. Use {@link #allocate(MemorySegment, long, long)}
     * instead.
     * </p>
     * 
     * @return unbound ScopedMemory
     * @throws UnsupportedOperationException for normal use
     */
    @Override
    public T allocate() {
        return allocateFromFreeList();
    }
    
    /**
     * Allocates and binds a ScopedMemory to a native segment.
     * 
     * @param segment the native memory segment
     * @param offset offset within segment
     * @param length length of data
     * @return bound ScopedMemory or null if exhausted
     */
    public T allocate(MemorySegment segment, long offset, long length) {
        T memory = allocateFromFreeList();
        if (memory == null) {
            return null;
        }
        
        memory.bind(segment, offset, length);
        return memory;
    }
    
    /**
     * Allocates and binds a ScopedMemory to a native segment.
     * 
     * @param segment the native memory segment
     * @return bound ScopedMemory or null if exhausted
     */
    public T allocate(MemorySegment segment) {
        return allocate(segment, 0, segment.byteSize());
    }
}
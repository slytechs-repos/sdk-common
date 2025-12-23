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

/**
 * Generic interface for memory pools.
 * 
 * @param <T> the type of memory managed by this pool
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public interface MemoryPool<T> {
    
    /**
     * Allocates a memory object from the pool.
     * 
     * @return allocated memory or null if pool exhausted
     */
    T allocate();
    
    default T allocate(long size) {
    	return allocate();
    }
    
    /**
     * Releases a memory object back to the pool.
     * 
     * @param memory the memory to release
     */
    void release(T memory);
    
    /**
     * Returns the pool capacity.
     * 
     * @return total number of objects in pool
     */
    long capacity();
    
    /**
     * Returns the number of available objects.
     * 
     * @return available count
     */
    long available();
    
    /**
     * Returns the default headroom for new allocations.
     * 
     * @return headroom in bytes
     */
    default long getDefaultHeadroom() {
        return 128;
    }
}
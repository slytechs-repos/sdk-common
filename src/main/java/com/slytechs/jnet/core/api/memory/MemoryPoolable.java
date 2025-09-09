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

/**
 * Interface for objects that can be managed by memory pools.
 * 
 * <p>
 * MemoryPoolable provides a contract for objects that can be pooled and reused.
 * This includes memory objects (FixedMemory, ScopedMemory), view objects
 * (Packet, Header), and buffers (MemoryBuffer). The interface ensures objects
 * can be reset to a clean state and properly managed by their owning pools.
 * </p>
 * 
 * <h2>Naming Clarification</h2>
 * <p>
 * The method {@code recycle()} is specifically for pool lifecycle management,
 * distinct from domain-specific reset operations. For example, MemoryBuffer has:
 * </p>
 * <ul>
 * <li>{@code reset()} - Resets buffer position to mark (ByteBuffer semantics)</li>
 * <li>{@code recycle()} - Prepares buffer for pool reuse (MemoryPoolable semantics)</li>
 * </ul>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 */
public interface MemoryPoolable {
    
    /**
     * Prepares this object for return to pool and reuse.
     * 
     * <p>
     * Clears all state to prepare the object for the next allocation.
     * This method should:
     * </p>
     * <ul>
     * <li>Clear any references to prevent memory leaks</li>
     * <li>Reset counters and flags to defaults</li>
     * <li>Unbind from any memory if applicable</li>
     * <li>NOT modify the pool reference itself</li>
     * </ul>
     */
    void recycle();
    
    /**
     * Sets the owning pool for this object.
     * 
     * <p>
     * Called by the pool during object creation or initialization.
     * The pool reference enables automatic return when appropriate
     * (e.g., refcount reaches zero for Memory objects).
     * </p>
     * 
     * @param pool the owning pool
     */
    void setPool(MemoryPool<?> pool);
    
    /**
     * Returns the pool that owns this object.
     * 
     * @return the owning pool, or null if not pooled
     */
    MemoryPool<?> getPool();
}
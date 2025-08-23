/*
 * Sly Technologies Free License
 * 
 * Copyright 2024 Sly Technologies Inc.
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
 * Interface for memory objects that can be managed by a MemoryPool.
 * 
 * <p>
 * This marker interface identifies memory objects that support pool management,
 * enabling automatic return to their originating pool when reference counts
 * reach zero. Implementations must maintain a reference to their owning pool to
 * enable proper pool integration.
 * </p>
 * 
 * <h2>Implementation Requirements</h2>
 * <ul>
 * <li>Store reference to owning pool during construction</li>
 * <li>Return correct pool reference from {@link #getOwningPool()}</li>
 * <li>Support pool-managed lifecycle through reference counting</li>
 * </ul>
 * 
 * @see MemoryPool for pool management
 * @see MemoryBufferView for example implementation
 */
public interface MemoryPoolable {
    /**
     * Returns the MemoryPool that owns this memory object.
     * 
     * <p>
     * This method enables the memory object to identify its originating pool, which
     * is essential for automatic return when reference counting reaches zero. The
     * returned pool must be the same instance that created this memory object.
     * </p>
     * 
     * @return the owning MemoryPool, or {@code null} if this memory is not pooled
     */
    MemoryPool<?> getOwningPool();
}
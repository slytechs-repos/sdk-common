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
package com.slytechs.sdk.common.memory.pool;

import java.lang.foreign.SegmentAllocator;

/**
 * Factory for creating poolable objects with optional memory allocation support.
 * 
 * <p>
 * PoolableFactory extends the simple {@code Supplier<T>} pattern to support objects
 * that need memory segments allocated during construction. The factory receives a
 * {@link SegmentAllocator} (typically backed by a {@code SlabAllocator}) that can
 * be used to allocate memory for the object's components.
 * </p>
 * 
 * <h2>Usage Examples</h2>
 * 
 * <h3>Simple Objects (no memory needed)</h3>
 * <pre>{@code
 * // Use Supplier directly - allocator ignored
 * LockFreePool<MyObject> pool = new LockFreePool<>(settings, MyObject::new);
 * }</pre>
 * 
 * <h3>Objects with Memory Components</h3>
 * <pre>{@code
 * // Use PoolableFactory to allocate memory during creation
 * LockFreePool<Packet> pool = new LockFreePool<>(settings, allocator -> {
 *     MemorySegment data = allocator.allocate(9000, 8);
 *     MemorySegment desc = allocator.allocate(128, 8);
 *     return Packet.ofFixed(DescriptorType.NET, data, desc);
 * });
 * }</pre>
 * 
 * <h2>Memory Lifecycle</h2>
 * 
 * <p>
 * Memory allocated via the provided {@code SegmentAllocator} is managed by the pool's
 * slab infrastructure. When pool entries are evicted during contraction, the slab
 * memory is automatically freed. Objects should bind to the allocated segments
 * permanently (for Fixed memory patterns) - no rebinding occurs in hot paths.
 * </p>
 *
 * @param <T> the type of poolable objects created by this factory
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see LockFreePool
 * @see SlabAllocator
 */
@FunctionalInterface
public interface PoolableFactory<T extends Poolable> {

    /**
     * Creates a new instance of the poolable object.
     * 
     * <p>
     * The provided allocator can be used to allocate memory segments for the
     * object's components. The allocator is typically backed by a slab allocator
     * that provides efficient bulk memory allocation.
     * </p>
     *
     * @param allocator segment allocator for memory allocation, never null
     * @return a new poolable instance
     */
    T create(SlabAllocator allocator);

    /**
     * Creates a PoolableFactory from a simple Supplier.
     * 
     * <p>
     * Convenience method for objects that don't need memory allocation.
     * The allocator parameter is ignored.
     * </p>
     *
     * @param <T> the poolable type
     * @param supplier the simple factory
     * @return a PoolableFactory wrapping the supplier
     */
    static <T extends Poolable> PoolableFactory<T> of(java.util.function.Supplier<T> supplier) {
        return _ -> supplier.get();
    }
}
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
 * Interface providing reference counting and lifecycle management for memory
 * objects.
 * 
 * <p>
 * MemoryRef implements a reference counting mechanism that enables safe,
 * deterministic memory management in multi-threaded environments. This pattern
 * is essential for memory pooling, resource sharing, and preventing memory
 * leaks in high-performance applications such as network packet processing and
 * streaming data systems.
 * </p>
 * 
 * <h2>Reference Counting Lifecycle</h2>
 * <p>
 * Memory objects follow a strict reference counting protocol:
 * </p>
 * <ol>
 * <li><strong>Creation:</strong> New memory starts with reference count =
 * 1</li>
 * <li><strong>Sharing:</strong> Each additional reference calls
 * {@link #incrementRef()}</li>
 * <li><strong>Release:</strong> Each reference release calls
 * {@link #decrementRef()}</li>
 * <li><strong>Cleanup:</strong> When count reaches 0, memory is automatically
 * closed</li>
 * </ol>
 * 
 * <h2>Usage Patterns</h2>
 * 
 * <pre>{@code
 * // Safe sharing pattern
 * Memory shared = original.incrementRef(); // Now ref count = 2
 * try {
 * 	// Use shared reference
 * 	processMemory(shared);
 * } finally {
 * 	shared.decrementRef(); // Back to ref count = 1
 * }
 * 
 * // Pool return pattern
 * Memory pooled = pool.allocate(); // ref count = 1
 * try {
 * 	// Use pooled memory
 * 	fillBuffer(pooled);
 * } finally {
 * 	pooled.decrementRef(); // ref count = 0, returns to pool
 * }
 * }</pre>
 * 
 * <h2>Thread Safety</h2>
 * <p>
 * Reference counting operations are atomic and thread-safe, enabling safe
 * sharing of memory objects across multiple threads without external
 * synchronization.
 * </p>
 * 
 * <h2>Best Practices</h2>
 * <ul>
 * <li><strong>Always pair increment/decrement:</strong> Every incrementRef()
 * must have a corresponding decrementRef()</li>
 * <li><strong>Use try-finally blocks:</strong> Ensure cleanup occurs even
 * during exceptions</li>
 * <li><strong>Check refcount before operations:</strong> Avoid operations on
 * closed memory</li>
 * <li><strong>Document ownership:</strong> Clearly specify who owns references
 * in APIs</li>
 * </ul>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see MemoryView for content access
 * @see MemoryWindow for bounds management
 * @since 1.0
 */
public interface MemoryRef {

    /**
     * Returns the current reference count of this memory object.
     * 
     * <p>The reference count indicates how many active references exist to this memory.
     * A count of 0 indicates the memory has been closed and is no longer usable.
     * This method is primarily useful for debugging and monitoring memory usage patterns.</p>
     * 
     * <p><strong>Thread Safety:</strong> This method provides a snapshot of the reference
     * count at the time of the call. The actual count may change immediately after
     * this method returns due to concurrent access from other threads.</p>
     * 
     * @return the current reference count (≥ 0)
     * @throws IllegalStateException if the memory object is in an invalid state
     * 
     * @see #incrementRef() to increase the count
     * @see #decrementRef() to decrease the count
     */
    int refCount();

    /**
     * Increments the reference count and returns the new count value.
     * 
     * <p>This method must be called whenever creating an additional reference to this
     * memory object. It ensures the memory remains valid as long as references exist.
     * The operation is atomic and thread-safe.</p>
     * 
     * <p><strong>Best Practice:</strong> Always pair increment calls with corresponding
     * decrement calls, preferably using try-finally blocks to ensure cleanup occurs
     * even in exceptional situations.</p>
     * 
     * <pre>{@code
     * Memory additional = memory.incrementRef();
     * try {
     *     // Use additional reference
     *     return processMemory(additional);
     * } finally {
     *     additional.decrementRef(); // Always cleanup
     * }
     * }</pre>
     * 
     * @return the new reference count after increment (≥ 1)
     * @throws IllegalStateException if this memory is already closed (refcount = 0)
     * 
     * @see #decrementRef() for the corresponding decrement operation
     * @see #refCount() to check current count
     */
    int incrementRef();

    /**
     * Decrements the reference count and returns the new count value.
     * 
     * <p>This method must be called when releasing a reference to this memory object.
     * If the reference count reaches 0, the memory is automatically closed and becomes
     * unusable. The operation is atomic and thread-safe.</p>
     * 
     * <p><strong>Automatic Cleanup:</strong> When the count reaches 0:</p>
     * <ul>
     *   <li>{@link #close()} is automatically called</li>
     *   <li>Memory is released back to pools if applicable</li>
     *   <li>All subsequent operations throw IllegalStateException</li>
     * </ul>
     * 
     * @return the new reference count after decrement (≥ 0)
     * @throws IllegalStateException if refcount is already 0 (underflow protection)
     * 
     * @see #incrementRef() for the corresponding increment operation
     * @see #close() for explicit cleanup
     */
    int decrementRef();
}
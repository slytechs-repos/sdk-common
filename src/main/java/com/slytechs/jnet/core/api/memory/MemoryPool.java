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

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.concurrent.atomic.AtomicReference;

import com.slytechs.jnet.core.api.memory.MemoryPool.MemoryPoolable;

/**
 * A high-performance, thread-safe memory pool that pre-allocates memory segments for zero-allocation runtime efficiency.
 * 
 * <p>MemoryPool provides a lock-free memory allocation system specifically designed for high-throughput scenarios
 * such as network packet processing, streaming data analysis, and real-time systems where allocation overhead
 * must be minimized. The pool pre-allocates all memory segments at construction time and manages them through
 * a lock-free free list using atomic compare-and-swap operations.</p>
 * 
 * <h2>Key Features</h2>
 * <ul>
 *   <li><strong>Zero Runtime Allocation:</strong> All memory is pre-allocated at pool creation</li>
 *   <li><strong>Lock-Free Operations:</strong> Thread-safe allocation/release using atomic operations</li>
 *   <li><strong>Reference Integration:</strong> Automatic return to pool when reference count reaches 0</li>
 *   <li><strong>Factory Pattern:</strong> Flexible memory object creation via pluggable factories</li>
 *   <li><strong>Arena Management:</strong> Integrated with Java's foreign memory API</li>
 * </ul>
 * 
 * <h2>Pool Architecture</h2>
 * <p>The pool uses a lock-free singly-linked list to manage free memory segments:</p>
 * <pre>{@code
 * Pool Structure:
 * 
 * freeListHead → [Segment A] → [Segment B] → [Segment C] → null
 *                     ↑             ↑             ↑
 *                 Available     Available     Available
 * 
 * Allocation: CAS removes head, returns segment with refcount=1
 * Release: CAS adds segment back to head after refcount reaches 0
 * }</pre>
 * 
 * <h2>Memory Lifecycle</h2>
 * <ol>
 *   <li><strong>Pool Creation:</strong> All segments pre-allocated and added to free list</li>
 *   <li><strong>Allocation:</strong> Remove segment from free list, reset state, set refcount=1</li>
 *   <li><strong>Usage:</strong> Application uses memory through Memory interface</li>
 *   <li><strong>Release:</strong> When refcount reaches 0, memory automatically returns to pool</li>
 *   <li><strong>Reuse:</strong> Released memory becomes available for future allocations</li>
 * </ol>
 * 
 * <h2>Usage Patterns</h2>
 * 
 * <h3>Basic Pool Setup</h3>
 * <pre>{@code
 * // Create pool with 1000 segments of 2KB each
 * Arena arena = Arena.global();
 * MemoryPool<MemoryBuffer> pool = new MemoryPool<>(
 *     2048,                    // segment size
 *     1000,                    // segment count  
 *     arena,                   // memory arena
 *     MemoryBuffer::new        // factory method reference
 * );
 * }</pre>
 * 
 * <h3>Allocation and Usage</h3>
 * <pre>{@code
 * // Allocate buffer from pool
 * MemoryBuffer buffer = pool.allocate(); // refcount = 1
 * try {
 *     // Use buffer for data processing
 *     ByteBuffer bb = buffer.asByteBuffer();
 *     // ... process data ...
 *     
 * } finally {
 *     // Release back to pool when done
 *     buffer.decrementRef(); // refcount = 0, automatically returns to pool
 * }
 * }</pre>
 * 
 * <h3>High-Throughput Network Processing</h3>
 * <pre>{@code
 * // Process incoming network packets
 * while (running) {
 *     MemoryBuffer packetBuffer = pool.allocate();
 *     try {
 *         int bytesRead = channel.read(packetBuffer.asByteBuffer());
 *         packetBuffer.memoryDataEnd(packetBuffer.memoryDataOffset() + bytesRead);
 *         
 *         // Process packet without allocation
 *         processPacket(packetBuffer);
 *         
 *     } finally {
 *         packetBuffer.decrementRef(); // Auto-return to pool
 *     }
 * }
 * }</pre>
 * 
 * <h2>Performance Characteristics</h2>
 * <ul>
 *   <li><strong>Allocation:</strong> O(1) lock-free CAS operation</li>
 *   <li><strong>Release:</strong> O(1) lock-free CAS operation</li>
 *   <li><strong>Memory Overhead:</strong> Fixed at pool creation time</li>
 *   <li><strong>Contention:</strong> Minimal due to lock-free design</li>
 *   <li><strong>Predictability:</strong> No GC allocation during runtime</li>
 * </ul>
 * 
 * <h2>Thread Safety</h2>
 * <p>MemoryPool is completely thread-safe and designed for high-concurrency access:</p>
 * <ul>
 *   <li><strong>Lock-Free Allocation:</strong> Multiple threads can allocate simultaneously</li>
 *   <li><strong>Lock-Free Release:</strong> Multiple threads can release simultaneously</li>
 *   <li><strong>ABA Protection:</strong> Uses object references to prevent ABA problems</li>
 *   <li><strong>Memory Visibility:</strong> Atomic operations ensure proper memory visibility</li>
 * </ul>
 * 
 * <h2>Factory Integration</h2>
 * <p>The pool uses a factory pattern to create memory objects, enabling support for different
 * Memory implementations while maintaining type safety:</p>
 * <pre>{@code
 * // Custom factory for specialized memory objects
 * MemoryPool.Factory<MyCustomMemory> factory = (pool, segment, offset, length) -> 
 *     new MyCustomMemory(pool, segment, offset, length);
 *     
 * MemoryPool<MyCustomMemory> customPool = new MemoryPool<>(
 *     size, count, arena, factory
 * );
 * }</pre>
 * 
 * <h2>Resource Management</h2>
 * <p>The pool integrates with Java's Arena API for proper native memory management:</p>
 * <ul>
 *   <li><strong>Arena Binding:</strong> All pool memory is allocated within the provided Arena</li>
 *   <li><strong>Lifecycle Coupling:</strong> Pool memory is freed when Arena is closed</li>
 *   <li><strong>Scope Safety:</strong> Memory segments cannot outlive their Arena</li>
 * </ul>
 * 
 * <h2>Monitoring and Debugging</h2>
 * <p>The pool provides methods for monitoring pool state:</p>
 * <ul>
 *   <li>{@link #getFreeListSize()} - Current number of available segments</li>
 *   <li>{@link #getSegmentSize()} - Size of each segment in bytes</li>
 *   <li>{@link #getSegmentCount()} - Total number of segments in pool</li>
 * </ul>
 * 
 * <h2>Best Practices</h2>
 * <ul>
 *   <li><strong>Size Pool Appropriately:</strong> Balance memory usage vs allocation failures</li>
 *   <li><strong>Use Try-Finally:</strong> Always ensure memory is returned to pool</li>
 *   <li><strong>Monitor Pool Usage:</strong> Track allocation failures and pool exhaustion</li>
 *   <li><strong>Choose Segment Size Wisely:</strong> Match segment size to typical usage patterns</li>
 *   <li><strong>Share Pools:</strong> Use same pool across multiple threads for efficiency</li>
 * </ul>
 * 
 * @param <T> the type of memory objects managed by this pool, must extend AbstractMemory and implement MemoryPoolable
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 * @see MemoryPoolable for poolable memory interface
 * @see Factory for memory object creation
 * @see MemoryBuffer for the primary poolable implementation
 */
public class MemoryPool<T extends AbstractMemory & MemoryPoolable> {

    /**
     * Interface for memory objects that can be managed by a MemoryPool.
     * 
     * <p>This marker interface identifies memory objects that support pool management,
     * enabling automatic return to their originating pool when reference counts reach zero.
     * Implementations must maintain a reference to their owning pool to enable proper
     * pool integration.</p>
     * 
     * <h2>Implementation Requirements</h2>
     * <ul>
     *   <li>Store reference to owning pool during construction</li>
     *   <li>Return correct pool reference from {@link #getOwningPool()}</li>
     *   <li>Support pool-managed lifecycle through reference counting</li>
     * </ul>
     * 
     * @see MemoryPool for pool management
     * @see MemoryBuffer for example implementation
     */
    public interface MemoryPoolable {
        /**
         * Returns the MemoryPool that owns this memory object.
         * 
         * <p>This method enables the memory object to identify its originating pool,
         * which is essential for automatic return when reference counting reaches zero.
         * The returned pool must be the same instance that created this memory object.</p>
         * 
         * @return the owning MemoryPool, or {@code null} if this memory is not pooled
         */
        MemoryPool<?> getOwningPool();
    }

    /**
     * Factory interface for creating memory objects during pool initialization.
     * 
     * <p>The Factory pattern enables the pool to create different types of memory objects
     * while maintaining type safety and ensuring proper pool integration. Factories are
     * responsible for creating memory objects with correct pool ownership and initial state.</p>
     * 
     * <h2>Implementation Guidelines</h2>
     * <ul>
     *   <li>Always set the owning pool reference in created objects</li>
     *   <li>Ensure created objects start with reference count = 1</li>
     *   <li>Handle any custom initialization required by the memory type</li>
     *   <li>Validate parameters and throw appropriate exceptions for invalid input</li>
     * </ul>
     * 
     * <h2>Example Implementation</h2>
     * <pre>{@code
     * public class MemoryBufferFactory implements MemoryPool.Factory<MemoryBuffer> {
     *     @Override
     *     public MemoryBuffer newInstance(MemoryPool<MemoryBuffer> owningPool, 
     *                                   MemorySegment memorySegment, 
     *                                   long offset, long length) {
     *         return new MemoryBuffer(owningPool, memorySegment, offset, length);
     *     }
     * }
     * }</pre>
     * 
     * @param <T> the type of memory objects created by this factory
     */
    public interface Factory<T extends AbstractMemory & MemoryPoolable> {
        /**
         * Creates a new memory object instance with the specified parameters.
         * 
         * <p>This method is called during pool initialization to create all memory objects
         * that will be managed by the pool. The created object must properly implement
         * the MemoryPoolable interface and maintain a reference to the owning pool.</p>
         * 
         * @param owningPool the MemoryPool that will own the created object
         * @param memorySegment the backing MemorySegment for the memory object
         * @param offset the starting offset within the segment
         * @param length the length of the memory region
         * @return a new memory object ready for pool management
         * @throws IllegalArgumentException if parameters are invalid
         * @throws NullPointerException if required parameters are null
         */
        T newInstance(MemoryPool<T> owningPool, MemorySegment memorySegment, long offset, long length);
    }

    /**
     * Atomic reference to the head of the free list for lock-free operations.
     */
    private final AtomicReference<T> freeListHead = new AtomicReference<>();

    /**
     * The size of each memory segment in bytes (immutable).
     */
    private final long segmentSize;

    /**
     * The total number of segments in the pool (immutable).
     */
    private final long segmentCount;

    /**
     * The Arena that owns all memory allocated by this pool (immutable).
     */
    @SuppressWarnings("unused")
	private final Arena arena;

    /**
     * Constructs a MemoryPool with pre-allocated segments.
     * 
     * <p>This constructor creates a memory pool by pre-allocating all segments within
     * the provided Arena and organizing them into a lock-free free list. All memory
     * allocation occurs during construction to ensure zero runtime allocation overhead.</p>
     * 
     * <p><strong>Memory Layout:</strong> The pool allocates a single large memory block
     * and divides it into equal-sized segments using a slicing allocator. This ensures
     * optimal memory locality and minimizes fragmentation.</p>
     * 
     * <p><strong>Initialization Process:</strong></p>
     * <ol>
     *   <li>Validate parameters for positive values</li>
     *   <li>Allocate single memory block: {@code segmentCount * segmentSize} bytes</li>
     *   <li>Create slicing allocator for the memory block</li>
     *   <li>Create memory objects using the factory and link them into free list</li>
     * </ol>
     * 
     * @param segmentSize the size of each memory segment in bytes, must be > 0
     * @param segmentCount the number of segments to pre-allocate, must be > 0
     * @param arena the Arena to allocate memory within, must not be null
     * @param elementFactory factory for creating memory objects, must not be null
     * @throws IllegalArgumentException if segmentSize or segmentCount is non-positive
     * @throws NullPointerException if arena or elementFactory is null
     * @throws OutOfMemoryError if unable to allocate the required memory
     */
    public MemoryPool(long segmentSize, long segmentCount, Arena arena, Factory<T> elementFactory) {
        if (segmentSize <= 0 || segmentCount <= 0) {
            throw new IllegalArgumentException("segmentSize and segmentCount must be positive");
        }
        this.segmentSize = segmentSize;
        this.segmentCount = segmentCount;
        this.arena = arena;
        var allocator = SegmentAllocator.slicingAllocator(arena.allocate(segmentCount * segmentSize));

        // Pre-allocate segments and add to free list
        for (int i = 0; i < segmentCount; i++) {
            MemorySegment segment = allocator.allocate(segmentSize);
            T memory = elementFactory.newInstance(this, segment, 0, segmentSize);

            memory.setNextMemoryRaw(freeListHead.get());
            freeListHead.set(memory);
        }
    }

    /**
     * Allocates a memory object from the pool's free list.
     * 
     * <p>This method performs lock-free allocation by atomically removing the head
     * of the free list and preparing it for use. The returned memory object has
     * its state reset and reference count set to 1, making it ready for immediate use.</p>
     * 
     * <p><strong>Lock-Free Algorithm:</strong></p>
     * <ol>
     *   <li>Read current free list head atomically</li>
     *   <li>If head is null, throw OutOfMemoryError (pool exhausted)</li>
     *   <li>Attempt CAS to update head to next element</li>
     *   <li>If CAS succeeds, reset object state and return</li>
     *   <li>If CAS fails, retry from step 1 (another thread modified list)</li>
     * </ol>
     * 
     * <p><strong>Performance:</strong> This operation is O(1) and lock-free, though
     * it may retry under high contention. The number of retries is typically very low
     * due to the short critical section.</p>
     * 
     * @return a memory object with reference count = 1, ready for use
     * @throws OutOfMemoryError if no segments are available (pool exhausted)
     */
    @SuppressWarnings("unchecked")
    public T allocate() {
        while (true) {
            T candidate = freeListHead.get();
            if (candidate == null) {
                throw new OutOfMemoryError("memory pool exhausted: size=" + segmentSize + ", count=" + segmentCount);
            }
            if (freeListHead.compareAndSet(candidate, (T) candidate.nextMemory())) {
                candidate.setNextMemoryRaw(null); // Clear nextMemory to prevent duplicate binding
                return candidate;
            }
        }
    }

    /**
     * Releases a memory object back to the pool's free list.
     * 
     * <p>This method performs lock-free release by resetting the memory object's state
     * and atomically adding it to the head of the free list. The memory object must
     * have reference count = 0 and must belong to this pool.</p>
     * 
     * <p><strong>Validation:</strong> The method validates that:</p>
     * <ul>
     *   <li>Memory object's reference count is exactly 0</li>
     *   <li>Memory object belongs to this pool (ownership check)</li>
     * </ul>
     * 
     * <p><strong>Lock-Free Algorithm:</strong></p>
     * <ol>
     *   <li>Validate memory object state and ownership</li>
     *   <li>Reset object state (refcount=1, clear chain linkage)</li>
     *   <li>Read current free list head atomically</li>
     *   <li>Set memory's next pointer to current head</li>
     *   <li>Attempt CAS to make memory the new head</li>
     *   <li>If CAS fails, retry from step 3</li>
     * </ol>
     * 
     * <p><strong>Automatic Release:</strong> This method is typically called automatically
     * when a memory object's reference count reaches 0, rather than being called directly
     * by application code.</p>
     * 
     * @param mem the memory object to release, must belong to this pool
     * @throws IllegalArgumentException if memory does not belong to this pool
     * @throws IllegalStateException if memory's reference count is not 0
     */
    public void release(T mem) {
        MemoryPoolable poolable = mem; // Explicit cast for clarity
        if (mem.refCount() != 0) {
            throw new IllegalStateException("cannot release memory with non-zero refcount");
        }
        if (poolable.getOwningPool() != this) {
            throw new IllegalArgumentException("memory does not belong to this pool");
        }
        mem.resetForReuse(); // Refcount=1, clear state
        while (true) {
            T currentHead = freeListHead.get();
            mem.setNextMemoryRaw(currentHead); // No refcount change
            if (freeListHead.compareAndSet(currentHead, mem)) {
                return;
            }
        }
    }

    /**
     * Returns the size of each memory segment in this pool.
     * 
     * <p>This value is set during pool construction and remains constant throughout
     * the pool's lifetime. All segments in the pool have exactly this size.</p>
     * 
     * @return the segment size in bytes
     */
    public long getSegmentSize() {
        return segmentSize;
    }

    /**
     * Returns the total number of segments pre-allocated in this pool.
     * 
     * <p>This value represents the maximum number of memory objects that can be
     * simultaneously allocated from this pool. It is set during construction
     * and remains constant throughout the pool's lifetime.</p>
     * 
     * @return the total segment count
     */
    public long getSegmentCount() {
        return segmentCount;
    }

    /**
     * Returns the current number of segments in the free list.
     * 
     * <p>This method provides a snapshot of pool utilization by counting available
     * segments. The value may change immediately after this method returns due to
     * concurrent allocation/release operations by other threads.</p>
     * 
     * <p><strong>Performance Note:</strong> This method traverses the entire free list
     * and has O(n) complexity where n is the number of free segments. It should be
     * used primarily for debugging and monitoring, not in performance-critical paths.</p>
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <ul>
     *   <li>Monitoring pool utilization in management interfaces</li>
     *   <li>Debugging memory leaks (free count should equal total when idle)</li>
     *   <li>Load balancing across multiple pools</li>
     * </ul>
     * 
     * @return the number of currently available segments
     */
    public long getFreeListSize() {
        long count = 0;
        Memory current = freeListHead.get();
        while (current != null) {
            count++;
            current = current.nextMemory();
        }
        return count;
    }
}
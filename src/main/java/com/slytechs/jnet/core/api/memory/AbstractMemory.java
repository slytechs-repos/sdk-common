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
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract base implementation providing common Memory interface functionality.
 * 
 * <p>AbstractMemory serves as the foundation for most concrete Memory implementations,
 * providing thread-safe reference counting, chain traversal logic, and common
 * validation methods. This class handles the complex aspects of memory management,
 * allowing subclasses to focus on specific storage and access patterns.</p>
 * 
 * <h2>Key Features</h2>
 * <ul>
 *   <li><strong>Thread-Safe Reference Counting:</strong> Atomic operations for safe concurrent access</li>
 *   <li><strong>Automatic Cleanup:</strong> Pool return and resource release on refcount = 0</li>
 *   <li><strong>Chain Support:</strong> Complete implementation of chain traversal</li>
 *   <li><strong>Validation Framework:</strong> Consistent error checking across implementations</li>
 *   <li><strong>ByteBuffer Caching:</strong> Lazy creation and reuse of ByteBuffer views</li>
 * </ul>
 * 
 * <h2>Reference Counting Implementation</h2>
 * <p>AbstractMemory uses atomic operations to ensure thread-safe reference counting:</p>
 * <ul>
 *   <li>Reference count starts at 1 upon creation</li>
 *   <li>All increment/decrement operations are atomic</li>
 *   <li>Automatic cleanup triggered when count reaches 0</li>
 *   <li>IllegalStateException thrown for operations on closed memory</li>
 * </ul>
 * 
 * <h2>Chain Management</h2>
 * <p>Provides complete chain traversal and navigation support:</p>
 * <pre>{@code
 * // Chain traversal example
 * Memory current = chainHead;
 * while (current != null) {
 *     processSegment(current);
 *     current = current.nextMemory();
 * }
 * 
 * // Seek to specific offset
 * Memory target = chainHead.seekMemory(offset);
 * }</pre>
 * 
 * <h2>Implementation Requirements</h2>
 * <p>AbstractMemory provides complete Memory interface implementation. Subclasses
 * may override methods for custom behavior but are not required to implement
 * any abstract methods.</p>
 * 
 * <h2>Extensibility Points</h2>
 * <p>Subclasses may override the following methods for custom behavior:</p>
 * <ul>
 *   <li>{@link #memoryDataOffset()} and {@link #memoryDataEnd()} - Custom data bounds</li>
 *   <li>{@link #resetForReuse()} - Pool-specific reset logic</li>
 *   <li>{@link #close()} - Custom cleanup behavior (must call super.close())</li>
 *   <li>{@link #chainMemoryCount()} - Custom chain counting for complex structures</li>
 * </ul>
 * 
 * <h2>Thread Safety</h2>
 * <p>AbstractMemory provides thread-safe operations for:</p>
 * <ul>
 *   <li>Reference counting (increment/decrement/query)</li>
 *   <li>Chain navigation (immutable chain structure)</li>
 *   <li>State checking (closed state detection)</li>
 *   <li>Chain modification (synchronized setNextMemory)</li>
 * </ul>
 * 
 * <p><strong>Note:</strong> While reference counting is thread-safe, chain modification
 * and custom data bound changes in subclasses may require additional synchronization.</p>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 * @see Memory for the complete interface
 * @see MemoryBuffer for poolable implementation
 * @see MemoryWrapper for simple wrapper implementation
 */
public abstract class AbstractMemory implements Memory {

    /**
     * Static utility method for thread-safe reference count decrementing.
     * 
     * <p>This method provides centralized reference count decrementing logic
     * with underflow protection. It ensures that reference counts never go
     * below zero, which would indicate a programming error.</p>
     * 
     * @param refCount the atomic reference counter to decrement
     * @return the new reference count after decrement
     * @throws IllegalStateException if decrement would cause underflow (count < 0)
     */
    static int decrementRef(AtomicInteger refCount) {
        int newRef = refCount.decrementAndGet();
        if (newRef < 0) {
            throw new IllegalStateException("refcount underflow");
        }
        return newRef;
    }

    /**
     * Static utility method for thread-safe reference count incrementing.
     * 
     * <p>This method provides centralized reference count incrementing logic
     * with closed-state protection. It ensures that reference counts cannot
     * be incremented on already-closed memory objects.</p>
     * 
     * @param refCount the atomic reference counter to increment
     * @return the new reference count after increment
     * @throws IllegalStateException if the memory is already closed (count = 0)
     */
    static int incrementRef(AtomicInteger refCount) {
        int currentRef;
        do {
            currentRef = refCount.get();
            if (currentRef == 0) {
                throw new IllegalStateException("cannot increment refcount on closed memory: ");
            }
        } while (!refCount.compareAndSet(currentRef, currentRef + 1));
        return currentRef + 1;
    }

    /**
     * Atomic reference counter for thread-safe memory management.
     * 
     * <p>This field maintains the current reference count using atomic operations
     * to ensure thread safety. A value of 0 indicates the memory has been closed.</p>
     */
    protected final AtomicInteger refCount = new AtomicInteger(1);

    /**
     * The backing MemorySegment providing the actual memory storage.
     * 
     * <p>This immutable field holds the underlying native memory segment that
     * provides the actual storage for this memory object.</p>
     */
    protected final MemorySegment memorySegment;

    /**
     * The absolute starting offset of the memory region (immutable).
     * 
     * <p>This field defines the first addressable byte within the backing
     * MemorySegment that this memory object covers.</p>
     */
    protected final long memoryOffset;

    /**
     * The absolute ending offset of the memory region (immutable, exclusive).
     * 
     * <p>This field defines the first byte beyond the addressable region
     * within the backing MemorySegment.</p>
     */
    protected final long memoryEnd;

    /**
     * Reference to the next Memory object in the chain.
     * 
     * <p>This field implements the chain linkage, allowing memory objects to form
     * linked lists. A null value indicates this is the last segment in the chain.</p>
     */
    protected Memory nextMemory;

    /**
     * Cached ByteBuffer view of this memory's data region.
     * 
     * <p>This field caches a ByteBuffer view to avoid repeated allocation.
     * The buffer is created lazily on first access and cleared when the
     * memory is reset or closed.</p>
     */
    private ByteBuffer byteBuffer;

    /**
     * Constructs an AbstractMemory with the specified bounds within a MemorySegment.
     * 
     * <p>This constructor establishes the immutable memory bounds and validates
     * that the specified region falls within the backing MemorySegment's capacity.
     * The reference count is initialized to 1.</p>
     * 
     * <p><strong>Bounds Validation:</strong> The constructor validates that:</p>
     * <ul>
     *   <li>memorySegment is not null</li>
     *   <li>memoryOffset ≥ 0</li>
     *   <li>memoryEnd ≥ memoryOffset</li>
     *   <li>memoryEnd - memoryOffset ≤ memorySegment.byteSize()</li>
     * </ul>
     * 
     * @param memorySegment the backing MemorySegment, must not be null
     * @param memoryOffset the starting offset within the segment (inclusive)
     * @param memoryEnd the ending offset within the segment (exclusive)
     * @throws NullPointerException if memorySegment is null
     * @throws IndexOutOfBoundsException if bounds are invalid
     */
    protected AbstractMemory(MemorySegment memorySegment, long memoryOffset, long memoryEnd) {
        Objects.requireNonNull(memorySegment, "memorySegment");
        if (memoryOffset < 0 || memoryEnd < memoryOffset || memoryEnd - memoryOffset > memorySegment.byteSize()) {
            throw new IndexOutOfBoundsException("invalid memory bounds: offset=" + memoryOffset + ", end=" + memoryEnd);
        }
        this.memorySegment = memorySegment;
        this.memoryOffset = memoryOffset;
        this.memoryEnd = memoryEnd;
    }

    /**
     * Returns a ByteBuffer view of this memory's usable data region.
     * 
     * <p>Creates or returns a cached ByteBuffer that provides access to the memory's
     * current data region. The buffer is sliced from the underlying MemorySegment
     * to cover exactly the data bounds. The buffer is cached to avoid repeated
     * allocation overhead.</p>
     * 
     * <p><strong>Caching Behavior:</strong> The ByteBuffer is created lazily on
     * first access and reused for subsequent calls. The cache is cleared when
     * the memory is reset or closed.</p>
     * 
     * @return a ByteBuffer positioned at the start of usable data
     * @throws IllegalStateException if this memory is closed
     */
    @Override
    public final ByteBuffer asByteBuffer() {
        checkNotClosed();
        if (byteBuffer == null) {
            byteBuffer = memorySegment
                    .asSlice(memoryDataOffset(), memoryDataLength())
                    .asByteBuffer();
        }
        return byteBuffer;
    }

    /**
     * Returns the underlying MemorySegment of this memory.
     * 
     * <p>Provides direct access to the backing MemorySegment without any slicing
     * or bounds adjustment. This gives access to the complete segment, not just
     * the region covered by this memory object's bounds.</p>
     * 
     * @return the complete backing MemorySegment
     * @throws IllegalStateException if this memory is closed
     */
    @Override
    public final MemorySegment asMemorySegment() {
        checkNotClosed();
        return memorySegment;
    }

    /**
     * Returns the MemorySegment containing the specified offset within the memory chain.
     * 
     * <p>This method provides efficient random access to any position within a chained
     * memory structure. For single segments, it returns the backing segment directly.
     * For chains, it uses {@link #seekMemory(long)} to locate the appropriate segment.</p>
     * 
     * @param chainOffset the byte offset from the start of the entire chain (0-based)
     * @return the MemorySegment containing the specified offset
     * @throws IllegalArgumentException if chainOffset is out of bounds
     * @throws IllegalStateException if this memory is closed
     */
    @Override
    public MemorySegment asMemorySegmentAt(long chainOffset) {
        checkNotClosed();
        if (chainOffset < 0 || chainOffset >= chainDataLength())
            throw new IllegalArgumentException("chainOffset out of bounds: " + chainOffset);

        if (nextMemory == null) {
            return memorySegment;
        }
        Memory sought = seekMemory(chainOffset);
        return sought.asMemorySegment();
    }

    /**
     * Returns the total capacity across all segments in the memory chain.
     * 
     * <p>This method traverses the entire memory chain, summing the individual
     * capacities of each segment. This implementation provides O(n) complexity
     * where n is the number of segments in the chain.</p>
     * 
     * @return the total capacity across all chain segments
     * @throws IllegalStateException if this memory is closed
     */
    @Override
    public long chainCapacity() {
        checkNotClosed();
        long total = 0;
        for (Memory mem = this; mem != null; mem = mem.nextMemory()) {
            total += mem.memoryCapacity();
        }
        return total;
    }

    /**
     * Returns the total usable data length across all segments in the memory chain.
     * 
     * <p>This method traverses the entire memory chain, summing the data lengths
     * of each segment. This implementation provides O(n) complexity where n is
     * the number of segments in the chain.</p>
     * 
     * @return the total usable data length across all chain segments
     * @throws IllegalStateException if this memory is closed
     */
    @Override
    public long chainDataLength() {
        checkNotClosed();
        long total = 0;
        for (Memory mem = this; mem != null; mem = mem.nextMemory()) {
            total += mem.memoryDataLength();
        }
        return total;
    }

    /**
     * Returns the number of Memory objects in the memory chain.
     * 
     * <p>This method counts the total number of linked Memory segments by
     * traversing the entire chain. This implementation provides O(n) complexity
     * where n is the number of segments.</p>
     * 
     * @return the number of Memory objects in the chain (≥ 1)
     * @throws IllegalStateException if this memory is closed
     */
    @Override
    public int chainMemoryCount() {
        checkNotClosed();
        int count = 0;
        for (Memory mem = this; mem != null; mem = mem.nextMemory()) {
            count++;
        }
        return count;
    }

    /**
     * Checks if this memory represents a null or invalid memory reference.
     * 
     * <p>Uses the static {@link Memory#isNull(MemorySegment)} method to check
     * the underlying MemorySegment for null conditions. This method does not
     * check the closed state.</p>
     * 
     * @return {@code true} if this represents a null or invalid memory reference
     */
    @Override
    public boolean isNull() {
        return Memory.isNull(memorySegment);
    }

    /**
     * Checks if this memory represents a pointer (zero-sized memory reference).
     * 
     * <p>A memory is considered a pointer if the backing MemorySegment has zero
     * byte size but a non-zero address. This indicates the segment represents
     * a memory address rather than actual data storage.</p>
     * 
     * @return {@code true} if this represents a pointer (zero-sized memory)
     * @throws IllegalStateException if this memory is closed
     */
    @Override
    public boolean isPointer() {
        checkNotClosed();
        return memorySegment.byteSize() == 0 && memorySegment.address() != 0;
    }

    /**
     * Returns the total capacity of the memory region in bytes.
     * 
     * <p>Calculates the capacity as {@code memoryEnd - memoryOffset}, representing
     * the total addressable space within this memory object's bounds.</p>
     * 
     * @return the total capacity in bytes (always ≥ 0)
     * @throws IllegalStateException if this memory is closed
     */
    @Override
    public long memoryCapacity() {
        checkNotClosed();
        return memoryEnd - memoryOffset;
    }

    /**
     * Returns the absolute ending offset of the current data region.
     * 
     * <p>The default implementation returns {@link #memoryEnd()}, meaning the
     * entire memory region is considered active data. Subclasses may override
     * this to provide custom data boundary management.</p>
     * 
     * @return the absolute data ending offset (exclusive bound)
     * @throws IllegalStateException if this memory is closed
     */
    @Override
    public long memoryDataEnd() {
        checkNotClosed();
        return memoryEnd;
    }

    /**
     * Returns the length of the current data region in bytes.
     * 
     * <p>Calculates the data length as {@code memoryDataEnd() - memoryDataOffset()}.
     * This represents the amount of currently accessible data within the memory bounds.</p>
     * 
     * @return the current data length in bytes (always ≥ 0)
     * @throws IllegalStateException if this memory is closed
     */
    @Override
    public long memoryDataLength() {
        checkNotClosed();
        return memoryDataEnd() - memoryDataOffset();
    }

    /**
     * Returns the absolute starting offset of the current data region.
     * 
     * <p>The default implementation returns {@link #memoryOffset()}, meaning
     * data starts at the beginning of the memory region. Subclasses may override
     * this to provide custom data boundary management.</p>
     * 
     * @return the absolute data starting offset (inclusive bound)
     * @throws IllegalStateException if this memory is closed
     */
    @Override
    public long memoryDataOffset() {
        checkNotClosed();
        return memoryOffset;
    }

    /**
     * Returns the data offset within the memory segment containing the specified chain offset.
     * 
     * <p>The default implementation returns {@link #memoryDataOffset()}, which is
     * appropriate for non-chained memory. Subclasses implementing complex chain
     * offset mapping may override this method.</p>
     * 
     * @param chainOffset the offset across the entire chain
     * @return the local data offset within the containing memory segment
     * @throws IllegalStateException if this memory is closed
     */
    @Override
    public long memoryDataOffsetAt(long chainOffset) {
        checkNotClosed();
        return memoryDataOffset();
    }

    /**
     * Returns the absolute ending offset of the memory region.
     * 
     * <p>This immutable value represents the first byte position beyond the
     * addressable memory region within the backing MemorySegment.</p>
     * 
     * @return the absolute ending offset (exclusive bound)
     * @throws IllegalStateException if this memory is closed
     */
    @Override
    public long memoryEnd() {
        checkNotClosed();
        return memoryEnd;
    }

    /**
     * Returns the absolute starting offset of the memory region.
     * 
     * <p>This immutable value represents the first addressable byte position
     * within the backing MemorySegment that this memory object covers.</p>
     * 
     * @return the absolute starting offset (inclusive bound)
     * @throws IllegalStateException if this memory is closed
     */
    @Override
    public long memoryOffset() {
        checkNotClosed();
        return memoryOffset;
    }

    /**
     * Returns the next Memory object in the chain.
     * 
     * <p>Provides navigation through the memory chain structure. This method is
     * thread-safe for reading the chain structure.</p>
     * 
     * @return the next Memory in the chain, or {@code null} if none exists
     * @throws IllegalStateException if this memory is closed
     */
    @Override
    public Memory nextMemory() {
        checkNotClosed();
        return nextMemory;
    }

    /**
     * Returns the current reference count of this memory object.
     * 
     * <p>Provides a thread-safe snapshot of the current reference count. The value
     * may change immediately after this method returns due to concurrent operations.</p>
     * 
     * @return the current reference count (≥ 0)
     * @throws IllegalStateException if this memory is closed
     */
    @Override
    public int refCount() {
        checkNotClosed();
        return refCount.get();
    }

    /**
     * Resets this memory object for reuse, typically called by memory pools.
     * 
     * <p>This method resets the reference count to 1, clears the chain linkage,
     * and invalidates cached objects like ByteBuffer views. Subclasses should
     * override this method to add custom reset logic while ensuring to call
     * {@code super.resetForReuse()}.</p>
     * 
     * <p><strong>Pool Integration:</strong> This method is typically called by
     * memory pools when preparing objects for reuse.</p>
     * 
     * @throws IllegalStateException if refcount is not 0
     */
    protected void resetForReuse() {
        if (refCount.get() != 0)
            throw new IllegalStateException("cannot reset memory with non-zero refcount");

        refCount.set(1);
        nextMemory = null;
        byteBuffer = null;
    }

    /**
     * Locates and returns the Memory object containing the specified chain offset.
     * 
     * <p>This method performs efficient navigation through the memory chain to find
     * the segment that contains the specified byte offset. For single segments,
     * it validates the offset and returns this object. For chains, it traverses
     * segments until finding the one containing the offset.</p>
     * 
     * @param chainOffset the byte offset from the start of the entire chain (0-based)
     * @return the Memory object containing the specified offset
     * @throws IllegalArgumentException if chainOffset is out of bounds
     * @throws IllegalStateException if this memory is closed
     */
    @Override
    public Memory seekMemory(long chainOffset) {
        checkNotClosed();
        if (nextMemory == null) {
            Objects.checkIndex(chainOffset, memoryDataLength());
            return this;
        }
        long chainDataLength = 0;
        Memory current = this;
        while (current != null && chainOffset >= (chainDataLength += current.memoryDataLength())) {
            current = current.nextMemory();
        }
        Objects.checkIndex(chainOffset, chainDataLength);
        return current;
    }

    /**
     * Sets the next Memory object in the chain with proper reference management.
     * 
     * <p>This method establishes or modifies chain linkage with automatic reference
     * counting. It decrements the reference count of any previously linked memory
     * and increments the count of the newly linked memory. The operation is
     * synchronized to ensure thread-safe chain modification.</p>
     * 
     * <p><strong>Reference Management:</strong> This method properly manages reference
     * counts to prevent memory leaks and ensure chain validity.</p>
     * 
     * @param next the next Memory object in the chain, or {@code null} to terminate
     * @throws IllegalStateException if this memory is closed
     * @throws IllegalArgumentException if attempting to create a duplicate binding
     */
    @Override
    public synchronized void setNextMemory(Memory next) {
        checkNotClosed();

        if (this.nextMemory == next)
            throw new IllegalArgumentException("duplicate binding: " + toString());

        Memory oldNext = this.nextMemory;
        if (oldNext != null) {
            oldNext.decrementRef();
        }
        if (next != null) {
            next.incrementRef();
        }
        this.nextMemory = next;
    }

    /**
     * Sets the next Memory object without reference counting for internal use.
     * 
     * <p>This method provides direct access to set the next memory reference
     * without reference counting, intended for internal operations such as
     * pool management. External code should use {@link #setNextMemory(Memory)} instead.</p>
     * 
     * @param next the next Memory object to set
     */
    void setNextMemoryRaw(Memory next) {
        this.nextMemory = next;
    }

    /**
     * Validates that this memory object is not closed.
     * 
     * <p>This utility method provides consistent closed-state checking across
     * all operations. It should be called at the beginning of methods that
     * require valid memory state.</p>
     * 
     * @throws IllegalStateException if this memory is closed (refcount = 0)
     */
    protected final void checkNotClosed() {
        if (refCount.get() == 0) {
            throw new IllegalStateException("memory is closed: " + toString());
        }
    }

    /**
     * Decrements the reference count atomically and returns the new value.
     * 
     * <p>When the reference count reaches 0, this method automatically calls
     * {@link #close()} to clean up the memory object. This operation is thread-safe.</p>
     * 
     * @return the new reference count after decrement (≥ 0)
     * @throws IllegalStateException if refcount underflow occurs
     */
    @Override
    public int decrementRef() {
        int newRef = decrementRef(refCount);
        if (newRef == 0) {
            close();
        }
        return newRef;
    }

    /**
     * Increments the reference count atomically and returns the new value.
     * 
     * <p>This operation is thread-safe and ensures the memory remains valid as long
     * as references exist. Each call to this method must be paired with a corresponding
     * call to {@link #decrementRef()}.</p>
     * 
     * @return the new reference count after increment (≥ 1)
     * @throws IllegalStateException if this memory is already closed
     */
    @Override
    public int incrementRef() {
        return incrementRef(refCount);
    }

    /**
     * Closes this memory object, releasing associated resources.
     * 
     * <p>This method sets the reference count to 0, clears chain linkages, and
     * invalidates cached objects. Subclasses should override this method to add
     * custom cleanup logic while ensuring to call {@code super.close()}.</p>
     * 
     * @throws IllegalStateException if refcount is not 0
     */
    @Override
    public void close() {
        if (refCount.get() != 0) {
            throw new IllegalStateException("cannot close memory with non-zero refcount: " + refCount.get());
        }
        refCount.set(0); // Mark as closed
        nextMemory = null;
        byteBuffer = null;
    }

    /**
     * Checks whether there is a next Memory object in the chain.
     * 
     * <p>This method provides a convenient way to test for chain continuation
     * without modifying navigation state.</p>
     * 
     * @return {@code true} if {@link #nextMemory()} would return a non-null value
     * @throws IllegalStateException if this memory is closed
     */
    @Override
    public boolean hasNextMemory() {
        checkNotClosed();
        return nextMemory != null;
    }

    /**
     * Returns a string representation of this AbstractMemory.
     * 
     * <p>The string includes memory bounds and reference count for debugging purposes.</p>
     * 
     * @return a string representation of this memory object
     */
    @Override
    public String toString() {
        return "AbstractMemory [memoryOffset=" + memoryOffset + ", memoryEnd=" + memoryEnd + ", refCount=" + refCount + "]";
    }
}
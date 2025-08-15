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
 * Abstract base implementation providing comprehensive Memory interface
 * functionality.
 * 
 * <p>
 * AbstractMemory serves as the foundation for all concrete Memory
 * implementations, providing thread-safe reference counting, chain management,
 * and core memory operations. This class handles the complex aspects of memory
 * lifecycle and boundary management, allowing subclasses to focus on
 * specialized behaviors such as pooling or custom data access patterns.
 * </p>
 * 
 * <h2>Memory Model</h2>
 * 
 * <h3>Segment Region Structure</h3>
 * 
 * <pre>{@code
* Memory Segment:
* ┌──────────────────────────────────────────────────────┐
* │                  segmentSize                          │
* │  ┌──────────┬─────────────────────┬──────────┐      │
* │  │ headroom │  activeBytesLength   │ tailroom │      │
* │  └──────────┴─────────────────────┴──────────┘      │
* └──────────────────────────────────────────────────────┘
*    ↑          ↑                      ↑          ↑
* segmentOffset activeBytesStart  activeBytesEnd  segmentEnd
* 
* Invariants maintained:
* • segmentOffset ≤ activeBytesStart ≤ activeBytesEnd ≤ segmentEnd
* • segmentSize = segmentEnd - segmentOffset (immutable)
* • activeBytesLength = activeBytesEnd - activeBytesStart (mutable)
* }</pre>
 * 
 * <h3>Reference Counting Model</h3>
 * <p>
 * AbstractMemory implements thread-safe reference counting:
 * </p>
 * <ul>
 * <li>Initial reference count is 1 upon creation</li>
 * <li>All increment/decrement operations use atomic CAS</li>
 * <li>Automatic cleanup when count reaches 0</li>
 * <li>Operations on closed memory (refcount=0) throw IllegalStateException</li>
 * </ul>
 * 
 * <h3>Chain Management</h3>
 * <p>
 * Provides complete support for linked memory structures:
 * </p>
 * 
 * <pre>{@code
 * // Chain traversal
 * Memory current = chainHead;
 * while (current != null) {
 * 	processSegment(current);
 * 	current = current.nextSegment();
 * }
 * 
 * // Position-based access
 * Memory target = chainHead.seekSegment(1500);
 * }</pre>
 * 
 * <h2>Key Features</h2>
 * 
 * <table border="1">
 * <caption>AbstractMemory Features</caption> <thead>
 * <tr>
 * <th>Feature</th>
 * <th>Implementation</th>
 * <th>Thread Safety</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>Reference Counting</td>
 * <td>Atomic operations</td>
 * <td>Yes</td>
 * </tr>
 * <tr>
 * <td>Chain Navigation</td>
 * <td>Linked list traversal</td>
 * <td>Read-safe</td>
 * </tr>
 * <tr>
 * <td>Boundary Management</td>
 * <td>Immutable segment, mutable active</td>
 * <td>Volatile fields</td>
 * </tr>
 * <tr>
 * <td>ByteBuffer Caching</td>
 * <td>Lazy creation</td>
 * <td>No</td>
 * </tr>
 * </tbody>
 * </table>
 * 
 * <h2>Extensibility Points</h2>
 * <p>
 * Subclasses may override these methods for custom behavior:
 * </p>
 * <ul>
 * <li>{@link #onRefCountZero()} - Handle cleanup when references reach
 * zero</li>
 * <li>{@link #resetForReuse()} - Reset state for pool reuse</li>
 * <li>{@link #activeBytesStart()} / {@link #activeBytesEnd()} - Custom data
 * bounds</li>
 * </ul>
 * 
 * <h2>Thread Safety Guarantees</h2>
 * <ul>
 * <li><strong>Safe:</strong> Reference counting, state checking, chain
 * reading</li>
 * <li><strong>Synchronized:</strong> Chain modification (setNextMemory)</li>
 * <li><strong>Unsafe:</strong> Active bytes modification, ByteBuffer
 * access</li>
 * </ul>
 * 
 * <p>
 * <strong>Note:</strong> While core operations are thread-safe, modifications
 * to active bytes boundaries and ByteBuffer access require external
 * synchronization if accessed concurrently.
 * </p>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 * @see Memory for the complete interface specification
 * @see MemoryBuffer for poolable buffer implementation
 * @see MemoryProxy for rebindable proxy implementation
 */
public abstract class AbstractMemory implements Memory {

	/**
	 * Thread-safely decrements an atomic reference counter with underflow
	 * protection.
	 * 
	 * <p>
	 * This utility method provides centralized reference count decrementing with
	 * validation to prevent underflow conditions that would indicate programming
	 * errors such as double-free attempts.
	 * </p>
	 * 
	 * @param refCount the atomic reference counter to decrement
	 * @return the new reference count after decrement (≥ 0)
	 * @throws IllegalStateException if decrement would cause underflow (count < 0)
	 */
	static int decrementRef(AtomicInteger refCount) {
		int newRef = refCount.decrementAndGet();
		if (newRef < 0) {
			// Reset to prevent further damage
			refCount.set(0);
			throw new IllegalStateException("Reference count underflow - possible double-free");
		}
		return newRef;
	}

	/**
	 * Thread-safely increments an atomic reference counter with closed-state
	 * protection.
	 * 
	 * <p>
	 * This utility method uses compare-and-swap operations to ensure atomic
	 * incrementing while preventing resurrection of already-closed memory objects
	 * (those with refcount = 0).
	 * </p>
	 * 
	 * @param refCount the atomic reference counter to increment
	 * @return the new reference count after increment (≥ 1)
	 * @throws IllegalStateException if the memory is already closed (count = 0)
	 */
	static int incrementRef(AtomicInteger refCount) {
		int currentRef;
		do {
			currentRef = refCount.get();
			if (currentRef == 0) {
				throw new IllegalStateException("Cannot increment reference count on released memory");
			}
		} while (!refCount.compareAndSet(currentRef, currentRef + 1));
		return currentRef + 1;
	}

	/**
	 * Atomic reference counter for thread-safe lifecycle management.
	 * 
	 * <p>
	 * Tracks the number of active references to this memory object. When the count
	 * reaches zero, the memory is considered released and all operations will fail
	 * with IllegalStateException.
	 * </p>
	 */
	protected final AtomicInteger refCount = new AtomicInteger(1);

	/**
	 * The backing MemorySegment providing actual memory storage.
	 * 
	 * <p>
	 * This immutable field holds the underlying Java FFM MemorySegment that
	 * provides the physical memory for this object. The segment may be larger than
	 * the region managed by this Memory object.
	 * </p>
	 */
	protected final MemorySegment segment;

	/**
	 * Starting offset of the segment region (immutable).
	 * 
	 * <p>
	 * Defines the first addressable byte within the backing MemorySegment that this
	 * Memory object manages. This boundary is immutable after construction.
	 * </p>
	 */
	protected final long segmentStart;

	/**
	 * Ending offset of the segment region (immutable, exclusive).
	 * 
	 * <p>
	 * Defines the first byte beyond the addressable region within the backing
	 * MemorySegment. This boundary is immutable after construction.
	 * </p>
	 */
	protected final long segmentStop;

	/**
	 * Starting offset of active bytes within the segment (mutable).
	 * 
	 * <p>
	 * Defines where the currently active data begins within the segment bounds.
	 * This can be adjusted at runtime to consume data or utilize headroom. Must
	 * satisfy: segmentStart ≤ activeBytesStart ≤ activeBytesEnd.
	 * </p>
	 */
	protected long activeBytesStart;

	/**
	 * Ending offset of active bytes within the segment (mutable, exclusive).
	 * 
	 * <p>
	 * Defines where the currently active data ends within the segment bounds. This
	 * can be adjusted at runtime to append data or utilize tailroom. Must satisfy:
	 * activeBytesStart ≤ activeBytesEnd ≤ segmentStop.
	 * </p>
	 */
	protected long activeBytesStop;

	/**
	 * Reference to the next Memory segment in the chain.
	 * 
	 * <p>
	 * Implements the singly-linked list structure for memory chains. A null value
	 * indicates this is the last segment. Access to this field should be
	 * synchronized when modifying the chain structure.
	 * </p>
	 */
	protected Memory nextSegment;

	/**
	 * Cached ByteBuffer view of the active bytes region.
	 * 
	 * <p>
	 * Lazily created on first access to avoid allocation overhead. Cleared when the
	 * memory is reset or closed. Not thread-safe - concurrent access requires
	 * external synchronization.
	 * </p>
	 */
	private ByteBuffer cachedByteBuffer;

	/**
	 * Constructs an AbstractMemory with specified segment and full active bounds.
	 * 
	 * <p>
	 * Establishes both the immutable segment boundaries and the initial active
	 * bytes region within those boundaries. The constructor validates that all
	 * bounds form a valid hierarchy and fall within the backing MemorySegment.
	 * </p>
	 * 
	 * <h3>Validation Rules</h3>
	 * <ul>
	 * <li>segment must not be null</li>
	 * <li>segmentStart ≥ 0</li>
	 * <li>segmentStart ≤ segmentStop</li>
	 * <li>segmentStop - segmentStart ≤ segment.byteSize()</li>
	 * </ul>
	 * 
	 * @param segment      the backing MemorySegment, must not be null
	 * @param segmentStart starting offset of the segment region (inclusive)
	 * @param segmentStop  ending offset of the segment region (exclusive)
	 * @throws NullPointerException      if segment is null
	 * @throws IndexOutOfBoundsException if bounds are invalid or violate
	 *                                   constraints
	 */
	protected AbstractMemory(
			MemorySegment segment,
			long segmentStart, long segmentStop) {
		this(segment, segmentStart, segmentStop, segmentStart, segmentStop);
	}

	/**
	 * Constructs an AbstractMemory with specified segment and active bounds.
	 * 
	 * <p>
	 * Establishes both the immutable segment boundaries and the initial active
	 * bytes region within those boundaries. The constructor validates that all
	 * bounds form a valid hierarchy and fall within the backing MemorySegment.
	 * </p>
	 * 
	 * <h3>Validation Rules</h3>
	 * <ul>
	 * <li>segment must not be null</li>
	 * <li>segmentStart ≥ 0</li>
	 * <li>segmentStart ≤ segmentStop</li>
	 * <li>segmentStop - segmentStart ≤ segment.byteSize()</li>
	 * <li>segmentStart ≤ activeBytesStart ≤ activeBytesStop ≤ segmentStop</li>
	 * </ul>
	 * 
	 * @param segment          the backing MemorySegment, must not be null
	 * @param segmentStart     starting offset of the segment region (inclusive)
	 * @param segmentStop      ending offset of the segment region (exclusive)
	 * @param activeBytesStart initial start of active bytes (inclusive)
	 * @param activeBytesStop  initial end of active bytes (exclusive)
	 * @throws NullPointerException      if segment is null
	 * @throws IndexOutOfBoundsException if bounds are invalid or violate
	 *                                   constraints
	 */
	protected AbstractMemory(
			MemorySegment segment,
			long segmentStart, long segmentStop,
			long activeBytesStart, long activeBytesStop) {

		Objects.requireNonNull(segment, "segment cannot be null");

		// Validate segment bounds
		if (segmentStart < 0 || segmentStop < segmentStart ||
				segmentStop - segmentStart > segment.byteSize()) {
			throw new IndexOutOfBoundsException(
					String.format("Invalid segment bounds: start=%d, stop=%d, segment.byteSize=%d",
							segmentStart, segmentStop, segment.byteSize()));
		}

		// Validate active bytes bounds
		if (activeBytesStart < segmentStart || activeBytesStop > segmentStop ||
				activeBytesStop < activeBytesStart) {
			throw new IndexOutOfBoundsException(
					String.format("Invalid active bytes bounds: activeStart=%d, activeStop=%d (segment: %d-%d)",
							activeBytesStart, activeBytesStop, segmentStart, segmentStop));
		}

		this.segment = segment;
		this.segmentStart = segmentStart;
		this.segmentStop = segmentStop;
		this.activeBytesStart = activeBytesStart;
		this.activeBytesStop = activeBytesStop;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Returns the current end boundary of active bytes within this segment. This
	 * value is mutable and can be adjusted using {@link #activeBytesEnd(long)}.
	 * </p>
	 */
	@Override
	public long activeBytesEnd() {
		checkValid();
		return activeBytesStop;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Adjusts the end boundary of active bytes, typically used when appending data
	 * or truncating the active region. The new boundary must respect the segment
	 * bounds and maintain valid ordering.
	 * </p>
	 */
	@Override
	public long activeBytesEnd(long newEnd) {
		checkValid();
		if (newEnd < activeBytesStart || newEnd > segmentStop) {
			throw new IllegalArgumentException(
					String.format("Invalid active bytes end: %d (must be between %d and %d)",
							newEnd, activeBytesStart, segmentStop));
		}
		this.activeBytesStop = newEnd;
		this.cachedByteBuffer = null; // Invalidate cache
		return newEnd;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Calculates the current length of active bytes as the difference between end
	 * and start boundaries.
	 * </p>
	 */
	@Override
	public long activeBytesLength() {
		checkValid();
		return activeBytesStop - activeBytesStart;
	}

	long activeBytesLengthRaw() {
		return activeBytesStop - activeBytesStart;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Returns the current start boundary of active bytes within this segment. This
	 * value is mutable and can be adjusted using {@link #activeBytesStart(long)}.
	 * </p>
	 */
	@Override
	public long activeBytesStart() {
		checkValid();
		return activeBytesStart;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Adjusts the start boundary of active bytes, typically used when consuming
	 * data from the front or utilizing headroom for header prepending.
	 * </p>
	 */
	@Override
	public long activeBytesStart(long newStart) {
		checkValid();
		if (newStart < segmentStart || newStart > activeBytesStop) {
			throw new IllegalArgumentException(
					String.format("Invalid active bytes start: %d (must be between %d and %d)",
							newStart, segmentStart, activeBytesStop));
		}
		this.activeBytesStart = newStart;
		this.cachedByteBuffer = null; // Invalidate cache
		return newStart;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Creates or returns a cached ByteBuffer view of the active bytes region. The
	 * buffer is sliced from the underlying MemorySegment to cover exactly the
	 * active bytes bounds. Caching avoids repeated allocation overhead.
	 * </p>
	 * 
	 * <p>
	 * <strong>Note:</strong> The cached buffer is invalidated when active bytes
	 * boundaries change.
	 * </p>
	 */
	@Override
	public final ByteBuffer asByteBuffer() {
		checkValid();
		if (cachedByteBuffer == null) {
			cachedByteBuffer = segment
					.asSlice(activeBytesStart, activeBytesLength())
					.asByteBuffer();
		}
		return cachedByteBuffer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Memory asMemory() {
		return this;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Returns the underlying MemorySegment sliced to the active bytes region.
	 * Unlike {@link #asByteBuffer()}, this method always creates a fresh slice
	 * without caching.
	 * </p>
	 */
	@Override
	public final MemorySegment asMemorySegment() {
		checkValid();
		return segment.asSlice(activeBytesStart, activeBytesLength());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Provides position-based access within the memory chain. For single segments,
	 * validates the position and returns the segment. For chains, delegates to
	 * {@link #seekSegment(long)} for navigation.
	 * </p>
	 */
	@Override
	public MemorySegment asMemorySegmentAt(long position) {
		checkValid();
		if (position < 0 || position >= totalActiveBytes()) {
			throw new IllegalArgumentException(
					String.format("Position out of bounds: %d (total active bytes: %d)",
							position, totalActiveBytes()));
		}

		if (nextSegment == null) {
			return segment.asSlice(activeBytesStart + position,
					activeBytesLength() - position);
		}

		Memory target = seekSegment(position);
		// Calculate local offset within target segment
		long localOffset = position;
		Memory current = this;
		while (current != target) {
			localOffset -= current.activeBytesLength();
			current = current.nextSegment();
		}
		return target.asMemorySegment().asSlice(localOffset);
	}

	public void bindMemorySegment(MemorySegment segment,
			long start, long stop, long activeStart, long activeStop) {
		if (refCount() != 0)
			throw new MemoryBindingException("already bound, refCount=" + refCount());
	}

	/**
	 * Validates that this memory object is not closed.
	 * 
	 * <p>
	 * Central validation method called by all operations requiring valid state.
	 * Checks that the reference count is non-zero, indicating the memory is still
	 * alive.
	 * </p>
	 * 
	 * @throws IllegalStateException if this memory is closed (refcount = 0)
	 */
	protected final void checkValid() {
		if (refCount.get() == 0) {
			throw new IllegalStateException(
					"Memory has been released: " + Objects.toIdentityString(segment) + ", ref=" + refCount());
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Thread-safe decrement with automatic cleanup when count reaches zero. Calls
	 * {@link #onRefCountZero()} to handle cleanup, which subclasses can override
	 * for custom behavior like pool return.
	 * </p>
	 */
	@Override
	public int decrementRef() {
		int newRef = decrementRef(refCount);
		if (newRef == 0) {
			onRefCountZero();
		}
		return newRef;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasNextSegment() {
		checkValid();
		return nextSegment != null;
	}

	long headroomRaw() {
		return activeBytesStart - segmentStart;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Thread-safe increment preventing resurrection of closed memory.
	 * </p>
	 */
	@Override
	public int incrementRef() {
		return incrementRef(refCount);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isNull() {
		return Memory.isNull(segment);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Checks if this represents a zero-sized pointer by examining the underlying
	 * MemorySegment size and address.
	 * </p>
	 */
	@Override
	public boolean isPointer() {
		checkValid();
		return segment.byteSize() == 0 && segment.address() != 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Memory nextSegment() {
		checkValid();
		return nextSegment;
	}

	/**
	 * Returns the next segment without validation (internal use only).
	 * 
	 * <p>
	 * Package-private method for pool management and other internal operations that
	 * need raw access to the chain structure.
	 * </p>
	 * 
	 * @return the next Memory in the chain, or null if none
	 */
	Memory nextSegmentRaw() {
		return nextSegment;
	}

	/**
	 * Called when reference count reaches zero.
	 * 
	 * <p>
	 * Hook method for subclasses to implement custom cleanup behavior such as
	 * returning to a memory pool. The default implementation clears references to
	 * aid garbage collection.
	 * </p>
	 * 
	 * <p>
	 * Subclasses overriding this method should typically:
	 * </p>
	 * <ul>
	 * <li>Reset internal state for reuse (if pooled)</li>
	 * <li>Return to pool if applicable</li>
	 * <li>Clear references to prevent memory leaks</li>
	 * </ul>
	 */
	protected void onRefCountZero() {
		nextSegment = null;
		cachedByteBuffer = null;
	}

	/**
	 * Prepares this memory for use (internal pool support).
	 * 
	 * <p>
	 * Package-private method called by memory pools when allocating a previously
	 * released object. Sets reference count to 1.
	 * </p>
	 */
	void prepareForUse() {
		refCount.set(1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final int refCount() {
		return refCount.get();
	}

	/**
	 * Resets this memory object for pool reuse.
	 * 
	 * <p>
	 * Called by memory pools when preparing objects for reuse. Resets reference
	 * count, clears chain linkage, and invalidates cached views. Subclasses should
	 * override to add custom reset logic while calling super.resetForReuse().
	 * </p>
	 * 
	 * @throws IllegalStateException if refcount is not 0
	 */
	protected void resetForReuse() {
		if (refCount.get() != 0) {
			throw new IllegalStateException(
					"Cannot reset memory with non-zero reference count: " + refCount.get());
		}

		refCount.set(1);
		nextSegment = null;
		cachedByteBuffer = null;
		// Reset active bytes to full segment
		activeBytesStart = segmentStart;
		activeBytesStop = segmentStop;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Navigates through the chain to find the segment containing the specified
	 * position. Uses linear search with O(n) complexity where n is the number of
	 * segments before the target.
	 * </p>
	 */
	@Override
	public Memory seekSegment(long position) {
		checkValid();

		if (position < 0) {
			throw new IllegalArgumentException("Position cannot be negative: " + position);
		}

		long currentPosition = 0;
		Memory current = this;

		while (current != null) {
			long segmentLength = current.activeBytesLength();
			if (position < currentPosition + segmentLength) {
				return current;
			}
			currentPosition += segmentLength;
			current = current.nextSegment();
		}

		throw new IndexOutOfBoundsException(
				String.format("Position %d exceeds total active bytes %d",
						position, currentPosition));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Counts segments by traversing the entire chain with O(n) complexity.
	 * </p>
	 */
	@Override
	public int segmentCount() {
		checkValid();
		int count = 0;
		for (Memory seg = this; seg != null; seg = seg.nextSegment()) {
			count++;
		}
		return count;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long segmentEnd() {
		checkValid();
		return segmentStop;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long segmentOffset() {
		checkValid();
		return segmentStart;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long segmentSize() {
		checkValid();
		return segmentStop - segmentStart;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Thread-safe chain modification with automatic reference counting. Decrements
	 * the reference of any previously linked segment and increments the reference
	 * of the newly linked segment. Prevents circular references.
	 * </p>
	 */
	@Override
	public synchronized void setNextMemory(Memory next) {
		checkValid();

		// Skip if no change
		if (this.nextSegment == next) {
			return;
		}

		// Prevent immediate circular reference
		if (next == this) {
			throw new IllegalArgumentException(
					"Cannot create circular reference: memory cannot point to itself");
		}

		// Check for deeper cycles
		if (next != null) {
			Memory current = next;
			while (current != null) {
				if (current == this) {
					throw new IllegalArgumentException(
							"Cannot create circular reference in chain");
				}
				current = current.nextSegment();
			}
		}

		// Update references
		Memory oldNext = this.nextSegment;
		if (oldNext != null) {
			oldNext.decrementRef();
		}
		if (next != null) {
			next.incrementRef();
		}
		this.nextSegment = next;
	}

	/**
	 * Sets next segment without reference counting (internal use).
	 * 
	 * <p>
	 * Package-private method for pool management that bypasses reference counting.
	 * Should only be used when reference counts are managed externally.
	 * </p>
	 * 
	 * @param next the next segment to set
	 */
	void setNextSegmentRaw(Memory next) {
		this.nextSegment = next;
	}

	long tailroomRaw() {
		return segmentStop - activeBytesStop;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return String.format("%s[segment=%d-%d, active=%d-%d, refCount=%d]",
				getClass().getSimpleName(),
				segmentStart, segmentStop,
				activeBytesStart, activeBytesStop,
				refCount.get());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Traverses the chain summing active bytes lengths with O(n) complexity.
	 * </p>
	 */
	@Override
	public long totalActiveBytes() {
		checkValid();
		long total = 0;
		for (AbstractMemory seg = this; seg != null; seg = (AbstractMemory) seg.nextSegment()) {
			total += seg.activeBytesLengthRaw();
		}
		return total;
	}

	long totalActiveBytesRaw() {
		long total = 0;
		for (AbstractMemory seg = this; seg != null; seg = (AbstractMemory) seg.nextSegment()) {
			total += seg.activeBytesLengthRaw();
		}
		return total;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Traverses the chain summing segment sizes with O(n) complexity.
	 * </p>
	 */
	@Override
	public long totalSegmentSize() {
		checkValid();
		long total = 0;
		for (Memory seg = this; seg != null; seg = seg.nextSegment()) {
			total += seg.segmentSize();
		}
		return total;
	}

	public void unbindMemorySegment() {

	}
}
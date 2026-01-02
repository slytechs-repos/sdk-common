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
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.Supplier;

/**
 * Lock-free pool implementation using a CAS-based free-list.
 * 
 * <p>
 * FreeListPool manages a pool of {@link Poolable} objects using an atomic
 * free-list for thread-safe allocation and release without locks. The pool
 * supports dynamic sizing between configurable min/max capacities.
 * </p>
 * 
 * <h2>Simple Objects</h2>
 * 
 * <p>
 * For objects that don't need memory allocation, use the {@link Supplier}
 * constructor:
 * </p>
 * 
 * <pre>{@code
 * FreeListPool<MyObject> pool = new FreeListPool<>(settings, MyObject::new);
 * }</pre>
 * 
 * <h2>Objects with Memory Components</h2>
 * 
 * <p>
 * For objects that need memory segments allocated during construction, use the
 * {@link PoolableFactory} constructor. The factory receives a
 * {@link SegmentAllocator} backed by a {@link SlabAllocator}:
 * </p>
 * 
 * <pre>{@code
 * FreeListPool<Packet> pool = new FreeListPool<>(settings, allocator -> {
 * 	MemorySegment data = allocator.allocate(9000, 8);
 * 	MemorySegment desc = allocator.allocate(128, 8);
 * 	return Packet.ofFixed(DescriptorType.NET, data, desc);
 * });
 * }</pre>
 * 
 * <h2>Slab Lifecycle</h2>
 * 
 * <p>
 * Memory allocated via the factory's allocator is tracked by the pool entry.
 * When entries are evicted during contraction, slab memory is automatically
 * freed. Slabs auto-close when all their segments are freed.
 * </p>
 * 
 * <h2>Thread Safety</h2>
 * 
 * <p>
 * All public methods are thread-safe. The free-list uses CAS operations via
 * VarHandle for lock-free push/pop.
 * </p>
 *
 * @param <T> the type of poolable objects
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see Pool
 * @see Poolable
 * @see PoolableFactory
 * @see SlabAllocator
 */
public class FreeListPool<T extends Poolable> implements Pool<T> {

	private static final VarHandle HEAD;

	static {
		try {
			HEAD = MethodHandles.lookup().findVarHandle(
					FreeListPool.class, "head", PoolEntry.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	/** Null allocator for non-memory pools - throws on use */
	private static final SlabAllocator NULL_ALLOCATOR = new SlabAllocator(); // Special, closed arena constructor

	private final PoolSettings settings;
	private final PoolableFactory<T> factory;
	private final ContractionStrategy contraction;
	private final int slabSize;
	private final Metrics metrics;

	private volatile PoolEntry head;
	private volatile long capacity;
	private volatile boolean closed;

	private SlabAllocator currentSlab;

	/**
	 * Creates a free-list pool with a simple factory.
	 * 
	 * <p>
	 * Use this constructor for objects that don't need memory allocation.
	 * </p>
	 *
	 * @param settings pool configuration
	 * @param factory  simple factory to create poolable objects
	 */
	public FreeListPool(PoolSettings settings, Supplier<T> factory) {
		this(settings, PoolableFactory.of(factory));
	}

	/**
	 * Creates a free-list pool with a memory-aware factory.
	 * 
	 * <p>
	 * Use this constructor for objects that need memory segments allocated during
	 * construction. The factory receives a {@link SegmentAllocator} backed by a
	 * {@link SlabAllocator}.
	 * </p>
	 *
	 * @param settings pool configuration
	 * @param factory  factory that receives allocator for memory allocation
	 */
	public FreeListPool(PoolSettings settings, PoolableFactory<T> factory) {
		this.settings = settings;
		this.factory = factory;
		this.contraction = settings.createContractionStrategy();
		this.slabSize = computeSlabSize(settings);
		this.metrics = new Metrics();
		this.capacity = 0;
		this.closed = false;

		// Preallocate min capacity
		grow(settings.minCapacity());
	}

	@Override
	public T allocate() {
		contraction.onAllocate(this);

		// Try to pop from free-list
		PoolEntry entry = pop();

		if (entry == null) {
			// Try to grow
			if (capacity < settings.maxCapacity()) {
				grow(slabSize);
				entry = pop();
			}
		}

		if (entry == null) {
			metrics.exhaustions++;
			return null;
		}

		entry.onAllocate();
		metrics.allocations++;

		@SuppressWarnings("unchecked")
		T item = (T) entry.owner;
		return item;
	}

	@Override
	public void releaseEntry(PoolEntry entry) {
		if (entry == null || closed) {
			return;
		}

		entry.onRecycle();
		push(entry);
		metrics.releases++;

		contraction.onRelease(this);
	}

	@Override
	public long grow(long count) {
		if (closed) {
			return 0;
		}

		long maxGrowth = settings.maxCapacity() - capacity;
		long actualGrowth = Math.min(count, maxGrowth);

		if (actualGrowth <= 0) {
			return 0;
		}

		// Create slab allocator for this growth batch
		SlabAllocator growthSlab = createSlabIfNeeded();
		SlabAllocator allocator = growthSlab != null ? growthSlab : NULL_ALLOCATOR;

		long grown = 0;
		for (int i = 0; i < actualGrowth; i++) {
			// Ensure slab has capacity, create new if exhausted
			if (growthSlab != null && !growthSlab.hasCapacity()) {
				growthSlab = new SlabAllocator(settings.segmentSize(), slabSize);
				currentSlab = growthSlab;
				allocator = growthSlab;
			}

			// Factory creates object, may allocate from slab
			T item = factory.create(allocator);

			PoolEntry entry = item.poolEntry();
			entry.owner = item;
			entry.owningPool = this;

			// Track slab for eviction if memory was allocated
			if (growthSlab != null) {
				// Entry tracks the slab it was created with
				entry.bindSlab(growthSlab, null); // Segment tracked internally by slab
			}

			push(entry);
			grown++;
		}

		capacity += grown;
		if (grown > 0) {
			metrics.growthEvents++;
		}

		return grown;
	}

	/**
	 * Creates a slab allocator if memory allocation is needed.
	 */
	private SlabAllocator createSlabIfNeeded() {
		if (settings.segmentSize() <= 0) {
			return null; // No memory allocation needed
		}

		if (currentSlab == null || !currentSlab.hasCapacity()) {
			currentSlab = new SlabAllocator(settings.segmentSize(), slabSize);
		}
		return currentSlab;
	}

	@Override
	public long contractUnused(float percent) {
		long available = available();
		long count = (long) (available * percent);
		return contractUnused(count);
	}

	@Override
	public long contractUnused(long count) {
		if (closed || count <= 0) {
			return 0;
		}

		long available = available();
		long excess = capacity - settings.minCapacity();
		long maxContractable = Math.min(available, excess);
		long actualContract = Math.min(count, maxContractable);

		if (actualContract <= 0) {
			return 0;
		}

		long contracted = 0;
		for (int i = 0; i < actualContract; i++) {
			PoolEntry entry = pop();
			if (entry == null) {
				break;
			}

			entry.onEvict();
			contracted++;
		}

		capacity -= contracted;
		if (contracted > 0) {
			metrics.contractions++;
			metrics.evictions += contracted;
		}

		return contracted;
	}

	@Override
	public long minCapacity() {
		return settings.minCapacity();
	}

	@Override
	public long maxCapacity() {
		return settings.maxCapacity();
	}

	@Override
	public long capacity() {
		return capacity;
	}

	@Override
	public long available() {
		long count = 0;
		PoolEntry e = head;
		while (e != null) {
			count++;
			e = e.next;
		}
		return count;
	}

	@Override
	public long maxByteSize() {
		return settings.segmentSize();
	}

	@Override
	public PoolMetrics metrics() {
		return metrics;
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}
		closed = true;

		// Evict all entries
		PoolEntry entry;
		while ((entry = pop()) != null) {
			entry.onEvict();
		}

		// Close current slab if any
		if (currentSlab != null) {
			currentSlab.close();
			currentSlab = null;
		}

		capacity = 0;
	}

	/**
	 * Pushes an entry onto the free-list (CAS).
	 */
	private void push(PoolEntry entry) {
		PoolEntry oldHead;
		do {
			oldHead = head;
			entry.next = oldHead;
		} while (!HEAD.compareAndSet(this, oldHead, entry));
	}

	/**
	 * Pops an entry from the free-list (CAS).
	 */
	private PoolEntry pop() {
		PoolEntry oldHead;
		PoolEntry newHead;
		do {
			oldHead = head;
			if (oldHead == null) {
				return null;
			}
			newHead = oldHead.next;
		} while (!HEAD.compareAndSet(this, oldHead, newHead));

		oldHead.next = null;
		return oldHead;
	}

	/**
	 * Computes optimal slab size.
	 */
	private static int computeSlabSize(PoolSettings settings) {
		if (settings.segmentSize() <= 0) {
			return 64; // Non-memory pool, arbitrary batch size
		}

		int tenPercent = settings.maxCapacity() / 10;
		int oneMbWorth = (int) (1024 * 1024 / settings.segmentSize());
		int slabSize = Math.min(tenPercent, oneMbWorth);

		return Math.max(slabSize, 16); // Floor at 16
	}

	/**
	 * Internal metrics implementation.
	 */
	private class Metrics implements PoolMetrics {
		volatile long allocations;
		volatile long releases;
		volatile long exhaustions;
		volatile long growthEvents;
		volatile long contractions;
		volatile long evictions;

		@Override
		public long allocations() {
			return allocations;
		}

		@Override
		public long releases() {
			return releases;
		}

		@Override
		public long exhaustions() {
			return exhaustions;
		}

		@Override
		public long growthEvents() {
			return growthEvents;
		}

		@Override
		public long contractions() {
			return contractions;
		}

		@Override
		public long evictions() {
			return evictions;
		}
	}

	@Override
	public String toString() {
		return String.format("FreeListPool[capacity=%d/%d, available=%d, segment=%d]",
				capacity, settings.maxCapacity(), available(), settings.segmentSize());
	}
}
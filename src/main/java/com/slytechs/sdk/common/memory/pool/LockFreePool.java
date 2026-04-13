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
 * Lock-free pool implementation with configurable FIFO or LIFO ordering.
 * 
 * <p>
 * LockFreePool manages a pool of {@link Poolable} objects using atomic
 * operations for thread-safe allocation and release without locks. The pool
 * supports dynamic sizing between configurable min/max capacities.
 * </p>
 * 
 * <p>
 * Use the inner {@link Fifo} and {@link Lifo} subclasses to explicitly specify
 * the allocation order:
 * </p>
 * 
 * <pre>{@code
 * // LIFO (stack) ordering - most recently recycled objects allocated first
 * Pool<Packet> lifoPool = new LockFreePool.Lifo<>(settings, Packet::new);
 * 
 * // FIFO (queue) ordering - oldest recycled objects allocated first
 * Pool<Packet> fifoPool = new LockFreePool.Fifo<>(settings, Packet::new);
 * }</pre>
 * 
 * <h2>Simple Objects</h2>
 * 
 * <p>
 * For objects that don't need memory allocation, use the {@link Supplier}
 * constructor:
 * </p>
 * 
 * <pre>{@code
 * LockFreePool<MyObject> pool = new LockFreePool.Lifo<>(settings, MyObject::new);
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
 * <pre>{@snippet :
 * LockFreePool<Packet> pool = new LockFreePool.Fifo<>(settings, allocator -> {
 * 	MemorySegment data = allocator.allocate(9000, 8);
 * 	MemorySegment desc = allocator.allocate(128, 8);
 * 	return Packet.ofFixed(DescriptorType.TYPE2, data, desc);
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
public class LockFreePool<T extends Poolable> implements Pool<T> {

	protected static final VarHandle HEAD;

	static {
		try {
			HEAD = MethodHandles.lookup().findVarHandle(
					LockFreePool.class, "head", PoolEntry.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static final SlabAllocator NULL_ALLOCATOR = new SlabAllocator();

	private final PoolSettings settings;
	private final PoolableFactory<T> factory;
	private final ContractionStrategy contraction;
	private final int slabSize;
	private final Metrics metrics;

	protected volatile PoolEntry head;
	private volatile long capacity;
	private volatile boolean closed;

	private SlabAllocator currentSlab;

	/**
	 * Creates a lock-free pool with LIFO ordering and a simple factory.
	 * 
	 * <p>
	 * Use this constructor for objects that don't need memory allocation.
	 * Consider using {@link Lifo} or {@link Fifo} subclasses for explicit ordering.
	 * </p>
	 *
	 * @param settings pool configuration
	 * @param factory  simple factory to create poolable objects
	 */
	public LockFreePool(PoolSettings settings, Supplier<T> factory) {
		this(settings, PoolableFactory.of(factory));
	}

	/**
	 * Creates a lock-free pool with LIFO ordering and a memory-aware factory.
	 * 
	 * <p>
	 * Use this constructor for objects that need memory segments allocated during
	 * construction. Consider using {@link Lifo} or {@link Fifo} subclasses for
	 * explicit ordering.
	 * </p>
	 *
	 * @param settings pool configuration
	 * @param factory  factory that receives allocator for memory allocation
	 */
	public LockFreePool(PoolSettings settings, PoolableFactory<T> factory) {
		this.settings = settings;
		this.factory = factory;
		this.contraction = settings.createContractionStrategy();
		this.slabSize = computeSlabSize(settings);
		this.metrics = new Metrics();
		this.capacity = 0;
		this.closed = false;

		grow(settings.minCapacity());
	}

	@Override
	public T allocate() {
		contraction.onAllocate(this);

		PoolEntry entry = pop();

		if (entry == null) {
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

		SlabAllocator growthSlab = createSlabIfNeeded();
		SlabAllocator allocator = growthSlab != null ? growthSlab : NULL_ALLOCATOR;

		long grown = 0;
		for (int i = 0; i < actualGrowth; i++) {
			if (growthSlab != null && !growthSlab.hasCapacity()) {
				growthSlab = new SlabAllocator(settings.segmentSize(), slabSize);
				currentSlab = growthSlab;
				allocator = growthSlab;
			}

			T item = factory.create(allocator);

			PoolEntry entry = item.poolEntry();
			entry.owner = item;
			entry.owningPool = this;

			if (growthSlab != null) {
				entry.bindSlab(growthSlab, null);
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

	private SlabAllocator createSlabIfNeeded() {
		if (settings.segmentSize() <= 0) {
			return null;
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

		PoolEntry entry;
		while ((entry = pop()) != null) {
			entry.onEvict();
		}

		if (currentSlab != null) {
			currentSlab.close();
			currentSlab = null;
		}

		capacity = 0;
	}

	/**
	 * Pushes an entry onto the free-list (CAS).
	 * 
	 * <p>
	 * Default implementation provides LIFO ordering. Override for different
	 * ordering strategies.
	 * </p>
	 */
	protected void push(PoolEntry entry) {
		PoolEntry oldHead;
		do {
			oldHead = head;
			entry.next = oldHead;
		} while (!HEAD.compareAndSet(this, oldHead, entry));
	}

	/**
	 * Pops an entry from the free-list (CAS).
	 */
	protected PoolEntry pop() {
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

	private static int computeSlabSize(PoolSettings settings) {
		if (settings.segmentSize() <= 0) {
			return 64;
		}

		int tenPercent = settings.maxCapacity() / 10;
		int oneMbWorth = (int) (1024 * 1024 / settings.segmentSize());
		int slabSize = Math.min(tenPercent, oneMbWorth);

		return Math.max(slabSize, 16);
	}

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
		return String.format("LockFreePool[capacity=%d/%d, available=%d, segment=%d]",
				capacity, settings.maxCapacity(), available(), settings.segmentSize());
	}

	/**
	 * LIFO (Last-In-First-Out) pool implementation.
	 * 
	 * <p>
	 * Objects most recently recycled are allocated first. This provides better
	 * cache locality as recently used objects are more likely to still be in CPU
	 * cache.
	 * </p>
	 * 
	 * <p>
	 * This is a thin wrapper around the base {@link LockFreePool} which inherently
	 * uses LIFO ordering via stack-based push/pop operations.
	 * </p>
	 *
	 * @param <T> the type of poolable objects
	 */
	public static class Lifo<T extends Poolable> extends LockFreePool<T> {

		/**
		 * Creates a LIFO pool with a simple factory.
		 *
		 * @param settings pool configuration
		 * @param factory  simple factory to create poolable objects
		 */
		public Lifo(PoolSettings settings, Supplier<T> factory) {
			super(settings, factory);
		}

		/**
		 * Creates a LIFO pool with a memory-aware factory.
		 *
		 * @param settings pool configuration
		 * @param factory  factory that receives allocator for memory allocation
		 */
		public Lifo(PoolSettings settings, PoolableFactory<T> factory) {
			super(settings, factory);
		}

		@Override
		public String toString() {
			return String.format("LockFreePool.Lifo[capacity=%d/%d, available=%d, segment=%d]",
					capacity(), maxCapacity(), available(), maxByteSize());
		}
	}

	/**
	 * FIFO (First-In-First-Out) pool implementation.
	 * 
	 * <p>
	 * Objects are allocated in the order they were recycled. The oldest available
	 * object is allocated first. This provides more predictable aging behavior and
	 * allows objects to be pre-filled and reused after the initial allocation.
	 * </p>
	 * 
	 * <p>
	 * Internally maintains both head and tail pointers to enable efficient queue
	 * operations using CAS.
	 * </p>
	 *
	 * @param <T> the type of poolable objects
	 */
	public static class Fifo<T extends Poolable> extends LockFreePool<T> {

		private static final VarHandle TAIL;

		static {
			try {
				TAIL = MethodHandles.lookup().findVarHandle(
						Fifo.class, "tail", PoolEntry.class);
			} catch (ReflectiveOperationException e) {
				throw new ExceptionInInitializerError(e);
			}
		}

		private volatile PoolEntry tail;

		/**
		 * Creates a FIFO pool with a simple factory.
		 *
		 * @param settings pool configuration
		 * @param factory  simple factory to create poolable objects
		 */
		public Fifo(PoolSettings settings, Supplier<T> factory) {
			super(settings, factory);
		}

		/**
		 * Creates a FIFO pool with a memory-aware factory.
		 *
		 * @param settings pool configuration
		 * @param factory  factory that receives allocator for memory allocation
		 */
		public Fifo(PoolSettings settings, PoolableFactory<T> factory) {
			super(settings, factory);
		}

		@Override
		protected void push(PoolEntry entry) {
			entry.next = null;

			while (true) {
				PoolEntry currentTail = tail;
				
				if (currentTail == null) {
					if (HEAD.compareAndSet(this, null, entry)) {
						TAIL.compareAndSet(this, null, entry);
						return;
					}
				} else {
					PoolEntry next = currentTail.next;
					if (currentTail == tail) {
						if (next == null) {
							if (TAIL.compareAndSet(this, currentTail, entry)) {
								currentTail.next = entry;
								return;
							}
						} else {
							TAIL.compareAndSet(this, currentTail, next);
						}
					}
				}
			}
		}

		@Override
		public String toString() {
			return String.format("LockFreePool.Fifo[capacity=%d/%d, available=%d, segment=%d]",
					capacity(), maxCapacity(), available(), maxByteSize());
		}
	}
}
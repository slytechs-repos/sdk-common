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

import java.lang.foreign.MemorySegment;
import java.util.concurrent.locks.LockSupport;

/**
 * Holds pool management state for a {@link Poolable} object.
 * 
 * <p>
 * PoolEntry encapsulates all pool-related state for an object, including
 * free-list linkage, owning pool reference, and slab memory tracking. By
 * embedding this state in the pooled object itself (via composition), the pool
 * avoids external wrapper objects and achieves efficient CAS-based free-list
 * management.
 * </p>
 * 
 * <h2>Slab Memory Tracking</h2>
 * 
 * <p>
 * For memory-backed poolables, the entry tracks which {@link SlabAllocator}
 * provided the memory segment. This enables automatic slab lifecycle
 * management: when an entry is evicted during pool contraction, it notifies its
 * slab, which may auto-close when all its segments are freed.
 * </p>
 * 
 * <h2>Lifecycle Callbacks</h2>
 * 
 * <p>
 * PoolEntry provides two callback methods that subclasses override to manage
 * object state during pool operations:
 * </p>
 * 
 * <ul>
 * <li>{@link #onAllocate()} - Called after removal from free-list, before
 * returning to user. Use to initialize or reset object for new use.</li>
 * <li>{@link #onRecycle()} - Called before adding to free-list. Use to clear
 * object state and release any held resources.</li>
 * <li>{@link #onEvict()} - Called when entry is permanently removed from pool
 * during contraction. Use to release slab memory.</li>
 * </ul>
 * 
 * <h2>Inner Class Pattern</h2>
 * 
 * <p>
 * The recommended pattern is to use a non-static inner class that extends
 * PoolEntry. This gives the callbacks access to the enclosing object's fields:
 * </p>
 * 
 * {@snippet :
 * 	public class MyObject implements Poolable {
 * 		private String data;
 * 		private List<Item> items = new ArrayList<>();
 * 
 * 		private final PoolEntry poolEntry = new PoolEntry() {
 * 			&#64;Override
 * 			protected void onRecycle() {
 * 				data = null;
 * 				items.clear();
 * 			}
 * 		};
 * 
 * 		@Override
 * 		public PoolEntry poolEntry() {
 * 			return poolEntry;
 * 		}
 * 	}
 * }
 * 
 * <h2>Thread Safety</h2>
 * 
 * <p>
 * The {@link #next} field is accessed only by the owning {@link Pool} using CAS
 * operations. The callbacks are invoked while the entry is not on the
 * free-list, so no synchronization is needed within callback implementations.
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see Poolable
 * @see Pool
 * @see SlabAllocator
 */
public class PoolEntry {

	/** Next entry in free-list. Package-private for Pool access. */
	PoolEntry next;

	/** Owning pool reference. Null if not allocated from a pool. */
	Pool<?> owningPool;

	/** Back-reference to the Poolable object containing this entry. */
	Poolable owner;

	/** Slab that allocated memory for this entry. Null if no slab-backed memory. */
	SlabAllocator slab;

	/** Memory segment from slab. Needed for slab.free() on eviction. */
	MemorySegment segment;

	/** Thread waiting for release signal. */
	private volatile Thread waitingThread;

	/**
	 * Constructs an unowned pool entry.
	 * 
	 * <p>
	 * The entry starts with no owning pool. The pool sets ownership when the
	 * containing object is added to the pool.
	 * </p>
	 */
	public PoolEntry() {}

	/**
	 * Called after allocation from pool, before returning to user.
	 * 
	 * <p>
	 * Subclasses override this method to initialize or reset object state for a new
	 * use cycle. The entry has been removed from the free-list when this method is
	 * called.
	 * </p>
	 * 
	 * <p>
	 * Default implementation does nothing.
	 * </p>
	 */
	protected void onAllocate() {}

	/**
	 * Called before returning to pool's free-list.
	 * 
	 * <p>
	 * Subclasses override this method to clear object state and release any held
	 * resources before the object is recycled for reuse. This is called before the
	 * entry is added to the free-list.
	 * </p>
	 * 
	 * <p>
	 * Default implementation does nothing.
	 * </p>
	 */
	protected void onRecycle() {}

	/**
	 * Called when this entry is permanently evicted from the pool.
	 * 
	 * <p>
	 * This occurs during pool contraction when excess capacity is being released.
	 * The entry will not be reused. This method releases the slab memory segment,
	 * which may trigger slab auto-close.
	 * </p>
	 * 
	 * <p>
	 * Subclasses may override to perform additional cleanup, but must call
	 * {@code super.onEvict()}.
	 * </p>
	 */
	protected void onEvict() {
		freeSlab();
		owningPool = null;
		owner = null;
	}

	/**
	 * Blocks until recycle() is called or thread is interrupted.
	 * 
	 * <p>
	 * Used for zero-copy handoff where the producer must wait for the consumer to
	 * finish before recycling the buffer.
	 * </p>
	 * <p>
	 * Warning, this is a low level call intended for advanced use as it may disrupt
	 * packet distribution and cause resource exhaustion.
	 * </p>
	 */
	public void awaitRecycle() {
		waitingThread = Thread.currentThread();
		LockSupport.park();
		waitingThread = null;
	}

	/**
	 * Returns this entry's object to its owning pool.
	 * 
	 * <p>
	 * If this entry has an owning pool, the containing object is returned to that
	 * pool. The {@link #onRecycle()} callback is invoked as part of the release
	 * process.
	 * </p>
	 * 
	 * <p>
	 * If this entry has no owning pool (object was not allocated from a pool), this
	 * method does nothing.
	 * </p>
	 * <p>
	 * If this entry has a waiting thread for release, sends a release signal. Safe
	 * to call even if no thread is waiting - unpark is "sticky" so a subsequent
	 * park() will return immediately.
	 * </p>
	 * 
	 */
	public final void recycle() {
		if (owningPool != null) {
			owningPool.releaseEntry(this);
		}

		/*
		 * Signal any waiting threads on release for backends that block their producer
		 * thread and wait for the packet to be released. This is faster and more
		 * efficient than object monitor, locking or other synchronization methods.
		 */
		Thread waiter = waitingThread;
		if (waiter != null) {
			LockSupport.unpark(waiter);
		}
	}

	/**
	 * Returns the owning pool.
	 *
	 * @return the pool that owns this entry, or null if not pooled
	 */
	public final Pool<?> owningPool() {
		return owningPool;
	}

	/**
	 * Checks if this entry is owned by a pool.
	 *
	 * @return true if this entry belongs to a pool
	 */
	public final boolean isPooled() {
		return owningPool != null;
	}

	/**
	 * Checks if this entry has slab-backed memory.
	 *
	 * @return true if memory was allocated from a slab
	 */
	public final boolean hasSlabMemory() {
		return slab != null && segment != null;
	}

	/**
	 * Frees the slab memory segment.
	 * 
	 * <p>
	 * Called during eviction to release memory back to the slab. May trigger slab
	 * auto-close if this was the last outstanding segment.
	 * </p>
	 */
	final void freeSlab() {
		if (slab != null && segment != null) {
			slab.free(segment);
			slab = null;
			segment = null;
		}
	}

	/**
	 * Binds this entry to a slab and its allocated segment.
	 * 
	 * <p>
	 * Called by the pool when growing capacity using slab allocation.
	 * </p>
	 *
	 * @param slab    the slab that provided the memory
	 * @param segment the allocated memory segment
	 */
	public final void bindSlab(SlabAllocator slab, MemorySegment segment) {
		this.slab = slab;
		this.segment = segment;
	}
}
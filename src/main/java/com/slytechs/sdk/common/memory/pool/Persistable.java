/*
 * Apache License, Version 2.0
 * 
 * Copyright 2005-2025 Sly Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.slytechs.sdk.common.memory.pool;

import com.slytechs.sdk.common.memory.BindableView;
import com.slytechs.sdk.common.memory.Memory;
import com.slytechs.sdk.common.memory.ScopedMemory;

/**
 * Interface for objects that support persistence and copying operations.
 * 
 * <p>
 * Provides a unified API for managing object lifecycle across different memory
 * strategies (scoped, fixed, hybrid). Objects implementing this interface can
 * be persisted beyond their original scope, copied independently, or duplicated
 * with shared memory.
 * </p>
 * 
 * <p>
 * This interface extends {@link BindableView} (objects must be rebindable to
 * memory) and {@link Poolable} (objects participate in pool lifecycle). The
 * combination enables zero-copy capture patterns where scoped packets can be
 * selectively persisted when needed.
 * </p>
 * 
 * <h2>Implementation Requirements</h2>
 * 
 * <p>
 * Implementing classes must provide only one method:
 * </p>
 * <ul>
 * <li>{@link #newUnbound()} - Returns an unbound instance without memory</li>
 * </ul>
 * 
 * <p>
 * All other methods have default implementations. Complex objects (like
 * {@code Packet}) may override {@link #copyTo(Persistable)} and
 * {@link #duplicate(Persistable)} to handle additional components such as
 * descriptors or metadata.
 * </p>
 * 
 * <h2>Method Summary</h2>
 * 
 * <table>
 * <caption>Persistable Methods</caption>
 * <tr>
 * <th>Method</th>
 * <th>Returns</th>
 * <th>Allocates</th>
 * <th>Shares Memory</th>
 * </tr>
 * <tr>
 * <td>{@link #persist()}</td>
 * <td>Same or new</td>
 * <td>Maybe</td>
 * <td>No</td>
 * </tr>
 * <tr>
 * <td>{@link #persistTo(Persistable)}</td>
 * <td>Same or target</td>
 * <td>No</td>
 * <td>No</td>
 * </tr>
 * <tr>
 * <td>{@link #persistTo(Pool)}</td>
 * <td>Same or pooled</td>
 * <td>Maybe</td>
 * <td>No</td>
 * </tr>
 * <tr>
 * <td>{@link #copy()}</td>
 * <td>New</td>
 * <td>Always</td>
 * <td>No</td>
 * </tr>
 * <tr>
 * <td>{@link #copyTo(Persistable)}</td>
 * <td>Target</td>
 * <td>No</td>
 * <td>No</td>
 * </tr>
 * <tr>
 * <td>{@link #copyTo(Pool)}</td>
 * <td>Pooled copy</td>
 * <td>From pool</td>
 * <td>No</td>
 * </tr>
 * <tr>
 * <td>{@link #duplicate()}</td>
 * <td>New</td>
 * <td>Object only</td>
 * <td>Yes (+ref)</td>
 * </tr>
 * <tr>
 * <td>{@link #duplicate(Persistable)}</td>
 * <td>Target</td>
 * <td>No</td>
 * <td>Yes (+ref)</td>
 * </tr>
 * </table>
 * 
 * <h2>Usage Examples</h2>
 * 
 * <h3>Selective Persistence in Capture Loop</h3>
 * 
 * <pre>{@code
 * pcap.loop(-1, packet -> {
 * 	if (interesting(packet)) {
 * 		Packet keeper = packet.persist();
 * 		queue.add(keeper);
 * 	}
 * 	// Non-persisted packets released with capture buffer
 * });
 * 
 * // Consumer thread
 * while (running) {
 * 	Packet p = queue.poll();
 * 	if (p != null) {
 * 		process(p);
 * 		p.recycle(); // Return to pool
 * 	}
 * }
 * }</pre>
 * 
 * <h3>Pooled Persistence</h3>
 * 
 * <pre>{@code
 * Pool<Packet> persistPool = PacketPool.ofFixed();
 * 
 * // Persist using pool allocation
 * Packet keeper = packet.persistTo(persistPool);
 * 
 * // Or with explicit target
 * Packet target = persistPool.allocate(packet.captureLength());
 * Packet keeper = packet.persistTo(target);
 * }</pre>
 * 
 * <h3>Independent Copy</h3>
 * 
 * <pre>{@code
 * // Non-pooled copy
 * Packet copy = packet.copy();
 * archive.store(copy);
 * 
 * // Pooled copy
 * Pool<Packet> copyPool = PacketPool.ofFixed();
 * Packet copy = packet.copyTo(copyPool);
 * }</pre>
 * 
 * <h3>Shared Memory Duplicate</h3>
 * 
 * <pre>{@code
 * Packet dup = packet.duplicate();
 * // Both packet and dup share memory, ref count incremented
 * 
 * packet.recycle(); // dup still valid (refCount > 0)
 * dup.recycle(); // Memory released (refCount == 0)
 * }</pre>
 *
 * <h2>Pooling Behavior</h2>
 * 
 * <p>
 * Default {@link #persist()} and {@link #copy()} create non-pooled objects with
 * auto-managed memory. Calling {@link Poolable#recycle()} on these objects
 * is a safe no-op.
 * </p>
 * 
 * <p>
 * For pooled persistence, use {@link #persistTo(Pool)} or
 * {@link #copyTo(Pool)}. Objects allocated from pools must be recycled to
 * return to their pool.
 * </p>
 * *
 *
 * @param <T> the type of object being persisted/copied (self-referential)
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see BindableView
 * @see Poolable
 * @see Pool
 */
public interface Persistable<T extends Persistable<T>> extends BindableView, Poolable {

	/**
	 * Creates a new unbound instance of this type.
	 * 
	 * <p>
	 * Returns a "shell" instance without any memory attached. The returned object
	 * is not bound to any memory segment and must have {@link #bind(Memory)} called
	 * before use.
	 * </p>
	 * 
	 * <p>
	 * This is the only method that implementing classes must provide. All other
	 * {@code Persistable} methods have default implementations that use this method
	 * to create new instances as needed.
	 * </p>
	 * 
	 * <h3>Implementation Example</h3>
	 * 
	 * <pre>{@code
	 * public class Packet implements Persistable<Packet> {
	 * 
	 * 	@Override
	 * 	public Packet newUnbound() {
	 * 		return new Packet(); // No memory attached
	 * 	}
	 * }
	 * }</pre>
	 *
	 * @return a new unbound instance of type T
	 */
	T newUnbound();

	/**
	 * Checks if this object is already persistent (safe beyond current scope).
	 * 
	 * <p>
	 * An object is persistent if it is bound and its underlying memory is fixed
	 * (not scoped to a capture buffer or other temporary region). Scoped objects
	 * bound to native capture buffers are NOT persistent and will become invalid
	 * when the capture scope ends.
	 * </p>
	 * 
	 * <h3>Usage</h3>
	 * 
	 * <pre>{@code
	 * if (!packet.isPersistent()) {
	 * 	packet = packet.persist(); // Make it safe to keep
	 * }
	 * queue.add(packet);
	 * }</pre>
	 *
	 * @return true if object can safely outlive current scope
	 */
	default boolean isPersistent() {
		return isBound() && boundMemory().isFixed();
	}

	/**
	 * Returns an object safe to keep beyond current scope.
	 * 
	 * <p>
	 * If already persistent ({@link #isPersistent()} returns true), returns
	 * {@code this}. Otherwise creates an independent non-pooled copy with fixed
	 * memory.
	 * </p>
	 * 
	 * <p>
	 * <b>Important:</b> Caller MUST use the returned value. The original object may
	 * become invalid after the current scope ends.
	 * </p>
	 * 
	 * <h3>Usage</h3>
	 * 
	 * <pre>{@code
	 * Packet keeper = packet.persist();
	 * queue.add(keeper); // Use keeper, not original packet
	 * }</pre>
	 *
	 * @return this if already persistent, otherwise a new copy with fixed memory
	 */
	@SuppressWarnings("unchecked")
	default T persist() {
		if (isPersistent())
			return (T) this;

		return copy();
	}

	/**
	 * Persists into the provided target if not already persistent.
	 * 
	 * <p>
	 * If already persistent, returns {@code this} and the target is unused.
	 * Otherwise copies data into target and returns target. This allows
	 * pool-allocated persistence without the pool lookup overhead.
	 * </p>
	 * 
	 * <h3>Usage</h3>
	 * 
	 * <pre>{@code
	 * Pool<Packet> pool = PacketPool.ofFixed();
	 * Packet target = pool.allocate(packet.captureLength());
	 * Packet keeper = packet.persistTo(target);
	 * }</pre>
	 *
	 * @param target the object to copy into if not persistent; must have
	 *               pre-allocated fixed memory
	 * @return this if already persistent, otherwise target with copied data
	 * @throws AssertionError if target is not persistent (no fixed memory)
	 */
	@SuppressWarnings("unchecked")
	default T persistTo(T target) {
		assert target.isPersistent()
				: "Target must have preallocated fixed memory for persistence";

		if (isPersistent())
			return (T) this;

		return copyTo(target);
	}

	/**
	 * Persists using pool allocation if not already persistent.
	 * 
	 * <p>
	 * If already persistent, returns {@code this}. Otherwise allocates from the
	 * provided pool, copies data, and returns the pooled copy. This is a
	 * convenience method combining pool allocation with persistence.
	 * </p>
	 * 
	 * <h3>Usage</h3>
	 * 
	 * <pre>{@code
	 * Pool<Packet> persistPool = PacketPool.ofFixed();
	 * Packet keeper = packet.persistTo(persistPool);
	 * // keeper is either original (if was persistent) or from pool
	 * }</pre>
	 *
	 * @param pool the pool to allocate from if copy needed
	 * @return this if already persistent, otherwise pooled copy
	 * @throws AssertionError if pool allocation fails
	 */
	@SuppressWarnings("unchecked")
	default T persistTo(Pool<T> pool) {
		if (isPersistent()) {
			return (T) this;
		}
		long length = view().length();
		T target = pool.allocate(length);
		assert target != null : "Pool allocation failed";
		return persistTo(target);
	}

	/**
	 * Creates an independent copy with new fixed memory (non-pooled).
	 * 
	 * <p>
	 * Always creates a new object with its own auto-managed fixed memory,
	 * regardless of whether this object is already persistent. The original object
	 * remains unchanged.
	 * </p>
	 * 
	 * <p>
	 * The copy uses {@link Memory#of(long)} which allocates from an auto-managed
	 * arena. For pooled copies, use {@link #copyTo(Pool)} instead.
	 * </p>
	 * 
	 * <h3>Usage</h3>
	 * 
	 * <pre>{@code
	 * Packet copy = packet.copy();
	 * archive.store(copy);
	 * // Both packet and copy are valid, independent memory
	 * }</pre>
	 *
	 * @return a new independent copy with fixed memory
	 */
	default T copy() {
		long length = view().length();
		T target = newUnbound();

		Memory fixed = Memory.of(length);
		target.bind(fixed);

		return copyTo(target);
	}

	/**
	 * Copies data into the provided target.
	 * 
	 * <p>
	 * Performs a memory segment copy from this object to the target. The target
	 * must have sufficient capacity. Use this for pooled copies where you control
	 * allocation, or when reusing existing objects.
	 * </p>
	 * 
	 * <p>
	 * Complex objects (like {@code Packet}) should override this method to copy
	 * additional components such as descriptors or metadata by calling
	 * {@code super.copyTo(target)} first.
	 * </p>
	 * 
	 * <h3>Usage</h3>
	 * 
	 * <pre>{@code
	 * Pool<Packet> pool = PacketPool.ofFixed();
	 * Packet target = pool.allocate(packet.captureLength());
	 * packet.copyTo(target);
	 * }</pre>
	 * 
	 * <h3>Override Example</h3>
	 * 
	 * <pre>{@code
	 * @Override
	 * public Packet copyTo(Packet target) {
	 * 	Persistable.super.copyTo(target); // Copy data segment
	 * 
	 * 	// Copy descriptor
	 * 	target.descriptor().boundMemory().segment()
	 * 			.copyFrom(this.descriptor().boundMemory().segment());
	 * 
	 * 	return target;
	 * }
	 * }</pre>
	 *
	 * @param target the target to copy into
	 * @return the target with copied data
	 */
	default T copyTo(T target) {
		target.boundMemory()
				.segment()
				.copyFrom(this.boundMemory()
						.segment());

		return target;
	}

	/**
	 * Returns this object to its owning pool.
	 * 
	 * <p>
	 * If this object was allocated from a pool, it is returned to that pool for
	 * reuse. The {@link PoolEntry#onRecycle()} callback is invoked to clear object
	 * state before the object is added to the pool's free list.
	 * </p>
	 * 
	 * <p>
	 * If this object is not pooled ({@link PoolEntry#isPooled()} returns false),
	 * this method is a safe no-op.
	 * </p>
	 * 
	 * <h3>Usage</h3>
	 * 
	 * <pre>{@code
	 * // Pooled object
	 * Packet packet = pool.allocate();
	 * process(packet);
	 * packet.recycle(); // Returns to pool
	 * 
	 * // Non-pooled object
	 * Packet copy = packet.copy();
	 * process(copy);
	 * copy.recycle(); // Safe no-op
	 * }</pre>
	 * 
	 * @see PoolEntry#recycle()
	 * @see Poolable
	 */
	default void recycle() {
		poolEntry().recycle();
	}

	/**
	 * Creates an independent copy using pool allocation.
	 * 
	 * <p>
	 * Allocates from the provided pool and copies data into the new object. This is
	 * a convenience method combining pool allocation with copying.
	 * </p>
	 * 
	 * <h3>Usage</h3>
	 * 
	 * <pre>{@code
	 * Pool<Packet> copyPool = PacketPool.ofFixed();
	 * Packet copy = packet.copyTo(copyPool);
	 * }</pre>
	 *
	 * @param pool the pool to allocate from
	 * @return a new pooled copy
	 * @throws AssertionError if pool allocation fails
	 */
	default T copyTo(Pool<T> pool) {
		long length = view().length();

		T target = pool.allocate(length);
		assert target != null : "Pool allocation failed";
		copyTo(target);
		return target;
	}

	/**
	 * Creates a new object sharing the same underlying memory.
	 * 
	 * <p>
	 * Returns a new object bound to the same memory segment with the reference
	 * count incremented. Both the original and duplicate remain valid until
	 * recycled. The underlying memory is released only when all references are
	 * recycled.
	 * </p>
	 * 
	 * <p>
	 * This is useful for parallel processing where multiple consumers need access
	 * to the same packet data without copying.
	 * </p>
	 * 
	 * <h3>Usage</h3>
	 * 
	 * <pre>{@code
	 * Packet dup = packet.duplicate();
	 * 
	 * executor.submit(() -> {
	 * 	analyze(dup);
	 * 	dup.recycle();
	 * });
	 * 
	 * packet.recycle(); // dup still valid (refCount > 0)
	 * }</pre>
	 *
	 * @return a new object sharing the same memory (ref count incremented)
	 */
	default T duplicate() {
		T target = newUnbound();
		ScopedMemory scoped = new ScopedMemory();
		target.bind(scoped);
		return duplicate(target);
	}

	/**
	 * Creates a duplicate using the provided target, sharing memory.
	 * 
	 * <p>
	 * Binds the target to this object's memory and increments the reference count.
	 * Both objects share the same underlying memory segment.
	 * </p>
	 * 
	 * <p>
	 * Complex objects (like {@code Packet}) should override this method to share
	 * additional components such as descriptors by calling
	 * {@code super.duplicate(target)} first.
	 * </p>
	 * 
	 * <h3>Override Example</h3>
	 * 
	 * <pre>{@code
	 * @Override
	 * public Packet duplicate(Packet target) {
	 * 	Persistable.super.duplicate(target); // Share data memory
	 * 
	 * 	// Share descriptor
	 * 	descriptor().boundMemory().incrementRef();
	 * 	target.descriptor().bind(this.descriptor().boundView());
	 * 
	 * 	return target;
	 * }
	 * }</pre>
	 *
	 * @param target the target to bind to shared memory
	 * @return the target bound to shared memory
	 */
	default T duplicate(T target) {
		boundMemory().incrementRef();
		target.bind(this.boundView());

		return target;
	}
}
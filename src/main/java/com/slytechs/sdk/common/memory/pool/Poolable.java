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

/**
 * Interface for objects that can be managed by a {@link Pool}.
 * 
 * <p>
 * Poolable objects carry their own pool management state via a
 * {@link PoolEntry} instance. This enables efficient pool operations without
 * external wrapper objects and allows the pool to manage lifecycle callbacks
 * directly through the entry.
 * </p>
 * 
 * <h2>Implementation Pattern</h2>
 * 
 * <p>
 * Implementations typically use a non-static inner class extending
 * {@link PoolEntry} to receive lifecycle callbacks with access to the enclosing
 * object's state:
 * </p>
 * 
 * <pre>
 * {
 * 	&#64;code
 * 	public class Packet implements Poolable {
 * 
 * 		private final PoolEntry poolEntry = new PoolEntry() {
 * 			&#64;Override
 * 			protected void onRecycle() {
 * 				// Reset packet state for reuse
 * 				headers.clear();
 * 				descriptor.unbind();
 * 			}
 * 
 * 			&#64;Override
 * 			protected void onAllocate() {
 * 				// Initialize after allocation
 * 			}
 * 		};
 * 
 * 		@Override
 * 		public PoolEntry poolEntry() {
 * 			return poolEntry;
 * 		}
 * 	}
 * }
 * </pre>
 * 
 * <h2>Pooled vs Non-Pooled Usage</h2>
 * 
 * <p>
 * The {@link #recycle()} method works identically for pooled and non-pooled
 * objects. If the object was allocated from a pool, it returns to that pool. If
 * not, the method does nothing. This allows user code to work without checking
 * pool status:
 * </p>
 * 
 * <pre>{@code
 * // Works for both pooled and non-pooled packets
 * void processPacket(Packet packet) {
 *     try {
 *         doWork(packet);
 * } finally {
 * packet.recycle(); // Returns to pool or no-op
 * }
 * }
 * }</pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see PoolEntry
 * @see Pool
 */
public interface Poolable {

	/**
	 * Returns the pool entry for this object.
	 * 
	 * <p>
	 * The pool entry holds pool management state including free-list linkage and
	 * owning pool reference. It is used internally by {@link Pool} for lifecycle
	 * management and should not typically be accessed directly by application code.
	 * </p>
	 *
	 * @return the pool entry, never null
	 */
	PoolEntry poolEntry();

	/**
	 * Returns this object to its owning pool.
	 * 
	 * <p>
	 * If this object was allocated from a pool, it is returned to that pool for
	 * reuse. The {@link PoolEntry#onRecycle()} callback is invoked before the
	 * object is added to the pool's free-list.
	 * </p>
	 * 
	 * <p>
	 * If this object was not allocated from a pool (i.e., created directly), this
	 * method does nothing. This allows identical code paths for pooled and
	 * non-pooled objects without requiring explicit checks.
	 * </p>
	 */
	default void recycle() {
		poolEntry().recycle();
	}
}
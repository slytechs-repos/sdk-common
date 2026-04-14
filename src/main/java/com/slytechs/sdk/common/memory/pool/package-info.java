/*
 * Sly Technologies Free License
 *
 * Copyright 2024-2026 Sly Technologies Inc.
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

/**
 * Lock-free object pooling and persistence lifecycle management.
 *
 * <p>
 * This package provides two complementary systems: a lock-free object pool
 * for reusing both Java instances and native memory segments without GC
 * pressure, and the {@link Persistable} interface that unifies object
 * lifecycle management across all pooled and non-pooled objects.
 * </p>
 *
 * <h2>Two-Tier Pooling</h2>
 *
 * <p>
 * High-throughput packet processing requires eliminating all allocation on the
 * hot capture path. This package provides two independent lock-free pools that
 * work together transparently:
 * </p>
 *
 * <table>
 * <caption>Pool Tiers</caption>
 * <tr><th>Tier</th><th>What is Pooled</th><th>Key Classes</th></tr>
 * <tr><td><b>Java instance pool</b></td>
 *     <td>View objects: {@code Packet}, {@code Descriptor},
 *         {@link com.slytechs.sdk.common.memory.MemoryBuffer} — reused by
 *         rebinding to new memory without constructing new instances</td>
 *     <td>{@link Pool}, {@link Poolable}, {@link PoolEntry},
 *         {@link LockFreePool}</td></tr>
 * <tr><td><b>Memory pool</b></td>
 *     <td>Pre-allocated native off-heap memory segments — returned to pool
 *         when the last view is recycled (refCount reaches zero)</td>
 *     <td>{@link MemoryPool}, {@link SlabAllocator},
 *         {@link MemoryPoolSettings}</td></tr>
 * </table>
 *
 * <p>
 * A single {@code persist()} call in a fully pooled configuration acquires
 * one Java instance and one memory segment from their respective pools,
 * creating no garbage at all.
 * </p>
 *
 * <h2>Class and Interface Overview</h2>
 *
 * <table>
 * <caption>Pool Package Classes</caption>
 * <tr><th>Type</th><th>Role</th></tr>
 * <tr><td>{@link Pool}</td>
 *     <td>Core pool interface. Lock-free free-list with configurable min/max
 *         capacity, size-aware allocation, dynamic grow/contract, and
 *         {@link PoolMetrics} support. Returns null when exhausted rather
 *         than blocking.</td></tr>
 * <tr><td>{@link LockFreePool}</td>
 *     <td>Primary {@link Pool} implementation. Atomic CAS-based free-list,
 *         no locks on the allocation/release hot path. Supports optional
 *         {@link ContractionStrategy} for automatic capacity management.</td></tr>
 * <tr><td>{@link BucketPool}</td>
 *     <td>Size-bucketed pool for variable-size allocations. Routes
 *         allocation to the appropriate size bucket. Individual buckets may
 *         be exhausted independently — callers must check for null
 *         returns.</td></tr>
 * <tr><td>{@link MemoryPool}</td>
 *     <td>Specialized pool for
 *         {@link com.slytechs.sdk.common.memory.FixedMemory} segments.
 *         Pre-allocates native memory from a {@link SlabAllocator} and
 *         manages both Java instances and their backing memory segments
 *         in a single pool.</td></tr>
 * <tr><td>{@link SlabAllocator}</td>
 *     <td>Allocates large contiguous native memory slabs and carves them
 *         into fixed-size segments for pool backing. Minimizes JNI overhead
 *         and memory fragmentation.</td></tr>
 * <tr><td>{@link Poolable}</td>
 *     <td>Marker interface for objects managed by a {@link Pool}. Carries
 *         its own {@link PoolEntry} for pool state — no external wrapper
 *         objects required. {@code recycle()} works identically for pooled
 *         and non-pooled objects.</td></tr>
 * <tr><td>{@link PoolEntry}</td>
 *     <td>Pool management state embedded in each {@link Poolable} object.
 *         Holds free-list linkage and owning pool reference. Provides
 *         {@code onRecycle()} and {@code onAllocate()} lifecycle callbacks
 *         implemented via non-static inner class in the enclosing
 *         object.</td></tr>
 * <tr><td>{@link PoolableFactory}</td>
 *     <td>Factory interface for creating new poolable instances. Used by
 *         pool implementations to expand capacity on demand.</td></tr>
 * <tr><td>{@link Persistable}</td>
 *     <td>Unified lifecycle API for all view objects. Single abstract method
 *         ({@code newUnbound()}) with complete default implementations for
 *         all persist, copy, duplicate, and recycle operations.
 *         Pool-agnostic — same code works for pooled and non-pooled
 *         objects.</td></tr>
 * <tr><td>{@link PoolSettings}</td>
 *     <td>Configuration for Java instance pools: initial capacity, min/max
 *         capacity, preallocate flag, and contraction strategy.</td></tr>
 * <tr><td>{@link MemoryPoolSettings}</td>
 *     <td>Extends {@link PoolSettings} with memory-specific configuration:
 *         segment size, slab size, and alignment.</td></tr>
 * <tr><td>{@link PoolMetrics}</td>
 *     <td>Runtime pool statistics: allocation count, recycle count, miss
 *         count (pool exhausted), current utilization, and contraction
 *         events.</td></tr>
 * <tr><td>{@link ContractionStrategy}</td>
 *     <td>Strategy interface for automatic pool capacity reduction when
 *         excess capacity is idle. Applied periodically to avoid unbounded
 *         pool growth.</td></tr>
 * <tr><td>{@link ActiveContractionStrategy}</td>
 *     <td>Time-based contraction — reduces capacity when objects have been
 *         idle for a configurable duration.</td></tr>
 * <tr><td>{@link DisabledContractionStrategy}</td>
 *     <td>No-op strategy for fixed-capacity pools that never contract.
 *         Appropriate for pre-warmed pools in steady-state high-throughput
 *         scenarios.</td></tr>
 * </table>
 *
 * <h2>Poolable Implementation Pattern</h2>
 *
 * <p>
 * Implementing classes embed a {@link PoolEntry} using a non-static inner
 * class to receive lifecycle callbacks with access to the enclosing object's
 * state:
 * </p>
 *
 * {@snippet lang=java :
 * public class Packet implements Persistable<Packet> {
 *
 *     private final PoolEntry poolEntry = new PoolEntry() {
 *         @Override
 *         protected void onRecycle() {
 *             // Clear state before returning to free-list
 *             headers.clear();
 *             descriptor.unbind();
 *             boundMemory().unbindIfScoped();
 *         }
 *
 *         @Override
 *         protected void onAllocate() {
 *             // Optional: initialize after allocation
 *         }
 *     };
 *
 *     @Override
 *     public PoolEntry poolEntry() { return poolEntry; }
 *
 *     @Override
 *     public Packet newUnbound() { return new Packet(); }
 * }
 * }
 *
 * <h2>The Persistable Contract</h2>
 *
 * <p>
 * {@link Persistable} provides a pool-agnostic API for managing view object
 * lifecycle. The same user code works for pooled and non-pooled objects, and
 * for all capture backends (libpcap, DPDK, NTAPI).
 * </p>
 *
 * <table>
 * <caption>Persistable Method Contracts</caption>
 * <tr><th>Method</th><th>Source State</th><th>Behavior</th><th>Returns</th></tr>
 * <tr><td rowspan="2">{@code persist()}</td>
 *     <td>Already persistent (FixedMemory)</td>
 *     <td>Increment refCount, no-op</td><td>this</td></tr>
 * <tr><td>Scoped (ScopedMemory)</td>
 *     <td>Pool allocation + copy, or heap copy</td><td>New instance</td></tr>
 * <tr><td rowspan="2">{@code persistTo(target)}</td>
 *     <td>Already persistent</td>
 *     <td>No-op, target unused</td><td>this</td></tr>
 * <tr><td>Scoped</td>
 *     <td>Copy to pre-allocated target</td><td>target</td></tr>
 * <tr><td rowspan="2">{@code persistTo(pool)}</td>
 *     <td>Already persistent</td>
 *     <td>No-op</td><td>this</td></tr>
 * <tr><td>Scoped</td>
 *     <td>Pool allocation + copy. May return null if BucketPool
 *         bucket exhausted.</td><td>Pooled copy or null</td></tr>
 * <tr><td>{@code copy()}</td>
 *     <td>Any</td>
 *     <td>Always allocates new FixedMemory and copies</td>
 *     <td>New non-pooled instance</td></tr>
 * <tr><td>{@code copyTo(target)}</td>
 *     <td>Any</td>
 *     <td>Raw segment copy into pre-allocated target</td>
 *     <td>target</td></tr>
 * <tr><td>{@code copyTo(pool)}</td>
 *     <td>Any</td>
 *     <td>Pool allocation + copy; always copies</td>
 *     <td>Pooled copy</td></tr>
 * <tr><td>{@code duplicate()}</td>
 *     <td>Any</td>
 *     <td>Increment refCount; new view over same memory</td>
 *     <td>New instance, shared memory</td></tr>
 * <tr><td rowspan="2">{@code recycle()}</td>
 *     <td>Pooled</td>
 *     <td>Decrement refCount; return to pool when zero</td><td>void</td></tr>
 * <tr><td>Non-pooled</td>
 *     <td>Safe no-op</td><td>void</td></tr>
 * </table>
 *
 * <h2>Packet Persistence — Three-Tier Resolution</h2>
 *
 * <p>
 * {@code persist()} resolves through three tiers to find the minimum work
 * necessary:
 * </p>
 *
 * <pre>
 * packet.persist()
 *    |
 *    +-- isPersistent() == true?
 *    |     --&gt; increment refCount, return this      (zero work)
 *    |
 *    +-- pool configured and available?
 *    |     --&gt; acquire Packet instance from instance pool   (no new)
 *    |     --&gt; acquire FixedMemory from memory pool         (no malloc)
 *    |     --&gt; copy packet data + descriptor independently
 *    |     --&gt; bind view, set refCount = 1
 *    |     --&gt; return pooled instance               (no GC pressure)
 *    |
 *    +-- fallback
 *          --&gt; Memory.of(length)  (auto-managed arena)
 *          --&gt; copy data
 *          --&gt; return non-pooled instance
 * </pre>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Selective Persistence</h3>
 *
 * {@snippet :
 * pcap.loop(-1, packet -> {
 *     if (interesting(packet)) {
 *         Packet kept = packet.persist();   // pool reuse or heap copy
 *         queue.add(kept);
 *     }
 * });
 *
 * Packet p = queue.poll();
 * process(p);
 * p.recycle();  // returns to pool, or no-op if non-pooled
 * }
 *
 * <h3>Pooled Persistence for High-Volume Capture</h3>
 *
 * {@snippet :
 * Pool<Packet> pool = PacketPool.ofFixedSize(
 *     new PoolSettings().capacity(10_000).preallocate(true),
 *     9000);  // 9KB segments
 *
 * pcap.loop(-1, packet -> {
 *     if (interesting(packet) && pool.available() > 0) {
 *         Packet kept = packet.persistTo(pool);
 *         queue.add(kept);
 *     }
 * });
 *
 * Packet p = queue.poll();
 * process(p);
 * p.recycle();  // returns instance + memory to respective pools
 * }
 *
 * <h3>Parallel Analysis with Duplicates</h3>
 *
 * {@snippet :
 * void analyzeInParallel(Packet packet) {
 *     Packet dup1 = packet.duplicate();  // refCount = 2
 *     Packet dup2 = packet.duplicate();  // refCount = 3
 *
 *     executor.submit(() -> { analyze(dup1); dup1.recycle(); });
 *     executor.submit(() -> { analyze(dup2); dup2.recycle(); });
 *
 *     packet.recycle();  // refCount-- ; pool release when all reach zero
 * }
 * }
 *
 * <h3>Bucketed Pool — Always Check for Null</h3>
 *
 * {@snippet :
 * Pool<Packet> pool = PacketPool.ofDefaultBuckets();
 *
 * pcap.loop(-1, packet -> {
 *     Packet kept = packet.persistTo(pool);
 *     if (kept != null) {
 *         queue.add(kept);
 *     } else {
 *         // Specific size bucket exhausted
 *         droppedCount.incrementAndGet();
 *     }
 * });
 * }
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 * @see Pool
 * @see Poolable
 * @see PoolEntry
 * @see Persistable
 * @see LockFreePool
 * @see BucketPool
 * @see MemoryPool
 * @see com.slytechs.sdk.common.memory
 */
package com.slytechs.sdk.common.memory.pool;
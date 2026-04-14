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
 * High-performance memory management for zero-allocation network packet
 * processing.
 *
 * <p>
 * This package provides the core memory abstraction layer used throughout the
 * jNetPcap, jNetWorks, and Vantage SDKs. The design targets 100M+ packets per
 * second by eliminating object allocation and garbage collection pressure from
 * hot capture paths.
 * </p>
 *
 * <h2>Design Philosophy — Views Over Memory</h2>
 *
 * <p>
 * Every user-facing object — {@code Packet}, {@code Header}, {@code Descriptor},
 * {@link MemoryBuffer} — is a <em>view</em> over physical memory, never an
 * owner of it. The view layer decouples the Java object from the memory it
 * currently describes, enabling three critical properties:
 * </p>
 * <ul>
 * <li><b>Zero-copy capture</b> — views bind directly to native capture buffers
 *     without copying a single byte on the hot path</li>
 * <li><b>Java instance reuse</b> — view objects are recycled from lock-free
 *     pools and rebound to new memory, eliminating object construction in hot
 *     paths</li>
 * <li><b>Safe persistence</b> — when a packet must outlive its callback scope,
 *     the minimum work is performed: no-op if already persistent, pool
 *     allocation if available, heap fallback as last resort</li>
 * </ul>
 *
 * <h2>Memory Types</h2>
 *
 * <p>Physical memory is one of two kinds:</p>
 *
 * <table>
 * <caption>Memory Types</caption>
 * <tr><th>Type</th><th>Ownership</th><th>Lifetime</th><th>Use Case</th></tr>
 * <tr><td>{@link ScopedMemory}</td><td>Borrowed</td>
 *     <td>Duration of one dispatch callback</td>
 *     <td>Zero-copy binding to native capture buffer, DPDK mbuf, or NTAPI
 *         host buffer. Never allocates.</td></tr>
 * <tr><td>{@link FixedMemory}</td><td>Owned, ref-counted</td>
 *     <td>Until refCount reaches zero</td>
 *     <td>Persistent packets, pool-allocated segments, auto-managed arena
 *         allocations.</td></tr>
 * </table>
 *
 * <h2>Interface and Class Overview</h2>
 *
 * <table>
 * <caption>Memory Package Classes</caption>
 * <tr><th>Type</th><th>Role</th></tr>
 * <tr><td>{@link MemoryWindow}</td>
 *     <td>Defines segment boundaries ({@code byteOffset}, {@code byteSize})
 *         and mutable data region ({@code start}, {@code end}, {@code length}).
 *         Includes headroom/tailroom for DPDK-style header prepend/append
 *         without copying. Aggregates across segment chains via
 *         {@code totalLength()} and {@code totalSize()}.</td></tr>
 * <tr><td>{@link MemoryView}</td>
 *     <td>Lightweight positioning container — segment, start, length, and
 *         optional source reference for ref-count delegation. Pre-allocated
 *         and reused; never created in hot paths.</td></tr>
 * <tr><td>{@link Memory}</td>
 *     <td>Core interface extending {@link MemoryWindow} with ownership
 *         semantics: reference counting, native segment access, and chain
 *         support for scatter-gather I/O.</td></tr>
 * <tr><td>{@link MemoryRefCounter}</td>
 *     <td>Thread-safe atomic reference counting interface. Provides
 *         {@code incrementRef()}, {@code decrementRef()}, and
 *         {@code refCount()}.</td></tr>
 * <tr><td>{@link AbstractMemory}</td>
 *     <td>Base implementation providing reference counting, chain support,
 *         and boundary validation. Extended by {@link FixedMemory} and
 *         {@link ScopedMemory}.</td></tr>
 * <tr><td>{@link FixedMemory}</td>
 *     <td>Owned memory with reference counting. Backed by a pre-allocated
 *         native segment or auto-managed arena. Released to pool or freed
 *         when refCount reaches zero.</td></tr>
 * <tr><td>{@link ScopedMemory}</td>
 *     <td>Borrowed memory bound to an external native buffer. No allocation,
 *         no copy. Invalidated when the owning scope (capture callback, DPDK
 *         mbuf, NTAPI stream) ends.</td></tr>
 * <tr><td>{@link MemoryBuffer}</td>
 *     <td>Poolable buffer with full java.nio.Buffer-style positioning
 *         (position, limit, capacity, mark). Supports high-speed inline
 *         operations and pool integration.</td></tr>
 * <tr><td>{@link MemoryHandle}</td>
 *     <td>Rebindable memory access proxy. Provides zero-allocation
 *         repositioning by updating the internal {@link MemoryView} in
 *         place rather than allocating a new object.</td></tr>
 * <tr><td>{@link MemoryStructure}</td>
 *     <td>Structured memory layout support for protocol headers and
 *         descriptors with typed field access.</td></tr>
 * <tr><td>{@link ChainedBuffer}</td>
 *     <td>Multi-segment buffer supporting scatter-gather operations, IP
 *         fragment reassembly, and zero-copy concatenation through linked
 *         {@link Memory} chains.</td></tr>
 * <tr><td>{@link ChainUtils}</td>
 *     <td>Utility methods for navigating and operating on memory segment
 *         chains.</td></tr>
 * <tr><td>{@link BindableView}</td>
 *     <td>Interface for view objects that can be bound and rebound to memory
 *         regions. The rebind operation updates positioning in place — the
 *         key mechanism behind zero-allocation header access via
 *         {@code hasHeader()}.</td></tr>
 * <tr><td>{@link BoundView}</td>
 *     <td>Extends {@link BindableView} with {@code isBound()},
 *         {@code boundMemory()}, and {@code boundView()} — the state queries
 *         used by {@code isPersistent()} and {@code persist()}.</td></tr>
 * <tr><td>{@link MemorySize}</td>
 *     <td>Size value type with unit support. Used for configuration of pool
 *         capacities, buffer sizes, and segment lengths.</td></tr>
 * <tr><td>{@link MemoryUnit}</td>
 *     <td>Units of memory measurement (bytes, KB, MB, GB). Used throughout
 *         the SDK for human-readable size configuration.</td></tr>
 * <tr><td>{@link BufferMetrics}</td>
 *     <td>Runtime metrics for buffer operations — allocation counts,
 *         hit/miss rates, and utilization statistics.</td></tr>
 * <tr><td>{@link BufferOperationException}</td>
 *     <td>Thrown when a buffer operation violates boundaries or is performed
 *         on an invalid (recycled) object.</td></tr>
 * </table>
 *
 * <h2>Memory Layout</h2>
 *
 * <p>
 * Each segment has immutable capacity boundaries and mutable data boundaries,
 * modeled after DPDK's {@code rte_mbuf} headroom/tailroom design:
 * </p>
 *
 * <pre>
 * Segment: [  headroom  ][    active data    ][  tailroom  ]
 *          |             |                    |            |
 *     byteOffset      start()              end()    byteOffset+byteSize
 *
 * headroom = start() - byteOffset            (space for prepending headers)
 * tailroom = (byteOffset + byteSize) - end() (space for appending data)
 * length   = end() - start()                 (current active data)
 * </pre>
 *
 * <p>
 * Prepending a protocol header is an O(1) pointer move — no copy, no shift:
 * </p>
 *
 * {@snippet :
 * if (memory.headroom() >= VLAN_TAG_LENGTH) {
 *     memory.start(memory.start() - VLAN_TAG_LENGTH);
 *     // Write VLAN tag at memory.start() — existing data untouched
 * }
 * }
 *
 * <h2>View Binding in the Hot Path</h2>
 *
 * <p>
 * Inside a dispatch callback, {@code Packet} and {@code Descriptor} are view
 * objects rebound to the native capture buffer. No Java objects are created.
 * Protocol headers are likewise rebound in place by {@code hasHeader()}:
 * </p>
 *
 * <pre>
 * Native Capture Buffer / DPDK Hugepage / NTAPI Host Buffer
 * +----------------------------------------------------------+
 * |  pkt[0]  |  pkt[1]  |  pkt[2]  |  ...                    |
 * +----------------------------------------------------------+
 *      |
 *      | ScopedMemory.bind() -- zero-copy, zero allocation
 *      |
 * +----+------------------+   +-----------------------------+
 * | Packet (reused)       |   | Descriptor (reused)         |
 * |  MemoryView:          |   |  MemoryView:                |
 * |   segment --&gt; buf     |   |   segment --&gt; buf           |
 * |   start   --&gt; offset  |   |   start   --&gt; desc offset   |
 * |   length  --&gt; capLen  |   |   length  --&gt; descLen       |
 * +-----------------------+   +-----------------------------+
 * Ip4, Tcp rebound by hasHeader() -- all zero allocation
 * </pre>
 *
 * <h2>Reference Counting</h2>
 *
 * <p>
 * {@link FixedMemory} uses atomic reference counting. The count starts at 1
 * on allocation and falls to 0 when all views are recycled, at which point
 * the segment is returned to its pool or arena.
 * </p>
 *
 * {@snippet :
 * Memory memory = pool.allocate();    // refCount = 1
 * memory.incrementRef();              // refCount = 2 -- share with worker
 * worker.process(memory);
 * memory.decrementRef();              // refCount = 1
 * // ... later in worker:
 * memory.decrementRef();              // refCount = 0 --> returned to pool
 * }
 *
 * <h2>Backend Portability</h2>
 *
 * <p>
 * The {@link ScopedMemory}/{@link FixedMemory} abstraction is designed to work
 * identically across all capture backends:
 * </p>
 *
 * <table>
 * <caption>Backend Memory Mapping</caption>
 * <tr><th>Backend</th><th>Native Memory</th><th>Memory Type</th>
 *     <th>isPersistent()</th></tr>
 * <tr><td>libpcap</td><td>Kernel capture ring buffer</td>
 *     <td>{@link ScopedMemory}</td>
 *     <td>false — persist() copies to FixedMemory</td></tr>
 * <tr><td>DPDK</td><td>Hugepage mempool (rte_mbuf)</td>
 *     <td>{@link FixedMemory}</td>
 *     <td>true — persist() is a no-op</td></tr>
 * <tr><td>Napatech NTAPI</td><td>Host buffer + NT stream descriptor</td>
 *     <td>{@link FixedMemory}</td>
 *     <td>true — persist() is a no-op</td></tr>
 * </table>
 *
 * <p>
 * User code is identical regardless of backend. The abstraction layer selects
 * the correct behavior automatically.
 * </p>
 *
 * <h2>When to Use Pools</h2>
 *
 * <table>
 * <caption>Performance Guidance</caption>
 * <tr><th>Packet Rate</th><th>Recommendation</th></tr>
 * <tr><td>Under ~1M pps</td>
 *     <td>Default settings — JIT and GC handle allocation efficiently</td></tr>
 * <tr><td>1–10M pps</td>
 *     <td>Enable packet pool to reduce GC pauses under sustained load</td></tr>
 * <tr><td>10M+ pps</td>
 *     <td>Full pooling essential — packet instances, descriptor instances,
 *         and memory segments all pooled</td></tr>
 * <tr><td>100M+ pps (jNetWorks/Vantage)</td>
 *     <td>Lock-free pools mandatory; DPDK/NTAPI backends provide
 *         pre-persistent packets — persist() becomes a true no-op</td></tr>
 * </table>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 * @see Memory
 * @see MemoryWindow
 * @see MemoryView
 * @see ScopedMemory
 * @see FixedMemory
 * @see MemoryBuffer
 * @see BindableView
 * @see BoundView
 * @see com.slytechs.sdk.common.memory.pool
 */
package com.slytechs.sdk.common.memory;
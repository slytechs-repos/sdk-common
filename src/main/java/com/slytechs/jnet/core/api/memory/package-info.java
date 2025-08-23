/**
 * High-performance memory management API for zero-allocation network packet processing.
 * 
 * <p>This package provides a comprehensive memory management system built on Java's Foreign Function
 * &amp; Memory (Panama FFM) API, designed for sustained throughput exceeding 100 million packets per second.
 * The API supports both single memory segments and chained segment structures with reference counting, memory
 * pooling, and efficient editing capabilities.
 * 
 * <h2>Getting Started</h2>
 * 
 * <p>The simplest way to begin using the Memory API is with the factory methods:
 * 
 * <pre>{@code
 * // Allocate a new MemoryBuffer
 * MemoryBuffer buffer = Memory.of(2048);  // Allocates 2KB buffer
 * 
 * // Use buffer for data operations
 * buffer.put("Hello World".getBytes());
 * buffer.flip();  // Prepare for reading
 * 
 * // Check basic properties
 * System.out.println("Capacity: " + buffer.capacity());
 * System.out.println("Active bytes: " + buffer.activeBytesLength());
 * 
 * // Release when done
 * buffer.decrementRef();  // Returns to internal pool or frees
 * }</pre>
 * 
 * <p>For high-performance scenarios, use memory pools:
 * 
 * <pre>{@code
 * // Create a memory pool for sustained allocation
 * MemoryPool<MemoryBuffer> pool = new MemoryPool<>(
 *     "packet-processing-pool",    // Named resource for monitoring
 *     2048,                        // Segment size (typical packet size)
 *     1000,                        // Number of pre-allocated segments
 *     128,                         // Default headroom
 *     128,                         // Default tailroom
 *     Arena.global(),              // Memory arena
 *     MemoryBuffer::new        // Factory method
 * );
 * 
 * // Allocate from pool - O(1) operation
 * MemoryBuffer buffer = pool.allocate();
 * try {
 *     // Use buffer for packet processing...
 *     buffer.put(packetData);
 * } finally {
 *     buffer.decrementRef();  // Auto-return to pool when refcount reaches 0
 * }
 * }</pre>
 * 
 * <h2>Key Concepts</h2>
 * 
 * <h3>Regions vs Projections</h3>
 * <p>The API distinguishes between <em>regions</em> (local segment properties) and 
 * <em>projections</em> (chain-wide views):
 * 
 * <pre>{@code
 * REGIONS (Per-Segment):
 * |---------- Segment Region (Fixed) --------|
 * | headroom |--- Active Bytes ---| tailroom |
 *            ↑                    ↑
 *     activeBytesStart      activeBytesEnd
 * 
 * PROJECTIONS (Across Chain):
 * [Segment 1] → [Segment 2] → [Segment 3]
 *     ↑             ↑             ↑
 * position=100  position=400  position=700
 * |------------- capacity (chain-wide) --------------|
 * |------ limit -------|
 * 
 * // Regions: segmentOffset() to segmentEnd() - per-segment bounds
 * // Active region: activeBytesStart() to activeBytesEnd() - data within segment
 * // Projections: position(), limit(), capacity() - across entire chain
 * }</pre>
 * 
 * <p>This design enables efficient space management similar to DPDK's rte_mbuf, where headroom
 * and tailroom can be used for in-place expansion without data movement.
 * 
 * <h3>Reference Counting</h3>
 * <p>All memory objects use atomic reference counting for safe resource management:
 * 
 * <pre>{@code
 * MemoryBuffer original = Memory.of(2048);    // refcount = 1
 * Memory shared = original.incrementRef();        // refcount = 2
 * 
 * // When refcount reaches 0, memory is automatically released to pool
 * shared.decrementRef();    // refcount = 1
 * original.decrementRef();  // refcount = 0, returned to pool or freed
 * }</pre>
 * 
 * <h3>Chained Segments</h3>
 * <p>Memory objects can be linked together to handle fragmented data without copying:
 * 
 * <pre>{@code
 * MemoryBuffer segment1 = Memory.of(1500);
 * MemoryBuffer segment2 = Memory.of(1500);
 * segment1.setNextMemory(segment2);  // Create chain
 * 
 * // Segment chain operations work across all segments
 * long totalActiveBytes = segment1.chainedActiveBytesLength();
 * int segmentCount = segment1.chainedSegmentCount();
 * 
 * // Buffer operations transparently span segments
 * segment1.position(1400);
 * segment1.putLong(0x123456789ABCDEFL);  // Automatically spans to segment2
 * }</pre>
 * 
 * <h3>Type Safety Through Interfaces</h3>
 * <p>The API uses interface composition to prevent misuse:
 * 
 * <ul>
 * <li>{@link MemoryView} - Read-only access and navigation</li>
 * <li>{@link MemoryWindow} - Region and projection information</li>
 * <li>{@link MemoryRef} - Reference counting and lifecycle</li>
 * <li>{@link Memory} - Complete memory abstraction (extends all three)</li>
 * </ul>
 * 
 * <p>{@link MemoryProxy} provides flexible, efficient access to chained segments without
 * buffer-style positioning, ideal for protocol parsing across segment boundaries.
 * 
 * <h2>MemoryBuffer - Buffer-Style Operations</h2>
 * 
 * <p>{@link MemoryBuffer} provides efficient buffer manipulation with get/put accessors
 * mimicking java.nio.ByteBuffer operations, but with multi-segment support:
 * 
 * <h3>Position, Limit, and Capacity</h3>
 * 
 * <pre>{@code
 * MemoryBuffer buffer = pool.allocate();
 * 
 * // Position/limit/capacity operate within totalActiveBytes region
 * buffer.position(100)     // Set position within active bytes
 *       .limit(500)        // Set limit within active bytes  
 *       .mark();           // Mark current position
 * 
 * // These are projections across the entire chain
 * long cap = buffer.capacity();      // Total active bytes across chain
 * long pos = buffer.position();      // Current position in chain
 * long lim = buffer.limit();         // Current limit in chain
 * 
 * // Buffer state
 * long remaining = buffer.remaining();     // Bytes between position and limit
 * boolean hasData = buffer.hasRemaining(); // Check for remaining data
 * }</pre>
 * 
 * <h3>Data Access Operations</h3>
 * 
 * <pre>{@code
 * // Relative put operations (advance position automatically)
 * buffer.put((byte) 0x42)          // Write byte, position += 1
 *       .putShort((short) 0x1234)  // Write short, position += 2
 *       .putInt(0x12345678)        // Write int, position += 4
 *       .putLong(0x123456789ABCDEFL) // Write long, position += 8
 *       .put(byteArray);           // Write array, position += array.length
 * 
 * // Absolute put operations (don't change position)
 * buffer.put(100, (byte) 0x42)     // Write byte at index 100
 *       .putInt(200, 0x12345678);  // Write int at index 200
 * 
 * // Network byte order operations
 * buffer.putShortBE((short) 0x0800)  // Write EtherType in big-endian
 *       .putIntBE(0x0A000001)        // Write IP address in network order
 *       .putLongBE(timestamp);        // Write timestamp in network order
 * 
 * // Get operations (advance position automatically)
 * byte b = buffer.get();             // Read byte, position += 1
 * short s = buffer.getShort();       // Read short, position += 2
 * int etherType = buffer.getShortBE() & 0xFFFF; // Read network byte order
 * buffer.get(destinationArray);      // Read into array
 * 
 * // Operations transparently span segments
 * buffer.position(segmentSize - 4);
 * buffer.putLong(0x123456789ABCDEFL); // Automatically spans segments
 * }</pre>
 * 
 * <h3>Buffer Editing Operations - InsertSpace and RemoveSpace</h3>
 * 
 * <pre>{@code
 * // InsertSpace - creates gaps for data insertion
 * buffer.position(12);
 * buffer.insertSpace(4);        // Creates 4-byte gap at position
 * buffer.putIntBE(0x8100BEEF);  // Write VLAN header in gap
 * 
 * // The insertSpace algorithm:
 * // 1. Try to expand into headroom or tailroom (no data movement)
 * // 2. Move smaller data portion to create space (left or right)
 * // 3. Push overflow to next segment's headroom if available
 * // 4. Allocate new segment(s) from pool if needed
 * 
 * // RemoveSpace - removes bytes from buffer
 * buffer.position(100);
 * buffer.removeSpace(20);       // Remove 20 bytes starting at position
 * 
 * // Split - divides segment into two with expansion space
 * buffer.position(1500);
 * buffer.split();               // Creates new segment at position
 *                              // Both segments get optimal headroom/tailroom
 * }</pre>
 * 
 * <h3>Native Segment Binding</h3>
 * 
 * <pre>{@code
 * // Bind buffer to external memory (zero-copy)
 * MemorySegment dpdkMbuf = ...; // From DPDK rx_burst
 * buffer.rewrap(dpdkMbuf, 0, packetLength);
 * 
 * // Process packet using same buffer instance
 * int etherType = buffer.getShortBE(12) & 0xFFFF;
 * 
 * // If modifications needed, insertSpace allocates from pool
 * if (needsVlan) {
 *     buffer.position(12);
 *     buffer.insertSpace(4);    // Allocates from linked pool
 *     buffer.putIntBE(vlanTag);
 * }
 * 
 * // Release native memory when done
 * buffer.decrementRef();        // Calls native release handler
 * }</pre>
 * 
 * <h2>MemoryProxy - Flexible Chain Access</h2>
 * 
 * <p>{@link MemoryProxy} provides efficient, full chain accessible access to segments
 * without buffer-style positioning constraints:
 * 
 * <h3>Basic Proxy Usage</h3>
 * 
 * <pre>{@code
 * // Create reusable proxy (typically as instance field)
 * MemoryProxy proxy = new MemoryProxy();
 * 
 * // Bind to memory region (can span segments)
 * proxy.bindMemory(packet, 0, packet.chainedActiveBytesLength());
 * 
 * // Access data at any offset efficiently
 * byte b = proxy.getByte(1000);        // Direct access at offset 1000
 * short s = proxy.getShort(1500);      // May span segments
 * int ip = proxy.getInt(26);           // Source IP at offset 26
 * 
 * // Read into arrays across segments
 * byte[] payload = new byte[1000];
 * proxy.getBytes(100, payload);        // Read from offset 100
 * 
 * // Unbind for reuse
 * proxy.unbindMemory();
 * }</pre>
 * 
 * <h3>Protocol Parsing with Proxies</h3>
 * 
 * <pre>{@code
 * public class PacketParser {
 *     private final MemoryProxy ethProxy = new MemoryProxy();
 *     private final MemoryProxy ipProxy = new MemoryProxy();
 *     private final MemoryProxy tcpProxy = new MemoryProxy();
 *     
 *     public void parse(Memory packet) {
 *         // Bind to entire packet for flexible access
 *         ethProxy.bindMemory(packet, 0, packet.chainedActiveBytesLength());
 *         
 *         // Parse Ethernet
 *         int etherType = ethProxy.getShortBE(12) & 0xFFFF;
 *         
 *         if (etherType == 0x0800) {  // IPv4
 *             // Bind IP proxy to IP header region
 *             ipProxy.bindMemory(packet, 14, 20);
 *             
 *             int ipProto = ipProxy.getByte(9) & 0xFF;
 *             int ipHdrLen = (ipProxy.getByte(0) & 0x0F) * 4;
 *             
 *             if (ipProto == 6) {  // TCP
 *                 // TCP header may span segments in jumbo frames
 *                 tcpProxy.bindMemory(packet, 14 + ipHdrLen, 20);
 *                 int srcPort = tcpProxy.getShortBE(0) & 0xFFFF;
 *                 int dstPort = tcpProxy.getShortBE(2) & 0xFFFF;
 *             }
 *         }
 *     }
 * }
 * }</pre>
 * 
 * <h2>Memory Pools for High Performance</h2>
 * 
 * <p>Memory pools provide zero-allocation operation during runtime by pre-allocating
 * all memory at pool creation time:
 * 
 * <h3>Creating Pools</h3>
 * 
 * <pre>{@code
 * // Standard pool with Arena allocator
 * MemoryPool<MemoryBuffer> standardPool = new MemoryPool<>(
 *     "ingress-packet-pool",
 *     2048,                        // Segment size
 *     10000,                       // Segment count
 *     128,                         // Default headroom
 *     128,                         // Default tailroom
 *     Arena.global(),              // Arena for allocation
 *     MemoryBuffer::new        // Factory method
 * );
 * 
 * // Pool with custom backend allocator
 * MemoryAllocator dpdkAllocator = new DpdkMemoryAllocator("dpdk-pool", rteMempool);
 * MemoryPool<MemoryBuffer> dpdkPool = new MemoryPool<>(
 *     "dpdk-packet-pool",
 *     2048, 10000,
 *     128, 128,                    // Headroom/tailroom
 *     dpdkAllocator,               // Backend-specific allocator
 *     MemoryBuffer::new
 * );
 * }</pre>
 * 
 * <h3>Pool Monitoring with Metrics</h3>
 * 
 * <pre>{@code
 * // Monitor pool health via PoolMetrics
 * PoolMetrics metrics = pool.getPoolMetrics();
 * 
 * // Check allocation failures (non-blocking counters)
 * long failures = metrics.getAllocationFailures();
 * if (failures > lastKnownFailures) {
 *     log.warn("Pool {} experiencing allocation failures: {}", 
 *              pool.name(), failures);
 *     // Consider increasing pool size
 * }
 * 
 * // Monitor utilization
 * double utilization = metrics.getUtilizationRatio();
 * if (utilization > 0.9) {
 *     log.warn("Pool {} nearly exhausted: {:.1f}%", 
 *              pool.name(), utilization * 100);
 * }
 * 
 * // Track segment allocations
 * long allocated = metrics.getSegmentsAllocated();
 * long inUse = metrics.getSegmentsInUse();
 * log.info("Pool {}: {}/{} segments in use", 
 *          pool.name(), inUse, allocated);
 * }</pre>
 * 
 * <h2>Buffer Metrics for Operation Monitoring</h2>
 * 
 * <p>BufferMetrics tracks all buffer operations without interrupting data flow:
 * 
 * <pre>{@code
 * BufferMetrics metrics = buffer.getMetrics();
 * 
 * // Monitor editing operations
 * long inserts = metrics.getInsertSpaceCount();
 * long removes = metrics.getRemoveSpaceCount();
 * long splits = metrics.getSplitCount();
 * 
 * // Track data movement
 * long bytesMovedLeft = metrics.getBytesMovedLeft();
 * long bytesMovedRight = metrics.getBytesMovedRight();
 * long crossSegmentCopies = metrics.getBytesCopiedCrossSegment();
 * 
 * // Monitor failures (non-throwing)
 * long outOfMemory = metrics.getOutOfMemoryCount();
 * long boundaryErrors = metrics.getBoundaryWriteErrors();
 * 
 * if (outOfMemory > 0) {
 *     // Pool exhaustion during insertSpace - adjust pool size
 *     adjustPoolSize(pool);
 * }
 * 
 * if (boundaryErrors > 0) {
 *     // Cross-segment write failures - check segment linking
 *     checkSegmentIntegrity(buffer);
 * }
 * }</pre>
 * 
 * <h2>Wrapper Pools for External Memory</h2>
 * 
 * <p>WrapperBufferPool provides efficient wrapping of external memory (DPDK, Napatech, libpcap):
 * 
 * <h3>Creating Wrapper Pools</h3>
 * 
 * <pre>{@code
 * // Create per-lcore wrapper pool (no thread contention)
 * MemoryPool<MemoryBuffer> editPool = new MemoryPool<>(
 *     "EditPool-lcore" + lcoreId,
 *     2048, 1000, 128, 128,
 *     hugepagesArena,
 *     MemoryBuffer::new);
 * 
 * WrapperBufferPool wrapperPool = new WrapperBufferPool(
 *     "WrapperPool-lcore" + lcoreId,
 *     editPool,      // Pool for edit operations
 *     100,           // Initial wrapper count
 *     1000);         // Max wrapper count
 * }</pre>
 * 
 * <h3>Wrapping External Packets</h3>
 * 
 * <pre>{@code
 * // Wrap DPDK mbuf - zero copy
 * MemorySegment mbufData = Mbuf.getDataSegment(mbufPtr);
 * WrapperByteBuffer packet = wrapperPool.wrap(mbufData, 0, packetLen);
 * packet.setNativeContext(mbufPtr, mbuf -> rte_pktmbuf_free(mbuf));
 * 
 * // Process wrapped packet
 * if (needsEncapsulation) {
 *     packet.position(0);
 *     packet.insertSpace(14);  // Allocates from editPool if needed
 *     packet.put(outerEtherHeader);
 * }
 * 
 * // Track wrapper pool metrics
 * WrapperMetrics wrapMetrics = wrapperPool.getMetrics();
 * log.debug("Wrapper pool: {} allocated, {} free, {} in use",
 *           wrapMetrics.getAllocated(),
 *           wrapMetrics.getFree(),
 *           wrapMetrics.getInUse());
 * 
 * // Release (returns wrapper to pool, frees mbuf)
 * packet.decrementRef();
 * }</pre>
 * 
 * <h2>Error Handling Philosophy</h2>
 * 
 * <p>The Memory API uses non-interrupting error handling with metrics for production environments:
 * 
 * <h3>Non-Throwing Operations</h3>
 * 
 * <pre>{@code
 * // Operations don't throw - they increment failure counters
 * MemoryBuffer buffer = pool.allocate();  // Returns null on exhaustion
 * if (buffer == null) {
 *     // Check metrics to understand failure
 *     PoolMetrics metrics = pool.getPoolMetrics();
 *     long failures = metrics.getAllocationFailures();
 *     handlePoolExhaustion(failures);
 *     return;  // Graceful degradation
 * }
 * 
 * // Buffer operations accumulate errors
 * buffer.insertSpace(2000);  // May fail if pool exhausted
 * 
 * // Check accumulated errors
 * if (buffer.hasError()) {
 *     BufferOperationException error = buffer.getError();
 *     // Log but don't interrupt processing
 *     log.error("Buffer operation failed: {}", error.getMessage());
 *     buffer.clearError();  // Clear for continued use
 * }
 * }</pre>
 * 
 * <h3>Metrics-Based Monitoring</h3>
 * 
 * <pre>{@code
 * // Monitor system health through metrics (all atomic/CAS operations)
 * public void monitorSystemHealth() {
 *     // Pool metrics
 *     for (MemoryPool<?> pool : activePools) {
 *         PoolMetrics pm = pool.getPoolMetrics();
 *         if (pm.getAllocationFailures() > threshold) {
 *             // Adjust pool size or alert operators
 *             expandPool(pool);
 *         }
 *     }
 *     
 *     // Buffer metrics
 *     BufferMetrics bm = globalBufferMetrics();
 *     if (bm.getOutOfMemoryCount() > 0) {
 *         // System under memory pressure
 *         triggerMemoryPressureResponse();
 *     }
 *     
 *     // No exceptions thrown - system continues processing
 * }
 * }</pre>
 * 
 * <h2>Best Practices</h2>
 * 
 * <h3>Performance Optimization</h3>
 * 
 * <ul>
 * <li><strong>Reuse buffers:</strong> Use WrapperBufferPool for external packets</li>
 * <li><strong>Pre-size headroom/tailroom:</strong> Configure based on typical editing patterns</li>
 * <li><strong>Monitor metrics:</strong> Adjust pool sizes based on failure counters</li>
 * <li><strong>Use network byte order methods:</strong> putShortBE/getShortBE for protocols</li>
 * </ul>
 * 
 * <h3>Memory Safety</h3>
 * 
 * <ul>
 * <li><strong>Reference counting:</strong> Always pair incrementRef() with decrementRef()</li>
 * <li><strong>Chain management:</strong> Use setNextMemory(null) before releasing segments</li>
 * <li><strong>Native memory:</strong> Set proper release handlers for external memory</li>
 * <li><strong>Error checking:</strong> Monitor metrics for resource exhaustion</li>
 * </ul>
 * 
 * <h3>Operational Excellence</h3>
 * 
 * <ul>
 * <li><strong>Named resources:</strong> Use descriptive names for pools and metrics</li>
 * <li><strong>Non-interrupting design:</strong> Use metrics instead of exceptions</li>
 * <li><strong>Capacity planning:</strong> Size pools for burst traffic + editing overhead</li>
 * <li><strong>Chain release:</strong> Use proper reverse-order release for long chains</li>
 * </ul>
 * 
 * <h2>Thread Safety</h2>
 * 
 * <p>The Memory API provides different levels of thread safety:
 * 
 * <ul>
 * <li><strong>Reference counting:</strong> Fully thread-safe (atomic operations)</li>
 * <li><strong>Memory pools:</strong> Fully thread-safe (lock-free freelist)</li>
 * <li><strong>Metrics:</strong> Fully thread-safe (atomic/CAS counters)</li>
 * <li><strong>Buffer operations:</strong> Not thread-safe (use one buffer per thread)</li>
 * <li><strong>Proxy operations:</strong> Not thread-safe (use one proxy per thread)</li>
 * </ul>
 * 
 * <h2>Related Documentation</h2>
 * 
 * <ul>
 * <li>{@link Memory} - Main memory interface and factory methods</li>
 * <li>{@link MemoryPool} - High-performance memory pooling</li>
 * <li>{@link MemoryBuffer} - Buffer-style operations with editing</li>
 * <li>{@link MemoryProxy} - Flexible chain-aware access</li>
 * <li>{@link WrapperBufferPool} - Pool for wrapping external memory</li>
 * <li>{@link BufferMetrics} - Buffer operation metrics</li>
 * <li>{@link PoolMetrics} - Pool allocation metrics</li>
 * <li>{@link MemoryAllocator} - Backend allocation strategies</li>
 * </ul>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 */
package com.slytechs.jnet.core.api.memory;
/**
 * High-performance memory management API for zero-allocation network packet processing.
 * 
 * <p>This package provides a comprehensive memory management system built on Java's Foreign Function
 * &amp; Memory (Panama FFM) API, designed for sustained throughput exceeding 100 million packets per second.
 * The API supports both single memory segments and chained structures with reference counting, memory
 * pooling, and efficient editing capabilities.
 * 
 * <h2>Getting Started</h2>
 * 
 * <p>The simplest way to begin using the Memory API is with the factory methods:
 * 
 * <pre>{@code
 * // Create memory from a MemorySegment
 * try (Arena arena = Arena.ofConfined()) {
 *     MemorySegment segment = arena.allocate(1024);
 *     Memory memory = Memory.of(segment, 0);  // Wrap entire segment
 *     
 *     // Access as ByteBuffer
 *     ByteBuffer buffer = memory.asByteBuffer();
 *     buffer.put("Hello World".getBytes());
 *     
 *     // Check basic properties
 *     System.out.println("Capacity: " + memory.memoryCapacity());
 *     System.out.println("Data length: " + memory.memoryDataLength());
 * }
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
 *     Arena.global(),              // Memory arena
 *     MemoryBuffer::new            // Factory method
 * );
 * 
 * // Allocate from pool
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
 * <h3>Memory vs Data Bounds</h3>
 * <p>The API distinguishes between <em>memory bounds</em> (total addressable space) and 
 * <em>data bounds</em> (currently active data region):
 * 
 * <pre>{@code
 * |-------- Memory Bounds (Fixed) --------|
 * |  gap  |--- Data Bounds ---|   gap     |
 *         ↑                   ↑
 *   memoryDataOffset     memoryDataEnd
 * 
 * // Memory bounds: memoryOffset() to memoryEnd() - immutable
 * // Data bounds: memoryDataOffset() to memoryDataEnd() - can be adjusted
 * }</pre>
 * 
 * <p>This design enables efficient space management similar to DPDK's rte_mbuf, where leading
 * and trailing space can be used for in-place expansion.
 * 
 * <h3>Reference Counting</h3>
 * <p>All memory objects use atomic reference counting for safe resource management:
 * 
 * <pre>{@code
 * Memory original = Memory.of(segment, 0);    // refcount = 1
 * Memory shared = original.incrementRef();    // refcount = 2
 * 
 * // When refcount reaches 0, memory is automatically cleaned up
 * shared.decrementRef();    // refcount = 1
 * original.decrementRef();  // refcount = 0, memory closed
 * }</pre>
 * 
 * <h3>Memory Chains</h3>
 * <p>Memory objects can be linked together to handle fragmented data without copying:
 * 
 * <pre>{@code
 * Memory segment1 = Memory.of(firstSegment, 0);
 * Memory segment2 = Memory.of(secondSegment, 0);
 * segment1.setNextMemory(segment2);  // Create chain
 * 
 * // Chain operations work across all segments
 * long totalLength = segment1.chainDataLength();
 * int segmentCount = segment1.chainMemoryCount();
 * }</pre>
 * 
 * <h3>Type Safety Through Interfaces</h3>
 * <p>The API uses interface composition to prevent misuse:
 * 
 * <ul>
 * <li>{@link MemoryView} - Read-only access and navigation</li>
 * <li>{@link MemoryWindow} - Bounds and positioning information</li>
 * <li>{@link MemoryRef} - Reference counting and lifecycle</li>
 * <li>{@link Memory} - Complete memory abstraction (extends all three)</li>
 * </ul>
 * 
 * <p>{@link MemoryProxy} implements the first three interfaces but <em>not</em> Memory,
 * preventing proxies from being used in chains or where actual memory is expected.
 * 
 * <h2>Basic Memory Operations</h2>
 * 
 * <h3>Creating Memory Objects</h3>
 * 
 * <pre>{@code
 * // Factory methods for MemoryWrapper (lightweight, immutable)
 * Memory memory1 = Memory.of(segment, 0);           // Entire segment
 * Memory memory2 = Memory.of(segment, 100, 512);    // Specific region
 * 
 * // For mutable data bounds, use MemorySlice
 * MemorySlice slice = new MemorySlice(segment, 0, 1024, 100, 600);
 * slice.memoryDataOffset(200);  // Adjust data start
 * slice.memoryDataEnd(800);     // Adjust data end
 * }</pre>
 * 
 * <h3>Accessing Memory Content</h3>
 * 
 * <pre>{@code
 * // ByteBuffer access (most common)
 * ByteBuffer buffer = memory.asByteBuffer();
 * buffer.put(data);
 * 
 * // Direct MemorySegment access
 * MemorySegment segment = memory.asMemorySegment();
 * segment.set(ValueLayout.JAVA_INT, 0, 42);
 * 
 * // Chain-aware access
 * MemorySegment segmentAt = memory.asMemorySegmentAt(1500);  // May span segments
 * }</pre>
 * 
 * <h3>Memory Properties</h3>
 * 
 * <pre>{@code
 * // Basic properties
 * long capacity = memory.memoryCapacity();           // Total addressable space
 * long dataLength = memory.memoryDataLength();       // Current data size
 * 
 * // Chain properties
 * long chainCapacity = memory.chainCapacity();       // Total across all segments
 * long chainDataLength = memory.chainDataLength();   // Data across all segments
 * int segmentCount = memory.chainMemoryCount();      // Number of segments
 * 
 * // State checks
 * boolean isNull = memory.isNull();                  // Check for null/invalid
 * boolean isPointer = memory.isPointer();            // Check for zero-sized pointer
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
 *     Arena.global(),              // Arena for allocation
 *     MemoryBuffer::new            // Factory method
 * );
 * 
 * // Pool with custom backend allocator
 * MemoryAllocator dpdkAllocator = new DpdkMemoryAllocator("dpdk-pool", rteMempool);
 * MemoryPool<MemoryBuffer> dpdkPool = new MemoryPool<>(
 *     "dpdk-packet-pool",
 *     2048, 10000,
 *     dpdkAllocator,               // Backend-specific allocator
 *     MemoryBuffer::new
 * );
 * }</pre>
 * 
 * <h3>Allocation Strategies</h3>
 * 
 * <pre>{@code
 * // Non-throwing allocation (returns null on exhaustion)
 * MemoryBuffer buffer = pool.allocate();
 * if (buffer == null) {
 *     handlePoolExhaustion();
 *     return;
 * }
 * 
 * // Fail-fast allocation (throws on exhaustion)
 * try {
 *     MemoryBuffer buffer = pool.allocateOrThrow();
 *     processPacket(buffer);
 * } catch (OutOfMemoryError e) {
 *     handlePoolExhaustion();
 * }
 * }</pre>
 * 
 * <h3>Pool Monitoring</h3>
 * 
 * <pre>{@code
 * // Check pool health
 * PoolMetrics metrics = pool.getMetrics();
 * double utilization = metrics.getUtilizationRatio();
 * long failures = metrics.getAllocationFailures();
 * 
 * if (utilization > 0.9) {
 *     log.warn("Pool '{}' nearly exhausted: {:.1f}%", pool.name(), utilization * 100);
 * }
 * }</pre>
 * 
 * <h2>Memory Proxies for Protocol Processing</h2>
 * 
 * <p>{@link MemoryProxy} provides zero-allocation, rebindable access to memory regions,
 * ideal for protocol header parsing:
 * 
 * <h3>Basic Proxy Usage</h3>
 * 
 * <pre>{@code
 * // Create reusable proxies (typically as instance fields)
 * MemoryProxy ethernetProxy = new MemoryProxy();
 * MemoryProxy ipProxy = new MemoryProxy();
 * 
 * // Bind to protocol headers in packet
 * ethernetProxy.bindMemory(packet, 0, 14);      // Ethernet header
 * ipProxy.bindMemory(packet, 14, 20);           // IP header
 * 
 * // Access header data
 * ByteBuffer ethHeader = ethernetProxy.asByteBuffer();
 * int etherType = ethHeader.getShort(12) & 0xFFFF;
 * 
 * // Unbind for reuse with next packet
 * ethernetProxy.unbindMemory();
 * ipProxy.unbindMemory();
 * }</pre>
 * 
 * <h3>Protocol Header Classes</h3>
 * 
 * <p>Extend MemoryProxy for protocol-specific access:
 * 
 * <pre>{@code
 * public class EthernetHeader extends MemoryProxy {
 *     private final MacAddressProxy dstMac = new MacAddressProxy();
 *     private final MacAddressProxy srcMac = new MacAddressProxy();
 *     
 *     @Override
 *     protected void onBind() {
 *         dstMac.bindMemory(asMemory(), 0, 6);
 *         srcMac.bindMemory(asMemory(), 6, 6);
 *     }
 *     
 *     public int getEtherType() {
 *         return asMemorySegment().get(ValueLayout.JAVA_SHORT_UNALIGNED, 12) & 0xFFFF;
 *     }
 * }
 * }</pre>
 * 
 * <h3>Proxy Type Safety</h3>
 * 
 * <p>Proxies cannot be used where Memory is expected (compile-time protection):
 * 
 * <pre>{@code
 * MemoryProxy proxy = new MemoryProxy();
 * proxy.bindMemory(packet, 0, 14);
 * 
 * // These operations are NOT allowed (won't compile):
 * // editor.edit(proxy);              // ❌ Proxy is not Memory
 * // packet.setNextMemory(proxy);     // ❌ Proxy cannot be in chains
 * 
 * // Get actual memory for editing:
 * Memory actualMemory = proxy.asMemory();  // ✅ Returns bound memory
 * editor.edit(actualMemory);               // ✅ Can edit actual memory
 * }</pre>
 * 
 * <h2>Buffer-Style Operations</h2>
 * 
 * <p>{@link MemoryBuffer} provides familiar buffer-style positioning operations
 * that work across memory chains:
 * 
 * <h3>Position and Limit Management</h3>
 * 
 * <pre>{@code
 * MemoryBuffer buffer = pool.allocate();
 * 
 * // Buffer-style operations (similar to java.nio.Buffer)
 * buffer.position(100)           // Set absolute position
 *       .limit(500)              // Set limit
 *       .mark();                 // Mark current position
 * 
 * // Delta operations (more efficient)
 * buffer.adjustPosition(50)      // Move position forward/backward
 *       .skip(20)                // Move forward (convenience)
 *       .backup(10)              // Move backward (convenience)
 *       .adjustLimit(-100);      // Shrink limit
 * 
 * // Buffer state
 * long remaining = buffer.remaining();        // Bytes between position and limit
 * boolean hasData = buffer.hasRemaining();    // Check for remaining data
 * }</pre>
 * 
 * <h3>Automatic Positioning with Proxies</h3>
 * 
 * <pre>{@code
 * // Position buffer at protocol headers with automatic bounds
 * MemoryBuffer packetBuffer = packet.asMemoryBuffer();
 * 
 * // Edit Ethernet header (position=0, limit=14)
 * packetBuffer.positionAt(ethernetProxy)
 *             .skip(6)                    // Move to source MAC
 *             .put(newSrcMacBytes)        // Write 6 bytes
 *             .putShort(0x0800);          // Write EtherType
 * 
 * // Edit IP header (position=14, limit=34)  
 * packetBuffer.positionAt(ipProxy)
 *             .skip(8)                    // Move to TTL field
 *             .put((byte)(ttl - 1));      // Decrement TTL
 * }</pre>
 * 
 * <h3>Data Access Operations</h3>
 * 
 * <pre>{@code
 * // Put operations (advance position automatically)
 * buffer.put((byte) 0x42)          // Write byte, position += 1
 *       .putShort((short) 0x1234)  // Write short, position += 2
 *       .putInt(0x12345678)        // Write int, position += 4
 *       .put(byteArray);           // Write array, position += array.length
 * 
 * // Get operations (advance position automatically)
 * byte b = buffer.get();           // Read byte, position += 1
 * short s = buffer.getShort();     // Read short, position += 2
 * buffer.get(destinationArray);    // Read into array
 * 
 * // Cross-segment writes (automatic expansion attempts)
 * buffer.putLong(0x123456789ABCDEFL);  // May span segments, handles automatically
 * }</pre>
 * 
 * <h2>Complex Memory Editing</h2>
 * 
 * <p>{@link MemoryEditor} provides comprehensive editing capabilities for complex
 * memory chain modifications:
 * 
 * <h3>Creating and Using Editors</h3>
 * 
 * <pre>{@code
 * // Create named editor for monitoring
 * try (MemoryEditor<MemoryBuffer> editor = MemoryEditor.create("vlan-insertion")) {
 *     
 *     MemoryBuffer result = editor
 *         .edit(packet)                              // Bind to memory chain
 *         .insertAt(12, vlanHeader)                  // Insert VLAN after MAC addresses
 *         .removeRange(100, 120)                     // Remove optional fields
 *         .appendToChain(trailer)                    // Add trailer
 *         .commit();                                 // Apply changes
 *     
 *     // Editor can be reused
 *     MemoryBuffer result2 = editor
 *         .edit(anotherPacket)
 *         .prependToChain(tunnelHeader)
 *         .commit();
 * }
 * }</pre>
 * 
 * <h3>Structural Operations</h3>
 * 
 * <pre>{@code
 * // Chain modification operations
 * editor.insertAt(offset, data)              // Insert at specific offset
 *       .removeRange(start, end)             // Remove byte range
 *       .replaceRange(start, end, newData)   // Replace with new data
 *       .appendToChain(data)                 // Add to end
 *       .prependToChain(data);               // Add to beginning
 * 
 * // Advanced operations
 * editor.splitAt(offset)                     // Split chain at offset
 *       .mergeWith(otherChain)               // Merge chains
 *       .compactChain();                     // Optimize structure
 * }</pre>
 * 
 * <h3>Editor Monitoring</h3>
 * 
 * <pre>{@code
 * // Monitor editor performance
 * EditorMetrics metrics = editor.getMetrics();
 * 
 * log.info("Editor '{}': {} operations, {} failures, {} cross-segment writes",
 *          editor.name(),
 *          metrics.getEditOperations(),
 *          metrics.getAllocationFailures(),
 *          metrics.getCrossSegmentOperations());
 * }</pre>
 * 
 * <h2>Backend Integration</h2>
 * 
 * <p>The Memory API supports multiple allocation backends through the
 * {@link MemoryAllocator} interface:
 * 
 * <h3>Standard Arena Allocator</h3>
 * 
 * <pre>{@code
 * // For development and testing
 * MemoryAllocator standardAllocator = new ArenaMemoryAllocator(
 *     "test-allocator", Arena.global());
 * }</pre>
 * 
 * <h3>DPDK Integration</h3>
 * 
 * <pre>{@code
 * // High-performance DPDK allocation
 * RteMempool rtePool = RteMbuf.pktmbufPoolCreate("dpdk-pool", 1024, 256, 0, 2048, 0);
 * MemoryAllocator dpdkAllocator = new DpdkMemoryAllocator("dpdk-allocator", rtePool);
 * 
 * // Pool automatically uses DPDK allocation
 * MemoryPool<MemoryBuffer> pool = new MemoryPool<>(
 *     "dpdk-packet-pool", 2048, 1000, dpdkAllocator, MemoryBuffer::new);
 * }</pre>
 * 
 * <h3>Napatech NTAPI Integration</h3>
 * 
 * <pre>{@code
 * // Hardware-accelerated capture
 * NtNetStreamRx ntStream = NtNetStreamRx.open("stream-0", NtNetInterface.NT_NET_INTERFACE_SEGMENT, 0, 64);
 * MemoryAllocator ntapiAllocator = new NtapiMemoryAllocator("ntapi-allocator", ntStream);
 * }</pre>
 * 
 * <h3>Custom Backend Implementation</h3>
 * 
 * <pre>{@code
 * public class CustomMemoryAllocator implements MemoryAllocator {
 *     private final String name;
 *     private final CustomMetrics metrics = new CustomMetrics();
 *     
 *     @Override
 *     public String name() { return name; }
 *     
 *     @Override
 *     public Memory allocateMemory(long size) {
 *         // Custom allocation logic
 *         MemorySegment segment = customAllocate(size);
 *         metrics.recordAllocation();
 *         return Memory.of(segment, 0);
 *     }
 *     
 *     @Override
 *     public BackendMemoryMetrics getMetrics() { return metrics; }
 * }
 * }</pre>
 * 
 * <h2>Error Handling and Monitoring</h2>
 * 
 * <p>The Memory API uses non-throwing error handling for production environments:
 * 
 * <h3>Error Counter Philosophy</h3>
 * 
 * <pre>{@code
 * // Operations increment error counters instead of throwing exceptions
 * MemoryBuffer buffer = pool.allocate();  // Returns null on exhaustion
 * if (buffer == null) {
 *     // Handle gracefully, check pool metrics
 *     PoolMetrics metrics = pool.getMetrics();
 *     long failures = metrics.getAllocationFailures();
 *     handlePoolExhaustion(failures);
 *     return;
 * }
 * 
 * // Cross-segment writes handle failures gracefully
 * buffer.putLong(value);  // Attempts expansion, falls back to byte-by-byte on failure
 * long boundaryErrors = buffer.getBoundaryWriteErrors();
 * if (boundaryErrors > 0) {
 *     log.warn("Boundary write failures: {}", boundaryErrors);
 * }
 * }</pre>
 * 
 * <h3>Operational Monitoring</h3>
 * 
 * <pre>{@code
 * // Monitor pool health
 * public void monitorMemoryHealth() {
 *     for (MemoryPool<?> pool : activePools) {
 *         PoolMetrics metrics = pool.getMetrics();
 *         
 *         // Check utilization
 *         if (metrics.getUtilizationRatio() > 0.9) {
 *             alertHighUtilization(pool.name(), metrics.getUtilizationRatio());
 *         }
 *         
 *         // Check for allocation failures
 *         long failures = metrics.getAllocationFailures();
 *         if (failures > previousFailures.get(pool.name())) {
 *             alertAllocationFailures(pool.name(), failures);
 *         }
 *     }
 * }
 * }</pre>
 * 
 * <h3>Exception Cases</h3>
 * 
 * <p>Exceptions are still thrown for programming errors and serious issues:
 * 
 * <pre>{@code
 * // Programming errors (IllegalStateException)
 * memory.memoryCapacity();  // Throws if memory is closed
 * proxy.bindMemory(mem, 0); // Throws if already bound
 * 
 * // Resource corruption (IllegalStateException)
 * memory.decrementRef();    // Throws on refcount underflow
 * 
 * // Resource exhaustion (OutOfMemoryError - only with allocateOrThrow)
 * MemoryBuffer buffer = pool.allocateOrThrow();  // Throws if pool exhausted
 * }</pre>
 * 
 * <h2>Best Practices</h2>
 * 
 * <h3>Performance Optimization</h3>
 * 
 * <ul>
 * <li><strong>Reuse objects:</strong> Create MemoryProxy instances once and reuse across packets</li>
 * <li><strong>Use appropriate tiers:</strong> Inline operations for 100M+ pps, structural for 10M+ pps</li>
 * <li><strong>Pool sizing:</strong> Size pools to handle burst traffic without exhaustion</li>
 * <li><strong>Backend selection:</strong> Use DPDK for highest performance, Arena for flexibility</li>
 * </ul>
 * 
 * <h3>Memory Safety</h3>
 * 
 * <ul>
 * <li><strong>Reference counting:</strong> Always pair incrementRef() with decrementRef()</li>
 * <li><strong>Try-finally blocks:</strong> Ensure cleanup even during exceptions</li>
 * <li><strong>Proxy lifecycle:</strong> Unbind proxies when done to prevent memory leaks</li>
 * <li><strong>Pool monitoring:</strong> Watch for allocation failures and high utilization</li>
 * </ul>
 * 
 * <h3>Operational Excellence</h3>
 * 
 * <ul>
 * <li><strong>Named resources:</strong> Use descriptive names for pools and editors</li>
 * <li><strong>Metrics monitoring:</strong> Set up alerts for error counters and utilization</li>
 * <li><strong>Error handling:</strong> Handle null returns gracefully, monitor error counters</li>
 * <li><strong>Backend matching:</strong> Choose allocators that match your deployment environment</li>
 * </ul>
 * 
 * <h2>Thread Safety</h2>
 * 
 * <p>The Memory API provides different levels of thread safety:
 * 
 * <ul>
 * <li><strong>Reference counting:</strong> Fully thread-safe (atomic operations)</li>
 * <li><strong>Memory pools:</strong> Fully thread-safe (lock-free algorithms)</li>
 * <li><strong>Chain navigation:</strong> Thread-safe for reading</li>
 * <li><strong>Buffer positioning:</strong> Not thread-safe (use external synchronization)</li>
 * <li><strong>Proxy operations:</strong> Not thread-safe (one proxy per thread)</li>
 * </ul>
 * 
 * <h2>Related Documentation</h2>
 * 
 * <ul>
 * <li>{@link Memory} - Main memory interface</li>
 * <li>{@link MemoryPool} - High-performance memory pooling</li>
 * <li>{@link MemoryBuffer} - Buffer-style operations</li>
 * <li>{@link MemoryProxy} - Zero-allocation protocol parsing</li>
 * <li>{@link MemoryEditor} - Complex memory editing</li>
 * <li>{@link MemoryAllocator} - Backend allocation strategies</li>
 * </ul>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 */
package com.slytechs.jnet.core.api.memory;
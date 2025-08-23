/*
 * Sly Technologies Free License
 * 
 * Copyright 2024 Sly Technologies Inc.
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

/**
 * A lightweight, rebindable proxy for zero-allocation memory access with
 * minimal overhead.
 * 
 * <p>
 * MemoryProxy provides a reusable view mechanism that can be bound to different
 * memory regions without allocation overhead. This class is optimized for
 * high-performance scenarios requiring frequent rebinding, such as network
 * packet processing where thousands of packets per second are accessed through
 * the same proxy objects.
 * </p>
 * 
 * <h2>Design Philosophy</h2>
 * 
 * <h3>Minimalist Architecture</h3>
 * <p>
 * MemoryProxy maintains only essential state (4 fields), delegating all
 * operations to the bound memory. This design provides:
 * </p>
 * <ul>
 * <li><strong>Fast binding:</strong> O(1) with minimal field assignments</li>
 * <li><strong>Small footprint:</strong> ~32 bytes per proxy instance</li>
 * <li><strong>Cache efficiency:</strong> Optimal CPU cache line
 * utilization</li>
 * <li><strong>Zero allocation:</strong> Reusable across unlimited bind/unbind
 * cycles</li>
 * </ul>
 * 
 * <h3>Binding Modes</h3>
 * 
 * <pre>{@code
 * UNBOUNDED MODE (length = -1):
 * Proxy → [Memory Segment 1] → [Segment 2] → [Segment 3]
 *         ↑
 *      offset (can access entire chain from offset)
 * 
 * BOUNDED MODE (length ≥ 0):
 * Proxy → [Memory Segment]
 *         ↑─────length─────↑
 *      offset          (isolated view)
 * }</pre>
 * 
 * <table border="1">
 * <caption>Binding Mode Characteristics</caption> <thead>
 * <tr>
 * <th>Mode</th>
 * <th>Length Value</th>
 * <th>Chain Access</th>
 * <th>Use Case</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>Unbounded</td>
 * <td>-1</td>
 * <td>Full chain navigation</td>
 * <td>Packet-level views</td>
 * </tr>
 * <tr>
 * <td>Bounded</td>
 * <td>≥ 0</td>
 * <td>Single segment only</td>
 * <td>Headers, fields</td>
 * </tr>
 * </tbody>
 * </table>
 * 
 * <h2>Lifecycle Management</h2>
 * 
 * <h3>Reference Counting</h3>
 * <p>
 * MemoryProxy participates in the memory's reference counting:
 * </p>
 * <ul>
 * <li>Binding increments the target memory's reference count</li>
 * <li>Unbinding decrements the reference count</li>
 * <li>Automatic unbinding occurs if bound memory is released</li>
 * </ul>
 * 
 * <h3>Chain Stability Requirements</h3>
 * <p>
 * <strong>CRITICAL:</strong> The underlying memory chain structure must remain
 * stable while a proxy is bound. Chain modifications require:
 * </p>
 * <ol>
 * <li>Unbind all proxies from the chain</li>
 * <li>Modify the chain structure</li>
 * <li>Rebind proxies as needed</li>
 * </ol>
 * 
 * <h2>Thread Safety</h2>
 * <ul>
 * <li><strong>Binding operations:</strong> NOT thread-safe, require external
 * synchronization</li>
 * <li><strong>Read operations:</strong> Thread-safe once bound (if underlying
 * memory is stable)</li>
 * <li><strong>Recommendation:</strong> Use thread-local proxy instances or
 * synchronize binding</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * 
 * <h3>Network Packet Processing</h3>
 * 
 * <pre>{@code
 * // Reusable proxy pool for packet analysis
 * class PacketAnalyzer {
 * 	private final MemoryProxy packet = new MemoryProxy();
 * 	private final MemoryProxy ethernet = new MemoryProxy();
 * 	private final MemoryProxy ip = new MemoryProxy();
 * 	private final MemoryProxy tcp = new MemoryProxy();
 * 
 * 	void analyzePacket(Memory capturedData) {
 * 		try {
 * 			// Bind to full packet (unbounded)
 * 			packet.bindMemory(capturedData, 0);
 * 
 * 			// Bind to protocol layers (bounded)
 * 			ethernet.bindMemory(packet, 0, 14); // Ethernet: 14 bytes
 * 			ip.bindMemory(packet, 14, 20); // IPv4: 20 bytes
 * 			tcp.bindMemory(packet, 34, getTcpLength()); // TCP: variable
 * 
 * 			// Process packet data
 * 			processEthernet(ethernet);
 * 			processIp(ip);
 * 			processTcp(tcp);
 * 
 * 		} finally {
 * 			// Clean unbinding for reuse
 * 			tcp.unbindMemory();
 * 			ip.unbindMemory();
 * 			ethernet.unbindMemory();
 * 			packet.unbindMemory();
 * 		}
 * 	}
 * }
 * }</pre>
 * 
 * <h3>Buffer Iteration (Hardware Capture)</h3>
 * 
 * <pre>{@code
 * // Process packets in a large capture buffer
 * void processCapture(Memory captureBuffer, List<PacketInfo> packets) {
 * 	MemoryProxy proxy = new MemoryProxy();
 * 
 * 	for (PacketInfo info : packets) {
 * 		// Bind to each packet region
 * 		proxy.bindMemory(captureBuffer, info.offset, info.length);
 * 
 * 		// Process packet
 * 		processPacket(proxy);
 * 
 * 		// Unbind for next iteration
 * 		proxy.unbindMemory();
 * 	}
 * }
 * }</pre>
 * 
 * <h3>Hierarchical Proxy Structure</h3>
 * 
 * <pre>
 * {@code
 * class EthernetHeader extends MemoryProxy {
 *     private final MacAddress dstMac = new MacAddress();
 *     private final MacAddress srcMac = new MacAddress();
 *     
 *
 * &#64;author Mark Bednarczyk [mark@slytechs.com]
 * &#64;author Sly Technologies Inc.
 * &#64;see Memory for the complete memory interface
 * &#64;see AbstractMemory for base implementation
 * &#64;see MemoryBufferView for editable memory buffers
 * &#64;since 1.0
 * &#64;Override     protected void onBindMemory() {
 *         // Cascade binding to sub-elements
 *         dstMac.bindMemory(this, 0, 6);   // Destination MAC
 *         srcMac.bindMemory(this, 6, 6);   // Source MAC
 *     }
 *     
 * @Override     protected void onUnbindMemory() {
 *         // Clean cascade unbinding
 *         srcMac.unbindMemory();
 *         dstMac.unbindMemory();
 *     }
 *     
 *     public MacAddress getDestination() { return dstMac; }
 *     public MacAddress getSource() { return srcMac; }
 *     public int getEtherType() {
 *         return asMemorySegment().get(ValueLayout.JAVA_SHORT_BE, 12);
 *     }
 * }
 * }
 * </pre>
 * 
 * <h2>Performance Characteristics</h2>
 * 
 * <table border="1">
 * <caption>Operation Performance</caption> <thead>
 * <tr>
 * <th>Operation</th>
 * <th>Complexity</th>
 * <th>Allocations</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>bind/unbind</td>
 * <td>O(1)</td>
 * <td>0</td>
 * </tr>
 * <tr>
 * <td>delegate call</td>
 * <td>O(1)</td>
 * <td>0</td>
 * </tr>
 * <tr>
 * <td>boundary check</td>
 * <td>O(1)</td>
 * <td>0</td>
 * </tr>
 * </tbody>
 * </table>
 */
public class MemoryProxy implements Memory {

   /**
    * The memory object this proxy is currently bound to.
    * 
    * <p>
    * When null, the proxy is unbound and all operations throw IllegalStateException.
    * This reference ensures the bound memory remains valid during the proxy's
    * bound lifetime through reference counting.
    * </p>
    */
   private Memory boundMemory;

   /**
    * Starting position within the bound memory where this proxy's view begins.
    * 
    * <p>
    * This position is relative to the bound memory's coordinate system and is
    * applied to all position-based operations to translate proxy coordinates
    * to bound memory coordinates.
    * </p>
    */
   private long proxyOffset;

   /**
    * Size of this proxy's view, or -1 for unbounded views.
    * 
    * <p>
    * Controls the proxy's access mode:
    * <ul>
    * <li><strong>-1:</strong> Unbounded - full chain access from offset</li>
    * <li><strong>≥0:</strong> Bounded - limited to specified byte count</li>
    * </ul>
    * </p>
    */
   private long proxyLength;

   /**
    * Indicates if this proxy has been permanently closed.
    * 
    * <p>
    * Once closed, the proxy cannot be reused. All operations on a closed
    * proxy throw IllegalStateException.
    * </p>
    */
   private boolean isClosed;

   /**
    * Constructs a new MemoryProxy in unbound state.
    * 
    * <p>
    * The proxy starts unbound and must be bound to memory using one of the
    * bindMemory methods before use. All Memory interface operations will
    * throw IllegalStateException until binding occurs.
    * </p>
    */
   public MemoryProxy() {
   	this.boundMemory = null;
   	this.proxyOffset = 0;
   	this.proxyLength = 0;
   	this.isClosed = false;
   }

   /**
    * {@inheritDoc}
    * 
    * <p>
    * Returns the active bytes end boundary, constrained by both the proxy's
    * bounds and the underlying memory's active region.
    * </p>
    */
   @Override
   public long activeBytesEnd() {
   	checkBound();
   	if (proxyLength == -1) {
   		return boundMemory.activeBytesEnd(); // Unbounded - use bound's end
   	}
   	// Bounded - constrain to our view
   	long ourEnd = boundMemory.segmentOffset() + proxyOffset + proxyLength;
   	return Math.min(ourEnd, boundMemory.activeBytesEnd());
   }

   /**
    * {@inheritDoc}
    * 
    * <p>
    * Sets the active bytes end within this proxy's view. The new boundary
    * must be within the proxy's segment bounds.
    * </p>
    */
   @Override
   public long activeBytesEnd(long newEnd) {
   	checkBound();
   	// Validate within our bounds
   	long minEnd = activeBytesStart();
   	long maxEnd = segmentEnd();
   	if (newEnd < minEnd || newEnd > maxEnd) {
   		throw new IllegalArgumentException(
   			String.format("Active bytes end out of bounds: %d (must be between %d and %d)",
   				newEnd, minEnd, maxEnd));
   	}
   	// Delegate to bound memory
   	return boundMemory.activeBytesEnd(newEnd);
   }

   /**
    * {@inheritDoc}
    * 
    * <p>
    * Returns the active bytes start boundary, constrained by both the proxy's
    * bounds and the underlying memory's active region.
    * </p>
    */
   @Override
   public long activeBytesStart() {
   	checkBound();
   	long boundStart = boundMemory.activeBytesStart();
   	long ourStart = boundMemory.segmentOffset() + proxyOffset;
   	return Math.max(boundStart, ourStart);
   }

   /**
    * {@inheritDoc}
    * 
    * <p>
    * Sets the active bytes start within this proxy's view. The new boundary
    * must be within the proxy's segment bounds.
    * </p>
    */
   @Override
   public long activeBytesStart(long newStart) {
   	checkBound();
   	// Validate within our bounds
   	long minStart = segmentOffset();
   	long maxStart = activeBytesEnd();
   	if (newStart < minStart || newStart > maxStart) {
   		throw new IllegalArgumentException(
   			String.format("Active bytes start out of bounds: %d (must be between %d and %d)",
   				newStart, minStart, maxStart));
   	}
   	// Delegate to bound memory
   	return boundMemory.activeBytesStart(newStart);
   }

   /**
    * {@inheritDoc}
    * 
    * <p>
    * Creates a ByteBuffer view by delegating to the bound memory and
    * adjusting position and limit to match this proxy's view.
    * </p>
    */
   @Override
   public ByteBuffer asByteBuffer() {
   	checkBound();
   	ByteBuffer buffer = boundMemory.asByteBuffer();
   	buffer.position((int) proxyOffset);
   	if (proxyLength != -1) {
   		buffer.limit((int) (proxyOffset + proxyLength));
   	}
   	return buffer.slice();
   }

   /**
    * {@inheritDoc}
    * 
    * <p>
    * Returns the bound memory object, not the proxy itself.
    * </p>
    */
   @Override
   public Memory asMemory() {
   	checkBound();
   	return boundMemory;
   }

   /**
    * {@inheritDoc}
    * 
    * <p>
    * Creates a MemorySegment view by delegating to the bound memory and
    * creating an appropriate slice for this proxy's bounds.
    * </p>
    */
   @Override
   public MemorySegment asMemorySegment() {
   	checkBound();
   	MemorySegment segment = boundMemory.asMemorySegment();
   	if (proxyLength == -1) {
   		return segment.asSlice(proxyOffset);
   	}
   	return segment.asSlice(proxyOffset, proxyLength);
   }

   /**
    * {@inheritDoc}
    * 
    * <p>
    * Delegates to the bound memory with position adjustment, allowing
    * position-based access relative to this proxy's offset.
    * </p>
    */
   @Override
   public MemorySegment asMemorySegmentAt(long position) {
   	checkBound();
   	if (proxyLength != -1 && position >= proxyLength) {
   		throw new IllegalArgumentException(
   			String.format("Position %d exceeds bounded view length %d", 
   				position, proxyLength));
   	}
   	return boundMemory.asMemorySegmentAt(proxyOffset + position);
   }

   /**
    * Binds this proxy to an unbounded view of memory from the specified offset.
    * 
    * <p>
    * Creates an unbounded view extending from the offset to the end of the
    * bound memory, including access to any chained segments. This mode is
    * ideal for packet-level views that may span multiple segments.
    * </p>
    * 
    * <pre>{@code
    * Memory packet = receivePacket();
    * proxy.bindMemory(packet, 0);  // View entire packet chain
    * }</pre>
    * 
    * @param memory the memory to bind to, must not be null
    * @param offset the starting position within the memory, must be ≥ 0
    * @throws IllegalStateException if this proxy is already bound or closed
    * @throws NullPointerException if memory is null
    * @throws IllegalArgumentException if offset is negative
    */
   public void bindMemory(Memory memory, long offset) {
   	checkNotClosed();
   	if (boundMemory != null) {
   		throw new IllegalStateException("Proxy is already bound - call unbindMemory() first");
   	}
   	if (memory == null) {
   		throw new NullPointerException("Cannot bind to null memory");
   	}
   	if (offset < 0) {
   		throw new IllegalArgumentException("Offset cannot be negative: " + offset);
   	}

   	this.boundMemory = memory;
   	this.proxyOffset = offset;
   	this.proxyLength = -1; // Unbounded marker
   	memory.incrementRef();

   	onBindMemory();
   }

   /**
    * Binds this proxy to a bounded view of memory.
    * 
    * <p>
    * Creates a bounded view limited to the specified region. This mode is
    * ideal for protocol headers, fields, or when iterating through buffers
    * containing multiple packets. Bounded views do not provide chain navigation.
    * </p>
    * 
    * <pre>{@code
    * // Bind to Ethernet header (14 bytes at offset 0)
    * proxy.bindMemory(packet, 0, 14);
    * }</pre>
    * 
    * @param memory the memory to bind to, must not be null
    * @param offset the starting position within the memory, must be ≥ 0
    * @param length the size of the view in bytes, must be ≥ 0
    * @throws IllegalStateException if this proxy is already bound or closed
    * @throws NullPointerException if memory is null
    * @throws IllegalArgumentException if offset or length is invalid
    */
   public void bindMemory(Memory memory, long offset, long length) {
   	checkNotClosed();
   	if (boundMemory != null) {
   		throw new IllegalStateException("Proxy is already bound - call unbindMemory() first");
   	}
   	if (memory == null) {
   		throw new NullPointerException("Cannot bind to null memory");
   	}
   	if (offset < 0) {
   		throw new IllegalArgumentException("Offset cannot be negative: " + offset);
   	}
   	if (length < 0) {
   		throw new IllegalArgumentException("Length cannot be negative: " + length);
   	}

   	this.boundMemory = memory;
   	this.proxyOffset = offset;
   	this.proxyLength = length;
   	memory.incrementRef();

   	onBindMemory();
   }

   /**
    * Validates that this proxy is bound to memory.
    * 
    * @throws IllegalStateException if not bound or closed
    */
   private void checkBound() {
   	checkNotClosed();
   	if (boundMemory == null) {
   		throw new IllegalStateException("Proxy is not bound to memory");
   	}
   }

   /**
    * Validates that this proxy is not closed.
    * 
    * @throws IllegalStateException if closed
    */
   private void checkNotClosed() {
   	if (isClosed) {
   		throw new IllegalStateException("Proxy has been closed");
   	}
   }

   /**
    * Permanently closes this proxy, releasing any bound memory.
    * 
    * <p>
    * After closing, the proxy cannot be reused. Any bound memory is
    * properly unbound with reference counting.
    * </p>
    */
   public void close() {
   	if (!isClosed) {
   		if (boundMemory != null) {
   			unbindMemory();
   		}
   		isClosed = true;
   	}
   }

   /**
    * {@inheritDoc}
    * 
    * <p>
    * Decrements the reference count of the bound memory. If the count
    * reaches zero, this proxy is automatically unbound.
    * </p>
    */
   @Override
   public int decrementRef() {
   	checkBound();
   	int newRef = boundMemory.decrementRef();
   	if (newRef == 0) {
   		// Auto-unbind when memory is released
   		boundMemory = null;
   		proxyOffset = 0;
   		proxyLength = 0;
   	}
   	return newRef;
   }

   /**
	 * Returns the memory object this proxy is bound to.
	 *
	 * @return the bound memory, or null if unbound
	 */
   public Memory getBoundMemory() {
   	checkNotClosed();
   	return boundMemory;
   }

   /**
    * {@inheritDoc}
    * 
    * <p>
    * For bounded views, always returns false as they represent single segments.
    * For unbounded views, delegates to the bound memory.
    * </p>
    */
   @Override
   public boolean hasNextSegment() {
   	checkBound();
   	if (proxyLength != -1) {
   		return false; // Bounded views have no chain
   	}
   	return boundMemory.hasNextSegment();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int incrementRef() {
   	checkBound();
   	return boundMemory.incrementRef();
   }

   /**
    * Checks if this proxy is currently bound to memory.
    * 
    * @return true if bound, false if unbound
    * @throws IllegalStateException if this proxy is closed
    */
   public boolean isBound() {
   	checkNotClosed();
   	return boundMemory != null;
   }

   /**
    * Checks if this proxy has been closed.
    * 
    * @return true if closed, false if still usable
    */
   public boolean isClosed() {
   	return isClosed;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isNull() {
   	checkBound();
   	return boundMemory.isNull();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isPointer() {
   	checkBound();
   	return boundMemory.isPointer();
   }

   /**
    * {@inheritDoc}
    * 
    * <p>
    * For bounded views, returns null as they don't support chain navigation.
    * For unbounded views, delegates to the bound memory.
    * </p>
    */
   @Override
   public Memory nextSegment() {
   	checkBound();
   	if (proxyLength != -1) {
   		return null; // Bounded views don't navigate chains
   	}
   	return boundMemory.nextSegment();
   }

   /**
    * Hook method called after successful memory binding.
    * 
    * <p>
    * Subclasses override this method to perform initialization after binding,
    * commonly used for cascading bindings of sub-elements. For example, an
    * Ethernet header proxy might bind MAC address proxies here.
    * </p>
    * 
    * <p>
    * This method is called after all binding state is set and the reference
    * count has been incremented, ensuring the proxy is fully initialized.
    * </p>
    * 
    * <pre>{@code
    * public class IpHeader extends MemoryProxy {
    *     private final IpAddress srcAddr = new IpAddress();
    *     private final IpAddress dstAddr = new IpAddress();
    *     
    *     @Override
    *     protected void onBindMemory() {
    *         srcAddr.bindMemory(this, 12, 4);  // Source IP at offset 12
    *         dstAddr.bindMemory(this, 16, 4);  // Dest IP at offset 16
    *     }
    *     
    *     @Override
    *     protected void onUnbindMemory() {
    *         dstAddr.unbindMemory();
    *         srcAddr.unbindMemory();
    *     }
    * }
    * }</pre>
    * 
    * <p>
    * The default implementation does nothing.
    * </p>
    */
   protected void onBindMemory() {
   	// Hook for subclasses
   }

   /**
    * Hook method called before memory unbinding.
    * 
    * <p>
    * Subclasses override this method to perform cleanup before unbinding,
    * typically to unbind sub-elements that were bound in {@link #onBindMemory()}.
    * Always unbind in reverse order of binding to maintain consistency.
    * </p>
    * 
    * <p>
    * This method is called before the reference count is decremented and
    * before the binding state is cleared.
    * </p>
    * 
    * <p>
    * The default implementation does nothing.
    * </p>
    */
   protected void onUnbindMemory() {
   	// Hook for subclasses
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int refCount() {
   	checkBound();
   	return boundMemory.refCount();
   }

   /**
    * {@inheritDoc}
    * 
    * <p>
    * Delegates to the bound memory with position adjustment for correct
    * segment resolution within the chain.
    * </p>
    */
   @Override
   public Memory seekSegment(long position) {
   	checkBound();
   	if (proxyLength != -1 && position >= proxyLength) {
   		throw new IllegalArgumentException(
   			String.format("Position %d beyond bounded view length %d", 
   				position, proxyLength));
   	}
   	return boundMemory.seekSegment(proxyOffset + position);
   }

   /**
    * {@inheritDoc}
    * 
    * <p>
    * For bounded views, returns 1 as they appear as single segments.
    * For unbounded views, returns the actual chain count.
    * </p>
    */
   @Override
   public int segmentCount() {
   	checkBound();
   	if (proxyLength != -1) {
   		return 1; // Bounded views appear as single segment
   	}
   	return boundMemory.segmentCount();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public long segmentEnd() {
   	checkBound();
   	if (proxyLength == -1) {
   		return boundMemory.segmentEnd(); // Unbounded - use bound's end
   	}
   	return boundMemory.segmentOffset() + proxyOffset + proxyLength;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public long segmentOffset() {
   	checkBound();
   	return boundMemory.segmentOffset() + proxyOffset;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public long segmentSize() {
   	checkBound();
   	if (proxyLength == -1) {
   		// Unbounded - calculate from offset to segment end
   		long start = boundMemory.segmentOffset() + proxyOffset;
   		long end = boundMemory.segmentEnd();
   		
   		// Safety check for overflow or invalid bounds
   		if (start < 0 || start > end) {
   			return 0;
   		}
   		return end - start;
   	}
   	return proxyLength;
   }

   /**
    * {@inheritDoc}
    * 
    * <p>
    * Chain modification is only supported for unbounded views.
    * Bounded views throw UnsupportedOperationException.
    * </p>
    */
   @Override
   public void setNextMemory(Memory next) {
   	checkBound();
   	if (proxyLength != -1) {
   		throw new UnsupportedOperationException(
   			"Cannot modify chain through bounded view");
   	}
   	boundMemory.setNextMemory(next);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String toString() {
   	if (isClosed) {
   		return "MemoryProxy[CLOSED]";
   	}
   	if (boundMemory == null) {
   		return "MemoryProxy[UNBOUND]";
   	}
   	String mode = (proxyLength == -1) ? "unbounded" : "bounded:" + proxyLength;
   	return String.format("MemoryProxy[%s, offset=%d, bound=%s]",
   		mode, proxyOffset, boundMemory.getClass().getSimpleName());
   }

   /**
    * {@inheritDoc}
    * 
    * <p>
    * For bounded views, returns the active bytes within the bounded region.
    * For unbounded views, returns the total from the offset position.
    * </p>
    */
   @Override
   public long totalActiveBytes() {
   	checkBound();
   	if (proxyLength != -1) {
   		// Bounded - active bytes within our view
   		long start = activeBytesStart();
   		long end = activeBytesEnd();
   		return end - start;
   	}
   	// Unbounded - total from our offset
   	long fullChainTotal = boundMemory.totalActiveBytes();
   	long skipped = proxyOffset;
   	return Math.max(0, fullChainTotal - skipped);
   }

   /**
    * {@inheritDoc}
    * 
    * <p>
    * For bounded views, returns the bounded segment size.
    * For unbounded views, returns the total from the offset position.
    * </p>
    */
   @Override
   public long totalSegmentSize() {
   	checkBound();
   	if (proxyLength != -1) {
   		return proxyLength; // Bounded view size
   	}
   	// Unbounded - calculate total from our offset
   	return boundMemory.totalSegmentSize() - proxyOffset;
   }

   /**
    * Unbinds this proxy from its currently bound memory.
    * 
    * <p>
    * Releases the binding and decrements the bound memory's reference count.
    * After unbinding, the proxy returns to unbound state and can be reused
    * by binding to different memory.
    * </p>
    * 
    * @throws IllegalStateException if this proxy is not bound or is closed
    */
   public void unbindMemory() {
   	checkNotClosed();
   	if (boundMemory == null) {
   		throw new IllegalStateException("Proxy is not bound");
   	}

   	onUnbindMemory();

   	boundMemory.decrementRef();
   	boundMemory = null;
   	proxyOffset = 0;
   	proxyLength = 0;
   }
}
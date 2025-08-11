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
package com.slytechs.jnet.core.api.memory;

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;

/**
 * A lightweight, rebindable proxy for managing access to memory chains with zero-allocation efficiency.
 * 
 * <p>MemoryProxy provides a reusable binding mechanism that can attach to different points in
 * memory chains for high-performance access without object allocation. This class is specifically
 * designed for scenarios requiring frequent repositioning across memory structures, such as
 * network packet processing, protocol header parsing, and streaming data analysis.</p>
 * 
 * <h2>Key Features</h2>
 * <ul>
 *   <li><strong>Zero-Allocation Rebinding:</strong> Reuse proxy objects without creating new instances</li>
 *   <li><strong>Chain Traversal:</strong> Efficient navigation through linked memory structures</li>
 *   <li><strong>Automatic Reference Management:</strong> Proper cleanup of bound memory references</li>
 *   <li><strong>Flexible Bounds:</strong> Support for both full memory and custom slice binding</li>
 * </ul>
 * 
 * <h2>Binding Lifecycle</h2>
 * <p>MemoryProxy follows a strict bind/unbind lifecycle:</p>
 * <ol>
 *   <li><strong>Creation:</strong> Proxy starts in unbound state</li>
 *   <li><strong>Binding:</strong> Attach to memory with {@link #bindMemory(Memory, long)} or variants</li>
 *   <li><strong>Usage:</strong> Access memory through Memory interface methods</li>
 *   <li><strong>Unbinding:</strong> Release with {@link #unbindMemory()} or automatic via {@link #close()}</li>
 *   <li><strong>Reuse:</strong> Rebind to different memory for next operation</li>
 * </ol>
 * 
 * <h2>Network Packet Processing Example</h2>
 * <pre>{@code
 * // Create reusable proxies for different protocol layers
 * MemoryProxy ethernetProxy = new MemoryProxy();
 * MemoryProxy ipProxy = new MemoryProxy();
 * MemoryProxy tcpProxy = new MemoryProxy();
 * 
 * // Process incoming packet
 * Memory packet = receivePacket();
 * 
 * // Bind to Ethernet header (offset 0, length 14)
 * ethernetProxy.bindMemory(packet, 0, 14);
 * int etherType = ethernetProxy.asMemorySegment().get(ValueLayout.JAVA_SHORT_UNALIGNED, 12);
 * 
 * if (etherType == 0x0800) { // IPv4
 *     // Bind to IP header (offset 14, length 20)
 *     ipProxy.bindMemory(packet, 14, 20);
 *     int protocol = ipProxy.asMemorySegment().get(ValueLayout.JAVA_BYTE, 9);
 *     
 *     if (protocol == 6) { // TCP
 *         // Bind to TCP header (offset 34, dynamic length)
 *         tcpProxy.bindMemory(packet, 34);
 *         processTcpHeader(tcpProxy);
 *     }
 * }
 * 
 * // Cleanup - proxies can be reused for next packet
 * ethernetProxy.unbindMemory();
 * ipProxy.unbindMemory();
 * tcpProxy.unbindMemory();
 * }</pre>
 * 
 * <h2>Chain Navigation Pattern</h2>
 * <pre>{@code
 * // Bind to start of chain
 * proxy.bindMemory(chainHead, 0);
 * 
 * // Process all segments
 * do {
 *     processSegment(proxy.asByteBuffer());
 * } while (proxy.nextMemory() != null);
 * 
 * // Unbind when done
 * proxy.unbindMemory();
 * }</pre>
 * 
 * <h2>Performance Characteristics</h2>
 * <ul>
 *   <li><strong>Binding:</strong> O(1) - Simple reference assignment and bounds calculation</li>
 *   <li><strong>Access:</strong> O(1) - Direct delegation to bound memory</li>
 *   <li><strong>Navigation:</strong> O(1) - Chain traversal without allocation</li>
 *   <li><strong>Memory:</strong> Fixed overhead regardless of bound memory size</li>
 * </ul>
 * 
 * <h2>Thread Safety</h2>
 * <p>MemoryProxy is <strong>not thread-safe</strong> for binding operations. Each thread should
 * use its own proxy instance, or external synchronization must be provided if sharing proxies
 * across threads. Once bound, read operations are thread-safe as long as the underlying
 * memory remains valid.</p>
 * 
 * <h2>State Management</h2>
 * <p>The proxy maintains internal state for navigation and bounds:</p>
 * <ul>
 *   <li><strong>Bound Memory:</strong> Reference to the currently bound memory chain head</li>
 *   <li><strong>Current Position:</strong> Track position within the chain for navigation</li>
 *   <li><strong>Local Bounds:</strong> Override bounds for slice operations</li>
 *   <li><strong>Closure State:</strong> Prevent operations on closed proxies</li>
 * </ul>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 * @see Memory for the complete memory interface
 * @see MemoryStructure for layout-aware proxy usage
 * @see MemoryPool for memory allocation
 */
public class MemoryProxy implements Memory {

    /**
     * The head of the currently bound memory chain.
     * 
     * <p>This field maintains a reference to the original memory object that this
     * proxy is bound to. When the proxy is unbound, this field is set to null.</p>
     */
    private Memory head;

    /**
     * Current navigation position within the memory chain.
     * 
     * <p>This field tracks the current position during chain traversal. It starts
     * as 'this' when initially bound, and changes as {@link #nextMemory()} is called
     * to navigate through the chain. Package-private for test access.</p>
     */
    Memory current;

    /**
     * Local memory offset override for slice binding.
     * 
     * <p>When the proxy is bound to a specific slice of memory, this field stores
     * the starting offset within the bound memory region.</p>
     */
    private long memoryOffset;

    /**
     * Local memory end override for slice binding.
     * 
     * <p>When the proxy is bound to a specific slice of memory, this field stores
     * the ending offset within the bound memory region (exclusive).</p>
     */
    private long memoryEnd;

    /**
     * Cached reference to next memory for navigation efficiency.
     * 
     * <p>This field caches the next memory in the chain to enable efficient
     * navigation without repeatedly querying the bound memory object.</p>
     */
    private Memory nextMemory;

    /**
     * Flag indicating if this proxy has been closed.
     * 
     * <p>Once closed, the proxy cannot be used for any operations and will
     * throw IllegalStateException for all method calls.</p>
     */
    private boolean isClosed;

    /**
     * Constructs a new MemoryProxy in unbound state.
     * 
     * <p>The proxy starts in unbound state and must be bound to memory using one of the
     * {@code bindMemory} methods before use. All Memory interface operations will throw
     * IllegalStateException until binding occurs.</p>
     */
    public MemoryProxy() {
        this.isClosed = false;
    }

    /**
     * Binds this proxy to the specified memory starting at the given offset.
     * 
     * <p>This method establishes a binding to the memory chain, setting up the proxy
     * to access memory from the specified offset to the end of the bound memory's capacity.
     * The proxy increments the bound memory's reference count to ensure validity.</p>
     * 
     * @param memory the memory chain to bind to
     * @param offset the starting offset within the memory
     * @throws MemoryBindingException if this proxy is already bound
     * @throws NullPointerException if memory is null
     * @throws IllegalArgumentException if offset is invalid
     * @throws IllegalStateException if this proxy is closed
     */
    public void bindMemory(Memory memory, long offset) {
        checkNotClosed();
        if (head != null) {
            throw new MemoryBindingException("Proxy is already bound to memory");
        }
        if (memory == null) {
            throw new NullPointerException("memory cannot be null");
        }
        if (offset < 0 || offset > memory.memoryCapacity()) {
            throw new IllegalArgumentException("Invalid offset: " + offset);
        }

        head = memory;
        head.incrementRef();
        current = this;
        memoryOffset = memory.memoryOffset() + offset;
        memoryEnd = memory.memoryEnd();
        nextMemory = memory.nextMemory();
        onBindMemory();
    }

    /**
     * Binds this proxy to the specified memory region with explicit bounds.
     * 
     * <p>This method establishes a binding to a specific slice of the memory chain,
     * setting up the proxy to access only the specified region. This enables precise
     * control over the accessible memory area.</p>
     * 
     * @param memory the memory chain to bind to
     * @param offset the starting offset within the memory
     * @param length the length of the accessible region
     * @throws MemoryBindingException if this proxy is already bound
     * @throws NullPointerException if memory is null
     * @throws IllegalArgumentException if offset or length is invalid
     * @throws IllegalStateException if this proxy is closed
     */
    public void bindMemory(Memory memory, long offset, long length) {
        checkNotClosed();
        if (head != null) {
            throw new MemoryBindingException("Proxy is already bound to memory");
        }
        if (memory == null) {
            throw new NullPointerException("memory cannot be null");
        }
        if (offset < 0 || offset > memory.memoryCapacity()) {
            throw new IllegalArgumentException("Invalid offset: " + offset);
        }
        if (length < 0 || offset + length > memory.memoryCapacity()) {
            throw new IllegalArgumentException("Invalid length: " + length);
        }

        head = memory;
        head.incrementRef();
        current = this;
        memoryOffset = memory.memoryOffset() + offset;
        memoryEnd = memory.memoryOffset() + offset + length;
        nextMemory = memory.nextMemory();
        onBindMemory();
    }

    /**
     * Unbinds this proxy from its currently bound memory.
     * 
     * <p>This method releases the binding to the current memory, decrements the
     * memory's reference count, and resets the proxy to unbound state. After
     * unbinding, the proxy can be reused by binding to different memory.</p>
     * 
     * @throws IllegalStateException if this proxy is closed or not bound
     */
    public void unbindMemory() {
        checkNotClosed();
        if (head == null) {
            throw new IllegalStateException("Proxy is not bound to memory");
        }

        onUnbindMemory();
        head.decrementRef();
        head = null;
        current = null;
        nextMemory = null;
        memoryOffset = 0;
        memoryEnd = 0;
    }

    /**
     * Checks if this proxy is currently bound to memory.
     * 
     * @return {@code true} if bound to memory, {@code false} if unbound
     * @throws IllegalStateException if this proxy is closed
     */
    public boolean isBound() {
        checkNotClosed();
        return head != null;
    }

    /**
     * Returns the currently bound memory head, or null if unbound.
     * 
     * <p>This method provides access to the original memory object that this
     * proxy is bound to, useful for debugging and advanced operations.</p>
     * 
     * @return the bound memory head, or {@code null} if unbound
     * @throws IllegalStateException if this proxy is closed
     */
    public Memory getBoundMemory() {
        checkNotClosed();
        return head;
    }

    // Memory interface implementation - delegates to bound memory or provides proxy behavior

    @Override
    public ByteBuffer asByteBuffer() {
        checkBound();
        if (current == this) {
            return head.asMemorySegment()
                .asSlice(memoryOffset - head.memoryOffset(), memoryEnd - memoryOffset)
                .asByteBuffer();
        }
        return current.asByteBuffer();
    }

    @Override
    public MemorySegment asMemorySegment() {
        checkBound();
        if (current == this) {
            return head.asMemorySegment()
                .asSlice(memoryOffset - head.memoryOffset(), memoryEnd - memoryOffset);
        }
        return current.asMemorySegment();
    }

    @Override
    public MemorySegment asMemorySegmentAt(long chainOffset) {
        checkBound();
        return Memory.super.asMemorySegmentAt(chainOffset);
    }

    @Override
    public Memory nextMemory() {
        checkBound();
        if (current == this) {
            current = nextMemory;
            return nextMemory;
        }
        if (current != null) {
            current = current.nextMemory();
            return current;
        }
        return null;
    }

    @Override
    public boolean hasNextMemory() {
        checkBound();
        if (current == this) {
            return nextMemory != null;
        }
        return current != null && current.hasNextMemory();
    }

    @Override
    public Memory seekMemory(long chainOffset) {
        checkBound();
        if (nextMemory == null) {
            if (chainOffset < 0 || chainOffset >= memoryDataLength()) {
                throw new IllegalArgumentException("chainOffset out of bounds: " + chainOffset);
            }
            return this;
        }
        
        long currentOffset = 0;
        Memory search = head;
        while (search != null && chainOffset >= (currentOffset += search.memoryDataLength())) {
            search = search.nextMemory();
        }
        
        if (search == null) {
            throw new IllegalArgumentException("chainOffset out of bounds: " + chainOffset);
        }
        return search;
    }

    @Override
    public boolean isNull() {
        checkBound();
        return Memory.isNull(asMemorySegment());
    }

    @Override
    public boolean isPointer() {
        checkBound();
        if (current == this) {
            return head.isPointer();
        }
        return current.isPointer();
    }

    @Override
    public long memoryCapacity() {
        checkBound();
        return current == this ? memoryEnd - memoryOffset : current.memoryCapacity();
    }

    @Override
    public long memoryOffset() {
        checkBound();
        return current == this ? memoryOffset : current.memoryOffset();
    }

    @Override
    public long memoryEnd() {
        checkBound();
        return current == this ? memoryEnd : current.memoryEnd();
    }

    @Override
    public long memoryDataOffset() {
        checkBound();
        return current == this ? memoryOffset : current.memoryDataOffset();
    }

    @Override
    public long memoryDataEnd() {
        checkBound();
        return current == this ? memoryEnd : current.memoryDataEnd();
    }

    @Override
    public long memoryDataLength() {
        checkBound();
        return current == this ? memoryEnd - memoryOffset : current.memoryDataLength();
    }

    @Override
    public long chainDataLength() {
        checkBound();
        if (nextMemory == null && current == this) {
            return memoryEnd - memoryOffset;
        }
        
        long total = 0;
        for (Memory mem = head; mem != null; mem = mem.nextMemory()) {
            total += mem.memoryDataLength();
        }
        return total;
    }

    @Override
    public int chainMemoryCount() {
        checkBound();
        int count = 0;
        for (Memory mem = head; mem != null; mem = mem.nextMemory()) {
            count++;
        }
        return count;
    }

    @Override
    public int refCount() {
        checkBound();
        return head.refCount();
    }

    @Override
    public int incrementRef() {
        checkBound();
        return head.incrementRef();
    }

    @Override
    public int decrementRef() {
        checkBound();
        int newRef = head.decrementRef();
        if (newRef == 0) {
            close();
        }
        return newRef;
    }

    @Override
    public void setNextMemory(Memory next) {
        checkBound();
        if (head != null) {
            head.setNextMemory(next);
        }
    }

    @Override
    public void close() {
        if (!isClosed && head != null) {
            unbindMemory();
        }
        isClosed = true;
    }

    /**
     * Hook method called after successful memory binding.
     * 
     * <p>Subclasses can override this method to perform additional initialization
     * after binding to memory. The default implementation does nothing.</p>
     */
    protected void onBindMemory() {
        // Default implementation - subclasses can override
    }

    /**
     * Hook method called before memory unbinding.
     * 
     * <p>Subclasses can override this method to perform cleanup operations
     * before releasing the memory binding. The default implementation does nothing.</p>
     */
    protected void onUnbindMemory() {
        // Default implementation - subclasses can override
    }

    /**
     * Validates that this proxy is not closed.
     * 
     * @throws IllegalStateException if this proxy is closed
     */
    private void checkNotClosed() {
        if (isClosed) {
            throw new IllegalStateException("Memory proxy is closed");
        }
    }

    /**
     * Validates that this proxy is bound to memory.
     * 
     * @throws IllegalStateException if this proxy is not bound or is closed
     */
    private void checkBound() {
        checkNotClosed();
        if (head == null) {
            throw new IllegalStateException("Memory proxy is not bound");
        }
    }

    @Override
    public String toString() {
        if (isClosed) {
            return "MemoryProxy[CLOSED]";
        }
        if (head == null) {
            return "MemoryProxy[UNBOUND]";
        }
        return String.format("MemoryProxy[bound=%s, offset=%d, end=%d, current=%s]",
            head.getClass().getSimpleName(), memoryOffset, memoryEnd, 
            current == this ? "self" : current.getClass().getSimpleName());
    }

	/**
	 * @see com.slytechs.jnet.core.api.memory.MemoryWindow#memoryDataOffset(long)
	 */
	@Override
	public long memoryDataOffset(long newOffset) {
        checkNotClosed();
        
        return current.memoryDataOffset(newOffset);
	}

	/**
	 * @see com.slytechs.jnet.core.api.memory.MemoryWindow#memoryDataEnd(long)
	 */
	@Override
	public long memoryDataEnd(long newEnd) {
        checkNotClosed();
        
        return current.memoryDataEnd(newEnd);
	}
}
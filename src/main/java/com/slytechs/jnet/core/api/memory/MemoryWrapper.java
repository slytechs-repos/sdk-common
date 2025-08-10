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

/**
 * Simple, immutable wrapper around a MemorySegment providing Memory interface access.
 * 
 * <p>MemoryWrapper provides the most lightweight implementation of the Memory interface,
 * serving as a direct wrapper around MemorySegment with fixed bounds. This class extends
 * {@link AbstractMemory} and inherits all standard Memory functionality without adding
 * any additional state or behavior beyond the base implementation.</p>
 * 
 * <h2>Design Philosophy</h2>
 * <p>MemoryWrapper embodies the "thin wrapper" pattern, providing just enough abstraction
 * to present a MemorySegment through the Memory interface while maintaining minimal overhead.
 * It serves as the foundational building block for the Memory API's factory methods.</p>
 * 
 * <h2>Key Characteristics</h2>
 * <ul>
 *   <li><strong>Minimal Implementation:</strong> No additional fields or state beyond AbstractMemory</li>
 *   <li><strong>Immutable Bounds:</strong> Memory boundaries are fixed at construction time</li>
 *   <li><strong>Zero Overhead:</strong> Direct delegation to AbstractMemory with no additional cost</li>
 *   <li><strong>Thread Safe:</strong> Immutable state ensures safe concurrent access</li>
 *   <li><strong>No Pool Integration:</strong> Simple reference counting without pool return</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * 
 * <h3>Factory Method Implementation</h3>
 * <p>MemoryWrapper is primarily used internally by Memory factory methods:</p>
 * <pre>{@code
 * // Memory.of() methods create MemoryWrapper instances
 * Memory memory1 = Memory.of(segment, 0);           // Wraps from offset to end
 * Memory memory2 = Memory.of(segment, 100, 200);    // Wraps specific region
 * }</pre>
 * 
 * <h3>Direct Instantiation</h3>
 * <p>Can be instantiated directly for custom memory wrapping scenarios:</p>
 * <pre>{@code
 * MemorySegment segment = Arena.global().allocate(1024);
 * Memory wrapper = new MemoryWrapper(segment, 0, segment.byteSize());
 * 
 * // Use as any Memory object
 * ByteBuffer buffer = wrapper.asByteBuffer();
 * MemorySegment slice = wrapper.asMemorySegment();
 * }</pre>
 * 
 * <h2>Memory Layout</h2>
 * <p>MemoryWrapper treats the entire specified region as both memory bounds and data bounds:</p>
 * <pre>{@code
 * |----------- Memory Region ------------|
 * |----------- Data Region --------------|
 * ↑                                      ↑
 * memoryOffset                   memoryEnd
 * memoryDataOffset             memoryDataEnd
 * 
 * All bounds are identical:
 * - memoryOffset = memoryDataOffset
 * - memoryEnd = memoryDataEnd  
 * - memoryCapacity = memoryDataLength
 * }</pre>
 * 
 * <h2>Performance Characteristics</h2>
 * <ul>
 *   <li><strong>Construction:</strong> O(1) - Parameter validation and super() call</li>
 *   <li><strong>Method Calls:</strong> O(1) - Direct delegation to AbstractMemory</li>
 *   <li><strong>Memory Footprint:</strong> Same as AbstractMemory (no additional fields)</li>
 *   <li><strong>Thread Safety:</strong> Lock-free for all operations</li>
 * </ul>
 * 
 * <h2>Comparison with Other Implementations</h2>
 * <table border="1">
 * <tr><th>Feature</th><th>MemoryWrapper</th><th>MemorySlice</th><th>MemoryBuffer</th><th>MemoryProxy</th></tr>
 * <tr><td>Additional Fields</td><td>None</td><td>2 (data bounds)</td><td>3 (data + pool)</td><td>5 (proxy state)</td></tr>
 * <tr><td>Data Bounds</td><td>Fixed</td><td>Mutable</td><td>Mutable</td><td>Delegated</td></tr>
 * <tr><td>Pool Support</td><td>No</td><td>No</td><td>Yes</td><td>No</td></tr>
 * <tr><td>Primary Use</td><td>Factory methods</td><td>Windowing</td><td>Buffer mgmt</td><td>Rebinding</td></tr>
 * </table>
 * 
 * <h2>When to Use MemoryWrapper</h2>
 * <ul>
 *   <li><strong>Simple Wrapping:</strong> When you need Memory interface for a MemorySegment</li>
 *   <li><strong>Factory Implementation:</strong> Building Memory objects from segments</li>
 *   <li><strong>Read-Only Access:</strong> When bounds will never change</li>
 *   <li><strong>Minimal Overhead:</strong> When every byte of memory footprint matters</li>
 *   <li><strong>Temporary Objects:</strong> Short-lived Memory instances</li>
 * </ul>
 * 
 * <h2>Alternatives</h2>
 * <ul>
 *   <li><strong>MemorySlice:</strong> When you need mutable data bounds within fixed memory</li>
 *   <li><strong>MemoryBuffer:</strong> When you need full buffer management with pool integration</li>
 *   <li><strong>MemoryProxy:</strong> When you need to rebind to different memory locations</li>
 * </ul>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 * @see Memory#of(MemorySegment, long) for factory creation
 * @see Memory#of(MemorySegment, long, long) for bounded factory creation
 * @see AbstractMemory for inherited functionality
 * @see MemorySlice for mutable data bounds
 * @see MemoryBuffer for poolable buffer management
 */
final class MemoryWrapper extends AbstractMemory {

    /**
     * Constructs a MemoryWrapper around the specified MemorySegment region.
     * 
     * <p>Creates an immutable Memory view covering the specified byte range within
     * the MemorySegment. The wrapper delegates all functionality to {@link AbstractMemory}
     * and provides no additional behavior beyond the standard Memory interface.</p>
     * 
     * <p><strong>Bounds Validation:</strong> All bounds validation is performed by the
     * {@link AbstractMemory} constructor, which ensures:</p>
     * <ul>
     *   <li>memorySegment is not null</li>
     *   <li>memoryOffset ≥ 0</li>
     *   <li>memoryEnd ≥ memoryOffset</li>
     *   <li>memoryEnd - memoryOffset ≤ memorySegment.byteSize()</li>
     * </ul>
     * 
     * <p><strong>Reference Counting:</strong> The wrapper starts with reference count = 1
     * and follows standard Memory lifecycle management through AbstractMemory.</p>
     * 
     * <p><strong>Factory Integration:</strong> This constructor is primarily used by
     * {@link Memory#of(MemorySegment, long)} and {@link Memory#of(MemorySegment, long, long)}
     * factory methods to create lightweight Memory objects.</p>
     * 
     * @param memorySegment the backing MemorySegment, must not be null
     * @param memoryOffset the starting offset within the segment (inclusive)
     * @param memoryEnd the ending offset within the segment (exclusive)
     * @throws NullPointerException if memorySegment is null
     * @throws IndexOutOfBoundsException if bounds are invalid
     * 
     * @see AbstractMemory#AbstractMemory(MemorySegment, long, long) for detailed validation rules
     * @see Memory#of(MemorySegment, long) for single-offset factory method
     * @see Memory#of(MemorySegment, long, long) for bounded factory method
     */
    public MemoryWrapper(MemorySegment memorySegment, long memoryOffset, long memoryEnd) {
        super(memorySegment, memoryOffset, memoryEnd);
    }
}
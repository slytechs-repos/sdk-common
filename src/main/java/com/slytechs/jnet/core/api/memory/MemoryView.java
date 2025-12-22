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
 * Lightweight view providing positioning information for memory regions.
 * 
 * <p>
 * MemoryView is a simple data container that holds the essential positioning
 * information needed to access a region of memory. It contains just four fields:
 * the memory segment, starting offset, length, and optional source reference for
 * reference counting delegation.
 * </p>
 * 
 * <h2>Design Principles</h2>
 * <ul>
 * <li><strong>Minimal overhead:</strong> Just 4 fields, no methods beyond accessors</li>
 * <li><strong>Mutable fields:</strong> Can be reused by updating fields directly</li>
 * <li><strong>No ownership:</strong> Views don't own memory, they just reference it</li>
 * <li><strong>Pre-allocated:</strong> Typically created once and reused via binding</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * 
 * <h3>Direct View Sharing</h3>
 * <pre>{@code
 * // When binding at offset 0, share the source's view directly
 * MemoryView view = memory.view();  // No copying, just reference
 * }</pre>
 * 
 * <h3>Mapped View</h3>
 * <pre>{@code
 * // When binding with offset, update a pre-allocated view
 * MemoryView mappedView = new MemoryView();  // Pre-allocated
 * mappedView.segment = memory.segment();
 * mappedView.start = memory.start() + offset;
 * mappedView.length = length;
 * mappedView.source = memory;
 * }</pre>
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @since 1.0
 */
public class MemoryView {
    
    /**
     * The underlying memory segment.
     * 
     * <p>
     * This is the native memory segment that contains the actual data.
     * It may be shared across multiple views.
     * </p>
     */
    MemorySegment segment;
    
    /**
     * The starting offset of data within the segment.
     * 
     * <p>
     * This is an absolute offset from the beginning of the segment where
     * the viewed data begins.
     * </p>
     */
    long start;
    
    /**
     * The length of data in bytes.
     * 
     * <p>
     * The view spans from start (inclusive) to start + length (exclusive).
     * </p>
     */
    long length;
    
    /**
     * Optional reference to the source Memory for reference counting.
     * 
     * <p>
     * When a view is bound to a Memory object, this field holds the reference
     * to enable proper reference counting delegation. May be null for views
     * that don't require reference counting.
     * </p>
     */
    Memory source;
    
    /**
     * Constructs an empty MemoryView.
     * 
     * <p>
     * All fields are initialized to null/0. The view should be bound to
     * actual memory before use.
     * </p>
     */
    public MemoryView() {
        // Fields default to null/0
    }
    
    /**
     * Constructs a MemoryView with the specified values.
     * 
     * @param segment the memory segment
     * @param start the starting offset
     * @param length the data length
     * @param source the source Memory (may be null)
     */
    public MemoryView(MemorySegment segment, long start, long length, Memory source) {
        this.segment = segment;
        this.start = start;
        this.length = length;
        this.source = source;
    }
    
    /**
     * Returns the segment.
     * 
     * @return the memory segment
     */
    public MemorySegment segment() {
        return segment;
    }
    
    /**
     * Returns the starting offset.
     * 
     * @return the start offset
     */
    public long start() {
        return start;
    }
    
    /**
     * Returns the data length.
     * 
     * @return the length in bytes
     */
    public long length() {
        return length;
    }
    
    /**
     * Returns the ending offset (exclusive).
     * 
     * @return start + length
     */
    public long end() {
        return start + length;
    }
    
    /**
     * Returns the source Memory.
     * 
     * @return the source or null
     */
    public Memory source() {
        return source;
    }
    
    /**
     * Checks if this view is bound to a source.
     * 
     * @return true if source is not null
     */
    public boolean isBound() {
        return source != null;
    }
    
    /**
     * Clears this view's fields.
     * 
     * <p>
     * Resets all fields to null/0. Useful for cleanup after unbinding.
     * </p>
     */
    public void clear() {
        this.segment = null;
        this.start = 0;
        this.length = 0;
        this.source = null;
    }

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MemoryView [start=" + start + ", length=" + length + "]";
	}
}
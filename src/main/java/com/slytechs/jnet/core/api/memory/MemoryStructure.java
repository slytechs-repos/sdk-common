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

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemoryLayout.*;
import static java.lang.foreign.ValueLayout.*;

/**
 * The Class MemoryStructure.
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class MemoryStructure extends AbstractMemory {

	/** The layout. */
	private final MemoryLayout layout;

	/**
	 * Instantiates a new memory structure.
	 *
	 * @param byteSize the byte size
	 * @param arena    the arena
	 */
	public MemoryStructure(long byteSize, Arena arena) {
		this(sequenceLayout(byteSize, JAVA_BYTE), arena);
	}

	/**
	 * Instantiates a new memory structure.
	 *
	 * @param layout the layout
	 * @param arena  the arena
	 */
	public MemoryStructure(MemoryLayout layout, Arena arena) {
		this(layout, arena.allocate(layout), 0);
	}

	/**
	 * Instantiates a new memory structure.
	 *
	 * @param layout  the layout
	 * @param pointer the pointer
	 */
	public MemoryStructure(MemoryLayout layout, MemorySegment pointer) {
		this(layout, pointer.reinterpret(layout.byteSize()), 0);
	}

	/**
	 * Instantiates a new memory structure.
	 *
	 * @param layout  the layout
	 * @param pointer the pointer
	 * @param arena   the arena
	 */
	public MemoryStructure(MemoryLayout layout, MemorySegment pointer, Arena arena) {
		this(layout, pointer.reinterpret(layout.byteSize(), arena, null), 0);
	}

	/**
	 * Instantiates a new memory structure.
	 *
	 * @param layout  the layout
	 * @param segment the segment
	 * @param offset  the offset
	 */
	public MemoryStructure(MemoryLayout layout, MemorySegment segment, long offset) {
		super(segment, offset, offset + segment.byteSize());
		this.layout = layout;

		assert segment.byteSize() > 0;
	}

	/**
	 * Size of.
	 *
	 * @return the long
	 */
	public long sizeOf() {
		return layout.byteSize();
	}

	/**
	 * Gets the compact memory layout.
	 *
	 * @return the compact memory layout
	 */
	public MemoryLayout getCompactMemoryLayout() {
		return layout;
	}

	/**
	 * Gets the memory layout.
	 *
	 * @return the memory layout
	 */
	public final MemoryLayout getMemoryLayout() {
		return layout;
	}

	/**
	 * Gets the padded memory layout.
	 *
	 * @return the padded memory layout
	 */
	public MemoryLayout getPaddedMemoryLayout() {
		return layout;
	}
}

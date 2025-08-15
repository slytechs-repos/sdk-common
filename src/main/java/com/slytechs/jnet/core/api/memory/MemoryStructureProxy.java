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

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemoryLayout.*;
import static java.lang.foreign.ValueLayout.*;

/**
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class MemoryStructureProxy extends MemoryProxy {

	private final MemoryLayout layout;

	public MemoryStructureProxy(long byteSize, Arena arena) {
		this(sequenceLayout(byteSize, JAVA_BYTE), arena);
	}

	public MemoryStructureProxy(MemoryLayout layout) {
		this.layout = layout;
	}

	public MemoryStructureProxy(MemoryLayout layout, Arena arena) {
		this(layout, arena.allocate(layout), 0);
	}

	public MemoryStructureProxy(MemoryLayout layout, MemorySegment pointer) {
		this(layout, pointer.reinterpret(layout.byteSize()), 0);
	}

	public MemoryStructureProxy(MemoryLayout layout, MemorySegment pointer, Arena arena) {
		this(layout, pointer.reinterpret(layout.byteSize(), arena, null), 0);
	}

	public MemoryStructureProxy(MemoryLayout layout, MemorySegment segment, long offset) {
		this.layout = layout;

		assert segment.byteSize() > 0;
	}

	public long sizeOf() {
		return layout.byteSize();
	}

	public MemoryLayout getCompactMemoryLayout() {
		return layout;
	}

	public final MemoryLayout getMemoryLayout() {
		return layout;
	}

	public MemoryLayout getPaddedMemoryLayout() {
		return layout;
	}
}

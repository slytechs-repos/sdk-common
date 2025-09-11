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

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;

/**
 * Type-safe wrapper for VarHandle operations on memory segments and views.
 * 
 * <p>
 * MemoryHandle provides a type-safe abstraction over {@link VarHandle} for
 * accessing primitive values in native memory. It eliminates the need for
 * casting from Object and provides specialized methods for working with
 * {@link Memory} and {@link MemoryView} objects, while maintaining the
 * performance benefits of VarHandle's JIT optimizations.
 * </p>
 * 
 * <h2>Design Principles</h2>
 * <ul>
 * <li><strong>Type Safety:</strong> Concrete implementations provide
 * type-specific methods that return primitives directly without casting</li>
 * <li><strong>Zero Overhead:</strong> All operations delegate to VarHandle,
 * allowing the JIT compiler to optimize to native memory access speeds</li>
 * <li><strong>View Integration:</strong> First-class support for MemoryView's
 * pre-calculated offsets and Memory's segment boundaries</li>
 * <li><strong>Layout Integration:</strong> Can be constructed directly from
 * MemoryLayout with path navigation</li>
 * </ul>
 * 
 * <h2>Construction Methods</h2>
 * <p>
 * MemoryHandle supports three construction patterns:
 * </p>
 * <ol>
 * <li><strong>Direct VarHandle:</strong> Pass a pre-created VarHandle</li>
 * <li><strong>Layout with PathElements:</strong> Navigate layout structure
 * programmatically</li>
 * <li><strong>Layout with String paths:</strong> Simple string-based navigation
 * with array syntax</li>
 * </ol>
 * 
 * <h2>String Path Syntax</h2>
 * <p>
 * The string path constructor supports intuitive navigation:
 * </p>
 * <ul>
 * <li>{@code "fieldName"} - Navigate to a struct field (groupElement)</li>
 * <li>{@code "arrayName[]"} - Navigate to array element (sequenceElement)</li>
 * <li>{@code "arrayName[3]"} - Navigate to specific array index
 * (sequenceElement(3))</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * // Create handles from layout
 * IntHandle srcAddr = new IntHandle(IP4_LAYOUT, "hdr_src_addr");
 * ShortHandle length = new ShortHandle(IP4_LAYOUT, "hdr_total_length");
 * 
 * // Access through Memory
 * Memory packet = pool.allocate();
 * int src = srcAddr.getInt(packet);
 * 
 * // Access through MemoryView  
 * MemoryView view = packet.view();
 * short len = length.getShort(view);
 * 
 * // Array access
 * IntHandle timestamps = new IntHandle(LAYOUT, "timestamps[]");
 * int firstTimestamp = timestamps.getIntAtIndex(packet, 0);
 * }</pre>
 * 
 * @param <T> the boxed type this handle operates on
 * @author Mark Bednarczyk
 * @author Sly Technologies Inc.
 * @since 1.0
 * @see VarHandle
 * @see Memory
 * @see MemoryView
 */
public abstract class MemoryHandle<T> {

	public static long byteOffset(MemoryLayout layout, String... path) {
		var elements = parsePath(path);

		return layout.byteOffset(elements);
	}

	public static long byteOffset(MemoryLayout layout, PathElement... path) {
		return layout.byteOffset(path);
	}

	/**
	 * Gets the underlying bound VarHandle.
	 *
	 * @return the backing VarHandle
	 */
	public VarHandle varHandle() {
		return handle;
	}

	/** The underlying VarHandle that performs actual memory access */
	protected final VarHandle handle;

	/**
	 * Constructs a MemoryHandle with a pre-created VarHandle.
	 * 
	 * <p>
	 * This constructor is used when you already have a VarHandle, typically
	 * obtained from {@link MemoryLayout#varHandle(PathElement...)}.
	 * </p>
	 * 
	 * @param handle the VarHandle for memory access
	 * @throws NullPointerException if handle is null
	 */
	protected MemoryHandle(VarHandle handle) {
		if (handle == null) {
			throw new NullPointerException("VarHandle cannot be null");
		}
		this.handle = handle;
	}

	/**
	 * Constructs a MemoryHandle by navigating a MemoryLayout with PathElements.
	 * 
	 * <p>
	 * This constructor creates a VarHandle by navigating the provided layout
	 * structure using the specified path elements.
	 * </p>
	 * 
	 * @param layout the memory layout to navigate
	 * @param path   the path elements for navigation
	 * @throws NullPointerException     if layout is null
	 * @throws IllegalArgumentException if the path is invalid for the layout
	 */
	protected MemoryHandle(MemoryLayout layout, PathElement... path) {
		if (layout == null) {
			throw new NullPointerException("MemoryLayout cannot be null");
		}
		this.handle = layout.varHandle(path);
	}

	/**
	 * Constructs a MemoryHandle using string-based path navigation.
	 * 
	 * <p>
	 * This constructor provides a convenient string-based syntax for navigating
	 * layout structures. Each string element is parsed according to these rules:
	 * </p>
	 * <ul>
	 * <li>{@code "fieldName"} - Becomes {@code groupElement("fieldName")}</li>
	 * <li>{@code "arrayName[]"} - Becomes {@code sequenceElement()}</li>
	 * <li>{@code "arrayName[3]"} - Becomes {@code sequenceElement(3)}</li>
	 * </ul>
	 * 
	 * @param layout the memory layout to navigate
	 * @param path   the string path elements for navigation
	 * @throws NullPointerException     if layout is null
	 * @throws IllegalArgumentException if the path syntax is invalid
	 * @throws NumberFormatException    if an array index is not a valid number
	 */
	protected MemoryHandle(MemoryLayout layout, String... path) {
		if (layout == null) {
			throw new NullPointerException("MemoryLayout cannot be null");
		}
		this.handle = layout.varHandle(parsePath(path));
	}

	/**
	 * Parses string path elements into MemoryLayout PathElements.
	 * 
	 * <p>
	 * Converts string-based path notation into PathElement objects for layout
	 * navigation:
	 * </p>
	 * <ul>
	 * <li>{@code "field"} → {@code PathElement.groupElement("field")}</li>
	 * <li>{@code "array[]"} → {@code PathElement.sequenceElement()}</li>
	 * <li>{@code "array[5]"} → {@code PathElement.sequenceElement(5)}</li>
	 * </ul>
	 * 
	 * @param path array of string path elements
	 * @return array of PathElements for layout navigation
	 * @throws IllegalArgumentException if path syntax is invalid
	 * @throws NumberFormatException    if array index is not a valid number
	 */
	private static PathElement[] parsePath(String... path) {
		List<PathElement> elements = new ArrayList<>();

		for (String element : path) {
			if (element == null || element.isEmpty()) {
				throw new IllegalArgumentException("Path element cannot be null or empty");
			}

			if (element.endsWith("[]")) {
				// Array access without index: "arrayName[]"
				elements.add(PathElement.sequenceElement());
			} else if (element.contains("[") && element.endsWith("]")) {
				// Array access with index: "arrayName[3]"
				int bracketIdx = element.indexOf('[');
				String indexStr = element.substring(bracketIdx + 1, element.length() - 1);

				if (indexStr.isEmpty()) {
					// Just "name[]"
					elements.add(PathElement.sequenceElement());
				} else {
					// "name[3]"
					try {
						long index = Long.parseLong(indexStr);
						elements.add(PathElement.sequenceElement(index));
					} catch (NumberFormatException e) {
						throw new IllegalArgumentException(
								"Invalid array index in path element: " + element, e);
					}
				}
			} else if (element.contains("[") || element.contains("]")) {
				// Malformed array syntax
				throw new IllegalArgumentException(
						"Malformed array syntax in path element: " + element);
			} else {
				// Regular group element: "fieldName"
				elements.add(PathElement.groupElement(element));
			}
		}

		return elements.toArray(new PathElement[0]);
	}

	// ==================== Abstract Methods ====================

	/**
	 * Gets a value from the specified memory segment at the given offset.
	 * 
	 * @param segment the memory segment to read from
	 * @param offset  the offset within the segment
	 * @return the value at the specified location
	 */
	public abstract T get(MemorySegment segment, long offset);

	/**
	 * Sets a value in the specified memory segment at the given offset.
	 * 
	 * @param segment the memory segment to write to
	 * @param offset  the offset within the segment
	 * @param value   the value to write
	 */
	public abstract void set(MemorySegment segment, long offset, T value);

	/**
	 * Gets a value from an array element at the specified index.
	 * 
	 * @param segment    the memory segment containing the array
	 * @param baseOffset the offset to the start of the array
	 * @param index      the array index
	 * @return the value at the specified array element
	 */
	public abstract T getAtIndex(MemorySegment segment, long baseOffset, long index);

	/**
	 * Sets a value in an array element at the specified index.
	 * 
	 * @param segment    the memory segment containing the array
	 * @param baseOffset the offset to the start of the array
	 * @param index      the array index
	 * @param value      the value to write
	 */
	public abstract void setAtIndex(MemorySegment segment, long baseOffset, long index, T value);

	// ==================== Memory Access Methods ====================

	/**
	 * Gets a value from the specified Memory object.
	 * 
	 * <p>
	 * Reads from the beginning of the Memory's active data region (start offset).
	 * </p>
	 * 
	 * @param memory the memory to read from
	 * @return the value at the memory's start offset
	 * @throws NullPointerException if memory is null
	 */
	public T get(Memory memory) {
		return get(memory.segment(), memory.start());
	}

	/**
	 * Gets a value from the specified Memory object with additional offset.
	 * 
	 * @param memory the memory to read from
	 * @param offset additional offset from the memory's start position
	 * @return the value at the specified location
	 * @throws NullPointerException if memory is null
	 */
	public T get(Memory memory, long offset) {
		return get(memory.segment(), memory.start() + offset);
	}

	/**
	 * Sets a value in the specified Memory object.
	 * 
	 * @param memory the memory to write to
	 * @param value  the value to write
	 * @throws NullPointerException if memory is null
	 */
	public void set(Memory memory, T value) {
		set(memory.segment(), memory.start(), value);
	}

	/**
	 * Sets a value in the specified Memory object with additional offset.
	 * 
	 * @param memory the memory to write to
	 * @param offset additional offset from the memory's start position
	 * @param value  the value to write
	 * @throws NullPointerException if memory is null
	 */
	public void set(Memory memory, long offset, T value) {
		set(memory.segment(), memory.start() + offset, value);
	}

	// ==================== MemoryView Access Methods ====================

	/**
	 * Gets a value from the specified MemoryView.
	 * 
	 * <p>
	 * This method is optimized for views as it uses the pre-calculated start offset
	 * stored in the view.
	 * </p>
	 * 
	 * @param view the memory view to read from
	 * @return the value at the view's start offset
	 * @throws NullPointerException if view is null
	 */
	public T get(MemoryView view) {
		return get(view.segment, view.start);
	}

	/**
	 * Gets a value from the specified MemoryView with additional offset.
	 * 
	 * @param view   the memory view to read from
	 * @param offset additional offset from the view's start position
	 * @return the value at the specified location
	 * @throws NullPointerException if view is null
	 */
	public T get(MemoryView view, long offset) {
		return get(view.segment, view.start + offset);
	}

	/**
	 * Sets a value in the specified MemoryView.
	 * 
	 * @param view  the memory view to write to
	 * @param value the value to write
	 * @throws NullPointerException if view is null
	 */
	public void set(MemoryView view, T value) {
		set(view.segment, view.start, value);
	}

	/**
	 * Sets a value in the specified MemoryView with additional offset.
	 * 
	 * @param view   the memory view to write to
	 * @param offset additional offset from the view's start position
	 * @param value  the value to write
	 * @throws NullPointerException if view is null
	 */
	public void set(MemoryView view, long offset, T value) {
		set(view.segment, view.start + offset, value);
	}

	// ==================== Concrete Implementations ====================

	/**
	 * Type-safe handle for byte values in memory.
	 * 
	 * <p>
	 * Provides primitive-specific methods to avoid boxing overhead when working
	 * with byte values in native memory.
	 * </p>
	 */
	public static class ByteHandle extends MemoryHandle<Byte> {

		public ByteHandle(VarHandle handle) {
			super(handle);
		}

		public ByteHandle(MemoryLayout layout, PathElement... path) {
			super(layout, path);
		}

		public ByteHandle(MemoryLayout layout, String... path) {
			super(layout, path);
		}

		@Override
		public Byte get(MemorySegment segment, long offset) {
			return (byte) handle.get(segment, offset);
		}

		@Override
		public void set(MemorySegment segment, long offset, Byte value) {
			handle.set(segment, offset, value.byteValue());
		}

		@Override
		public Byte getAtIndex(MemorySegment segment, long baseOffset, long index) {
			return getByte(segment, baseOffset + index);
		}

		@Override
		public void setAtIndex(MemorySegment segment, long baseOffset, long index, Byte value) {
			setByte(segment, baseOffset + index, value.byteValue());
		}

		public byte getByte(MemorySegment segment, long offset) {
			return (byte) handle.get(segment, offset);
		}

		public void setByte(MemorySegment segment, long offset, byte value) {
			handle.set(segment, offset, value);
		}

		public byte getByte(Memory memory) {
			return getByte(memory.segment(), memory.start());
		}

		public byte getByte(Memory memory, long offset) {
			return getByte(memory.segment(), memory.start() + offset);
		}

		public byte getByte(MemoryView view) {
			return getByte(view.segment, view.start);
		}

		public byte getByte(MemoryView view, long offset) {
			return getByte(view.segment, view.start + offset);
		}

		public void setByte(Memory memory, byte value) {
			setByte(memory.segment(), memory.start(), value);
		}

		public void setByte(Memory memory, long offset, byte value) {
			setByte(memory.segment(), memory.start() + offset, value);
		}

		public void setByte(MemoryView view, byte value) {
			setByte(view.segment, view.start, value);
		}

		public void setByte(MemoryView view, long offset, byte value) {
			setByte(view.segment, view.start + offset, value);
		}

		public byte getByteAtIndex(MemorySegment segment, long baseOffset, long index) {
			return getByte(segment, baseOffset + index);
		}

		public byte getByteAtIndex(Memory memory, long index) {
			return getByte(memory.segment(), memory.start() + index);
		}

		public byte getByteAtIndex(MemoryView view, long index) {
			return getByte(view.segment, view.start + index);
		}

		public void setByteAtIndex(MemorySegment segment, long baseOffset, long index, byte value) {
			setByte(segment, baseOffset + index, value);
		}

		public void setByteAtIndex(Memory memory, long index, byte value) {
			setByte(memory.segment(), memory.start() + index, value);
		}

		public void setByteAtIndex(MemoryView view, long index, byte value) {
			setByte(view.segment, view.start + index, value);
		}
	}

	/**
	 * Type-safe handle for short values in memory.
	 */
	public static class ShortHandle extends MemoryHandle<Short> {

		public ShortHandle(VarHandle handle) {
			super(handle);
		}

		public ShortHandle(MemoryLayout layout, PathElement... path) {
			super(layout, path);
		}

		public ShortHandle(MemoryLayout layout, String... path) {
			super(layout, path);
		}

		@Override
		public Short get(MemorySegment segment, long offset) {
			return (short) handle.get(segment, offset);
		}

		@Override
		public void set(MemorySegment segment, long offset, Short value) {
			handle.set(segment, offset, value.shortValue());
		}

		@Override
		public Short getAtIndex(MemorySegment segment, long baseOffset, long index) {
			return getShort(segment, baseOffset + (index * 2));
		}

		@Override
		public void setAtIndex(MemorySegment segment, long baseOffset, long index, Short value) {
			setShort(segment, baseOffset + (index * 2), value.shortValue());
		}

		public short getShort(MemorySegment segment, long offset) {
			return (short) handle.get(segment, offset);
		}

		public void setShort(MemorySegment segment, long offset, short value) {
			handle.set(segment, offset, value);
		}

		public short getShort(Memory memory) {
			return getShort(memory.segment(), memory.start());
		}

		public short getShort(Memory memory, long offset) {
			return getShort(memory.segment(), memory.start() + offset);
		}

		public short getShort(MemoryView view) {
			return getShort(view.segment, view.start);
		}

		public short getShort(MemoryView view, long offset) {
			return getShort(view.segment, view.start + offset);
		}

		public void setShort(Memory memory, short value) {
			setShort(memory.segment(), memory.start(), value);
		}

		public void setShort(Memory memory, long offset, short value) {
			setShort(memory.segment(), memory.start() + offset, value);
		}

		public void setShort(MemoryView view, short value) {
			setShort(view.segment, view.start, value);
		}

		public void setShort(MemoryView view, long offset, short value) {
			setShort(view.segment, view.start + offset, value);
		}

		public short getShortAtIndex(MemorySegment segment, long baseOffset, long index) {
			return getShort(segment, baseOffset + (index * 2));
		}

		public short getShortAtIndex(Memory memory, long index) {
			return getShort(memory.segment(), memory.start() + (index * 2));
		}

		public short getShortAtIndex(MemoryView view, long index) {
			return getShort(view.segment, view.start + (index * 2));
		}

		public void setShortAtIndex(MemorySegment segment, long baseOffset, long index, short value) {
			setShort(segment, baseOffset + (index * 2), value);
		}

		public void setShortAtIndex(Memory memory, long index, short value) {
			setShort(memory.segment(), memory.start() + (index * 2), value);
		}

		public void setShortAtIndex(MemoryView view, long index, short value) {
			setShort(view.segment, view.start + (index * 2), value);
		}
	}

	/**
	 * Type-safe handle for int values in memory.
	 */
	public static class IntHandle extends MemoryHandle<Integer> {

		public IntHandle(VarHandle handle) {
			super(handle);
		}

		public IntHandle(MemoryLayout layout, PathElement... path) {
			super(layout, path);
		}

		public IntHandle(MemoryLayout layout, String... path) {
			super(layout, path);
		}

		@Override
		public Integer get(MemorySegment segment, long offset) {
			return (int) handle.get(segment, offset);
		}

		@Override
		public void set(MemorySegment segment, long offset, Integer value) {
			handle.set(segment, offset, value.intValue());
		}

		@Override
		public Integer getAtIndex(MemorySegment segment, long baseOffset, long index) {
			return getInt(segment, baseOffset + (index * 4));
		}

		@Override
		public void setAtIndex(MemorySegment segment, long baseOffset, long index, Integer value) {
			setInt(segment, baseOffset + (index * 4), value.intValue());
		}

		public int getInt(MemorySegment segment, long offset) {
			return (int) handle.get(segment, offset);
		}

		public void setInt(MemorySegment segment, long offset, int value) {
			handle.set(segment, offset, value);
		}

		public int getInt(Memory memory) {
			return getInt(memory.segment(), memory.start());
		}

		public int getInt(Memory memory, long offset) {
			return getInt(memory.segment(), memory.start() + offset);
		}

		public int getInt(MemoryView view) {
			return getInt(view.segment, view.start);
		}

		public int getInt(MemoryView view, long offset) {
			return getInt(view.segment, view.start + offset);
		}

		public void setInt(Memory memory, int value) {
			setInt(memory.segment(), memory.start(), value);
		}

		public void setInt(Memory memory, long offset, int value) {
			setInt(memory.segment(), memory.start() + offset, value);
		}

		public void setInt(MemoryView view, int value) {
			setInt(view.segment, view.start, value);
		}

		public void setInt(MemoryView view, long offset, int value) {
			setInt(view.segment, view.start + offset, value);
		}

		public int getIntAtIndex(MemorySegment segment, long baseOffset, long index) {
			return getInt(segment, baseOffset + (index * 4));
		}

		public int getIntAtIndex(Memory memory, long index) {
			return getInt(memory.segment(), memory.start() + (index * 4));
		}

		public int getIntAtIndex(MemoryView view, long index) {
			return getInt(view.segment, view.start + (index * 4));
		}

		public void setIntAtIndex(MemorySegment segment, long baseOffset, long index, int value) {
			setInt(segment, baseOffset + (index * 4), value);
		}

		public void setIntAtIndex(Memory memory, long index, int value) {
			setInt(memory.segment(), memory.start() + (index * 4), value);
		}

		public void setIntAtIndex(MemoryView view, long index, int value) {
			setInt(view.segment, view.start + (index * 4), value);
		}

		public boolean compareAndSet(MemorySegment segment, long offset, int expected, int value) {
			return handle.compareAndSet(segment, offset, expected, value);
		}

		public int getAndAdd(MemorySegment segment, long offset, int delta) {
			return (int) handle.getAndAdd(segment, offset, delta);
		}

		public int getAndSet(MemorySegment segment, long offset, int value) {
			return (int) handle.getAndSet(segment, offset, value);
		}
	}

	/**
	 * Type-safe handle for long values in memory.
	 */
	public static class LongHandle extends MemoryHandle<Long> {

		public LongHandle(VarHandle handle) {
			super(handle);
		}

		public LongHandle(MemoryLayout layout, PathElement... path) {
			super(layout, path);
		}

		public LongHandle(MemoryLayout layout, String... path) {
			super(layout, path);
		}

		@Override
		public Long get(MemorySegment segment, long offset) {
			return (long) handle.get(segment, offset);
		}

		@Override
		public void set(MemorySegment segment, long offset, Long value) {
			handle.set(segment, offset, value.longValue());
		}

		@Override
		public Long getAtIndex(MemorySegment segment, long baseOffset, long index) {
			return getLong(segment, baseOffset + (index * 8));
		}

		@Override
		public void setAtIndex(MemorySegment segment, long baseOffset, long index, Long value) {
			setLong(segment, baseOffset + (index * 8), value.longValue());
		}

		public long getLong(MemorySegment segment, long offset) {
			return (long) handle.get(segment, offset);
		}

		public void setLong(MemorySegment segment, long offset, long value) {
			handle.set(segment, offset, value);
		}

		public long getLong(Memory memory) {
			return getLong(memory.segment(), memory.start());
		}

		public long getLong(Memory memory, long offset) {
			return getLong(memory.segment(), memory.start() + offset);
		}

		public long getLong(MemoryView view) {
			return getLong(view.segment, view.start);
		}

		public long getLong(MemoryView view, long offset) {
			return getLong(view.segment, view.start + offset);
		}

		public void setLong(Memory memory, long value) {
			setLong(memory.segment(), memory.start(), value);
		}

		public void setLong(Memory memory, long offset, long value) {
			setLong(memory.segment(), memory.start() + offset, value);
		}

		public void setLong(MemoryView view, long value) {
			setLong(view.segment, view.start, value);
		}

		public void setLong(MemoryView view, long offset, long value) {
			setLong(view.segment, view.start + offset, value);
		}

		public long getLongAtIndex(MemorySegment segment, long baseOffset, long index) {
			return getLong(segment, baseOffset + (index * 8));
		}

		public long getLongAtIndex(Memory memory, long index) {
			return getLong(memory.segment(), memory.start() + (index * 8));
		}

		public long getLongAtIndex(MemoryView view, long index) {
			return getLong(view.segment, view.start + (index * 8));
		}

		public void setLongAtIndex(MemorySegment segment, long baseOffset, long index, long value) {
			setLong(segment, baseOffset + (index * 8), value);
		}

		public void setLongAtIndex(Memory memory, long index, long value) {
			setLong(memory.segment(), memory.start() + (index * 8), value);
		}

		public void setLongAtIndex(MemoryView view, long index, long value) {
			setLong(view.segment, view.start + (index * 8), value);
		}

		public boolean compareAndSet(MemorySegment segment, long offset, long expected, long value) {
			return handle.compareAndSet(segment, offset, expected, value);
		}

		public long getAndAdd(MemorySegment segment, long offset, long delta) {
			return (long) handle.getAndAdd(segment, offset, delta);
		}

		public long getAndSet(MemorySegment segment, long offset, long value) {
			return (long) handle.getAndSet(segment, offset, value);
		}
	}

	/**
	 * Type-safe handle for float values in memory.
	 */
	public static class FloatHandle extends MemoryHandle<Float> {

		public FloatHandle(VarHandle handle) {
			super(handle);
		}

		public FloatHandle(MemoryLayout layout, PathElement... path) {
			super(layout, path);
		}

		public FloatHandle(MemoryLayout layout, String... path) {
			super(layout, path);
		}

		@Override
		public Float get(MemorySegment segment, long offset) {
			return (float) handle.get(segment, offset);
		}

		@Override
		public void set(MemorySegment segment, long offset, Float value) {
			handle.set(segment, offset, value.floatValue());
		}

		@Override
		public Float getAtIndex(MemorySegment segment, long baseOffset, long index) {
			return getFloat(segment, baseOffset + (index * 4));
		}

		@Override
		public void setAtIndex(MemorySegment segment, long baseOffset, long index, Float value) {
			setFloat(segment, baseOffset + (index * 4), value.floatValue());
		}

		public float getFloat(MemorySegment segment, long offset) {
			return (float) handle.get(segment, offset);
		}

		public void setFloat(MemorySegment segment, long offset, float value) {
			handle.set(segment, offset, value);
		}

		public float getFloat(Memory memory) {
			return getFloat(memory.segment(), memory.start());
		}

		public float getFloat(Memory memory, long offset) {
			return getFloat(memory.segment(), memory.start() + offset);
		}

		public float getFloat(MemoryView view) {
			return getFloat(view.segment, view.start);
		}

		public float getFloat(MemoryView view, long offset) {
			return getFloat(view.segment, view.start + offset);
		}

		public void setFloat(Memory memory, float value) {
			setFloat(memory.segment(), memory.start(), value);
		}

		public void setFloat(Memory memory, long offset, float value) {
			setFloat(memory.segment(), memory.start() + offset, value);
		}

		public void setFloat(MemoryView view, float value) {
			setFloat(view.segment, view.start, value);
		}

		public void setFloat(MemoryView view, long offset, float value) {
			setFloat(view.segment, view.start + offset, value);
		}

		public float getFloatAtIndex(MemorySegment segment, long baseOffset, long index) {
			return getFloat(segment, baseOffset + (index * 4));
		}

		public float getFloatAtIndex(Memory memory, long index) {
			return getFloat(memory.segment(), memory.start() + (index * 4));
		}

		public float getFloatAtIndex(MemoryView view, long index) {
			return getFloat(view.segment, view.start + (index * 4));
		}

		public void setFloatAtIndex(MemorySegment segment, long baseOffset, long index, float value) {
			setFloat(segment, baseOffset + (index * 4), value);
		}

		public void setFloatAtIndex(Memory memory, long index, float value) {
			setFloat(memory.segment(), memory.start() + (index * 4), value);
		}

		public void setFloatAtIndex(MemoryView view, long index, float value) {
			setFloat(view.segment, view.start + (index * 4), value);
		}
	}

	/**
	 * Type-safe handle for double values in memory.
	 */
	public static class DoubleHandle extends MemoryHandle<Double> {

		public DoubleHandle(VarHandle handle) {
			super(handle);
		}

		public DoubleHandle(MemoryLayout layout, PathElement... path) {
			super(layout, path);
		}

		public DoubleHandle(MemoryLayout layout, String... path) {
			super(layout, path);
		}

		@Override
		public Double get(MemorySegment segment, long offset) {
			return (double) handle.get(segment, offset);
		}

		@Override
		public void set(MemorySegment segment, long offset, Double value) {
			handle.set(segment, offset, value.doubleValue());
		}

		@Override
		public Double getAtIndex(MemorySegment segment, long baseOffset, long index) {
			return getDouble(segment, baseOffset + (index * 8));
		}

		@Override
		public void setAtIndex(MemorySegment segment, long baseOffset, long index, Double value) {
			setDouble(segment, baseOffset + (index * 8), value.doubleValue());
		}

		public double getDouble(MemorySegment segment, long offset) {
			return (double) handle.get(segment, offset);
		}

		public void setDouble(MemorySegment segment, long offset, double value) {
			handle.set(segment, offset, value);
		}

		public double getDouble(Memory memory) {
			return getDouble(memory.segment(), memory.start());
		}

		public double getDouble(Memory memory, long offset) {
			return getDouble(memory.segment(), memory.start() + offset);
		}

		public double getDouble(MemoryView view) {
			return getDouble(view.segment, view.start);
		}

		public double getDouble(MemoryView view, long offset) {
			return getDouble(view.segment, view.start + offset);
		}

		public void setDouble(Memory memory, double value) {
			setDouble(memory.segment(), memory.start(), value);
		}

		public void setDouble(Memory memory, long offset, double value) {
			setDouble(memory.segment(), memory.start() + offset, value);
		}

		public void setDouble(MemoryView view, double value) {
			setDouble(view.segment, view.start, value);
		}

		public void setDouble(MemoryView view, long offset, double value) {
			setDouble(view.segment, view.start + offset, value);
		}

		public double getDoubleAtIndex(MemorySegment segment, long baseOffset, long index) {
			return getDouble(segment, baseOffset + (index * 8));
		}

		public double getDoubleAtIndex(Memory memory, long index) {
			return getDouble(memory.segment(), memory.start() + (index * 8));
		}

		public double getDoubleAtIndex(MemoryView view, long index) {
			return getDouble(view.segment, view.start + (index * 8));
		}

		public void setDoubleAtIndex(MemorySegment segment, long baseOffset, long index, double value) {
			setDouble(segment, baseOffset + (index * 8), value);
		}

		public void setDoubleAtIndex(Memory memory, long index, double value) {
			setDouble(memory.segment(), memory.start() + (index * 8), value);
		}

		public void setDoubleAtIndex(MemoryView view, long index, double value) {
			setDouble(view.segment, view.start + (index * 8), value);
		}
	}

	/**
	 * Type-safe handle for char values in memory.
	 */
	public static class CharHandle extends MemoryHandle<Character> {

		public CharHandle(VarHandle handle) {
			super(handle);
		}

		public CharHandle(MemoryLayout layout, PathElement... path) {
			super(layout, path);
		}

		public CharHandle(MemoryLayout layout, String... path) {
			super(layout, path);
		}

		@Override
		public Character get(MemorySegment segment, long offset) {
			return (char) handle.get(segment, offset);
		}

		@Override
		public void set(MemorySegment segment, long offset, Character value) {
			handle.set(segment, offset, value.charValue());
		}

		@Override
		public Character getAtIndex(MemorySegment segment, long baseOffset, long index) {
			return getChar(segment, baseOffset + (index * 2));
		}

		@Override
		public void setAtIndex(MemorySegment segment, long baseOffset, long index, Character value) {
			setChar(segment, baseOffset + (index * 2), value.charValue());
		}

		public char getChar(MemorySegment segment, long offset) {
			return (char) handle.get(segment, offset);
		}

		public void setChar(MemorySegment segment, long offset, char value) {
			handle.set(segment, offset, value);
		}

		public char getChar(Memory memory) {
			return getChar(memory.segment(), memory.start());
		}

		public char getChar(Memory memory, long offset) {
			return getChar(memory.segment(), memory.start() + offset);
		}

		public char getChar(MemoryView view) {
			return getChar(view.segment, view.start);
		}

		public char getChar(MemoryView view, long offset) {
			return getChar(view.segment, view.start + offset);
		}

		public void setChar(Memory memory, char value) {
			setChar(memory.segment(), memory.start(), value);
		}

		public void setChar(Memory memory, long offset, char value) {
			setChar(memory.segment(), memory.start() + offset, value);
		}

		public void setChar(MemoryView view, char value) {
			setChar(view.segment, view.start, value);
		}

		public void setChar(MemoryView view, long offset, char value) {
			setChar(view.segment, view.start + offset, value);
		}

		public char getCharAtIndex(MemorySegment segment, long baseOffset, long index) {
			return getChar(segment, baseOffset + (index * 2));
		}

		public char getCharAtIndex(Memory memory, long index) {
			return getChar(memory.segment(), memory.start() + (index * 2));
		}

		public char getCharAtIndex(MemoryView view, long index) {
			return getChar(view.segment, view.start + (index * 2));
		}

		public void setCharAtIndex(MemorySegment segment, long baseOffset, long index, char value) {
			setChar(segment, baseOffset + (index * 2), value);
		}

		public void setCharAtIndex(Memory memory, long index, char value) {
			setChar(memory.segment(), memory.start() + (index * 2), value);
		}

		public void setCharAtIndex(MemoryView view, long index, char value) {
			setChar(view.segment, view.start + (index * 2), value);
		}
	}

	/**
	 * Type-safe handle for boolean values in memory.
	 */
	public static class BooleanHandle extends MemoryHandle<Boolean> {

		public BooleanHandle(VarHandle handle) {
			super(handle);
		}

		public BooleanHandle(MemoryLayout layout, PathElement... path) {
			super(layout, path);
		}

		public BooleanHandle(MemoryLayout layout, String... path) {
			super(layout, path);
		}

		@Override
		public Boolean get(MemorySegment segment, long offset) {
			return (boolean) handle.get(segment, offset);
		}

		@Override
		public void set(MemorySegment segment, long offset, Boolean value) {
			handle.set(segment, offset, value.booleanValue());
		}

		@Override
		public Boolean getAtIndex(MemorySegment segment, long baseOffset, long index) {
			return getBoolean(segment, baseOffset + index);
		}

		@Override
		public void setAtIndex(MemorySegment segment, long baseOffset, long index, Boolean value) {
			setBoolean(segment, baseOffset + index, value.booleanValue());
		}

		public boolean getBoolean(MemorySegment segment, long offset) {
			return (boolean) handle.get(segment, offset);
		}

		public void setBoolean(MemorySegment segment, long offset, boolean value) {
			handle.set(segment, offset, value);
		}

		public boolean getBoolean(Memory memory) {
			return getBoolean(memory.segment(), memory.start());
		}

		public boolean getBoolean(Memory memory, long offset) {
			return getBoolean(memory.segment(), memory.start() + offset);
		}

		public boolean getBoolean(MemoryView view) {
			return getBoolean(view.segment, view.start);
		}

		public boolean getBoolean(MemoryView view, long offset) {
			return getBoolean(view.segment, view.start + offset);
		}

		public void setBoolean(Memory memory, boolean value) {
			setBoolean(memory.segment(), memory.start(), value);
		}

		public void setBoolean(Memory memory, long offset, boolean value) {
			setBoolean(memory.segment(), memory.start() + offset, value);
		}

		public void setBoolean(MemoryView view, boolean value) {
			setBoolean(view.segment, view.start, value);
		}

		public void setBoolean(MemoryView view, long offset, boolean value) {
			setBoolean(view.segment, view.start + offset, value);
		}

		public boolean getBooleanAtIndex(MemorySegment segment, long baseOffset, long index) {
			return getBoolean(segment, baseOffset + index);
		}

		public boolean getBooleanAtIndex(Memory memory, long index) {
			return getBoolean(memory.segment(), memory.start() + index);
		}

		public boolean getBooleanAtIndex(MemoryView view, long index) {
			return getBoolean(view.segment, view.start + index);
		}

		public void setBooleanAtIndex(MemorySegment segment, long baseOffset, long index, boolean value) {
			setBoolean(segment, baseOffset + index, value);
		}

		public void setBooleanAtIndex(Memory memory, long index, boolean value) {
			setBoolean(memory.segment(), memory.start() + index, value);
		}

		public void setBooleanAtIndex(MemoryView view, long index, boolean value) {
			setBoolean(view.segment, view.start + index, value);
		}
	}
}
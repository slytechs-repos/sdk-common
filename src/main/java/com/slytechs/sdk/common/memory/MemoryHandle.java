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
package com.slytechs.sdk.common.memory;

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
 * Memory packet = freeListPool.allocate();
 * int src = srcAddr.getInt(packet);
 * 
 * // Access through MemoryView  
 * MemoryView view = packet.view();
 * short len = length.getShort(view);
 * 
 * // Array access
 * LongHandle timestamps = new LongHandle(LAYOUT, "timestamps[]");
 * long firstTimestamp = timestamps.getLongAtIndex(packet, 0);
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

	public static class BooleanHandle extends MemoryHandle<Boolean> {

		public BooleanHandle(MemoryLayout layout, PathElement... path) {
			super(layout, path);
		}

		public BooleanHandle(MemoryLayout layout, String... path) {
			super(layout, path);
		}

		public BooleanHandle(VarHandle handle) {
			super(handle);
		}

		@Override
		public Boolean get(MemorySegment segment, long offset) {
			return (boolean) handle.get(segment, offset);
		}

		@Override
		public Boolean getAtIndex(MemorySegment segment, long baseOffset, long index) {
			return (boolean) handle.get(segment, baseOffset, index);
		}

		public boolean getBoolean(Memory memory) {
			return getBoolean(memory.segment(), memory.start());
		}

		public boolean getBoolean(Memory memory, long offset) {
			return getBoolean(memory.segment(), memory.start() + offset);
		}

		// Primitive accessors
		public boolean getBoolean(MemorySegment segment, long offset) {
			return (boolean) handle.get(segment, offset);
		}

		public boolean getBoolean(MemoryView view) {
			return getBoolean(view.segment, view.start);
		}

		public boolean getBoolean(MemoryView view, long offset) {
			return getBoolean(view.segment, view.start + offset);
		}

		public boolean getBooleanAtIndex(Memory memory, long index) {
			return (boolean) handle.get(memory.segment(), memory.start(), index);
		}

		// Array accessors - use VarHandle (segment, baseOffset, index) signature
		public boolean getBooleanAtIndex(MemorySegment segment, long baseOffset, long index) {
			return (boolean) handle.get(segment, baseOffset, index);
		}

		public boolean getBooleanAtIndex(MemoryView view, long index) {
			return (boolean) handle.get(view.segment, view.start, index);
		}

		@Override
		public void set(MemorySegment segment, long offset, Boolean value) {
			handle.set(segment, offset, value.booleanValue());
		}

		@Override
		public void setAtIndex(MemorySegment segment, long baseOffset, long index, Boolean value) {
			handle.set(segment, baseOffset, index, value.booleanValue());
		}

		public void setBoolean(Memory memory, boolean value) {
			setBoolean(memory.segment(), memory.start(), value);
		}

		public void setBoolean(Memory memory, long offset, boolean value) {
			setBoolean(memory.segment(), memory.start() + offset, value);
		}

		public void setBoolean(MemorySegment segment, long offset, boolean value) {
			handle.set(segment, offset, value);
		}

		public void setBoolean(MemoryView view, boolean value) {
			setBoolean(view.segment, view.start, value);
		}

		public void setBoolean(MemoryView view, long offset, boolean value) {
			setBoolean(view.segment, view.start + offset, value);
		}

		public void setBooleanAtIndex(Memory memory, long index, boolean value) {
			handle.set(memory.segment(), memory.start(), index, value);
		}

		public void setBooleanAtIndex(MemorySegment segment, long baseOffset, long index, boolean value) {
			handle.set(segment, baseOffset, index, value);
		}

		public void setBooleanAtIndex(MemoryView view, long index, boolean value) {
			handle.set(view.segment, view.start, index, value);
		}
	}

	public static class ByteHandle extends MemoryHandle<Byte> {

		public ByteHandle(MemoryLayout layout, PathElement... path) {
			super(layout, path);
		}

		public ByteHandle(MemoryLayout layout, String... path) {
			super(layout, path);
		}

		public ByteHandle(VarHandle handle) {
			super(handle);
		}

		@Override
		public Byte get(MemorySegment segment, long offset) {
			return (byte) handle.get(segment, offset);
		}

		@Override
		public Byte getAtIndex(MemorySegment segment, long baseOffset, long index) {
			return (byte) handle.get(segment, baseOffset, index);
		}

		public byte getByte(Memory memory) {
			return getByte(memory.segment(), memory.start());
		}

		public byte getByte(Memory memory, long offset) {
			return getByte(memory.segment(), memory.start() + offset);
		}

		// Primitive accessors
		public byte getByte(MemorySegment segment, long offset) {
			return (byte) handle.get(segment, offset);
		}

		public byte getByte(MemoryView view) {
			return getByte(view.segment, view.start);
		}

		public byte getByte(MemoryView view, long offset) {
			return getByte(view.segment, view.start + offset);
		}

		public byte[] getByteArray(Memory memory, int length) {
			return getByteArray(memory.segment(), memory.start(), length);
		}

		public byte[] getByteArray(Memory memory, long offset, int length) {
			return getByteArray(memory.segment(), memory.start() + offset, length);
		}

		// Byte array methods (these use offset arithmetic, not VarHandle array access)
		public byte[] getByteArray(MemorySegment segment, long offset, int length) {
			byte[] array = new byte[length];
			for (int i = 0; i < length; i++) {
				array[i] = getByte(segment, offset + i);
			}
			return array;
		}

		public byte[] getByteArray(MemoryView view, int length) {
			return getByteArray(view.segment, view.start, length);
		}

		public byte[] getByteArray(MemoryView view, long offset, int length) {
			return getByteArray(view.segment, view.start + offset, length);
		}

		public byte getByteAtIndex(Memory memory, long index) {
			return (byte) handle.get(memory.segment(), memory.start(), index);
		}

		// Array accessors - use VarHandle (segment, baseOffset, index) signature
		public byte getByteAtIndex(MemorySegment segment, long baseOffset, long index) {
			return (byte) handle.get(segment, baseOffset, index);
		}

		public byte getByteAtIndex(MemoryView view, long index) {
			return (byte) handle.get(view.segment, view.start, index);
		}

		@Override
		public void set(MemorySegment segment, long offset, Byte value) {
			handle.set(segment, offset, value.byteValue());
		}

		@Override
		public void setAtIndex(MemorySegment segment, long baseOffset, long index, Byte value) {
			handle.set(segment, baseOffset, index, value.byteValue());
		}

		public void setByte(Memory memory, byte value) {
			setByte(memory.segment(), memory.start(), value);
		}

		public void setByte(Memory memory, long offset, byte value) {
			setByte(memory.segment(), memory.start() + offset, value);
		}

		public void setByte(MemorySegment segment, long offset, byte value) {
			handle.set(segment, offset, value);
		}

		public void setByte(MemoryView view, byte value) {
			setByte(view.segment, view.start, value);
		}

		public void setByte(MemoryView view, long offset, byte value) {
			setByte(view.segment, view.start + offset, value);
		}

		public void setByteArray(Memory memory, byte[] array) {
			setByteArray(memory.segment(), memory.start(), array);
		}

		public void setByteArray(Memory memory, long offset, byte[] array) {
			setByteArray(memory.segment(), memory.start() + offset, array);
		}

		public void setByteArray(MemorySegment segment, long offset, byte[] array) {
			for (int i = 0; i < array.length; i++) {
				setByte(segment, offset + i, array[i]);
			}
		}

		public void setByteArray(MemorySegment segment, long offset, byte[] array, int arrayOffset, int length) {
			for (int i = 0; i < length; i++) {
				setByte(segment, offset + i, array[arrayOffset + i]);
			}
		}

		public void setByteArray(MemoryView view, byte[] array) {
			setByteArray(view.segment, view.start, array);
		}

		public void setByteArray(MemoryView view, long offset, byte[] array) {
			setByteArray(view.segment, view.start + offset, array);
		}

		public void setByteAtIndex(Memory memory, long index, byte value) {
			handle.set(memory.segment(), memory.start(), index, value);
		}

		public void setByteAtIndex(MemorySegment segment, long baseOffset, long index, byte value) {
			handle.set(segment, baseOffset, index, value);
		}

		public void setByteAtIndex(MemoryView view, long index, byte value) {
			handle.set(view.segment, view.start, index, value);
		}
	}

	public static class CharHandle extends MemoryHandle<Character> {

		public CharHandle(MemoryLayout layout, PathElement... path) {
			super(layout, path);
		}

		public CharHandle(MemoryLayout layout, String... path) {
			super(layout, path);
		}

		public CharHandle(VarHandle handle) {
			super(handle);
		}

		@Override
		public Character get(MemorySegment segment, long offset) {
			return (char) handle.get(segment, offset);
		}

		@Override
		public Character getAtIndex(MemorySegment segment, long baseOffset, long index) {
			return (char) handle.get(segment, baseOffset, index);
		}

		public char getChar(Memory memory) {
			return getChar(memory.segment(), memory.start());
		}

		public char getChar(Memory memory, long offset) {
			return getChar(memory.segment(), memory.start() + offset);
		}

		// Primitive accessors
		public char getChar(MemorySegment segment, long offset) {
			return (char) handle.get(segment, offset);
		}

		public char getChar(MemoryView view) {
			return getChar(view.segment, view.start);
		}

		public char getChar(MemoryView view, long offset) {
			return getChar(view.segment, view.start + offset);
		}

		public char getCharAtIndex(Memory memory, long index) {
			return (char) handle.get(memory.segment(), memory.start(), index);
		}

		// Array accessors - use VarHandle (segment, baseOffset, index) signature
		public char getCharAtIndex(MemorySegment segment, long baseOffset, long index) {
			return (char) handle.get(segment, baseOffset, index);
		}

		public char getCharAtIndex(MemoryView view, long index) {
			return (char) handle.get(view.segment, view.start, index);
		}

		@Override
		public void set(MemorySegment segment, long offset, Character value) {
			handle.set(segment, offset, value.charValue());
		}

		@Override
		public void setAtIndex(MemorySegment segment, long baseOffset, long index, Character value) {
			handle.set(segment, baseOffset, index, value.charValue());
		}

		public void setChar(Memory memory, char value) {
			setChar(memory.segment(), memory.start(), value);
		}

		public void setChar(Memory memory, long offset, char value) {
			setChar(memory.segment(), memory.start() + offset, value);
		}

		public void setChar(MemorySegment segment, long offset, char value) {
			handle.set(segment, offset, value);
		}

		public void setChar(MemoryView view, char value) {
			setChar(view.segment, view.start, value);
		}

		public void setChar(MemoryView view, long offset, char value) {
			setChar(view.segment, view.start + offset, value);
		}

		public void setCharAtIndex(Memory memory, long index, char value) {
			handle.set(memory.segment(), memory.start(), index, value);
		}

		public void setCharAtIndex(MemorySegment segment, long baseOffset, long index, char value) {
			handle.set(segment, baseOffset, index, value);
		}

		public void setCharAtIndex(MemoryView view, long index, char value) {
			handle.set(view.segment, view.start, index, value);
		}
	}

	public static class DoubleHandle extends MemoryHandle<Double> {

		public DoubleHandle(MemoryLayout layout, PathElement... path) {
			super(layout, path);
		}

		public DoubleHandle(MemoryLayout layout, String... path) {
			super(layout, path);
		}

		public DoubleHandle(VarHandle handle) {
			super(handle);
		}

		@Override
		public Double get(MemorySegment segment, long offset) {
			return (double) handle.get(segment, offset);
		}

		@Override
		public Double getAtIndex(MemorySegment segment, long baseOffset, long index) {
			return (double) handle.get(segment, baseOffset, index);
		}

		public double getDouble(Memory memory) {
			return getDouble(memory.segment(), memory.start());
		}

		public double getDouble(Memory memory, long offset) {
			return getDouble(memory.segment(), memory.start() + offset);
		}

		// Primitive accessors
		public double getDouble(MemorySegment segment, long offset) {
			return (double) handle.get(segment, offset);
		}

		public double getDouble(MemoryView view) {
			return getDouble(view.segment, view.start);
		}

		public double getDouble(MemoryView view, long offset) {
			return getDouble(view.segment, view.start + offset);
		}

		public double getDoubleAtIndex(Memory memory, long index) {
			return (double) handle.get(memory.segment(), memory.start(), index);
		}

		// Array accessors - use VarHandle (segment, baseOffset, index) signature
		public double getDoubleAtIndex(MemorySegment segment, long baseOffset, long index) {
			return (double) handle.get(segment, baseOffset, index);
		}

		public double getDoubleAtIndex(MemoryView view, long index) {
			return (double) handle.get(view.segment, view.start, index);
		}

		@Override
		public void set(MemorySegment segment, long offset, Double value) {
			handle.set(segment, offset, value.doubleValue());
		}

		@Override
		public void setAtIndex(MemorySegment segment, long baseOffset, long index, Double value) {
			handle.set(segment, baseOffset, index, value.doubleValue());
		}

		public void setDouble(Memory memory, double value) {
			setDouble(memory.segment(), memory.start(), value);
		}

		public void setDouble(Memory memory, long offset, double value) {
			setDouble(memory.segment(), memory.start() + offset, value);
		}

		public void setDouble(MemorySegment segment, long offset, double value) {
			handle.set(segment, offset, value);
		}

		public void setDouble(MemoryView view, double value) {
			setDouble(view.segment, view.start, value);
		}

		public void setDouble(MemoryView view, long offset, double value) {
			setDouble(view.segment, view.start + offset, value);
		}

		public void setDoubleAtIndex(Memory memory, long index, double value) {
			handle.set(memory.segment(), memory.start(), index, value);
		}

		public void setDoubleAtIndex(MemorySegment segment, long baseOffset, long index, double value) {
			handle.set(segment, baseOffset, index, value);
		}

		public void setDoubleAtIndex(MemoryView view, long index, double value) {
			handle.set(view.segment, view.start, index, value);
		}
	}

	public static class FloatHandle extends MemoryHandle<Float> {

		public FloatHandle(MemoryLayout layout, PathElement... path) {
			super(layout, path);
		}

		public FloatHandle(MemoryLayout layout, String... path) {
			super(layout, path);
		}

		public FloatHandle(VarHandle handle) {
			super(handle);
		}

		@Override
		public Float get(MemorySegment segment, long offset) {
			return (float) handle.get(segment, offset);
		}

		@Override
		public Float getAtIndex(MemorySegment segment, long baseOffset, long index) {
			return (float) handle.get(segment, baseOffset, index);
		}

		public float getFloat(Memory memory) {
			return getFloat(memory.segment(), memory.start());
		}

		public float getFloat(Memory memory, long offset) {
			return getFloat(memory.segment(), memory.start() + offset);
		}

		// Primitive accessors
		public float getFloat(MemorySegment segment, long offset) {
			return (float) handle.get(segment, offset);
		}

		public float getFloat(MemoryView view) {
			return getFloat(view.segment, view.start);
		}

		public float getFloat(MemoryView view, long offset) {
			return getFloat(view.segment, view.start + offset);
		}

		public float getFloatAtIndex(Memory memory, long index) {
			return (float) handle.get(memory.segment(), memory.start(), index);
		}

		// Array accessors - use VarHandle (segment, baseOffset, index) signature
		public float getFloatAtIndex(MemorySegment segment, long baseOffset, long index) {
			return (float) handle.get(segment, baseOffset, index);
		}

		public float getFloatAtIndex(MemoryView view, long index) {
			return (float) handle.get(view.segment, view.start, index);
		}

		@Override
		public void set(MemorySegment segment, long offset, Float value) {
			handle.set(segment, offset, value.floatValue());
		}

		@Override
		public void setAtIndex(MemorySegment segment, long baseOffset, long index, Float value) {
			handle.set(segment, baseOffset, index, value.floatValue());
		}

		public void setFloat(Memory memory, float value) {
			setFloat(memory.segment(), memory.start(), value);
		}

		public void setFloat(Memory memory, long offset, float value) {
			setFloat(memory.segment(), memory.start() + offset, value);
		}

		public void setFloat(MemorySegment segment, long offset, float value) {
			handle.set(segment, offset, value);
		}

		public void setFloat(MemoryView view, float value) {
			setFloat(view.segment, view.start, value);
		}

		public void setFloat(MemoryView view, long offset, float value) {
			setFloat(view.segment, view.start + offset, value);
		}

		public void setFloatAtIndex(Memory memory, long index, float value) {
			handle.set(memory.segment(), memory.start(), index, value);
		}

		public void setFloatAtIndex(MemorySegment segment, long baseOffset, long index, float value) {
			handle.set(segment, baseOffset, index, value);
		}

		public void setFloatAtIndex(MemoryView view, long index, float value) {
			handle.set(view.segment, view.start, index, value);
		}
	}

	public static class IntHandle extends MemoryHandle<Integer> {

		public IntHandle(MemoryLayout layout, PathElement... path) {
			super(layout, path);
		}

		public IntHandle(MemoryLayout layout, String... path) {
			super(layout, path);
		}

		public IntHandle(VarHandle handle) {
			super(handle);
		}

		// Atomic operations
		public boolean compareAndSet(MemorySegment segment, long offset, int expected, int value) {
			return handle.compareAndSet(segment, offset, expected, value);
		}

		@Override
		public Integer get(MemorySegment segment, long offset) {
			return (int) handle.get(segment, offset);
		}

		public int getAndAdd(MemorySegment segment, long offset, int delta) {
			return (int) handle.getAndAdd(segment, offset, delta);
		}

		public int getAndSet(MemorySegment segment, long offset, int value) {
			return (int) handle.getAndSet(segment, offset, value);
		}

		@Override
		public Integer getAtIndex(MemorySegment segment, long baseOffset, long index) {
			return (int) handle.get(segment, baseOffset, index);
		}

		public int getInt(Memory memory) {
			return getInt(memory.segment(), memory.start());
		}

		public int getInt(Memory memory, long offset) {
			return getInt(memory.segment(), memory.start() + offset);
		}

		// Primitive accessors
		public int getInt(MemorySegment segment, long offset) {
			return (int) handle.get(segment, offset);
		}

		public int getInt(MemoryView view) {
			return getInt(view.segment, view.start);
		}

		public int getInt(MemoryView view, long offset) {
			return getInt(view.segment, view.start + offset);
		}

		public int getIntAtIndex(Memory memory, long index) {
			return (int) handle.get(memory.segment(), memory.start(), index);
		}

		// Array accessors - use VarHandle (segment, baseOffset, index) signature
		public int getIntAtIndex(MemorySegment segment, long baseOffset, long index) {
			return (int) handle.get(segment, baseOffset, index);
		}

		public int getIntAtIndex(MemoryView view, long index) {
			return (int) handle.get(view.segment, view.start, index);
		}

		@Override
		public void set(MemorySegment segment, long offset, Integer value) {
			handle.set(segment, offset, value.intValue());
		}

		@Override
		public void setAtIndex(MemorySegment segment, long baseOffset, long index, Integer value) {
			handle.set(segment, baseOffset, index, value.intValue());
		}

		public void setInt(Memory memory, int value) {
			setInt(memory.segment(), memory.start(), value);
		}

		public void setInt(Memory memory, long offset, int value) {
			setInt(memory.segment(), memory.start() + offset, value);
		}

		public void setInt(MemorySegment segment, long offset, int value) {
			handle.set(segment, offset, value);
		}

		public void setInt(MemoryView view, int value) {
			setInt(view.segment, view.start, value);
		}

		public void setInt(MemoryView view, long offset, int value) {
			setInt(view.segment, view.start + offset, value);
		}

		public void setIntAtIndex(Memory memory, long index, int value) {
			handle.set(memory.segment(), memory.start(), index, value);
		}

		public void setIntAtIndex(MemorySegment segment, long baseOffset, long index, int value) {
			handle.set(segment, baseOffset, index, value);
		}

		public void setIntAtIndex(MemoryView view, long index, int value) {
			handle.set(view.segment, view.start, index, value);
		}
	}

	public static class LongHandle extends MemoryHandle<Long> {

		public LongHandle(MemoryLayout layout, PathElement... path) {
			super(layout, path);
		}

		public LongHandle(MemoryLayout layout, String... path) {
			super(layout, path);
		}

		public LongHandle(VarHandle handle) {
			super(handle);
		}

		// Atomic operations
		public boolean compareAndSet(MemorySegment segment, long offset, long expected, long value) {
			return handle.compareAndSet(segment, offset, expected, value);
		}

		@Override
		public Long get(MemorySegment segment, long offset) {
			return (long) handle.get(segment, offset);
		}

		public long getAndAdd(MemorySegment segment, long offset, long delta) {
			return (long) handle.getAndAdd(segment, offset, delta);
		}

		public long getAndSet(MemorySegment segment, long offset, long value) {
			return (long) handle.getAndSet(segment, offset, value);
		}

		@Override
		public Long getAtIndex(MemorySegment segment, long baseOffset, long index) {
			return (long) handle.get(segment, baseOffset, index);
		}

		public long getLong(Memory memory) {
			return getLong(memory.segment(), memory.start());
		}

		public long getLong(Memory memory, long offset) {
			return getLong(memory.segment(), memory.start() + offset);
		}

		// Primitive accessors
		public long getLong(MemorySegment segment, long offset) {
			return (long) handle.get(segment, offset);
		}

		public long getLong(MemoryView view) {
			return getLong(view.segment, view.start);
		}

		public long getLong(MemoryView view, long offset) {
			return getLong(view.segment, view.start + offset);
		}

		public long getLongAtIndex(Memory memory, long index) {
			return (long) handle.get(memory.segment(), memory.start(), index);
		}

		// Array accessors - use VarHandle (segment, baseOffset, index) signature
		public long getLongAtIndex(MemorySegment segment, long baseOffset, long index) {
			return (long) handle.get(segment, baseOffset, index);
		}

		public long getLongAtIndex(MemoryView view, long index) {
			return (long) handle.get(view.segment, view.start, index);
		}

		@Override
		public void set(MemorySegment segment, long offset, Long value) {
			handle.set(segment, offset, value.longValue());
		}

		@Override
		public void setAtIndex(MemorySegment segment, long baseOffset, long index, Long value) {
			handle.set(segment, baseOffset, index, value.longValue());
		}

		public void setLong(Memory memory, long value) {
			setLong(memory.segment(), memory.start(), value);
		}

		public void setLong(Memory memory, long offset, long value) {
			setLong(memory.segment(), memory.start() + offset, value);
		}

		public void setLong(MemorySegment segment, long offset, long value) {
			handle.set(segment, offset, value);
		}

		public void setLong(MemoryView view, long value) {
			setLong(view.segment, view.start, value);
		}

		public void setLong(MemoryView view, long offset, long value) {
			setLong(view.segment, view.start + offset, value);
		}

		public void setLongAtIndex(Memory memory, long index, long value) {
			handle.set(memory.segment(), memory.start(), index, value);
		}

		public void setLongAtIndex(MemorySegment segment, long baseOffset, long index, long value) {
			handle.set(segment, baseOffset, index, value);
		}

		public void setLongAtIndex(MemoryView view, long index, long value) {
			handle.set(view.segment, view.start, index, value);
		}
	}

	public static class ShortHandle extends MemoryHandle<Short> {

		public ShortHandle(MemoryLayout layout, PathElement... path) {
			super(layout, path);
		}

		public ShortHandle(MemoryLayout layout, String... path) {
			super(layout, path);
		}

		public ShortHandle(VarHandle handle) {
			super(handle);
		}

		@Override
		public Short get(MemorySegment segment, long offset) {
			return (short) handle.get(segment, offset);
		}

		@Override
		public Short getAtIndex(MemorySegment segment, long baseOffset, long index) {
			return (short) handle.get(segment, baseOffset, index);
		}

		public short getShort(Memory memory) {
			return getShort(memory.segment(), memory.start());
		}

		public short getShort(Memory memory, long offset) {
			return getShort(memory.segment(), memory.start() + offset);
		}

		// Primitive accessors
		public short getShort(MemorySegment segment, long offset) {
			return (short) handle.get(segment, offset);
		}

		public short getShort(MemoryView view) {
			return getShort(view.segment, view.start);
		}

		public int getUnsignedShort(MemoryView view) {
			return Short.toUnsignedInt(getShort(view.segment, view.start));
		}

		public short getShort(MemoryView view, long offset) {
			return getShort(view.segment, view.start + offset);
		}

		public short getShortAtIndex(Memory memory, long index) {
			return (short) handle.get(memory.segment(), memory.start(), index);
		}

		// Array accessors - use VarHandle (segment, baseOffset, index) signature
		public short getShortAtIndex(MemorySegment segment, long baseOffset, long index) {
			return (short) handle.get(segment, baseOffset, index);
		}

		public short getShortAtIndex(MemoryView view, long index) {
			return (short) handle.get(view.segment, view.start, index);
		}

		@Override
		public void set(MemorySegment segment, long offset, Short value) {
			handle.set(segment, offset, value.shortValue());
		}

		@Override
		public void setAtIndex(MemorySegment segment, long baseOffset, long index, Short value) {
			handle.set(segment, baseOffset, index, value.shortValue());
		}

		public void setShort(Memory memory, long offset, short value) {
			setShort(memory.segment(), memory.start() + offset, value);
		}

		public void setShort(Memory memory, short value) {
			setShort(memory.segment(), memory.start(), value);
		}

		public void setShort(MemorySegment segment, long offset, short value) {
			handle.set(segment, offset, value);
		}

		public void setShort(MemoryView view, long offset, short value) {
			setShort(view.segment, view.start + offset, value);
		}

		public void setShort(MemoryView view, short value) {
			setShort(view.segment, view.start, value);
		}

		public void setShortAtIndex(Memory memory, long index, short value) {
			handle.set(memory.segment(), memory.start(), index, value);
		}

		public void setShortAtIndex(MemorySegment segment, long baseOffset, long index, short value) {
			handle.set(segment, baseOffset, index, value);
		}

		public void setShortAtIndex(MemoryView view, long index, short value) {
			handle.set(view.segment, view.start, index, value);
		}
	}

	public static long byteOffset(MemoryLayout layout, PathElement... path) {
		return layout.byteOffset(path);
	}

	public static long byteOffset(MemoryLayout layout, String... path) {
		return layout.byteOffset(parsePath(path));
	}

	public static PathElement[] parsePath(String... path) {
		List<PathElement> elements = new ArrayList<>();

		for (String element : path) {
			if (element == null || element.isEmpty()) {
				throw new IllegalArgumentException("Path element cannot be null or empty");
			}

			if (element.endsWith("[]")) {
				// Array access without index: "arrayName[]"
				String groupElement = element.split("\\[")[0];
				elements.add(PathElement.groupElement(groupElement));
				elements.add(PathElement.sequenceElement());
			} else if (element.contains("[") && element.endsWith("]")) {
				// Array access with index: "arrayName[3]"
				int bracketIdx = element.indexOf('[');
				String groupName = element.substring(0, bracketIdx);
				String indexStr = element.substring(bracketIdx + 1, element.length() - 1);

				if (!groupName.isEmpty()) {
					elements.add(PathElement.groupElement(groupName));
				}

				if (indexStr.isEmpty()) {
					elements.add(PathElement.sequenceElement());
				} else {
					try {
						long index = Long.parseLong(indexStr);
						elements.add(PathElement.sequenceElement(index));
					} catch (NumberFormatException e) {
						throw new IllegalArgumentException(
								"Invalid array index in path element: " + element, e);
					}
				}
			} else if (element.contains("[") || element.contains("]")) {
				throw new IllegalArgumentException(
						"Malformed array syntax in path element: " + element);
			} else {
				elements.add(PathElement.groupElement(element));
			}
		}

		return elements.toArray(new PathElement[0]);
	}

	/** The underlying VarHandle that performs actual memory access */
	protected final VarHandle handle;

	/**
	 * Constructs a MemoryHandle by navigating a MemoryLayout with PathElements.
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
		this.handle = layout.varHandle(path).withInvokeExactBehavior();
	}

	/**
	 * Constructs a MemoryHandle using string-based path navigation.
	 * 
	 * @param layout the memory layout to navigate
	 * @param path   the string path elements for navigation
	 * @throws NullPointerException     if layout is null
	 * @throws IllegalArgumentException if the path syntax is invalid
	 */
	protected MemoryHandle(MemoryLayout layout, String... path) {
		if (layout == null) {
			throw new NullPointerException("MemoryLayout cannot be null");
		}
		this.handle = layout.varHandle(parsePath(path));
	}

	/**
	 * Constructs a MemoryHandle with a pre-created VarHandle.
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

	public T get(Memory memory) {
		return get(memory.segment(), memory.start());
	}

	public T get(Memory memory, long offset) {
		return get(memory.segment(), memory.start() + offset);
	}

	// ==================== Path Parsing ====================

	public abstract T get(MemorySegment segment, long offset);

	public T get(MemoryView view) {
		return get(view.segment, view.start);
	}

	public T get(MemoryView view, long offset) {
		return get(view.segment, view.start + offset);
	}

	// ==================== BooleanHandle ====================

	public abstract T getAtIndex(MemorySegment segment, long baseOffset, long index);

	// ==================== ByteHandle ====================

	public void set(Memory memory, long offset, T value) {
		set(memory.segment(), memory.start() + offset, value);
	}

	// ==================== CharHandle ====================

	public void set(Memory memory, T value) {
		set(memory.segment(), memory.start(), value);
	}

	// ==================== ShortHandle ====================

	public abstract void set(MemorySegment segment, long offset, T value);

	// ==================== IntHandle ====================

	public void set(MemoryView view, long offset, T value) {
		set(view.segment, view.start + offset, value);
	}

	// ==================== LongHandle ====================

	public void set(MemoryView view, T value) {
		set(view.segment, view.start, value);
	}

	// ==================== FloatHandle ====================

	public abstract void setAtIndex(MemorySegment segment, long baseOffset, long index, T value);

	// ==================== DoubleHandle ====================

	public VarHandle varHandle() {
		return handle;
	}
}
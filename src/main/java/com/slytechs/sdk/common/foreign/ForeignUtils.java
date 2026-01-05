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
package com.slytechs.sdk.common.foreign;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.VarHandle;
import java.util.function.Consumer;

/**
 * The Class ForeignUtils.
 *
 * @author Mark Bednarczyk
 */
public final class ForeignUtils {

	/** The Constant EMPTY_CLEANUP. */
	public static final Consumer<MemorySegment> EMPTY_CLEANUP = new Consumer<MemorySegment>() {
		@Override
		public void accept(MemorySegment t) {

		}
	};

	/** The Constant DEFAULT_MAX_STRING_LEN. */
	private final static long DEFAULT_MAX_STRING_LEN = 64 * 1024;

	/**
	 * To java string.
	 *
	 * @param memorySegment the memory segment
	 * @return the string
	 */
	public static String toJavaString(Object memorySegment) {
		return toJavaString(((MemorySegment) memorySegment));
	}

	/**
	 * Checks if is null address.
	 *
	 * @param address the address
	 * @return true, if is null address
	 */
	public static boolean isNullAddress(MemorySegment address) {
		return (address == null) || (address.address() == 0);
	}

	/**
	 * To java string.
	 *
	 * @param addr the addr
	 * @return the string
	 */
	public static String toJavaString(MemorySegment addr) {
		if (ForeignUtils.isNullAddress(addr))
			return null;

		if (addr.byteSize() == 0)
			addr = addr.reinterpret(DEFAULT_MAX_STRING_LEN);

		String str = addr.getString(0);
		return str;
	}

	/**
	 * Returns a new memory segment with the same address and size as this segment,
	 * but with the provided scope. As such, the returned segment cannot be accessed
	 * after the provided arena has been closed. Moreover, the returned segment can
	 * be accessed compatibly with the confinement restrictions associated with the
	 * provided arena: that is, if the provided arena is a
	 * {@linkplain Arena#ofConfined() confined arena}, the returned segment can only
	 * be accessed by the arena's owner thread, regardless of the confinement
	 * restrictions associated with this segment. In other words, this method
	 * returns a segment that behaves as if it had been allocated using the provided
	 * arena.
	 * <p>
	 * Clients can specify an optional cleanup action that should be executed when
	 * the provided scope becomes invalid. This cleanup action receives a fresh
	 * memory segment that is obtained from this segment as follows:
	 * {@snippet lang = java :
	 * MemorySegment cleanupSegment = MemorySegment.ofAddress(this.address())
	 * 		.reinterpret(byteSize());
	 * }
	 * That is, the cleanup action receives a segment that is associated with the
	 * global scope, and is accessible from any thread. The size of the segment
	 * accepted by the cleanup action is {@link #byteSize()}.
	 *
	 * @apiNote The cleanup action (if present) should take care not to leak the
	 *          received segment to external clients that might access the segment
	 *          after its backing region of memory is no longer available.
	 *          Furthermore, if the provided scope is the scope of an
	 *          {@linkplain Arena#ofAuto() automatic arena}, the cleanup action must
	 *          not prevent the scope from becoming <a href=
	 *          "../../../java/lang/ref/package.html#reachability">unreachable</a>.
	 *          A failure to do so will permanently prevent the regions of memory
	 *          allocated by the automatic arena from being deallocated.
	 *
	 * @param arena   the arena to be associated with the returned segment
	 * @param cleanup the cleanup action that should be executed when the provided
	 *                arena is closed (can be {@code null})
	 * @return a new memory segment with unbounded size
	 * @throws IllegalStateException         if
	 *                                       {@code arena.scope().isAlive() == false}
	 * @throws UnsupportedOperationException if this segment is not a
	 *                                       {@linkplain #isNative() native} segment
	 * @throws IllegalCallerException        if the caller is in a module that does
	 *                                       not have native access enabled
	 */
	public static MemorySegment reinterpret(MemorySegment pointer, Arena arena, Consumer<MemorySegment> cleanup) {
		return pointer.reinterpret(arena, cleanup);
	}

	/**
	 * Returns a new memory segment that has the same address and scope as this
	 * segment, but with the provided size.
	 *
	 * @param newSize the size of the returned segment
	 * @return a new memory segment that has the same address and scope as this
	 *         segment, but the new provided size
	 * @throws IllegalArgumentException      if {@code newSize < 0}
	 * @throws UnsupportedOperationException if this segment is not a
	 *                                       {@linkplain #isNative() native} segment
	 * @throws IllegalCallerException        if the caller is in a module that does
	 *                                       not have native access enabled
	 */
	public static MemorySegment reinterpret(MemorySegment pointer, long newSize) {
		return pointer.reinterpret(newSize);

	}

	/**
	 * Returns a new segment with the same address as this segment, but with the
	 * provided size and scope. As such, the returned segment cannot be accessed
	 * after the provided arena has been closed. Moreover, if the returned segment
	 * can be accessed compatibly with the confinement restrictions associated with
	 * the provided arena: that is, if the provided arena is a
	 * {@linkplain Arena#ofConfined() confined arena}, the returned segment can only
	 * be accessed by the arena's owner thread, regardless of the confinement
	 * restrictions associated with this segment. In other words, this method
	 * returns a segment that behaves as if it had been allocated using the provided
	 * arena.
	 * <p>
	 * Clients can specify an optional cleanup action that should be executed when
	 * the provided scope becomes invalid. This cleanup action receives a fresh
	 * memory segment that is obtained from this segment as follows:
	 * {@snippet lang = java :
	 * MemorySegment cleanupSegment = MemorySegment.ofAddress(this.address())
	 * 		.reinterpret(newSize);
	 * }
	 * That is, the cleanup action receives a segment that is associated with the
	 * global scope, and is accessible from any thread. The size of the segment
	 * accepted by the cleanup action is {@code newSize}.
	 *
	 * @apiNote The cleanup action (if present) should take care not to leak the
	 *          received segment to external clients that might access the segment
	 *          after its backing region of memory is no longer available.
	 *          Furthermore, if the provided scope is the scope of an
	 *          {@linkplain Arena#ofAuto() automatic arena}, the cleanup action must
	 *          not prevent the scope from becoming <a href=
	 *          "../../../java/lang/ref/package.html#reachability">unreachable</a>.
	 *          A failure to do so will permanently prevent the regions of memory
	 *          allocated by the automatic arena from being deallocated.
	 *
	 * @param newSize the size of the returned segment
	 * @param arena   the arena to be associated with the returned segment
	 * @param cleanup the cleanup action that should be executed when the provided
	 *                arena is closed (can be {@code null}).
	 * @return a new segment that has the same address as this segment, but with the
	 *         new size and its scope set to that of the provided arena.
	 * @throws UnsupportedOperationException if this segment is not a
	 *                                       {@linkplain #isNative() native} segment
	 * @throws IllegalArgumentException      if {@code newSize < 0}
	 * @throws IllegalStateException         if
	 *                                       {@code arena.scope().isAlive() == false}
	 * @throws IllegalCallerException        if the caller is in a module that does
	 *                                       not have native access enabled
	 */
	public static MemorySegment reinterpret(MemorySegment pointer, long newSize,
			Arena arena,
			Consumer<MemorySegment> cleanup) {
		return pointer.reinterpret(newSize, arena, cleanup);
	}

	/**
	 * Read address.
	 *
	 * @param handle    the handle
	 * @param addressAt the address at
	 * @return the memory segment
	 */
	public static MemorySegment readAddress(VarHandle handle, MemorySegment addressAt) {
		var read = (MemorySegment) handle.get(addressAt);
		return read;
	}

	/**
	 * Instantiates a new foreign utils.
	 */
	private ForeignUtils() {}

}

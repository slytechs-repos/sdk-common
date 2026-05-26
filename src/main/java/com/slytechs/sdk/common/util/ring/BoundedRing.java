/*
 * Copyright 2005-2026 Sly Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.slytechs.sdk.common.util.ring;

/**
 * A {@link Ring} with a fixed maximum capacity.
 *
 * <p>
 * Bounded rings reject puts when full unless the calling thread is willing to
 * block or wait. They are the standard choice for back-pressure-driven
 * pipelines where producer rate must not outrun consumer rate.
 * </p>
 *
 * @param <T> the type of items held in the ring
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public interface BoundedRing<T> extends Ring<T> {

	/**
	 * Returns the maximum number of items this ring can hold.
	 *
	 * @return the capacity
	 */
	int capacity();

	/**
	 * Returns {@code true} if the ring is at capacity.
	 *
	 * <p>
	 * The returned value is a snapshot and may be stale under concurrent
	 * access.
	 * </p>
	 *
	 * @return {@code true} if full
	 */
	boolean isFull();

	/**
	 * Returns the number of items that can be inserted before the ring is
	 * full.
	 *
	 * <p>
	 * Equivalent to {@code capacity() - size()}. The returned value is a
	 * snapshot and may be stale under concurrent access.
	 * </p>
	 *
	 * @return remaining capacity
	 */
	int remaining();
}
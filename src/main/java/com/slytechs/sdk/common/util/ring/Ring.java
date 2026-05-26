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

import java.time.Duration;

/**
 * High-throughput ring buffer for networking pipelines.
 *
 * <p>
 * {@code Ring} is a producer-consumer data structure optimized for low
 * latency and high throughput in packet-processing scenarios. It exposes a
 * minimal API surface — put on one side, take on the other — without the
 * overhead and contract complexity of the JDK collections framework.
 * </p>
 *
 * <p>
 * Concrete implementations declare their concurrency characteristics through
 * marker interfaces:
 * </p>
 * <ul>
 * <li>{@link SpscCapable} — one producer, one consumer</li>
 * <li>{@link MpscCapable} — many producers, one consumer</li>
 * <li>{@link SpmcCapable} — one producer, many consumers</li>
 * <li>{@link MpmcCapable} — many producers, many consumers</li>
 * </ul>
 *
 * <p>
 * Capacity semantics are split across two sub-interfaces:
 * </p>
 * <ul>
 * <li>{@link BoundedRing} — fixed maximum capacity, may reject puts when
 * full</li>
 * <li>{@link UnboundedRing} — grows as needed, puts always succeed (subject
 * to available memory)</li>
 * </ul>
 *
 * @param <T> the type of items held in the ring
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see BoundedRing
 * @see UnboundedRing
 */
public interface Ring<T> {

	/**
	 * Inserts an item into the ring without blocking.
	 *
	 * <p>
	 * For bounded rings, returns {@code false} if the ring is full. For unbounded
	 * rings, returns {@code true} unless an internal allocation fails.
	 * </p>
	 *
	 * @param item the item to insert
	 * @return {@code true} if the item was inserted
	 */
	boolean offer(T item);

	/**
	 * Inserts an item into the ring, blocking if necessary until space is
	 * available.
	 *
	 * @param item the item to insert
	 * @throws InterruptedException if the current thread is interrupted while
	 *                              waiting for space
	 */
	void put(T item) throws InterruptedException;

	/**
	 * Inserts an item into the ring, waiting up to the specified duration if
	 * the ring is full.
	 *
	 * @param item    the item to insert
	 * @param timeout maximum time to wait for space
	 * @return {@code true} if the item was inserted within the timeout,
	 *         {@code false} if the timeout expired
	 * @throws InterruptedException if the current thread is interrupted while
	 *                              waiting
	 */
	boolean tryOffer(T item, Duration timeout) throws InterruptedException;

	/**
	 * Retrieves and removes the next item from the ring without blocking.
	 *
	 * @return the next item, or {@code null} if the ring is empty
	 */
	T poll();

	/**
	 * Retrieves and removes the next item from the ring, blocking if necessary
	 * until an item becomes available.
	 *
	 * @return the next item
	 * @throws InterruptedException if the current thread is interrupted while
	 *                              waiting
	 */
	T take() throws InterruptedException;

	/**
	 * Retrieves and removes the next item from the ring, waiting up to the
	 * specified duration if the ring is empty.
	 *
	 * @param timeout maximum time to wait
	 * @return the next item, or {@code null} if the timeout expired
	 * @throws InterruptedException if the current thread is interrupted while
	 *                              waiting
	 */
	T tryPoll(Duration timeout) throws InterruptedException;

	/**
	 * Returns the current number of items in the ring.
	 *
	 * <p>
	 * The returned value is a snapshot — under concurrent access it may be
	 * stale by the time the caller observes it. Use as an estimate, not a
	 * synchronization point.
	 * </p>
	 *
	 * @return the current size
	 */
	int size();

	/**
	 * Returns {@code true} if the ring contains no items.
	 *
	 * <p>
	 * As with {@link #size()}, the returned value is a snapshot and may be
	 * stale under concurrent access.
	 * </p>
	 *
	 * @return {@code true} if empty
	 */
	boolean isEmpty();
}
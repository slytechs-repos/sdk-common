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
import java.util.Comparator;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Multi-producer, single-consumer bounded ring that delivers items to the
 * consumer in key order via k-way merge across per-producer sub-rings.
 *
 * <p>
 * This is the structure used by the packet mixer for streams that need
 * key-ordered output (for example, ARRIVAL_TIMESTAMP or PACKET_INDEX). Each
 * producer registers via {@link #registerProducer()} to obtain its own SPSC
 * sub-ring. The consumer side performs a k-way merge across all sub-rings
 * using a priority queue to select the next item by the configured
 * comparator.
 * </p>
 *
 * <p>
 * The total capacity is split evenly across registered sub-rings. Producers
 * are typically registered up front during pipeline construction, then
 * pinned to their sub-ring for the lifetime of the merge.
 * </p>
 *
 * <p>
 * <b>Ordering correctness</b> requires that each producer submit items to
 * its sub-ring already in non-decreasing key order. The merge guarantees
 * global key order across producers; it does not sort within a producer's
 * own stream. For the mixer case this is naturally satisfied because each
 * worker drains its channel in arrival order.
 * </p>
 *
 * <p>
 * <b>Thread safety:</b> producer threads each operate on their assigned
 * sub-ring independently. Exactly one thread may call consumer-side
 * methods.
 * </p>
 *
 * @param <T> the type of items held in the ring
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public final class KeyedMergeRing<T> implements BoundedRing<T>, MpscCapable {

	private final Comparator<? super T> comparator;
	private final int capacity;
	private final int subRingCapacity;
	private final ArraySpscRing<T>[] subRings;
	private final AtomicInteger nextProducerId = new AtomicInteger(0);

	// consumer-only state — no synchronization needed
	private final PriorityQueue<MergeEntry<T>> heap;

	@SuppressWarnings("unchecked")
	public KeyedMergeRing(int totalCapacity, int producerCount, Comparator<? super T> comparator) {
		if (totalCapacity < producerCount)
			throw new IllegalArgumentException("capacity must be >= producerCount");
		if (producerCount < 1)
			throw new IllegalArgumentException("producerCount must be >= 1");

		this.comparator = Objects.requireNonNull(comparator, "comparator");
		this.subRingCapacity = totalCapacity / producerCount;
		this.capacity = subRingCapacity * producerCount;

		this.subRings = new ArraySpscRing[producerCount];
		for (int i = 0; i < producerCount; i++)
			this.subRings[i] = new ArraySpscRing<>(subRingCapacity);

		this.heap = new PriorityQueue<>(producerCount,
				(a, b) -> comparator.compare(a.item, b.item));
	}

	/**
	 * Registers a new producer and returns the SPSC sub-ring assigned to it.
	 *
	 * <p>
	 * Each producer thread calls this once during setup, retains the returned
	 * ring, and uses it for all subsequent puts. Producers must not exceed
	 * the registered count fixed at construction.
	 * </p>
	 *
	 * @return the producer's assigned sub-ring
	 * @throws IllegalStateException if more producers register than were
	 *                               configured at construction
	 */
	public ArraySpscRing<T> registerProducer() {
		int id = nextProducerId.getAndIncrement();
		if (id >= subRings.length)
			throw new IllegalStateException(
					"More producers registered than configured (max=" + subRings.length + ")");
		return subRings[id];
	}

	/**
	 * Returns the comparator used to order items across producers.
	 *
	 * @return the comparator
	 */
	public Comparator<? super T> comparator() {
		return comparator;
	}

	/**
	 * <p>
	 * <b>Note:</b> the unified put-side methods on this ring are not the
	 * preferred path for KeyedMergeRing. Producers should call
	 * {@link #registerProducer()} once and use the returned sub-ring directly.
	 * The Ring-level offer methods round-robin across sub-rings, which does
	 * not preserve per-producer ordering and may cause merge ordering to
	 * degrade.
	 * </p>
	 */
	@Override
	public boolean offer(T item) {
		Objects.requireNonNull(item, "item");

		// fall-back: round-robin across sub-rings
		// not recommended — use registerProducer() instead
		int start = (int) (Thread.currentThread().getId() % subRings.length);
		for (int i = 0; i < subRings.length; i++) {
			ArraySpscRing<T> r = subRings[(start + i) % subRings.length];
			if (r.offer(item))
				return true;
		}
		return false;
	}

	@Override
	public void put(T item) throws InterruptedException {
		while (!offer(item)) {
			if (Thread.interrupted())
				throw new InterruptedException();
			Thread.onSpinWait();
		}
	}

	@Override
	public boolean tryOffer(T item, Duration timeout) throws InterruptedException {
		long deadlineNanos = System.nanoTime() + timeout.toNanos();

		while (!offer(item)) {
			if (Thread.interrupted())
				throw new InterruptedException();

			long remaining = deadlineNanos - System.nanoTime();
			if (remaining <= 0)
				return false;

			Thread.onSpinWait();
		}
		return true;
	}

	@Override
	public T poll() {
		// refill heap: any sub-ring not currently represented should contribute
		// its head if non-empty
		for (int i = 0; i < subRings.length; i++) {
			if (heapHasProducer(i))
				continue;

			T head = subRings[i].poll();
			if (head != null)
				heap.offer(new MergeEntry<>(i, head));
		}

		if (heap.isEmpty())
			return null;

		MergeEntry<T> winner = heap.poll();
		T item = winner.item;

		// try to refill from the winning producer immediately
		T next = subRings[winner.producerId].poll();
		if (next != null)
			heap.offer(new MergeEntry<>(winner.producerId, next));

		return item;
	}

	@Override
	public T take() throws InterruptedException {
		T item;
		while ((item = poll()) == null) {
			if (Thread.interrupted())
				throw new InterruptedException();
			Thread.onSpinWait();
		}
		return item;
	}

	@Override
	public T tryPoll(Duration timeout) throws InterruptedException {
		long deadlineNanos = System.nanoTime() + timeout.toNanos();

		T item;
		while ((item = poll()) == null) {
			if (Thread.interrupted())
				throw new InterruptedException();

			long remaining = deadlineNanos - System.nanoTime();
			if (remaining <= 0)
				return null;

			Thread.onSpinWait();
		}
		return item;
	}

	@Override
	public int capacity() {
		return capacity;
	}

	@Override
	public int size() {
		int total = heap.size();
		for (ArraySpscRing<T> r : subRings)
			total += r.size();
		return total;
	}

	@Override
	public boolean isEmpty() {
		if (!heap.isEmpty())
			return false;
		for (ArraySpscRing<T> r : subRings) {
			if (!r.isEmpty())
				return false;
		}
		return true;
	}

	@Override
	public boolean isFull() {
		for (ArraySpscRing<T> r : subRings) {
			if (!r.isFull())
				return false;
		}
		return true;
	}

	@Override
	public int remaining() {
		int total = 0;
		for (ArraySpscRing<T> r : subRings)
			total += r.remaining();
		return total;
	}

	private boolean heapHasProducer(int producerId) {
		for (MergeEntry<T> e : heap) {
			if (e.producerId == producerId)
				return true;
		}
		return false;
	}

	private static final class MergeEntry<T> {
		final int producerId;
		final T item;

		MergeEntry(int producerId, T item) {
			this.producerId = producerId;
			this.item = item;
		}
	}
}
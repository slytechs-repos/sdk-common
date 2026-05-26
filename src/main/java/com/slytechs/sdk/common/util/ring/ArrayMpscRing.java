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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

/**
 * Multi-producer, single-consumer bounded ring backed by a power-of-two
 * sized array.
 *
 * <p>
 * Producers claim slots via atomic compare-and-swap on the producer cursor,
 * write their item, and publish via a release on the array slot. The single
 * consumer reads slots in order using acquire semantics.
 * </p>
 *
 * <p>
 * Slots use a sentinel value to indicate "claimed but not yet written" — the
 * consumer skips slots that haven't been written yet, even if the producer
 * cursor has advanced past them. This avoids a separate sequence array at
 * the cost of one sentinel comparison per consume.
 * </p>
 *
 * <p>
 * <b>Thread safety:</b> any number of threads may call producer-side methods
 * concurrently. Exactly one thread may call consumer-side methods.
 * </p>
 *
 * @param <T> the type of items held in the ring
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public final class ArrayMpscRing<T> implements BoundedRing<T>, MpscCapable {

	// @formatter:off
    private static final VarHandle ARRAY      = MethodHandles.arrayElementVarHandle(Object[].class);
    private static final VarHandle PRODUCER   = findHandle("producerSeq");
    private static final VarHandle CONSUMER   = findHandle("consumerSeq");
    // @formatter:on

	private static VarHandle findHandle(String name) {
		try {
			return MethodHandles.lookup().findVarHandle(ArrayMpscRing.class, name, long.class);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private final Object[] buffer;
	private final int mask;
	private final int capacity;

	@SuppressWarnings("unused")
	private volatile long producerSeq;

	@SuppressWarnings("unused")
	private long consumerSeq;

	public ArrayMpscRing(int requestedCapacity) {
		if (requestedCapacity < 1)
			throw new IllegalArgumentException("capacity must be >= 1");

		this.capacity = nextPowerOfTwo(requestedCapacity);
		this.mask = capacity - 1;
		this.buffer = new Object[capacity];
	}

	@Override
	public boolean offer(T item) {
		Objects.requireNonNull(item, "item");

		long producer;
		long consumer;
		do {
			producer = (long) PRODUCER.getAcquire(this);
			consumer = (long) CONSUMER.getAcquire(this);

			if (producer - consumer >= capacity)
				return false;

		} while (!PRODUCER.compareAndSet(this, producer, producer + 1));

		ARRAY.setRelease(buffer, (int) (producer & mask), item);
		return true;
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

			LockSupport.parkNanos(Math.min(remaining, 1_000_000L));
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T poll() {
		long consumer = (long) CONSUMER.getOpaque(this);
		long producer = (long) PRODUCER.getAcquire(this);

		if (consumer >= producer)
			return null;

		int idx = (int) (consumer & mask);
		T item = (T) ARRAY.getAcquire(buffer, idx);

		if (item == null)
			// producer has claimed this slot but hasn't written yet
			return null;

		ARRAY.setRelease(buffer, idx, null);
		CONSUMER.setRelease(this, consumer + 1);
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

			LockSupport.parkNanos(Math.min(remaining, 1_000_000L));
		}
		return item;
	}

	@Override
	public int capacity() {
		return capacity;
	}

	@Override
	public int size() {
		long producer = (long) PRODUCER.getAcquire(this);
		long consumer = (long) CONSUMER.getAcquire(this);
		return (int) (producer - consumer);
	}

	@Override
	public boolean isEmpty() {
		long producer = (long) PRODUCER.getAcquire(this);
		long consumer = (long) CONSUMER.getAcquire(this);
		return producer == consumer;
	}

	@Override
	public boolean isFull() {
		long producer = (long) PRODUCER.getAcquire(this);
		long consumer = (long) CONSUMER.getAcquire(this);
		return producer - consumer >= capacity;
	}

	@Override
	public int remaining() {
		return capacity - size();
	}

	private static int nextPowerOfTwo(int n) {
		if (n <= 1)
			return 1;
		return Integer.highestOneBit(n - 1) << 1;
	}
}
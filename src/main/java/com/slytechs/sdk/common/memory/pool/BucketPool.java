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
package com.slytechs.sdk.common.memory.pool;

import java.util.function.Supplier;

/**
 * Size-based bucket pool for efficient allocation of variable-sized objects.
 * 
 * <p>
 * BucketPool maintains multiple {@link FreeListPool} instances, each handling
 * objects of a specific size class. Allocation requests are routed to the
 * smallest bucket that can satisfy the requested size, reducing memory waste
 * while maintaining pool efficiency.
 * </p>
 * 
 * <h2>Simple Objects</h2>
 * 
 * <pre>{@code
 * long[] sizes = {
 * 		1518,
 * 		9000,
 * 		16384,
 * 		65536
 * };
 * BucketPool<MyObject> pool = new BucketPool<>(settings, sizes, MyObject::new);
 * }</pre>
 * 
 * <h2>Objects with Memory Components</h2>
 * 
 * <pre>{@code
 * // Factory receives allocator AND bucket size
 * BucketPool<Packet> pool = new BucketPool<>(settings, sizes, (allocator, bucketSize) -> {
 * 	MemorySegment data = allocator.allocate(bucketSize, 8);
 * 	MemorySegment desc = allocator.allocate(128, 8);
 * 	return Packet.ofFixed(DescriptorType.NET, data, desc);
 * });
 * }</pre>
 *
 * @param <T> the type of poolable objects managed by this pool
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see Pool
 * @see FreeListPool
 */
public class BucketPool<T extends Poolable> implements Pool<T> {

	/**
	 * Factory for creating poolable objects in a bucketed pool. Receives both
	 * allocator and bucket size.
	 */
	@FunctionalInterface
	public interface BucketFactory<T extends Poolable> {
		T create(SlabAllocator allocator, long bucketSize);
	}

	private final FreeListPool<T>[] buckets;
	private final long[] sizes;
	private final BucketMetrics metrics;
	private volatile boolean closed;

	/**
	 * Creates a bucket pool with a simple factory.
	 *
	 * @param settings base settings (capacity applies per bucket)
	 * @param sizes    array of bucket sizes in ascending order
	 * @param factory  simple factory to create poolable objects
	 */
	public BucketPool(PoolSettings settings, long[] sizes, Supplier<T> factory) {
		this(settings, sizes, (_, _) -> factory.get());
	}

	/**
	 * Creates a bucket pool with a memory-aware factory.
	 *
	 * @param settings base settings (capacity applies per bucket)
	 * @param sizes    array of bucket sizes in ascending order
	 * @param factory  factory that receives allocator and bucket size
	 */
	@SuppressWarnings("unchecked")
	public BucketPool(PoolSettings settings, long[] sizes, BucketFactory<T> factory) {
		validateSizes(sizes);

		this.sizes = sizes.clone();
		this.buckets = new FreeListPool[sizes.length];
		this.metrics = new BucketMetrics();
		this.closed = false;

		for (int i = 0; i < sizes.length; i++) {
			final long bucketSize = sizes[i];

			PoolSettings bucketSettings = new PoolSettings()
					.minCapacity(settings.minCapacity())
					.maxCapacity(settings.maxCapacity())
					.segmentSize(bucketSize)
					.contractionEnabled(settings.contractionEnabled())
					.checkpointInterval(settings.checkpointInterval())
					.contractionCheckpoints(settings.contractionCheckpoints())
					.contractionThreshold(settings.contractionThreshold());

			// Wrap BucketFactory to provide bucket size to factory
			buckets[i] = new FreeListPool<>(bucketSettings,
					(SlabAllocator allocator) -> factory.create(allocator, bucketSize));
		}
	}

	private void validateSizes(long[] sizes) {
		if (sizes == null || sizes.length == 0) {
			throw new IllegalArgumentException("Sizes array cannot be null or empty");
		}
		for (int i = 0; i < sizes.length; i++) {
			if (sizes[i] <= 0) {
				throw new IllegalArgumentException("Size must be positive: " + sizes[i]);
			}
			if (i > 0 && sizes[i] <= sizes[i - 1]) {
				throw new IllegalArgumentException(
						"Sizes must be in ascending order: " + sizes[i - 1] + " >= " + sizes[i]);
			}
		}
	}

	@Override
	public T allocate() {
		return allocate(sizes[0]);
	}

	@Override
	public T allocate(long requiredSize) {
		int bucketIndex = findBucket(requiredSize);
		if (bucketIndex < 0) {
			metrics.exhaustions++;
			return null;
		}

		T item = buckets[bucketIndex].allocate();
		if (item != null) {
			metrics.allocations++;
			metrics.bucketAllocations[bucketIndex]++;
		} else {
			metrics.exhaustions++;
			metrics.bucketExhaustions[bucketIndex]++;
		}
		return item;
	}

	@Override
	public void releaseEntry(PoolEntry entry) {
		if (entry == null || closed) {
			return;
		}
		if (entry.owningPool != null) {
			entry.owningPool.releaseEntry(entry);
			metrics.releases++;
		}
	}

	private int findBucket(long size) {
		for (int i = 0; i < sizes.length; i++) {
			if (sizes[i] >= size) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public long minCapacity() {
		long total = 0;
		for (FreeListPool<T> bucket : buckets) {
			total += bucket.minCapacity();
		}
		return total;
	}

	@Override
	public long maxCapacity() {
		long total = 0;
		for (FreeListPool<T> bucket : buckets) {
			total += bucket.maxCapacity();
		}
		return total;
	}

	@Override
	public long capacity() {
		long total = 0;
		for (FreeListPool<T> bucket : buckets) {
			total += bucket.capacity();
		}
		return total;
	}

	@Override
	public long available() {
		long total = 0;
		for (FreeListPool<T> bucket : buckets) {
			total += bucket.available();
		}
		return total;
	}

	@Override
	public long maxByteSize() {
		return sizes[sizes.length - 1];
	}

	@Override
	public long contractUnused(float percent) {
		long total = 0;
		for (FreeListPool<T> bucket : buckets) {
			total += bucket.contractUnused(percent);
		}
		if (total > 0) {
			metrics.contractions++;
			metrics.evictions += total;
		}
		return total;
	}

	@Override
	public long contractUnused(long count) {
		long perBucket = count / buckets.length;
		long total = 0;
		for (FreeListPool<T> bucket : buckets) {
			total += bucket.contractUnused(perBucket);
		}
		if (total > 0) {
			metrics.contractions++;
			metrics.evictions += total;
		}
		return total;
	}

	@Override
	public long grow(long count) {
		long perBucket = count / buckets.length;
		long total = 0;
		for (FreeListPool<T> bucket : buckets) {
			total += bucket.grow(perBucket);
		}
		if (total > 0) {
			metrics.growthEvents++;
		}
		return total;
	}

	/**
	 * Returns the pool for a specific bucket.
	 *
	 * @param bucketIndex bucket index
	 * @return the pool for that bucket
	 */
	public Pool<T> bucket(int bucketIndex) {
		return buckets[bucketIndex];
	}

	/**
	 * Returns the number of buckets.
	 *
	 * @return bucket count
	 */
	public int bucketCount() {
		return buckets.length;
	}

	/**
	 * Returns the size for a specific bucket.
	 *
	 * @param bucketIndex the bucket index
	 * @return the size for that bucket
	 */
	public long bucketSize(int bucketIndex) {
		return sizes[bucketIndex];
	}

	/**
	 * Returns the size classes.
	 *
	 * @return copy of the sizes array
	 */
	public long[] sizes() {
		return sizes.clone();
	}

	@Override
	public PoolMetrics metrics() {
		return metrics;
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}
		closed = true;
		for (FreeListPool<T> bucket : buckets) {
			bucket.close();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("BucketPool[buckets=").append(buckets.length);
		sb.append(", sizes=[");
		for (int i = 0; i < sizes.length; i++) {
			if (i > 0)
				sb.append(", ");
			sb.append(sizes[i]);
		}
		sb.append("], capacity=").append(capacity());
		sb.append(", available=").append(available());
		sb.append("]");
		return sb.toString();
	}

	/**
	 * Internal metrics with per-bucket tracking.
	 */
	private class BucketMetrics implements PoolMetrics {
		volatile long allocations;
		volatile long releases;
		volatile long exhaustions;
		volatile long growthEvents;
		volatile long contractions;
		volatile long evictions;

		final long[] bucketAllocations = new long[buckets.length];
		final long[] bucketExhaustions = new long[buckets.length];

		@Override
		public long allocations() {
			return allocations;
		}

		@Override
		public long releases() {
			long total = 0;
			for (FreeListPool<T> bucket : buckets) {
				total += bucket.metrics().releases();
			}
			return total;
		}

		@Override
		public long exhaustions() {
			return exhaustions;
		}

		@Override
		public long growthEvents() {
			return growthEvents;
		}

		@Override
		public long contractions() {
			return contractions;
		}

		@Override
		public long evictions() {
			return evictions;
		}

		@Override
		public int bucketCount() {
			return buckets.length;
		}

		@Override
		public long bucketSize(int bucketIndex) {
			return sizes[bucketIndex];
		}

		@Override
		public long allocations(int bucketIndex) {
			return bucketAllocations[bucketIndex];
		}

		@Override
		public long releases(int bucketIndex) {
			return buckets[bucketIndex].metrics().releases();
		}

		@Override
		public long exhaustions(int bucketIndex) {
			return bucketExhaustions[bucketIndex];
		}

		@Override
		public long bucketCapacity(int bucketIndex) {
			return buckets[bucketIndex].capacity();
		}

		@Override
		public long bucketAvailable(int bucketIndex) {
			return buckets[bucketIndex].available();
		}
	}
}
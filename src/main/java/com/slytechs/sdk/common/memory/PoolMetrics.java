package com.slytechs.sdk.common.memory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Lightweight pool metrics for monitoring.
 */
public class PoolMetrics {
	private final String poolName;
	private final long capacity;
	private final AtomicLong allocations = new AtomicLong(0);
	private final AtomicLong releases = new AtomicLong(0);
	private final AtomicLong exhaustions = new AtomicLong(0);

	public PoolMetrics(String poolName, long capacity) {
		this.poolName = poolName;
		this.capacity = capacity;
	}

	public void recordAllocation() {
		allocations.incrementAndGet();
	}

	public void recordRelease() {
		releases.incrementAndGet();
	}

	public void recordExhaustion() {
		exhaustions.incrementAndGet();
	}

	// Getters
	public String getName() {
		return poolName;
	}

	public long getCapacity() {
		return capacity;
	}

	public long getAllocations() {
		return allocations.get();
	}

	public long getReleases() {
		return releases.get();
	}

	public long getExhaustions() {
		return exhaustions.get();
	}

	/**
	 * Creates an aggregated view of multiple metrics.
	 * 
	 * @param name    name for the aggregated metrics
	 * @param metrics metrics to aggregate
	 * @return new PoolMetrics with summed values
	 */
	public static PoolMetrics aggregate(String name, PoolMetrics... metrics) {
		int totalCapacity = 0;
		int totalAllocations = 0;
		int totalReleases = 0;
		int totalExhaustions = 0;
		int totalAvailable = 0;

		for (PoolMetrics m : metrics) {
			totalCapacity += m.getCapacity();
			totalAllocations += m.getAllocations();
			totalReleases += m.getReleases();
			totalExhaustions += m.getExhaustions();
		}

		PoolMetrics aggregated = new PoolMetrics(name, totalCapacity);
		aggregated.allocations.set(totalAllocations);
		aggregated.releases.set(totalReleases);
		aggregated.exhaustions.set(totalExhaustions);

		return aggregated;
	}

	@Override
	public String toString() {
		return String.format("%s[capacity=%d, allocations=%d, releases=%d, exhaustions=%d]",
				poolName, capacity, allocations.get(), releases.get(), exhaustions.get());
	}
}
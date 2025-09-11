package com.slytechs.jnet.core.api.memory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lightweight pool metrics for monitoring.
 */
public class PoolMetrics {
	private final String poolName;
	private final int capacity;
	private final AtomicInteger allocations = new AtomicInteger(0);
	private final AtomicInteger releases = new AtomicInteger(0);
	private final AtomicInteger exhaustions = new AtomicInteger(0);

	public PoolMetrics(String poolName, int capacity) {
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

	public int getCapacity() {
		return capacity;
	}

	public int getAllocations() {
		return allocations.get();
	}

	public int getReleases() {
		return releases.get();
	}

	public int getExhaustions() {
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
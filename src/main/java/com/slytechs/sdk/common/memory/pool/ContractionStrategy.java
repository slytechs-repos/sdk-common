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

/**
 * Strategy for automatic pool contraction.
 * 
 * <p>
 * ContractionStrategy determines when a pool should release excess capacity.
 * The strategy is invoked on each allocation/release cycle and decides whether
 * to trigger contraction based on usage patterns.
 * </p>
 * 
 * <h2>Hot Path Optimization</h2>
 * 
 * <p>
 * The {@link #disabled()} strategy returns a singleton that does nothing. The
 * JIT compiler completely eliminates calls to this no-op implementation,
 * ensuring zero overhead when contraction is disabled.
 * </p>
 * 
 * <h2>Active Strategy</h2>
 * 
 * <p>
 * The {@link #enabled(PoolSettings)} strategy performs periodic checks using a
 * bitmask counter (avoiding modulo operations). When excess capacity remains
 * idle for multiple consecutive checkpoints, contraction is triggered.
 * </p>
 * 
 * {@snippet :
 * // No contraction overhead for small pools
 * Pool<Packet> smallPool = new LockFreePool<>(
 * 		new PoolSettings().maxCapacity(1000).contractionEnabled(false),
 * 		Packet::new);
 * 
 * // Auto-contraction for large pools
 * Pool<Packet> largePool = new LockFreePool<>(
 * 		new PoolSettings().maxCapacity(100_000).contractionEnabled(true),
 * 		Packet::new);
 * }
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see Pool
 * @see PoolSettings
 */
public interface ContractionStrategy {

	/**
	 * Called on each pool allocation.
	 * 
	 * <p>
	 * The strategy may use this to track allocation cycles and periodically check
	 * for contraction opportunities.
	 * </p>
	 *
	 * @param pool the pool being allocated from
	 */
	void onAllocate(Pool<?> pool);

	/**
	 * Called on each pool release.
	 *
	 * @param pool the pool being released to
	 */
	void onRelease(Pool<?> pool);

	/**
	 * Returns a disabled (no-op) contraction strategy.
	 * 
	 * <p>
	 * The returned singleton has empty method bodies. The JIT compiler eliminates
	 * calls to this implementation entirely, ensuring zero overhead when
	 * contraction is disabled.
	 * </p>
	 *
	 * @return disabled strategy singleton
	 */
	static ContractionStrategy disabled() {
		return DisabledContractionStrategy.INSTANCE;
	}

	/**
	 * Returns an active contraction strategy with the given settings.
	 *
	 * @param settings pool settings containing contraction parameters
	 * @return active contraction strategy
	 */
	static ContractionStrategy enabled(PoolSettings settings) {
		return new ActiveContractionStrategy(settings);
	}
}
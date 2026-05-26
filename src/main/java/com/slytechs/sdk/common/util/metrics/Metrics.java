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
package com.slytechs.sdk.common.util.metrics;

import java.time.Instant;

/**
 * Base contract for all metric types in the SDK.
 *
 * <p>
 * {@code Metrics} is the common supertype for both live counter objects and
 * immutable snapshot records. Every metric carries an implicit wall-clock
 * timestamp used by the {@link Statistics} utility for rate and delta
 * calculations between snapshot pairs.
 * </p>
 *
 * <p>
 * Concrete metric types are domain-specific interfaces extending
 * {@code Metrics}: {@code ChannelMetrics}, {@code BufferMetrics},
 * {@code PacketChannelMetrics}, and so on. Each domain interface declares its
 * own counter accessors and a {@code snapshot()} method that returns an
 * immutable view of the current counter values.
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see Statistics
 */
public interface Metrics {

	/**
	 * Returns the current wall-clock time in milliseconds since the Unix epoch.
	 *
	 * <p>
	 * This is the canonical time source for metrics in the SDK. Live counter
	 * objects return the current system time; snapshot records return the time
	 * at which the snapshot was taken. Callers should treat this as
	 * monotonically increasing for the purposes of rate calculations, though
	 * it is subject to the usual wall-clock caveats (NTP adjustments, system
	 * clock changes).
	 * </p>
	 *
	 * @return wall-clock time in milliseconds since the epoch
	 */
	default long timeMilli() {
		return System.currentTimeMillis();
	}

	/**
	 * Returns the timestamp as an {@link Instant}.
	 *
	 * <p>
	 * Convenience accessor that converts the result of {@link #timeMilli()}
	 * into a richer time type for logging, display, and serialization.
	 * </p>
	 *
	 * @return the timestamp as an Instant
	 */
	default Instant time() {
		return Instant.ofEpochMilli(timeMilli());
	}

	/**
	 * Computes statistics for this metric relative to an earlier snapshot.
	 *
	 * <p>
	 * This is an optional convenience method on the metric itself for the
	 * common case of comparing a live or current snapshot against an earlier
	 * one. The default implementation throws
	 * {@link UnsupportedOperationException}; concrete metric types may
	 * override it to return a domain-specific statistics view.
	 * </p>
	 *
	 * <p>
	 * For most statistics calculations, use the static helpers in
	 * {@link Statistics} directly rather than this method.
	 * </p>
	 *
	 * @param last the earlier snapshot to compare against
	 * @param <T>  the specific metric type
	 * @return a statistics view derived from the two metrics
	 * @throws UnsupportedOperationException if the concrete metric type does
	 *                                       not provide a statistics
	 *                                       implementation
	 */
	default <T extends Metrics> T calculateStatistics(T last) {
		throw new UnsupportedOperationException();
	}
}
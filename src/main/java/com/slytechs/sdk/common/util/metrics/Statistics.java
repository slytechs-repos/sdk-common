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

import java.util.function.Function;

/**
 * Utility helpers for computing rates, deltas, and averages between pairs of
 * metric snapshots.
 *
 * <p>
 * {@code Statistics} operates on pairs of {@link Metrics} snapshots and
 * produces derived values such as rate of change per second. Because
 * snapshots carry their own timestamps via {@link Metrics#timeMilli()}, no
 * additional time-tracking state is required — callers pass two snapshots
 * and a counter extractor, and the utility computes the result.
 * </p>
 *
 * <p>
 * All methods are static and stateless. Pass the earlier snapshot as the
 * {@code start} argument and the later one as {@code end}.
 * </p>
 *
 * <h2>Typical Usage</h2>
 *
 * <pre>{@code
 * ChannelMetrics before = channel.metrics();
 * // ... some time passes ...
 * ChannelMetrics after  = channel.metrics();
 *
 * double readsPerSec = Statistics.average(before, after, ChannelMetrics::readCount);
 * double dropsPerSec = Statistics.average(before, after, ChannelMetrics::dropCount);
 * }</pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 * @see Metrics
 */
public interface Statistics {

	/** Conversion factor for converting milliseconds to seconds. */
	double MILLIS_TO_SECONDS = 1000.;

	/**
	 * Computes the rate of change per second from a pair of raw counter values
	 * and an elapsed duration.
	 *
	 * <p>
	 * Most callers should prefer
	 * {@link #average(Metrics, Metrics, Function)} which derives the duration
	 * automatically from snapshot timestamps.
	 * </p>
	 *
	 * @param start         the earlier counter value
	 * @param end           the later counter value
	 * @param durationMilli the elapsed time between the two counter readings,
	 *                      in milliseconds
	 * @return the rate of change per second
	 */
	static double average(long start, long end, long durationMilli) {
		return ((end - start) / durationMilli) * MILLIS_TO_SECONDS;
	}

	/**
	 * Computes the rate of change per second for a single counter between two
	 * metric snapshots.
	 *
	 * <p>
	 * The counter is extracted from each snapshot via the provided getter.
	 * The elapsed time is derived from the snapshot timestamps.
	 * </p>
	 *
	 * @param start  the earlier snapshot
	 * @param end    the later snapshot
	 * @param getter a function that extracts the counter value from a snapshot
	 * @param <T>    the specific metric type
	 * @return the rate of change per second for the extracted counter
	 */
	static <T extends Metrics> double average(T start, T end, Function<T, Long> getter) {
		long s = getter.apply(start);
		long e = getter.apply(end);
		long dur = end.timeMilli() - start.timeMilli();

		return average(s, e, dur);
	}
}
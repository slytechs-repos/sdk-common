/**
 * Lightweight, zero-cost metrics collection and snapshot facility.
 *
 * <p>
 * This package provides the core contracts for collecting counter-based
 * metrics throughout the SDK. The design is optimized for high-throughput
 * scenarios where counter increments must add no measurable overhead to the
 * hot path, while still allowing readers to take immutable snapshots on
 * demand for diagnostic tooling, state tree rendering, and statistical
 * analysis.
 * </p>
 *
 * <h2>Core Pattern</h2>
 *
 * <p>
 * Each component that needs metrics follows a consistent three-part shape:
 * </p>
 * <ol>
 * <li>A public {@code XxxMetrics} interface extending {@link Metrics} that
 * declares counter accessors and a {@code snapshot()} method</li>
 * <li>A package-private {@code XxxCounters} implementation holding the live
 * counter fields, written by the owning thread with plain {@code long}
 * increments</li>
 * <li>A local {@code XxxSnapshot} record defined inside {@code snapshot()}
 * that captures the current counter state as an immutable frozen view</li>
 * </ol>
 *
 * <p>
 * The live counters advance continuously on the hot path without any
 * synchronization — a counter increment compiles to a single register-memory
 * add instruction. When a reader requests a snapshot, the counters are read
 * through {@link java.lang.invoke.VarHandle} with volatile semantics, which
 * inserts a memory fence and guarantees the reader sees the producer's most
 * recent writes. The resulting record is fully immutable and can be retained,
 * compared, or serialized without concern for ongoing mutations on the live
 * counters.
 * </p>
 *
 * <h2>Single-Writer Discipline</h2>
 *
 * <p>
 * Live counter objects are written by exactly one thread — the thread that
 * owns the component being measured. This discipline eliminates the need for
 * atomic operations on every increment. Multi-threaded aggregation is done at
 * the snapshot layer: each component produces its own snapshot independently,
 * and aggregated views are computed over collections of snapshots.
 * </p>
 *
 * <h2>Snapshot Consistency</h2>
 *
 * <p>
 * Snapshots are loosely consistent. A snapshot read sequentially visits each
 * counter field, so counters may drift relative to each other by a handful of
 * increments between the first and last field read. This is acceptable for
 * all metric use cases in the SDK — statistical calculations are inherently
 * tolerant of small counter drift, and strict cross-counter invariants
 * (e.g., {@code total == succeeded + failed}) are not maintained by the
 * snapshot contract.
 * </p>
 *
 * <h2>Time Precision</h2>
 *
 * <p>
 * Metrics use millisecond-precision wall-clock time from
 * {@link java.lang.System#currentTimeMillis()}. This is sufficient for rate
 * calculations over any realistic observation window and avoids the platform
 * variability of nanosecond clock sources. Components that genuinely require
 * higher precision (for example, hardware-timestamped capture counters) may
 * expose a dedicated high-precision metric alongside the standard one.
 * </p>
 *
 * <h2>Statistics</h2>
 *
 * <p>
 * The {@link Statistics} utility class provides helpers for computing rates,
 * deltas, and averages between pairs of snapshots. Because snapshots carry
 * their own timestamps, rate calculations are self-contained — callers pass
 * two snapshots and a field extractor, and the utility computes the rate
 * per second without additional time-tracking state.
 * </p>
 *
 * <h2>Typical Usage</h2>
 *
 * <pre>{@code
 * // Producer side (owning thread) — zero overhead
 * counters.readCount++;
 * counters.byteCount += packet.wireLength();
 *
 * // Reader side (any thread) — take a snapshot
 * ChannelMetrics before = channel.metrics();
 * Thread.sleep(1000);
 * ChannelMetrics after = channel.metrics();
 *
 * // Compute throughput
 * double packetsPerSec = Statistics.average(before, after, ChannelMetrics::readCount);
 * }</pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
package com.slytechs.sdk.common.util.metrics;
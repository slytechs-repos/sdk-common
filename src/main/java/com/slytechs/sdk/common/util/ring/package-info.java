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

/**
 * High-performance ring buffer abstractions for networking pipelines.
 *
 * <p>
 * The classes in this package provide minimal-overhead producer-consumer
 * structures optimized for the data-plane workloads in jNetWorks: per-channel
 * RX queues, mixer fan-in buffers, transmit submission rings, and similar.
 * </p>
 *
 * <h2>Design Goals</h2>
 *
 * <p>
 * The ring API deliberately avoids the JDK collections framework — no
 * iteration, no contains, no removeIf, no concurrent collections semantics.
 * Rings are streaming structures: items go in one side and come out the other,
 * and the only operations that matter are the put and take primitives.
 * </p>
 *
 * <p>
 * Implementations declare their concurrency characteristics through marker
 * interfaces ({@link com.slytechs.sdk.common.util.ring.SpscCapable},
 * {@link com.slytechs.sdk.common.util.ring.MpscCapable}, etc.). The choice of
 * implementation depends on the topology — single-producer rings have the
 * fastest hot path; multi-producer rings pay an atomic claim per insert.
 * </p>
 *
 * <h2>Bounded vs Unbounded</h2>
 *
 * <p>
 * {@link com.slytechs.sdk.common.util.ring.BoundedRing} provides fixed
 * capacity with full-rejection semantics — natural back-pressure when the
 * consumer falls behind. {@link com.slytechs.sdk.common.util.ring.UnboundedRing}
 * grows as needed and is appropriate when memory pressure is preferred over
 * blocking the producer, or when consumer rate is provably faster than
 * producer rate.
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
package com.slytechs.sdk.common.util.ring;
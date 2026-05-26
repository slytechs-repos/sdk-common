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

/**
 * Marker interface declaring that a ring implementation supports the
 * multi-producer, single-consumer concurrency model.
 *
 * <p>
 * MPSC rings allow many threads to insert items concurrently while a single
 * thread consumes. Producer-side coordination uses atomic operations to claim
 * slots; consumer-side reads are uncontended. This is the typical model for
 * fan-in pipelines (multiple worker threads feeding a single drain).
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public interface MpscCapable {
}
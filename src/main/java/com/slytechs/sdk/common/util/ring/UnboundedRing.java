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
 * A {@link Ring} with no fixed capacity limit.
 *
 * <p>
 * Unbounded rings grow as needed to accept new items. {@link #offer(Object)}
 * and {@link #put(Object)} always succeed unless an internal memory
 * allocation fails. Use unbounded rings when producer rate is known to never
 * exceed consumer rate, or when memory pressure is preferred over
 * back-pressure.
 * </p>
 *
 * @param <T> the type of items held in the ring
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public interface UnboundedRing<T> extends Ring<T> {
	// marker — UnboundedRing IS the contract that capacity is unlimited
}
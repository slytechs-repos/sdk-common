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
 * single-producer, multi-consumer concurrency model.
 *
 * <p>
 * SPMC rings allow a single thread to publish items to many concurrent
 * consumers. Consumer-side coordination uses atomic operations to claim
 * items; producer-side writes are uncontended. This is the typical model
 * for broadcast or work-distribution pipelines.
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public interface SpmcCapable {
}
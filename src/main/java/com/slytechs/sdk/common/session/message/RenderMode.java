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
package com.slytechs.sdk.common.session.message;

/**
 * Controls how {@link LazyArg} and {@link MessageRecord} values are rendered.
 * 
 * <p>
 * The render mode determines what information is included in the output:
 * <ul>
 * <li>{@link #CURRENT} - Shows only the current value</li>
 * <li>{@link #SNAPSHOT} - Shows only the snapshot (initial) value</li>
 * <li>{@link #TRANSITION} - Shows the change from snapshot to current</li>
 * </ul>
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public enum RenderMode {

	/**
	 * Render only the current value.
	 * <p>
	 * Example output: {@code "draining 3 packets"}
	 * </p>
	 */
	CURRENT,

	/**
	 * Render only the snapshot (initial) value.
	 * <p>
	 * Example output: {@code "draining 15 packets"}
	 * </p>
	 */
	SNAPSHOT,

	/**
	 * Render the transition from snapshot to current value.
	 * <p>
	 * If the values are equal, shows just the current value.
	 * If different, shows the transition: {@code "(15→3)"}
	 * </p>
	 * <p>
	 * Example output: {@code "draining (15→3) packets"}
	 * </p>
	 */
	TRANSITION
}
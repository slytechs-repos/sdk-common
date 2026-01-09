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
package com.slytechs.sdk.common.session.managed;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import com.slytechs.sdk.common.session.message.MessageRecord;
import com.slytechs.sdk.common.session.message.RenderMode;

/**
 * Tracks information about a pending wait condition in a managed session.
 * 
 * <p>
 * WaitInfo captures diagnostic information about what is blocking a session
 * from terminating. This includes:
 * <ul>
 * <li>A name identifying the wait condition (e.g., "queue-drain", "dispatch-loop")</li>
 * <li>A message describing what is being waited for (supports lazy evaluation)</li>
 * <li>The time when the wait started</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Example output in tree rendering:
 * <pre>
 * PacketChannel [name=hello-channel, state=DRAINING]
 *   └── queue-drain: draining (15→3) packets [waiting 2.3s]
 * </pre>
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public final class WaitInfo {

	private final String name;
	private final MessageRecord message;
	private final Instant since;

	/**
	 * Creates a new WaitInfo with the given name and message.
	 *
	 * @param name    the identifier for this wait condition
	 * @param message the log record describing the wait
	 */
	public WaitInfo(String name, MessageRecord message) {
		this.name = Objects.requireNonNull(name, "name");
		this.message = Objects.requireNonNull(message, "message");
		this.since = Instant.now();
	}

	/**
	 * Returns the name identifying this wait condition.
	 *
	 * @return the wait name
	 */
	public String name() {
		return name;
	}

	/**
	 * Returns the log record describing what is being waited for.
	 *
	 * @return the wait message
	 */
	public MessageRecord message() {
		return message;
	}

	/**
	 * Returns the instant when this wait started.
	 *
	 * @return the start time
	 */
	public Instant since() {
		return since;
	}

	/**
	 * Returns the duration since this wait started.
	 *
	 * @return the elapsed duration
	 */
	public Duration elapsed() {
		return Duration.between(since, Instant.now());
	}

	/**
	 * Freezes the underlying message, causing lazy arguments to return
	 * their snapshot values.
	 */
	public void freeze() {
		message.freeze();
	}

	/**
	 * Checks if this WaitInfo is frozen.
	 *
	 * @return true if frozen
	 */
	public boolean isFrozen() {
		return message.isFrozen();
	}

	/**
	 * Renders the wait message with the specified mode.
	 *
	 * @param mode the render mode
	 * @return the rendered message
	 */
	public String render(RenderMode mode) {
		return message.render(mode);
	}

	/**
	 * Renders a complete description including name, message, and elapsed time.
	 *
	 * @param mode the render mode for the message
	 * @return the full rendered description
	 */
	public String renderFull(RenderMode mode) {
		Duration elapsed = elapsed();
		String elapsedStr = formatDuration(elapsed);
		return name + ": " + message.render(mode) + " [waiting " + elapsedStr + "]";
	}

	private String formatDuration(Duration duration) {
		long seconds = duration.getSeconds();
		if (seconds < 60) {
			return String.format("%.1fs", duration.toMillis() / 1000.0);
		} else if (seconds < 3600) {
			return String.format("%dm %ds", seconds / 60, seconds % 60);
		} else {
			return String.format("%dh %dm", seconds / 3600, (seconds % 3600) / 60);
		}
	}

	@Override
	public String toString() {
		return renderFull(RenderMode.TRANSITION);
	}
}
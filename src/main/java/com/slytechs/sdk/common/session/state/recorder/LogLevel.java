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
package com.slytechs.sdk.common.session.state.recorder;

/**
 * Log levels for state recording, following SLF4J conventions.
 * 
 * <p>
 * Levels are ordered by severity. When filtering, all levels at or above
 * the threshold are included.
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public enum LogLevel {
	
	/** Fine-grained diagnostic events */
	TRACE(0),
	
	/** Diagnostic information for debugging */
	DEBUG(1),
	
	/** Informational messages highlighting progress */
	INFO(2),
	
	/** Potentially harmful situations */
	WARN(3),
	
	/** Error events that might still allow continued operation */
	ERROR(4),
	
	/** Disables all logging */
	OFF(Integer.MAX_VALUE);
	
	private final int severity;
	
	LogLevel(int severity) {
		this.severity = severity;
	}
	
	/**
	 * Returns the severity level (higher = more severe).
	 *
	 * @return severity value
	 */
	public int severity() {
		return severity;
	}
	
	/**
	 * Checks if this level is enabled given a threshold.
	 * 
	 * @param threshold the minimum level to display
	 * @return true if this level should be displayed
	 */
	public boolean isEnabled(LogLevel threshold) {
		return this.severity >= threshold.severity;
	}
}
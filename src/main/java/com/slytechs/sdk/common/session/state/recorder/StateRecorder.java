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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.slytechs.sdk.common.session.state.StateMachine;

/**
 * Records state events for a {@link StateMachine}.
 * 
 * <p>
 * StateRecorder collects {@link StateRecord} instances that capture diagnostic
 * events during state machine operation. Records are hierarchical and can be
 * filtered by {@link LogLevel} during rendering.
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class StateRecorder implements StateLogger {

	private final List<StateRecord> records = Collections.synchronizedList(new LinkedList<>());
	private final StateMachine<?> state;

	public StateRecorder(StateMachine<?> state) {
		this.state = state;
	}

	@Override
	public <R extends StateRecord> R logRecord(R newRecord) {
		records.add(newRecord);
		return newRecord;
	}

	@Override
	public <R extends StateRecord> R logRecord(LogLevel level, R newRecord) {
		records.add(newRecord);
		return newRecord;
	}

	@Override
	public StateRecord log(String template, Object... args) {
		return log(LogLevel.DEBUG, template, args);
	}

	@Override
	public StateRecord log(LogLevel level, String template, Object... args) {
		return logRecord(level, new StateRecord(state, level, template, args));
	}

	/**
	 * Returns all recorded events.
	 *
	 * @return unmodifiable list of records
	 */
	public List<StateRecord> listRecords() {
		return Collections.unmodifiableList(records);
	}

	/**
	 * Clears all recorded events.
	 */
	public void clear() {
		records.clear();
	}

	/**
	 * Returns the number of recorded events.
	 *
	 * @return record count
	 */
	public int size() {
		return records.size();
	}

	// Convenience methods for each log level

	public StateRecord trace(String template, Object... args) {
		return log(LogLevel.TRACE, template, args);
	}

	public StateRecord debug(String template, Object... args) {
		return log(LogLevel.DEBUG, template, args);
	}

	public StateRecord info(String template, Object... args) {
		return log(LogLevel.INFO, template, args);
	}

	public StateRecord warn(String template, Object... args) {
		return log(LogLevel.WARN, template, args);
	}

	public StateRecord error(String template, Object... args) {
		return log(LogLevel.ERROR, template, args);
	}

	// Guarded logging - only create record if level is enabled

	public boolean isTraceEnabled(LogLevel threshold) {
		return LogLevel.TRACE.isEnabled(threshold);
	}

	public boolean isDebugEnabled(LogLevel threshold) {
		return LogLevel.DEBUG.isEnabled(threshold);
	}

	public boolean isInfoEnabled(LogLevel threshold) {
		return LogLevel.INFO.isEnabled(threshold);
	}

	public boolean isWarnEnabled(LogLevel threshold) {
		return LogLevel.WARN.isEnabled(threshold);
	}

	public boolean isErrorEnabled(LogLevel threshold) {
		return LogLevel.ERROR.isEnabled(threshold);
	}
}
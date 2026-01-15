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

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.slytechs.sdk.common.session.state.StateMachine;
import com.slytechs.sdk.common.session.text.Line;

/**
 * A recorded state event with thread and state context.
 * 
 * <p>
 * StateRecord captures diagnostic information at a point in time:
 * </p>
 * <ul>
 *   <li>Timestamp of the event</li>
 *   <li>Log level for filtering</li>
 *   <li>Message template and arguments</li>
 *   <li>Thread snapshot (name, id, state)</li>
 *   <li>State machine snapshot (current/previous state)</li>
 *   <li>Hierarchical children for nested logging</li>
 * </ul>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class StateRecord implements Line, StateLogger {

	private final Instant timestamp;
	private final LogLevel level;
	private final String template;
	private final Object[] args;
	private final Reference<StateMachine<?>> state;
	private final ThreadSnapshot threadSnapshot;
	private final StateSnapshot<?> stateSnapshot;
	private final List<StateRecord> children = Collections.synchronizedList(new LinkedList<>());

	/**
	 * Creates a root-level record from a state machine.
	 */
	public StateRecord(StateMachine<?> state, LogLevel level, String template, Object[] args) {
		this.timestamp = Instant.now();
		this.level = level;
		this.state = new WeakReference<>(state);
		this.template = template;
		this.args = args;
		this.threadSnapshot = new ThreadSnapshot(Thread.currentThread());
		this.stateSnapshot = state.stateSnapshot();
	}

	/**
	 * Creates a child record inheriting context from parent.
	 */
	public StateRecord(StateRecord parent, LogLevel level, String template, Object[] args) {
		this.timestamp = Instant.now();
		this.level = level;
		this.state = parent.state;
		this.template = template;
		this.args = args;
		this.threadSnapshot = new ThreadSnapshot(Thread.currentThread());

		// Use parent's snapshot if state machine is gone or generation changed
		StateMachine<?> machine = state.get();
		boolean isStateStale = machine == null 
				|| parent.stateSnapshot().generationId() != machine.generationId();

		this.stateSnapshot = isStateStale ? parent.stateSnapshot : machine.stateSnapshot();
	}

	public Instant timestamp() {
		return timestamp;
	}

	public LogLevel level() {
		return level;
	}

	public String template() {
		return template;
	}

	public Object[] args() {
		return args;
	}

	public ThreadSnapshot threadSnapshot() {
		return threadSnapshot;
	}

	public StateSnapshot<?> stateSnapshot() {
		return stateSnapshot;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<? extends Line> children() {
		return children;
	}

	/**
	 * Returns child records (typed accessor).
	 */
	public List<StateRecord> childRecords() {
		return Collections.unmodifiableList(children);
	}

	@Override
	public <R extends StateRecord> R logRecord(R newRecord) {
		children.add(newRecord);
		return newRecord;
	}

	@Override
	public <R extends StateRecord> R logRecord(LogLevel level, R newRecord) {
		children.add(newRecord);
		return newRecord;
	}

	@Override
	public StateRecord log(String template, Object... args) {
		return log(LogLevel.DEBUG, template, args);
	}

	@Override
	public StateRecord log(LogLevel level, String template, Object... args) {
		return logRecord(level, new StateRecord(this, level, template, args));
	}

	// Convenience methods for each level
	
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
}
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
package com.slytechs.sdk.common.session.state;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

import com.slytechs.sdk.common.session.state.recorder.LogLevel;
import com.slytechs.sdk.common.session.state.recorder.StateRecord;
import com.slytechs.sdk.common.session.text.LazyArg;

/**
 * Renders a {@link ComponentHierarchy} as an ASCII tree with state records.
 * 
 * <p>
 * Combines the component hierarchy (state machines) with their recorded
 * state events for comprehensive diagnostic output.
 * </p>
 * 
 * <p>
 * Example output:
 * </p>
 * <pre>
 * Lifecycle [name=PcapBackend, state=RUNNING→SHUTDOWN, components=2]
 * │   [DEBUG 14:23:01.123] State changed RUNNING → SHUTDOWN
 * │   [INFO  14:23:01.125] Initiating graceful shutdown
 * ├── Lifecycle [name=hello-capture, state=RUNNING, components=1]
 * │   │   [DEBUG 14:23:00.100] State changed CREATED → RUNNING
 * │   │   [TRACE 14:23:00.101] Dispatch loop started on thread capture-0
 * └── Lifecycle [name=hello-channel, state=DRAINING, components=0]
 *     │   [DEBUG 14:23:01.130] State changed RUNNING → DRAINING
 *     │   [INFO  14:23:01.131] Draining 15 packets from queue
 * </pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class StateTreeRenderer {

	private static final String BRANCH = "├── ";
	private static final String LAST_BRANCH = "└── ";
	private static final String VERTICAL = "│   ";
	private static final String SPACE = "    ";
	private static final String RECORD_BRANCH = "│   ";

	private static final DateTimeFormatter TIME_FORMAT = 
			DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

	private final LogLevel threshold;
	private final boolean showRecords;
	private final boolean showThreadInfo;
	private final Instant referenceTime;

	/**
	 * Creates a renderer with default settings (INFO level, records enabled).
	 */
	public StateTreeRenderer() {
		this(LogLevel.INFO, true, false);
	}

	/**
	 * Creates a renderer with specified settings.
	 *
	 * @param threshold      minimum log level to display
	 * @param showRecords    whether to include state records
	 * @param showThreadInfo whether to include thread information
	 */
	public StateTreeRenderer(LogLevel threshold, boolean showRecords, boolean showThreadInfo) {
		this.threshold = threshold;
		this.showRecords = showRecords;
		this.showThreadInfo = showThreadInfo;
		this.referenceTime = Instant.now();
	}

	/**
	 * Renders a component hierarchy with state records.
	 *
	 * @param root the root of the hierarchy
	 * @return formatted ASCII tree
	 */
	public String render(StateHierarchyTree<?> root) {
		StringBuilder sb = new StringBuilder();
		renderNode(sb, root, "", true, true);
		return sb.toString();
	}

	/**
	 * Static convenience method with default settings.
	 */
	public static String render(StateHierarchyTree<?> root, LogLevel threshold) {
		return new StateTreeRenderer(threshold, true, false).render(root);
	}

	/**
	 * Static convenience method - hierarchy only, no records.
	 */
	public static String renderHierarchyOnly(StateHierarchyTree<?> root) {
		return new StateTreeRenderer(LogLevel.OFF, false, false).render(root);
	}

	private void renderNode(StringBuilder sb, StateHierarchyTree<?> node,
	                        String prefix, boolean isLast, boolean isRoot) {
		// Render the component node
		if (isRoot) {
			sb.append(formatNode(node)).append("\n");
		} else {
			sb.append(prefix);
			sb.append(isLast ? LAST_BRANCH : BRANCH);
			sb.append(formatNode(node)).append("\n");
		}

		// Calculate prefixes
		String childPrefix = isRoot ? "" : prefix + (isLast ? SPACE : VERTICAL);

		// Render state records for this node
		if (showRecords) {
			List<StateRecord> records = node.stateMachine().recorder().listRecords();
			boolean hasChildren = !node.children().isEmpty();
			renderRecords(sb, records, childPrefix, hasChildren);
		}

		// Render children
		var children = node.children();
		for (int i = 0; i < children.size(); i++) {
			boolean lastChild = (i == children.size() - 1);
			renderNode(sb, children.get(i), childPrefix, lastChild, false);
		}
	}

	private void renderRecords(StringBuilder sb, List<StateRecord> records, String prefix, boolean hasMoreSiblings) {
		// Filter records first
		List<StateRecord> filtered = records.stream()
				.filter(r -> r.level().isEnabled(threshold))
				.toList();

		for (int i = 0; i < filtered.size(); i++) {
			boolean isLast = (i == filtered.size() - 1) && !hasMoreSiblings;
			renderRecord(sb, filtered.get(i), prefix, isLast, 0);
		}
	}

	private void renderRecord(StringBuilder sb, StateRecord record, String prefix, boolean isLast, int depth) {
		// Filter by log level
		if (!record.level().isEnabled(threshold)) {
			return;
		}

		sb.append(prefix);
		sb.append(isLast ? LAST_BRANCH : RECORD_BRANCH);
		sb.append(formatRecord(record)).append("\n");

		// Render child records
		List<StateRecord> children = record.childRecords().stream()
				.filter(r -> r.level().isEnabled(threshold))
				.toList();
		
		String childPrefix = prefix + (isLast ? SPACE : VERTICAL);
		for (int i = 0; i < children.size(); i++) {
			boolean lastChild = (i == children.size() - 1);
			renderRecord(sb, children.get(i), childPrefix, lastChild, depth + 1);
		}
	}

	private String formatNode(StateHierarchyTree<?> node) {
		StateMachine<?> machine = node.stateMachine();
		String typeName = formatTypeName(machine);
		String stateStr = formatState(machine);
		long count = node.count();
		int children = node.children().size();

		if (count > 0 && count != children) {
			return "%s [name=%s, state=%s, count=%d]".formatted(
					typeName, node.name(), stateStr, count);
		} else {
			return "%s [name=%s, state=%s]".formatted(
					typeName, node.name(), stateStr);
		}
	}

	private String formatTypeName(StateMachine<?> machine) {
		String name = machine.getClass().getSimpleName();
		if (name.endsWith("StateMachine")) {
			return name.substring(0, name.length() - "StateMachine".length());
		}
		return name;
	}

	private String formatState(StateMachine<?> machine) {
		var current = machine.currentState();
		var previous = machine.previousState();

		if (previous != null && previous != current) {
			return previous + "→" + current;
		}
		return String.valueOf(current);
	}

	private String formatRecord(StateRecord record) {
		String levelStr = String.format("%-5s", record.level());
		String timeStr = TIME_FORMAT.format(record.timestamp());
		String message = formatMessage(record.template(), record.args());

		StringBuilder sb = new StringBuilder();
		sb.append("[").append(levelStr).append(" ").append(timeStr).append("] ");
		sb.append(message);

		if (showThreadInfo) {
			sb.append(" @").append(record.threadSnapshot().name());
		}

		return sb.toString();
	}

	private String formatMessage(String template, Object[] args) {
		if (args == null || args.length == 0) {
			return template;
		}

		StringBuilder result = new StringBuilder();
		int argIndex = 0;
		int i = 0;

		while (i < template.length()) {
			if (i < template.length() - 1 && template.charAt(i) == '{' && template.charAt(i + 1) == '}') {
				// Found {} placeholder
				if (argIndex < args.length) {
					result.append(resolveArg(args[argIndex++]));
				} else {
					result.append("{}");
				}
				i += 2;
			} else {
				result.append(template.charAt(i));
				i++;
			}
		}

		return result.toString();
	}

	private Object resolveArg(Object arg) {
		if (arg instanceof LazyArg<?> lazy) {
			return lazy.current();
		}
		if (arg instanceof Supplier<?> supplier) {
			return supplier.get();
		}
		return arg;
	}

	// Builder for fluent configuration
	
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private LogLevel threshold = LogLevel.INFO;
		private boolean showRecords = true;
		private boolean showThreadInfo = false;

		public Builder threshold(LogLevel level) {
			this.threshold = level;
			return this;
		}

		public Builder showRecords(boolean show) {
			this.showRecords = show;
			return this;
		}

		public Builder showThreadInfo(boolean show) {
			this.showThreadInfo = show;
			return this;
		}

		public Builder trace() {
			return threshold(LogLevel.TRACE);
		}

		public Builder debug() {
			return threshold(LogLevel.DEBUG);
		}

		public Builder info() {
			return threshold(LogLevel.INFO);
		}

		public Builder warn() {
			return threshold(LogLevel.WARN);
		}

		public Builder error() {
			return threshold(LogLevel.ERROR);
		}

		public StateTreeRenderer build() {
			return new StateTreeRenderer(threshold, showRecords, showThreadInfo);
		}

		public String render(StateHierarchyTree<?> root) {
			return build().render(root);
		}
	}
}
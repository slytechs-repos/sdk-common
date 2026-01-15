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

/**
 * Renders a {@link ComponentHierarchy} as an ASCII tree.
 * 
 * <p>
 * Example output:
 * </p>
 * <pre>
 * Lifecycle [name=PcapBackend, state=RUNNING→SHUTDOWN]
 * ├── Lifecycle [name=hello-capture, state=RUNNING→SHUTDOWN]
 * └── Lifecycle [name=hello-channel, state=DRAINING, components=2]
 *     ├── Lifecycle [name=worker-0, state=TERMINATED]
 *     └── Lifecycle [name=worker-1, state=TERMINATED]
 * </pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class TreeRenderer {

	private static final String BRANCH = "├── ";
	private static final String LAST_BRANCH = "└── ";
	private static final String VERTICAL = "│   ";
	private static final String SPACE = "    ";

	/**
	 * Renders a component hierarchy as an ASCII tree string.
	 *
	 * @param root the root of the hierarchy to render
	 * @return formatted ASCII tree
	 */
	public static String render(ComponentTree<?> root) {
		StringBuilder sb = new StringBuilder();
		renderNode(sb, root, "", true, true);
		return sb.toString();
	}

	private static void renderNode(StringBuilder sb, ComponentTree<?> node, 
	                                String prefix, boolean isLast, boolean isRoot) {
		// Render this node
		if (isRoot) {
			sb.append(formatNode(node)).append("\n");
		} else {
			sb.append(prefix);
			sb.append(isLast ? LAST_BRANCH : BRANCH);
			sb.append(formatNode(node)).append("\n");
		}

		// Calculate prefix for children
		String childPrefix;
		if (isRoot) {
			childPrefix = "";  // Root's children start with no indent
		} else {
			childPrefix = prefix + (isLast ? SPACE : VERTICAL);
		}

		// Render children
		var children = node.children();
		for (int i = 0; i < children.size(); i++) {
			boolean lastChild = (i == children.size() - 1);
			renderNode(sb, children.get(i), childPrefix, lastChild, false);
		}
	}

	private static String formatNode(ComponentTree<?> node) {
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

	private static String formatTypeName(StateMachine<?> machine) {
		String name = machine.getClass().getSimpleName();
		if (name.endsWith("StateMachine")) {
			return name.substring(0, name.length() - "StateMachine".length());
		}
		return name;
	}

	private static String formatState(StateMachine<?> machine) {
		var current = machine.currentState();
		var previous = machine.previousState();

		if (previous != null && previous != current) {
			return previous + "→" + current;
		}
		return String.valueOf(current);
	}
}
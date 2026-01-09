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

import java.util.List;

import com.slytechs.sdk.common.session.message.RenderMode;

/**
 * ASCII-art tree renderer for session hierarchies.
 * 
 * <p>
 * Produces output like:
 * <pre>
 * PcapBackend [name=pcap, state=RUNNING→SHUTDOWN]
 * ├── PcapCapture [name=hello-capture, state=RUNNING]
 * │   └── dispatch-loop: waiting for pcap_dispatch [2.3s]
 * ├── PacketChannel [name=hello-channel, state=DRAINING]
 * │   └── queue-drain: draining (15→3) packets [1.1s]
 * └── TaskScope [name=scope-1, state=TERMINATED]
 * </pre>
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
final class AsciiTreeRenderer implements TreeRenderer {

	static final AsciiTreeRenderer INSTANCE = new AsciiTreeRenderer();

	private static final String BRANCH = "├── ";
	private static final String LAST_BRANCH = "└── ";
	private static final String VERTICAL = "│   ";
	private static final String SPACE = "    ";

	private AsciiTreeRenderer() {
	}

	@Override
	public String render(ManagedSession root, RenderMode mode) {
		StringBuilder sb = new StringBuilder();
		renderNode(sb, root, "", mode);
		return sb.toString();
	}

	private void renderNode(StringBuilder sb, ManagedSession session, String prefix, RenderMode mode) {
		ManagedState state = session.managedState();

		sb.append(formatSession(session, mode));
		sb.append("\n");

		List<WaitInfo> waits = state.pendingWaits();
		List<ManagedSession> children = session.childSessions();

		int totalItems = waits.size() + children.size();
		int itemIndex = 0;

		for (WaitInfo wait : waits) {
			itemIndex++;
			boolean isLast = itemIndex == totalItems;
			String branch = isLast ? LAST_BRANCH : BRANCH;

			sb.append(prefix).append(branch);
			sb.append(wait.renderFull(mode));
			sb.append("\n");
		}

		for (ManagedSession child : children) {
			itemIndex++;
			boolean isLast = itemIndex == totalItems;
			String branch = isLast ? LAST_BRANCH : BRANCH;
			String continuation = isLast ? SPACE : VERTICAL;

			sb.append(prefix).append(branch);
			renderNode(sb, child, prefix + continuation, mode);
		}
	}

	private String formatSession(ManagedSession session, RenderMode mode) {
		ManagedState state = session.managedState();
		String className = session.getClass().getSimpleName();

		StringBuilder sb = new StringBuilder();
		sb.append(className);
		sb.append(" [name=").append(state.name());
		sb.append(", state=").append(state.stateString());

		int waitCount = state.pendingWaits().size();
		if (waitCount > 0) {
			sb.append(", waits=").append(waitCount);
		}

		int childCount = session.childSessions().size();
		if (childCount > 0) {
			sb.append(", children=").append(childCount);
		}

		sb.append("]");
		return sb.toString();
	}
}
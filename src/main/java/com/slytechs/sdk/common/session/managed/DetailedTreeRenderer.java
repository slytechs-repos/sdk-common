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
 * Detailed tree renderer that includes additional state information.
 * 
 * <p>
 * Produces more verbose output than {@link AsciiTreeRenderer}, including
 * boolean state flags and frozen status:
 * <pre>
 * PcapBackend [name=pcap, state=RUNNING→SHUTDOWN, running=false, shutdown=true, terminated=false]
 * ├── PcapCapture [name=hello-capture, state=RUNNING, running=true]
 * │   └── dispatch-loop: waiting for pcap_dispatch on port enp15s0 [waiting 2.3s]
 * ├── PacketChannel [name=hello-channel, state=DRAINING, frozen=false]
 * │   └── queue-drain: draining (15→3) packets from hello-channel [waiting 1.1s]
 * └── TaskScope [name=scope-1, state=TERMINATED, frozen=true]
 * </pre>
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
final class DetailedTreeRenderer implements TreeRenderer {

	static final DetailedTreeRenderer INSTANCE = new DetailedTreeRenderer();

	private static final String BRANCH = "├── ";
	private static final String LAST_BRANCH = "└── ";
	private static final String VERTICAL = "│   ";
	private static final String SPACE = "    ";

	private DetailedTreeRenderer() {
	}

	@Override
	public String render(ManagedSession root, RenderMode mode) {
		StringBuilder sb = new StringBuilder();
		renderNode(sb, root, "", mode);
		return sb.toString();
	}

	private void renderNode(StringBuilder sb, ManagedSession session, String prefix, RenderMode mode) {
		ManagedState state = session.managedState();

		sb.append(formatSessionDetailed(session, mode));
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
			sb.append(formatWaitDetailed(wait, mode));
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

	private String formatSessionDetailed(ManagedSession session, RenderMode mode) {
		ManagedState state = session.managedState();
		String className = session.getClass().getSimpleName();

		StringBuilder sb = new StringBuilder();
		sb.append(className);
		sb.append(" [name=").append(state.name());
		sb.append(", state=").append(state.stateString());
		sb.append(", running=").append(state.isRunning());
		sb.append(", shutdown=").append(state.isShutdown());
		sb.append(", terminated=").append(state.isTerminated());

		if (state.isFrozen()) {
			sb.append(", frozen=true");
		}

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

	private String formatWaitDetailed(WaitInfo wait, RenderMode mode) {
		StringBuilder sb = new StringBuilder();
		sb.append(wait.name()).append(": ");
		sb.append(wait.render(mode));
		sb.append(" [waiting ").append(formatDuration(wait.elapsed().toMillis())).append("]");

		if (wait.isFrozen()) {
			sb.append(" (frozen)");
		}

		return sb.toString();
	}

	private String formatDuration(long millis) {
		if (millis < 1000) {
			return millis + "ms";
		}
		double seconds = millis / 1000.0;
		if (seconds < 60) {
			return String.format("%.1fs", seconds);
		}
		long minutes = (long) (seconds / 60);
		long secs = (long) (seconds % 60);
		if (minutes < 60) {
			return String.format("%dm %ds", minutes, secs);
		}
		long hours = minutes / 60;
		minutes = minutes % 60;
		return String.format("%dh %dm", hours, minutes);
	}
}
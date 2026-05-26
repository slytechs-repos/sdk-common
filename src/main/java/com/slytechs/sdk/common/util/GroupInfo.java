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
package com.slytechs.sdk.common.util;

/**
 * Description of a contiguous range of globally unique integer identifiers
 * allocated as a group.
 *
 * <p>{@code GroupInfo} is a generic group descriptor used throughout the SDK
 * wherever resources are allocated in batches that share a common origin —
 * for example, channels created by
 * {@code NetRuntime.packetChannels(name, count)} or ports allocated as a
 * hardware queue group. The descriptor is shared by reference across all
 * members of the group: a 1024-channel group holds one {@code GroupInfo}
 * instance, not 1024.</p>
 *
 * <h2>Identity Spaces</h2>
 * <p>The {@code groupId} occupies a separate identity space from the member
 * IDs themselves. Group IDs are assigned from their own monotonically
 * incrementing counter and are independent of the IDs of the members within
 * the group. There is no general relationship of the form
 * {@code member.id() == member.groupInfo().groupId()} — these come from
 * different counters and the comparison is not meaningful.</p>
 *
 * <p>To recover the global ID of the first member of a group, use
 * {@link #groupOffset()}. The full ID range covered by the group is
 * {@code [groupOffset(), groupOffset() + groupSize())}.</p>
 *
 * <h2>Reuse and Equality</h2>
 * <p>Implementations typically allocate a single {@code GroupInfo} per group
 * and share the same instance across all members. Reference equality
 * ({@code ==}) is therefore a reliable test for "are these two members in the
 * same group?" Records also provide structural {@code equals} and
 * {@code hashCode}: two {@code GroupInfo} values with identical fields are
 * equal even if they are distinct instances. Since {@code groupId} is
 * globally unique, structural equality and reference equality coincide in
 * practice — but reference comparison is the cheaper and more direct check.</p>
 *
 * <h2>Usage</h2>
 *
 * {@snippet :
 * GroupInfo group = new GroupInfo(7, 64, 16);   // group 7, IDs 64..79
 *
 * int channelId = 70;
 * int local = group.groupIndex(channelId);      // returns 6
 *
 * boolean inGroup = (channelId >= group.groupOffset()
 *         && channelId < group.groupOffset() + group.groupSize());
 * }
 *
 * @param groupId     the globally unique identifier of this group, allocated
 *                    from the group identity counter at group creation
 * @param groupOffset the global ID of the first member of this group; member
 *                    IDs occupy the contiguous range
 *                    {@code [groupOffset, groupOffset + groupSize)}
 * @param groupSize   the number of members in this group; always positive
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public record GroupInfo(int groupId, int groupOffset, int groupSize) {

	/**
	 * Returns the local index of a member within this group.
	 *
	 * <p>The local index is the position of the member relative to the
	 * start of the group's ID range, computed as {@code id - groupOffset()}.
	 * For a group with {@code groupOffset = 64} and {@code groupSize = 16},
	 * a member with {@code id = 70} has local index {@code 6}.</p>
	 *
	 * <p>This method validates that {@code id} falls within the group's
	 * range and throws if it does not. Use this method in contexts where
	 * receiving an out-of-range ID indicates a programming error; for a
	 * silent bounds check, compare {@code id} against
	 * {@link #groupOffset()} and {@link #groupSize()} directly.</p>
	 *
	 * @param id the global ID of a member within this group
	 * @return the zero-based local index of the member within this group
	 * @throws IllegalArgumentException if {@code id} is outside the range
	 *                                  {@code [groupOffset, groupOffset + groupSize)}
	 */
	public int groupIndex(int id) {
		if (id < groupOffset() || id >= groupOffset() + groupSize())
			throw new IllegalArgumentException("id not part of this group " + this);

		return id - groupOffset();
	}
}
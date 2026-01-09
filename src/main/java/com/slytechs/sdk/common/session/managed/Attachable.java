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

import com.slytechs.sdk.common.util.Registration;

/**
 * Interface for sessions that can have children attached to them.
 * 
 * <p>
 * Attachable provides the mechanism for building session hierarchies where
 * parent sessions track their children. This enables:
 * <ul>
 * <li>Lifecycle coordination - parents wait for children to terminate</li>
 * <li>Tree visualization - rendering the complete session hierarchy</li>
 * <li>Event propagation - shutdown signals flow through the tree</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Example hierarchy:
 * <pre>
 * Net (Attachable)
 *  ├── Capture
 *  │   └── Channel
 *  └── TaskScope
 *      └── Task
 * </pre>
 * </p>
 *
 * @param <T> the type of child that can be attached
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public interface Attachable<T extends ManagedSession> {

	/**
	 * Attaches a child session to this parent.
	 * 
	 * <p>
	 * The child's state is registered with this parent, and the parent will
	 * wait for the child to terminate before completing its own termination.
	 * </p>
	 *
	 * @param child the child session to attach
	 * @return a registration that can be used to detach the child
	 * @throws IllegalStateException if this session is already terminated
	 * @throws IllegalArgumentException if the child is already attached to another parent
	 */
	Registration attachChild(T child);

	/**
	 * Detaches a child session from this parent.
	 * 
	 * <p>
	 * The child is removed from this parent's children list. This does not
	 * affect the child's lifecycle - it continues running independently.
	 * </p>
	 *
	 * @param child the child session to detach
	 */
	void detachChild(T child);

	/**
	 * Checks if the given session is attached as a child.
	 *
	 * @param child the session to check
	 * @return true if the session is attached to this parent
	 */
	boolean hasChild(T child);

	/**
	 * Returns the number of attached children.
	 *
	 * @return the child count
	 */
	int childCount();
}
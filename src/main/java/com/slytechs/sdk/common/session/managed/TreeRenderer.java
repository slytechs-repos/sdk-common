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

import com.slytechs.sdk.common.session.message.RenderMode;

/**
 * Interface for rendering session trees as text output.
 * 
 * <p>
 * TreeRenderer implementations transform a {@link ManagedSession} hierarchy
 * into human-readable text for debugging and diagnostics.
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public interface TreeRenderer {

	/**
	 * Renders a session tree with the default render mode (TRANSITION).
	 *
	 * @param root the root session to render
	 * @return the rendered tree string
	 */
	default String render(ManagedSession root) {
		return render(root, RenderMode.TRANSITION);
	}

	/**
	 * Renders a session tree with the specified render mode.
	 *
	 * @param root the root session to render
	 * @param mode the render mode for lazy arguments
	 * @return the rendered tree string
	 */
	String render(ManagedSession root, RenderMode mode);

	/**
	 * Returns the default ASCII tree renderer.
	 *
	 * @return an ASCII tree renderer instance
	 */
	static TreeRenderer ascii() {
		return AsciiTreeRenderer.INSTANCE;
	}

	/**
	 * Returns a detailed tree renderer with additional state information.
	 *
	 * @return a detailed tree renderer instance
	 */
	static TreeRenderer detailed() {
		return DetailedTreeRenderer.INSTANCE;
	}
}
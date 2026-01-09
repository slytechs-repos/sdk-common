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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TreeRenderer} factory methods.
 */
class TreeRendererTest {

	@Test
	void asciiReturnsRenderer() {
		TreeRenderer renderer = TreeRenderer.ascii();
		assertNotNull(renderer);
	}

	@Test
	void detailedReturnsRenderer() {
		TreeRenderer renderer = TreeRenderer.detailed();
		assertNotNull(renderer);
	}

	@Test
	void asciiReturnsSameInstance() {
		TreeRenderer r1 = TreeRenderer.ascii();
		TreeRenderer r2 = TreeRenderer.ascii();
		assertSame(r1, r2);
	}

	@Test
	void detailedReturnsSameInstance() {
		TreeRenderer r1 = TreeRenderer.detailed();
		TreeRenderer r2 = TreeRenderer.detailed();
		assertSame(r1, r2);
	}

	@Test
	void asciiAndDetailedAreDifferent() {
		TreeRenderer ascii = TreeRenderer.ascii();
		TreeRenderer detailed = TreeRenderer.detailed();
		assertNotSame(ascii, detailed);
	}
}
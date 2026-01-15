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
package com.slytechs.sdk.common.session.message;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.slytechs.sdk.common.session.state.RenderMode;

/**
 * Unit tests for {@link RenderMode} enum.
 */
class RenderModeTest {

	@Test
	void allValuesExist() {
		assertEquals(3, RenderMode.values().length);
		assertNotNull(RenderMode.CURRENT);
		assertNotNull(RenderMode.SNAPSHOT);
		assertNotNull(RenderMode.TRANSITION);
	}

	@Test
	void valueOfWorks() {
		assertEquals(RenderMode.CURRENT, RenderMode.valueOf("CURRENT"));
		assertEquals(RenderMode.SNAPSHOT, RenderMode.valueOf("SNAPSHOT"));
		assertEquals(RenderMode.TRANSITION, RenderMode.valueOf("TRANSITION"));
	}

	@Test
	void ordinalOrder() {
		assertEquals(0, RenderMode.CURRENT.ordinal());
		assertEquals(1, RenderMode.SNAPSHOT.ordinal());
		assertEquals(2, RenderMode.TRANSITION.ordinal());
	}
}
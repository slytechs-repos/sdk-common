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

import com.slytechs.sdk.common.text.LazyArg.Mode;

/**
 * Unit tests for {@link Mode} enum.
 */
class RenderModeTest {

	@Test
	void allValuesExist() {
		assertEquals(3, Mode.values().length);
		assertNotNull(Mode.CURRENT);
		assertNotNull(Mode.SNAPSHOT);
		assertNotNull(Mode.TRANSITION);
	}

	@Test
	void valueOfWorks() {
		assertEquals(Mode.CURRENT, Mode.valueOf("CURRENT"));
		assertEquals(Mode.SNAPSHOT, Mode.valueOf("SNAPSHOT"));
		assertEquals(Mode.TRANSITION, Mode.valueOf("TRANSITION"));
	}

	@Test
	void ordinalOrder() {
		assertEquals(0, Mode.CURRENT.ordinal());
		assertEquals(1, Mode.SNAPSHOT.ordinal());
		assertEquals(2, Mode.TRANSITION.ordinal());
	}
}
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
package com.slytechs.sdk.common.session.state.recorder;

import com.slytechs.sdk.common.session.state.State;
import com.slytechs.sdk.common.session.state.StateMachine;

/**
 * 
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public record StateSnapshot<T extends Enum<T> & State<T>>(
		String name,
		long generationId,
		State<T> initialState,
		State<T> previousState,
		State<T> currentState,
		Class<?> classInfo) {

	public StateSnapshot(StateMachine<T> state) {
		this(state.name(), state.generationId(),
				state.initialState(),
				state.previousState(),
				state.currentState(),
				state.getClass());
	}

}

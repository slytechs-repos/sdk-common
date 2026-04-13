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
 * An asynchronous update interface for receiving notifications about Transition
 * information as the Transition is constructed.
 *
 * @param <T> the generic type
 */
public interface TransitionObserver<T extends Enum<T> & State<T>> {

	/**
	 * This method is called when information about an Transition which was
	 * previously requested using an asynchronous interface becomes available.
	 *
	 * @param source   the source
	 * @param oldState the old state
	 * @param newState the new state
	 */
	void onStateTransition(StateMachine<T> source, T oldState, T newState);

}
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
 * Base interface for state enum values that participate in state machine transitions.
 * 
 * <p>
 * Each state defines its transition rules via {@link #canTransistion(State)} and
 * provides tense helpers for logging and error messages.
 * </p>
 *
 * @param <T> the self-referential state type
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public interface State<T extends State<T>> {

	/**
	 * Checks if a transition from this state to the new state is allowed.
	 *
	 * @param newState the target state
	 * @return true if transition is allowed
	 */
	boolean canTransistion(T newState);

	/**
	 * Returns the present tense form of this state for logging.
	 * Example: TERMINATING returns "terminating"
	 *
	 * @return present tense string
	 */
	default String presentTense() {
		return toString().toLowerCase();
	}

	/**
	 * Returns the future tense form of this state for logging.
	 * Example: TERMINATED returns "termination"
	 *
	 * @return future tense string
	 */
	default String futureTense() {
		return toString().toLowerCase();
	}

	/**
	 * Returns the past tense form of this state for logging.
	 * Example: TERMINATED returns "terminated"
	 *
	 * @return past tense string
	 */
	default String pastTense() {
		return toString().toLowerCase();
	}
}
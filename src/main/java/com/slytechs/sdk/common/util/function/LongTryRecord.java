/*
 * Sly Technologies Free License
 * 
 * Copyright 2024 Sly Technologies Inc.
 *
 * Licensed under the Sly Technologies Free License (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.slytechs.com/free-license-text
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.slytechs.sdk.common.util.function;

/**
 * Record implementation of LongTry that stores either an long value or failure
 * exception.
 *
 * @param value   the value
 * @param failure the failure
 */
public record LongTryRecord(long value, @Nullable Exception failure) implements LongTry {

	/**
	 * Constructs an LongTry instance, ensuring either value is valid or failure is
	 * present.
	 *
	 * @param value   the value
	 * @param failure the failure
	 * @throws IllegalArgumentException if failure is null when isSuccess is false
	 */
	public LongTryRecord {
		if (!isSuccess() && failure == null) {
			throw new IllegalArgumentException("Failure case requires non-null exception");
		}
	}

	/**
	 * @see com.slytechs.sdk.common.util.function.LongTry#isSuccess()
	 */
	@Override
	public boolean isSuccess() {
		return failure == null;
	}

	/**
	 * @see java.lang.Record#toString()
	 */
	@Override
	public String toString() {
		return isSuccess()
				? "LongSuccess[" + value + "]"
				: "LongFailure[" + failure + "]";
	}
}
/*
 * Sly Technologies Free License
 * 
 * Copyright 2025 Sly Technologies Inc.
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
package com.slytechs.jnet.core.api.util.function;

/**
 * Record implementation of IntTry that stores either an int value or failure
 * exception.
 */
public record IntTryRecord(int value, @Nullable Exception failure) implements IntTry {

	/**
	 * Constructs an IntTry instance, ensuring either value is valid or failure is
	 * present.
	 *
	 * @throws IllegalArgumentException if failure is null when isSuccess is false
	 */
	public IntTryRecord {
		if (!isSuccess() && failure == null) {
			throw new IllegalArgumentException("Failure case requires non-null exception");
		}
	}

	@Override
	public boolean isSuccess() {
		return failure == null;
	}

	@Override
	public String toString() {
		return isSuccess()
				? "IntSuccess[" + value + "]"
				: "IntFailure[" + failure + "]";
	}
}
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
 * Record implementation of Try that stores a success value.
 */
record TrySuccessRecord<T>(@Nullable T success) implements Try<T> {

	@Override
	public String toString() {
		return "Success[" + String.valueOf(success) + "]";
	}

	/**
	 * @see com.slytechs.jnet.platform.api.util.function.Try#isFailure()
	 */
	@Override
	public boolean isFailure() {
		return false;
	}

	/**
	 * @see com.slytechs.jnet.platform.api.util.function.Try#isSuccess()
	 */
	@Override
	public boolean isSuccess() {
		return true;
	}

	/**
	 * @see com.slytechs.jnet.platform.api.util.function.Try#failure()
	 */
	@Override
	public @Nullable Exception failure() {
		return null;
	}

	/**
	 * @see com.slytechs.jnet.platform.api.util.function.Try#get()
	 */
	@Override
	public T get() throws Exception {
		return success;
	}
}
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
 * Record implementation of Try that stores a failure exception.
 */
record TryFailureRecord<T>(Exception failure) implements Try<T> {

	/**
	 * @see com.slytechs.jnet.platform.api.util.function.Try#isFailure()
	 */
	@Override
	public boolean isFailure() {
		return true;
	}

	/**
	 * @see com.slytechs.jnet.platform.api.util.function.Try#isSuccess()
	 */
	@Override
	public boolean isSuccess() {
		return false;
	}

	@Override
	public String toString() {
		return "Failure[" + failure + "]";
	}

	/**
	 * @see com.slytechs.jnet.platform.api.util.function.Try#success()
	 */
	@Override
	public @Nullable T success() {
		return null;
	}

	/**
	 * @see com.slytechs.jnet.platform.api.util.function.Try#get()
	 */
	@Override
	public T get() throws Exception {
		throw failure;
	}
}
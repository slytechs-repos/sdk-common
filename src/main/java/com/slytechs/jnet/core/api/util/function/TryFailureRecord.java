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
package com.slytechs.jnet.core.api.util.function;

/**
 * Record implementation of Try that stores a failure exception.
 *
 * @param <T>     the generic type
 * @param failure the failure
 */
record TryFailureRecord<T>(Exception failure) implements Try<T> {

	/**
	 * Checks if is failure.
	 *
	 * @return true, if is failure
	 * @see com.slytechs.jnet.platform.api.util.function.Try#isFailure()
	 */
	@Override
	public boolean isFailure() {
		return true;
	}

	/**
	 * Checks if is success.
	 *
	 * @return true, if is success
	 * @see com.slytechs.jnet.platform.api.util.function.Try#isSuccess()
	 */
	@Override
	public boolean isSuccess() {
		return false;
	}

	/**
	 * @see java.lang.Record#toString()
	 */
	@Override
	public String toString() {
		return "Failure[" + failure + "]";
	}

	/**
	 * Success.
	 *
	 * @return the t
	 * @see com.slytechs.jnet.platform.api.util.function.Try#success()
	 */
	@Override
	public @Nullable T success() {
		return null;
	}

	/**
	 * Gets the.
	 *
	 * @return the t
	 * @throws Exception the exception
	 * @see com.slytechs.jnet.platform.api.util.function.Try#get()
	 */
	@Override
	public T get() throws Exception {
		throw failure;
	}
}
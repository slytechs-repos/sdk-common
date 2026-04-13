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

import java.util.function.Consumer;

/**
 * A runnable that can throw checked exceptions.
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
@FunctionalInterface
public interface ThrowingRunnable {

	/**
	 * Drops a throwing runnable into a safe runnable with custom error handling.
	 *
	 * @param r            the runnable that may throw
	 * @param errorHandler handles any exceptions
	 * @return a runnable that safely handles exceptions
	 */
	static Runnable drop(ThrowingRunnable r, Consumer<Exception> errorHandler) {
		return () -> {
			try {
				r.run();
			} catch (Exception e) {
				errorHandler.accept(e);
			}
		};
	}

	/**
	 * Of.
	 *
	 * @param runnable the runnable
	 * @return the throwing runnable
	 */
	static ThrowingRunnable of(ThrowingRunnable runnable) {
		return runnable::run;
	}

	/**
	 * Lift.
	 *
	 * @param runnable the runnable
	 * @return the throwing runnable
	 */
	static ThrowingRunnable lift(Runnable runnable) {
		return runnable::run;
	}

	/**
	 * Performs this operation.
	 *
	 * @throws Exception if unable to perform the operation
	 */
	void run() throws Exception;
}
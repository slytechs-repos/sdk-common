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

import java.util.function.Function;

/**
 * A function that can throw checked exceptions.
 *
 * @param <T> the type of the input
 * @param <R> the type of the result
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
@FunctionalInterface
public interface ThrowingFunction<T, R> {

	/**
	 * Lift.
	 *
	 * @param <T>      the generic type
	 * @param <R>      the generic type
	 * @param function the function
	 * @return the function
	 */
	static <T, R> Function<T, R> lift(ThrowingFunction<T, R> function) {
		return t -> {
			try {
				return function.apply(t);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
	}

	/**
	 * Of.
	 *
	 * @param <T>      the generic type
	 * @param <R>      the generic type
	 * @param function the function
	 * @return the throwing function
	 */
	static <T, R> ThrowingFunction<T, R> of(ThrowingFunction<T, R> function) {
		return function;
	}

	/**
	 * Applies this function to the given argument.
	 *
	 * @param t the function argument
	 * @return the function result
	 * @throws Exception if unable to compute the function
	 */
	R apply(T t) throws Exception;
}
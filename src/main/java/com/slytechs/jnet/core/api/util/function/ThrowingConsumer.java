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

import java.util.function.Consumer;

/**
 * A consumer that can throw checked exceptions.
 *
 * @param <T> the type of the input
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
@FunctionalInterface
public interface ThrowingConsumer<T> {

	/**
	 * Drops a throwing consumer into a safe consumer that converts all exceptions
	 * to runtime.
	 *
	 * @param <T> the input type
	 * @param c   the consumer that may throw
	 * @return a safe consumer
	 */
	static <T> Consumer<T> lift(ThrowingConsumer<T> consumer) {
		return t -> {
			try {
				consumer.accept(t);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
	}

	static <T> ThrowingConsumer<T> of(ThrowingConsumer<T> consumer) {
		return consumer;
	}

	/**
	 * Performs this operation on the given argument.
	 *
	 * @param t the input argument
	 * @throws Exception if unable to process the input
	 */
	void accept(T t) throws Exception;
}
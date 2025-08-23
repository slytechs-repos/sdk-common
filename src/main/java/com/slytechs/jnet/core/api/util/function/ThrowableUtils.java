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
 * The Class ThrowableUtils.
 *
 * @author Mark Bednarczyk
 */
final class ThrowableUtils {

	/**
	 * Cast as checked or throw runtime.
	 *
	 * @param <E>          the element type
	 * @param e            the e
	 * @param checkedClass the checked class
	 * @return the e
	 */
	@SuppressWarnings("unchecked")
	public static <E extends Throwable> E castAsCheckedOrThrowRuntime(Throwable e, Class<E> checkedClass) {
		if (e.getClass().isAssignableFrom(checkedClass))
			return (E) e;

		throw new RuntimeException(e);
	}

	/**
	 * Unwrap exception.
	 *
	 * @param e the e
	 * @return the exception
	 */
	public static Exception unwrapException(RuntimeException e) {
		Throwable r = e;

		while (r.getCause() != null)
			r = r.getCause();

		if (r instanceof RuntimeException re)
			throw re;

		return (Exception) r;
	}

	/**
	 * Unwrap exception or throw runtime.
	 *
	 * @param e the e
	 * @return the exception
	 */
	public static Exception unwrapExceptionOrThrowRuntime(RuntimeException e) {
		if (e.getCause() == null)
			throw e;

		if (e.getCause() instanceof RuntimeException re)
			throw re;

		return (Exception) e.getCause();
	}

	/**
	 * Unwrap checked or throw runtime.
	 *
	 * @param <E>          the element type
	 * @param e            the e
	 * @param checkedClass the checked class
	 * @return the e
	 */
	public static <E extends Throwable> E unwrapCheckedOrThrowRuntime(RuntimeException e, Class<E> checkedClass) {
		if (e.getCause() == null)
			throw e;

		if (e.getCause() instanceof RuntimeException re)
			throw re;

		return castAsCheckedOrThrowRuntime(e.getCause(), checkedClass);
	}

	/**
	 * Instantiates a new throwable utils.
	 */
	private ThrowableUtils() {
	}

}

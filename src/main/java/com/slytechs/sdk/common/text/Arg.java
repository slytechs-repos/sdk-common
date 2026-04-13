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
package com.slytechs.sdk.common.text;

/**
 * Functional interface for argument getters with variations.
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public interface Arg {

	static <T> Get<T> get(Get<T> get) {
		return get;
	}

	static <T> Fmt<T> fmt(Fmt<T> fmt) {
		return fmt;
	}

	static <T> Str<T> str(Str<T> str) {
		return str;
	}

	interface Get<T> extends Arg {
		Object get(T target);

		@SuppressWarnings("unchecked")
		@Override
		default Object value(Object target) {
			return get((T) target);
		}
	}

	interface Get2<T1, T2> extends Arg {
		Object get(T1 arg1, T2 arg2);

		@Override
		default Object value(Object target) {
			throw new UnsupportedOperationException("requires 2 arguments");
		}
	}

	interface Fmt<T> extends Arg {
		String format(T target, String format, Arg... args);

		@SuppressWarnings("unchecked")
		@Override
		default Object value(Object target) {
			return format((T) target, "%s");
		}
	}

	interface Str<T> extends Arg {
		String toString(T target);

		@SuppressWarnings("unchecked")
		@Override
		default Object value(Object target) {
			return toString((T) target);
		}

	}

	static Object[] resolve(Object target, Arg[] args) {
		Object[] arr = new Object[args.length];

		int i = 0;
		for (Arg arg : args)
			arr[i++] = arg.value(target);

		return arr;
	}

	static Object[] resolve(Object target, Arg arg, Arg[] args) {
		Object[] arr = new Object[args.length + 1];

		int i = 0;
		arr[i++] = arg.value(target);

		for (Arg a : args)
			arr[i++] = a.value(target);

		return arr;
	}

	Object value(Object target);

	default Object value() {
		return value(null);
	}

}
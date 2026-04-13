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
package com.slytechs.sdk.common.text.format;

/**
 * Holds the primary value for format operations. Wraps any value type.
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
final class FormatValue {

	private Object value;

	FormatValue() {
	}

	FormatValue set(Object value) {
		this.value = value;
		return this;
	}

	Object raw() {
		return value;
	}

	long asLong() {
		if (value instanceof Number n) return n.longValue();
		return 0L;
	}

	boolean isNumber() {
		return value instanceof Number;
	}

	Object masked(long mask) {
		if (value instanceof Number n) {
			long val = n.longValue() & mask;
			if (mask != 0)
				val >>>= Long.numberOfTrailingZeros(mask);
			return val;
		}
		return value;
	}

	String invokeMacro(Macro macro, long mask) {
		if (value == null)
			return "null";
		return macro.accept(value, mask);
	}
}
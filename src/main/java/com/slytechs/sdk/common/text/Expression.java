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
 * A single value transform operation parsed from format string placeholders.
 * Expressions are compiled at parse time and applied at format time.
 *
 * {@snippet :
 * // These format placeholders produce expressions:
 * "{>>}"       // auto-shift right by mask trailing zeros
 * "{>> 4}"     // shift right by 4
 * "{<< 2}"     // shift left by 2
 * "{* 4}"      // multiply by 4
 * "{& 0xFF}"   // AND mask
 * "{~}"        // bitwise invert
 * "{>> 4:0x%X}" // shift right by 4, then format as hex
 * }
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
@FunctionalInterface
public interface Expression {

	long evaluate(long value, long mask);

	// @formatter:off
	Expression IDENTITY   = (v, m) -> v;
	Expression INVERT     = (v, m) -> ~v;
	Expression AUTO_SHIFT = (v, m) -> (m != 0 && m != ~0L)
			? (v & m) >>> Long.numberOfTrailingZeros(m)
			: v & m;
	// @formatter:on

	static Expression shiftRight(int n) {
		return (v, m) -> (v & m) >>> n;
	}

	static Expression shiftLeft(int n) {
		return (v, m) -> (v & m) << n;
	}

	static Expression multiply(long n) {
		return (v, m) -> (v & m) * n;
	}

	static Expression and(long n) {
		return (v, m) -> v & n;
	}

	static Expression chain(Expression first, Expression second) {
		return (v, m) -> {
			long intermediate = first.evaluate(v, m);
			return second.evaluate(intermediate, ~0L); // no mask on chained op
		};
	}
}
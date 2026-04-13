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
 * Compiled segments of a format string. Each segment knows how to render itself
 * given a value, mask, and arguments. Pure formatting — no expression
 * evaluation. Shared between {@link TextFormat} and {@link BitFormat}.
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
sealed interface FormatSegment
		permits FormatSegment.Literal,
		FormatSegment.ValueRef,
		FormatSegment.ArgRef,
		FormatSegment.MacroRef,
		FormatSegment.BitField,
		FormatSegment.AutoArg,
		FormatSegment.ExprRef {

	long render(StringBuilder sb, FormatValue value, long mask, Object[] args);

	record Literal(String text) implements FormatSegment {
		@Override
		public long render(StringBuilder sb, FormatValue value, long mask, Object[] args) {
			sb.append(text);
			return mask;
		}
	}

	record ValueRef(String format) implements FormatSegment {
		@Override
		public long render(StringBuilder sb, FormatValue value, long mask, Object[] args) {
			Object val = value.masked(mask);

			if (format == null) {
				sb.append(val);
			} else {
				Object[] fmtArgs = new Object[args.length + 1];
				fmtArgs[0] = val;
				System.arraycopy(args, 0, fmtArgs, 1, args.length);
				sb.append(String.format(format, fmtArgs));
			}
			return mask;
		}
	}

	record ArgRef(int index, String format) implements FormatSegment {
		@Override
		public long render(StringBuilder sb, FormatValue value, long mask, Object[] args) {
			Object arg = args[index - 1];

			if (format == null)
				sb.append(arg);
			else
				sb.append(String.format(format, arg));
			return mask;
		}
	}

	record AutoArg(int index, String format) implements FormatSegment {
		@Override
		public long render(StringBuilder sb, FormatValue value, long mask, Object[] args) {
			Object arg = args[index];

			if (format == null)
				sb.append(arg);
			else
				sb.append(String.format(format, arg));
			return mask;
		}
	}

	record MacroRef(Macro macro, int argIndex) implements FormatSegment {
		MacroRef(Macro macro) {
			this(macro, -1);
		}

		@Override
		public long render(StringBuilder sb, FormatValue value, long mask, Object[] args) {
			if (argIndex >= 0) {
				sb.append(macro.accept(args[argIndex - 1], mask));
			} else {
				sb.append(value.invokeMacro(macro, mask));
			}
			return mask;
		}
	}

	record BitField(
			long mask,
			int bitCount,
			BitPos[] positions,
			char onChar,
			char offChar,
			char maskedChar,
			boolean hidden,
			boolean inverted) implements FormatSegment {

		@Override
		public long render(StringBuilder sb, FormatValue value, long mask, Object[] args) {
			long effectiveMask = inverted ? ~this.mask : this.mask;

			if (!hidden) {
				long val = value.asLong();
				for (BitPos pos : positions) {
					switch (pos.type()) {
					case BIT -> {
						boolean isActive = (effectiveMask & pos.bitMask()) != 0;
						if (!isActive) {
							sb.append(maskedChar);
						} else {
							boolean isOn = (val & pos.bitMask()) != 0;
							char pch = pos.patternChar();
							boolean isCustom = (pch != onChar && pch != offChar && pch != maskedChar
									&& pch != '0' && pch != '1' && pch != '.');
							sb.append(isOn ? (isCustom ? pch : onChar) : offChar);
						}
					}
					case SPACE -> sb.append(' ');
					case SKIP -> {}
					case ALIGN -> sb.append(' ');
					}
				}
			}

			return effectiveMask;
		}
	}

	enum ExprOp {
		SHIFT_RIGHT,          // {>>} or {>>:fmt}
		SHIFT_RIGHT_MULTIPLY, // {>> * N} or {>> * N:fmt}
		SHIFT_LEFT,           // {<< N} or {<< N:fmt}
		MULTIPLY,             // {* N} or {* N:fmt}
		AND,                  // {& N} or {& N:fmt}
		COMPLEMENT,           // {~} or {~:fmt}
	}

	record ExprRef(ExprOp op, long operand, String format) implements FormatSegment {
		@Override
		public long render(StringBuilder sb, FormatValue value, long mask, Object[] args) {
			long raw = value.asLong();
			long masked = (mask != 0) ? (raw & mask) >>> Long.numberOfTrailingZeros(mask) : raw;

			long result = switch (op) {
			case SHIFT_RIGHT -> masked;
			case SHIFT_RIGHT_MULTIPLY -> masked * operand;
			case SHIFT_LEFT -> masked << operand;
			case MULTIPLY -> masked * operand;
			case AND -> raw & operand;
			case COMPLEMENT -> ~raw;
			};

			if (format == null)
				sb.append(result);
			else
				sb.append(String.format(format, result));

			return mask;
		}
	}

	record BitPos(BitPosType type, long bitMask, char patternChar) {
		BitPos(BitPosType type) {
			this(type, 0L, '\0');
		}
	}

	enum BitPosType {
		BIT,
		SPACE,
		SKIP,
		ALIGN
	}
}
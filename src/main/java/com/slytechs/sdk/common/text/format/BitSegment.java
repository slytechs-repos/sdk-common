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
 * Compiled segments of a bit format string. Each segment knows how to render
 * itself given a value, mask, and arguments.
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
sealed interface BitSegment {

	/**
	 * Renders this segment into the string builder.
	 *
	 * @param sb    the output builder
	 * @param value the primary value being formatted
	 * @param mask  the current line mask (may be updated by BitField segments)
	 * @param args  resolved arguments
	 * @return the active mask after this segment (only BitField changes it)
	 */
	long render(StringBuilder sb, long value, long mask, Object[] args);

	/**
	 * Literal text copied as-is to output.
	 */
	record Literal(String text) implements BitSegment {
		@Override
		public long render(StringBuilder sb, long value, long mask, Object[] args) {
			sb.append(text);
			return mask;
		}
	}

	/**
	 * Value reference: {@code {}} or {@code {:fmt}}. Formats the masked value using
	 * String.valueOf or String.format.
	 */
	record ValueRef(String format, boolean shifted) implements BitSegment {
		ValueRef(String format) {
			this(format, false);
		}

		@Override
		public long render(StringBuilder sb, long value, long mask, Object[] args) {
			long masked = value & mask;
			if (shifted && mask != 0 && mask != ~0L)
				masked = masked >>> Long.numberOfTrailingZeros(mask);

			if (format == null) {
				sb.append(masked);
			} else {
				Object[] fmtArgs = new Object[args.length + 1];
				fmtArgs[0] = masked;
				System.arraycopy(args, 0, fmtArgs, 1, args.length);
				sb.append(String.format(format, fmtArgs));
			}
			return mask;
		}
	}

	/**
	 * Argument reference: {@code {N}} or {@code {N:fmt}}. Formats the Nth argument
	 * (1-based index).
	 */
	record ArgRef(int index, String format) implements BitSegment {
		@Override
		public long render(StringBuilder sb, long value, long mask, Object[] args) {
			Object arg = args[index - 1];
			if (format == null) {
				sb.append(arg);
			} else {
				sb.append(String.format(format, arg));
			}
			return mask;
		}
	}

	/**
	 * Macro reference: {@code {@name}} or {@code {N:@name}}. Invokes the macro with
	 * the masked value (or Nth arg) and current line mask.
	 */
	record MacroRef(Macro macro, int argIndex) implements BitSegment {
		/**
		 * Creates a macro ref that uses the primary value.
		 */
		MacroRef(Macro macro) {
			this(macro, -1);
		}

		@Override
		public long render(StringBuilder sb, long value, long mask, Object[] args) {
			long v = (argIndex < 0) ? (value & mask) : ((Number) args[argIndex - 1]).longValue();
			sb.append(macro.accept(v, mask));
			return mask;
		}
	}

	/**
	 * Compiled bit field: {@code {/.... 1.../}}. Holds the computed mask, bit
	 * count, and per-position render info. Establishes a new line mask for
	 * subsequent segments.
	 */
	record BitField(
			long mask,
			int bitCount,
			BitPos[] positions,
			char onChar,
			char offChar,
			char maskedChar,
			boolean hidden,
			boolean inverted) implements BitSegment {

		@Override
		public long render(StringBuilder sb, long value, long mask, Object[] args) {
			long effectiveMask = inverted ? ~this.mask : this.mask;

			if (!hidden) {
				long val = value;
				for (BitPos pos : positions) {
					switch (pos.type()) {
					case BIT -> {
						boolean isActive = (effectiveMask & pos.bitMask()) != 0;
						if (!isActive) {
							sb.append(maskedChar);
						} else {
							boolean isOn = (val & pos.bitMask()) != 0;
							char pch = pos.patternChar();
							// If pattern char is a custom char (not default on/off/masked)
							// use it as the "on" display char for this position
							boolean isCustom = (pch != onChar && pch != offChar && pch != maskedChar
									&& pch != '0' && pch != '1' && pch != '.');
							if (isOn) {
								sb.append(isCustom ? pch : onChar);
							} else {
								sb.append(offChar);
							}
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

	/**
	 * Position info for a single character in a bit field pattern.
	 */
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
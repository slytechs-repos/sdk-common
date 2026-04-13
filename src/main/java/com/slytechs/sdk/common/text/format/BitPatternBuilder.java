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
 * Generates bit format pattern strings from field bit offset and length. Scopes
 * the pattern to the minimum byte-aligned range containing the field, matching
 * Wireshark's display convention.
 *
 * {@snippet :
 * // IPv4 Version (bits 0-3 within byte 0)
 * BitPatternBuilder.pattern(0, 4)    // → "{/1111 ..../}"
 *
 * // IPv4 IHL (bits 4-7 within byte 0)
 * BitPatternBuilder.pattern(4, 4)    // → "{/.... 1111/}"
 *
 * // TCP Don't Fragment (bit 49 within bytes 6-7)
 * BitPatternBuilder.pattern(49, 1)   // → "{/.1.. .... .... ..../}"
 *
 * // Fragment Offset (bits 51-63 within bytes 6-7)
 * BitPatternBuilder.pattern(51, 13)  // → "{/...1 1111 1111 1111/}"
 * }
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public final class BitPatternBuilder {

	private static final int BITS_PER_BYTE = 8;
	private static final int NIBBLE = 4;

	/**
	 * Generates a bit format pattern for a field at the given bit position.
	 * The pattern is scoped to the minimum byte-aligned range containing the
	 * field, with spaces inserted every 4 bits for readability.
	 *
	 * @param bitOffset bit offset from the start of the header
	 * @param bitLength number of bits in the field
	 * @return the pattern string, e.g. {@code "{/1111 ..../}"}
	 */
	public static String pattern(long bitOffset, long bitLength) {
		return pattern(bitOffset, bitLength, '1', '.');
	}

	/**
	 * Generates a bit format pattern with custom on/masked characters.
	 *
	 * @param bitOffset  bit offset from the start of the header
	 * @param bitLength  number of bits in the field
	 * @param onChar     character for selected bits
	 * @param maskedChar character for non-selected bits
	 * @return the pattern string
	 */
	public static String pattern(long bitOffset, long bitLength, char onChar, char maskedChar) {
		int startByte = (int) (bitOffset / BITS_PER_BYTE);
		int endByte = (int) ((bitOffset + bitLength - 1) / BITS_PER_BYTE);
		int scopeBits = (endByte - startByte + 1) * BITS_PER_BYTE;
		int localStart = (int) (bitOffset - (startByte * BITS_PER_BYTE));

		return buildPattern(scopeBits, localStart, (int) bitLength, onChar, maskedChar);
	}

	/**
	 * Generates a bit format pattern scoped to a parent field range. Use this
	 * for sub-fields within a composite (e.g., individual flags within a 16-bit
	 * flags field).
	 *
	 * @param scopeOffset bit offset of the parent scope (byte-aligned)
	 * @param scopeLength bit length of the parent scope
	 * @param bitOffset   bit offset of the sub-field (absolute)
	 * @param bitLength   number of bits in the sub-field
	 * @return the pattern string scoped to the parent
	 */
	public static String patternScoped(long scopeOffset, long scopeLength,
			long bitOffset, long bitLength) {
		return patternScoped(scopeOffset, scopeLength, bitOffset, bitLength, '1', '.');
	}

	/**
	 * Generates a scoped bit format pattern with custom characters.
	 *
	 * @param scopeOffset bit offset of the parent scope
	 * @param scopeLength bit length of the parent scope
	 * @param bitOffset   bit offset of the sub-field (absolute)
	 * @param bitLength   number of bits in the sub-field
	 * @param onChar      character for selected bits
	 * @param maskedChar  character for non-selected bits
	 * @return the pattern string
	 */
	public static String patternScoped(long scopeOffset, long scopeLength,
			long bitOffset, long bitLength, char onChar, char maskedChar) {
		int localStart = (int) (bitOffset - scopeOffset);

		return buildPattern((int) scopeLength, localStart, (int) bitLength, onChar, maskedChar);
	}

	private static String buildPattern(int scopeBits, int localStart, int bitLength,
			char onChar, char maskedChar) {
		int localEnd = localStart + bitLength;

		StringBuilder sb = new StringBuilder();
		sb.append("{/");

		for (int i = 0; i < scopeBits; i++) {
			if (i > 0 && (i % NIBBLE) == 0)
				sb.append(' ');

			sb.append((i >= localStart && i < localEnd) ? onChar : maskedChar);
		}

		sb.append("/}");
		return sb.toString();
	}

	/**
	 * Generates a complete bit format line with label and value, matching
	 * Wireshark's display style.
	 *
	 * @param bitOffset bit offset from the start of the header
	 * @param bitLength number of bits in the field
	 * @param label     the display label for the field
	 * @return format string like {@code "{/1111 ..../} = Version: {>>}"}
	 */
	public static String fieldLine(long bitOffset, long bitLength, String label) {
		return pattern(bitOffset, bitLength) + " = " + label + ": {>>}";
	}

	/**
	 * Generates a scoped bit format line within a parent scope.
	 *
	 * @param scopeOffset bit offset of the parent scope
	 * @param scopeLength bit length of the parent scope
	 * @param bitOffset   bit offset of the sub-field (absolute)
	 * @param bitLength   number of bits in the sub-field
	 * @param label       the display label
	 * @return format string
	 */
	public static String fieldLine(long scopeOffset, long scopeLength,
			long bitOffset, long bitLength, String label) {
		return patternScoped(scopeOffset, scopeLength, bitOffset, bitLength)
				+ " = " + label + ": {>>}";
	}

	/**
	 * Generates a bit format line for a boolean/flag field.
	 *
	 * @param bitOffset bit offset from the start of the header
	 * @param label     the display label for the field
	 * @return format string like {@code "{/.... .1.. ..../} = Don't Fragment: {@set}"}
	 */
	public static String flagLine(long bitOffset, String label) {
		return pattern(bitOffset, 1) + " = " + label + ": {@set}";
	}

	/**
	 * Generates a scoped flag line within a parent scope.
	 *
	 * @param scopeOffset bit offset of the parent scope
	 * @param scopeLength bit length of the parent scope
	 * @param bitOffset   bit offset of the flag (absolute)
	 * @param label       the display label
	 * @return format string
	 */
	public static String flagLine(long scopeOffset, long scopeLength,
			long bitOffset, String label) {
		return patternScoped(scopeOffset, scopeLength, bitOffset, 1)
				+ " = " + label + ": {@set}";
	}

	/**
	 * Generates a multi-line bit format for a byte-aligned field, showing all
	 * sub-fields. Useful for composite fields like TCP flags or IPv4 TOS.
	 *
	 * @param scopeOffset bit offset of the containing scope (byte-aligned)
	 * @param scopeLength bit length of the containing scope
	 * @param fields      array of sub-field definitions
	 * @return multi-line format string
	 */
	public static String composite(long scopeOffset, long scopeLength, SubField... fields) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < fields.length; i++) {
			if (i > 0) sb.append('\n');

			SubField f = fields[i];
			long absOffset = scopeOffset + f.relativeOffset;

			if (f.bitLength == 1) {
				sb.append(flagLine(scopeOffset, scopeLength, absOffset, f.label));
			} else {
				sb.append(fieldLine(scopeOffset, scopeLength, absOffset, f.bitLength, f.label));
			}
		}

		return sb.toString();
	}

	/**
	 * Returns the byte offset of the scope start for a given bit offset.
	 * Useful for extracting the right bytes from the header buffer.
	 *
	 * @param bitOffset bit offset from the start of the header
	 * @return byte offset of the containing scope
	 */
	public static int scopeByteOffset(long bitOffset) {
		return (int) (bitOffset / BITS_PER_BYTE);
	}

	/**
	 * Returns the byte length of the scope for a given bit range.
	 *
	 * @param bitOffset bit offset from the start of the header
	 * @param bitLength number of bits in the field
	 * @return byte length of the minimum containing scope
	 */
	public static int scopeByteLength(long bitOffset, long bitLength) {
		int startByte = (int) (bitOffset / BITS_PER_BYTE);
		int endByte = (int) ((bitOffset + bitLength - 1) / BITS_PER_BYTE);
		return endByte - startByte + 1;
	}

	/**
	 * Sub-field definition for composite patterns.
	 */
	public record SubField(long relativeOffset, long bitLength, String label) {
		public static SubField of(long relativeOffset, long bitLength, String label) {
			return new SubField(relativeOffset, bitLength, label);
		}

		public static SubField flag(long relativeOffset, String label) {
			return new SubField(relativeOffset, 1, label);
		}
	}

	private BitPatternBuilder() {
	}
}
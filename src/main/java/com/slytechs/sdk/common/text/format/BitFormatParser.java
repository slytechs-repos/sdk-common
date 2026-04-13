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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.slytechs.sdk.common.text.format.FormatSegment.BitField;
import com.slytechs.sdk.common.text.format.FormatSegment.BitPos;
import com.slytechs.sdk.common.text.format.FormatSegment.BitPosType;

/**
 * Parser for bit format strings. Extends the base parser to add
 * {@code {/pattern/}} bit field directives.
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
final class BitFormatParser extends BaseFormatParser {

	private final char defaultOn;
	private final char defaultOff;
	private final char defaultMasked;

	BitFormatParser(char on, char off, char masked, Map<String, Macro> macros) {
		super(macros, false);
		this.defaultOn = on;
		this.defaultOff = off;
		this.defaultMasked = masked;
	}

	@Override
	FormatSegment parseExtendedDirective(char ch) {
		if (ch == '/')
			return parseBitField();

		return null;
	}

	private static final char CH_SPACE = ' ';
	private static final char CH_SKIP = '+';
	private static final char CH_ALIGN = '=';

	private FormatSegment parseBitField() {
		pos++; // skip opening '/'

		List<Character> patternChars = new ArrayList<>();
		List<BitPosType> posTypes = new ArrayList<>();
		int bitCount = 0;

		while (pos < input.length() && input.charAt(pos) != '/') {
			char ch = input.charAt(pos);
			patternChars.add(ch);

			if (ch == CH_SPACE) {
				posTypes.add(BitPosType.SPACE);
			} else if (ch == CH_SKIP) {
				posTypes.add(BitPosType.SKIP);
				bitCount++;
			} else if (ch == CH_ALIGN) {
				posTypes.add(BitPosType.ALIGN);
				bitCount++;
			} else {
				posTypes.add(BitPosType.BIT);
				bitCount++;
			}

			pos++;
		}

		if (pos >= input.length())
			throw parseError("unterminated bit field, expected '/'");

		pos++; // skip closing '/'

		char onChar = defaultOn;
		char offChar = defaultOff;
		char maskedChar = defaultMasked;
		boolean hidden = false;
		boolean inverted = false;

		if (pos < input.length() && input.charAt(pos) != '}') {
			while (pos < input.length() && input.charAt(pos) != '}') {
				skipSpaces();
				if (pos >= input.length() || input.charAt(pos) == '}')
					break;

				char mch = input.charAt(pos);

				if (mch == ',') {
					pos++;
					continue;
				}

				if (mch == '~') {
					inverted = true;
					pos++;
					continue;
				}

				if (matchKeyword("hide")) {
					hidden = true;
					continue;
				}

				if (mch == '0' && peekAt(1) == '=') {
					pos += 2;
					offChar = input.charAt(pos++);
					continue;
				}
				if (mch == '1' && peekAt(1) == '=') {
					pos += 2;
					onChar = input.charAt(pos++);
					continue;
				}
				if (mch == '.' && peekAt(1) == '=') {
					pos += 2;
					maskedChar = input.charAt(pos++);
					continue;
				}
				if (matchKeyword("on=")) {
					onChar = input.charAt(pos++);
					continue;
				}
				if (matchKeyword("off=")) {
					offChar = input.charAt(pos++);
					continue;
				}
				if (matchKeyword("mask=")) {
					maskedChar = input.charAt(pos++);
					continue;
				}

				throw parseError("unexpected modifier character '%c'", mch);
			}
		}

		pos++; // skip '}'

		long mask = 0L;
		int currentBit = bitCount - 1;

		List<BitPos> positions = new ArrayList<>();
		for (int i = 0; i < patternChars.size(); i++) {
			BitPosType type = posTypes.get(i);
			char pch = patternChars.get(i);

			switch (type) {
			case SPACE -> positions.add(new BitPos(BitPosType.SPACE));

			case SKIP -> {
				positions.add(new BitPos(BitPosType.SKIP));
				currentBit--;
			}

			case ALIGN -> {
				positions.add(new BitPos(BitPosType.ALIGN));
				currentBit--;
			}

			case BIT -> {
				long bitMask = 1L << currentBit;
				positions.add(new BitPos(BitPosType.BIT, bitMask, pch));

				if (pch != defaultMasked) {
					mask |= bitMask;
				}

				currentBit--;
			}
			}
		}

		return new BitField(
				mask,
				bitCount,
				positions.toArray(BitPos[]::new),
				onChar,
				offChar,
				maskedChar,
				hidden,
				inverted);
	}
}
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

import com.slytechs.sdk.common.text.format.FormatSegment.ArgRef;
import com.slytechs.sdk.common.text.format.FormatSegment.AutoArg;
import com.slytechs.sdk.common.text.format.FormatSegment.Literal;
import com.slytechs.sdk.common.text.format.FormatSegment.MacroRef;
import com.slytechs.sdk.common.text.format.FormatSegment.ValueRef;

/**
 * Base parser for format strings. Handles all common {@code {}} directive
 * parsing including value refs, arg refs, macros, format specs, escapes, and
 * auto-incrementing argument indices. No expression evaluation — that is
 * handled by the {@link com.slytechs.sdk.common.text.Template} layer.
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
class BaseFormatParser {

	final Map<String, Macro> macros;

	String input;
	int pos;
	int autoArgIndex;
	private final boolean autoArgs;

	BaseFormatParser(Map<String, Macro> macros, boolean autoArgs) {
		this.macros = macros;
		this.autoArgs = autoArgs;
	}

	FormatPattern parse(String format) {
		this.input = format;
		this.pos = 0;
		this.autoArgIndex = 0;

		List<FormatSegment> segments = new ArrayList<>();
		List<Integer> lineStarts = new ArrayList<>();
		lineStarts.add(0);

		StringBuilder literal = new StringBuilder();

		while (pos < input.length()) {
			char ch = input.charAt(pos);

			if (ch == '\\' && pos + 1 < input.length()) {
				char next = input.charAt(pos + 1);
				if (next == '{' || next == '}') {
					literal.append(next);
					pos += 2;
					continue;
				}
			}

			if (ch == '{') {
				flushLiteral(literal, segments);
				segments.add(parseDirective());
				continue;
			}

			if (ch == '\n') {
				literal.append('\n');
				flushLiteral(literal, segments);
				lineStarts.add(segments.size());
				pos++;
				continue;
			}

			literal.append(ch);
			pos++;
		}

		flushLiteral(literal, segments);

		int[] offsets = lineStarts.stream().mapToInt(Integer::intValue).toArray();
		return new FormatPattern(segments, offsets);
	}

	FormatPattern.Line parseLine(String format) {
		this.input = format;
		this.pos = 0;
		this.autoArgIndex = 0;

		List<FormatSegment> segments = new ArrayList<>();
		StringBuilder literal = new StringBuilder();

		while (pos < input.length()) {
			char ch = input.charAt(pos);

			if (ch == '\\' && pos + 1 < input.length()) {
				char next = input.charAt(pos + 1);
				if (next == '{' || next == '}') {
					literal.append(next);
					pos += 2;
					continue;
				}
			}

			if (ch == '{') {
				flushLiteral(literal, segments);
				segments.add(parseDirective());
				continue;
			}

			literal.append(ch);
			pos++;
		}

		flushLiteral(literal, segments);
		return new FormatPattern.Line(segments);
	}

	void flushLiteral(StringBuilder literal, List<FormatSegment> segments) {
		if (literal.length() > 0) {
			segments.add(new Literal(literal.toString()));
			literal.setLength(0);
		}
	}

	FormatSegment parseDirective() {
		pos++; // skip '{'

		if (pos >= input.length())
			throw parseError("unexpected end of input after '{'");

		char ch = input.charAt(pos);

		// {} — empty placeholder
		if (ch == '}') {
			pos++;
			return autoArgs
					? new AutoArg(autoArgIndex++, null)
					: new ValueRef(null);
		}

		// Hook for subclass directives (e.g., bitfields)
		FormatSegment extended = parseExtendedDirective(ch);
		if (extended != null)
			return extended;

		// {@macro}
		if (ch == '@') {
			return parseMacroRef(-1);
		}

		// {:fmt} or {:@macro}
		if (ch == ':') {
			pos++;
			if (autoArgs)
				return parseAutoArgFormat();
			return parseFormatOrMacro(-1);
		}

		// {N} or {N:fmt}
		if (Character.isDigit(ch)) {
			return parseArgDirective();
		}

		// Expression operators: {>>}, {<< N}, {* N}, {& N}, {~}
		if (ch == '>' || ch == '<' || ch == '*' || ch == '&' || ch == '~') {
			return parseExprDirective(ch);
		}

		// Fallback: treat as format string
		return parseFormatOrMacro(-1);
	}

	FormatSegment parseExtendedDirective(char ch) {
		return null;
	}

	private FormatSegment parseArgDirective() {
		int argIndex = parseInteger();
		skipSpaces();
		char ch = peek();

		if (ch == '}') {
			pos++;
			return new ArgRef(argIndex, null);
		}

		if (ch == ':') {
			pos++;
			return parseFormatOrMacro(argIndex);
		}

		throw parseError("expected '}' or ':' after argument index, got '%c'", ch);
	}

	private FormatSegment parseAutoArgFormat() {
		if (peek() == '@')
			return parseMacroRef(autoArgIndex++);

		String fmt = readUntil('}');
		pos++; // skip '}'
		return new AutoArg(autoArgIndex++, fmt.isEmpty() ? null : fmt);
	}

	FormatSegment parseFormatOrMacro(int argIndex) {
		if (peek() == '@')
			return parseMacroRef(argIndex);

		String fmt = readUntil('}');
		pos++; // skip '}'

		if (argIndex < 0)
			return new ValueRef(fmt.isEmpty() ? null : fmt);
		else
			return new ArgRef(argIndex, fmt.isEmpty() ? null : fmt);
	}

	FormatSegment parseMacroRef(int argIndex) {
		pos++; // skip '@'
		String name = readUntil('}');
		pos++; // skip '}'

		Macro macro = macros.get(name);
		if (macro == null)
			throw parseError("undefined macro '%s'", name);

		if (argIndex < 0)
			return new MacroRef(macro);
		else
			return new MacroRef(macro, argIndex);
	}

	private FormatSegment parseExprDirective(char ch) {
		FormatSegment.ExprOp op;
		long operand = 0;

		switch (ch) {
		case '>' -> {
			pos++;
			if (pos < input.length() && input.charAt(pos) == '>')
				pos++;
			op = FormatSegment.ExprOp.SHIFT_RIGHT;
			skipSpaces();
			// Check for chained operation: {>> * 4}
			if (pos < input.length() && input.charAt(pos) == '*') {
				pos++;
				skipSpaces();
				operand = parseInteger();
				op = FormatSegment.ExprOp.SHIFT_RIGHT_MULTIPLY;
			}
		}
		case '<' -> {
			pos++;
			if (pos < input.length() && input.charAt(pos) == '<')
				pos++;
			op = FormatSegment.ExprOp.SHIFT_LEFT;
			skipSpaces();
			if (pos < input.length() && Character.isDigit(input.charAt(pos)))
				operand = parseInteger();
		}
		case '*' -> {
			pos++;
			op = FormatSegment.ExprOp.MULTIPLY;
			skipSpaces();
			if (pos < input.length() && Character.isDigit(input.charAt(pos)))
				operand = parseInteger();
		}
		case '&' -> {
			pos++;
			op = FormatSegment.ExprOp.AND;
			skipSpaces();
			operand = parseLongLiteral();
		}
		case '~' -> {
			pos++;
			op = FormatSegment.ExprOp.COMPLEMENT;
		}
		default -> throw parseError("unexpected expression operator '%c'", ch);
		}

		skipSpaces();
		String fmt = null;

		if (pos < input.length() && input.charAt(pos) == ':') {
			pos++;
			fmt = readUntil('}');
		}

		if (pos < input.length() && input.charAt(pos) == '}')
			pos++;

		return new FormatSegment.ExprRef(op, operand, fmt);
	}

	private long parseLongLiteral() {
		skipSpaces();
		if (pos + 1 < input.length() && input.charAt(pos) == '0'
				&& (input.charAt(pos + 1) == 'x' || input.charAt(pos + 1) == 'X')) {
			pos += 2;
			int start = pos;
			while (pos < input.length() && isHexDigit(input.charAt(pos)))
				pos++;
			return Long.parseUnsignedLong(input.substring(start, pos), 16);
		}

		return parseInteger();
	}

	private boolean isHexDigit(char ch) {
		return (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F');
	}

	int parseInteger() {
		int start = pos;
		while (pos < input.length() && Character.isDigit(input.charAt(pos)))
			pos++;

		if (pos == start)
			throw parseError("expected integer");

		return Integer.parseInt(input.substring(start, pos));
	}

	String readUntil(char terminator) {
		int start = pos;
		while (pos < input.length() && input.charAt(pos) != terminator)
			pos++;

		if (pos >= input.length())
			throw parseError("expected '%c'", terminator);

		return input.substring(start, pos);
	}

	char peek() {
		if (pos >= input.length())
			throw parseError("unexpected end of input");

		return input.charAt(pos);
	}

	char peekAt(int offset) {
		int idx = pos + offset;
		return (idx < input.length()) ? input.charAt(idx) : '\0';
	}

	boolean matchKeyword(String keyword) {
		if (input.startsWith(keyword, pos)) {
			pos += keyword.length();
			return true;
		}
		return false;
	}

	void skipSpaces() {
		while (pos < input.length() && input.charAt(pos) == ' ')
			pos++;
	}

	IllegalArgumentException parseError(String fmt, Object... args) {
		String msg = String.format(fmt, args);
		return new IllegalArgumentException("format parse error at position %d: %s".formatted(pos, msg));
	}
}
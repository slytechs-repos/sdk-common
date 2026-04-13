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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.slytechs.sdk.common.text.format.FormatPattern;
import com.slytechs.sdk.common.text.format.TextFormat;

/**
 * A two-phase text template with named references and expression evaluation.
 * Phase 1 resolves named references against a {@link Resolver} and evaluates
 * any expressions. Phase 2 passes the resolved/evaluated values to a compiled
 * {@link FormatPattern} for final string formatting.
 *
 * <p>
 * Templates handle name resolution and value transformation. The format layer
 * handles display formatting only (printf, macros, bit patterns).
 *
 * {@snippet :
 * Template tmpl = Template.compile(textFormat,
 *     "Frame {frame.number}: {frame.cap_len} bytes ({frame.cap_len * 8} bits)");
 *
 * String result = tmpl.format(name -> switch (name) {
 *     case "frame.number" -> 4;
 *     case "frame.cap_len" -> 200;
 *     default -> null;
 * });
 * // → "Frame 4: 200 bytes (1600 bits)"
 * }
 *
 * <h2>Expression Operators</h2>
 *
 * <p>
 * Expressions transform resolved values before formatting:
 *
 * <table border="1" cellpadding="4">
 * <caption>Expression operators</caption>
 * <tr><th>Syntax</th><th>Operation</th></tr>
 * <tr><td>{@code {name * N}}</td><td>multiply</td></tr>
 * <tr><td>{@code {name >> N}}</td><td>unsigned right shift</td></tr>
 * <tr><td>{@code {name << N}}</td><td>left shift</td></tr>
 * <tr><td>{@code {name & N}}</td><td>AND mask (supports 0x hex)</td></tr>
 * <tr><td>{@code {name ~}}</td><td>bitwise invert</td></tr>
 * <tr><td>{@code {name * N:fmt}}</td><td>expression + format spec</td></tr>
 * </table>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public final class Template {

	@FunctionalInterface
	public interface Resolver {
		Object resolve(String name);
	}

	record Ref(String name, int argIndex, Expression expr) {
		Ref(String name, int argIndex) {
			this(name, argIndex, null);
		}
	}

	private final FormatPattern pattern;
	private final Ref[] refs;
	private final String[] uniqueNames;

	private Template(FormatPattern pattern, Ref[] refs, String[] uniqueNames) {
		this.pattern = pattern;
		this.refs = refs;
		this.uniqueNames = uniqueNames;
	}

	public static Template compile(TextFormat textFormat, String template) {
		return new TemplateParser(textFormat, null).compile(template);
	}

	/**
	 * Compiles a template with a ref callback. The callback is invoked for each
	 * named reference discovered during compilation, allowing the caller to
	 * register stub refs for forward references.
	 *
	 * @param textFormat the text format
	 * @param template   the template string
	 * @param refCallback called for each reference name found, may be null
	 * @return the compiled template
	 */
	public static Template compile(TextFormat textFormat, String template,
			java.util.function.Consumer<String> refCallback) {
		return new TemplateParser(textFormat, refCallback).compile(template);
	}

	/**
	 * Phase 1: resolve names and evaluate expressions. Phase 2: format.
	 *
	 * @param resolver resolves names to values
	 * @return the formatted string
	 */
	public String format(Resolver resolver) {
		// Phase 1: resolve unique names
		Map<String, Object> resolved = new LinkedHashMap<>();
		for (String name : uniqueNames)
			resolved.put(name, resolver.resolve(name));

		// Build args array with expression evaluation
		Object[] args = new Object[refs.length];
		for (int i = 0; i < refs.length; i++) {
			Ref ref = refs[i];
			Object val = resolved.get(ref.name);

			if (ref.expr != null && val instanceof Number n)
				val = ref.expr.evaluate(n.longValue(), ~0L);

			args[i] = val;
		}

		// Phase 2: format
		return pattern.format(null, args);
	}

	public String[] referenceNames() {
		return uniqueNames.clone();
	}

	public int resolveCount() {
		return uniqueNames.length;
	}

	static final class TemplateParser {

		private final TextFormat textFormat;
		private final java.util.function.Consumer<String> refCallback;
		private String input;
		private int pos;

		TemplateParser(TextFormat textFormat, java.util.function.Consumer<String> refCallback) {
			this.textFormat = textFormat;
			this.refCallback = refCallback;
		}

		Template compile(String template) {
			this.input = template;
			this.pos = 0;

			List<Ref> refs = new ArrayList<>();
			List<String> uniqueNames = new ArrayList<>();
			StringBuilder formatStr = new StringBuilder();

			while (pos < input.length()) {
				char ch = input.charAt(pos);

				if (ch == '\\' && pos + 1 < input.length()) {
					char next = input.charAt(pos + 1);
					if (next == '{' || next == '}') {
						formatStr.append('\\').append(next);
						pos += 2;
						continue;
					}
				}

				if (ch == '{') {
					int close = findClose(input, pos);
					if (close < 0)
						throw new IllegalArgumentException("unclosed '{' at position " + pos);

					String content = input.substring(pos + 1, close);
					String name = extractName(content);

					if (name != null) {
						if (!uniqueNames.contains(name))
							uniqueNames.add(name);

						if (refCallback != null)
							refCallback.accept(name);

						String remainder = content.substring(name.length()).stripLeading();

						// Parse expression from remainder
						Expression expr = null;
						String formatSpec = null;

						if (!remainder.isEmpty()) {
							ExprParseResult epr = parseExpression(remainder);
							expr = epr.expr;
							formatSpec = epr.formatSpec;
						}

						refs.add(new Ref(name, refs.size(), expr));

						// Pass format spec to TextFormat as {:fmt}
						if (formatSpec != null && !formatSpec.isEmpty())
							formatStr.append("{:").append(formatSpec).append('}');
						else
							formatStr.append("{}");
					} else {
						formatStr.append('{').append(content).append('}');
					}

					pos = close + 1;
					continue;
				}

				formatStr.append(ch);
				pos++;
			}

			FormatPattern pattern = textFormat.compile(formatStr.toString());

			return new Template(
					pattern,
					refs.toArray(Ref[]::new),
					uniqueNames.toArray(String[]::new));
		}

		record ExprParseResult(Expression expr, String formatSpec) {}

		private ExprParseResult parseExpression(String remainder) {
			int p = 0;
			Expression expr = null;

			// Try parse operator
			if (p < remainder.length()) {
				char ch = remainder.charAt(p);

				if (ch == '~') {
					expr = Expression.INVERT;
					p++;
				} else if (ch == '>' && p + 1 < remainder.length() && remainder.charAt(p + 1) == '>') {
					p += 2;
					String rest = remainder.substring(p).stripLeading();
					if (!rest.isEmpty() && (Character.isDigit(rest.charAt(0)) || rest.startsWith("0x"))) {
						NumberParse np = parseNumber(rest);
						expr = Expression.shiftRight(np.value);
						p = remainder.length() - (rest.length() - np.consumed);

						// Check for chained operator after >>
						String afterShift = remainder.substring(p).stripLeading();
						if (!afterShift.isEmpty()) {
							ExprParseResult chained = parseExpression(afterShift);
							if (chained.expr != null)
								expr = Expression.chain(expr, chained.expr);
							return new ExprParseResult(expr, chained.formatSpec);
						}
					} else {
						expr = Expression.AUTO_SHIFT;
						// Check for chain: >>* N
						if (!rest.isEmpty() && isOperator(rest.charAt(0))) {
							ExprParseResult chained = parseExpression(rest);
							if (chained.expr != null)
								expr = Expression.chain(expr, chained.expr);
							return new ExprParseResult(expr, chained.formatSpec);
						}
					}
				} else if (ch == '<' && p + 1 < remainder.length() && remainder.charAt(p + 1) == '<') {
					p += 2;
					String rest = remainder.substring(p).stripLeading();
					NumberParse np = parseNumber(rest);
					expr = Expression.shiftLeft(np.value);
					p = remainder.length() - (rest.length() - np.consumed);
				} else if (ch == '*') {
					p++;
					String rest = remainder.substring(p).stripLeading();
					NumberParse np = parseNumber(rest);
					expr = Expression.multiply(np.value);
					p = remainder.length() - (rest.length() - np.consumed);
				} else if (ch == '&') {
					p++;
					String rest = remainder.substring(p).stripLeading();
					NumberParse np = parseNumber(rest);
					expr = Expression.and(np.value);
					p = remainder.length() - (rest.length() - np.consumed);
				}
			}

			// Remaining is format spec (after optional :)
			String formatSpec = null;
			String leftover = (p < remainder.length()) ? remainder.substring(p).stripLeading() : "";
			if (leftover.startsWith(":"))
				formatSpec = leftover.substring(1);
			else if (!leftover.isEmpty() && expr == null)
				formatSpec = leftover; // no expr found, treat all as format

			return new ExprParseResult(expr, formatSpec);
		}

		record NumberParse(int value, int consumed) {}

		private NumberParse parseNumber(String s) {
			int p = 0;
			if (s.startsWith("0x") || s.startsWith("0X")) {
				p = 2;
				int start = p;
				while (p < s.length() && isHexDigit(s.charAt(p))) p++;
				if (p == start)
					throw new IllegalArgumentException("expected hex digits after 0x");
				return new NumberParse(Integer.parseInt(s.substring(start, p), 16), p);
			}

			int start = p;
			while (p < s.length() && Character.isDigit(s.charAt(p))) p++;
			if (p == start)
				throw new IllegalArgumentException("expected number");
			return new NumberParse(Integer.parseInt(s.substring(start, p)), p);
		}

		private boolean isOperator(char ch) {
			return ch == '*' || ch == '&' || ch == '<' || ch == '~';
		}

		private boolean isHexDigit(char ch) {
			return (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F');
		}

		private String extractName(String content) {
			if (content.isEmpty())
				return null;

			char first = content.charAt(0);

			if (first == ':' || first == '/' || first == '@'
					|| first == '>' || first == '<' || first == '*'
					|| first == '&' || first == '~'
					|| Character.isDigit(first))
				return null;

			int i = 0;
			while (i < content.length()) {
				char ch = content.charAt(i);
				if (Character.isLetterOrDigit(ch) || ch == '.' || ch == '_')
					i++;
				else
					break;
			}

			if (i == 0)
				return null;

			return content.substring(0, i);
		}

		private int findClose(String str, int openPos) {
			int depth = 1;
			for (int i = openPos + 1; i < str.length(); i++) {
				char ch = str.charAt(i);
				if (ch == '\\' && i + 1 < str.length()) {
					i++;
					continue;
				}
				if (ch == '{') depth++;
				if (ch == '}') {
					depth--;
					if (depth == 0) return i;
				}
			}
			return -1;
		}
	}
}
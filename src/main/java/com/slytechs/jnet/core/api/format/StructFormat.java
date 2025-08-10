/*
 * Sly Technologies Free License
 * 
 * Copyright 2025 Sly Technologies Inc.
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
package com.slytechs.jnet.core.api.format;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public final class StructFormat {
	public static final int DEFAULT_INDENT = 2;
	private static final String INDENT_CHAR_BUFFER = " ";
	private static final String LEFT_RIGHT_SEPARATOR = " = ";

	private String open;
	private String close;

	private final StringBuilder sb;
	private final String indent;
	private final StructFormat parent;

	public StructFormat() {
		this(new StringBuilder());
	}

	public StructFormat(int indentation, StringBuilder sb) {
		this.sb = sb;
		this.indent = INDENT_CHAR_BUFFER.repeat(indentation);
		this.parent = null;
	}

	public StructFormat(StringBuilder sb) {
		this(0, sb);
	}

	private StructFormat(int indentation, StructFormat parent) {
		this.parent = parent;
		this.indent = INDENT_CHAR_BUFFER.repeat(indentation);
		this.sb = parent.sb;

		this.open = parent.open;
		this.close = parent.close;

//		System.out.println("INDENT() " + indent.length());
	}

	public StructFormat close() {
		var inner = this;
		var outter = (parent != null) ? parent : inner;
		outter.printIndent();

		sb.append(close);

		return outter;
	}

	public StructFormat closeln() {
		return close().println();
	}

	public StructFormat indent() {
		return new StructFormat(indent.length() + DEFAULT_INDENT, this);
	}

	public StructFormat open(String structName) {
		return open(structName + " {", "}");
	}

	public StructFormat open(String open, String close) {
		this.open = open;
		this.close = close;

		append(open);

		var inner = indent();
//		append("" + inner.indent.length());

		return inner;
	}

	public StructFormat openln(String structName) {
		return open(structName).println();
	}

	public StructFormat openln(String open, String close) {
		return open(open, close).println();
	}

	public StructFormat append(String text) {
		sb.append(text);

		return this;
	}

	public StructFormat append(String left, Object... right) {
		appendLeft(left);
		printRight(Stream.of(right)
				.map(Object::toString)
				.collect(Collectors.joining(" ")));

		return this;
	}

	public StructFormat print(String text) {
		printIndent();
		sb.append(text);

		return this;
	}

	public StructFormat println(String text) {
		return print(text).println();
	}

	public StructFormat print(String left, Object... right) {

		printIndent();
		printLeft(left);
		printRight(Stream.of(right)
				.map(Object::toString)
				.collect(Collectors.joining(" ")));

		return this;
	}

	public StructFormat printf(String left, Object... args) {
		printIndent();
		printLeft(left);
		printRight(left.formatted(args));

		return this;
	}

	public StructFormat printIndent() {
//		sb.append('<');
		sb.append(indent);
//		sb.append('>');

		return this;
	}

	private StructFormat printLeft(String left) {
		printIndent();
		sb.append(left);
		sb.append(LEFT_RIGHT_SEPARATOR);

		return this;
	}

	private StructFormat appendLeft(String left) {
		sb.append(left);
		sb.append(LEFT_RIGHT_SEPARATOR);

		return this;
	}

	public StructFormat println() {
		sb.append('\n');

		return this;
	}

	public StructFormat println(String left, Object... right) {
		printLeft(left);
		printRightln(Stream.of(right)
				.filter(a -> a != null)
				.map(String::valueOf)
				.collect(Collectors.joining(" ")));

		return this;
	}

	public StructFormat print(String left, StructFormattable right) {
		printLeft(left);

		if (right != null)
			right.format(this);

		return this;
	}

	public StructFormat println(String left, StructFormattable right) {
		return print(left, right).println();
	}

	private StructFormat printRight(String right) {
		sb.append(right);

		return this;
	}

	private StructFormat printRightln(String right) {
		sb.append(right);
		println();

		return this;
	}

	@Override
	public String toString() {
		return sb.toString();
	}
}

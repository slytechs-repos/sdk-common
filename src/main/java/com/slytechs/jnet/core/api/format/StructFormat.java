/*
 * Sly Technologies Free License
 * 
 * Copyright 2024 Sly Technologies Inc.
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
 * The Class StructFormat.
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public final class StructFormat {
	
	/** The Constant DEFAULT_INDENT. */
	public static final int DEFAULT_INDENT = 2;
	
	/** The Constant INDENT_CHAR_BUFFER. */
	private static final String INDENT_CHAR_BUFFER = " ";
	
	/** The Constant LEFT_RIGHT_SEPARATOR. */
	private static final String LEFT_RIGHT_SEPARATOR = " = ";

	/** The open. */
	private String open;
	
	/** The close. */
	private String close;

	/** The sb. */
	private final StringBuilder sb;
	
	/** The indent. */
	private final String indent;
	
	/** The parent. */
	private final StructFormat parent;

	/**
	 * Instantiates a new struct format.
	 */
	public StructFormat() {
		this(new StringBuilder());
	}

	/**
	 * Instantiates a new struct format.
	 *
	 * @param indentation the indentation
	 * @param sb          the sb
	 */
	public StructFormat(int indentation, StringBuilder sb) {
		this.sb = sb;
		this.indent = INDENT_CHAR_BUFFER.repeat(indentation);
		this.parent = null;
	}

	/**
	 * Instantiates a new struct format.
	 *
	 * @param sb the sb
	 */
	public StructFormat(StringBuilder sb) {
		this(0, sb);
	}

	/**
	 * Instantiates a new struct format.
	 *
	 * @param indentation the indentation
	 * @param parent      the parent
	 */
	private StructFormat(int indentation, StructFormat parent) {
		this.parent = parent;
		this.indent = INDENT_CHAR_BUFFER.repeat(indentation);
		this.sb = parent.sb;

		this.open = parent.open;
		this.close = parent.close;

//		System.out.println("INDENT() " + indent.length());
	}

	/**
	 * Close.
	 *
	 * @return the struct format
	 */
	public StructFormat close() {
		var inner = this;
		var outter = (parent != null) ? parent : inner;
		outter.printIndent();

		sb.append(close);

		return outter;
	}

	/**
	 * Closeln.
	 *
	 * @return the struct format
	 */
	public StructFormat closeln() {
		return close().println();
	}

	/**
	 * Indent.
	 *
	 * @return the struct format
	 */
	public StructFormat indent() {
		return new StructFormat(indent.length() + DEFAULT_INDENT, this);
	}

	/**
	 * Open.
	 *
	 * @param structName the struct name
	 * @return the struct format
	 */
	public StructFormat open(String structName) {
		return open(structName + " {", "}");
	}

	/**
	 * Open.
	 *
	 * @param open  the open
	 * @param close the close
	 * @return the struct format
	 */
	public StructFormat open(String open, String close) {
		this.open = open;
		this.close = close;

		append(open);

		var inner = indent();
//		append("" + inner.indent.length());

		return inner;
	}

	/**
	 * Openln.
	 *
	 * @param structName the struct name
	 * @return the struct format
	 */
	public StructFormat openln(String structName) {
		return open(structName).println();
	}

	/**
	 * Openln.
	 *
	 * @param open  the open
	 * @param close the close
	 * @return the struct format
	 */
	public StructFormat openln(String open, String close) {
		return open(open, close).println();
	}

	/**
	 * Append.
	 *
	 * @param text the text
	 * @return the struct format
	 */
	public StructFormat append(String text) {
		sb.append(text);

		return this;
	}

	/**
	 * Append.
	 *
	 * @param left  the left
	 * @param right the right
	 * @return the struct format
	 */
	public StructFormat append(String left, Object... right) {
		appendLeft(left);
		printRight(Stream.of(right)
				.map(Object::toString)
				.collect(Collectors.joining(" ")));

		return this;
	}

	/**
	 * Prints the.
	 *
	 * @param text the text
	 * @return the struct format
	 */
	public StructFormat print(String text) {
		printIndent();
		sb.append(text);

		return this;
	}

	/**
	 * Println.
	 *
	 * @param text the text
	 * @return the struct format
	 */
	public StructFormat println(String text) {
		return print(text).println();
	}

	/**
	 * Prints the.
	 *
	 * @param left  the left
	 * @param right the right
	 * @return the struct format
	 */
	public StructFormat print(String left, Object... right) {

		printIndent();
		printLeft(left);
		printRight(Stream.of(right)
				.map(Object::toString)
				.collect(Collectors.joining(" ")));

		return this;
	}

	/**
	 * Printf.
	 *
	 * @param left the left
	 * @param args the args
	 * @return the struct format
	 */
	public StructFormat printf(String left, Object... args) {
		printIndent();
		printLeft(left);
		printRight(left.formatted(args));

		return this;
	}

	/**
	 * Prints the indent.
	 *
	 * @return the struct format
	 */
	public StructFormat printIndent() {
//		sb.append('<');
		sb.append(indent);
//		sb.append('>');

		return this;
	}

	/**
	 * Prints the left.
	 *
	 * @param left the left
	 * @return the struct format
	 */
	private StructFormat printLeft(String left) {
		printIndent();
		sb.append(left);
		sb.append(LEFT_RIGHT_SEPARATOR);

		return this;
	}

	/**
	 * Append left.
	 *
	 * @param left the left
	 * @return the struct format
	 */
	private StructFormat appendLeft(String left) {
		sb.append(left);
		sb.append(LEFT_RIGHT_SEPARATOR);

		return this;
	}

	/**
	 * Println.
	 *
	 * @return the struct format
	 */
	public StructFormat println() {
		sb.append('\n');

		return this;
	}

	/**
	 * Println.
	 *
	 * @param left  the left
	 * @param right the right
	 * @return the struct format
	 */
	public StructFormat println(String left, Object... right) {
		printLeft(left);
		printRightln(Stream.of(right)
				.filter(a -> a != null)
				.map(String::valueOf)
				.collect(Collectors.joining(" ")));

		return this;
	}

	/**
	 * Prints the.
	 *
	 * @param left  the left
	 * @param right the right
	 * @return the struct format
	 */
	public StructFormat print(String left, StructFormattable right) {
		printLeft(left);

		if (right != null)
			right.format(this);

		return this;
	}

	/**
	 * Println.
	 *
	 * @param left  the left
	 * @param right the right
	 * @return the struct format
	 */
	public StructFormat println(String left, StructFormattable right) {
		return print(left, right).println();
	}

	/**
	 * Prints the right.
	 *
	 * @param right the right
	 * @return the struct format
	 */
	private StructFormat printRight(String right) {
		sb.append(right);

		return this;
	}

	/**
	 * Prints the rightln.
	 *
	 * @param right the right
	 * @return the struct format
	 */
	private StructFormat printRightln(String right) {
		sb.append(right);
		println();

		return this;
	}

	/**
	 * To string.
	 *
	 * @return the string
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return sb.toString();
	}
}

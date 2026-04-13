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

import java.util.List;

import com.slytechs.sdk.common.text.Arg;

/**
 * A compiled bit format pattern. Immutable and reusable. Created by
 * {@link BitFormat#compile(String)} or {@link BitFormat#compileLine(String)}.
 *
 * {@snippet :
 * BitFormat fmt = new BitFormat();
 * fmt.setMacro("set", (v, m) -> v == 0 ? "Not Set" : "Set");
 *
 * BitPattern pattern = fmt.compile("""
 *     Flags: {:0x%03X}
 *         {/1111 ..../} = High nibble: {}
 *         {/.... 1111/} = Low nibble: {}
 *     """);
 *
 * String output = pattern.format(0xA5);
 * String[] lines = pattern.formatLines(0xA5);
 * }
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public final class BitPattern {

	/**
	 * A single compiled line within a pattern.
	 */
	public static final class Line {
		private final List<BitSegment> segments;

		Line(List<BitSegment> segments) {
			this.segments = List.copyOf(segments);
		}

		/**
		 * Formats this line with the given value and default full mask.
		 *
		 * @param value the value to format
		 * @param args  optional arguments for placeholders
		 * @return the formatted line
		 */
		public String format(long value, Object... args) {
			return format(value, ~0L, args);
		}

		/**
		 * Formats this line with the given value and mask.
		 *
		 * @param value the value to format
		 * @param mask  the initial mask for this line
		 * @param args  optional arguments for placeholders
		 * @return the formatted line
		 */
		public String format(long value, long mask, Object... args) {
			StringBuilder sb = new StringBuilder();
			long currentMask = mask;
			for (BitSegment seg : segments) {
				currentMask = seg.render(sb, value, currentMask, args);
			}
			return sb.toString();
		}

		/**
		 * Formats this line using Arg-based resolution against a target object.
		 *
		 * @param value  the value to format
		 * @param target the target object for Arg resolution
		 * @param args   Arg instances to resolve against target
		 * @return the formatted line
		 */
		public String format(long value, Object target, Arg... args) {
			return format(value, ~0L, target, args);
		}

		/**
		 * Formats this line using Arg-based resolution against a target object.
		 *
		 * @param value  the value to format
		 * @param mask   the initial mask for this line
		 * @param target the target object for Arg resolution
		 * @param args   Arg instances to resolve against target
		 * @return the formatted line
		 */
		public String format(long value, long mask, Object target, Arg... args) {
			return format(value, mask, Arg.resolve(target, args));
		}
	}

	private final List<BitSegment> segments;
	private final int[] lineOffsets;

	BitPattern(List<BitSegment> segments, int[] lineOffsets) {
		this.segments = List.copyOf(segments);
		this.lineOffsets = lineOffsets;
	}

	/**
	 * Formats the entire pattern as a single multi-line string.
	 *
	 * @param value the value to format
	 * @param args  optional arguments for placeholders
	 * @return the formatted output
	 */
	public String format(long value, Object... args) {
		return format(value, ~0L, args);
	}

	/**
	 * Formats the entire pattern as a single multi-line string with an initial
	 * mask.
	 *
	 * @param value the value to format
	 * @param mask  the initial mask (reset per line)
	 * @param args  optional arguments for placeholders
	 * @return the formatted output
	 */
	public String format(long value, long mask, Object... args) {
		StringBuilder sb = new StringBuilder();
		long currentMask = mask;

		for (int i = 0; i < segments.size(); i++) {
			BitSegment seg = segments.get(i);

			if (isLineStart(i))
				currentMask = mask;

			currentMask = seg.render(sb, value, currentMask, args);
		}

		return sb.toString();
	}

	/**
	 * Formats the pattern and returns each line as a separate string.
	 *
	 * @param value the value to format
	 * @param args  optional arguments for placeholders
	 * @return array of formatted lines
	 */
	public String[] formatLines(long value, Object... args) {
		return formatLines(value, ~0L, args);
	}

	/**
	 * Formats the pattern and returns each line as a separate string.
	 *
	 * @param value the value to format
	 * @param mask  the initial mask (reset per line)
	 * @param args  optional arguments for placeholders
	 * @return array of formatted lines
	 */
	public String[] formatLines(long value, long mask, Object... args) {
		int lineCount = lineOffsets.length;
		String[] lines = new String[lineCount];

		for (int li = 0; li < lineCount; li++) {
			int start = lineOffsets[li];
			int end = (li + 1 < lineCount) ? lineOffsets[li + 1] : segments.size();

			StringBuilder sb = new StringBuilder();
			long currentMask = mask;

			for (int i = start; i < end; i++) {
				currentMask = segments.get(i).render(sb, value, currentMask, args);
			}

			String result = sb.toString();
			if (result.endsWith("\n"))
				result = result.substring(0, result.length() - 1);
			lines[li] = result;
		}

		return lines;
	}

	/**
	 * Formats using Arg-based resolution against a target object.
	 *
	 * @param value  the value to format
	 * @param target the target object for Arg resolution
	 * @param args   Arg instances to resolve against target
	 * @return the formatted output
	 */
	public String format(long value, Object target, Arg... args) {
		return format(value, ~0L, Arg.resolve(target, args));
	}

	/**
	 * Returns the number of lines in this pattern.
	 *
	 * @return line count
	 */
	public int lineCount() {
		return lineOffsets.length;
	}

	private boolean isLineStart(int segmentIndex) {
		for (int offset : lineOffsets) {
			if (offset == segmentIndex)
				return true;
		}
		return false;
	}
}
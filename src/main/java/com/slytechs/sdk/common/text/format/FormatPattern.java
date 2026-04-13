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
 * A compiled format pattern. Immutable and reusable. Created by
 * {@link BitFormat#compile(String)} or {@link TextFormat#compile(String)}.
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public final class FormatPattern {

	public static final class Line {
		private final List<FormatSegment> segments;

		Line(List<FormatSegment> segments) {
			this.segments = List.copyOf(segments);
		}

		public String format(Object value, Object... args) {
			return formatMasked(value, ~0L, args);
		}

		public String formatMasked(Object value, long mask, Object... args) {
			FormatValue fv = new FormatValue().set(value);
			StringBuilder sb = new StringBuilder();
			long currentMask = mask;
			for (FormatSegment seg : segments)
				currentMask = seg.render(sb, fv, currentMask, args);
			return sb.toString();
		}

		public String formatWith(Object value, Object target, Arg... args) {
			return format(value, Arg.resolve(target, args));
		}

		public String formatWith(Object value, long mask, Object target, Arg... args) {
			return formatMasked(value, mask, Arg.resolve(target, args));
		}
	}

	private final List<FormatSegment> segments;
	private final int[] lineOffsets;

	FormatPattern(List<FormatSegment> segments, int[] lineOffsets) {
		this.segments = List.copyOf(segments);
		this.lineOffsets = lineOffsets;
	}

	public String format(Object value, Object... args) {
		return formatMasked(value, ~0L, args);
	}

	public String formatMasked(Object value, long mask, Object... args) {
		FormatValue fv = new FormatValue().set(value);
		return renderAll(fv, mask, args);
	}

	public String[] formatLines(Object value, Object... args) {
		return formatLinesMasked(value, ~0L, args);
	}

	public String[] formatLinesMasked(Object value, long mask, Object... args) {
		FormatValue fv = new FormatValue().set(value);
		return renderLines(fv, mask, args);
	}

	public String formatWith(Object value, Object target, Arg... args) {
		return format(value, Arg.resolve(target, args));
	}

	public int lineCount() {
		return lineOffsets.length;
	}

	private String renderAll(FormatValue fv, long mask, Object[] args) {
		StringBuilder sb = new StringBuilder();
		long currentMask = mask;

		for (int i = 0; i < segments.size(); i++) {
			if (isLineStart(i))
				currentMask = mask;

			currentMask = segments.get(i).render(sb, fv, currentMask, args);
		}

		return sb.toString();
	}

	private String[] renderLines(FormatValue fv, long mask, Object[] args) {
		int lineCount = lineOffsets.length;
		String[] lines = new String[lineCount];

		for (int li = 0; li < lineCount; li++) {
			int start = lineOffsets[li];
			int end = (li + 1 < lineCount) ? lineOffsets[li + 1] : segments.size();

			StringBuilder sb = new StringBuilder();
			long currentMask = mask;

			for (int i = start; i < end; i++)
				currentMask = segments.get(i).render(sb, fv, currentMask, args);

			String result = sb.toString();
			if (result.endsWith("\n"))
				result = result.substring(0, result.length() - 1);
			lines[li] = result;
		}

		return lines;
	}

	private boolean isLineStart(int segmentIndex) {
		for (int offset : lineOffsets)
			if (offset == segmentIndex)
				return true;
		return false;
	}
}
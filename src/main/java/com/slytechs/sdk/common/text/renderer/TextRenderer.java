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
package com.slytechs.sdk.common.text.renderer;

import com.slytechs.sdk.common.text.DataEmitter.DataRender;
import com.slytechs.sdk.common.text.Detail;
import com.slytechs.sdk.common.text.StringText;
import com.slytechs.sdk.common.text.Text;

/**
 * Renders tshark-style flat text output. Fields are indented by depth, meta
 * fields are wrapped in brackets, and summary lines mark section headers.
 * Reusable across any target type.
 *
 * {@snippet :
 * TextRenderer renderer = new TextRenderer();
 *
 * DataEmitter<Packet> emitter = new DataEmitter<Packet>()
 * 		.section(SUMMARY, sec -> sec
 * 				.field("Frame Number", Packet::frameNumber, "frame.number")
 * 				.meta("Frame is marked", p -> "False"));
 *
 * Text text = renderer.render(emitter, packet, contexts);
 * System.out.println(text);
 * // Frame 1: 200 bytes
 * //     Frame Number: 1
 * //     [Frame is marked: False]
 * }
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class TextRenderer extends AbstractRenderer {

	public static final TextRenderer INSTANCE = new TextRenderer();

	private static final String DEFAULT_INDENT = "    ";
	private static final String DEFAULT_FIELD_SEPARATOR = ": ";
	private static final String DEFAULT_META_PREFIX = "[";
	private static final String DEFAULT_META_SUFFIX = "]";

	private final String indent;
	private final String fieldSeparator;
	private final String metaPrefix;
	private final String metaSuffix;

	private StringBuilder sb;

	public TextRenderer() {
		this(DEFAULT_INDENT, DEFAULT_FIELD_SEPARATOR,
				DEFAULT_META_PREFIX, DEFAULT_META_SUFFIX);
	}

	public TextRenderer(String indent) {
		this(indent, DEFAULT_FIELD_SEPARATOR,
				DEFAULT_META_PREFIX, DEFAULT_META_SUFFIX);
	}

	public TextRenderer(String indent, String fieldSeparator,
			String metaPrefix, String metaSuffix) {
		this.indent = indent;
		this.fieldSeparator = fieldSeparator;
		this.metaPrefix = metaPrefix;
		this.metaSuffix = metaSuffix;
	}

	@Override
	protected void onBegin(Detail detail) {
		sb = new StringBuilder();
	}

	@Override
	protected Text buildText(Detail detail) {
		if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n')
			sb.setLength(sb.length() - 1);

		String content = sb.toString();
		sb = null;

		return new StringText(content);
	}

	@Override
	public DataRender line(String line) {
		appendIndent();
		sb.append(line);
		sb.append('\n');
		return this;
	}

	@Override
	public DataRender row(String line) {
		sb.append(line);
		sb.append('\n');
		return this;
	}

	@Override
	public DataRender summary(String line) {
		appendIndent();
		sb.append(line);
		sb.append('\n');
		return this;
	}

	@Override
	public DataRender field(String label, String value) {
		appendIndent();
		if (value.isEmpty()) {
			sb.append(label);
		} else {
			sb.append(label);
			sb.append(fieldSeparator);
			sb.append(value);
		}
		sb.append('\n');
		return this;
	}

	@Override
	public DataRender bitfield(String label, String bitPattern, long maskedValue, String formattedValue) {
		appendIndent();
		sb.append(formattedValue);
		sb.append('\n');
		return this;
	}

	@Override
	public DataRender meta(String label, String value) {
		appendIndent();
		sb.append(metaPrefix);
		sb.append(label);
		sb.append(fieldSeparator);
		sb.append(value);
		sb.append(metaSuffix);
		sb.append('\n');
		return this;
	}

	@Override
	public DataRender mime(String type) {
		return this;
	}

	private void appendIndent() {
		for (int i = 0; i < depth(); i++)
			sb.append(indent);
	}
}
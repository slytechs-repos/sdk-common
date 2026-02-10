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

import java.io.IOException;

/**
 * A {@link Text} implementation backed by a rendered string. Immutable
 * snapshot with no reference to the source object.
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class StringText implements Text {

	private final String content;

	public StringText(String content) {
		this.content = content;
	}

	@Override
	public String toString() {
		return content;
	}

	@Override
	public Appendable append(Appendable out) throws IOException {
		out.append(content);
		return out;
	}

	@Override
	public StringBuilder append(StringBuilder sb) {
		sb.append(content);
		return sb;
	}
}
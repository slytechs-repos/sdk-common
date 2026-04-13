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

import com.slytechs.sdk.common.text.DataEmitter;
import com.slytechs.sdk.common.text.DataEmitter.DataContexts;
import com.slytechs.sdk.common.text.DataEmitter.DataRender;
import com.slytechs.sdk.common.text.Detail;
import com.slytechs.sdk.common.text.Text;

/**
 * Base renderer that tracks render state (depth, detail level) and delegates
 * to subclasses for format-specific output. Reusable across any target type
 * since the emitter and target are provided at render time, not construction.
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public abstract class AbstractRenderer implements DataRender {

	private int depth;
	private Detail detail;

	protected AbstractRenderer() {
	}

	public <T> Text render(DataEmitter<T> emitter, T target) {
		return render(emitter, Detail.HIGH, target, DataContexts.EMPTY);
	}

	public <T> Text render(DataEmitter<T> emitter, T target, DataContexts contexts) {
		return render(emitter, Detail.HIGH, target, contexts);
	}

	public synchronized <T> Text render(DataEmitter<T> emitter, Detail detail, T target, DataContexts contexts) {
		this.detail = detail;
		this.depth = 0;

		onBegin(detail);
		emitter.dsl().emit(this, target, contexts);
		onEnd(detail);

		return buildText(detail);
	}

	@Override
	public int depth() {
		return depth;
	}

	@Override
	public Detail detail() {
		return detail;
	}

	@Override
	public DataRender push() {
		depth++;
		onPush(depth);
		return this;
	}

	@Override
	public DataRender pop() {
		onPop(depth);
		depth--;
		return this;
	}

	protected void onBegin(Detail detail) {}

	protected void onEnd(Detail detail) {}

	protected void onPush(int newDepth) {}

	protected void onPop(int oldDepth) {}

	protected abstract Text buildText(Detail detail);
}
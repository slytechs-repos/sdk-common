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

import com.slytechs.sdk.common.text.DataEmitter.DataContexts;
import com.slytechs.sdk.common.text.DataEmitter.DataEmittable;
import com.slytechs.sdk.common.text.DataEmitter.DataRender;
import com.slytechs.sdk.common.text.renderer.TextRenderer;

/**
 * Implemented by protocol headers and data structures that can produce textual
 * representations. Provides both low-level emission for delegation into an
 * existing render stream, and standalone rendering that returns a {@link Text}
 * snapshot.
 *
 * {@snippet :
 * class Ip4 extends Header implements Textual {
 * 	private static final DataEmitter<Ip4> EMITTER = new DataEmitter<Ip4>()
 * 			.section("Internet Protocol Version 4, Src: {ip.src}, Dst: {ip.dst}", sec -> sec
 * 					.field("Source Address", Ip4::src, "ip.src")
 * 					.field("Destination Address", Ip4::dst, "ip.dst"));
 *
 * 	public void emitText(DataRender render, DataContexts parentContexts) {
 * 		DataContexts combined = EMITTER.injectResolver(this, parentContexts);
 * 		EMITTER.dsl().emit(render, this, combined);
 * 	}
 *
 * 	public Text toText(Detail detail) {
 * 		return Textual.render(EMITTER, detail, this);
 * 	}
 *
 * 	public DataEmitter<?> dataEmitter() {
 * 		return EMITTER;
 * 	}
 * }
 * }
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public interface Textual {

	TextRenderer DEFAULT_RENDERER = new TextRenderer();

	/**
	 * Renders a standalone text snapshot using the default renderer.
	 *
	 * @param <T>     the target type
	 * @param emitter the emitter definition
	 * @param detail  the detail level
	 * @param target  the target object
	 * @return immutable text snapshot
	 */
	static <T> Text render(DataEmitter<T> emitter, Detail detail, T target) {
		return DEFAULT_RENDERER.render(emitter, detail, target, DataContexts.EMPTY);
	}

	/**
	 * Renders a standalone text snapshot with contexts.
	 *
	 * @param <T>      the target type
	 * @param emitter  the emitter definition
	 * @param detail   the detail level
	 * @param target   the target object
	 * @param contexts the contexts
	 * @return immutable text snapshot
	 */
	static <T> Text render(DataEmitter<T> emitter, Detail detail, T target, DataContexts contexts) {
		return DEFAULT_RENDERER.render(emitter, detail, target, contexts);
	}

	/**
	 * Emits this object's textual representation into the given render. The
	 * implementation should inject its own resolver into the parent contexts before
	 * emitting, so both parent and local refs are available to templates.
	 *
	 * @param render         the render callback to write to
	 * @param parentContexts the parent's contexts (carries parent resolvers)
	 */
	@SuppressWarnings("unchecked")
	default <T> void emitText(DataRender render, DataContexts parentContexts) {
		DataEmittable<T> dsl = (DataEmittable<T>) dataEmitter().dsl();

		dsl.emit(render, (T) this, parentContexts);
	}

	/**
	 * Renders a standalone text snapshot at the default detail level.
	 *
	 * @return immutable text snapshot
	 */
	default Text toText() {
		return toText(Detail.DEFAULT);
	}

	/**
	 * Renders a standalone text snapshot at the specified detail level.
	 *
	 * @param detail the detail level
	 * @return immutable text snapshot
	 */
	@SuppressWarnings("unchecked")
	default <T> Text toText(Detail detail) {
		return DEFAULT_RENDERER.render((DataEmitter<T>) dataEmitter(), (T) this);
	}

	/**
	 * Returns the data emitter definition for this object.
	 *
	 * @return the data emitter definition
	 */
	DataEmitter<?> dataEmitter();
}
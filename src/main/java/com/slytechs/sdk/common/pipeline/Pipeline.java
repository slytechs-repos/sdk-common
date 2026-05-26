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
package com.slytechs.sdk.common.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.slytechs.sdk.common.util.Named;
import com.slytechs.sdk.common.util.Prioritizable;

/**
 * The pipeline is single-threaded stateless.
 *
 * Pipelines are allocated 1 per every thread/port that needs processing.
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class Pipeline<T extends PipelineContext>
		implements Named {
	public static final String DEFAULT_NAME = "pipeline";

	public class PipelineBuilder<C extends PipelineContext> {

		private List<PipelineProcessor<C>> list = new ArrayList<>();

		private String name = DEFAULT_NAME;

		private Supplier<C> contextFactory;

		private long availableFeatures;

		public PipelineBuilder<C> usingFeatureMask(long featureMask) {
			this.availableFeatures = featureMask;
			
			return this;
		}

		public PipelineBuilder<C> addStage(PipelineProcessor<C> processor) {
			list.add(processor);

			return this;
		}

		public PipelineBuilder<C> addStageIf(long featureMask, PipelineProcessor<C> processor) {
			if ((availableFeatures & featureMask) != 0)
				list.add(processor);

			return this;
		}

		public PipelineBuilder<C> addStageIf(long featureMask, PipelineProcessor<C> processor,
				PipelineProcessor<C> orElse) {
			if ((availableFeatures & featureMask) != 0)
				list.add(processor);
			else
				list.add(orElse);

			return this;
		}

		public PipelineBuilder<C> withName(String name) {
			this.name = name;
			return this;
		}

		public PipelineBuilder<C> usingContext(Supplier<C> contextFactory) {
			this.contextFactory = contextFactory;
			return this;
		}

		public Pipeline<C> build() {
			Collections.sort(list, Prioritizable.LOW_HIGH_COMPARATOR);
			PipelineProcessor<C> head = linkProcessors(this.list);

			return new Pipeline<C>(name, head, contextFactory);
		}

		private PipelineProcessor<C> linkProcessors(List<PipelineProcessor<C>> list) {
			if (list.isEmpty())
				throw new IllegalStateException("Pipeline %s, has no processors defined"
						.formatted(name));

			PipelineProcessor<C> last = null, next = null, head = null;

			for (int i = 0; i < list.size(); i++) {
				next = list.get(i);

				if (last == null)
					head = next;
				else
					last.setNext(next);

				last = next;
			}

			return head;
		}
	}

	private final String name;
	protected final T context;
	private final PipelineProcessor<T> head;

	public Pipeline(String name, PipelineProcessor<T> head, Supplier<T> context) {
		this.name = name;
		this.head = head;
		this.context = context.get();
	}

	/**
	 * @see com.slytechs.sdk.common.util.Named#name()
	 */
	@Override
	public String name() {
		return name;
	}

	public void reset() {
		this.context.reset();
	}

	protected void processChain() {

		PipelineProcessor<T> next = head;

		// Each chain link returns the next link or null if last,
		// all state stored in shared context
		while (next != null)
			next = next.process(context);
	}

}

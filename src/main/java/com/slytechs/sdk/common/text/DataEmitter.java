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

import java.util.HashMap;
import java.util.Map;

import com.slytechs.sdk.common.text.format.BitFormat;
import com.slytechs.sdk.common.text.format.FormatPattern;
import com.slytechs.sdk.common.text.format.Macro;
import com.slytechs.sdk.common.text.format.TextFormat;

/**
 * Builder for data-aware text output with protocol field semantics. Accumulates
 * an emitter lambda chain during construction that is executed at emit time
 * against a target object and optional external context.
 *
 * <p>
 * Each DataEmitter instance builds a {@link DataResolver} from its registered
 * field refs. At emit time, the resolver is injected into the
 * {@link DataContexts} and composed with any existing resolver (e.g., from a
 * parent protocol). Templates pull resolved values from the context's resolver
 * chain.
 *
 * <p>
 * Fields and meta elements sourced from a {@link DataContexts} are automatically
 * skipped when the context type is not available.
 *
 * @param <T> the target type (e.g., Packet, Ip4 header)
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class DataEmitter<T> {

	@SuppressWarnings("unchecked")
	interface DataRef {
		static <T> DataRef of(Arg.Get<T> getter) {
			return (ctx, t) -> getter.get((T) t);
		}

		static <T, C> DataRef ofCtx(Class<C> ctxType, Arg.Get<C> getter) {
			return new DataRef() {
				@Override
				public Object resolve(Object ctx, Object target) {
					return getter.get((C) ctx);
				}

				@Override
				public Class<?> contextClass() {
					return ctxType;
				}
			};
		}

		static <T, C> DataRef ofCtx2(Class<C> ctxType, Arg.Get2<C, T> getter) {
			return new DataRef() {
				@Override
				public Object resolve(Object ctx, Object target) {
					return getter.get((C) ctx, (T) target);
				}

				@Override
				public Class<?> contextClass() {
					return ctxType;
				}
			};
		}

		Object resolve(Object ctx, Object target);

		default Class<?> contextClass() {
			return null;
		}
	}

	public interface DataContexts {
		DataContexts EMPTY = cl -> null;

		static DataContexts of(Object context) {
			return cl -> cl.isAssignableFrom(context.getClass())
					? context
					: null;
		}

		static DataContexts of(Object... contexts) {
			return cl -> {
				for (Object ctx : contexts)
					if (cl.isAssignableFrom(ctx.getClass()))
						return ctx;
				return null;
			};
		}

		Object get(Class<?> contextType);

		@SuppressWarnings("unchecked")
		default <C> C getTyped(Class<C> contextType) {
			return (C) get(contextType);
		}

		default <C> Object getValue(Class<C> contextType, Arg.Get<C> value) {
			var ctx = getTyped(contextType);
			if (ctx == null)
				return null;

			return value.get(ctx);
		}

		default <C> Object getValue(Class<C> contextType, Arg.Get<C> value, Object defaultValue) {
			var ctx = getTyped(contextType);
			if (ctx == null)
				return defaultValue;

			var res = value.get(ctx);
			if (res == null)
				return defaultValue;

			return res;
		}

		/**
		 * Combines this context with another. This context is checked first.
		 *
		 * @param other the fallback context
		 * @return a combined context
		 */
		default DataContexts combine(DataContexts other) {
			return (Class<?> cl) -> {
				var ret = get(cl);
				if (ret != null)
					return ret;
				return other.get(cl);
			};
		}

		/**
		 * Creates a new context with an additional binding. The new binding takes
		 * priority over any existing binding for the same type.
		 *
		 * @param binding the object to add
		 * @return a new context with the binding
		 */
		default DataContexts with(Object binding) {
			return combine(DataContexts.of(binding));
		}
	}

	@FunctionalInterface
	public interface DataEmittable<T> {
		DataRender emit(DataRender emitter, T target, DataContexts contexts);
	}

	public interface DataRender {
		int depth();

		Detail detail();

		DataRender bitfield(String label, String bitPattern, long maskedValue, String formattedValue);

		DataRender field(String label, String value);

		DataRender line(String line);

		DataRender meta(String label, String value);

		DataRender mime(String type);

		DataRender pop();

		DataRender push();

		DataRender row(String line);

		DataRender summary(String line);
	}

	@FunctionalInterface
	public interface DataSection<T> {
		DataEmitter<T> newSection(DataEmitter<T> parent);
	}

	public static <T> DataEmitter<T> of(String template, DataSection<T> section) {
		var text = new DataEmitter<T>();
		text.detail(Detail.DEFAULT, template, section);

		return text;
	}

	private final TextFormat textFormat;
	private final BitFormat bitFormat;
	private final Map<String, DataRef> refs = new HashMap<>();
	private final java.util.Set<String> unresolved = new java.util.LinkedHashSet<>();
	protected DataEmittable<T> dsl = (e, t, c) -> e;

	private static final DataRef UNLINKED = (ctx, t) -> null;

	public DataEmitter() {
		this.textFormat = new TextFormat();
		this.bitFormat = new BitFormat();
	}

	public DataEmitter(TextFormat textFormat, BitFormat bitFormat) {
		this.textFormat = textFormat;
		this.bitFormat = bitFormat;
	}

	DataEmitter(DataEmitter<T> parent) {
		this.textFormat = parent.textFormat;
		this.bitFormat = parent.bitFormat;
	}

	public TextFormat textFormat() {
		return textFormat;
	}

	public BitFormat bitFormat() {
		return bitFormat;
	}

	public DataEmittable<T> dsl() {
		return dsl;
	}

	/**
	 * Builds a {@link DataResolver} from this instance's registered refs, bound to
	 * the given target and context. The resolver handles both target-sourced and
	 * context-sourced refs.
	 */
	DataResolver buildResolver(T target, DataContexts contexts) {
		return name -> {
			DataRef ref = refs.get(name);
			if (ref == null || ref == UNLINKED)
				return null;

			Class<?> ctxClass = ref.contextClass();
			if (ctxClass != null) {
				Object ctx = contexts.get(ctxClass);
				if (ctx == null)
					return null;
				return ref.resolve(ctx, target);
			}

			return ref.resolve(null, target);
		};
	}

	/**
	 * Gets the resolver from context, combining this instance's resolver with any
	 * existing resolver in the context chain.
	 */
	private DataResolver contextResolver(T target, DataContexts contexts) {
		DataResolver mine = buildResolver(target, contexts);
		DataResolver existing = contexts.getTyped(DataResolver.class);

		if (existing != null)
			return mine.combine(existing);

		return mine;
	}

	/**
	 * Creates a new context with this instance's resolver injected, composed with
	 * any existing resolver.
	 */
	public DataContexts injectResolver(T target, DataContexts contexts) {
		DataResolver combined = contextResolver(target, contexts);
		return contexts.with(combined);
	}

	private void registerRef(String name, DataRef ref) {
		if (name != null) {
			refs.put(name, ref);
			unresolved.remove(name);
		}
	}

	/**
	 * Ensures a ref stub exists for the given name. Called during template
	 * compilation so that forward references are available for resolver
	 * composition. The stub will be replaced when the actual field is defined
	 * via {@link #registerRef}.
	 *
	 * @param name the reference name
	 */
	void ensureRef(String name) {
		if (name != null && !refs.containsKey(name)) {
			refs.put(name, UNLINKED);
			unresolved.add(name);
		}
	}

	/**
	 * Resolves a template against the context's resolver chain.
	 */
	private String resolveTemplate(Template tmpl, T target, DataContexts contexts) {
		DataResolver resolver = contextResolver(target, contexts);
		return tmpl.format(resolver::resolve);
	}

	// --- Structural ---

	/**
	 * Appends a custom DSL step to the chain. Used for dynamic delegation
	 * where the emission target is not known at DSL construction time, such
	 * as iterating protocol headers in a packet.
	 *
	 * @param custom the custom DSL step
	 * @return this builder
	 */
	public DataEmitter<T> delegate(DataEmittable<T> custom) {
		DataEmittable<T> prev = this.dsl;
		this.dsl = (e, t, c) -> custom.emit(prev.emit(e, t, c), t, c);
		return this;
	}

	@SuppressWarnings("unchecked")
	public <U> DataEmitter<T> attach(DataEmitter<U> other) {
		DataEmittable<T> otherDsl = (DataEmittable<T>) other.dsl;
		DataEmittable<T> prev = this.dsl;

		this.dsl = (e, t, c) -> otherDsl.emit(prev.emit(e, t, c), t, c);

		return this;
	}

	public DataEmitter<T> detail(Detail detail, String summary, DataSection<T> section) {
		DataEmitter<T> child = section.newSection(new DataEmitter<>(this));
		Template summaryTmpl = Template.compile(textFormat, summary, child::ensureRef);
		DataEmittable<T> prev = this.dsl;

		this.dsl = (e, t, c) -> {
			e = prev.emit(e, t, c);

			if (detail.isVisible(e.detail())) {
				// Inject child's resolver into context for summary resolution
				DataContexts enriched = child.injectResolver(t, c);

				e.summary(summaryTmpl.format(
						child.contextResolver(t, enriched)::resolve));
				e.push();

				try {
					return child.dsl.emit(e, t, enriched);
				} finally {
					e.pop();
				}
			}

			return e;
		};

		return child;
	}

	public DataEmitter<T> section(String summary, DataSection<T> section) {
		return detail(Detail.DEFAULT, summary, section);
	}

	// --- Line / Row / Summary ---

	public DataEmitter<T> line(String format) {
		Template tmpl = Template.compile(textFormat, format, this::ensureRef);
		DataEmittable<T> prev = this.dsl;

		this.dsl = (e, t, c) -> prev.emit(e, t, c)
				.line(resolveTemplate(tmpl, t, c));

		return this;
	}

	public DataEmitter<T> line(Arg.Str<T> line) {
		DataEmittable<T> prev = this.dsl;

		this.dsl = (e, t, c) -> prev.emit(e, t, c)
				.line(line.toString(t));

		return this;
	}

	public DataEmitter<T> row(String format) {
		Template tmpl = Template.compile(textFormat, format, this::ensureRef);
		DataEmittable<T> prev = this.dsl;

		this.dsl = (e, t, c) -> prev.emit(e, t, c)
				.row(resolveTemplate(tmpl, t, c));

		return this;
	}

	public DataEmitter<T> summary(String format) {
		Template tmpl = Template.compile(textFormat, format, this::ensureRef);
		DataEmittable<T> prev = this.dsl;

		this.dsl = (e, t, c) -> prev.emit(e, t, c)
				.summary(resolveTemplate(tmpl, t, c));

		return this;
	}

	// --- Macro registration ---

	public DataEmitter<T> macro(String name, Macro macro) {
		textFormat.setMacro(name, macro);
		bitFormat.setMacro(name, macro);
		return this;
	}

	public DataEmitter<T> macro(String name, Macro.Named... namedMacros) {
		for (Macro.Named nm : namedMacros) {
			String fullName = name + "." + nm.suffix();
			macro(fullName, nm.macro());
		}
		return this;
	}

	// --- Field: label + value from target T ---

	public DataEmitter<T> field(String label, Arg.Get<T> value) {
		DataEmittable<T> prev = this.dsl;

		this.dsl = (e, t, c) -> {
			e = prev.emit(e, t, c);
			Object val = value.get(t);
			return e.field(label, String.valueOf(val));
		};

		return this;
	}

	public DataEmitter<T> field(String label, Arg.Get<T> value, String name) {
		registerRef(name, DataRef.of(value));
		DataEmittable<T> prev = this.dsl;

		this.dsl = (e, t, c) -> {
			e = prev.emit(e, t, c);
			Object val = value.get(t);
			return e.field(label, String.valueOf(val));
		};

		return this;
	}

	public DataEmitter<T> field(String label, String format, Arg.Get<T> value) {
		FormatPattern pattern = textFormat.compile(format);
		DataEmittable<T> prev = this.dsl;

		this.dsl = (e, t, c) -> {
			e = prev.emit(e, t, c);
			Object val = value.get(t);
			return e.field(label, pattern.format(val));
		};

		return this;
	}

	public DataEmitter<T> field(String label, String format, Arg.Get<T> value, String name) {
		registerRef(name, DataRef.of(value));
		Template tmpl = Template.compile(textFormat, format, this::ensureRef);
		DataEmittable<T> prev = this.dsl;

		this.dsl = (e, t, c) -> {
			e = prev.emit(e, t, c);
			return e.field(label, resolveTemplate(tmpl, t, c));
		};

		return this;
	}

	public DataEmitter<T> field(String label, Arg.Get<T> value, String name, DataSection<T> section) {
		registerRef(name, DataRef.of(value));
		DataEmitter<T> child = section.newSection(new DataEmitter<>(this));
		Template labelTmpl = Template.compile(textFormat, label, n -> {
			ensureRef(n);
			child.ensureRef(n);
		});
		DataEmittable<T> prev = this.dsl;

		this.dsl = (e, t, c) -> {
			e = prev.emit(e, t, c);

			DataResolver myResolver = buildResolver(t, c);
			DataResolver childResolver = child.buildResolver(t, c);
			DataResolver combined = myResolver.combine(childResolver);
			DataResolver existing = c.getTyped(DataResolver.class);
			if (existing != null)
				combined = combined.combine(existing);

			DataContexts enriched = c.with(combined);

			e.field(labelTmpl.format(combined::resolve), "");
			e.push();

			try {
				return child.dsl.emit(e, t, enriched);
			} finally {
				e.pop();
			}
		};

		return this;
	}

	public DataEmitter<T> field(String label, String format) {
		Template tmpl = Template.compile(textFormat, format, this::ensureRef);
		DataEmittable<T> prev = this.dsl;

		this.dsl = (e, t, c) -> {
			e = prev.emit(e, t, c);
			return e.field(label, resolveTemplate(tmpl, t, c));
		};

		return this;
	}

	

	// --- Field: from external context C ---

	public <C> DataEmitter<T> field(String label, Class<C> ctx, Arg.Get<C> value, String name) {
		registerRef(name, DataRef.ofCtx(ctx, value));
		DataEmittable<T> prev = this.dsl;

		this.dsl = (e, t, c) -> {
			e = prev.emit(e, t, c);
			C context = c.getTyped(ctx);
			if (context == null)
				return e;
			Object val = value.get(context);
			if (val == null)
				return e;
			return e.field(label, String.valueOf(val));
		};

		return this;
	}

	public <C> DataEmitter<T> field(String label, Class<C> ctx, Arg.Get2<C, T> value, String name) {
		registerRef(name, DataRef.ofCtx2(ctx, value));
		DataEmittable<T> prev = this.dsl;

		this.dsl = (e, t, c) -> {
			e = prev.emit(e, t, c);
			C context = c.getTyped(ctx);
			if (context == null)
				return e;
			Object val = value.get(context, t);
			if (val == null)
				return e;
			return e.field(label, String.valueOf(val));
		};

		return this;
	}

	// --- Bit format field ---

	public DataEmitter<T> bits(String bitFormatStr, Arg.Get<T> value) {
		FormatPattern.Line pattern = bitFormat.compileLine(bitFormatStr);
		DataEmittable<T> prev = this.dsl;

		this.dsl = (e, t, c) -> {
			e = prev.emit(e, t, c);
			Object val = value.get(t);
			if (val instanceof Number n)
				return e.line(pattern.format(n.longValue()));
			return e.line(pattern.format(val));
		};

		return this;
	}

	public DataEmitter<T> bits(String bitFormatStr, Arg.Get<T> value, String name) {
		registerRef(name, DataRef.of(value));
		FormatPattern.Line pattern = bitFormat.compileLine(bitFormatStr);
		DataEmittable<T> prev = this.dsl;

		this.dsl = (e, t, c) -> {
			e = prev.emit(e, t, c);
			Object val = value.get(t);
			if (val instanceof Number n)
				return e.line(pattern.format(n.longValue()));
			return e.line(pattern.format(val));
		};

		return this;
	}

	// --- Meta: from target T ---

	public DataEmitter<T> meta(String label, Arg.Get<T> value) {
		DataEmittable<T> prev = this.dsl;

		this.dsl = (e, t, c) -> {
			e = prev.emit(e, t, c);
			Object val = value.get(t);
			return e.meta(label, String.valueOf(val));
		};

		return this;
	}

	public DataEmitter<T> meta(String label, Arg.Get<T> value, String name) {
		registerRef(name, DataRef.of(value));
		DataEmittable<T> prev = this.dsl;

		this.dsl = (e, t, c) -> {
			e = prev.emit(e, t, c);
			Object val = value.get(t);
			return e.meta(label, String.valueOf(val));
		};

		return this;
	}

	public DataEmitter<T> meta(String label, String staticValue) {
		DataEmittable<T> prev = this.dsl;

		this.dsl = (e, t, c) -> prev.emit(e, t, c)
				.meta(label, staticValue);

		return this;
	}

	// --- Meta: from external context C ---

	public <C> DataEmitter<T> meta(String label, Class<C> ctx, Arg.Get<C> value) {
		DataEmittable<T> prev = this.dsl;

		this.dsl = (e, t, c) -> {
			e = prev.emit(e, t, c);
			C context = c.getTyped(ctx);
			if (context == null)
				return e;
			Object val = value.get(context);
			if (val == null)
				return e;
			return e.meta(label, String.valueOf(val));
		};

		return this;
	}

	public <C> DataEmitter<T> meta(String label, Class<C> ctx, Arg.Get<C> value, String name) {
		registerRef(name, DataRef.ofCtx(ctx, value));
		DataEmittable<T> prev = this.dsl;

		this.dsl = (e, t, c) -> {
			e = prev.emit(e, t, c);
			C context = c.getTyped(ctx);
			if (context == null)
				return e;
			Object val = value.get(context);
			if (val == null)
				return e;
			return e.meta(label, String.valueOf(val));
		};

		return this;
	}

	public <C> DataEmitter<T> meta(String label, Class<C> ctx, Arg.Get2<C, T> value, String name) {
		registerRef(name, DataRef.ofCtx2(ctx, value));
		DataEmittable<T> prev = this.dsl;

		this.dsl = (e, t, c) -> {
			e = prev.emit(e, t, c);
			C context = c.getTyped(ctx);
			if (context == null)
				return e;
			Object val = value.get(context, t);
			if (val == null)
				return e;
			return e.meta(label, String.valueOf(val));
		};

		return this;
	}

	/**
	 * @param string
	 * @param string2
	 * @param i
	 * @param j
	 * @param object
	 * @return
	 */
	public DataEmitter<T> bitfield(String bitFormatStr, String name, long bitOffset, long bitLength, Arg.Get<T> getter) {
		registerRef(name, DataRef.of(getter));
		FormatPattern.Line pattern = bitFormat.compileLine(bitFormatStr);
		DataEmittable<T> prev = this.dsl;

		this.dsl = (e, t, c) -> {
			e = prev.emit(e, t, c);
			Object val = getter.get(t);
			long raw = (val instanceof Number n) ? n.longValue() : 0L;
			String formatted = pattern.format(raw);
			return e.bitfield(formatted, "", raw, formatted);
		};

		return this;
	}
}
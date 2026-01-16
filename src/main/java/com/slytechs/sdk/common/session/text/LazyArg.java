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
package com.slytechs.sdk.common.session.text;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * A lazy argument wrapper that captures a snapshot at creation time and
 * supports dynamic evaluation with graceful fallback.
 * 
 * <p>
 * LazyArg enables deferred evaluation of values in log messages and diagnostic
 * output. When created, it immediately captures a snapshot of the current
 * value. On subsequent evaluations, it attempts to get the current value from
 * the supplier, falling back to the snapshot if the supplier throws an
 * exception or if the LazyArg has been frozen.
 * </p>
 * 
 * <p>
 * The snapshot serves multiple purposes:
 * <ul>
 * <li>Provides a baseline for transition rendering (showing change from initial
 * to current state)</li>
 * <li>Acts as a fallback when the supplier becomes invalid (e.g., referenced
 * object is garbage collected or session is closed)</li>
 * <li>Ensures diagnostic output always produces meaningful values</li>
 * </ul>
 * </p>
 * 
 * <p>
 * When a LazyArg is frozen (typically when its parent session terminates), all
 * subsequent evaluations return the snapshot value, preventing stale lambda
 * references from producing incorrect results.
 * </p>
 *
 * @param <T> the type of value this LazyArg holds
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public final class LazyArg<T> {

	/**
	 * Controls how {@link LazyArg} and {@link MessageRecord} values are rendered.
	 * 
	 * <p>
	 * The render mode determines what information is included in the output:
	 * <ul>
	 * <li>{@link #CURRENT} - Shows only the current value</li>
	 * <li>{@link #SNAPSHOT} - Shows only the snapshot (initial) value</li>
	 * <li>{@link #TRANSITION} - Shows the change from snapshot to current</li>
	 * </ul>
	 * </p>
	 *
	 * @author Mark Bednarczyk [mark@slytechs.com]
	 * @author Sly Technologies Inc.
	 */
	public enum Mode {

		/**
		 * Render only the current value.
		 * <p>
		 * Example output: {@code "draining 3 packets"}
		 * </p>
		 */
		CURRENT,

		/**
		 * Render only the snapshot (initial) value.
		 * <p>
		 * Example output: {@code "draining 15 packets"}
		 * </p>
		 */
		SNAPSHOT,

		/**
		 * Render the transition from snapshot to current value.
		 * <p>
		 * If the values are equal, shows just the current value. If different, shows
		 * the transition: {@code "(15→3)"}
		 * </p>
		 * <p>
		 * Example output: {@code "draining (15→3) packets"}
		 * </p>
		 */
		TRANSITION
	}

	private final Supplier<T> supplier;
	private final T snapshot;
	private final AtomicBoolean frozen;

	private LazyArg(Supplier<T> supplier, T snapshot, AtomicBoolean frozen) {
		this.supplier = Objects.requireNonNull(supplier, "supplier");
		this.snapshot = snapshot;
		this.frozen = Objects.requireNonNull(frozen, "frozen");
	}

	/**
	 * Creates a new LazyArg with the given supplier, capturing an immediate
	 * snapshot. The frozen flag is shared with the parent MessageRecord.
	 *
	 * @param <T>      the type of value
	 * @param supplier the supplier for dynamic value retrieval
	 * @param frozen   the shared frozen flag from the parent MessageRecord
	 * @return a new LazyArg instance
	 */
	public static <T> LazyArg<T> of(Supplier<T> supplier, AtomicBoolean frozen) {
		T initialValue = safeEval(supplier);
		return new LazyArg<>(supplier, initialValue, frozen);
	}

	/**
	 * Creates a new LazyArg with the given supplier, using a dedicated frozen flag.
	 * This is useful for standalone LazyArgs not associated with a MessageRecord.
	 *
	 * @param <T>      the type of value
	 * @param supplier the supplier for dynamic value retrieval
	 * @return a new LazyArg instance with its own frozen flag
	 */
	public static <T> LazyArg<T> of(Supplier<T> supplier) {
		return of(supplier, new AtomicBoolean(false));
	}

	private static <T> T safeEval(Supplier<T> supplier) {
		try {
			return supplier.get();
		} catch (Throwable e) {
			return null;
		}
	}

	/**
	 * Returns the current value from the supplier, or the snapshot if frozen or if
	 * evaluation fails.
	 *
	 * @return the current value or snapshot fallback
	 */
	public T current() {
		if (frozen.get()) {
			return snapshot;
		}
		try {
			return supplier.get();
		} catch (Throwable e) {
			return snapshot;
		}
	}

	/**
	 * Returns the snapshot value captured at creation time.
	 *
	 * @return the snapshot value
	 */
	public T snapshot() {
		return snapshot;
	}

	/**
	 * Checks if the current value differs from the snapshot.
	 *
	 * @return true if the value has changed since creation
	 */
	public boolean hasChanged() {
		return !Objects.equals(snapshot, current());
	}

	/**
	 * Checks if this LazyArg is frozen.
	 *
	 * @return true if frozen (will always return snapshot)
	 */
	public boolean isFrozen() {
		return frozen.get();
	}

	/**
	 * Renders the value according to the specified mode.
	 *
	 * @param mode the render mode
	 * @return the rendered string representation
	 */
	public String render(Mode mode) {
		if (frozen.get() && mode != Mode.SNAPSHOT) {
			return String.valueOf(snapshot) + "(frozen)";
		}

		T now = current();

		return switch (mode) {
		case CURRENT -> String.valueOf(now);
		case SNAPSHOT -> String.valueOf(snapshot);
		case TRANSITION -> {
			if (Objects.equals(snapshot, now)) {
				yield String.valueOf(now);
			} else {
				yield "(" + snapshot + "→" + now + ")";
			}
		}
		};
	}

	@Override
	public String toString() {
		return render(Mode.TRANSITION);
	}
}
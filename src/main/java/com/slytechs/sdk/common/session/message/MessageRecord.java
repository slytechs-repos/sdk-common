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
package com.slytechs.sdk.common.session.message;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * A log record that supports SLF4J-style message templates with lazy argument
 * evaluation and snapshot-based transitions.
 * 
 * <p>
 * MessageRecord provides deferred message composition where arguments are only
 * evaluated when the message is rendered. This is particularly useful for
 * diagnostic messages in session management where:
 * <ul>
 * <li>The message may never be rendered (performance optimization)</li>
 * <li>Arguments may change between registration and rendering</li>
 * <li>Transition tracking is needed (showing change from initial to current state)</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Messages use SLF4J-style {@code {}} placeholders:
 * <pre>{@code
 * MessageRecord record = MessageRecord.of("draining {} packets from {}", 
 *     lazy(queue::size), 
 *     channelName);
 * 
 * // Later, when rendered with TRANSITION mode:
 * // "draining (15→3) packets from hello-channel"
 * }</pre>
 * </p>
 * 
 * <p>
 * When a MessageRecord is frozen (typically when its parent session terminates),
 * all lazy arguments return their snapshot values, preventing stale references
 * from producing incorrect or exception-throwing results.
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public final class MessageRecord {

	private static final String PLACEHOLDER = "{}";

	private final String template;
	private final Object[] args;
	private final AtomicBoolean frozen = new AtomicBoolean(false);

	private MessageRecord(String template, Object[] args) {
		this.template = Objects.requireNonNull(template, "template");
		this.args = args != null ? args.clone() : new Object[0];
	}

	/**
	 * Creates a new MessageRecord with the given template and arguments.
	 * 
	 * <p>
	 * Arguments can be:
	 * <ul>
	 * <li>Concrete values - rendered as-is</li>
	 * <li>{@link Supplier} - converted to {@link LazyArg} automatically</li>
	 * <li>{@link LazyArg} - used directly</li>
	 * </ul>
	 * </p>
	 *
	 * @param template the message template with {@code {}} placeholders
	 * @param args     the arguments to substitute
	 * @return a new MessageRecord instance
	 */
	public static MessageRecord of(String template, Object... args) {
		MessageRecord record = new MessageRecord(template, args);
		record.wrapSuppliers();
		return record;
	}

	private void wrapSuppliers() {
		for (int i = 0; i < args.length; i++) {
			if (args[i] instanceof Supplier<?> supplier && !(args[i] instanceof LazyArg<?>)) {
				args[i] = LazyArg.of(supplier, frozen);
			}
		}
	}

	/**
	 * Creates a {@link LazyArg} bound to this MessageRecord's frozen flag.
	 * 
	 * <p>
	 * Use this method when you need explicit control over lazy argument creation:
	 * <pre>{@code
	 * MessageRecord record = MessageRecord.of("processing {} items", 
	 *     record.lazy(queue::size));
	 * }</pre>
	 * </p>
	 *
	 * @param <T>      the type of value
	 * @param supplier the value supplier
	 * @return a LazyArg bound to this record's frozen flag
	 */
	public <T> LazyArg<T> lazy(Supplier<T> supplier) {
		return LazyArg.of(supplier, frozen);
	}

	/**
	 * Renders the message with current values only.
	 *
	 * @return the rendered message
	 */
	public String render() {
		return render(RenderMode.CURRENT);
	}

	/**
	 * Renders the message according to the specified mode.
	 *
	 * @param mode the render mode
	 * @return the rendered message
	 */
	public String render(RenderMode mode) {
		StringBuilder result = new StringBuilder();
		int argIndex = 0;
		int searchStart = 0;

		while (searchStart < template.length()) {
			int placeholderPos = template.indexOf(PLACEHOLDER, searchStart);

			if (placeholderPos < 0) {
				result.append(template.substring(searchStart));
				break;
			}

			result.append(template.substring(searchStart, placeholderPos));

			if (argIndex < args.length) {
				result.append(resolveArg(args[argIndex], mode));
				argIndex++;
			} else {
				result.append(PLACEHOLDER);
			}

			searchStart = placeholderPos + PLACEHOLDER.length();
		}

		return result.toString();
	}

	private String resolveArg(Object arg, RenderMode mode) {
		if (arg instanceof LazyArg<?> lazy) {
			return lazy.render(mode);
		}
		return String.valueOf(arg);
	}

	/**
	 * Freezes this MessageRecord, causing all lazy arguments to return their
	 * snapshot values on subsequent renders.
	 * 
	 * <p>
	 * This is typically called when the parent session terminates, ensuring
	 * that diagnostic output remains valid even after the session's objects
	 * are no longer accessible.
	 * </p>
	 */
	public void freeze() {
		frozen.set(true);
	}

	/**
	 * Checks if this MessageRecord is frozen.
	 *
	 * @return true if frozen
	 */
	public boolean isFrozen() {
		return frozen.get();
	}

	/**
	 * Returns the message template.
	 *
	 * @return the template string
	 */
	public String template() {
		return template;
	}

	/**
	 * Returns the number of arguments.
	 *
	 * @return the argument count
	 */
	public int argCount() {
		return args.length;
	}

	@Override
	public String toString() {
		return render(RenderMode.TRANSITION);
	}
}
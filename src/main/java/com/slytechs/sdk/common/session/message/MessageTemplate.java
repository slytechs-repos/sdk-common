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

import java.util.function.Supplier;

/**
 * Factory utilities for creating {@link MessageRecord} instances with lazy arguments.
 * 
 * <p>
 * This class provides static imports for convenient message creation:
 * <pre>{@code
 * import static com.slytechs.sdk.common.session.message.MessageTemplate.*;
 * 
 * // Create a log record with lazy evaluation
 * MessageRecord record = of("draining {} packets from {}", 
 *     lazy(queue::size),    // Evaluated on render
 *     channelName);         // Concrete value
 * 
 * // Render with transitions
 * String output = record.render(RenderMode.TRANSITION);
 * // "draining (15→3) packets from hello-channel"
 * }</pre>
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public final class MessageTemplate {

	private MessageTemplate() {
		// Static utility class
	}

	/**
	 * Creates a new MessageRecord with the given template and arguments.
	 * 
	 * <p>
	 * This is a convenience method for {@link MessageRecord#of(String, Object...)}.
	 * </p>
	 *
	 * @param template the message template with {@code {}} placeholders
	 * @param args     the arguments to substitute
	 * @return a new MessageRecord instance
	 */
	public static MessageRecord of(String template, Object... args) {
		return MessageRecord.of(template, args);
	}

	/**
	 * Wraps a supplier as a lazy argument.
	 * 
	 * <p>
	 * The supplier is not evaluated until the MessageRecord is rendered. A snapshot
	 * of the initial value is captured when the MessageRecord is created.
	 * </p>
	 *
	 * @param <T>      the type of value
	 * @param supplier the value supplier
	 * @return a Supplier that will be converted to LazyArg when added to MessageRecord
	 */
	public static <T> Supplier<T> lazy(Supplier<T> supplier) {
		return supplier;
	}

	/**
	 * Creates a lazy argument that captures the snapshot immediately.
	 * 
	 * <p>
	 * This is useful when you want explicit control over the LazyArg creation
	 * outside of a MessageRecord context.
	 * </p>
	 *
	 * @param <T>      the type of value
	 * @param supplier the value supplier
	 * @return a LazyArg with captured snapshot
	 */
	public static <T> LazyArg<T> lazyArg(Supplier<T> supplier) {
		return LazyArg.of(supplier);
	}

	/**
	 * Creates a simple MessageRecord with no placeholders.
	 *
	 * @param message the static message
	 * @return a new MessageRecord instance
	 */
	public static MessageRecord message(String message) {
		return MessageRecord.of(message);
	}
}
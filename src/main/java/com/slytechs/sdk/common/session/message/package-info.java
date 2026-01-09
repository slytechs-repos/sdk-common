/*
 * Sly Technologies Free License
 * 
 * Copyright 2025 Sly Technologies Inc.
 *
 * Licensed under the Sly Technologies Free License (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.slytechs.com/free-license-text
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * Message templating with lazy argument evaluation and snapshot transitions.
 * 
 * <p>
 * This package provides SLF4J-style message composition with deferred evaluation
 * and transition tracking for session diagnostics.
 * </p>
 * 
 * <p>
 * Key classes:
 * <ul>
 * <li>{@link com.slytechs.sdk.common.session.message.MessageRecord} - Message template with args</li>
 * <li>{@link com.slytechs.sdk.common.session.message.LazyArg} - Lazy-evaluated argument with snapshot</li>
 * <li>{@link com.slytechs.sdk.common.session.message.MessageTemplate} - Factory utilities</li>
 * <li>{@link com.slytechs.sdk.common.session.message.RenderMode} - Output format control</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Example usage:
 * <pre>{@code
 * import static com.slytechs.sdk.common.session.message.MessageTemplate.*;
 * 
 * MessageRecord record = of("draining {} packets from {}",
 *     lazy(queue::size),
 *     channelName);
 * 
 * // Render with transitions: "draining (15→3) packets from hello-channel"
 * String output = record.render(RenderMode.TRANSITION);
 * }</pre>
 * </p>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
package com.slytechs.sdk.common.session.message;
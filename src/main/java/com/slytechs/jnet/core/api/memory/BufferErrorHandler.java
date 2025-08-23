/*
 * Sly Technologies Free License
 * 
 * Copyright 2024 Sly Technologies Inc.
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
package com.slytechs.jnet.core.api.memory;

/**
 * Functional interface for handling buffer errors with type-safe buffer access.
 * 
 * @param <T> the specific buffer type being handled
 */
@FunctionalInterface
public interface BufferErrorHandler<T extends MemoryBuffer> {
	/**
	 * Handles a buffer operation error.
	 * 
	 * @param buf the buffer where the error occurred
	 * @param e   the exception describing the error
	 * @throws BufferOperationException if the error cannot be recovered
	 */
	void handle(T buf, BufferOperationException e) throws BufferOperationException;
}
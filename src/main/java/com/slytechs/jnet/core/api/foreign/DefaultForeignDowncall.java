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
package com.slytechs.jnet.core.api.foreign;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

/**
 * The Class DefaultForeignDowncall.
 *
 * @author Mark Bednarczyk
 */
public class DefaultForeignDowncall extends ForeignDowncall<ForeignIllegalStateException> {

	/**
	 * Instantiates a new default foreign downcall.
	 *
	 * @param symbolName    the symbol name
	 * @param symbolAddress the symbol address
	 * @param handle        the handle
	 */
	public DefaultForeignDowncall(String symbolName, MemorySegment symbolAddress, MethodHandle handle) {
		super(symbolName, symbolAddress, handle, ForeignIllegalStateException::new);
	}

	/**
	 * Instantiates a new default foreign downcall.
	 *
	 * @param symbolName the symbol name
	 * @param cause      the cause
	 */
	public DefaultForeignDowncall(String symbolName, Throwable cause) {
		super(symbolName, cause);
	}

	/**
	 * Instantiates a new default foreign downcall.
	 *
	 * @param symbolName the symbol name
	 */
	public DefaultForeignDowncall(String symbolName) {
		super(symbolName);
	}

}

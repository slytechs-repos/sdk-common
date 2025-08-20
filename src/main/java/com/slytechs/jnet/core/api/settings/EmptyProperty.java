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
package com.slytechs.jnet.core.api.settings;

import java.util.function.Function;

final class EmptyProperty<T, T_BASE extends Property<T, T_BASE>> extends Property<T, T_BASE> {

	EmptyProperty(String name) {
		super(name);
	}

	/**
	 * @see com.slytechs.jnet.platform.api.common.settings.Property#getValue()
	 */
	@Override
	public T getValue() {
		throw new UnsupportedOperationException("");
	}

	/**
	 * @see com.slytechs.jnet.platform.api.common.settings.Property#map(java.util.function.Function)
	 */
	@Override
	public <U, P_BASE extends Property<U, P_BASE>> P_BASE map(Function<? super T, ? extends U> action) {
		throw new UnsupportedOperationException("not implemented yet");
	}

	/**
	 * @see com.slytechs.jnet.platform.api.common.settings.Property#deserializeValue(java.lang.String)
	 */
	@Override
	public T_BASE deserializeValue(String newValue) {
		throw new UnsupportedOperationException("not implemented yet");
	}

	/**
	 * @see com.slytechs.jnet.platform.api.common.settings.Property#setValue(java.lang.Object)
	 */
	@Override
	public T_BASE setValue(T newValue) {
		throw new UnsupportedOperationException("");
	}

}
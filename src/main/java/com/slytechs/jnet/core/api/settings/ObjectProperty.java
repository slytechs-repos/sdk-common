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

public final class ObjectProperty<E> extends Property<E, ObjectProperty<E>> {

	public ObjectProperty(String name, Deserializer<E> deserializer, Serializer<E> serializer) {
		super(name);
		super.setDeserializer(deserializer);
		super.setSerializer(serializer);
	}

	public ObjectProperty(String name, Deserializer<E> deserializer, Serializer<E> serializer, E value) {
		super(name, value);
		super.setDeserializer(deserializer);
		super.setSerializer(serializer);
	}

	ObjectProperty(SettingsSupport support, String name, Deserializer<E> deserializer, Serializer<E> serializer) {
		super(support, name);
		super.setDeserializer(deserializer);
		super.setSerializer(serializer);
	}

	ObjectProperty(SettingsSupport support, String name, Deserializer<E> deserializer, Serializer<E> serializer,
			E value) {
		super(support, name, value);
		super.setDeserializer(deserializer);
		super.setSerializer(serializer);
	}

	public E getObject() {
		return getValue();
	}
}
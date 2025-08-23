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

import com.slytechs.jnet.core.api.util.Enums;

/**
 * A specialized property class for handling enumeration values within the
 * settings framework. This class provides type-safe operations for enum
 * properties, including parsing from strings and value manipulation with proper
 * enum type checking.
 * 
 * <p>
 * EnumProperty extends the base Property class and implements enum-specific
 * functionality. It maintains type safety by requiring the enum type to be
 * specified either through the class object or an enum value. The class
 * supports all Java enumeration types that extend {@code Enum<E>}.
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * enum Status {
 * 	ACTIVE, INACTIVE, PENDING
 * }
 * 
 * // Create with enum class
 * EnumProperty&lt;Status&gt; status = new EnumProperty&lt;&gt;("system.status", Status.class);
 * status.setEnum(Status.ACTIVE);
 * 
 * // Create with enum value
 * EnumProperty&lt;Status&gt; status2 = new EnumProperty&lt;&gt;("system.status", Status.PENDING);
 * 
 * // Parse from string
 * status.parseValue("INACTIVE");
 * 
 * Status currentStatus = status.getEnum();
 * </pre>
 *
 * @param <E> the enum type this property will hold, must extend Enum&lt;E&gt;
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc
 * @see Property
 * @see Enum
 */
public final class EnumProperty<E extends Enum<E>> extends Property<E, EnumProperty<E>> {

	/** The enum type. */
	private final Class<E> enumType;

	/**
	 * Creates a new EnumProperty with the specified name and enum type. The
	 * property will be created in an unset state, but will use the provided enum
	 * class for type safety and value parsing.
	 *
	 * @param name     the name of the property, used for identification
	 * @param enumType the Class object representing the enum type E
	 */
	public EnumProperty(String name, Class<E> enumType) {
		super(name);
		this.enumType = enumType;

		super.setDeserializer((newValue) -> Enums.getEnumOrThrow(
				enumType,
				newValue, () -> new IllegalArgumentException(newValue)));
	}

	/**
	 * Creates a new EnumProperty with the specified name and initial enum value.
	 * The enum type is automatically determined from the provided value.
	 *
	 * @param name  the name of the property, used for identification
	 * @param value the initial enum value for this property, also used to determine
	 *              the enum type
	 */
	@SuppressWarnings("unchecked")
	public EnumProperty(String name, E value) {
		super(name, value);
		this.enumType = (Class<E>) value.getClass();
		super.setDeserializer((newValue) -> Enums.getEnumOrThrow(
				enumType,
				newValue, () -> new IllegalArgumentException(newValue)));
	}

	/**
	 * Creates a new EnumProperty with the specified name and enum type. The
	 * property will be created in an unset state, but will use the provided enum
	 * class for type safety and value parsing.
	 *
	 * @param support  the settings support instance for handling property change
	 *                 notifications
	 * @param name     the name of the property, used for identification
	 * @param enumType the Class object representing the enum type E
	 */
	EnumProperty(SettingsSupport support, String name, Class<E> enumType) {
		super(support, name);
		this.enumType = enumType;
		super.setDeserializer((newValue) -> Enums.getEnumOrThrow(
				enumType,
				newValue, () -> new IllegalArgumentException(newValue)));
	}

	/**
	 * Creates a new EnumProperty with the specified name and initial enum value.
	 * The enum type is automatically determined from the provided value.
	 *
	 * @param support the settings support instance for handling property change
	 *                notifications
	 * @param name    the name of the property, used for identification
	 * @param value   the initial enum value for this property, also used to
	 *                determine the enum type
	 */
	@SuppressWarnings("unchecked")
	EnumProperty(SettingsSupport support, String name, E value) {
		super(support, name, value);
		this.enumType = (Class<E>) value.getClass();
		super.setDeserializer((newValue) -> Enums.getEnumOrThrow(
				enumType,
				newValue, () -> new IllegalArgumentException(newValue)));
	}

	/**
	 * Retrieves the current enum value of this property. This is a convenience
	 * method that provides direct access to the enum value without requiring
	 * casting from the generic type.
	 *
	 * @return the current enum value of this property
	 * @throws IllegalStateException if the property has not been set
	 */
	public E getEnum() {
		return getValue();
	}

	/**
	 * Sets the value of this property to the specified enum value. This is a
	 * convenience method that provides a more natural way to set enum values
	 * compared to the generic setValue method.
	 *
	 * @param newValue the new enum value to set
	 * @return this EnumProperty instance for method chaining
	 */
	public EnumProperty<E> setEnum(E newValue) {
		return super.setValue(newValue);
	}
}
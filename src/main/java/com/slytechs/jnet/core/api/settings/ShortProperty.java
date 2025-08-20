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

import java.util.Optional;

/**
 * A specialized property class for handling short integer values within the
 * settings framework. This class provides type-safe operations for short
 * properties, including parsing from strings, value manipulation, and optional
 * value retrieval.
 * 
 * <p>
 * ShortProperty extends the base Property class and implements short-specific
 * functionality. It can be used to store and manage short integer configuration
 * values with proper type safety and change notification support. The class
 * supports the full range of 16-bit signed integers ({@code -32,768} to
 * {@code 32,767}).
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * ShortProperty port = new ShortProperty("device.port", (short) 80);
 * port.setShort((short) 443);
 * short value = port.getShort();
 * 
 * // Parse from string
 * port.parseValue("8080");
 * 
 * // Get as Optional
 * Optional&lt;Short&gt; optionalValue = port.toShortOptional();
 * </pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc
 * @see Property
 * @see Short
 */
public final class ShortProperty extends Property<Short, ShortProperty> {

	/**
	 * Creates a new ShortProperty with the specified name and no initial value. The
	 * property will be created in an unset state.
	 *
	 * @param name the name of the property, used for identification
	 */
	public ShortProperty(String name) {
		super(name);
	}

	/**
	 * Creates a new ShortProperty with the specified name and initial value. The
	 * property will be initialized with the provided short value.
	 *
	 * @param name  the name of the property, used for identification
	 * @param value the initial short value for this property
	 */
	public ShortProperty(String name, short value) {
		super(name, value);
	}

	/**
	 * Creates a new ShortProperty with the specified name and no initial value. The
	 * property will be created in an unset state.
	 *
	 * @param support the settings support instance for handling property change
	 *                notifications
	 * @param name    the name of the property, used for identification
	 */
	ShortProperty(SettingsSupport support, String name) {
		super(support, name);
	}

	/**
	 * Creates a new ShortProperty with the specified name and initial value. The
	 * property will be initialized with the provided short value.
	 *
	 * @param support the settings support instance for handling property change
	 *                notifications
	 * @param name    the name of the property, used for identification
	 * @param value   the initial short value for this property
	 */
	ShortProperty(SettingsSupport support, String name, short value) {
		super(support, name, value);
	}

	/**
	 * Retrieves the current short value of this property. This is a convenience
	 * method that provides direct access to the short value without requiring
	 * unboxing from the generic type.
	 *
	 * @return the current short value of this property
	 * @throws IllegalStateException if the property has not been set
	 */
	public short getShort() {
		return getValue();
	}

	/**
	 * Parses a string value and sets the property's value accordingly. The string
	 * value is converted to a short using {@link Short#parseShort(String)}. A null
	 * input will result in the property being set to null. The method supports
	 * decimal, hexadecimal (with "0x" or "#" prefix), and octal (with "0" prefix)
	 * formats.
	 *
	 * @param newValue the string value to parse, may be null
	 * @return this ShortProperty instance for method chaining
	 * @throws NumberFormatException if the string does not contain a parsable short
	 *                               or if the value is out of range for a 16-bit
	 *                               signed short
	 * @see Short#parseShort(String)
	 */
	@Override
	public ShortProperty deserializeValue(String newValue) {
		return setValue(newValue == null ? null : Short.parseShort(newValue));
	}

	/**
	 * Sets the value of this property to the specified short value. This is a
	 * convenience method that provides a more natural way to set short values
	 * compared to the generic setValue method.
	 *
	 * @param newValue the new short value to set
	 * @return this ShortProperty instance for method chaining
	 */
	public ShortProperty setShort(short newValue) {
		return setValue(newValue);
	}

	/**
	 * Converts the current property value to an Optional&lt;Short&gt;. If the
	 * property has a value set, it will be wrapped in an Optional. If the property
	 * is unset, an empty Optional will be returned. This method provides a
	 * null-safe way to handle optional short values.
	 *
	 * @return an Optional containing the current short value if present, or an
	 *         empty Optional if the property is unset
	 * @see Optional
	 */
	public Optional<Short> toShortOptional() {
		if (isPresent())
			return Optional.of(getValue());

		return Optional.empty();
	}
}
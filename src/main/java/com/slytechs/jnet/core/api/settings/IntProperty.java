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

import java.util.OptionalInt;

/**
 * A specialized property class for handling integer values within the settings
 * framework. This class provides type-safe operations for integer properties,
 * including parsing from strings, value manipulation, and optional value
 * retrieval using the memory-efficient OptionalInt class.
 * 
 * <p>
 * IntProperty extends the base Property class and implements integer-specific
 * functionality. It can be used to store and manage integer configuration
 * values with proper type safety and change notification support. The class
 * supports the full range of 32-bit signed integers ({@code -2^31} to
 * {@code 2^31-1}).
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * IntProperty port = new IntProperty("server.port", 8080);
 * port.setInt(9090);
 * int value = port.getInt();
 * 
 * // Parse from string
 * port.parseValue("8443");
 * 
 * // Get as OptionalInt
 * OptionalInt optionalValue = port.toIntOptional();
 * 
 * // Use with different number formats
 * port.parseValue("0xFF"); // Hexadecimal
 * port.parseValue("0100"); // Octal
 * </pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc
 * @see Property
 * @see Integer
 * @see OptionalInt
 */
public final class IntProperty extends Property<Integer, IntProperty> {

	/**
	 * Creates a new IntProperty with the specified name and no initial value. The
	 * property will be created in an unset state.
	 *
	 * @param name the name of the property, used for identification
	 */
	public IntProperty(String name) {
		super(name);
	}

	/**
	 * Creates a new IntProperty with the specified name and initial value. The
	 * property will be initialized with the provided integer value.
	 *
	 * @param name  the name of the property, used for identification
	 * @param value the initial integer value for this property
	 */
	public IntProperty(String name, int value) {
		super(name, value);
	}

	/**
	 * Creates a new IntProperty with the specified name and no initial value. The
	 * property will be created in an unset state.
	 *
	 * @param support the settings support instance for handling property change
	 *                notifications
	 * @param name    the name of the property, used for identification
	 */
	IntProperty(SettingsSupport support, String name) {
		super(support, name);
	}

	/**
	 * Creates a new IntProperty with the specified name and initial value. The
	 * property will be initialized with the provided integer value.
	 *
	 * @param support the settings support instance for handling property change
	 *                notifications
	 * @param name    the name of the property, used for identification
	 * @param value   the initial integer value for this property
	 */
	IntProperty(SettingsSupport support, String name, int value) {
		super(support, name, value);
	}

	/**
	 * Retrieves the current integer value of this property. This is a convenience
	 * method that provides direct access to the integer value without requiring
	 * unboxing from the generic type.
	 *
	 * @return the current integer value of this property
	 * @throws IllegalStateException if the property has not been set
	 */
	public int getInt() {
		return getValue();
	}

	/**
	 * Parses a string value and sets the property's value accordingly. The string
	 * value is converted to an integer using {@link Integer#parseInt(String)}. A
	 * null input will result in the property being set to null. The method supports
	 * decimal, hexadecimal (with "0x" or "#" prefix), and octal (with "0" prefix)
	 * formats.
	 *
	 * @param newValue the string value to parse, may be null
	 * @return this IntProperty instance for method chaining
	 * @throws NumberFormatException if the string does not contain a parsable
	 *                               integer or if the value is out of range for a
	 *                               32-bit signed integer
	 * @see Integer#parseInt(String)
	 */
	@Override
	public IntProperty deserializeValue(String newValue) {
		return setValue(newValue == null ? null : Integer.parseInt(newValue));
	}

	/**
	 * Sets the value of this property to the specified integer value. This is a
	 * convenience method that provides a more natural way to set integer values
	 * compared to the generic setValue method.
	 *
	 * @param newValue the new integer value to set
	 * @return this IntProperty instance for method chaining
	 */
	public IntProperty setInt(int newValue) {
		return setValue(newValue);
	}

	/**
	 * Converts the current property value to an OptionalInt. If the property has a
	 * value set, it will be wrapped in an OptionalInt. If the property is unset, an
	 * empty OptionalInt will be returned. This method provides a memory-efficient
	 * way to handle optional integer values compared to using
	 * Optional&lt;Integer&gt;.
	 *
	 * @return an OptionalInt containing the current integer value if present, or an
	 *         empty OptionalInt if the property is unset
	 * @see OptionalInt
	 */
	public OptionalInt toIntOptional() {
		if (isPresent())
			return OptionalInt.of(getValue());

		return OptionalInt.empty();
	}
}
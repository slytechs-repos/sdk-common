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

import java.util.OptionalLong;

/**
 * A specialized property class for handling long integer values within the
 * settings framework. This class provides type-safe operations for long
 * properties, including parsing from strings, value manipulation, and optional
 * value retrieval using the memory-efficient OptionalLong class.
 * 
 * <p>
 * LongProperty extends the base Property class and implements long-specific
 * functionality. It can be used to store and manage long integer configuration
 * values with proper type safety and change notification support. The class
 * supports the full range of 64-bit signed integers ({@code -2^63} to
 * {@code 2^63-1}).
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * LongProperty fileSize = new LongProperty("file.maxSize", 1073741824L); // 1GB
 * fileSize.setLong(2147483648L); // 2GB
 * long size = fileSize.getLong();
 * 
 * // Parse from string
 * fileSize.parseValue("3221225472"); // 3GB
 * 
 * // Get as OptionalLong
 * OptionalLong optionalValue = fileSize.toOptionalLong();
 * 
 * // Use with different number formats
 * fileSize.setLong("0xFFFFFFFFFFFF"); // Hexadecimal
 * fileSize.setLong("0777777777777"); // Octal
 * </pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc
 * @see Property
 * @see Long
 * @see OptionalLong
 */
public final class LongProperty extends Property<Long, LongProperty> {

	/**
	 * Creates a new LongProperty with the specified name and no initial value. The
	 * property will be created in an unset state.
	 *
	 * @param name the name of the property, used for identification
	 */
	public LongProperty(String name) {
		super(name);
	}

	/**
	 * Creates a new LongProperty with the specified name and initial value. The
	 * property will be initialized with the provided long value.
	 *
	 * @param name  the name of the property, used for identification
	 * @param value the initial long value for this property
	 */
	public LongProperty(String name, long value) {
		super(name, value);
	}

	/**
	 * Creates a new LongProperty with the specified name and no initial value. The
	 * property will be created in an unset state.
	 *
	 * @param support the settings support instance for handling property change
	 *                notifications
	 * @param name    the name of the property, used for identification
	 */
	LongProperty(SettingsSupport support, String name) {
		super(support, name);
	}

	/**
	 * Creates a new LongProperty with the specified name and initial value. The
	 * property will be initialized with the provided long value.
	 *
	 * @param support the settings support instance for handling property change
	 *                notifications
	 * @param name    the name of the property, used for identification
	 * @param value   the initial long value for this property
	 */
	LongProperty(SettingsSupport support, String name, long value) {
		super(support, name, value);
	}

	/**
	 * Retrieves the current long value of this property. This is a convenience
	 * method that provides direct access to the long value without requiring
	 * unboxing from the generic type.
	 *
	 * @return the current long value of this property
	 * @throws IllegalStateException if the property has not been set
	 */
	public long getLong() {
		return getValue();
	}

	/**
	 * Parses a string value and sets the property's value accordingly. The string
	 * value is converted to a long using {@link Long#parseLong(String)}. A null
	 * input will result in the property being set to null. The method supports
	 * decimal, hexadecimal (with "0x" or "#" prefix), and octal (with "0" prefix)
	 * formats.
	 *
	 * @param newValue the string value to parse, may be null
	 * @return this LongProperty instance for method chaining
	 * @throws NumberFormatException if the string does not contain a parsable long
	 *                               or if the value is out of range for a 64-bit
	 *                               signed long
	 * @see Long#parseLong(String)
	 */
	@Override
	public LongProperty deserializeValue(String newValue) {
		return setValue(newValue == null ? null : Long.parseLong(newValue));
	}

	/**
	 * Sets the value of this property to the specified integer value. The integer
	 * value is automatically widened to a long.
	 *
	 * @param newValue the new integer value to set
	 * @return this LongProperty instance for method chaining
	 */
	public LongProperty setLong(int newValue) {
		return super.setValue((long) newValue);
	}

	/**
	 * Sets the value of this property to the specified long value. This is a
	 * convenience method that provides a more natural way to set long values
	 * compared to the generic setValue method.
	 *
	 * @param newValue the new long value to set
	 * @return this LongProperty instance for method chaining
	 */
	public LongProperty setLong(long newValue) {
		return super.setValue(newValue);
	}

	/**
	 * Sets the value of this property by parsing the provided string. The string
	 * value is converted to a long using {@link Long#parseLong(String)}. This
	 * method supports decimal, hexadecimal, and octal formats.
	 *
	 * @param newValue the string value to parse and set
	 * @return this LongProperty instance for method chaining
	 * @throws NumberFormatException if the string does not contain a parsable long
	 *                               or if the value is out of range
	 * @see Long#parseLong(String)
	 */
	public LongProperty setLong(String newValue) {
		return super.setValue(Long.parseLong(newValue));
	}

	/**
	 * Converts the current property value to an OptionalLong. If the property has a
	 * value set, it will be wrapped in an OptionalLong. If the property is unset,
	 * an empty OptionalLong will be returned. This method provides a
	 * memory-efficient way to handle optional long values compared to using
	 * Optional&lt;Long&gt;.
	 *
	 * @return an OptionalLong containing the current long value if present, or an
	 *         empty OptionalLong if the property is unset
	 * @see OptionalLong
	 */
	public OptionalLong toOptionalLong() {
		if (isPresent())
			return OptionalLong.of(getValue());

		return OptionalLong.empty();
	}
}
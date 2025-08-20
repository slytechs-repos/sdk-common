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
 * A specialized property class for handling unsigned 16-bit integer values
 * within the settings framework. This class provides type-safe operations for
 * unsigned shorts, storing them internally as Java integers while maintaining
 * proper bounds checking and unsigned semantics.
 * 
 * <p>
 * UnsignedShortProperty extends the base Property class and implements unsigned
 * short-specific functionality. It supports values in the range 0 to 65,535
 * (2^16-1) and provides methods for working with both signed shorts and
 * unsigned integer representations.
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * UnsignedShortProperty port = new UnsignedShortProperty("server.port", 0xFFFF);
 * port.setUnsignedShort(65535); // Maximum value
 * int value = port.getValue(); // Returns unsigned value as int
 * 
 * // Parse from string
 * port.parseValue("65535"); // Maximum value
 * 
 * // Use with signed short
 * short signedShort = -1;
 * port.setUnsignedShort(signedShort); // Sets to 65535
 * 
 * // Get as OptionalInt
 * OptionalInt optionalValue = port.toOptionalUnsignedShort();
 * </pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc
 * @see Property
 */
public final class UnsignedShortProperty extends Property<Integer, UnsignedShortProperty> {

	/** The minimum value (0) that this unsigned short property can hold */
	public static final int MIN_VALUE = 0;

	/** The maximum value (65,535) that this unsigned short property can hold */
	public static final int MAX_VALUE = (1 << 16) - 1;

	/**
	 * Creates a new UnsignedShortProperty with the specified name and no initial
	 * value. The property will be created in an unset state.
	 *
	 * @param name the name of the property, used for identification
	 */
	public UnsignedShortProperty(String name) {
		super(name);
	}

	/**
	 * Creates a new UnsignedShortProperty with the specified name and unsigned
	 * integer value.
	 *
	 * @param name          the name of the property, used for identification
	 * @param unsignedValue the initial unsigned value (must be between 0 and
	 *                      65,535)
	 * @throws IllegalArgumentException if the value is outside the valid range
	 */
	public UnsignedShortProperty(String name, int unsignedValue) {
		super(name, unsignedValue);
	}

	/**
	 * Creates a new UnsignedShortProperty with the specified name and signed short
	 * value. The signed short value is automatically converted to its unsigned
	 * representation.
	 *
	 * @param name          the name of the property, used for identification
	 * @param unsignedValue the initial value as a signed short, will be converted
	 *                      to unsigned
	 */
	public UnsignedShortProperty(String name, short unsignedValue) {
		super(name, Short.toUnsignedInt(unsignedValue));
	}

	/**
	 * Creates a new UnsignedShortProperty with the specified name and no initial
	 * value. The property will be created in an unset state.
	 *
	 * @param support the settings support instance for handling property change
	 *                notifications
	 * @param name    the name of the property, used for identification
	 */
	UnsignedShortProperty(SettingsSupport support, String name) {
		super(support, name);
	}

	/**
	 * Creates a new UnsignedShortProperty with the specified name and unsigned
	 * integer value.
	 *
	 * @param support       the settings support instance for handling property
	 *                      change notifications
	 * @param name          the name of the property, used for identification
	 * @param unsignedValue the initial unsigned value (must be between 0 and
	 *                      65,535)
	 * @throws IllegalArgumentException if the value is outside the valid range
	 */
	UnsignedShortProperty(SettingsSupport support, String name, int unsignedValue) {
		super(support, name, unsignedValue);
	}

	/**
	 * Validates that a value is within the acceptable range for an unsigned short.
	 * 
	 * @param unsignedValue the value to validate
	 * @throws IllegalArgumentException if the value is less than 0 or greater than
	 *                                  65,535
	 */
	@Override
	protected void checkBounds(Integer unsignedValue) throws IllegalArgumentException {
		if (unsignedValue < MIN_VALUE || unsignedValue > MAX_VALUE)
			throw valueOutOfBoundsException(unsignedValue, MIN_VALUE, MAX_VALUE);
	}

	/**
	 * Parses a string value and sets the property's value accordingly. The string
	 * value is converted to an integer and validated against the unsigned short
	 * range.
	 *
	 * @param newValue the string value to parse, may be null
	 * @return this UnsignedShortProperty instance for method chaining
	 * @throws NumberFormatException    if the string does not contain a parsable
	 *                                  integer
	 * @throws IllegalArgumentException if the parsed value is outside the valid
	 *                                  range
	 */
	@Override
	public UnsignedShortProperty deserializeValue(String newValue) {
		return setValue(newValue == null ? null : Integer.parseInt(newValue));
	}

	/**
	 * Sets the value of this property using an unsigned integer value.
	 *
	 * @param newValue the new unsigned value to set (must be between 0 and 65,535)
	 * @return this UnsignedShortProperty instance for method chaining
	 * @throws IllegalArgumentException if the value is outside the valid range
	 */
	public UnsignedShortProperty setUnsignedShort(int newValue) {
		return super.setValue(newValue);
	}

	/**
	 * Sets the value of this property using a signed short value. The signed short
	 * is automatically converted to its unsigned representation.
	 *
	 * @param newValue the new signed short value to set
	 * @return this UnsignedShortProperty instance for method chaining
	 */
	public UnsignedShortProperty setUnsignedShort(short newValue) {
		return super.setValue(Short.toUnsignedInt(newValue));
	}

	/**
	 * Converts the current property value to an OptionalInt. If the property has a
	 * value set, it will be wrapped in an OptionalInt. If the property is unset, an
	 * empty OptionalInt will be returned.
	 *
	 * @return an OptionalInt containing the current unsigned value if present, or
	 *         an empty OptionalInt if the property is unset
	 * @see OptionalInt
	 */
	public OptionalInt toOptionalUnsignedShort() {
		if (isPresent())
			return OptionalInt.of(getValue());

		return OptionalInt.empty();
	}
}
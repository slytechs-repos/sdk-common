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
 * A specialized property class for handling unsigned 32-bit integer values
 * within the settings framework. This class provides type-safe operations for
 * unsigned integers, storing them internally as Java longs while maintaining
 * proper bounds checking and unsigned semantics.
 * 
 * <p>
 * UnsignedIntProperty extends the base Property class and implements unsigned
 * integer-specific functionality. It supports values in the range 0 to 2^32-1
 * (4,294,967,295) and provides methods for working with both signed integers
 * and unsigned long representations.
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * UnsignedIntProperty counter = new UnsignedIntProperty("packet.counter", 0xFFFFFFFFL);
 * counter.setUnsignedInt(Integer.MAX_VALUE + 1L); // Value beyond signed int range
 * long value = counter.getValue(); // Returns unsigned value as long
 * 
 * // Parse from string
 * counter.parseValue("4294967295"); // Maximum value (2^32 - 1)
 * 
 * // Use with signed int
 * int signedInt = -1;
 * counter.setUnsignedInt(signedInt); // Sets to 4294967295
 * 
 * // Get as OptionalLong
 * OptionalLong optionalValue = counter.toOptionalUnsignedInt();
 * </pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc
 * @see Property
 */
public final class UnsignedIntProperty extends Property<Long, UnsignedIntProperty> {

	/** The minimum value (0) that this unsigned int property can hold */
	public static final long MIN_VALUE = 0;

	/**
	 * The maximum value (2^32 - 1 = 4,294,967,295) that this unsigned int property
	 * can hold
	 */
	public static final long MAX_VALUE = (1L << 32) - 1L;

	/**
	 * Creates a new UnsignedIntProperty with the specified name and no initial
	 * value. The property will be created in an unset state.
	 *
	 * @param name the name of the property, used for identification
	 */
	public UnsignedIntProperty(String name) {
		super(name);
	}

	/**
	 * Creates a new UnsignedIntProperty with the specified name and unsigned long
	 * value.
	 *
	 * @param name          the name of the property, used for identification
	 * @param unsignedValue the initial unsigned value (must be between 0 and
	 *                      2^32-1)
	 * @throws IllegalArgumentException if the value is outside the valid range
	 */
	public UnsignedIntProperty(String name, long unsignedValue) {
		super(name, unsignedValue);
	}

	/**
	 * Creates a new UnsignedIntProperty with the specified name and no initial
	 * value. The property will be created in an unset state.
	 *
	 * @param support the settings support instance for handling property change
	 *                notifications
	 * @param name    the name of the property, used for identification
	 */
	UnsignedIntProperty(SettingsSupport support, String name) {
		super(support, name);
	}

	/**
	 * Creates a new UnsignedIntProperty with the specified name and unsigned long
	 * value.
	 *
	 * @param support       the settings support instance for handling property
	 *                      change notifications
	 * @param name          the name of the property, used for identification
	 * @param unsignedValue the initial unsigned value (must be between 0 and
	 *                      2^32-1)
	 * @throws IllegalArgumentException if the value is outside the valid range
	 */
	UnsignedIntProperty(SettingsSupport support, String name, long unsignedValue) {
		super(support, name, unsignedValue);
	}

	/**
	 * Validates that a value is within the acceptable range for an unsigned
	 * integer.
	 * 
	 * @param value the value to validate
	 * @throws IllegalArgumentException if the value is less than 0 or greater than
	 *                                  2^32-1
	 */
	@Override
	protected void checkBounds(Long value) throws IllegalArgumentException {
		if (value < MIN_VALUE || value > MAX_VALUE)
			throw valueOutOfBoundsException(value, MIN_VALUE, MAX_VALUE);
	}

	/**
	 * Parses a string value and sets the property's value accordingly. The string
	 * value is converted to a long and validated against the unsigned integer
	 * range.
	 *
	 * @param newValue the string value to parse, may be null
	 * @return this UnsignedIntProperty instance for method chaining
	 * @throws NumberFormatException    if the string does not contain a parsable
	 *                                  long
	 * @throws IllegalArgumentException if the parsed value is outside the valid
	 *                                  range
	 */
	@Override
	public UnsignedIntProperty deserializeValue(String newValue) {
		return setValue(newValue == null ? null : Long.parseLong(newValue));
	}

	/**
	 * Sets the value of this property using a signed integer value. The signed
	 * integer is automatically converted to its unsigned representation.
	 *
	 * @param newValue the new signed integer value to set
	 * @return this UnsignedIntProperty instance for method chaining
	 */
	public UnsignedIntProperty setUnsignedInt(int newValue) {
		return super.setValue(Integer.toUnsignedLong(newValue));
	}

	/**
	 * Sets the value of this property using an unsigned long value.
	 *
	 * @param newValue the new unsigned value to set (must be between 0 and 2^32-1)
	 * @return this UnsignedIntProperty instance for method chaining
	 * @throws IllegalArgumentException if the value is outside the valid range
	 */
	public UnsignedIntProperty setUnsignedInt(long newValue) {
		return super.setValue(newValue);
	}

	/**
	 * Converts the current property value to an OptionalLong. If the property has a
	 * value set, it will be wrapped in an OptionalLong. If the property is unset,
	 * an empty OptionalLong will be returned.
	 *
	 * @return an OptionalLong containing the current unsigned value if present, or
	 *         an empty OptionalLong if the property is unset
	 * @see OptionalLong
	 */
	public OptionalLong toOptionalUnsignedInt() {
		if (isPresent())
			return OptionalLong.of(getValue());

		return OptionalLong.empty();
	}
}
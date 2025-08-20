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
 * A specialized property class for handling unsigned byte values within the
 * settings framework. This class provides type-safe operations for 8-bit
 * unsigned integers, storing them internally as Java integers while maintaining
 * proper bounds checking and unsigned semantics.
 * 
 * <p>
 * UnsignedByteProperty extends the base Property class and implements unsigned
 * byte-specific functionality. It supports values in the range 0 to 255
 * (inclusive) and provides methods for working with both signed bytes and
 * unsigned integer representations.
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * UnsignedByteProperty flags = new UnsignedByteProperty("packet.flags", 0xFF);
 * flags.setUnsignedByte(128);
 * int value = flags.getValue(); // Returns unsigned value as int
 * 
 * // Parse from string
 * flags.parseValue("255"); // Maximum value
 * 
 * // Use with signed byte
 * byte signedByte = -1;
 * flags.setUnsignedByte(signedByte); // Sets to 255
 * 
 * // Get as OptionalInt
 * OptionalInt optionalValue = flags.toOptionalUnsignedByte();
 * </pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc
 * @see Property
 */
public final class UnsignedByteProperty extends Property<Integer, UnsignedByteProperty> {

	/** The minimum value (0) that this unsigned byte property can hold */
	public static final int MIN_VALUE = 0;

	/** The maximum value (255) that this unsigned byte property can hold */
	public static final int MAX_VALUE = (1 << 8) - 1;

	/**
	 * Creates a new UnsignedByteProperty with the specified name and no initial
	 * value. The property will be created in an unset state.
	 *
	 * @param name the name of the property, used for identification
	 */
	public UnsignedByteProperty(String name) {
		super(name);
	}

	/**
	 * Creates a new UnsignedByteProperty with the specified name and signed byte
	 * value. The signed byte value is automatically converted to its unsigned
	 * representation.
	 *
	 * @param name          the name of the property, used for identification
	 * @param unsignedValue the initial value as a signed byte, will be converted to
	 *                      unsigned
	 */
	public UnsignedByteProperty(String name, byte unsignedValue) {
		super(name, Byte.toUnsignedInt(unsignedValue));
	}

	/**
	 * Creates a new UnsignedByteProperty with the specified name and unsigned
	 * integer value.
	 *
	 * @param name          the name of the property, used for identification
	 * @param unsignedValue the initial unsigned value (must be between 0 and 255)
	 * @throws IllegalArgumentException if the value is outside the valid range
	 */
	public UnsignedByteProperty(String name, int unsignedValue) {
		super(name, unsignedValue);
	}

	/**
	 * Creates a new UnsignedByteProperty with the specified name and no initial
	 * value. The property will be created in an unset state.
	 *
	 * @param support the settings support instance for handling property change
	 *                notifications
	 * @param name    the name of the property, used for identification
	 */
	UnsignedByteProperty(SettingsSupport support, String name) {
		super(support, name);
	}

	/**
	 * Creates a new UnsignedByteProperty with the specified name and unsigned
	 * integer value, using the provided settings support for change notifications.
	 *
	 * @param support       the settings support instance for handling property
	 *                      change notifications
	 * @param name          the name of the property, used for identification
	 * @param unsignedValue the initial unsigned value (must be between 0 and 255)
	 * @throws IllegalArgumentException if the value is outside the valid range
	 */
	UnsignedByteProperty(SettingsSupport support, String name, int unsignedValue) {
		super(support, name, unsignedValue);
	}

	/**
	 * Validates that a value is within the acceptable range for an unsigned byte.
	 * 
	 * @param unsignedValue the value to validate
	 * @throws IllegalArgumentException if the value is less than 0 or greater than
	 *                                  255
	 */
	@Override
	protected void checkBounds(Integer unsignedValue) throws IllegalArgumentException {
		if (unsignedValue < MIN_VALUE || unsignedValue > MAX_VALUE)
			throw valueOutOfBoundsException(unsignedValue, MIN_VALUE, MAX_VALUE);
	}

	/**
	 * Parses a string value and sets the property's value accordingly. The string
	 * value is converted to an integer and validated against the unsigned byte
	 * range.
	 *
	 * @param newValue the string value to parse, may be null
	 * @return this UnsignedByteProperty instance for method chaining
	 * @throws NumberFormatException    if the string does not contain a parsable
	 *                                  integer
	 * @throws IllegalArgumentException if the parsed value is outside the valid
	 *                                  range
	 */
	@Override
	public UnsignedByteProperty deserializeValue(String newValue) {
		return setValue(newValue == null ? null : Integer.parseInt(newValue));
	}

	/**
	 * Sets the value of this property using a signed byte value. The signed byte is
	 * automatically converted to its unsigned representation.
	 *
	 * @param newValue the new signed byte value to set
	 * @return this UnsignedByteProperty instance for method chaining
	 */
	public UnsignedByteProperty setUnsignedByte(byte newValue) {
		return super.setValue(Byte.toUnsignedInt(newValue));
	}

	/**
	 * Sets the value of this property using an unsigned integer value.
	 *
	 * @param newValue the new unsigned value to set (must be between 0 and 255)
	 * @return this UnsignedByteProperty instance for method chaining
	 * @throws IllegalArgumentException if the value is outside the valid range
	 */
	public UnsignedByteProperty setUnsignedByte(int newValue) {
		return super.setValue(newValue);
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
	public OptionalInt toOptionalUnsignedByte() {
		if (isPresent())
			return OptionalInt.of(getValue());

		return OptionalInt.empty();
	}
}
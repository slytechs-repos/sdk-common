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
 * A specialized property class for handling byte values within the settings
 * framework. This class provides type-safe operations for byte properties,
 * including parsing from strings, value manipulation, and optional value
 * retrieval.
 * 
 * <p>
 * ByteProperty extends the base Property class and implements byte-specific
 * functionality. It can be used to store and manage byte configuration values
 * with proper type safety and change notification support. The valid range for
 * values is from -128 to 127 (inclusive).
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * ByteProperty port = new ByteProperty("system.port", (byte) 80);
 * port.setByte((byte) 443);
 * byte currentPort = port.getByte();
 * 
 * // Parse from string
 * port.parseValue("8");
 * 
 * // Get as Optional
 * Optional&lt;Byte&gt; optionalValue = port.toShortOptional();
 * </pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc
 * @see Property
 */
public final class ByteProperty extends Property<Byte, ByteProperty> {

	/**
	 * Creates a new ByteProperty with the specified name and no initial value. The
	 * property will be created in an unset state.
	 *
	 * @param name the name of the property, used for identification
	 */
	public ByteProperty(String name) {
		super(name);
	}

	/**
	 * Creates a new ByteProperty with the specified name and initial value. The
	 * property will be initialized with the provided byte value.
	 *
	 * @param name  the name of the property, used for identification
	 * @param value the initial byte value for this property
	 */
	public ByteProperty(String name, byte value) {
		super(name, value);
	}

	/**
	 * Creates a new ByteProperty with the specified name and no initial value. The
	 * property will be created in an unset state.
	 *
	 * @param support the settings support instance for handling property change
	 *                notifications
	 * @param name    the name of the property, used for identification
	 */
	ByteProperty(SettingsSupport support, String name) {
		super(support, name);
	}

	/**
	 * Creates a new ByteProperty with the specified name and initial value. The
	 * property will be initialized with the provided byte value.
	 *
	 * @param support the settings support instance for handling property change
	 *                notifications
	 * @param name    the name of the property, used for identification
	 * @param value   the initial byte value for this property
	 */
	ByteProperty(SettingsSupport support, String name, byte value) {
		super(support, name, value);
	}

	/**
	 * Retrieves the current byte value of this property. This is a convenience
	 * method that provides direct access to the byte value without requiring
	 * casting from the generic type.
	 *
	 * @return the current byte value of this property
	 * @throws IllegalStateException if the property has not been set
	 */
	public byte getByte() {
		return getValue();
	}

	/**
	 * Parses a string value and sets the property's value accordingly. The string
	 * value is converted to a byte using {@link Byte#parseByte(String)}. A null
	 * input will result in the property being set to null.
	 *
	 * @param newValue the string value to parse, may be null
	 * @return this ByteProperty instance for method chaining
	 * @throws NumberFormatException if the string does not contain a parsable byte
	 *                               or if the value is out of range (-128 to 127)
	 * @see Byte#parseByte(String)
	 */
	@Override
	public ByteProperty deserializeValue(String newValue) {
		return setValue(newValue == null ? null : Byte.parseByte(newValue));
	}

	/**
	 * Sets the value of this property to the specified byte value. This is a
	 * convenience method that provides a more natural way to set byte values
	 * compared to the generic setValue method.
	 *
	 * @param newValue the new byte value to set
	 * @return this ByteProperty instance for method chaining
	 */
	public ByteProperty setByte(byte newValue) {
		return setValue(newValue);
	}

	/**
	 * Converts the current property value to an Optional<Byte>. If the property has
	 * a value set, it will be wrapped in an Optional. If the property is unset, an
	 * empty Optional will be returned.
	 *
	 * @return an Optional containing the current byte value if present, or an empty
	 *         Optional if the property is unset
	 */
	public Optional<Byte> toShortOptional() {
		if (isPresent())
			return Optional.of(getValue());

		return Optional.empty();
	}
}
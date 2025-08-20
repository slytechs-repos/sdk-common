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

/**
 * A specialized property class for handling boolean values within the settings
 * framework. This class provides type-safe operations for boolean properties,
 * including parsing from strings and value manipulation.
 * 
 * <p>
 * BooleanProperty extends the base Property class and implements
 * boolean-specific functionality. It can be used to store and manage boolean
 * configuration values with proper type safety and change notification support.
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * BooleanProperty debug = new BooleanProperty("app.debug", false);
 * debug.setBoolean(true);
 * boolean isDebug = debug.getBoolean();
 * 
 * // Can also parse from string
 * debug.parseValue("true");
 * </pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc
 * @see Property
 */
public final class BooleanProperty extends Property<Boolean, BooleanProperty> {

	/**
	 * Creates a new BooleanProperty with the specified name and no initial value.
	 * The property will be created in an unset state.
	 *
	 * @param name the name of the property, used for identification
	 */
	public BooleanProperty(String name) {
		super(name);
	}

	/**
	 * Creates a new BooleanProperty with the specified name and initial value. The
	 * property will be initialized with the provided boolean value.
	 *
	 * @param name  the name of the property, used for identification
	 * @param value the initial boolean value for this property
	 */
	public BooleanProperty(String name, boolean value) {
		super(name, value);
	}

	/**
	 * Creates a new BooleanProperty with the specified name and no initial value.
	 * The property will be created in an unset state.
	 *
	 * @param support the settings support instance for handling property change
	 *                notifications
	 * @param name    the name of the property, used for identification
	 */
	BooleanProperty(SettingsSupport support, String name) {
		super(support, name);
	}

	/**
	 * Creates a new BooleanProperty with the specified name and initial value. The
	 * property will be initialized with the provided boolean value.
	 *
	 * @param support the settings support instance for handling property change
	 *                notifications
	 * @param name    the name of the property, used for identification
	 * @param value   the initial boolean value for this property
	 */
	BooleanProperty(SettingsSupport support, String name, boolean value) {
		super(support, name, value);
	}

	/**
	 * Retrieves the current boolean value of this property. This is a convenience
	 * method that provides direct access to the boolean value without requiring
	 * casting from the generic type.
	 *
	 * @return the current boolean value of this property
	 * @throws IllegalStateException if the property has not been set
	 */
	public boolean getBoolean() {
		return getValue();
	}

	/**
	 * Parses a string value and sets the property's value accordingly. The string
	 * value is converted to a boolean using {@link Boolean#parseBoolean(String)},
	 * which returns false for any value other than the literal "true"
	 * (case-insensitive). A null input will result in the property being set to
	 * null.
	 *
	 * @param newValue the string value to parse, may be null
	 * @return this BooleanProperty instance for method chaining
	 * @see Boolean#parseBoolean(String)
	 */
	@Override
	public BooleanProperty deserializeValue(String newValue) {
		return setValue(newValue == null ? null : Boolean.parseBoolean(newValue));
	}

	/**
	 * Sets the value of this property to the specified boolean value. This is a
	 * convenience method that provides a more natural way to set boolean values
	 * compared to the generic setValue method.
	 *
	 * @param newValue the new boolean value to set
	 * @return this BooleanProperty instance for method chaining
	 */
	public BooleanProperty setBoolean(boolean newValue) {
		return setValue(newValue);
	}
}
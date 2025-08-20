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
 * A specialized property class for handling string values within the settings
 * framework. This class provides direct string value management without any
 * type conversion needs, making it particularly suitable for text-based
 * configuration values.
 * 
 * <p>
 * StringProperty extends the base Property class and implements string-specific
 * functionality. Unlike numeric property types, StringProperty handles values
 * in their native form, requiring no parsing or conversion beyond what the base
 * Property class provides for string formatting.
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * StringProperty hostName = new StringProperty("server.host", "localhost");
 * hostName.setString("example.com");
 * String host = hostName.getString();
 * 
 * // Direct parsing of string values
 * hostName.parseValue("new-server.domain.com");
 * 
 * // Use with format strings
 * hostName.setFormat("Host: %s");
 * String formatted = hostName.toFormattedValue(); // "Host: new-server.domain.com"
 * </pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc
 * @see Property
 */
public final class StringProperty extends Property<String, StringProperty> {

	/**
	 * Creates a new StringProperty with the specified name and no initial value.
	 * The property will be created in an unset state.
	 *
	 * @param name the name of the property, used for identification
	 */
	public StringProperty(String name) {
		super(name);
	}

	/**
	 * Creates a new StringProperty with the specified name and initial value. The
	 * property will be initialized with the provided string value.
	 *
	 * @param name  the name of the property, used for identification
	 * @param value the initial string value for this property
	 */
	public StringProperty(String name, String value) {
		super(name, value);
	}

	/**
	 * Creates a new StringProperty with the specified name and no initial value.
	 * The property will be created in an unset state.
	 *
	 * @param support the settings support instance for handling property change
	 *                notifications
	 * @param name    the name of the property, used for identification
	 */
	StringProperty(SettingsSupport support, String name) {
		super(support, name);
	}

	/**
	 * Creates a new StringProperty with the specified name and initial value. The
	 * property will be initialized with the provided string value.
	 *
	 * @param support the settings support instance for handling property change
	 *                notifications
	 * @param name    the name of the property, used for identification
	 * @param value   the initial string value for this property
	 */
	StringProperty(SettingsSupport support, String name, String value) {
		super(support, name, value);
	}

	/**
	 * Retrieves the current string value of this property. This is a convenience
	 * method that provides direct access to the string value without requiring
	 * casting from the generic type.
	 *
	 * @return the current string value of this property
	 * @throws IllegalStateException if the property has not been set
	 */
	public String getString() {
		return getValue();
	}

	/**
	 * Sets the value of this property from a string input. Unlike other property
	 * types that need to parse their input, this method simply sets the value
	 * directly as no conversion is needed.
	 *
	 * @param newValue the new string value to set, may be null
	 * @return this StringProperty instance for method chaining
	 */
	@Override
	public StringProperty deserializeValue(String newValue) {
		return setValue(newValue);
	}

	/**
	 * Sets the value of this property to the specified string value. This is a
	 * convenience method that provides a more natural way to set string values
	 * compared to the generic setValue method.
	 *
	 * @param newValue the new string value to set, may be null
	 * @return this StringProperty instance for method chaining
	 */
	public StringProperty setString(String newValue) {
		return super.setValue(newValue);
	}
}
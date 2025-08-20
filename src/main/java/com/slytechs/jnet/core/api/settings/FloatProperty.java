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
 * A specialized property class for handling single-precision floating-point
 * values within the settings framework. This class provides type-safe
 * operations for float properties, including parsing from strings, value
 * manipulation, and optional value retrieval.
 * 
 * <p>
 * FloatProperty extends the base Property class and implements float-specific
 * functionality. It can be used to store and manage float configuration values
 * with proper type safety and change notification support. The class supports
 * the full range of IEEE 754 single-precision floating point values.
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * FloatProperty factor = new FloatProperty("system.factor", 1.5f);
 * factor.setFloat(2.5f);
 * float value = factor.getFloat();
 * 
 * // Parse from string
 * factor.parseValue("3.75");
 * 
 * // Get as Optional
 * Optional&lt;Float&gt; optionalValue = factor.toFloatOptional();
 * 
 * // Use with scientific notation
 * factor.parseValue("1.5e-3");
 * </pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc
 * @see Property
 * @see Float
 */
public final class FloatProperty extends Property<Float, FloatProperty> {

	/**
	 * Creates a new FloatProperty with the specified name and no initial value. The
	 * property will be created in an unset state.
	 *
	 * @param name the name of the property, used for identification
	 */
	public FloatProperty(String name) {
		super(name);
	}

	/**
	 * Creates a new FloatProperty with the specified name and initial value. The
	 * property will be initialized with the provided float value.
	 *
	 * @param name  the name of the property, used for identification
	 * @param value the initial float value for this property
	 */
	public FloatProperty(String name, float value) {
		super(name, value);
	}

	/**
	 * Creates a new FloatProperty with the specified name and no initial value. The
	 * property will be created in an unset state.
	 *
	 * @param support the settings support instance for handling property change
	 *                notifications
	 * @param name    the name of the property, used for identification
	 */
	FloatProperty(SettingsSupport support, String name) {
		super(support, name);
	}

	/**
	 * Creates a new FloatProperty with the specified name and initial value. The
	 * property will be initialized with the provided float value.
	 *
	 * @param support the settings support instance for handling property change
	 *                notifications
	 * @param name    the name of the property, used for identification
	 * @param value   the initial float value for this property
	 */
	FloatProperty(SettingsSupport support, String name, float value) {
		super(support, name, value);
	}

	/**
	 * Retrieves the current float value of this property. This is a convenience
	 * method that provides direct access to the float value without requiring
	 * casting from the generic type.
	 *
	 * @return the current float value of this property
	 * @throws IllegalStateException if the property has not been set
	 */
	public float getFloat() {
		return getValue();
	}

	/**
	 * Parses a string value and sets the property's value accordingly. The string
	 * value is converted to a float using {@link Float#parseFloat(String)}. A null
	 * input will result in the property being set to null. The method supports
	 * standard decimal notation as well as scientific notation (e.g., "1.23e-4").
	 *
	 * @param newValue the string value to parse, may be null
	 * @return this FloatProperty instance for method chaining
	 * @throws NumberFormatException if the string does not contain a parsable float
	 *                               or if the value is out of range for a float
	 * @see Float#parseFloat(String)
	 */
	@Override
	public FloatProperty deserializeValue(String newValue) {
		return setValue(newValue == null ? null : Float.parseFloat(newValue));
	}

	/**
	 * Sets the value of this property to the specified float value. This is a
	 * convenience method that provides a more natural way to set float values
	 * compared to the generic setValue method.
	 *
	 * @param newValue the new float value to set
	 * @return this FloatProperty instance for method chaining
	 */
	public FloatProperty setFloat(float newValue) {
		return setValue(newValue);
	}

	/**
	 * Converts the current property value to an Optional&lt;Float&gt;. If the
	 * property has a value set, it will be wrapped in an Optional. If the property
	 * is unset, an empty Optional will be returned. This method provides a
	 * null-safe way to handle optional float values.
	 *
	 * @return an Optional containing the current float value if present, or an
	 *         empty Optional if the property is unset
	 * @see Optional
	 */
	public Optional<Float> toFloatOptional() {
		if (isPresent())
			return Optional.of(getValue());

		return Optional.empty();
	}
}
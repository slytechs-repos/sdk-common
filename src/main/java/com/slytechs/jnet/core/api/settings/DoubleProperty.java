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

import java.util.OptionalDouble;

/**
 * A specialized property class for handling double-precision floating-point
 * values within the settings framework. This class provides type-safe
 * operations for double properties, including parsing from strings, value
 * manipulation, and optional value retrieval.
 * 
 * <p>
 * DoubleProperty extends the base Property class and implements double-specific
 * functionality. It can be used to store and manage double configuration values
 * with proper type safety and change notification support. The class supports
 * the full range of IEEE 754 double-precision floating point values.
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * DoubleProperty threshold = new DoubleProperty("system.threshold", 0.75);
 * threshold.setDouble(0.85);
 * double value = threshold.getDouble();
 * 
 * // Parse from string
 * threshold.parseValue("0.95");
 * 
 * // Get as OptionalDouble
 * OptionalDouble optionalValue = threshold.toDoubleOptional();
 * 
 * // Use with scientific notation
 * threshold.parseValue("1.5e-3");
 * </pre>
 *
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc
 * @see Property
 * @see Double
 */
public final class DoubleProperty extends Property<Double, DoubleProperty> {

	/**
	 * Creates a new DoubleProperty with the specified name and no initial value.
	 * The property will be created in an unset state.
	 *
	 * @param name the name of the property, used for identification
	 */
	public DoubleProperty(String name) {
		super(name);
	}

	/**
	 * Creates a new DoubleProperty with the specified name and initial value. The
	 * property will be initialized with the provided double value.
	 *
	 * @param name  the name of the property, used for identification
	 * @param value the initial double value for this property
	 */
	public DoubleProperty(String name, double value) {
		super(name, value);
	}

	/**
	 * Creates a new DoubleProperty with the specified name and no initial value.
	 * The property will be created in an unset state.
	 *
	 * @param support the settings support instance for handling property change
	 *                notifications
	 * @param name    the name of the property, used for identification
	 */
	public DoubleProperty(SettingsSupport support, String name) {
		super(support, name);
	}

	/**
	 * Creates a new DoubleProperty with the specified name and initial value. The
	 * property will be initialized with the provided double value.
	 *
	 * @param support the settings support instance for handling property change
	 *                notifications
	 * @param name    the name of the property, used for identification
	 * @param value   the initial double value for this property
	 */
	public DoubleProperty(SettingsSupport support, String name, double value) {
		super(support, name, value);
	}

	/**
	 * Retrieves the current double value of this property. This is a convenience
	 * method that provides direct access to the double value without requiring
	 * casting from the generic type.
	 *
	 * @return the current double value of this property
	 * @throws IllegalStateException if the property has not been set
	 */
	public double getDouble() {
		return getValue();
	}

	/**
	 * Parses a string value and sets the property's value accordingly. The string
	 * value is converted to a double using {@link Double#parseDouble(String)}. A
	 * null input will result in the property being set to null. The method supports
	 * standard decimal notation as well as scientific notation (e.g., "1.23e-4").
	 *
	 * @param newValue the string value to parse, may be null
	 * @return this DoubleProperty instance for method chaining
	 * @throws NumberFormatException if the string does not contain a parsable
	 *                               double or if the value is out of range for a
	 *                               double
	 * @see Double#parseDouble(String)
	 */
	@Override
	public DoubleProperty deserializeValue(String newValue) {
		return setValue(newValue == null ? null : Double.parseDouble(newValue));
	}

	/**
	 * Sets the value of this property to the specified double value. This is a
	 * convenience method that provides a more natural way to set double values
	 * compared to the generic setValue method.
	 *
	 * @param newValue the new double value to set
	 * @return this DoubleProperty instance for method chaining
	 */
	public DoubleProperty setDouble(double newValue) {
		return setValue(newValue);
	}

	/**
	 * Converts the current property value to an OptionalDouble. If the property has
	 * a value set, it will be wrapped in an OptionalDouble. If the property is
	 * unset, an empty OptionalDouble will be returned. This method provides a
	 * null-safe way to handle optional double values.
	 *
	 * @return an OptionalDouble containing the current double value if present, or
	 *         an empty OptionalDouble if the property is unset
	 * @see OptionalDouble
	 */
	public OptionalDouble toDoubleOptional() {
		if (isPresent())
			return OptionalDouble.of(getValue());

		return OptionalDouble.empty();
	}
}
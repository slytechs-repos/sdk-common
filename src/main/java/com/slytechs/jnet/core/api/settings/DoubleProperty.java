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
 * A property that holds a double-precision floating-point value.
 * 
 * <p>
 * DoubleProperty provides type-safe access to double configuration values with
 * automatic resolution from system properties, environment variables, and
 * domain configuration files. This is useful for ratios, thresholds,
 * percentages, and other fractional values.
 * </p>
 * 
 * <h2>Parsing</h2>
 * <p>
 * String values are parsed using {@link Double#parseDouble(String)}, which supports:
 * </p>
 * <ul>
 * <li>Decimal: {@code 3.14159}, {@code -2.5}</li>
 * <li>Scientific notation: {@code 1.5e-10}, {@code 2.0E6}</li>
 * <li>Special values: {@code NaN}, {@code Infinity}, {@code -Infinity}</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class ThrottleSettings extends Settings {
 *     private final DoubleProperty ratio;
 *     private final DoubleProperty threshold;
 *     
 *     public ThrottleSettings() {
 *         super("config", "throttle");
 *         this.ratio = doubleProperty("ratio", 0.8)
 *             .comment("Throttle ratio (0.0 to 1.0)");
 *         this.threshold = doubleProperty("threshold", 0.95);
 *     }
 *     
 *     public double ratio() { return ratio.getDouble(); }
 *     public double threshold() { return threshold.getDouble(); }
 *     
 *     public ThrottleSettings withRatio(double ratio) {
 *         this.ratio.setDouble(ratio);
 *         return this;
 *     }
 * }
 * }</pre>
 *
 * @author Mark Bednarczyk
 * @author Sly Technologies Inc.
 * @see Settings#doubleProperty(String, double)
 */
public final class DoubleProperty extends Property<Double> {

    /**
     * Constructs a new DoubleProperty with the specified name, default value, and domain.
     *
     * @param name         the fully qualified property name
     * @param defaultValue the default value
     * @param domain       the domain this property belongs to, or null
     */
    public DoubleProperty(String name, double defaultValue, String domain) {
        super(name, defaultValue, domain);
    }

    /**
     * Returns the double value of this property.
     * 
     * <p>
     * This is a convenience method equivalent to {@link #get()} but returns
     * a primitive {@code double} instead of {@link Double}.
     * </p>
     *
     * @return the resolved double value
     */
    public double getDouble() {
        return get();
    }

    /**
     * Sets the double value of this property.
     * 
     * <p>
     * This is a convenience method equivalent to {@link #set(Object)} but
     * accepts a primitive {@code double}.
     * </p>
     *
     * @param value the value to set
     */
    public void setDouble(double value) {
        set(value);
    }

    /**
     * Parses a string value into a double.
     *
     * @param value the string to parse
     * @return the parsed double value
     * @throws NumberFormatException if the string cannot be parsed
     */
    @Override
    protected Double parseValue(String value) {
        return Double.parseDouble(value.trim());
    }
}
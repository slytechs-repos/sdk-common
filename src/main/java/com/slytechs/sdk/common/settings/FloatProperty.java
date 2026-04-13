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
package com.slytechs.sdk.common.settings;

/**
 * A property that holds a single-precision floating-point value.
 * 
 * <p>
 * FloatProperty provides type-safe access to float configuration values with
 * automatic resolution from system properties, environment variables, and
 * domain configuration files.
 * </p>
 * 
 * <h2>Parsing</h2>
 * <p>
 * String values are parsed using {@link Float#parseFloat(String)}, which supports:
 * </p>
 * <ul>
 * <li>Decimal: {@code 3.14}, {@code -2.5}</li>
 * <li>Scientific notation: {@code 1.5e-10}, {@code 2.0E6}</li>
 * <li>Special values: {@code NaN}, {@code Infinity}, {@code -Infinity}</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class SamplingSettings extends Settings {
 *     private final FloatProperty rate;
 *     
 *     public SamplingSettings() {
 *         super("config", "sampling");
 *         this.rate = floatProperty("rate", 0.1f)
 *             .comment("Sampling rate (0.0 to 1.0)");
 *     }
 *     
 *     public float rate() { return rate.getFloat(); }
 *     
 *     public SamplingSettings withRate(float rate) {
 *         this.rate.setFloat(rate);
 *         return this;
 *     }
 * }
 * }</pre>
 *
 * @author Mark Bednarczyk
 * @author Sly Technologies Inc.
 * @see Settings#floatProperty(String, float)
 */
public final class FloatProperty extends Property<Float> {

    /**
     * Constructs a new FloatProperty with the specified name, default value, and domain.
     *
     * @param name         the fully qualified property name
     * @param defaultValue the default value
     * @param domain       the domain this property belongs to, or null
     */
    public FloatProperty(String name, float defaultValue, String domain) {
        super(name, defaultValue, domain);
    }

    /**
     * Returns the float value of this property.
     * 
     * <p>
     * This is a convenience method equivalent to {@link #get()} but returns
     * a primitive {@code float} instead of {@link Float}.
     * </p>
     *
     * @return the resolved float value
     */
    public float getFloat() {
        return get();
    }

    /**
     * Sets the float value of this property.
     * 
     * <p>
     * This is a convenience method equivalent to {@link #set(Object)} but
     * accepts a primitive {@code float}.
     * </p>
     *
     * @param value the value to set
     */
    public void setFloat(float value) {
        set(value);
    }

    /**
     * Parses a string value into a float.
     *
     * @param value the string to parse
     * @return the parsed float value
     * @throws NumberFormatException if the string cannot be parsed
     */
    @Override
    protected Float parseValue(String value) {
        return Float.parseFloat(value.trim());
    }
}
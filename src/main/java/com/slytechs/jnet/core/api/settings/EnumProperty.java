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

import java.util.Objects;

/**
 * A property that holds an enum value.
 * 
 * <p>
 * EnumProperty provides type-safe access to enum configuration values with
 * automatic resolution from system properties, environment variables, and
 * domain configuration files. This is useful for configuration options with
 * a fixed set of valid values.
 * </p>
 * 
 * <h2>Parsing</h2>
 * <p>
 * String values are parsed with case-insensitive matching. The following
 * formats are supported:
 * </p>
 * <ul>
 * <li>Exact match: {@code SECONDS}</li>
 * <li>Case-insensitive: {@code seconds}, {@code Seconds}</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class TimeoutSettings extends Settings {
 *     private final LongProperty timeout;
 *     private final EnumProperty<TimeUnit> unit;
 *     
 *     public TimeoutSettings() {
 *         super("config", "timeout");
 *         this.timeout = longProperty("value", 30L);
 *         this.unit = enumProperty("unit", TimeUnit.SECONDS)
 *             .comment("Time unit for timeout");
 *     }
 *     
 *     public long timeout() { return timeout.getLong(); }
 *     public TimeUnit unit() { return unit.getEnum(); }
 *     
 *     public Duration asDuration() {
 *         return Duration.of(timeout(), unit().toChronoUnit());
 *     }
 *     
 *     public TimeoutSettings withTimeout(long value, TimeUnit unit) {
 *         this.timeout.setLong(value);
 *         this.unit.setEnum(unit);
 *         return this;
 *     }
 * }
 * }</pre>
 *
 * @param <E> the enum type
 * @author Mark Bednarczyk
 * @author Sly Technologies Inc.
 * @see Settings#enumProperty(String, Enum)
 */
public final class EnumProperty<E extends Enum<E>> extends Property<E> {

    /** The enum class for parsing. */
    private final Class<E> enumType;

    /**
     * Constructs a new EnumProperty with the specified name, default value, and domain.
     * 
     * <p>
     * The enum type is inferred from the default value's class.
     * </p>
     *
     * @param name         the fully qualified property name
     * @param defaultValue the default value (must not be null)
     * @param domain       the domain this property belongs to, or null
     * @throws NullPointerException if defaultValue is null
     */
    @SuppressWarnings("unchecked")
    public EnumProperty(String name, E defaultValue, String domain) {
        super(name, Objects.requireNonNull(defaultValue, "default enum value cannot be null"), domain);
        this.enumType = (Class<E>) defaultValue.getClass();
    }

    /**
     * Returns the enum type of this property.
     *
     * @return the enum class
     */
    public Class<E> enumType() {
        return enumType;
    }

    /**
     * Returns the enum value of this property.
     * 
     * <p>
     * This is a convenience method equivalent to {@link #get()} and is provided
     * for API clarity.
     * </p>
     *
     * @return the resolved enum value
     */
    public E getEnum() {
        return get();
    }

    /**
     * Sets the enum value of this property.
     * 
     * <p>
     * This is a convenience method equivalent to {@link #set(Object)} and is
     * provided for API clarity.
     * </p>
     *
     * @param value the value to set
     */
    public void setEnum(E value) {
        set(value);
    }

    /**
     * Parses a string value into an enum constant.
     * 
     * <p>
     * Parsing is case-insensitive. First attempts exact match, then
     * tries uppercase conversion for case-insensitive matching.
     * </p>
     *
     * @param value the string to parse
     * @return the parsed enum constant
     * @throws IllegalArgumentException if no matching enum constant is found
     */
    @Override
    protected E parseValue(String value) {
        String normalized = value.trim();

        // Try exact match first
        for (E constant : enumType.getEnumConstants()) {
            if (constant.name().equals(normalized)) {
                return constant;
            }
        }

        // Try case-insensitive match
        String upper = normalized.toUpperCase();
        for (E constant : enumType.getEnumConstants()) {
            if (constant.name().equals(upper)) {
                return constant;
            }
        }

        // Build error message with valid options
        StringBuilder validValues = new StringBuilder();
        for (E constant : enumType.getEnumConstants()) {
            if (validValues.length() > 0) {
                validValues.append(", ");
            }
            validValues.append(constant.name());
        }

        throw new IllegalArgumentException(
            "Invalid enum value: '" + value + "' for type " + enumType.getSimpleName()
            + ". Valid values: " + validValues);
    }

    /**
     * Formats an enum value as its name.
     *
     * @param value the enum value to format
     * @return the enum constant name
     */
    @Override
    protected String formatValue(E value) {
        return value == null ? "" : value.name();
    }
}
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
 * A property that holds a boolean value.
 * 
 * <p>
 * BooleanProperty provides type-safe access to boolean configuration values
 * with automatic resolution from system properties, environment variables, and
 * domain configuration files.
 * </p>
 * 
 * <h2>Parsing</h2>
 * <p>
 * String values are parsed with flexible matching (case-insensitive):
 * </p>
 * <ul>
 * <li><b>True values:</b> {@code true}, {@code yes}, {@code on}, {@code 1}, {@code enabled}</li>
 * <li><b>False values:</b> {@code false}, {@code no}, {@code off}, {@code 0}, {@code disabled}</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class DebugSettings extends Settings {
 *     private final BooleanProperty enabled;
 *     private final BooleanProperty verbose;
 *     
 *     public DebugSettings() {
 *         super("config", "debug");
 *         this.enabled = booleanProperty("enabled", false)
 *             .comment("Enable debug mode");
 *         this.verbose = booleanProperty("verbose", false);
 *     }
 *     
 *     public boolean isEnabled() { return enabled.getBoolean(); }
 *     public boolean isVerbose() { return verbose.getBoolean(); }
 *     
 *     public DebugSettings withEnabled(boolean enabled) {
 *         this.enabled.setBoolean(enabled);
 *         return this;
 *     }
 * }
 * }</pre>
 *
 * @author Mark Bednarczyk
 * @author Sly Technologies Inc.
 * @see Settings#booleanProperty(String, boolean)
 */
public final class BooleanProperty extends Property<Boolean> {

    /**
     * Constructs a new BooleanProperty with the specified name, default value, and domain.
     *
     * @param name         the fully qualified property name
     * @param defaultValue the default value
     * @param domain       the domain this property belongs to, or null
     */
    public BooleanProperty(String name, boolean defaultValue, String domain) {
        super(name, defaultValue, domain);
    }

    /**
     * Returns the boolean value of this property.
     * 
     * <p>
     * This is a convenience method equivalent to {@link #get()} but returns
     * a primitive {@code boolean} instead of {@link Boolean}.
     * </p>
     *
     * @return the resolved boolean value
     */
    public boolean getBoolean() {
        return get();
    }

    /**
     * Sets the boolean value of this property.
     * 
     * <p>
     * This is a convenience method equivalent to {@link #set(Object)} but
     * accepts a primitive {@code boolean}.
     * </p>
     *
     * @param value the value to set
     */
    public void setBoolean(boolean value) {
        set(value);
    }

    /**
     * Parses a string value into a boolean.
     * 
     * <p>
     * Supports flexible parsing with common boolean representations:
     * </p>
     * <ul>
     * <li>True: "true", "yes", "on", "1", "enabled"</li>
     * <li>False: "false", "no", "off", "0", "disabled"</li>
     * </ul>
     *
     * @param value the string to parse
     * @return the parsed boolean value
     * @throws IllegalArgumentException if the string is not a recognized boolean value
     */
    @Override
    protected Boolean parseValue(String value) {
        String normalized = value.trim().toLowerCase();

        return switch (normalized) {
            case "true", "yes", "on", "1", "enabled" -> true;
            case "false", "no", "off", "0", "disabled" -> false;
            default -> throw new IllegalArgumentException(
                "Invalid boolean value: '" + value + "'. Expected: true/false, yes/no, on/off, 1/0, enabled/disabled");
        };
    }
}
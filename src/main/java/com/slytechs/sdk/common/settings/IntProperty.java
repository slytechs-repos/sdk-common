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
 * A property that holds an integer value.
 * 
 * <p>
 * IntProperty provides type-safe access to integer configuration values with
 * automatic resolution from system properties, environment variables, and
 * domain configuration files.
 * </p>
 * 
 * <h2>Parsing</h2>
 * <p>
 * String values are parsed using {@link Integer#decode(String)}, which supports:
 * </p>
 * <ul>
 * <li>Decimal: {@code 1234}</li>
 * <li>Hexadecimal: {@code 0x4D2} or {@code #4D2}</li>
 * <li>Octal: {@code 02322}</li>
 * <li>Negative values: {@code -1234}</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class ServerSettings extends Settings {
 *     private final IntProperty port;
 *     
 *     public ServerSettings() {
 *         super("config", "server");
 *         this.port = intProperty("port", 8080).comment("Server listen port");
 *     }
 *     
 *     public int port() { return port.getInt(); }
 *     
 *     public ServerSettings withPort(int port) {
 *         this.port.setInt(port);
 *         return this;
 *     }
 * }
 * }</pre>
 *
 * @author Mark Bednarczyk
 * @author Sly Technologies Inc.
 * @see Settings#intProperty(String, int)
 */
public final class IntProperty extends Property<Integer> {

    /**
     * Constructs a new IntProperty with the specified name, default value, and domain.
     *
     * @param name         the fully qualified property name
     * @param defaultValue the default value
     * @param domain       the domain this property belongs to, or null
     */
    public IntProperty(String name, int defaultValue, String domain) {
        super(name, defaultValue, domain);
    }

    /**
     * Returns the integer value of this property.
     * 
     * <p>
     * This is a convenience method equivalent to {@link #get()} but returns
     * a primitive {@code int} instead of {@link Integer}.
     * </p>
     *
     * @return the resolved integer value
     */
    public int getInt() {
        return get();
    }

    /**
     * Sets the integer value of this property.
     * 
     * <p>
     * This is a convenience method equivalent to {@link #set(Object)} but
     * accepts a primitive {@code int}.
     * </p>
     *
     * @param value the value to set
     */
    public void setInt(int value) {
        set(value);
    }

    /**
     * Parses a string value into an integer.
     * 
     * <p>
     * Uses {@link Integer#decode(String)} to support decimal, hexadecimal,
     * and octal formats.
     * </p>
     *
     * @param value the string to parse
     * @return the parsed integer value
     * @throws NumberFormatException if the string cannot be parsed
     */
    @Override
    protected Integer parseValue(String value) {
        return Integer.decode(value.trim());
    }
}
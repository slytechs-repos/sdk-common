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
 * A property that holds a string value.
 * 
 * <p>
 * StringProperty provides access to string configuration values with automatic
 * resolution from system properties, environment variables, and domain
 * configuration files. This is useful for paths, hostnames, labels, and other
 * textual configuration.
 * </p>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class ConnectionSettings extends Settings {
 *     private final StringProperty host;
 *     private final StringProperty username;
 *     
 *     public ConnectionSettings() {
 *         super("config", "connection");
 *         this.host = stringProperty("host", "localhost")
 *             .comment("Server hostname or IP address");
 *         this.username = stringProperty("username", null);
 *     }
 *     
 *     public String host() { return host.get(); }
 *     public String username() { return username.get(); }
 *     
 *     public ConnectionSettings withHost(String host) {
 *         this.host.set(host);
 *         return this;
 *     }
 * }
 * }</pre>
 *
 * @author Mark Bednarczyk
 * @author Sly Technologies Inc.
 * @see Settings#stringProperty(String, String)
 */
public final class StringProperty extends Property<String> {

    /**
     * Constructs a new StringProperty with the specified name, default value, and domain.
     *
     * @param name         the fully qualified property name
     * @param defaultValue the default value (may be null)
     * @param domain       the domain this property belongs to, or null
     */
    public StringProperty(String name, String defaultValue, String domain) {
        super(name, defaultValue, domain);
    }

    /**
     * Returns the string value of this property.
     * 
     * <p>
     * This method is equivalent to {@link #get()} and is provided for
     * API consistency with other property types.
     * </p>
     *
     * @return the resolved string value, may be null
     */
    public String getString() {
        return get();
    }

    /**
     * Sets the string value of this property.
     * 
     * <p>
     * This method is equivalent to {@link #set(Object)} and is provided for
     * API consistency with other property types.
     * </p>
     *
     * @param value the value to set (may be null)
     */
    public void setString(String value) {
        set(value);
    }

    /**
     * Parses a string value.
     * 
     * <p>
     * For StringProperty, parsing is identity - the value is returned as-is.
     * </p>
     *
     * @param value the string to parse
     * @return the same string value
     */
    @Override
    protected String parseValue(String value) {
        return value;
    }
}
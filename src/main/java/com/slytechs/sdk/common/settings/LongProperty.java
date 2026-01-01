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
 * A property that holds a long value.
 * 
 * <p>
 * LongProperty provides type-safe access to long integer configuration values
 * with automatic resolution from system properties, environment variables, and
 * domain configuration files. This is particularly useful for memory sizes,
 * byte counts, timestamps, and other large numeric values.
 * </p>
 * 
 * <h2>Parsing</h2>
 * <p>
 * String values are parsed using {@link Long#decode(String)}, which supports:
 * </p>
 * <ul>
 * <li>Decimal: {@code 1234567890123}</li>
 * <li>Hexadecimal: {@code 0x11F71FB04CB} or {@code #11F71FB04CB}</li>
 * <li>Octal: {@code 021756377604313}</li>
 * <li>Negative values: {@code -1234567890123}</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class MemoryPoolSettings extends Settings {
 *     private final LongProperty size;
 *     
 *     public MemoryPoolSettings(String baseName) {
 *         super("config", baseName);
 *         this.size = longProperty("size", 256L * 1024 * 1024)
 *             .comment("FreeListPool size in bytes");
 *     }
 *     
 *     public long size() { return size.getLong(); }
 *     
 *     public MemoryPoolSettings withSize(long bytes) {
 *         size.setLong(bytes);
 *         return this;
 *     }
 * }
 * }</pre>
 *
 * @author Mark Bednarczyk
 * @author Sly Technologies Inc.
 * @see Settings#longProperty(String, long)
 */
public final class LongProperty extends Property<Long> {

    /**
     * Constructs a new LongProperty with the specified name, default value, and domain.
     *
     * @param name         the fully qualified property name
     * @param defaultValue the default value
     * @param domain       the domain this property belongs to, or null
     */
    public LongProperty(String name, long defaultValue, String domain) {
        super(name, defaultValue, domain);
    }

    /**
     * Returns the long value of this property.
     * 
     * <p>
     * This is a convenience method equivalent to {@link #get()} but returns
     * a primitive {@code long} instead of {@link Long}.
     * </p>
     *
     * @return the resolved long value
     */
    public long getLong() {
        return get();
    }

    /**
     * Sets the long value of this property.
     * 
     * <p>
     * This is a convenience method equivalent to {@link #set(Object)} but
     * accepts a primitive {@code long}.
     * </p>
     *
     * @param value the value to set
     */
    public void setLong(long value) {
        set(value);
    }

    /**
     * Parses a string value into a long.
     * 
     * <p>
     * Uses {@link Long#decode(String)} to support decimal, hexadecimal,
     * and octal formats.
     * </p>
     *
     * @param value the string to parse
     * @return the parsed long value
     * @throws NumberFormatException if the string cannot be parsed
     */
    @Override
    protected Long parseValue(String value) {
        return Long.decode(value.trim());
    }
}
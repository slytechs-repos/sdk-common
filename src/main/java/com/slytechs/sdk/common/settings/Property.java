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

import java.util.Objects;
import java.util.Optional;

/**
 * Abstract base class for type-safe configuration properties with automatic
 * resolution from multiple sources.
 * 
 * <p>
 * Property implements a layered resolution strategy that checks multiple
 * sources in priority order to determine the effective value:
 * </p>
 * 
 * <ol>
 * <li><b>Explicit value</b> - Value set programmatically via {@link #set(Object)}</li>
 * <li><b>System property</b> - JVM system property (e.g., {@code -Dtcp.reassembly.timeout=30})</li>
 * <li><b>Environment variable</b> - OS environment variable (e.g., {@code TCP_REASSEMBLY_TIMEOUT=30})</li>
 * <li><b>Domain properties</b> - Loaded from config files via {@link Settings#load}</li>
 * <li><b>Default value</b> - Value provided at property construction</li>
 * </ol>
 * 
 * <p>
 * Resolution is performed lazily on first access and cached until the explicit
 * value changes. This design supports both programmatic configuration and
 * external configuration via system properties, environment variables, or
 * configuration files without code changes.
 * </p>
 * 
 * <h2>Naming Conventions</h2>
 * <p>
 * Property names use dot notation (e.g., {@code tcp.reassembly.timeout}). When
 * resolving from environment variables, dots are converted to underscores and
 * the name is uppercased (e.g., {@code TCP_REASSEMBLY_TIMEOUT}).
 * </p>
 * 
 * <h2>Comments</h2>
 * <p>
 * Properties can have optional comments that are written to configuration files
 * when saved. Use {@link #comment(String)} to set a description.
 * </p>
 * 
 * <h2>Transient Properties</h2>
 * <p>
 * Properties marked as transient via {@link #transient_()} are excluded from
 * save operations. Use this for session-only values that should not be persisted.
 * </p>
 * 
 * <h2>Thread Safety</h2>
 * <p>
 * Properties are designed for configuration at startup time and are not
 * thread-safe for concurrent modification. Once configured, property values
 * should be read but not modified during normal operation.
 * </p>
 *
 * @param <T> the type of value this property holds
 * @author Mark Bednarczyk
 * @author Sly Technologies Inc.
 * @see Settings
 */
public abstract class Property<T> {

    /** The fully qualified property name including base name prefix. */
    private final String name;

    /** The default value used when no other source provides a value. */
    private final T defaultValue;

    /** The domain this property belongs to, or null for standalone. */
    private final String domain;

    /** The explicitly set value, or null if not explicitly set. */
    private T explicitValue;

    /** Cached resolved value to avoid repeated resolution. */
    private T cachedValue;

    /** Flag indicating whether the cached value is valid. */
    private boolean cacheValid;

    /** Optional comment describing this property. */
    private String comment;

    /** If true, property is excluded from save operations. */
    private boolean transient_;

    /**
     * Constructs a new property with the specified name, default value, and domain.
     *
     * @param name         the fully qualified property name (e.g., "tcp.timeout")
     * @param defaultValue the default value to use when no other source provides a value
     * @param domain       the domain this property belongs to, or null for standalone
     * @throws NullPointerException if name is null
     */
    protected Property(String name, T defaultValue, String domain) {
        this.name = Objects.requireNonNull(name, "property name cannot be null");
        this.defaultValue = defaultValue;
        this.domain = domain;
        this.cacheValid = false;
    }

    /**
     * Returns the fully qualified name of this property.
     * 
     * <p>
     * The name includes any base name prefix from the containing {@link Settings}
     * instance (e.g., "packet.memory.pool.size").
     * </p>
     *
     * @return the property name, never null
     */
    public final String name() {
        return name;
    }

    /**
     * Returns the default value for this property.
     * 
     * <p>
     * The default value is used when no explicit value is set and no value is
     * found in system properties, environment variables, or domain properties.
     * </p>
     *
     * @return the default value, may be null
     */
    public final T defaultValue() {
        return defaultValue;
    }

    /**
     * Returns the domain this property belongs to.
     *
     * @return the domain name, or null if standalone
     */
    public final String domain() {
        return domain;
    }

    /**
     * Returns the resolved value of this property.
     * 
     * <p>
     * Resolution follows this priority order:
     * </p>
     * <ol>
     * <li>Explicit value (set via {@link #set(Object)})</li>
     * <li>System property</li>
     * <li>Environment variable</li>
     * <li>Domain properties (loaded config files)</li>
     * <li>Default value</li>
     * </ol>
     * 
     * <p>
     * The resolved value is cached for performance. The cache is invalidated
     * when {@link #set(Object)} is called.
     * </p>
     *
     * @return the resolved value, may be null if default is null and no other
     *         source provides a value
     */
    public final T get() {
        if (!cacheValid) {
            cachedValue = resolve();
            cacheValid = true;
        }
        return cachedValue;
    }

    /**
     * Returns the resolved value as an Optional.
     *
     * @return an Optional containing the resolved value, or empty if the value is null
     */
    public final Optional<T> optional() {
        return Optional.ofNullable(get());
    }

    /**
     * Sets the explicit value for this property.
     * 
     * <p>
     * Setting an explicit value gives it highest priority in the resolution
     * order. Pass {@code null} to clear the explicit value and fall back to
     * system property, environment variable, domain properties, or default.
     * </p>
     *
     * @param value the value to set, or null to clear
     */
    public final void set(T value) {
        this.explicitValue = value;
        this.cacheValid = false;
    }

    /**
     * Checks if an explicit value has been set for this property.
     *
     * @return true if an explicit value is set, false otherwise
     */
    public final boolean isExplicitlySet() {
        return explicitValue != null;
    }

    /**
     * Resets this property to its default state.
     * 
     * <p>
     * Clears any explicit value and invalidates the cache, causing the next
     * {@link #get()} call to re-resolve from system properties, environment
     * variables, domain properties, or the default value.
     * </p>
     */
    public final void reset() {
        this.explicitValue = null;
        this.cacheValid = false;
    }

    /**
     * Invalidates the cache, forcing re-resolution on next access.
     * 
     * <p>
     * Called by Settings when domain properties are reloaded.
     * </p>
     */
    final void invalidateCache() {
        this.cacheValid = false;
    }

    /**
     * Returns the comment for this property.
     *
     * @return the comment, or null if not set
     */
    public final String comment() {
        return comment;
    }

    /**
     * Sets a comment describing this property.
     * 
     * <p>
     * Comments are written to configuration files when the property is saved.
     * </p>
     *
     * @param <P>     the property subtype for fluent chaining
     * @param comment the comment text
     * @return this property for method chaining
     */
    @SuppressWarnings("unchecked")
    public <P extends Property<T>> P comment(String comment) {
        this.comment = comment;
        return (P) this;
    }

    /**
     * Checks if this property is transient.
     * 
     * <p>
     * Transient properties are excluded from save operations.
     * </p>
     *
     * @return true if transient, false otherwise
     */
    public final boolean isTransient() {
        return transient_;
    }

    /**
     * Marks this property as transient.
     * 
     * <p>
     * Transient properties are excluded from save operations. Use this for
     * session-only values that should not be persisted to configuration files.
     * </p>
     *
     * @param <P> the property subtype for fluent chaining
     * @return this property for method chaining
     */
    @SuppressWarnings("unchecked")
    public <P extends Property<T>> P transient_() {
        this.transient_ = true;
        return (P) this;
    }

    /**
     * Resolves the property value from all sources in priority order.
     *
     * @return the resolved value
     */
    private T resolve() {
        // Priority 1: Explicit value
        if (explicitValue != null) {
            return explicitValue;
        }

        // Priority 2: System property (uses property name directly)
        String systemValue = System.getProperty(name);
        if (systemValue != null && !systemValue.isEmpty()) {
            try {
                return parseValue(systemValue);
            } catch (Exception e) {
                // Log warning and continue to next source
                System.err.println("Warning: Failed to parse system property " 
                    + name + "=" + systemValue + ": " + e.getMessage());
            }
        }

        // Priority 3: Environment variable
        String envVarName = toEnvVarName(name);
        String envValue = System.getenv(envVarName);
        if (envValue != null && !envValue.isEmpty()) {
            try {
                return parseValue(envValue);
            } catch (Exception e) {
                // Log warning and continue to next source
                System.err.println("Warning: Failed to parse environment variable " 
                    + envVarName + "=" + envValue + ": " + e.getMessage());
            }
        }

        // Priority 4: Domain properties (global → defaults via Properties chain)
        if (domain != null) {
            String domainValue = Settings.getProperty(domain, name);
            if (domainValue != null && !domainValue.isEmpty()) {
                try {
                    return parseValue(domainValue);
                } catch (Exception e) {
                    // Log warning and continue to default
                    System.err.println("Warning: Failed to parse domain property " 
                        + name + "=" + domainValue + ": " + e.getMessage());
                }
            }
        }

        // Priority 5: Default value
        return defaultValue;
    }

    /**
     * Converts a property name to environment variable format.
     * 
     * <p>
     * Conversion rules:
     * </p>
     * <ul>
     * <li>Replace dots with underscores</li>
     * <li>Convert to uppercase</li>
     * </ul>
     * 
     * <p>
     * Example: {@code tcp.reassembly.timeout} becomes {@code TCP_REASSEMBLY_TIMEOUT}
     * </p>
     *
     * @param propertyName the property name in dot notation
     * @return the environment variable name
     */
    private static String toEnvVarName(String propertyName) {
        return propertyName.replace('.', '_').toUpperCase();
    }

    /**
     * Parses a string value into this property's type.
     * 
     * <p>
     * Subclasses must implement this method to convert string representations
     * from system properties, environment variables, or config files into the
     * appropriate type.
     * </p>
     *
     * @param value the string value to parse, never null or empty
     * @return the parsed value
     * @throws IllegalArgumentException if the value cannot be parsed
     */
    protected abstract T parseValue(String value);

    /**
     * Formats this property's value as a string for serialization.
     * 
     * <p>
     * The default implementation uses {@link Object#toString()}. Subclasses
     * may override for custom formatting.
     * </p>
     *
     * @param value the value to format
     * @return the string representation
     */
    protected String formatValue(T value) {
        return value == null ? "" : value.toString();
    }

    /**
     * Returns the current value formatted as a string.
     * 
     * <p>
     * Used for serialization to properties files.
     * </p>
     *
     * @return the formatted value string
     */
    public final String getAsString() {
        return formatValue(get());
    }

    /**
     * Sets the value by parsing a string representation.
     * 
     * <p>
     * Used for deserialization from properties files.
     * </p>
     *
     * @param value the string value to parse and set
     * @throws IllegalArgumentException if the value cannot be parsed
     */
    public final void setFromString(String value) {
        if (value == null || value.isEmpty()) {
            set(null);
        } else {
            set(parseValue(value));
        }
    }

    /**
     * Returns a string representation of this property for debugging.
     *
     * @return a string representation including name and current value
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(get());
        if (isExplicitlySet()) {
            sb.append(" [explicit]");
        } else {
            sb.append(" [resolved]");
        }
        if (transient_) {
            sb.append(" [transient]");
        }
        return sb.toString();
    }
}
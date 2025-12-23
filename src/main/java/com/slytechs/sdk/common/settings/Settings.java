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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base class for type-safe, hierarchical configuration settings with
 * domain-based organization and layered property resolution.
 * 
 * <p>
 * Settings provides a clean, inheritance-friendly framework for defining
 * configuration properties. Subclasses define properties using protected
 * factory methods and expose them through public getters and fluent setters.
 * </p>
 * 
 * <h2>Design Philosophy</h2>
 * <ul>
 * <li><b>Simple inheritance</b> - Child classes provide a different base name
 *     and optionally add properties</li>
 * <li><b>Protected property creation</b> - Users see only the public API you expose</li>
 * <li><b>Layered resolution</b> - Values resolved from explicit value, system property,
 *     environment variable, loaded config files, or coded default</li>
 * <li><b>Domain grouping</b> - Related settings organized into domains for
 *     grouped save/load operations</li>
 * <li><b>Fluent API</b> - Chainable setters with covariant return types</li>
 * </ul>
 * 
 * <h2>Domains</h2>
 * <p>
 * Domains group related settings together for organized save/load operations.
 * For example, UI-related settings might use domain "ui" while capture settings
 * use domain "capture". This allows saving/loading specific groups of settings
 * to/from separate configuration files.
 * </p>
 * 
 * <h2>Resolution Order</h2>
 * <p>
 * Each property resolves its value using this priority order:
 * </p>
 * <ol>
 * <li><b>Explicit</b> - Value set programmatically via setter</li>
 * <li><b>System Property</b> - {@code -D<baseName>.<propName>=value}</li>
 * <li><b>Environment Variable</b> - {@code <BASENAME>_<PROPNAME>=value}</li>
 * <li><b>Domain Properties</b> - Loaded via {@link #load} or {@link #loadDefaults}</li>
 * <li><b>Default</b> - Value specified in property definition</li>
 * </ol>
 * 
 * <h2>Layered Defaults</h2>
 * <p>
 * Multiple calls to {@link #loadDefaults} layer on top of each other using
 * Java Properties' built-in parent chain. This enables SDK defaults to be
 * overridden by application defaults, which can be overridden by user preferences:
 * </p>
 * <pre>{@code
 * // SDK defaults (lowest priority)
 * Settings.loadDefaults(Sdk.class.getResourceAsStream("/defaults.properties"), "ui");
 * 
 * // Application defaults (override SDK)
 * Settings.loadDefaults(new FileInputStream("app-config.properties"), "ui");
 * 
 * // User preferences (override application)
 * Settings.loadDefaults(new FileInputStream(userPrefs), "ui");
 * }</pre>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class WindowSettings extends Settings {
 *     
 *     private final IntProperty x;
 *     private final IntProperty y;
 *     private final IntProperty sessionCount;
 *     
 *     public WindowSettings() {
 *         super("ui", "window");
 *         setComment("Main window position and size preferences");
 *         
 *         this.x = intProperty("x", 0).comment("X position in pixels");
 *         this.y = intProperty("y", 0).comment("Y position in pixels");
 *         this.sessionCount = intProperty("session.count", 0).transient_();
 *     }
 *     
 *     public int x() { return x.getInt(); }
 *     public int y() { return y.getInt(); }
 *     
 *     public WindowSettings withX(int x) {
 *         this.x.setInt(x);
 *         return this;
 *     }
 * }
 * 
 * // Save all UI settings
 * Settings.save(new File("ui.properties"), "ui");
 * 
 * // Load UI settings
 * Settings.load(new FileInputStream("ui.properties"), "ui");
 * }</pre>
 * 
 * <h2>Thread Safety</h2>
 * <p>
 * The static registry and property maps are thread-safe for registration and
 * lookup. Individual Settings instances are designed for configuration at
 * startup time and should not be modified concurrently.
 * </p>
 *
 * @author Mark Bednarczyk
 * @author Sly Technologies Inc.
 * @see Property
 */
public abstract class Settings {

    // =========================================================================
    // Static Domain Registry
    // =========================================================================

    /** Domain → registered Settings instances. */
    private static final Map<String, Set<Settings>> registry = new ConcurrentHashMap<>();

    /** Domain → current session properties (replaced on each load). */
    private static final Map<String, Properties> global = new ConcurrentHashMap<>();

    /** Domain → layered defaults (compounded on each loadDefaults). */
    private static final Map<String, Properties> defaults = new ConcurrentHashMap<>();

    /**
     * Loads default properties for a domain, layering on any previous defaults.
     * 
     * <p>
     * Call order matters: SDK defaults first, then application defaults, then
     * user preferences. Each subsequent call layers on top of the previous,
     * with later values taking precedence.
     * </p>
     * 
     * <pre>{@code
     * // SDK defaults (lowest priority)
     * Settings.loadDefaults(Sdk.class.getResourceAsStream("/defaults.properties"), "capture");
     * 
     * // User preferences (highest priority)
     * Settings.loadDefaults(new FileInputStream(userPrefs), "capture");
     * }</pre>
     *
     * @param in     the input stream to read properties from
     * @param domain the domain to load defaults for
     * @throws IOException if an I/O error occurs
     */
    public static void loadDefaults(InputStream in, String domain) throws IOException {
        Objects.requireNonNull(in, "input stream cannot be null");
        Objects.requireNonNull(domain, "domain cannot be null");

        Properties oldDefaults = defaults.get(domain);
        Properties newDefaults = new Properties(oldDefaults); // old as parent
        newDefaults.load(in);
        defaults.put(domain, newDefaults);

        // Invalidate caches for all properties in this domain
        invalidateDomainCaches(domain);
    }

    /**
     * Loads default properties for a domain from a file.
     * 
     * <p>
     * Convenience method that opens a FileInputStream for the specified file.
     * </p>
     *
     * @param file   the file to read properties from
     * @param domain the domain to load defaults for
     * @throws IOException if an I/O error occurs
     * @see #loadDefaults(InputStream, String)
     */
    public static void loadDefaults(File file, String domain) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            loadDefaults(in, domain);
        }
    }

    /**
     * Loads session properties for a domain, replacing any previous load.
     * 
     * <p>
     * The loaded properties use the domain's defaults chain as a fallback.
     * Unlike {@link #loadDefaults}, multiple calls to load() replace rather
     * than layer.
     * </p>
     *
     * @param in     the input stream to read properties from
     * @param domain the domain to load properties for
     * @throws IOException if an I/O error occurs
     */
    public static void load(InputStream in, String domain) throws IOException {
        Objects.requireNonNull(in, "input stream cannot be null");
        Objects.requireNonNull(domain, "domain cannot be null");

        Properties domainDefaults = defaults.get(domain);
        Properties props = new Properties(domainDefaults); // defaults as parent
        props.load(in);
        global.put(domain, props); // replaces any previous

        // Invalidate caches for all properties in this domain
        invalidateDomainCaches(domain);
    }

    /**
     * Loads session properties for a domain from a file.
     *
     * @param file   the file to read properties from
     * @param domain the domain to load properties for
     * @throws IOException if an I/O error occurs
     * @see #load(InputStream, String)
     */
    public static void load(File file, String domain) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            load(in, domain);
        }
    }

    /**
     * Saves all non-transient properties in a domain to an output stream.
     * 
     * <p>
     * Properties are written with their comments. Settings with comments are
     * written with a header comment. Transient properties are excluded.
     * </p>
     *
     * @param out    the output stream to write to
     * @param domain the domain to save
     * @throws IOException if an I/O error occurs
     */
    public static void save(OutputStream out, String domain) throws IOException {
        Objects.requireNonNull(out, "output stream cannot be null");
        Objects.requireNonNull(domain, "domain cannot be null");

        Set<Settings> domainSettings = registry.get(domain);
        if (domainSettings == null || domainSettings.isEmpty()) {
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(out, StandardCharsets.UTF_8))) {

            // Write file header
            writer.write("# Domain: ");
            writer.write(domain);
            writer.newLine();
            writer.write("# Generated: ");
            writer.write(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            writer.newLine();
            writer.newLine();

            // Write each settings instance
            for (Settings settings : domainSettings) {
                writeSettings(writer, settings);
            }
        }
    }

    /**
     * Saves all non-transient properties in a domain to a file.
     *
     * @param file   the file to write to
     * @param domain the domain to save
     * @throws IOException if an I/O error occurs
     * @see #save(OutputStream, String)
     */
    public static void save(File file, String domain) throws IOException {
        try (OutputStream out = new FileOutputStream(file)) {
            save(out, domain);
        }
    }

    /**
     * Writes a single Settings instance to the output.
     */
    private static void writeSettings(BufferedWriter writer, Settings settings) throws IOException {
        // Settings header comment
        writer.write("# ");
        writer.write(settings.getClass().getSimpleName());
        writer.newLine();

        if (settings.comment != null && !settings.comment.isEmpty()) {
            writer.write("# ");
            writer.write(settings.comment);
            writer.newLine();
        }
        writer.newLine();

        // Write properties
        for (Property<?> property : settings.properties) {
            if (property.isTransient()) {
                continue;
            }

            // Property comment
            if (property.comment() != null && !property.comment().isEmpty()) {
                writer.write("# ");
                writer.write(property.comment());
                writer.newLine();
            }

            // Property value
            writer.write(property.name());
            writer.write("=");
            writer.write(property.getAsString());
            writer.newLine();
        }

        writer.newLine();
    }

    /**
     * Gets a property value from the domain's property chain.
     * 
     * <p>
     * Resolution order: global → defaults chain (via Properties parent).
     * </p>
     *
     * @param domain the domain to look up
     * @param name   the property name
     * @return the property value, or null if not found
     */
    static String getProperty(String domain, String name) {
        // Check global first (includes defaults via parent chain)
        Properties props = global.get(domain);
        if (props != null) {
            String value = props.getProperty(name);
            if (value != null) {
                return value;
            }
        }

        // Fall back to defaults only (if no global loaded)
        props = defaults.get(domain);
        return props != null ? props.getProperty(name) : null;
    }

    /**
     * Invalidates caches for all properties in the specified domain.
     */
    private static void invalidateDomainCaches(String domain) {
        Set<Settings> domainSettings = registry.get(domain);
        if (domainSettings != null) {
            for (Settings settings : domainSettings) {
                for (Property<?> property : settings.properties) {
                    property.invalidateCache();
                }
            }
        }
    }

    /**
     * Clears all loaded properties and defaults for a domain.
     * 
     * <p>
     * Does not unregister Settings instances; they remain registered but
     * will resolve to their coded defaults.
     * </p>
     *
     * @param domain the domain to clear
     */
    public static void clear(String domain) {
        global.remove(domain);
        defaults.remove(domain);
        invalidateDomainCaches(domain);
    }

    /**
     * Returns an unmodifiable view of all registered settings for a domain.
     *
     * @param domain the domain to query
     * @return set of registered settings, or empty set if none
     */
    public static Set<Settings> getSettings(String domain) {
        Set<Settings> domainSettings = registry.get(domain);
        return domainSettings != null 
            ? Collections.unmodifiableSet(domainSettings)
            : Collections.emptySet();
    }

    // =========================================================================
    // Instance Fields
    // =========================================================================

    /** The domain this settings belongs to, or null for standalone. */
    private final String domain;

    /** The base name prefix for all properties in this settings instance. */
    private final String baseName;

    /** List of all properties for serialization support. */
    private final List<Property<?>> properties;

    /** Optional comment describing this settings instance. */
    private String comment;

    // =========================================================================
    // Constructors
    // =========================================================================

    /**
     * Constructs a standalone Settings instance with no domain registration.
     * 
     * <p>
     * Standalone settings do not participate in domain-based save/load operations.
     * Properties will not resolve from loaded config files.
     * </p>
     *
     * @param baseName the base name prefix for properties
     * @throws NullPointerException if baseName is null
     */
    protected Settings(String baseName) {
        this(null, baseName);
    }

    /**
     * Constructs a Settings instance registered with the specified domain.
     * 
     * <p>
     * The base name forms the prefix for all property names. The domain
     * determines which config files properties are loaded from and saved to.
     * </p>
     *
     * @param domain   the domain for grouping (e.g., "ui", "capture"), or null for standalone
     * @param baseName the base name prefix for properties (e.g., "window", "tcp.reassembly")
     * @throws NullPointerException if baseName is null
     */
    protected Settings(String domain, String baseName) {
        Objects.requireNonNull(baseName, "baseName cannot be null");

        // Normalize: ensure no trailing dot
        this.baseName = baseName.endsWith(".")
            ? baseName.substring(0, baseName.length() - 1)
            : baseName;
        this.domain = domain;
        this.properties = new ArrayList<>();

        // Register with domain
        if (domain != null) {
            registry.computeIfAbsent(domain, k -> ConcurrentHashMap.newKeySet())
                    .add(this);
        }
    }

    // =========================================================================
    // Instance Methods
    // =========================================================================

    /**
     * Returns the domain this settings belongs to.
     *
     * @return the domain name, or null if standalone
     */
    public final String domain() {
        return domain;
    }

    /**
     * Returns the base name prefix for this settings instance.
     *
     * @return the base name, never null
     */
    public final String baseName() {
        return baseName;
    }

    /**
     * Returns an unmodifiable view of all properties in this settings instance.
     *
     * @return unmodifiable list of properties
     */
    public final List<Property<?>> properties() {
        return Collections.unmodifiableList(properties);
    }

    /**
     * Returns the comment for this settings instance.
     *
     * @return the comment, or null if not set
     */
    public final String comment() {
        return comment;
    }

    /**
     * Sets a comment describing this settings instance.
     * 
     * <p>
     * Comments are written to configuration files as a header above this
     * settings' properties.
     * </p>
     *
     * @param comment the comment text
     * @return this settings instance for method chaining
     */
    public Settings setComment(String comment) {
        this.comment = comment;
        return this;
    }

    /**
     * Creates the fully qualified property name by combining the base name
     * with the property name.
     *
     * @param propertyName the property name (e.g., "timeout")
     * @return the fully qualified name (e.g., "tcp.reassembly.timeout")
     */
    private String qualifiedName(String propertyName) {
        return baseName + "." + propertyName;
    }

    /**
     * Registers a property with this settings instance.
     *
     * @param <P>      the property type
     * @param property the property to register
     * @return the same property for method chaining
     */
    private <P extends Property<?>> P register(P property) {
        properties.add(property);
        return property;
    }

    /**
     * Resets all properties to their default values.
     */
    public void reset() {
        for (Property<?> property : properties) {
            property.reset();
        }
    }

    // =========================================================================
    // Property Factory Methods
    // =========================================================================

    /**
     * Creates a new integer property with the specified name and default value.
     *
     * @param name         the property name (e.g., "timeout")
     * @param defaultValue the default value
     * @return a new IntProperty instance
     */
    protected final IntProperty intProperty(String name, int defaultValue) {
        return register(new IntProperty(qualifiedName(name), defaultValue, domain));
    }

    /**
     * Creates a new long property with the specified name and default value.
     *
     * @param name         the property name
     * @param defaultValue the default value
     * @return a new LongProperty instance
     */
    protected final LongProperty longProperty(String name, long defaultValue) {
        return register(new LongProperty(qualifiedName(name), defaultValue, domain));
    }

    /**
     * Creates a new boolean property with the specified name and default value.
     *
     * @param name         the property name
     * @param defaultValue the default value
     * @return a new BooleanProperty instance
     */
    protected final BooleanProperty booleanProperty(String name, boolean defaultValue) {
        return register(new BooleanProperty(qualifiedName(name), defaultValue, domain));
    }

    /**
     * Creates a new string property with the specified name and default value.
     *
     * @param name         the property name
     * @param defaultValue the default value (may be null)
     * @return a new StringProperty instance
     */
    protected final StringProperty stringProperty(String name, String defaultValue) {
        return register(new StringProperty(qualifiedName(name), defaultValue, domain));
    }

    /**
     * Creates a new double property with the specified name and default value.
     *
     * @param name         the property name
     * @param defaultValue the default value
     * @return a new DoubleProperty instance
     */
    protected final DoubleProperty doubleProperty(String name, double defaultValue) {
        return register(new DoubleProperty(qualifiedName(name), defaultValue, domain));
    }

    /**
     * Creates a new float property with the specified name and default value.
     *
     * @param name         the property name
     * @param defaultValue the default value
     * @return a new FloatProperty instance
     */
    protected final FloatProperty floatProperty(String name, float defaultValue) {
        return register(new FloatProperty(qualifiedName(name), defaultValue, domain));
    }

    /**
     * Creates a new enum property with the specified name and default value.
     *
     * @param <E>          the enum type
     * @param name         the property name
     * @param defaultValue the default value (also determines the enum type)
     * @return a new EnumProperty instance
     */
    protected final <E extends Enum<E>> EnumProperty<E> enumProperty(String name, E defaultValue) {
        return register(new EnumProperty<>(qualifiedName(name), defaultValue, domain));
    }

    // =========================================================================
    // Object Methods
    // =========================================================================

    /**
     * Returns a string representation of this settings instance.
     *
     * @return string representation including class name, domain, and base name
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append("[");
        if (domain != null) {
            sb.append("domain=").append(domain).append(", ");
        }
        sb.append("baseName=").append(baseName);
        sb.append("]");
        return sb.toString();
    }

    /**
     * Returns a detailed string representation including all property values.
     *
     * @return detailed string with all properties
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append("[");
        if (domain != null) {
            sb.append("domain=").append(domain).append(", ");
        }
        sb.append("baseName=").append(baseName);
        sb.append("] {\n");
        for (Property<?> property : properties) {
            sb.append("  ").append(property.toString()).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
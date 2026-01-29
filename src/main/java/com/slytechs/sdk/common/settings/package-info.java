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

/**
 * Type-safe, hierarchical configuration settings framework with domain-based
 * organization and layered property resolution.
 * 
 * <h2>Overview</h2>
 * <p>
 * This package provides a clean, inheritance-friendly framework for defining
 * configuration properties. Settings classes define properties using protected
 * factory methods and expose them through public getters and fluent setters.
 * Properties are organized into domains for grouped save/load operations.
 * </p>
 * 
 * <h2>Key Features</h2>
 * <ul>
 * <li><b>Type Safety</b> - Strongly typed properties with compile-time checking</li>
 * <li><b>Hierarchical Resolution</b> - Values resolved from explicit settings,
 *     system properties, environment variables, config files, or defaults</li>
 * <li><b>Domain Organization</b> - Related settings grouped for organized I/O</li>
 * <li><b>Layered Defaults</b> - SDK defaults, application config, user preferences</li>
 * <li><b>Clean Inheritance</b> - Base settings with common properties, specialized
 *     settings with additional properties and different namespaces</li>
 * <li><b>Fluent API</b> - Chainable configuration with covariant return types</li>
 * <li><b>Comments</b> - Self-documenting configuration files</li>
 * <li><b>Transient Properties</b> - SystemSession-only values excluded from persistence</li>
 * </ul>
 * 
 * <h2>Resolution Order</h2>
 * <p>
 * Each property resolves its value from the first available source:
 * </p>
 * <ol>
 * <li><b>Explicit Value</b> - Set programmatically via setter method</li>
 * <li><b>System Property</b> - {@code -D<baseName>.<propName>=value}</li>
 * <li><b>Environment Variable</b> - {@code <BASENAME>_<PROPNAME>=value}</li>
 * <li><b>Domain Properties</b> - Loaded via {@link Settings#load} or {@link Settings#loadDefaults}</li>
 * <li><b>Default Value</b> - Specified in property definition</li>
 * </ol>
 * 
 * <h2>Domains</h2>
 * <p>
 * Domains group related settings together for organized save/load operations.
 * For example:
 * </p>
 * <ul>
 * <li>{@code "ui"} - Window positions, panel visibility, themes</li>
 * <li>{@code "capture"} - Buffer sizes, timeout values, filter settings</li>
 * <li>{@code "preferences"} - User preferences across the application</li>
 * </ul>
 * 
 * <h2>Quick Start</h2>
 * 
 * <h3>Define a Settings Class</h3>
 * <pre>{@code
 * public class WindowSettings extends Settings {
 *     
 *     private final IntProperty x;
 *     private final IntProperty y;
 *     private final IntProperty width;
 *     private final IntProperty height;
 *     private final IntProperty sessionCount;
 *     
 *     public WindowSettings() {
 *         super("ui", "window");  // domain, baseName
 *         setComment("Main window position and size preferences");
 *         
 *         this.x = intProperty("x", 0).comment("X position in pixels");
 *         this.y = intProperty("y", 0).comment("Y position in pixels");
 *         this.width = intProperty("width", 1280);
 *         this.height = intProperty("height", 720);
 *         this.sessionCount = intProperty("session.count", 0).transient_();
 *     }
 *     
 *     // Getters
 *     public int x() { return x.getInt(); }
 *     public int y() { return y.getInt(); }
 *     public int width() { return width.getInt(); }
 *     public int height() { return height.getInt(); }
 *     
 *     // Fluent setters
 *     public WindowSettings withPosition(int x, int y) {
 *         this.x.setInt(x);
 *         this.y.setInt(y);
 *         return this;
 *     }
 * }
 * }</pre>
 * 
 * <h3>Layered Configuration</h3>
 * <pre>{@code
 * // Application startup
 * 
 * // 1. Load SDK defaults (bundled in jar) - lowest priority
 * Settings.loadDefaults(App.class.getResourceAsStream("/defaults/ui.properties"), "ui");
 * 
 * // 2. Load user preferences - highest priority  
 * Settings.loadDefaults(new FileInputStream(userPrefsFile), "ui");
 * 
 * // Create settings - values resolve from loaded config
 * var windowSettings = new WindowSettings();
 * int x = windowSettings.x();  // From user prefs, or SDK default
 * 
 * // On application exit - save all UI settings
 * Settings.save(new File(userPrefsFile), "ui");
 * }</pre>
 * 
 * <h3>Inheritance for Specialization</h3>
 * <pre>{@code
 * // Base settings
 * public class PoolSettings extends Settings {
 *     private final LongProperty size;
 *     
 *     public PoolSettings(String domain, String baseName) {
 *         super(domain, baseName);
 *         this.size = longProperty("size", 256L * 1024 * 1024);
 *     }
 *     
 *     public long size() { return size.getLong(); }
 *     
 *     public PoolSettings withSize(long bytes) {
 *         size.setLong(bytes);
 *         return this;
 *     }
 * }
 * 
 * // Specialized settings - different namespace, additional properties
 * public class PacketPoolSettings extends PoolSettings {
 *     private final IntProperty maxPacketSize;
 *     
 *     public PacketPoolSettings() {
 *         super("capture", "packet.pool");  // Different domain and baseName
 *         this.maxPacketSize = intProperty("max.packet.size", 9000);
 *     }
 *     
 *     public int maxPacketSize() { return maxPacketSize.getInt(); }
 *     
 *     @Override
 *     public PacketPoolSettings withSize(long bytes) {
 *         super.withSize(bytes);
 *         return this;
 *     }
 *     
 *     public PacketPoolSettings withMaxPacketSize(int size) {
 *         maxPacketSize.setInt(size);
 *         return this;
 *     }
 * }
 * }</pre>
 * 
 * <h2>Property Types</h2>
 * <table border="1">
 * <caption>Available Property Types</caption>
 * <tr><th>Property Type</th><th>Java Type</th><th>Factory Method</th></tr>
 * <tr><td>{@link IntProperty}</td><td>int/Integer</td><td>{@code intProperty(name, default)}</td></tr>
 * <tr><td>{@link LongProperty}</td><td>long/Long</td><td>{@code longProperty(name, default)}</td></tr>
 * <tr><td>{@link BooleanProperty}</td><td>boolean/Boolean</td><td>{@code booleanProperty(name, default)}</td></tr>
 * <tr><td>{@link StringProperty}</td><td>String</td><td>{@code stringProperty(name, default)}</td></tr>
 * <tr><td>{@link DoubleProperty}</td><td>double/Double</td><td>{@code doubleProperty(name, default)}</td></tr>
 * <tr><td>{@link FloatProperty}</td><td>float/Float</td><td>{@code floatProperty(name, default)}</td></tr>
 * <tr><td>{@link EnumProperty}</td><td>Enum&lt;E&gt;</td><td>{@code enumProperty(name, default)}</td></tr>
 * </table>
 * 
 * <h2>Configuration File Format</h2>
 * <p>
 * Configuration files use standard Java properties format with comments:
 * </p>
 * <pre>
 * # Domain: ui
 * # Generated: 2024-01-15T10:30:00
 * 
 * # WindowSettings
 * # Main window position and size preferences
 * 
 * # X position in pixels
 * window.x=150
 * 
 * # Y position in pixels
 * window.y=100
 * 
 * window.width=1920
 * window.height=1080
 * </pre>
 * 
 * <h2>Thread Safety</h2>
 * <p>
 * The static domain registry is thread-safe for registration and lookup.
 * Individual Settings instances are designed for configuration at startup time.
 * Configure once, then read values during operation.
 * </p>
 *
 * @author Mark Bednarczyk
 * @author Sly Technologies Inc.
 * @see Settings
 * @see Property
 */
package com.slytechs.sdk.common.settings;
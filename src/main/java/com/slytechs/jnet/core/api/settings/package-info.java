/**
 * Provides a comprehensive type-safe settings and configuration management
 * framework. This package offers a flexible system for handling various types
 * of configuration properties with support for type safety, change
 * notifications, and system property integration.
 * 
 * <h2>Key Features</h2>
 * <ul>
 * <li>Type-safe property handling for multiple data types</li>
 * <li>Support for primitive types, strings, enums, and lists</li>
 * <li>Change notification system with action listeners</li>
 * <li>Seamless system property integration</li>
 * <li>Optional value handling with null safety</li>
 * <li>Support for both signed and unsigned numeric types</li>
 * <li>Fluent API design for easy configuration</li>
 * </ul>
 * 
 * <h2>Main Components</h2>
 * <ul>
 * <li>{@link com.slytechs.jnet.platform.api.common.settings.Settings} - The main
 * settings container class</li>
 * <li>{@link com.slytechs.jnet.platform.api.common.settings.Property} - Base class
 * for all property types</li>
 * <li>Type-specific property implementations (e.g., IntProperty,
 * StringProperty)</li>
 * <li>Unsigned number support (UnsignedByteProperty, UnsignedIntProperty,
 * etc.)</li>
 * </ul>
 * 
 * <h2>Usage Examples</h2>
 * 
 * <p>
 * <b>Basic Settings Configuration:</b>
 * </p>
 * 
 * <pre>
 * class ServerSettings extends Settings {
 * 	private final IntProperty port = ofInt("server.port", 8080)
 * 			.on((newValue, source) -> System.out.println("Port changed to: " + newValue));
 * 
 * 	private final StringProperty host = ofString("server.host", "localhost");
 * 
 * 	public ServerSettings() {
 * 		super("ServerSettings");
 * 		enableUpdates(true);
 * 	}
 * }
 * </pre>
 * 
 * <p>
 * <b>Using Different Property Types:</b>
 * </p>
 * 
 * <pre>
 * // Enum properties
 * EnumProperty&lt;TimeUnit&gt; timeUnit = settings.ofEnum("time.unit", TimeUnit.SECONDS);
 * 
 * // List properties
 * ListProperty&lt;String&gt; allowedHosts = settings.ofList("allowed.hosts", String::trim);
 * allowedHosts.parseValue("localhost,127.0.0.1,example.com");
 * 
 * // Unsigned number properties
 * UnsignedShortProperty port = settings.of("port", new UnsignedShortProperty("port", 443));
 * </pre>
 * 
 * <p>
 * <b>System Property Integration:</b>
 * </p>
 * 
 * <pre>
 * // Will load from system property if available
 * IntProperty timeout = settings.ofInt("connection.timeout", 5000)
 * 		.systemProperty();
 * </pre>
 * 
 * <h2>Property Change Notifications</h2>
 * 
 * <pre>
 * property.on((newValue, source) -> {
 * 	System.out.println("Value changed to: " + newValue + " from source: " + source);
 * });
 * </pre>
 * 
 * <h2>Thread Safety</h2>
 * <p>
 * The settings framework provides thread-safe property access and updates
 * through synchronized methods where appropriate. However, custom action
 * listeners should implement their own synchronization if required.
 * </p>
 * 
 * <h2>Best Practices</h2>
 * <ul>
 * <li>Group related settings in a custom Settings subclass</li>
 * <li>Use meaningful property names, typically in dot notation (e.g.,
 * "app.feature.setting")</li>
 * <li>Provide default values for properties where appropriate</li>
 * <li>Enable updates before registering change listeners</li>
 * <li>Use the most specific property type for your data</li>
 * </ul>
 * 
 * @see com.slytechs.jnet.platform.api.common.settings.Settings
 * @see com.slytechs.jnet.platform.api.common.settings.Property
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc
 */
package com.slytechs.jnet.core.api.settings;
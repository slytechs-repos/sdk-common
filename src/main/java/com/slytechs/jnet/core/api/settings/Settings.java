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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

import com.slytechs.jnet.core.api.settings.Property.Action;
import com.slytechs.jnet.core.api.settings.Property.Deserializer;
import com.slytechs.jnet.core.api.settings.Property.PropertyFactory;
import com.slytechs.jnet.core.api.settings.Property.PropertyFactoryWithValue;
import com.slytechs.jnet.core.api.settings.Property.Serializer;
import com.slytechs.jnet.core.api.util.Registration;

/**
 * A flexible and extensible settings management system that provides type-safe
 * property handling with support for various data types, default values, and
 * change notifications.
 * 
 * <p>
 * The Settings class serves as a central repository for application
 * configuration, allowing for dynamic property management with type safety and
 * change tracking. It supports various property types including primitives,
 * enums, strings, and lists, with the ability to register listeners for
 * property changes.
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * class MySettings extends Settings {
 * 	private final IntProperty port = ofInt("server.port", 8080)
 * 			.on((newValue, source) -> System.out.println("Port changed to: " + newValue));
 * 
 * 	public MySettings() {
 * 		super("MyServerSettings");
 * 		enableUpdates(true);
 * 	}
 * }
 * </pre>
 *
 * @param <T_BASE> the generic type
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc.
 */
public class Settings<T_BASE extends Settings<T_BASE>> {

	/**
	 * The Interface CleanSettingsNotification.
	 *
	 * @param <T> the generic type
	 */
	interface CleanSettingsNotification<T extends Settings<T>> {

		/**
		 * On clear settings.
		 *
		 * @param settings the settings
		 */
		void onClearSettings(T settings);
	}

	/**
	 * The Interface ResetSettingsNotification.
	 *
	 * @param <T> the generic type
	 */
	interface ResetSettingsNotification<T extends Settings<T>> {

		/**
		 * On reset settings.
		 *
		 * @param settings the settings
		 */
		void onResetSettings(T settings);
	}

	/**
	 * The main method.
	 *
	 * @param __ the arguments
	 */
	// TODO: Remove after testing and validation period.
	public static void main(String[] __) {

		class MySettings extends Settings<MySettings> {
			private static final String BASENAME = "ports";

			private final IntProperty x = super.<Integer, IntProperty>newProperty("delay", 10, IntProperty::new)
					.on(Action.withSource(this::setX_withSource, this).andThen(this::setY_simpleSetter))
					.loadSystemProperty();

			private final EnumProperty<TimeUnit> e = super.<TimeUnit, EnumProperty<TimeUnit>>newProperty("timeunit",
					TimeUnit.MICROSECONDS, EnumProperty::new)
					.on(Action.withSource(this::setTimeUnit, this));

			private final ListProperty<TimeUnit> l = super.<TimeUnit>newListProperty("ids", TimeUnit::valueOf,
					TimeUnit.MICROSECONDS);

			MySettings(String name) {
				super(BASENAME, name);

				super.onClear(settings -> System.out.println("clear:: " + settings));
				super.onReset(settings -> System.out.println("reset:: " + settings));

				/*
				 * Action's on settings are disabled by default so that they don't fire updates
				 * before class fields are initialized and actually assigned which happens
				 * before the code in the constructor is executed. So we need to enable them in
				 * the constructor and then force fire actions which will update
				 */
				enableActions(true);
				updateAllActions();
			}

			public int getX() {
				return x.getInt();
			}

			public void setTimeUnit(TimeUnit unit, Object source) {
				System.out.println(unit + ", source=" + source);
			}

			public MySettings setX_withSource(int newValue, Object source) {
				if (source != this)
					x.setValue(newValue, this);

				System.out.println(name() + "::setX x=" + x.getInt() + ", source=" + source);

				return this;
			}

			public void setY_simpleSetter(int newValue) {
				System.out.println(name() + "::setY x=" + newValue);
			}

		}

		StringProperty property = new IntProperty("property.id", 11)
				.on(newValue -> System.out.println("on:: property.id = " + newValue))
				.setFormat("0x%08X")
				.map(i -> String.valueOf(i))
				.mapToFormattedString()

		;

		System.out.println("main:: " + property);

		MySettings s1 = new MySettings("s1")
				.setX_withSource(20, null);

		MySettings s2 = new MySettings("s2")
				.setX_withSource(30, null);

		s1.e.setEnum(TimeUnit.DAYS);

		System.out.println("---");
		s1.mergeValues(s2);

		System.out.println("s2:: direct getX() = " + s2.getX());
		s2.clear();
		s1.reset();
	}

	/** The name. */
	// Private fields
	private String name = getClass().getSimpleName();
	
	/** The us. */
	@SuppressWarnings("unchecked")
	private final T_BASE us = (T_BASE) this;
	
	/** The settings support. */
	private final SettingsSupport settingsSupport = new SettingsSupport();
	
	/** The properties. */
	final List<Property<?, ?>> properties = new ArrayList<>();
	
	/** The clear actions. */
	private final List<CleanSettingsNotification<T_BASE>> clearActions = new ArrayList<>();
	
	/** The reset actions. */
	private final List<ResetSettingsNotification<T_BASE>> resetActions = new ArrayList<>();

	/** The base name. */
	private final String baseName;

	/**
	 * Creates a new Settings instance with updates disabled by default.
	 *
	 * @param baseName the base name
	 */
	public Settings(String baseName) {
		this.baseName = baseName.endsWith(".") ? baseName : baseName + ".";
		settingsSupport.enableFireEvents = false;
	}

	/**
	 * Creates a new Settings instance with the specified name and updates disabled
	 * by default.
	 *
	 * @param baseName the base name
	 * @param name     the name of this settings instance, used for identification
	 *                 and logging
	 */
	public Settings(String baseName, String name) {
		this.baseName = baseName.endsWith(".") ? baseName : baseName + ".";
		this.name = name;
		settingsSupport.enableFireEvents = false;
	}

	/**
	 * Creates the property name.
	 *
	 * @param name the name
	 * @return the string
	 */
	protected String createPropertyName(String name) {
		return baseName + name;
	}

	/**
	 * Reads and loads property values from a Java properties format input stream.
	 * The stream may contain multiple sections of properties, each identified by a
	 * header in the format "# [section_name]". Properties are loaded from the
	 * section matching this settings instance's name (from {@link #name()}).
	 * 
	 * <p>
	 * Example properties file format:
	 * </p>
	 * 
	 * <pre>
	* # Properties without a section header go to default section
	* global.property=value
	* 
	* # [network]
	* port=8080
	* host=localhost
	* 
	* # [security]
	* ssl.enabled=true
	* keystore.path=/path/to/keystore
	 * </pre>
	 * 
	 * <p>
	 * Properties without a section header are stored in a default section and will
	 * be loaded if no matching section is found for the settings name. This is
	 * useful for single-section files that don't have an explicit header.
	 * </p>
	 *
	 * @param input the input stream containing the property values in Java
	 *              properties format
	 * @return this settings instance for method chaining
	 * @throws IOException if an I/O error occurs while reading from the stream
	 * @see #name()
	 * @see #writeSettings(OutputStream)
	 */
	public T_BASE readSettings(InputStream input) throws IOException {
		var reader = new SettingsReader(input);
		reader.read(this);
		return us;
	}

	/**
	 * Writes the current property values to an output stream in Java properties
	 * format, within a section identified by this settings instance's name. The
	 * output can be read back using {@link #readSettings(InputStream)}.
	 * 
	 * <p>
	 * The properties are written with a section header in the format "#
	 * [section_name]" where section_name is obtained from {@link #name()}. Multiple
	 * settings can be written to the same file by calling writeSettings on
	 * different settings instances with the same output stream, creating a
	 * multi-section properties file.
	 * </p>
	 * 
	 * <p>
	 * Example usage for writing multiple sections:
	 * </p>
	 * 
	 * <pre>
	 * try (var output = new FileOutputStream("config.properties")) {
	 * 	networkSettings.writeSettings(output); // Writes [network] section
	 * 	securitySettings.writeSettings(output); // Appends [security] section
	 * }
	 * </pre>
	 *
	 * @param output the output stream to write the property values to
	 * @return this settings instance for method chaining
	 * @throws IOException if an I/O error occurs while writing to the stream
	 * @see #name()
	 * @see #readSettings(InputStream)
	 */
	public T_BASE writeSettings(OutputStream output) throws IOException {
		var writer = new SettingsWriter(output);
		writer.write(this);
		return us;
	}

	/**
	 * Clears all properties in this settings instance, resetting them to their
	 * unset state. This operation removes all current values and triggers any
	 * registered clear notifications. Clear actions are only triggered if property
	 * change notifications are enabled.
	 * 
	 * @return this settings instance for method chaining
	 * @see #onClear(CleanSettingsNotification)
	 */
	public T_BASE clear() {
		for (var p : properties) {
			p.clear();
		}

		if (settingsSupport.enableFireEvents)
			clearActions.forEach(a -> a.onClearSettings(us));

		return us;
	}

	/**
	 * Controls whether property changes trigger registered action listeners. When
	 * actions are disabled, property values can still be changed, but listeners
	 * will not be notified of the changes. Re-enabling actions does not
	 * retroactively trigger notifications for changes that occurred while actions
	 * were disabled.
	 * 
	 * <p>
	 * This method is thread-safe and can be used to temporarily suspend action
	 * notifications during batch updates.
	 * </p>
	 * 
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>
	 * Settings settings = new Settings("config");
	 * IntProperty port = settings.newIntProperty("port", 8080)
	 * 		.on((value, source) -> System.out.println("Port changed to: " + value));
	 * 
	 * settings.enableActions(false); // Disable notifications
	 * port.setValue(9090); // Listener won't be called
	 * port.setValue(9091); // Listener won't be called
	 * settings.enableActions(true); // Re-enable notifications
	 * port.setValue(9092); // Listener will be called
	 * </pre>
	 *
	 * @param b true to enable action notifications, false to disable them
	 */
	public synchronized void enableActions(boolean b) {
		settingsSupport.enableEventDispatching(b);
	}

	/**
	 * Locates a property by its name in the properties collection.
	 *
	 * @param <T>      the generic type
	 * @param <P_BASE> the generic type
	 * @param name     the name of the property to find
	 * @return the found property or null if not found
	 */
	@SuppressWarnings("unchecked")
	private <T, P_BASE extends Property<T, P_BASE>> P_BASE findProperty(String name) {
		for (var p : properties) {
			if (p.name().equals(name))
				return (P_BASE) p;
		}
		return null;
	}

	/**
	 * Fires a property change event for the specified property.
	 *
	 * @param property the property for which to fire the change event
	 */
	private void fireForProperty(Property<?, ?> property) {
		settingsSupport.fireValueChange(property.name(), null, property.getValue(), "<refresh>");
	}

	/**
	 * Merges only the values from another Settings instance into this one,
	 * maintaining separation of action listeners. This method performs a one-way
	 * copy of values from the source settings to matching properties in this
	 * instance.
	 * 
	 * <p>
	 * Key characteristics of the merge:
	 * </p>
	 * <ul>
	 * <li>Only copies values for properties that already exist in this
	 * instance</li>
	 * <li>Only copies non-null values from the source</li>
	 * <li>Does not transfer action listeners from source to destination</li>
	 * <li>Triggers change notifications on this instance's existing listeners</li>
	 * </ul>
	 * 
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>
	 * Settings defaultSettings = new Settings("defaults");
	 * defaultSettings.newIntProperty("timeout", 1000);
	 * 
	 * Settings customSettings = new Settings("custom");
	 * customSettings.newIntProperty("timeout", 2000);
	 * customSettings.newStringProperty("name", "test"); // Won't be merged if not in defaults
	 * 
	 * defaultSettings.mergeValues(customSettings); // Only merges matching properties
	 * </pre>
	 *
	 * @param srcSettings the source Settings instance whose values should be merged
	 *                    into this one. Only values are copied, not action
	 *                    listeners.
	 */
	@SuppressWarnings("unchecked")
	public void mergeValues(Settings<?> srcSettings) {
		var ours = this.properties;

		for (@SuppressWarnings("rawtypes")
		Property our : ours) {
			var their = srcSettings.findProperty(our.name());
			if (their != null && their.isPresent()) {
				Object value = their.getValue();
				our.setValue(value, "<merge>");
			}
		}
	}

	/**
	 * Returns the name of this settings instance.
	 *
	 * @return the name of this settings instance
	 */
	public String name() {
		return name;
	}

	/**
	 * New boolean property.
	 *
	 * @param name the name
	 * @return the boolean property
	 */
	public BooleanProperty newBooleanProperty(String name) {
		return newProperty(name, BooleanProperty::new);
	}

	/**
	 * New boolean property.
	 *
	 * @param name         the name
	 * @param defaultValue the default value
	 * @return the boolean property
	 */
	public BooleanProperty newBooleanProperty(String name, boolean defaultValue) {
		return newProperty(name, defaultValue);
	}

	/**
	 * Creates a new byte property with the specified name.
	 *
	 * @param name the name of the property
	 * @return a new ByteProperty instance
	 */
	public ByteProperty newByteProperty(String name) {
		return newProperty(name, ByteProperty::new);
	}

	/**
	 * Creates a new byte property with the specified name and default value.
	 *
	 * @param name         the name of the property
	 * @param defaultValue the default value for the property
	 * @return a new ByteProperty instance
	 */
	public ByteProperty newByteProperty(String name, byte defaultValue) {
		return newProperty(name, defaultValue);
	}

	/**
	 * Creates a new double property with the specified name.
	 *
	 * @param name the name of the property
	 * @return a new DoubleProperty instance
	 */
	public DoubleProperty newDoubleProperty(String name) {
		return newProperty(name, DoubleProperty::new);
	}

	/**
	 * Creates a new double property with the specified name and default value.
	 *
	 * @param name         the name of the property
	 * @param defaultValue the default value for the property
	 * @return a new DoubleProperty instance
	 */
	public DoubleProperty newDoubleProperty(String name, double defaultValue) {
		return newProperty(name, defaultValue);
	}

	/**
	 * Creates a new enum property with the specified name and enum type.
	 *
	 * @param <E>      the enum type
	 * @param name     the name of the property
	 * @param enumType the Class object of the enum type
	 * @return a new EnumProperty instance
	 */
	public <E extends Enum<E>> EnumProperty<E> newEnumProperty(String name, Class<E> enumType) {
		return newProperty(name, (s, n) -> new EnumProperty<E>(s, n, enumType));
	}

	/**
	 * Creates a new enum property with the specified name and default value.
	 *
	 * @param <E>          the enum type
	 * @param name         the name of the property
	 * @param defaultValue the default enum value
	 * @return a new EnumProperty instance
	 */
	public <E extends Enum<E>> EnumProperty<E> newEnumProperty(String name, E defaultValue) {
		return newProperty(name, (s, n) -> new EnumProperty<E>(s, n, defaultValue));
	}

	/**
	 * Creates a new float property with the specified name.
	 *
	 * @param name the name of the property
	 * @return a new FloatProperty instance
	 */
	public FloatProperty newFloatProperty(String name) {
		return newProperty(name, FloatProperty::new);
	}

	/**
	 * Creates a new float property with the specified name and default value.
	 *
	 * @param name         the name of the property
	 * @param defaultValue the default value for the property
	 * @return a new FloatProperty instance
	 */
	public FloatProperty newFloatProperty(String name, float defaultValue) {
		return newProperty(name, defaultValue);
	}

	/**
	 * Creates a new integer property with the specified name.
	 *
	 * @param name the name of the property
	 * @return a new IntProperty instance
	 */
	public IntProperty newIntProperty(String name) {
		return newProperty(name, IntProperty::new);
	}

	/**
	 * Creates a new integer property with the specified name and default value.
	 *
	 * @param name         the name of the property
	 * @param defaultValue the default value for the property
	 * @return a new IntProperty instance
	 */
	public IntProperty newIntProperty(String name, int defaultValue) {
		return newProperty(name, defaultValue);
	}

	/**
	 * Creates a new list property with the specified name and parser function.
	 *
	 * @param <E>    the type of elements in the list
	 * @param name   the name of the property
	 * @param parser the function to parse string values into list elements
	 * @return a new ListProperty instance
	 */
	public <E> ListProperty<E> newListProperty(String name, Function<String, E> parser) {
		return newProperty(name, (s, n) -> new ListProperty<E>(s, n, parser));
	}

	/**
	 * Creates a new list property with the specified name, parser function, and
	 * variable number of default values.
	 *
	 * @param <E>          the type of elements in the list
	 * @param name         the name of the property
	 * @param parser       the function to parse string values into list elements
	 * @param defaultValue the default values as varargs
	 * @return a new ListProperty instance
	 */
	@SafeVarargs
	public final <E> ListProperty<E> newListProperty(String name, Function<String, E> parser, E... defaultValue) {
		return newProperty(name, (s, n) -> new ListProperty<E>(s, n, parser, Arrays.asList(defaultValue)));
	}

	/**
	 * Creates a new list property with the specified name, parser function, and
	 * default value.
	 *
	 * @param <E>          the type of elements in the list
	 * @param name         the name of the property
	 * @param parser       the function to parse string values into list elements
	 * @param defaultValue the default list value
	 * @return a new ListProperty instance
	 */
	public <E> ListProperty<E> newListProperty(String name, Function<String, E> parser, List<E> defaultValue) {
		return newProperty(name, (s, n) -> new ListProperty<E>(s, n, parser, defaultValue));
	}

	/**
	 * Creates a new list property with the specified name, parser function, default
	 * value, and action.
	 *
	 * @param <E>          the type of elements in the list
	 * @param name         the name of the property
	 * @param parser       the function to parse string values into list elements
	 * @param defaultValue the default list value
	 * @param action       the action to be performed when the property changes
	 * @return a new ListProperty instance
	 */
	public <E> ListProperty<E> newListProperty(String name, Function<String, E> parser, List<E> defaultValue,
			Action<List<E>> action) {
		return newProperty(name, (s, n) -> new ListProperty<E>(s, n, parser, defaultValue));
	}

	/**
	 * New object property.
	 *
	 * @param <E>          the element type
	 * @param name         the name
	 * @param deserializer the deserializer
	 * @param serializer   the serializer
	 * @return the object property
	 */
	public <E> ObjectProperty<E> newObjectProperty(String name, Deserializer<E> deserializer,
			Serializer<E> serializer) {
		return newProperty(name, (s, n) -> new ObjectProperty<E>(s, n, deserializer, serializer));
	}

	/**
	 * New object property.
	 *
	 * @param <E>          the element type
	 * @param name         the name
	 * @param deserializer the deserializer
	 * @param serializer   the serializer
	 * @param value        the value
	 * @return the object property
	 */
	public <E> ObjectProperty<E> newObjectProperty(String name, Deserializer<E> deserializer,
			Serializer<E> serializer, E value) {
		return newProperty(name, (s, n) -> new ObjectProperty<E>(s, n, deserializer, serializer, value));
	}

	/**
	 * New array property.
	 *
	 * @param <E>          the element type
	 * @param name         the name
	 * @param deserializer the deserializer
	 * @param serializer   the serializer
	 * @param arrayFactory the array factory
	 * @return the array property
	 */
	public <E> ArrayProperty<E> newArrayProperty(String name, Deserializer<E> deserializer,
			Serializer<E> serializer, IntFunction<E[]> arrayFactory) {
		return newProperty(name, (s, n) -> new ArrayProperty<E>(s, n, deserializer, serializer, arrayFactory));
	}

	/**
	 * New array property.
	 *
	 * @param <E>          the element type
	 * @param name         the name
	 * @param deserializer the deserializer
	 * @param serializer   the serializer
	 * @param arrayFactory the array factory
	 * @param value        the value
	 * @return the array property
	 */
	@SuppressWarnings("unchecked")
	public <E> ArrayProperty<E> newArrayProperty(String name, Deserializer<E> deserializer,
			Serializer<E> serializer, IntFunction<E[]> arrayFactory, E... value) {
		return newProperty(name, (s, n) -> new ArrayProperty<E>(s, n, deserializer, serializer, arrayFactory, value));
	}

	/**
	 * Creates a new long property with the specified name.
	 *
	 * @param name the name of the property
	 * @return a new LongProperty instance
	 */
	public LongProperty newLongProperty(String name) {
		return newProperty(name, LongProperty::new);
	}

	/**
	 * Creates a new long property with the specified name and default value.
	 *
	 * @param name         the name of the property
	 * @param defaultValue the default value for the property
	 * @return a new LongProperty instance
	 */
	public LongProperty newLongProperty(String name, long defaultValue) {
		return newProperty(name, defaultValue);
	}

	/**
	 * Creates or retrieves a property with the specified name using a factory
	 * function.
	 *
	 * @param <T>      the type of the property value
	 * @param <P_BASE> the type of the property
	 * @param name     the name of the property
	 * @param factory  the factory function to create the property if it doesn't
	 *                 exist
	 * @return the existing or newly created property
	 */
	public <T, P_BASE extends Property<T, P_BASE>> P_BASE newProperty(String name, PropertyFactory<P_BASE> factory) {
		name = createPropertyName(name);

		P_BASE p = findProperty(name);
		if (p != null)
			return p;

		P_BASE property = Property.of(settingsSupport, name, factory);
		properties.add(property);
		return property;
	}

	/**
	 * Creates or retrieves a property with the specified name and default value.
	 *
	 * @param <T>          the type of the property value
	 * @param <P_BASE>     the type of the property
	 * @param name         the name of the property
	 * @param defaultValue the default value for the property
	 * @return the existing or newly created property
	 */
	public <T, P_BASE extends Property<T, P_BASE>> P_BASE newProperty(String name, T defaultValue) {
		name = createPropertyName(name);

		P_BASE p = findProperty(name);
		if (p != null)
			return p;

		P_BASE property = Property.of(settingsSupport, name, defaultValue);
		properties.add(property);
		return property;
	}

	/**
	 * Creates or retrieves a property with the specified name, default value, and
	 * factory.
	 *
	 * @param <T>          the type of the property value
	 * @param <P_BASE>     the type of the property
	 * @param name         the name of the property
	 * @param defaultValue the default value for the property
	 * @param factory      the factory function to create the property
	 * @return the existing or newly created property
	 */
	public <T, P_BASE extends Property<T, P_BASE>> P_BASE newProperty(String name,
			T defaultValue,
			PropertyFactoryWithValue<T, P_BASE> factory) {
		name = createPropertyName(name);

		P_BASE p = findProperty(name);
		if (p != null)
			return p;

		P_BASE property = Property.of(settingsSupport, name, defaultValue, factory);
		properties.add(property);
		return property;
	}

	/**
	 * Creates a new short property with the specified name.
	 *
	 * @param name the name of the property
	 * @return a new ShortProperty instance
	 */
	public ShortProperty newShortProperty(String name) {
		return newProperty(name, ShortProperty::new);
	}

	/**
	 * Creates a new short property with the specified name and default value.
	 *
	 * @param name         the name of the property
	 * @param defaultValue the default value for the property
	 * @return a new ShortProperty instance
	 */
	public ShortProperty newShortProperty(String name, short defaultValue) {
		return newProperty(name, defaultValue);
	}

	/**
	 * Creates a new string property with the specified name.
	 *
	 * @param name the name of the property
	 * @return a new StringProperty instance
	 */
	public StringProperty newStringProperty(String name) {
		return newProperty(name, StringProperty::new);
	}

	/**
	 * Creates a new string property with the specified name and default value.
	 *
	 * @param name         the name of the property
	 * @param defaultValue the default value for the property
	 * @return a new StringProperty instance
	 */
	public StringProperty newStringProperty(String name, String defaultValue) {
		return newProperty(name, defaultValue);
	}

	/**
	 * Registers an action to be executed when this settings instance is cleared.
	 * The action will be triggered by the {@link #clear()} method if notifications
	 * are enabled.
	 *
	 * @param action the action to execute when settings are cleared
	 * @return this settings instance for method chaining
	 * @see #clear()
	 */
	public T_BASE onClear(CleanSettingsNotification<T_BASE> action) {
		registerClearAction(action);
		return us;
	}

	/**
	 * Registers an action to be executed when this settings instance is cleared,
	 * providing registration tracking through a consumer.
	 *
	 * @param action       the action to execute when settings are cleared
	 * @param registration consumer to receive the registration object for later
	 *                     unregistration/cleanup
	 * @return this settings instance for method chaining
	 * @see #clear()
	 */
	public T_BASE onClear(CleanSettingsNotification<T_BASE> action, Consumer<Registration> registration) {
		var reg = registerClearAction(action);
		registration.accept(reg);
		return us;
	}

	/**
	 * Registers an action to be executed when this settings instance is reset. The
	 * action will be triggered by the {@link #reset()} method if notifications are
	 * enabled.
	 *
	 * @param action the action to execute when settings are reset
	 * @return this settings instance for method chaining
	 * @see #reset()
	 */
	public T_BASE onReset(ResetSettingsNotification<T_BASE> action) {
		registerResetAction(action);
		return us;
	}

	/**
	 * Registers an action to be executed when this settings instance is reset,
	 * providing registration tracking through a consumer.
	 *
	 * @param action       the action to execute when settings are reset
	 * @param registration consumer to receive the registration object for later
	 *                     unregistration/cleanup
	 * @return this settings instance for method chaining
	 * @see #reset()
	 */
	public T_BASE onReset(ResetSettingsNotification<T_BASE> action, Consumer<Registration> registration) {
		var reg = registerResetAction(action);
		registration.accept(reg);
		return us;
	}

	/**
	 * Registers a clear action and returns a Registration object that can be used
	 * to unregister the action later.
	 *
	 * @param action the action to register for clear notifications
	 * @return a Registration object that can be used to unregister the action
	 */
	public Registration registerClearAction(CleanSettingsNotification<T_BASE> action) {
		clearActions.add(action);
		return () -> clearActions.remove(action);
	}

	/**
	 * Registers a reset action and returns a Registration object that can be used
	 * to unregister the action later.
	 *
	 * @param action the action to register for reset notifications
	 * @return a Registration object that can be used to unregister the action
	 */
	public Registration registerResetAction(ResetSettingsNotification<T_BASE> action) {
		resetActions.add(action);
		return () -> resetActions.remove(action);
	}

	/**
	 * Resets all properties in this settings instance to their original default
	 * values. Properties that have no default value will be cleared. This operation
	 * triggers any registered reset notifications if property change notifications
	 * are enabled.
	 * 
	 * @return this settings instance for method chaining
	 * @see #onReset(ResetSettingsNotification)
	 */
	public T_BASE reset() {
		for (var p : properties) {
			p.reset();
		}

		if (settingsSupport.enableFireEvents)
			resetActions.forEach(a -> a.onResetSettings(us));

		return us;
	}

	/**
	 * Returns a string representation of this Settings instance, including its name
	 * and a subset of its properties (limited to 10 for readability).
	 *
	 * @return a string representation of this Settings instance
	 */
	@Override
	public String toString() {
		final int maxLen = 10;
		return name()
				+ " [properties=" + (properties != null ? properties.subList(0, Math.min(properties.size(),
						maxLen)) : null) + "]";
	}

	/**
	 * Update all property listeners by firing change notifications for all
	 * non-empty properties. This can be used to reinitialize listeners after
	 * enabling updates. Only fires events if updates are enabled.
	 */
	public synchronized void updateAllActions() {
		if (settingsSupport.enableFireEvents == false)
			return;

		properties.stream()
				.filter(Property::isPresent)
				.forEach(this::fireForProperty);
	}
}
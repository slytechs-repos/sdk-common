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

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.slytechs.jnet.core.api.util.Registration;

/**
 * A sealed abstract base class for type-safe property handling within the
 * settings framework. This class provides a comprehensive foundation for
 * managing configuration properties with support for value validation, change
 * notifications, optional handling, and system property integration.
 * 
 * <p>
 * The Property class is sealed and can only be extended by specific property
 * types defined in the permits clause. Each property type provides type-safe
 * operations for its specific data type while inheriting common functionality
 * from this base class.
 * </p>
 * 
 * <p>
 * Key features include:
 * </p>
 * <ul>
 * <li>Type-safe value handling with generic type parameters</li>
 * <li>Optional value support with null safety</li>
 * <li>Change notification through action listeners</li>
 * <li>System property integration</li>
 * <li>Value formatting and string conversion</li>
 * <li>Functional programming support with map/filter operations</li>
 * </ul>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * IntProperty port = Property.of("server.port", 8080, IntProperty::new)
 * 		.on((newValue, source) -> System.out.println("Port changed to: " + newValue))
 * 		.setFormat("Port: %d");
 * 
 * port.setValue(9090);
 * port.systemProperty(); // Load from system properties if available
 * 
 * OptionalInt optPort = port.toIntOptional();
 * </pre>
 *
 * @param <T>      the type of value stored in this property
 * @param <T_BASE> the specific property type extending this class
 * 
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc
 */
public sealed abstract class Property<T, T_BASE extends Property<T, T_BASE>>
		permits EmptyProperty,
		ByteProperty,
		ShortProperty,
		IntProperty,
		LongProperty,
		FloatProperty,
		DoubleProperty,
		StringProperty,
		EnumProperty,
		BooleanProperty,
		UnsignedByteProperty,
		UnsignedShortProperty,
		UnsignedIntProperty,
		ListProperty,
		ArrayProperty,
		ObjectProperty {

	/**
	 * The Interface Serializer.
	 *
	 * @param <T> the generic type
	 */
	public interface Serializer<T> {
		
		/**
		 * Serialize object.
		 *
		 * @param object the object
		 * @return the string
		 */
		String serializeObject(T object);
	}

	/**
	 * The Interface Deserializer.
	 *
	 * @param <T> the generic type
	 */
	public interface Deserializer<T> {
		
		/**
		 * Deserialize object.
		 *
		 * @param serializedObject the serialized object
		 * @return the t
		 */
		T deserializeObject(String serializedObject);
	}

	/**
	 * Interface defining actions that can be performed when property values change.
	 * Actions can be chained and can filter based on the source of the change.
	 *
	 * @param <T> the type of value handled by this action
	 */
	public interface Action<T> {

		/**
		 * Creates a standard Action from a FluentAction by adapting the fluent
		 * interface to the standard action interface.
		 *
		 * @param <T>      the type of value handled by this action
		 * @param <T_BASE> the base type used for method chaining in the fluent action
		 * @param action   the fluent action to adapt
		 * @return a standard Action that delegates to the fluent action
		 */
		static <T, T_BASE> Action<T> ofFluent(FluentAction<T, T_BASE> action) {
			return action.asAction();
		}

		/**
		 * A specialized action interface that supports fluent method chaining when
		 * handling property changes. This interface allows actions to return a base
		 * type for method chaining while still being compatible with the standard
		 * Action interface.
		 *
		 * @param <T>      the type of value handled by this action
		 * @param <T_BASE> the base type returned for method chaining
		 */
		interface FluentAction<T, T_BASE> {

			/**
			 * Creates a FluentAction from another FluentAction, effectively acting as an
			 * identity function for fluent actions.
			 *
			 * @param <T>      the type of value handled by this action
			 * @param <T_BASE> the base type used for method chaining
			 * @param action   the fluent action to return
			 * @return the provided fluent action
			 */
			static <T, T_BASE> FluentAction<T, T_BASE> of(FluentAction<T, T_BASE> action) {
				return action;
			}

			/**
			 * Performs the action when a property value changes, supporting method
			 * chaining.
			 *
			 * @param newValue the new value of the property
			 * @return the base instance for method chaining
			 */
			T_BASE propertyChangeFluentAction(T newValue);

			/**
			 * Converts this FluentAction to a standard Action by wrapping the fluent method
			 * call. This allows FluentActions to be used anywhere a standard Action is
			 * expected.
			 *
			 * @return an Action that delegates to this FluentAction's implementation
			 */
			default Action<T> asAction() {
				return FluentAction.this::propertyChangeFluentAction;
			}

		}

		/**
		 * Wraps an action to enable method chaining with {@code andThen()} when using
		 * method references or lambda expressions. This method exists purely for
		 * syntactical convenience, as method references cannot directly use
		 * {@code andThen()} without being wrapped first.
		 * 
		 * <p>
		 * Example usage with method reference:
		 * </p>
		 * 
		 * <pre>
		 * // Without wrapper - cannot chain directly on method reference
		 * property.on(this::handleChange); // Can't do this::handleChange.andThen(...)
		 * 
		 * // With wrapper - enables chaining
		 * property.on(Action.of(this::handleChange)
		 * 		.andThen(otherAction));
		 * </pre>
		 *
		 * @param <T>    the type of value handled by this action
		 * @param action the action to wrap
		 * @return the same action, wrapped to enable method chaining
		 */
		static <T> Property.Action<T> of(Action<T> action) {
			return action;
		}

		/**
		 * Creates an Action that executes the given BiConsumer when property values
		 * change, with optional source filtering.
		 *
		 * @param <T>    the type of value handled by this action
		 * @param action the action to execute on property change
		 * @param source the source object to filter changes (may be null for no
		 *               filtering)
		 * @return a new Action instance
		 */
		static <T> Property.Action<T> withSource(BiConsumer<T, Object> action, Object source) {
			Objects.requireNonNull(action);

			return new Property.Action<T>() {
				@Override
				public boolean canFireFromSource(Object src) {
					return source == null || source != src;
				}

				@Override
				public void propertyChangeAction(String name, T oldValue, T newValue, Object src) {
					try {
						action.accept(newValue, src);
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}

				@Override
				public void propertyChangeAction(T newValue) {
					throw new UnsupportedOperationException();
				}
			};
		}

		/**
		 * Chains this action with another action to be executed after this one.
		 *
		 * @param afterAction the action to execute after this one
		 * @return a new Action that executes both actions in sequence
		 */
		default Property.Action<T> andThen(Property.Action<T> afterAction) {
			Property.Action<T> before = this;
			return new Property.Action<T>() {
				@Override
				public boolean canFireFromSource(Object source) {
					return before.canFireFromSource(source);
				}

				@Override
				public void propertyChangeAction(String name, T oldValue, T newValue, Object src) {

					try {
						before.propertyChangeAction(name, oldValue, newValue, src);
						afterAction.propertyChangeAction(name, oldValue, newValue, src);
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}

				@Override
				public void propertyChangeAction(T newValue) {
					throw new UnsupportedOperationException();
				}
			};
		}

		/**
		 * Determines if the action should be executed for changes from the given
		 * source.
		 *
		 * @param source the source of the property change
		 * @return true if the action should be executed, false otherwise
		 */
		default boolean canFireFromSource(Object source) {
			return true;
		}

		/**
		 * Called when a property value changes, with just the property name and new
		 * value.
		 *
		 * @param name     the name of the property that changed
		 * @param newValue the new value of the property
		 */
		default void propertyChangeAction(String name, T newValue) {
			propertyChangeAction(newValue);
		}

		/**
		 * Called when a property value changes, with the property name, old value, and
		 * new value.
		 *
		 * @param name     the name of the property that changed
		 * @param oldValue the previous value of the property
		 * @param newValue the new value of the property
		 */
		default void propertyChangeAction(String name, T oldValue, T newValue) {
			propertyChangeAction(name, newValue);
		}

		/**
		 * Called when a property value changes, with complete change information.
		 *
		 * @param name     the name of the property that changed
		 * @param oldValue the previous value of the property
		 * @param newValue the new value of the property
		 * @param source   the source of the change
		 */
		default void propertyChangeAction(String name, T oldValue, T newValue, Object source) {
			propertyChangeAction(name, oldValue, newValue);
		}

		/**
		 * The base property change action method that must be implemented.
		 *
		 * @param newValue the new value of the property
		 */
		void propertyChangeAction(T newValue);
	}

	/**
	 * Interface defining actions that are triggered when a property is cleared. The
	 * action receives the property name and its previous value before clearing.
	 * This interface is specifically used with the Property.onClear() listener
	 * registration method.
	 *
	 * @param <T> the type of the value being cleared from the property
	 * @see Property#onClear(ClearAction)
	 */
	interface ClearAction<T> {
		/**
		 * Called when a property is cleared.
		 *
		 * @param name     the name of the property that was cleared
		 * @param oldValue the value that was cleared from the property
		 */
		void onPropertyClear(String name, T oldValue);
	}

	/**
	 * A factory for creating Property objects.
	 *
	 * @param <T_BASE> the generic type
	 */
	public interface PropertyFactory<T_BASE extends Property<?, T_BASE>> {
		
		/**
		 * New instance.
		 *
		 * @param support the support
		 * @param name    the name
		 * @return the t base
		 */
		T_BASE newInstance(SettingsSupport support, String name);
	}

	/**
	 * The Interface PropertyFactoryWithValue.
	 *
	 * @param <T>      the generic type
	 * @param <T_BASE> the generic type
	 */
	public interface PropertyFactoryWithValue<T, T_BASE extends Property<T, T_BASE>> {
		
		/**
		 * New instance.
		 *
		 * @param support the support
		 * @param name    the name
		 * @param value   the value
		 * @return the t base
		 */
		T_BASE newInstance(SettingsSupport support, String name, T value);
	}

	/**
	 * Interface defining actions that are triggered when a property is cleared. The
	 * action receives the property name and its previous value before clearing.
	 * This interface is specifically used with the Property.onClear() listener
	 * registration method.
	 *
	 * @param <T> the type of the value being cleared from the property
	 * @see Property#onReset(ResetAction)
	 */
	interface ResetAction<T> {
		
		/**
		 * Called when a property is reset.
		 *
		 * @param name     the name of the property that was cleared
		 * @param oldValue the value that was cleared from the property
		 * @param newValue the new value
		 */
		void onPropertyReset(String name, T oldValue, T newValue);
	}

	/**
	 * Creates an empty property with the specified name.
	 *
	 * @param <U>      the type of value the property would hold
	 * @param <P_BASE> the specific property type
	 * @param name     the name for the empty property
	 * @return a new empty property instance
	 */
	public static <U, P_BASE extends Property<U, P_BASE>> P_BASE empty(String name) {
		@SuppressWarnings("unchecked")
		P_BASE empty = (P_BASE) new EmptyProperty<>(name);
		return empty;
	}

	/**
	 * Creates a property using the provided factory function.
	 *
	 * @param <T>      the type of value the property will hold
	 * @param <T_BASE> the specific property type
	 * @param support  the support
	 * @param name     the name for the property
	 * @param factory  the factory function to create the property
	 * @return a new property instance
	 */
	public static <T, T_BASE extends Property<T, T_BASE>> T_BASE of(SettingsSupport support, String name,
			PropertyFactory<T_BASE> factory) {
		return factory.newInstance(support, name);
	}

	/**
	 * Creates a property with a specified name and value, automatically determining
	 * the appropriate property type based on the value's class.
	 *
	 * @param <T>      the type of value the property will hold
	 * @param <T_BASE> the specific property type
	 * @param support  the support
	 * @param name     the name for the property
	 * @param value    the initial value for the property
	 * @return a new property instance of the appropriate type
	 * @throws IllegalArgumentException if the value type is not supported
	 */
	public static <T, T_BASE extends Property<T, T_BASE>> T_BASE of(SettingsSupport support, String name, T value) {
		@SuppressWarnings("unchecked")
		T_BASE property = (T_BASE) switch (value) {
		case null -> new EmptyProperty<>(name);
		case Byte bvalue -> new ByteProperty(name, bvalue);
		case Short svalue -> new ShortProperty(name, svalue);
		case Integer ivalue -> new IntProperty(name, ivalue);
		case Long lvalue -> new LongProperty(name, lvalue);
		case Float fvalue -> new FloatProperty(name, fvalue);
		case Double dvalue -> new DoubleProperty(name, dvalue);
		case String svalue -> new StringProperty(name, svalue);
		case Boolean bvalue -> new BooleanProperty(name, bvalue);
		default -> throw new IllegalArgumentException("unknown property type for value of " + value);
		};

		return property;
	}

	/**
	 * Creates a property using a factory function that takes both name and initial
	 * value.
	 *
	 * @param <T>      the type of value the property will hold
	 * @param <T_BASE> the specific property type
	 * @param support  the support
	 * @param name     the name for the property
	 * @param value    the initial value for the property
	 * @param factory  the factory function to create the property
	 * @return a new property instance
	 */
	public static <T, T_BASE extends Property<T, T_BASE>> T_BASE of(SettingsSupport support, String name, T value,
			PropertyFactoryWithValue<T, T_BASE> factory) {
		return factory.newInstance(support, name, value);
	}

	/** Support class for managing property change notifications. */
	private final SettingsSupport settingsSupport;

	/** Reference to this property instance cast to its specific type. */
	@SuppressWarnings("unchecked")
	private final T_BASE us = (T_BASE) this;

	/** The current value of the property. */
	private T value;
	
	/** The initial value. */
	private final T initialValue;

	/** Format string for value representation. */
	private String formatString = "%s";

	/** The name of the property. */
	private final String name;

	/** The clear actions. */
	private final List<ClearAction<T>> clearActions = new ArrayList<>(1);
	
	/** The reset actions. */
	private final List<ResetAction<T>> resetActions = new ArrayList<>(1);

	/** The serializer. */
	private Serializer<T> serializer = String::valueOf;
	
	/** The deserializer. */
	private Deserializer<T> deserializer;

	/**
	 * Creates a new Property with the specified name and no initial value.
	 *
	 * @param settingsSupport support class for managing property change
	 *                        notifications
	 * @param name            the name of the property (must not be null)
	 * @throws NullPointerException if name is null
	 */
	protected Property(SettingsSupport settingsSupport, String name) {
		this.settingsSupport = Objects.requireNonNull(settingsSupport);
		this.name = Objects.requireNonNull(name);
		this.initialValue = null;
	}

	/**
	 * Creates a new Property with the specified name and initial value.
	 *
	 * @param settingsSupport support class for managing property change
	 *                        notifications
	 * @param name            the name of the property (must not be null)
	 * @param value           the initial value for the property
	 * @throws NullPointerException if name is null
	 */
	protected Property(SettingsSupport settingsSupport, String name, T value) {
		this.settingsSupport = Objects.requireNonNull(settingsSupport);
		this.name = Objects.requireNonNull(name);
		this.value = value;
		this.initialValue = value;
	}

	/**
	 * Creates a new Property with the specified name and no initial value.
	 *
	 * @param name the name of the property (must not be null)
	 * @throws NullPointerException if name is null
	 */
	protected Property(String name) {
		this.settingsSupport = new SettingsSupport();
		this.name = Objects.requireNonNull(name);
		this.initialValue = null;
	}

	/**
	 * Creates a new Property with the specified name and initial value.
	 *
	 * @param name  the name of the property (must not be null)
	 * @param value the initial value for the property
	 * @throws NullPointerException if name is null
	 */
	protected Property(String name, T value) {
		this.settingsSupport = new SettingsSupport();
		this.name = Objects.requireNonNull(name);
		this.value = value;
		this.initialValue = value;
	}

	/**
	 * Sets the serializer.
	 *
	 * @param newSerializer the new serializer
	 */
	protected void setSerializer(Serializer<T> newSerializer) {
		this.serializer = newSerializer;
	}

	/**
	 * Sets the deserializer.
	 *
	 * @param newDeserializer the new deserializer
	 */
	protected void setDeserializer(Deserializer<T> newDeserializer) {
		this.deserializer = newDeserializer;
	}

	/**
	 * Gets the serializer.
	 *
	 * @return the serializer
	 */
	protected Serializer<T> getSerializer() {
		return serializer;
	}

	/**
	 * No deserializer exception.
	 *
	 * @return the illegal state exception
	 */
	protected IllegalStateException noDeserializerException() {
		return new IllegalStateException("property deserializer is not set [%s]".formatted(name()));
	}

	/**
	 * Gets the deserializer.
	 *
	 * @return the deserializer
	 */
	protected Deserializer<T> getDeserializer() {
		if (deserializer == null)
			throw noDeserializerException();

		return deserializer;
	}

	/**
	 * Validates that a value is within acceptable bounds for this property type.
	 * Base implementation performs no validation. Subclasses should override this
	 * method to implement type-specific validation.
	 *
	 * @param value the value to validate
	 * @throws IllegalArgumentException if the value is outside acceptable bounds
	 */
	protected void checkBounds(T value) throws IllegalArgumentException {
		// No bounds check by default, override to implement a check
	}

	/**
	 * Clears the current value of the property, setting it to null. Fires a
	 * property change event if the property had a value.
	 *
	 * @return this property instance for method chaining
	 */
	public T_BASE clear() {

		T oldValue = this.value;

		this.value = null;

		if (oldValue != null)
			clearActions.forEach(a -> a.onPropertyClear(name, oldValue));

		return us;
	}

	/**
	 * Gets the current format string used for value representation.
	 *
	 * @return the current format string
	 */
	public String getFormat() {
		return formatString;
	}

	/**
	 * Attempts to load a value from system properties, using the provided default
	 * if no system property is found.
	 *
	 * @param defaultValue the default value to use if no system property exists
	 * @return this property instance for method chaining
	 */
	public T_BASE getSystemProperty(T defaultValue) {
		String newValue = System.getProperty(name());
		if (newValue != null)
			return deserializeValue(newValue);
		else if (isEmpty())
			return setValue(defaultValue);

		return us;
	}

	/**
	 * Gets the current value of the property.
	 *
	 * @return the current value
	 */
	public T getValue() {
		if (isEmpty())
			throw noValueException();

		return value;
	}

	/**
	 * Executes the provided action if the property has no value.
	 *
	 * @param action the action to execute if the property is empty
	 * @return this property instance for method chaining
	 */
	public T_BASE ifEmpty(Runnable action) {
		if (value == null)
			action.run();

		return us;
	}

	/**
	 * Executes the provided action if the property has a value.
	 *
	 * @param action the action to execute with the current value
	 */
	public void ifPresent(Consumer<? super T> action) {
		if (value != null)
			action.accept(value);
	}

	/**
	 * Checks if the property has no value.
	 *
	 * @return true if the property has no value, false otherwise
	 */
	public boolean isEmpty() {
		return value == null;
	}

	/**
	 * Checks if the property has a value.
	 *
	 * @return true if the property has a value, false otherwise
	 */
	public boolean isPresent() {
		return value != null;
	}

	/**
	 * Attempts to load this property's value from system properties. This method
	 * checks if a system property exists with the same name as this property and if
	 * found, parses and sets its value as this property's value. If the system
	 * property exists, it will override any current value of this property.
	 * 
	 * <p>
	 * The system property can be set using the JVM argument format:
	 * {@code -Dproperty.name=value}
	 * </p>
	 * 
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>
	 * IntProperty port = new IntProperty("server.port", 8080);
	 * port.loadSystemProperty(); // Will load value from -Dserver.port if defined
	 * </pre>
	 *
	 * @return this property instance for method chaining
	 * @see System#getProperty(String)
	 */
	public T_BASE loadSystemProperty() {
		String newValue = System.getProperty(name());
		if (newValue != null)
			return deserializeValue(newValue);
		return us;
	}

	/**
	 * Maps this property's value to a new property of a different type using the
	 * provided transformation function.
	 *
	 * @param <U>      the type of the new property value
	 * @param <P_BASE> the type of the new property
	 * @param action   the transformation function to apply to the value
	 * @return a new property containing the transformed value, or an empty property
	 *         if this property is empty
	 */
	public <U, P_BASE extends Property<U, P_BASE>> P_BASE map(Function<? super T, ? extends U> action) {
		if (isPresent()) {
			U mappedValue = action.apply(value);
			return Property.of(this.settingsSupport, name(), mappedValue);
		}
		return empty(name());
	}

	/**
	 * Creates a new StringProperty containing this property's value formatted
	 * according to the current format string.
	 *
	 * @return a new StringProperty containing the formatted value
	 */
	public StringProperty mapToFormattedString() {
		return new StringProperty(name(), toFormattedValue());
	}

	/**
	 * Returns the name of this property.
	 *
	 * @return the property name
	 */
	public final String name() {
		return this.name;
	}

	/**
	 * Creates a standard exception for when a value is requested but none exists.
	 *
	 * @return an IllegalStateException with appropriate message
	 */
	protected final IllegalStateException noValueException() {
		return new IllegalStateException("no value");
	}

	/**
	 * Registers a simple action to be executed when this property's value changes,
	 * with optional source filtering. This is a convenience method that creates an
	 * Action from the provided BiConsumer.
	 * 
	 * <p>
	 * The BiConsumer receives both the new value and the source of the change. If a
	 * source object is provided, the action will only be executed for changes that
	 * come from a different source.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>
	 * property.on((newValue, source) -> System.out.println("Value changed to: " + newValue),
	 * 		this); // Won't fire for changes from 'this'
	 * </pre>
	 *
	 * @param before the action to execute, receiving the new value and change
	 *               source
	 * @param source optional source object for filtering changes, or null for no
	 *               filtering
	 * @return this property instance for method chaining
	 */
	public T_BASE on(BiConsumer<T, Object> before, Object source) {
		return on(Action.withSource(before, source));
	}

	/**
	 * On.
	 *
	 * @param <R>    the generic type
	 * @param before the before
	 * @param source the source
	 * @return the t base
	 */
	public <R> T_BASE on(Function<T, R> before, Object source) {
		return on((T t) -> before.apply(t));
	}

	/**
	 * Registers an action to be executed when this property's value changes.
	 *
	 * @param action the action to execute on value changes
	 * @return this property instance for method chaining
	 */
	public T_BASE on(Property.Action<T> action) {
		settingsSupport.addAction(name(), action);
		return us;
	}

	/**
	 * Registers an action to be executed when this property's value changes and
	 * provides registration tracking through a consumer.
	 *
	 * @param action       the action to execute on value changes
	 * @param registration consumer to receive the registration object
	 * @return this property instance for method chaining
	 */
	public T_BASE on(Property.Action<T> action, Consumer<Registration> registration) {

		var reg = settingsSupport.addAction(name(), action);
		registration.accept(reg);

		return us;
	}

	/**
	 * Registers an action to be executed when this property is cleared. This
	 * notification is triggered before the property value is cleared.
	 *
	 * @param action the action to execute when property is cleared
	 * @return this property instance for method chaining
	 */
	public T_BASE onClear(Property.ClearAction<T> action) {
		clearActions.add(action);
		return us;
	}

	/**
	 * Registers an action to be executed when this property is reset to its default
	 * value. This notification is triggered before the property is reset.
	 *
	 * @param action the action to execute when property is reset
	 * @return this property instance for method chaining
	 */
	public T_BASE onReset(Property.ResetAction<T> action) {
		resetActions.add(action);
		return us;
	}

	/**
	 * Returns an alternative property if this one is empty.
	 *
	 * @param supplier the supplier of the alternative property
	 * @return this property if it has a value, otherwise the supplied alternative
	 * @throws NullPointerException if the supplier or its result is null
	 */
	public T_BASE or(Supplier<? extends T_BASE> supplier) {
		Objects.requireNonNull(supplier);
		if (isPresent()) {
			return us;
		} else {
			T_BASE r = supplier.get();
			return Objects.requireNonNull(r);
		}
	}

	/**
	 * Returns the value if present, otherwise returns the alternative value.
	 *
	 * @param altValue the value to return if this property is empty
	 * @return the value of this property if present, otherwise altValue
	 */
	public T orElse(T altValue) {
		return altValue;
	}

	/**
	 * Executes an action if this property is empty and returns the property.
	 *
	 * @param action the action to execute if the property is empty
	 * @return this property instance
	 */
	public T_BASE orElseAction(Runnable action) {
		if (value == null)
			action.run();
		return us;
	}

	/**
	 * Returns the value if present, otherwise returns the result of the supplier.
	 *
	 * @param supplier the supplier of the alternative value
	 * @return the value if present, otherwise the result of the supplier
	 */
	public T orElseGet(Supplier<? extends T> supplier) {
		return value != null ? value : supplier.get();
	}

	/**
	 * Returns the value if present, otherwise throws NoSuchElementException.
	 *
	 * @return the non-null value
	 * @throws NoSuchElementException if no value is present
	 */
	public T orElseThrow() {
		if (value == null) {
			throw new NoSuchElementException("No value present");
		}
		return value;
	}

	/**
	 * Returns the value if present, otherwise throws an exception produced by the
	 * supplier.
	 *
	 * @param <X>               type of exception to throw
	 * @param exceptionSupplier supplier of the exception to throw
	 * @return the value if present
	 * @throws X                    if no value is present
	 * @throws NullPointerException if no value is present and the exception
	 *                              supplier is null
	 */
	public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
		if (value != null) {
			return value;
		} else {
			throw exceptionSupplier.get();
		}
	}

	/**
	 * Parse/deserializes a string value and sets the property's value accordingly.
	 * Must be implemented by concrete subclasses to provide type-specific parsing.
	 *
	 * @param newValue the string value to parse
	 * @return this property instance for method chaining
	 */
	public T_BASE deserializeValue(String newValue) {
		T deserializedValue = getDeserializer().deserializeObject(newValue);

		setValue(deserializedValue);

		return us;
	}

	/**
	 * Serialize value.
	 *
	 * @return the string
	 */
	public String serializeValue() {
		if (isEmpty())
			throw noValueException();

		return getSerializer().serializeObject(getValue());
	}

	/**
	 * Registers a clear action and returns a Registration object that can be used
	 * to unregister the action later.
	 * 
	 * @param action the action to register for clear notifications
	 * @return a Registration object that can be used to remove this action
	 */
	public Registration registerClearAction(Property.ClearAction<T> action) {
		clearActions.add(action);
		return () -> clearActions.remove(action);
	}

	/**
	 * Registers a reset action and returns a Registration object that can be used
	 * to unregister the action later.
	 *
	 * @param action the action to register for reset notifications
	 * @return a Registration object that can be used to remove this action
	 */
	public Registration registerResetAction(Property.ResetAction<T> action) {
		resetActions.add(action);
		return () -> resetActions.remove(action);
	}

	/**
	 * Resets the current value of the property, setting it to its initial value, if
	 * it was set. Fires a property change event if the property had a value.
	 *
	 * @return this property instance for method chaining
	 */
	public T_BASE reset() {
		T oldValue = this.value;
		this.value = initialValue;

		if (oldValue != initialValue)
			resetActions.forEach(a -> a.onPropertyReset(name, oldValue, initialValue));

		return us;
	}

	/**
	 * Sets the format string used for value representation.
	 *
	 * @param formatString the new format string to use
	 * @return this property instance for method chaining
	 */
	public T_BASE setFormat(String formatString) {
		this.formatString = formatString;
		return us;
	}

	/**
	 * Sets the value of this property.
	 *
	 * @param newValue the new value to set
	 * @return this property instance for method chaining
	 */
	public T_BASE setValue(T newValue) {
		return setValue(newValue, null);
	}

	/**
	 * Sets the value of this property with a source identifier.
	 *
	 * @param newValue the new value to set
	 * @param source   the source of the value change
	 * @return this property instance for method chaining
	 */
	public T_BASE setValue(T newValue, Object source) {
		if (this.value == newValue)
			return us;

		if (newValue != null)
			checkBounds(newValue);

		this.value = newValue;
		settingsSupport.fireValueChange(name(), this.value, newValue, source);
		return us;
	}

	/**
	 * Returns a stream containing the property value if present, or an empty stream
	 * if the property is empty.
	 *
	 * @return a stream containing zero or one elements
	 */
	public Stream<T> stream() {
		if (isEmpty())
			return Stream.empty();
		else
			return Stream.of(value);
	}

	/**
	 * Returns the property value formatted according to the current format string.
	 *
	 * @return the formatted value string, or "<empty>" if the property is empty
	 */
	public String toFormattedValue() {
		if (isEmpty())
			return "<empty>";
		return formatString.formatted(value);
	}

	/**
	 * Converts this property to an Optional containing its value if present.
	 *
	 * @return an Optional describing the value of this property
	 */
	public Optional<T> toOptional() {
		return Optional.ofNullable(value);
	}

	/**
	 * Returns a string representation of this property.
	 *
	 * @return a string representation including the property name and value
	 */
	@Override
	public String toString() {
		if (isEmpty())
			return name + " = " + "<empty>";
		return name + " = " + String.valueOf(value);
	}

	/**
	 * Creates an exception for when a value is outside the allowed bounds.
	 *
	 * @param value the value that was out of bounds
	 * @param min   the minimum allowed value
	 * @param max   the maximum allowed value
	 * @return an IllegalArgumentException with appropriate message
	 */
	protected final IllegalArgumentException valueOutOfBoundsException(T value, T min, T max) {
		return new IllegalArgumentException("value %s [min=%s, max=%s]".formatted(value, min, max));
	}
}

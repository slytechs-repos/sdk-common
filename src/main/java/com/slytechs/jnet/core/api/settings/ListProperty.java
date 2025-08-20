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
import java.util.function.Function;

/**
 * A specialized property class for handling lists of elements within the
 * settings framework. This class provides type-safe operations for list
 * properties, including parsing from comma-separated strings and value
 * manipulation with proper type conversion.
 * 
 * <p>
 * ListProperty extends the base Property class and implements list-specific
 * functionality. It uses a provided parser function to convert string
 * representations of elements into their proper types. The class supports lists
 * of any type that can be parsed from a string representation.
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * // Create a list property for integers
 * ListProperty&lt;Integer&gt; numbers = new ListProperty&lt;&gt;("config.numbers", Integer::parseInt);
 * numbers.parseValue("1,2,3,4,5");
 * List&lt;Integer&gt; values = numbers.getList();
 * 
 * // Create a list property for enums
 * ListProperty&lt;TimeUnit&gt; units = new ListProperty&lt;&gt;("config.timeunits", TimeUnit::valueOf);
 * units.parseValue("SECONDS,MINUTES,HOURS");
 * 
 * // Create with initial values
 * List&lt;String&gt; initial = Arrays.asList("red", "green", "blue");
 * ListProperty&lt;String&gt; colors = new ListProperty&lt;&gt;("config.colors", String::trim, initial);
 * </pre>
 *
 * @param <E> the type of elements in the list
 * @author Mark Bednarczyk [mark@slytechs.com]
 * @author Sly Technologies Inc
 * @see Property
 * @see List
 */
public final class ListProperty<E> extends Property<List<E>, ListProperty<E>> {

	private final Function<String, E> componentParser;

	/**
	 * Creates a new ListProperty with the specified name and component parser. The
	 * property will be created in an unset state.
	 *
	 * @param name            the name of the property, used for identification
	 * @param componentParser the function used to parse individual elements from
	 *                        their string representation. This function should
	 *                        convert a string to an element of type E or throw an
	 *                        appropriate exception if parsing fails
	 */
	public ListProperty(String name, Function<String, E> componentParser) {
		super(name);
		this.componentParser = componentParser;
	}

	/**
	 * Creates a new ListProperty with the specified name, component parser, and
	 * initial list value. The property will be initialized with the provided list.
	 *
	 * @param name            the name of the property, used for identification
	 * @param componentParser the function used to parse individual elements from
	 *                        their string representation
	 * @param value           the initial list value for this property
	 */
	public ListProperty(String name, Function<String, E> componentParser, List<E> value) {
		super(name, value);
		this.componentParser = componentParser;
	}

	/**
	 * Creates a new ListProperty with the specified name and component parser. The
	 * property will be created in an unset state.
	 *
	 * @param support         the settings support instance for handling property
	 *                        change notifications
	 * @param name            the name of the property, used for identification
	 * @param componentParser the function used to parse individual elements from
	 *                        their string representation. This function should
	 *                        convert a string to an element of type E or throw an
	 *                        appropriate exception if parsing fails
	 */
	ListProperty(SettingsSupport support, String name, Function<String, E> componentParser) {
		super(support, name);
		this.componentParser = componentParser;
	}

	/**
	 * Creates a new ListProperty with the specified name, component parser, and
	 * initial list value. The property will be initialized with the provided list.
	 *
	 * @param support         the settings support instance for handling property
	 *                        change notifications
	 * @param name            the name of the property, used for identification
	 * @param componentParser the function used to parse individual elements from
	 *                        their string representation
	 * @param value           the initial list value for this property
	 */
	ListProperty(SettingsSupport support, String name, Function<String, E> componentParser, List<E> value) {
		super(support, name, value);
		this.componentParser = componentParser;
	}

	/**
	 * Parses a comma-separated string value and sets the property's value
	 * accordingly. The string is split on commas, and each element is parsed using
	 * the component parser function provided at construction. The resulting list
	 * maintains the order of elements as they appear in the input string.
	 *
	 * @param newValue the comma-separated string value to parse
	 * @return this ListProperty instance for method chaining
	 * @throws NullPointerException if newValue is null
	 * @throws RuntimeException     if the component parser throws an exception
	 *                              while parsing any element
	 */
	@Override
	public ListProperty<E> deserializeValue(String newValue) {
		String[] array = newValue.split(",");
		List<E> list = new ArrayList<>(array.length);

		for (String element : array) {
			E e = componentParser.apply(element);
			list.add(e);
		}

		return setValue(list);
	}

	/**
	 * Retrieves the current list value of this property. This is a convenience
	 * method that provides direct access to the list value without requiring
	 * casting from the generic type.
	 *
	 * @return the current list value of this property
	 * @throws IllegalStateException if the property has not been set
	 */
	public List<E> getList() {
		return getValue();
	}
}